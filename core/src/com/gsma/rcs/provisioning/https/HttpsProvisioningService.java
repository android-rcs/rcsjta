/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provisioning.https;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provisioning.ProvisioningInfo;
import com.gsma.rcs.service.LauncherUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;

/**
 * HTTPS auto configuration service
 * 
 * @author hlxn7157
 * @author G. LE PESSOT
 * @author Deutsche Telekom AG
 */
public class HttpsProvisioningService extends Service {
    /**
     * Intent key - Provisioning requested after (re)boot
     */
    private static final String FIRST_KEY = "first";

    /**
     * Intent key - Provisioning requested by user
     */
    private static final String USER_KEY = "user";

    /**
     * Retry Intent
     */
    private PendingIntent mRetryIntent;

    private RcsSettings mRcsSettings;

    /**
     * Provisioning manager
     */
    private HttpsProvisioningManager mHttpsProvisioningMng;

    private LocalContentResolver mLocalContentResolver;

    private Context mContext;

    private MessagingLog mMessagingLog;

    private ContactsManager mContactManager;

    /**
     * Retry action for provisioning failure
     */
    private static final String ACTION_RETRY = "com.gsma.rcs.provisioning.https.HttpsProvisioningService.ACTION_RETRY";

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(HttpsProvisioningService.class
            .getSimpleName());

    @Override
    public void onCreate() {
        if (sLogger.isActivated()) {
            sLogger.debug("onCreate");
        }
        mContext = getApplicationContext();
        ContentResolver contentResolver = mContext.getContentResolver();
        mLocalContentResolver = new LocalContentResolver(contentResolver);
        mRcsSettings = RcsSettings.createInstance(mLocalContentResolver);
        mMessagingLog = MessagingLog.createInstance(mContext, mLocalContentResolver, mRcsSettings);
        mRetryIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_RETRY), 0);
        mContactManager = ContactsManager.createInstance(mContext, contentResolver,
                mLocalContentResolver, mRcsSettings);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("Start HTTPS provisioning");
        }

        boolean first = false;
        boolean user = false;
        if (intent != null) {
            first = intent.getBooleanExtra(FIRST_KEY, false);
            user = intent.getBooleanExtra(USER_KEY, false);
        }
        String version = mRcsSettings.getProvisioningVersion();
        // It makes no sense to start service if version is 0 (unconfigured)
        // if version = 0, then (re)set first to true
        try {
            int ver = Integer.parseInt(version);
            if (ver == 0) {
                first = true;
            }
        } catch (NumberFormatException e) {
            // Nothing to do
        }
        registerReceiver(retryReceiver, new IntentFilter(ACTION_RETRY));

        mHttpsProvisioningMng = new HttpsProvisioningManager(mContext, mLocalContentResolver,
                mRetryIntent, first, user, mRcsSettings, mMessagingLog, mContactManager);
        if (logActivated) {
            sLogger.debug(new StringBuilder("Provisioning (boot=").append(first).append(") (user=")
                    .append(user).append(") (version=").append(version).append(")").toString());
        }

        boolean requestConfig = false;
        if (first) {
            requestConfig = true;
        } else if (ProvisioningInfo.Version.RESETED_NOQUERY.equals(version)) {
            // Nothing to do
        } else if (ProvisioningInfo.Version.DISABLED_NOQUERY.equals(version)) {
            if (user == true) {
                requestConfig = true;
            }
        } else if (ProvisioningInfo.Version.DISABLED_DORMANT.equals(version) && user == true) {
            requestConfig = true;
        } else { // version > 0
            long expiration = LauncherUtils.getProvisioningExpirationDate(this);
            if (expiration <= 0) {
                requestConfig = true;
            } else {
                long now = System.currentTimeMillis();
                if (expiration <= now) {
                    if (logActivated) {
                        sLogger.debug("Configuration validity expired at ".concat(String
                                .valueOf(expiration)));
                    }
                    requestConfig = true;
                } else {
                    long delay = expiration - now;
                    long validity = LauncherUtils.getProvisioningValidity(this);
                    if (validity > 0 && delay > validity) {
                        delay = validity;
                    }
                    if (logActivated) {
                        sLogger.debug(new StringBuilder("Configuration will expire in ")
                                .append(delay / 1000).append(" secs at ").append(expiration)
                                .toString());
                    }
                    startRetryAlarm(this, mRetryIntent, delay);
                }
            }
        }

        if (requestConfig) {
            if (logActivated)
                sLogger.debug("Request HTTP configuration update");
            // Send default connection event
            if (!mHttpsProvisioningMng.connectionEvent(ConnectivityManager.CONNECTIVITY_ACTION)) {
                // If the UpdateConfig has NOT been done:
                mHttpsProvisioningMng.registerNetworkStateListener();
            }
        }
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    /**
     * Start retry alarm
     * 
     * @param context
     * @param intent
     * @param delay delay in milliseconds
     */
    public static void startRetryAlarm(Context context, PendingIntent intent, long delay) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Retry HTTP configuration update in ")
                    .append(delay / 1000).append(" secs").toString());
        }
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, intent);
    }

    /**
     * Cancel retry alarm
     * 
     * @param context
     * @param intent
     */
    public static void cancelRetryAlarm(Context context, PendingIntent intent) {
        if (sLogger.isActivated()) {
            sLogger.debug("Stop retry configuration update");
        }
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(intent);
    }

    @Override
    public void onDestroy() {
        if (mHttpsProvisioningMng != null) {
            // Unregister network state listener
            mHttpsProvisioningMng.unregisterNetworkStateListener();

            // Unregister wifi disabling listener
            mHttpsProvisioningMng.unregisterWifiDisablingListener();

            // Unregister SMS provisioning receiver
            mHttpsProvisioningMng.unregisterSmsProvisioningReceiver();
        }

        cancelRetryAlarm(this, mRetryIntent);
        // Unregister retry receiver
        try {
            unregisterReceiver(retryReceiver);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Retry receiver
     */
    private BroadcastReceiver retryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Thread() {
                public void run() {
                    mHttpsProvisioningMng.updateConfig();
                }
            }.start();
        }
    };

    /**
     * Start the HTTPs provisioning service
     * 
     * @param context
     * @param firstLaunch first launch after (re)boot
     * @param userLaunch launch is requested by user action
     */
    public static void startHttpsProvisioningService(Context context, boolean firstLaunch,
            boolean userLaunch) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("startHttpsProvisioningService (first=")
                    .append(firstLaunch).append(") (user=").append(userLaunch).append(")")
                    .toString());
        }
        // Start HTTPS provisioning service
        Intent provisioningIntent = new Intent(context, HttpsProvisioningService.class);
        provisioningIntent.putExtra(FIRST_KEY, firstLaunch);
        provisioningIntent.putExtra(USER_KEY, userLaunch);
        context.startService(provisioningIntent);
    }
}
