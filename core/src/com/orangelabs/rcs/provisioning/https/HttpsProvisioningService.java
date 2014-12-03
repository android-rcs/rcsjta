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

package com.orangelabs.rcs.provisioning.https;

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;

import com.orangelabs.rcs.provider.LocalContentResolver;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provisioning.ProvisioningInfo;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.utils.logger.Logger;

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
    private PendingIntent retryIntent = null;

    /**
     * Provisioning manager
     */
    HttpsProvisioningManager httpsProvisioningMng;

    LocalContentResolver mLocalContentResolver;

    /**
	 * Retry action for provisioning failure
	 */
    private static final String ACTION_RETRY = "com.orangelabs.rcs.provisioning.https.HttpsProvisioningService.ACTION_RETRY";

	/**
	 * The logger
	 */
    private static Logger logger = Logger.getLogger(HttpsProvisioningService.class.getSimpleName());

	@Override
	public void onCreate() {
		if (logger.isActivated()) {
			logger.debug("onCreate");
		}
		Context ctx = getApplicationContext();
		mLocalContentResolver = new LocalContentResolver(ctx.getContentResolver());
		RcsSettings.createInstance(ctx);
		this.retryIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_RETRY), 0);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (logger.isActivated()) {
			logger.debug("Start HTTPS provisioning");
		}

		boolean first = false;
		boolean user = false;
		if (intent != null) {
			first = intent.getBooleanExtra(FIRST_KEY, false);
			user = intent.getBooleanExtra(USER_KEY, false);
		}
		String version = RcsSettings.getInstance().getProvisioningVersion();
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

		httpsProvisioningMng = new HttpsProvisioningManager(getApplicationContext(),
				mLocalContentResolver, retryIntent, first, user);
		if (logger.isActivated()) {
			logger.debug("Provisioning parameter: boot=" + first + ", user=" + user + ", version=" + version);
		}

		boolean requestConfig = false;
        if (first) {
            requestConfig = true;
        } else {
            if (ProvisioningInfo.Version.RESETED_NOQUERY.equals(version)) {
                // Nothing to do
            } else if (ProvisioningInfo.Version.DISABLED_NOQUERY.equals(version)) {
                if (user == true) {
                    requestConfig = true;
                }
            } else if (ProvisioningInfo.Version.DISABLED_DORMANT.equals(version) && user == true) {
                requestConfig = true;
            } else { // version > 0
                Date expiration = LauncherUtils.getProvisioningExpirationDate(this);
                if (expiration == null) {
                    requestConfig = true;
                } else {
                    Date now = new Date();
                    if (expiration.before(now)) {
                        if (logger.isActivated())
                            logger.debug("Configuration validity expired at " + expiration);
                        requestConfig = true;
                    } else {
                        long delay = (expiration.getTime() - now.getTime());
                        if (delay <= 0L) {
                            requestConfig = true;
                        } else {
                            Long validity = LauncherUtils.getProvisioningValidity(this) * 1000L;
                            if (validity != null && delay > validity) {
                                delay = validity;
                            }
                            if (logger.isActivated())
                                logger.debug("Configuration will expire in " + (delay / 1000)
                                        + " secs at " + expiration);
                            startRetryAlarm(this, retryIntent, delay);
                        }
                    }
                }
            }
        }

		if (requestConfig) {
			if (logger.isActivated())
				logger.debug("Request HTTP configuration update");
			// Send default connection event
			if (!httpsProvisioningMng.connectionEvent(ConnectivityManager.CONNECTIVITY_ACTION)) {
				// If the UpdateConfig has NOT been done:
				httpsProvisioningMng.registerNetworkStateListener();
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
	 * @param delay
	 *            delay in milli seconds
	 */
	public static void startRetryAlarm(Context context, PendingIntent intent, long delay) {
		if (logger.isActivated()) {
			logger.debug( "Retry HTTP configuration update in "+(delay/1000)+" secs");
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
		if (logger.isActivated()) {
			logger.debug( "Stop retry configuration update");
		}
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(intent);
	}
    
    @Override
    public void onDestroy() {
		if (httpsProvisioningMng != null) {
			// Unregister network state listener
			httpsProvisioningMng.unregisterNetworkStateListener();

			// Unregister wifi disabling listener
			httpsProvisioningMng.unregisterWifiDisablingListener();

			// Unregister SMS provisioning receiver
			httpsProvisioningMng.unregisterSmsProvisioningReceiver();
		}

		cancelRetryAlarm(this, retryIntent);
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
            Thread t = new Thread() {
                public void run() {
                    httpsProvisioningMng.updateConfig();
                }
            };
            t.start();
        }
    };
    
	/**
	 * Start the HTTPs provisioning service
	 * 
	 * @param context
	 * @param firstLaunch
	 *            first launch after (re)boot
	 * @param userLaunch
	 *            launch is requested by user action
	 */
	public static void startHttpsProvisioningService(Context context , boolean firstLaunch, boolean userLaunch) {
		// Start Https provisioning service
		Intent provisioningIntent = new Intent(context, HttpsProvisioningService.class);
		provisioningIntent.putExtra(FIRST_KEY, firstLaunch);
		provisioningIntent.putExtra(USER_KEY, userLaunch);
		context.startService(provisioningIntent);
	}
}