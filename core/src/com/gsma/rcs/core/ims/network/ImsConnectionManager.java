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

package com.gsma.rcs.core.ims.network;

import java.util.Random;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.telephony.TelephonyManager;

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.ImsNetworkInterface.DnsResolvedFields;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.platform.network.NetworkFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.NetworkAccessType;
import com.gsma.rcs.service.LauncherUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.CommonServiceConfiguration.MinimumBatteryLevel;

/**
 * IMS connection manager
 * 
 * @author JM. Auffret
 * @author Deutsche Telekom
 */
public class ImsConnectionManager implements Runnable {
    /**
     * IMS module
     */
    private ImsModule mImsModule;

    /**
     * Network interfaces
     */
    private ImsNetworkInterface[] mNetworkInterfaces = new ImsNetworkInterface[2];

    /**
     * IMS network interface
     */
    private ImsNetworkInterface mCurrentNetworkInterface;

    /**
     * IMS polling thread
     */
    private Thread mImsPollingThread;

    /**
     * IMS polling thread Id
     */
    private long mImsPollingThreadId = -1;

    /**
     * Connectivity manager
     */
    private ConnectivityManager mCnxManager;

    /**
     * Network access type
     */
    private NetworkAccessType mNetwork;

    /**
     * Operator
     */
    private String mOperator;

    /**
     * DNS resolved fields
     */
    private DnsResolvedFields mDnsResolvedFields;

    /**
     * Battery level state
     */
    private boolean mDisconnectedByBattery = false;

