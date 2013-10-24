/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.provisioning.https;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;

import com.orangelabs.rcs.provider.settings.RcsSettings;
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
     * Provisioning manager
     */
    HttpsProvisioningManager httpsProvisioningMng;
    
    /**
	 * Retry action for provisioning failure
	 */
    protected String ACTION_RETRY = "com.orangelabs.rcs.provisioning.https.HttpsProvisioningService.ACTION_RETRY";

	/**
	 * The logger
	 */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    @Override
    public void onCreate() {
        // Instantiate RcsSettings and Provisioning manager
        RcsSettings.createInstance(getApplicationContext());
        
        PendingIntent retryIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_RETRY), 0);
        registerReceiver(retryReceiver, new IntentFilter(ACTION_RETRY));
        httpsProvisioningMng = new HttpsProvisioningManager(getApplicationContext(), retryIntent);
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		if (logger.isActivated()) {
			logger.debug("Start HTTPS provisioning");
		}

        String version = RcsSettings.getInstance().getProvisioningVersion();
        if (logger.isActivated()) {
        	logger.debug("Provisioning parameter: first=" + httpsProvisioningMng.setIsFirstLaunchFromIntent(intent) + ", version= " + version);
        }
        
        // it makes no sense to start service if version is 0 (unconfigured)
        // if version = 0, then (re)set first to true
        int ver = Integer.parseInt(version);
        if (ver == 0) {
        	httpsProvisioningMng.setIsFirstLaunch(true);
        }

        // Send default connection event
        if (!httpsProvisioningMng.connectionEvent(ConnectivityManager.CONNECTIVITY_ACTION)) {
            // If the UpdateConfig has NOT been done:
        	httpsProvisioningMng.registerNetworkStateListener();
        }
		
    	// We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
    

    @Override
    public void onDestroy() {
		// Unregister network state listener
        httpsProvisioningMng.unregisterNetworkStateListener();

        // Unregister wifi disabling listener
        httpsProvisioningMng.unregisterWifiDisablingListener();
        
        // Unregister SMS provisioning receiver
        httpsProvisioningMng.unregisterSmsProvisioningReceiver();

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
}