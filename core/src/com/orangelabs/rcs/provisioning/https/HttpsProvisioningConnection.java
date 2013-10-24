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

import com.orangelabs.rcs.utils.logger.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

/**
 * HTTPS provisioning connection management
 *
 * @author Orange
 */
public class HttpsProvisioningConnection {

    /**
     * HttpsProvisioningManager manages HTTP and SMS reception to load
     * provisioning from network
     */
    private HttpsProvisioningManager manager;

    /**
     * Network state listener
     */
    private BroadcastReceiver networkStateListener = null;

    /**
     * Connection manager
     */
    private ConnectivityManager connMgr = null;

    /**
     * Wifi disabling listener
     */
    private BroadcastReceiver wifiDisablingListener = null;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param httpsProvisioningManager HTTP provisioning manager
     */
    public HttpsProvisioningConnection(HttpsProvisioningManager httpsProvisioningManager) {
        manager = httpsProvisioningManager;

        // Get connectivity manager
        if (connMgr == null) {
            connMgr = (ConnectivityManager) httpsProvisioningManager.getContext().getSystemService(
                    Context.CONNECTIVITY_SERVICE);
        }
    }

    /**
     * Get connection manager
     *
     * @return connection manager
     */
    public ConnectivityManager getConnectionMngr() {
        return connMgr;
    }

    /**
     * Register the broadcast receiver for network state
     */
    protected void registerNetworkStateListener() {
        // Check if network state listener is already registered
        if (networkStateListener != null) {
            if (logger.isActivated()) {
                logger.debug("Network state listener already registered");
            }
            return;
        }

        if (logger.isActivated()) {
            logger.debug("Registering network state listener");
        }

        // Instantiate the network state listener
        networkStateListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                Thread t = new Thread() {
                    public void run() {
                        if (logger.isActivated()) {
                            logger.debug("Network state listener - Received broadcast: "
                                    + intent.toString());
                        }

                        manager.connectionEvent(intent.getAction());
                    }
                };
                t.start();
            }
        };

        // Register network state listener
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        manager.getContext().registerReceiver(networkStateListener, intentFilter);
    }

    /**
     * Unregister the broadcast receiver for network state
     */
    protected void unregisterNetworkStateListener() {
        if (networkStateListener != null) {
            if (logger.isActivated()) {
                logger.debug("Unregistering network state listener");
            }

            try {
                manager.getContext().unregisterReceiver(networkStateListener);
            } catch (IllegalArgumentException e) {
                // Nothing to do
            }
            networkStateListener = null;
        }
    }

    /**
     * Register the broadcast receiver for wifi disabling
     */
    protected void registerWifiDisablingListener() {
        // Check if wifi disabling listener is already registered
        if (wifiDisablingListener != null) {
            if (logger.isActivated()) {
                logger.debug("WIFI disabling listener already registered");
            }
            return;
        }

        if (logger.isActivated()) {
            logger.debug("Registering WIFI disabling listener");
        }

        // Instantiate the wifi listener
        wifiDisablingListener = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                Thread t = new Thread() {
                    public void run() {
                        if (logger.isActivated()) {
                            logger.debug("Wifi disabling listener - Received broadcast: "
                                    + intent.toString());
                        }

                        // Only notify the listener when the wifi is really
                        // disabled
                        if (intent != null
                                && intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                                        WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_DISABLED) {

                            manager.resetCounters();

                            // Register network state listener
                            registerNetworkStateListener();

                            // Unregister wifi disabling listener
                            unregisterWifiDisablingListener();
                        }
                    }
                };
                t.start();
            }
        };

        // Register wifi disabling listener
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        manager.getContext().registerReceiver(wifiDisablingListener, intentFilter);
    }

    /**
     * Unregister the broadcast receiver for wifi disabling
     */
    protected void unregisterWifiDisablingListener() {
        if (wifiDisablingListener != null) {
            if (logger.isActivated()) {
                logger.debug("Unregistering WIFI disabling listener");
            }

            try {
                manager.getContext().unregisterReceiver(wifiDisablingListener);
            } catch (IllegalArgumentException e) {
                // Nothing to do
            }
            wifiDisablingListener = null;
        }
    }
}