    /**
     * IMS services already started
     */
    private boolean mImsServicesStarted = false;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param imsModule IMS module
     * @param rcsSettings RcsSettings instance
     * @throws CoreException
     */
    public ImsConnectionManager(ImsModule imsModule, RcsSettings rcsSettings) throws CoreException {
        mImsModule = imsModule;
        mRcsSettings = rcsSettings;

        // Get network access parameters
        mNetwork = rcsSettings.getNetworkAccess();

        // Get network operator parameters
        mOperator = rcsSettings.getNetworkOperator();

        Context appContext = AndroidFactory.getApplicationContext();
        // Set the connectivity manager
        mCnxManager = (ConnectivityManager) appContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        // Instantiates the IMS network interfaces
        mNetworkInterfaces[0] = new MobileNetworkInterface(imsModule, rcsSettings);
        mNetworkInterfaces[1] = new WifiNetworkInterface(imsModule, rcsSettings);

        // Set the mobile network interface by default
        mCurrentNetworkInterface = getMobileNetworkInterface();

        // Load the user profile
        loadUserProfile();

        // Register network state listener
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        appContext.registerReceiver(networkStateListener, intentFilter);

        // Battery management
        appContext.registerReceiver(batteryLevelListener, new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED));
    }

    /**
     * Returns the current network interface
     * 
     * @return Current network interface
     */
    public ImsNetworkInterface getCurrentNetworkInterface() {
        return mCurrentNetworkInterface;
    }

    /**
     * Returns the mobile network interface
     * 
     * @return Mobile network interface
     */
    public ImsNetworkInterface getMobileNetworkInterface() {
        return mNetworkInterfaces[0];
    }

    /**
     * Returns the Wi-Fi network interface
     * 
     * @return Wi-Fi network interface
     */
    public ImsNetworkInterface getWifiNetworkInterface() {
        return mNetworkInterfaces[1];
    }

    /**
     * Is connected to Wi-Fi
     * 
     * @return Boolean
     */
    public boolean isConnectedToWifi() {
        return mCurrentNetworkInterface == getWifiNetworkInterface();
    }

    /**
     * Is connected to mobile
     * 
     * @return Boolean
     */
    public boolean isConnectedToMobile() {
        return mCurrentNetworkInterface == getMobileNetworkInterface();
    }

    /**
     * Is disconnected by battery
     * 
     * @return Returns true if disconnected by battery, else returns false
     */
    public boolean isDisconnectedByBattery() {
        return mDisconnectedByBattery;
    }

    /**
     * Load the user profile associated to the network interface
     */
    private void loadUserProfile() {
        ImsModule.IMS_USER_PROFILE = mCurrentNetworkInterface.getUserProfile();
        if (logger.isActivated()) {
            logger.debug("User profile has been reloaded");
        }
    }

    /**
     * Terminate the connection manager
     */
    public void terminate() {
        if (logger.isActivated()) {
            logger.info("Terminate the IMS connection manager");
        }

        // Unregister battery listener
        try {
            AndroidFactory.getApplicationContext().unregisterReceiver(batteryLevelListener);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }

        // Unregister network state listener
        try {
            AndroidFactory.getApplicationContext().unregisterReceiver(networkStateListener);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }

        // Stop the IMS connection manager
        stopImsConnection();

        // Unregister from the IMS
        mCurrentNetworkInterface.unregister();

        if (logger.isActivated()) {
            logger.info("IMS connection manager has been terminated");
        }
    }

    /**
     * Network state listener
     */
    private BroadcastReceiver networkStateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            Thread t = new Thread() {
                public void run() {
                    connectionEvent(intent);
                }
            };
            t.start();
        }
    };

    /**
     * Connection event
     * 
     * @param intent Intent
     */
    private synchronized void connectionEvent(Intent intent) {
        if (mDisconnectedByBattery) {
            return;
        }

        if (!intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            return;
        }

        boolean connectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY,
                false);
        String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
        boolean failover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
        if (logger.isActivated()) {
            logger.debug("Connectivity event change: failover=" + failover + ", connectivity="
                    + !connectivity + ", reason=" + reason);
        }

        // Check received network info
        NetworkInfo networkInfo = mCnxManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            // Disconnect from IMS network interface
            if (logger.isActivated()) {
                logger.debug("Disconnect from IMS: no network (e.g. air plane mode)");
            }
            disconnectFromIms();
            return;
        }

        // Check if SIM account has changed (i.e. hot SIM swap)
        if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            String lastUserAccount = LauncherUtils.getLastUserAccount(AndroidFactory
                    .getApplicationContext());
            String currentUserAccount = LauncherUtils.getCurrentUserAccount(AndroidFactory
                    .getApplicationContext());
            if (lastUserAccount != null) {
                if ((currentUserAccount == null)
                        || !currentUserAccount.equalsIgnoreCase(lastUserAccount)) {
                    mImsModule.getCoreListener().handleSimHasChanged();
                    return;
                }
            }
        }

        // Get the current local IP address
        String localIpAddr = null;

        // Check if the network access type has changed
        if (networkInfo.getType() != mCurrentNetworkInterface.getType()) {
            // Network interface changed
            if (logger.isActivated()) {
                logger.info("Data connection state: NETWORK ACCESS CHANGED");
            }

            // Disconnect from current IMS network interface
            if (logger.isActivated()) {
                logger.debug("Disconnect from IMS: network access has changed");
            }
            disconnectFromIms();

            // Change current network interface
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                if (logger.isActivated()) {
                    logger.debug("Change the network interface to mobile");
                }
                mCurrentNetworkInterface = getMobileNetworkInterface();
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                if (logger.isActivated()) {
                    logger.debug("Change the network interface to Wi-Fi");
                }
                mCurrentNetworkInterface = getWifiNetworkInterface();
            }

            // Load the user profile for the new network interface
            loadUserProfile();

            // update DNS entry
            try {
                mDnsResolvedFields = mCurrentNetworkInterface.getDnsResolvedFields();
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error(
                            "Resolving remote IP address to figure out initial local IP address failed!",
                            e);
                }
            }

            // get latest local IP address
            localIpAddr = NetworkFactory.getFactory().getLocalIpAddress(mDnsResolvedFields,
                    networkInfo.getType());

        } else {
            // Check if the IP address has changed
            try {
                if (mDnsResolvedFields == null) {
                    mDnsResolvedFields = mCurrentNetworkInterface.getDnsResolvedFields();
                }
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error(
                            "Resolving remote IP address to figure out initial local IP address failed!",
                            e);
                }
            }
            localIpAddr = NetworkFactory.getFactory().getLocalIpAddress(mDnsResolvedFields,
                    networkInfo.getType());

            if (localIpAddr != null) {
                String lastIpAddr = mCurrentNetworkInterface.getNetworkAccess().getIpAddress();
                if (!localIpAddr.equals(lastIpAddr)) {
                    // Changed by Deutsche Telekom
                    if (lastIpAddr != null) {
                        // Disconnect from current IMS network interface
                        if (logger.isActivated()) {
                            logger.debug("Disconnect from IMS: IP address has changed");
                        }
                        disconnectFromIms();
                    } else {
                        if (logger.isActivated()) {
                            logger.debug("IP address available (again)");
                        }
                    }
                } else {
                    // Changed by Deutsche Telekom
                    if (logger.isActivated()) {
                        logger.debug("Neither interface nor IP address has changed; nothing to do.");
                    }
                    return;
                }
            }
        }

        // Check if there is an IP connectivity
        if (networkInfo.isConnected() && (localIpAddr != null)) {
            String remoteAddress;
            if (mDnsResolvedFields != null) {
                remoteAddress = mDnsResolvedFields.mIpAddress;
            } else {
                remoteAddress = new String("unresolved");
            }

            if (logger.isActivated()) {
                logger.info("Data connection state: CONNECTED to " + networkInfo.getTypeName()
                        + " with local IP " + localIpAddr + " valid for " + remoteAddress);
            }

            // Test network access type
            if (!NetworkAccessType.ANY.equals(mNetwork)
                    && (mNetwork.toInt() != networkInfo.getType())) {
                if (logger.isActivated()) {
                    logger.warn("Network access " + networkInfo.getTypeName()
                            + " is not authorized");
                }
                return;
            }

            // Test the operator id
            TelephonyManager tm = (TelephonyManager) AndroidFactory.getApplicationContext()
                    .getSystemService(Context.TELEPHONY_SERVICE);
            String currentOpe = tm.getSimOperatorName();
            if ((mOperator.length() > 0) && !currentOpe.equalsIgnoreCase(mOperator)) {
                if (logger.isActivated()) {
                    logger.warn("Operator not authorized");
                }
                return;
            }

            // Test the configuration
            if (!mCurrentNetworkInterface.isInterfaceConfigured()) {
                if (logger.isActivated()) {
                    logger.warn("IMS network interface not well configured");
                }
                return;
            }

            // Connect to IMS network interface
            if (logger.isActivated()) {
                logger.debug("Connect to IMS");
            }
            connectToIms(localIpAddr);
        } else {
            if (logger.isActivated()) {
                logger.info("Data connection state: DISCONNECTED from " + networkInfo.getTypeName());
            }

            // Disconnect from IMS network interface
            if (logger.isActivated()) {
                logger.debug("Disconnect from IMS: IP connection lost");
            }
            disconnectFromIms();
        }
    }

    /**
     * Connect to IMS network interface
     * 
     * @param ipAddr IP address
     */
    private void connectToIms(String ipAddr) {
        // Connected to the network access
        mCurrentNetworkInterface.getNetworkAccess().connect(ipAddr);

        // Start the IMS connection
        startImsConnection();
    }

    /**
     * Disconnect from IMS network interface
     */
    private void disconnectFromIms() {
        // Stop the IMS connection
        stopImsConnection();

        // Registration terminated
        mCurrentNetworkInterface.registrationTerminated();

        // Disconnect from the network access
        mCurrentNetworkInterface.getNetworkAccess().disconnect();
    }

    /**
     * Start the IMS connection
     */
    private synchronized void startImsConnection() {
        if (mImsPollingThreadId >= 0) {
            // Already connected
            return;
        }

        // Set the connection flag
        if (logger.isActivated()) {
            logger.info("Start the IMS connection manager");
        }

        // Start background polling thread
        try {
            mImsPollingThread = new Thread(this);
            mImsPollingThreadId = mImsPollingThread.getId();
            mImsPollingThread.start();
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Internal exception while starting IMS polling thread", e);
            }
        }
    }

    /**
     * Stop the IMS connection
     */
    private synchronized void stopImsConnection() {
        if (mImsPollingThreadId == -1) {
            // Already disconnected
            return;
        }

        // Set the connection flag
        if (logger.isActivated()) {
            logger.info("Stop the IMS connection manager");
        }
        mImsPollingThreadId = -1;

        // Stop background polling thread
        try {
            mImsPollingThread.interrupt();
            mImsPollingThread = null;
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Internal exception while stopping IMS polling thread", e);
            }
        }

        // Stop IMS services
        if (mImsServicesStarted) {
            mImsModule.stopImsServices();
            mImsServicesStarted = false;
        }
    }

    /**
     * Background processing
     */
    public void run() {
        if (logger.isActivated()) {
            logger.debug("Start polling of the IMS connection");
        }

        int servicePollingPeriod = mRcsSettings.getImsServicePollingPeriod();
        int regBaseTime = mRcsSettings.getRegisterRetryBaseTime();
        int regMaxTime = mRcsSettings.getRegisterRetryMaxTime();
        Random random = new Random();
        int nbFailures = 0;

        while (mImsPollingThreadId == Thread.currentThread().getId()) {
            if (logger.isActivated()) {
                logger.debug("Polling: check IMS connection");
            }

            // Connection management
            try {
                // Test IMS registration
                if (!mCurrentNetworkInterface.isRegistered()) {
                    if (logger.isActivated()) {
                        logger.debug("Not yet registered to IMS: try registration");
                    }

                    // Try to register to IMS
                    if (mCurrentNetworkInterface.register(mDnsResolvedFields)) {
                        // InterruptedException thrown by stopImsConnection() may be caught by one
                        // of the methods used in currentNetworkInterface.register() above
                        if (mImsPollingThreadId != Thread.currentThread().getId()) {
                            if (logger.isActivated()) {
                                logger.debug("IMS connection polling thread race condition");
                            }
                            break;
                        } else {
                            if (logger.isActivated()) {
                                logger.debug("Registered to the IMS with success: start IMS services");
                            }
                            if (mImsModule.isInitializationFinished() && !mImsServicesStarted) {
                                mImsModule.startImsServices();
                                mImsServicesStarted = true;
                            }

                            // Reset number of failures
                            nbFailures = 0;
                        }
                    } else {
                        if (logger.isActivated()) {
                            logger.debug("Can't register to the IMS");
                        }

                        // Increment number of failures
                        nbFailures++;

                        // Force to perform a new DNS lookup
                        mDnsResolvedFields = null;
                    }
                } else {
                    if (mImsModule.isInitializationFinished()) {
                        if (!mImsServicesStarted) {
                            if (logger.isActivated()) {
                                logger.debug("Already registered to IMS: start IMS services");
                            }
                            mImsModule.startImsServices();
                            mImsServicesStarted = true;
                        } else {
                            if (logger.isActivated()) {
                                logger.debug("Already registered to IMS: check IMS services");
                            }
                            mImsModule.checkImsServices();
                        }
                    } else {
                        if (logger.isActivated()) {
                            logger.debug("Already registered to IMS: IMS services not yet started");
                        }
                    }
                }
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Internal exception", e);
                }
                // Force to perform a new DNS lookup
                mDnsResolvedFields = null;
            }

            // InterruptedException thrown by stopImsConnection() may be caught by one
            // of the methods used in currentNetworkInterface.register() above
            if (mImsPollingThreadId != Thread.currentThread().getId()) {
                logger.debug("IMS connection polling thread race condition");
                break;
            }

            // Make a pause before the next polling
            try {
                if (!mCurrentNetworkInterface.isRegistered()) {
                    // Pause before the next register attempt
                    double w = Math.min(regMaxTime, (regBaseTime * Math.pow(2, nbFailures)));
                    double coeff = (random.nextInt(51) + 50) / 100.0; // Coeff between 50% and 100%
                    int retryPeriod = (int) (coeff * w);
                    if (logger.isActivated()) {
                        logger.debug("Wait " + retryPeriod
                                + "s before retry registration (failures=" + nbFailures
                                + ", coeff=" + coeff + ")");
                    }
                    Thread.sleep(retryPeriod * 1000);
                } else if (!mImsServicesStarted) {
                    int retryPeriod = 5;
                    if (logger.isActivated()) {
                        logger.debug("Wait " + retryPeriod + "s before retry to start services");
                    }
                    Thread.sleep(retryPeriod * 1000);
                } else {
                    // Pause before the next service check
                    Thread.sleep(servicePollingPeriod * 1000);
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        if (logger.isActivated()) {
            logger.debug("IMS connection polling is terminated");
        }
    }

    /**
     * Battery level listener
     */
    private BroadcastReceiver batteryLevelListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MinimumBatteryLevel batteryLimit = mRcsSettings.getMinBatteryLevel();
            if (MinimumBatteryLevel.NEVER_STOP == batteryLimit) {
                mDisconnectedByBattery = false;
                return;

            }
            int batteryLevel = intent.getIntExtra("level", 0);
            int batteryPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 1);
            if (logger.isActivated()) {
                logger.info(new StringBuilder("Battery level: ").append(batteryLevel)
                        .append("% plugged: ").append(batteryPlugged)
                        .toString());
            }
            if (batteryLevel <= batteryLimit.toInt() && batteryPlugged == 0) {
                if (!mDisconnectedByBattery) {
                    mDisconnectedByBattery = true;

                    // Disconnect
                    disconnectFromIms();
                }
            } else {
                if (mDisconnectedByBattery) {
                    mDisconnectedByBattery = false;

                    // Reconnect with a connection event
                    connectionEvent(new Intent(ConnectivityManager.CONNECTIVITY_ACTION));
                }
            }
        }
    };

    /**
     * @return true is device is in roaming
     */
    public boolean isInRoaming() {
        if (mCnxManager != null && mCnxManager.getActiveNetworkInfo() != null) {
            return mCnxManager.getActiveNetworkInfo().isRoaming();
        }
        return false;
    }
}
