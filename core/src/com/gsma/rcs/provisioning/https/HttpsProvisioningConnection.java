/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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
import com.gsma.rcs.provisioning.ProvisioningFailureReasons;
import com.gsma.rcs.utils.logger.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import java.io.IOException;

/**
 * HTTPS provisioning connection management
 * 
 * @author Orange
 */
public class HttpsProvisioningConnection {

    /**
     * HttpsProvisioningManager manages HTTP and SMS reception to load provisioning from network
     */
    private HttpsProvisioningManager mProvisioningManager;

    private BroadcastReceiver mNetworkStateListener;

    private ConnectivityManager mConnectionManager;

    /**
     * Wifi disabling listener
     */
    private BroadcastReceiver mWifiDisablingListener;

    private final Context mContext;

    private static final Logger sLogger = Logger.getLogger(HttpsProvisioningConnection.class
            .getName());

    /**
     * Constructor
     * 
     * @param httpsProvisioningManager HTTP provisioning manager
     * @param context The context
     */
    public HttpsProvisioningConnection(HttpsProvisioningManager httpsProvisioningManager,
            Context context) {
        mProvisioningManager = httpsProvisioningManager;
        mContext = context;
        mConnectionManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Get connection manager
     * 
     * @return connection manager
     */
    public ConnectivityManager getConnectionMngr() {
        return mConnectionManager;
    }

    /**
     * Register the broadcast receiver for network state
     */
    protected void registerNetworkStateListener() {
        // Check if network state listener is already registered
        if (mNetworkStateListener != null) {
            if (sLogger.isActivated()) {
                sLogger.debug("Network state listener already registered");
            }
            return;
        }

        if (sLogger.isActivated()) {
            sLogger.debug("Registering network state listener");
        }

        // Instantiate the network state listener
        mNetworkStateListener = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                mProvisioningManager.scheduleProvisioningOperation(new Runnable() {
                    @Override
                    public void run() {
                        String action = intent.getAction();
                        try {
                            if (sLogger.isActivated()) {
                                sLogger.debug("Network state listener - Received broadcast: "
                                        + action);
                            }
                            mProvisioningManager.connectionEvent(action);
                        } catch (RcsAccountException e) {
                            sLogger.error("Unable to handle connection event for intent action: "
                                    + action, e);
                        } catch (IOException e) {
                            if (sLogger.isActivated()) {
                                sLogger.debug(new StringBuilder(
                                        "Unable to handle connection event for intent action: ")
                                        .append(action).append(", Message=").append(e.getMessage())
                                        .toString());
                            }
                            /* Start the RCS service */
                            if (mProvisioningManager.isFirstProvisioningAfterBoot()) {
                                /* Reason: No configuration present */
                                if (sLogger.isActivated()) {
                                    sLogger.debug("Initial provisioning failed!");
                                }
                                mProvisioningManager
                                        .provisioningFails(ProvisioningFailureReasons.CONNECTIVITY_ISSUE);
                                mProvisioningManager.retry();
                            } else {
                                mProvisioningManager.tryLaunchRcsCoreService(context, -1);
                            }
                        } catch (RuntimeException e) {
                            /*
                             * Normally we are not allowed to catch runtime exceptions as these are
                             * genuine bugs which should be handled/fixed within the code. However
                             * the cases when we are executing operations on a thread unhandling
                             * such exceptions will eventually lead to exit the system and thus can
                             * bring the whole system down, which is not intended.
                             */
                            sLogger.error("Unable to handle connection event for intent action: "
                                    + action, e);
                        }
                    }
                });
            }
        };

        // Register network state listener
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mNetworkStateListener, intentFilter);
    }

    /**
     * Unregister the broadcast receiver for network state
     */
    protected void unregisterNetworkStateListener() {
        if (mNetworkStateListener != null) {
            if (sLogger.isActivated()) {
                sLogger.debug("Unregistering network state listener");
            }
            mContext.unregisterReceiver(mNetworkStateListener);
            mNetworkStateListener = null;
        }
    }

    /**
     * Register the broadcast receiver for wifi disabling
     */
    protected void registerWifiDisablingListener() {
        if (mWifiDisablingListener != null) {
            if (sLogger.isActivated()) {
                sLogger.debug("WIFI disabling listener already registered");
            }
            return;
        }

        if (sLogger.isActivated()) {
            sLogger.debug("Registering WIFI disabling listener");
        }

        mWifiDisablingListener = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                mProvisioningManager.scheduleProvisioningOperation(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (sLogger.isActivated()) {
                                sLogger.debug("Wifi disabling listener - Received broadcast: "
                                        + intent.toString());
                            }

                            if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                                    WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_DISABLED) {
                                mProvisioningManager.resetCounters();
                                registerNetworkStateListener();
                                unregisterWifiDisablingListener();
                            }
                        } catch (RuntimeException e) {
                            /*
                             * Normally we are not allowed to catch runtime exceptions as these are
                             * genuine bugs which should be handled/fixed within the code. However
                             * the cases when we are executing operations on a thread unhandling
                             * such exceptions will eventually lead to exit the system and thus can
                             * bring the whole system down, which is not intended.
                             */
                            sLogger.error(new StringBuilder(
                                    "Unable to handle wifi state change event for action : ")
                                    .append(intent.getAction()).toString(), e);
                        }
                    }
                });
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mWifiDisablingListener, intentFilter);
    }

    /**
     * Unregister the broadcast receiver for wifi disabling
     */
    protected void unregisterWifiDisablingListener() {
        if (mWifiDisablingListener != null) {
            if (sLogger.isActivated()) {
                sLogger.debug("Unregistering WIFI disabling listener");
            }
            try {
                mContext.unregisterReceiver(mWifiDisablingListener);
            } catch (IllegalArgumentException e) {
                // Nothing to do
            }
            mWifiDisablingListener = null;
        }
    }
}
