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

import com.gsma.rcs.addressbook.RcsAccountException;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.TermsAndConditionsResponse;
import com.gsma.rcs.provisioning.ProvisioningFailureReasons;
import com.gsma.rcs.provisioning.ProvisioningInfo.Version;
import com.gsma.rcs.provisioning.TermsAndConditionsRequest;
import com.gsma.rcs.service.LauncherUtils;
import com.gsma.rcs.utils.TimerUtils;
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
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;

import java.io.IOException;

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

    private ContactManager mContactManager;

    /**
     * Retry action for provisioning failure
     */
    private static final String ACTION_RETRY = "com.gsma.rcs.provisioning.https.HttpsProvisioningService.ACTION_RETRY";

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
        mMessagingLog = MessagingLog.createInstance(mLocalContentResolver, mRcsSettings);
        mRetryIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_RETRY), 0);
        mContactManager = ContactManager.createInstance(mContext, contentResolver,
                mLocalContentResolver, mRcsSettings);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        // @FIXME: Below code block needs a complete refactor, However at this
        // moment due to other prior tasks the refactoring task has been kept in
        // backlog.
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
        int version = mRcsSettings.getProvisioningVersion();

        if (Version.RESETED.toInt() == version) {
            first = true;
        }
        registerReceiver(retryReceiver, new IntentFilter(ACTION_RETRY));

        TelephonyManager tm = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        String imsi = tm.getSubscriberId();
        if (imsi == null) {
            /*
             * IMSI may be null if SIM card is not present or Telephony manager is not fully
             * initialized and it is not the first launch. We should then consider the last user
             * account.
             */
            imsi = LauncherUtils.getLastUserAccount(mContext);
        }
        String imei = tm.getDeviceId();

        mHttpsProvisioningMng = new HttpsProvisioningManager(imei, imsi, mContext,
                mLocalContentResolver, mRetryIntent, first, user, mRcsSettings, mMessagingLog,
                mContactManager);
        if (logActivated) {
            sLogger.debug(new StringBuilder("Provisioning (first=").append(first)
                    .append(") (user=").append(user).append(") (version=").append(version)
                    .append(")").toString());
        }

        boolean requestConfig = false;
        if (TermsAndConditionsResponse.DECLINED == mRcsSettings.getTermsAndConditionsResponse()) {
            if (logActivated) {
                sLogger.debug("Do not request configuration since TC were declined!");
            }
        } else if (first) {
            requestConfig = true;
        } else if (Version.RESETED.toInt() == version) {
            requestConfig = true;
        } else if (Version.RESETED_NOQUERY.toInt() == version) {
            // Nothing to do
        } else if (Version.DISABLED_NOQUERY.toInt() == version) {
            if (user == true) {
                requestConfig = true;
            }
        } else if (Version.DISABLED_DORMANT.toInt() == version && user == true) {
            requestConfig = true;
        } else { // version > 0
            long expiration = LauncherUtils.getProvisioningExpirationDate(this);
            if (expiration <= 0) {
                requestConfig = true;
            } else {
                long now = System.currentTimeMillis();
                if (expiration <= now) {
                    if (logActivated) {
                        sLogger.debug("Configuration validity expired at ".concat(DateUtils
                                .formatDateTime(mContext, expiration, DateUtils.FORMAT_SHOW_DATE
                                        | DateUtils.FORMAT_SHOW_TIME
                                        | DateUtils.FORMAT_NUMERIC_DATE)));
                    }
                    requestConfig = true;
                } else {
                    if (TermsAndConditionsResponse.NO_ANSWER == mRcsSettings
                            .getTermsAndConditionsResponse()) {
                        TermsAndConditionsRequest.showTermsAndConditions(mContext);
                    }
                    long delay = expiration - now;
                    long validity = LauncherUtils.getProvisioningValidity(this);
                    if (validity > 0 && delay > validity) {
                        delay = validity;
                    }
                    if (logActivated) {
                        sLogger.debug(new StringBuilder("Configuration will expire at ").append(
                                DateUtils.formatDateTime(mContext, expiration,
                                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
                                                | DateUtils.FORMAT_NUMERIC_DATE)).toString());
                    }
                    startRetryAlarm(this, mRetryIntent, delay);
                }
            }
        }

        if (requestConfig) {
            if (logActivated)
                sLogger.debug("Request HTTP configuration update");
            mHttpsProvisioningMng.scheduleProvisioningOperation(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Send default connection event
                        if (!mHttpsProvisioningMng
                                .connectionEvent(ConnectivityManager.CONNECTIVITY_ACTION)) {
                            // If the UpdateConfig has NOT been done:
                            mHttpsProvisioningMng.registerNetworkStateListener();
                        }

                    } catch (RcsAccountException e) {
                        /**
                         * This is a non revocable use-case as the RCS account itself was not
                         * created, So we log this as error and stop the service itself.
                         */
                        sLogger.error("Failed to handle connection event!", e);
                        stopSelf();

                    } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                        sLogger.error("Unable to handle connection event!", e);

                    } catch (IOException e) {
                        if (sLogger.isActivated()) {
                            sLogger.debug(new StringBuilder(
                                    "Unable to handle connection event, Message=").append(
                                    e.getMessage()).toString());
                        }
                        /* Start the RCS service */
                        if (mHttpsProvisioningMng.isFirstProvisioningAfterBoot()) {
                            /* Reason: No configuration present */
                            if (sLogger.isActivated()) {
                                sLogger.debug("Initial provisioning failed!");
                            }
                            mHttpsProvisioningMng
                                    .provisioningFails(ProvisioningFailureReasons.CONNECTIVITY_ISSUE);
                            mHttpsProvisioningMng.retry();
                        } else {
                            mHttpsProvisioningMng.tryLaunchRcsCoreService(mContext, -1);
                        }
                    }
                }
            });
        }
        /*
         * We want this service to continue running until it is explicitly stopped, so return
         * sticky.
         */
        return START_STICKY;
    }

    /**
     * Start retry alarm
     * 
     * @param context the application context
     * @param intent the pending intent to execute when alarm is raised
     * @param delay delay in milliseconds
     */
    public static void startRetryAlarm(Context context, PendingIntent intent, long delay) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Retry HTTP configuration update in ").append(
                    DateUtils.formatElapsedTime(delay / 1000)).toString());
        }
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        TimerUtils.setExactTimer(am, System.currentTimeMillis() + delay, intent);
    }

    /**
     * Cancel retry alarm
     * 
     * @param context the application context
     * @param intent the pending intent to cancel
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
            mHttpsProvisioningMng.unregisterNetworkStateListener();
            mHttpsProvisioningMng.unregisterWifiDisablingListener();
            mHttpsProvisioningMng.unregisterSmsProvisioningReceiver();
            mHttpsProvisioningMng.quitProvisioningOperation();
        }
        cancelRetryAlarm(this, mRetryIntent);
        try {
            unregisterReceiver(retryReceiver);
        } catch (IllegalArgumentException e) {
            /* Nothing to be handled here */
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
        public void onReceive(Context context, final Intent intent) {
            mHttpsProvisioningMng.scheduleProvisioningOperation(new Runnable() {
                public void run() {
                    try {
                        mHttpsProvisioningMng.updateConfig();

                    } catch (RcsAccountException e) {
                        sLogger.error("Failed to update configuration!", e);

                    } catch (RuntimeException e) {
                        /*
                         * Intentionally catch runtime exceptions as else it will abruptly end the
                         * thread and eventually bring the whole system down, which is not intended.
                         */
                        sLogger.error("Failed to update configuration!", e);

                    } catch (IOException e) {
                        if (sLogger.isActivated()) {
                            sLogger.debug(new StringBuilder(
                                    "Unable to handle connection event, Message=").append(
                                    e.getMessage()).toString());
                        }
                        /* Start the RCS service */
                        if (mHttpsProvisioningMng.isFirstProvisioningAfterBoot()) {
                            /* Reason: No configuration present */
                            if (sLogger.isActivated()) {
                                sLogger.debug("Initial provisioning failed!");
                            }
                            mHttpsProvisioningMng
                                    .provisioningFails(ProvisioningFailureReasons.CONNECTIVITY_ISSUE);
                            mHttpsProvisioningMng.retry();
                        } else {
                            mHttpsProvisioningMng.tryLaunchRcsCoreService(mContext, -1);
                        }
                    }
                }
            });
        }
    };

    /**
     * Start the HTTPs provisioning service
     * 
     * @param context the application context
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
