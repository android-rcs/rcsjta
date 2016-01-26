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

package com.gsma.rcs.core.ims.network;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.ImsNetworkInterface.DnsResolvedFields;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtpSource;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.platform.network.NetworkFactory;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.NetworkAccessType;
import com.gsma.rcs.service.LauncherUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.CommonServiceConfiguration.MinimumBatteryLevel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.telephony.TelephonyManager;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.util.Random;

/**
 * IMS connection manager
 * 
 * @author JM. Auffret
 * @author Deutsche Telekom
 */
public class ImsConnectionManager implements Runnable {

    private static final long DEFAULT_RETRY_PERIOD = 5000;

    private final Core mCore;

    private final ImsModule mImsModule;

    private ImsNetworkInterface[] mNetworkInterfaces = new ImsNetworkInterface[2];

    private ImsNetworkInterface mCurrentNetworkInterface;

    private Thread mImsPollingThread;

    private long mImsPollingThreadId = -1;

    private ConnectivityManager mCnxManager;

    private NetworkAccessType mNetwork;

    private String mOperator;

    private DnsResolvedFields mDnsResolvedFields;

    /**
     * Battery level state
     */
    private boolean mDisconnectedByBattery = false;

    private boolean mImsServicesStarted = false;

    private static final Logger sLogger = Logger.getLogger(ImsConnectionManager.class.getName());

    private final RcsSettings mRcsSettings;

    private final Context mCtx;

    private NetworkStateListener mNetworkStateListener;

    private BatteryLevelListener mBatteryLevelListener;

    /**
     * Constructor
     * 
     * @param imsModule The IMS module instance
     * @param ctx The application context
     * @param core The Core instance
     * @param rcsSettings RcsSettings instance
     */
    public ImsConnectionManager(ImsModule imsModule, Context ctx, Core core, RcsSettings rcsSettings) {
        mImsModule = imsModule;
        mCore = core;
        mRcsSettings = rcsSettings;
        mCtx = ctx;
    }

    /**
     * Initializes IMS connection
     */
    public void initialize() {
        mCnxManager = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetwork = mRcsSettings.getNetworkAccess();
        mOperator = mRcsSettings.getNetworkOperator();

        /* Instantiates the IMS network interfaces */
        mNetworkInterfaces[0] = new MobileNetworkInterface(mImsModule, mRcsSettings);
        mNetworkInterfaces[1] = new WifiNetworkInterface(mImsModule, mRcsSettings);

        /* Set the mobile network interface by default */
        mCurrentNetworkInterface = getMobileNetworkInterface();

        loadUserProfile();

        if (mNetworkStateListener == null) {
            /* Register network state listener */
            mNetworkStateListener = new NetworkStateListener();
            mCtx.registerReceiver(mNetworkStateListener, new IntentFilter(
                    ConnectivityManager.CONNECTIVITY_ACTION));
        }

        if (mBatteryLevelListener == null) {
            /* Register changes about battery: charging state, level, etc... */
            mBatteryLevelListener = new BatteryLevelListener();
            mCtx.registerReceiver(mBatteryLevelListener, new IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED));
        }
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
        ImsModule.setImsUserProfile(mCurrentNetworkInterface.getUserProfile());
        RtpSource.setCname(ImsModule.getImsUserProfile().getPublicUri());
        if (sLogger.isActivated()) {
            sLogger.debug("User profile has been reloaded");
        }
    }

    /**
     * Terminate the connection manager
     * 
     * @throws PayloadException
     * @throws NetworkException
     * @throws ContactManagerException
     */
    public void terminate() throws PayloadException, NetworkException, ContactManagerException {
        if (sLogger.isActivated()) {
            sLogger.info("Terminate the IMS connection manager");
        }
        if (mBatteryLevelListener != null) {
            mCtx.unregisterReceiver(mBatteryLevelListener);
            mBatteryLevelListener = null;
        }
        if (mNetworkStateListener != null) {
            mCtx.unregisterReceiver(mNetworkStateListener);
            mNetworkStateListener = null;
        }
        stopImsConnection(TerminationReason.TERMINATION_BY_SYSTEM);
        mCurrentNetworkInterface.unregister();
        if (sLogger.isActivated()) {
            sLogger.info("IMS connection manager has been terminated");
        }
    }

    /**
     * Network state listener
     */
    private class NetworkStateListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            mCore.scheduleCoreOperation(new Runnable() {
                @Override
                public void run() {

                    try {
                        connectionEvent(intent);
                    } catch (ContactManagerException e) {
                        sLogger.error(
                                new StringBuilder(
                                        "Unable to handle connection event for intent action : ")
                                        .append(intent.getAction()).toString(), e);
                    } catch (PayloadException e) {
                        sLogger.error(
                                new StringBuilder(
                                        "Unable to handle connection event for intent action : ")
                                        .append(intent.getAction()).toString(), e);
                    } catch (NetworkException e) {
                        if (sLogger.isActivated()) {
                            sLogger.debug(e.getMessage());
                        }
                    } catch (CertificateException e) {
                        sLogger.error(
                                new StringBuilder(
                                        "Unable to handle connection event for intent action : ")
                                        .append(intent.getAction()).toString(), e);
                    } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                        sLogger.error(
                                new StringBuilder(
                                        "Unable to handle connection event for intent action : ")
                                        .append(intent.getAction()).toString(), e);
                    }
                }
            });
        }
    }

    /**
     * Connection event
     * 
     * @param intent Intent
     * @throws PayloadException
     * @throws CertificateException
     * @throws NetworkException
     * @throws ContactManagerException
     */
    // @FIXME: This method is doing so many things at this moment and has become too complex thus
    // needs a complete refactor, However at this moment due to other prior tasks the refactoring
    // task has been kept in backlog.
    private void connectionEvent(Intent intent) throws PayloadException, CertificateException,
            NetworkException, ContactManagerException {
        try {
            if (mDisconnectedByBattery) {
                return;
            }

            if (!intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                return;
            }

            boolean connectivity = intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
            boolean failover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
            if (sLogger.isActivated()) {
                sLogger.debug("Connectivity event change: failover=" + failover + ", connectivity="
                        + !connectivity + ", reason=" + reason);
            }
            NetworkInfo networkInfo = mCnxManager.getActiveNetworkInfo();
            if (networkInfo == null) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Disconnect from IMS: no network (e.g. air plane mode)");
                }
                disconnectFromIms();
                return;
            }
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                String lastUserAccount = LauncherUtils.getLastUserAccount(mCtx);
                String currentUserAccount = LauncherUtils.getCurrentUserAccount(mCtx);
                if (lastUserAccount != null) {
                    if ((currentUserAccount == null)
                            || !currentUserAccount.equalsIgnoreCase(lastUserAccount)) {
                        mImsModule.getCoreListener().onSimChangeDetected();
                        return;
                    }
                }
            }
            String localIpAddr = null;
            if (networkInfo.getType() != mCurrentNetworkInterface.getType()) {
                if (sLogger.isActivated()) {
                    sLogger.info("Data connection state: NETWORK ACCESS CHANGED");
                }
                if (sLogger.isActivated()) {
                    sLogger.debug("Disconnect from IMS: network access has changed");
                }
                disconnectFromIms();

                if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Change the network interface to mobile");
                    }
                    mCurrentNetworkInterface = getMobileNetworkInterface();
                } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Change the network interface to Wi-Fi");
                    }
                    mCurrentNetworkInterface = getWifiNetworkInterface();
                }

                loadUserProfile();

                try {
                    mDnsResolvedFields = mCurrentNetworkInterface.getDnsResolvedFields();
                } catch (UnknownHostException e) {
                    /*
                     * Even if we are not able to resolve host name , we should still continue to
                     * get local IP as this is a very obvious case, Specially for networks
                     * supporting IPV4 protocol.
                     */
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }
                }
                localIpAddr = NetworkFactory.getFactory().getLocalIpAddress(mDnsResolvedFields,
                        networkInfo.getType());
            } else {
                /* Check if the IP address has changed */
                try {
                    if (mDnsResolvedFields == null) {
                        mDnsResolvedFields = mCurrentNetworkInterface.getDnsResolvedFields();
                    }
                } catch (UnknownHostException e) {
                    /*
                     * Even if we are not able to resolve host name , we should still continue to
                     * get local IP as this is a very obvious case, Specially for networks
                     * supporting IPV4 protocol.
                     */
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }
                }
                localIpAddr = NetworkFactory.getFactory().getLocalIpAddress(mDnsResolvedFields,
                        networkInfo.getType());
                String lastIpAddr = mCurrentNetworkInterface.getNetworkAccess().getIpAddress();
                if (!localIpAddr.equals(lastIpAddr)) {
                    // Changed by Deutsche Telekom
                    if (lastIpAddr != null) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Disconnect from IMS: IP address has changed");
                        }
                        disconnectFromIms();
                    } else {
                        if (sLogger.isActivated()) {
                            sLogger.debug("IP address available (again)");
                        }
                    }
                } else {
                    // Changed by Deutsche Telekom
                    if (sLogger.isActivated()) {
                        sLogger.debug("Neither interface nor IP address has changed; nothing to do.");
                    }
                    return;
                }
            }
            if (networkInfo.isConnected()) {
                String remoteAddress;
                if (mDnsResolvedFields != null) {
                    remoteAddress = mDnsResolvedFields.mIpAddress;
                } else {
                    remoteAddress = new String("unresolved");
                }

                if (sLogger.isActivated()) {
                    sLogger.info("Data connection state: CONNECTED to " + networkInfo.getTypeName()
                            + " with local IP " + localIpAddr + " valid for " + remoteAddress);
                }

                if (!NetworkAccessType.ANY.equals(mNetwork)
                        && (mNetwork.toInt() != networkInfo.getType())) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("Network access " + networkInfo.getTypeName()
                                + " is not authorized");
                    }
                    return;
                }

                TelephonyManager tm = (TelephonyManager) mCtx
                        .getSystemService(Context.TELEPHONY_SERVICE);
                String currentOpe = tm.getSimOperatorName();
                if (mOperator != null && !currentOpe.equalsIgnoreCase(mOperator)) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("Operator not authorized current=" + currentOpe
                                + " authorized=" + mOperator);
                    }
                    return;
                }

                if (!mCurrentNetworkInterface.isInterfaceConfigured()) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("IMS network interface not well configured");
                    }
                    return;
                }

                if (sLogger.isActivated()) {
                    sLogger.debug("Connect to IMS");
                }
                connectToIms(localIpAddr);
            }
        } catch (SocketException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            disconnectFromIms();
        } catch (IOException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            disconnectFromIms();
        }
    }

    /**
     * Connect to IMS network interface
     * 
     * @param ipAddr IP address
     * @throws CertificateException
     * @throws IOException
     */
    private void connectToIms(String ipAddr) throws CertificateException, IOException {
        // Connected to the network access
        mCurrentNetworkInterface.getNetworkAccess().connect(ipAddr);

        // Start the IMS connection
        startImsConnection();
    }

    /**
     * Disconnect from IMS network interface
     * 
     * @throws PayloadException
     * @throws NetworkException
     * @throws ContactManagerException
     */
    private void disconnectFromIms() throws PayloadException, NetworkException,
            ContactManagerException {
        // Stop the IMS connection
        stopImsConnection(TerminationReason.TERMINATION_BY_CONNECTION_LOST);

        // Registration terminated
        mCurrentNetworkInterface.registrationTerminated();

        // Disconnect from the network access
        mCurrentNetworkInterface.getNetworkAccess().disconnect();
    }

    /**
     * Disconnect from IMS network interface and de-register due to battery low
     * 
     * @throws PayloadException
     * @throws NetworkException
     * @throws ContactManagerException
     */
    private void disconnectFromImsByBatteryLow() throws PayloadException, NetworkException,
            ContactManagerException {
        if (sLogger.isActivated()) {
            sLogger.debug("Disconnect from IMS network interface and de-register due to battery low");
        }
        stopImsConnection(TerminationReason.TERMINATION_BY_LOW_BATTERY);
        mCurrentNetworkInterface.unregister();
        mCurrentNetworkInterface.getNetworkAccess().disconnect();
    }

    /**
     * Start the IMS connection
     */
    private synchronized void startImsConnection() {
        if (mImsPollingThreadId >= 0) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.info("Start the IMS connection manager");
        }
        mImsPollingThread = new Thread(this);
        mImsPollingThreadId = mImsPollingThread.getId();
        mImsPollingThread.start();
    }

    /**
     * Stop the IMS connection
     * 
     * @throws PayloadException
     * @throws NetworkException
     * @throws ContactManagerException
     */
    private synchronized void stopImsConnection(TerminationReason reasonCode)
            throws PayloadException, NetworkException, ContactManagerException {
        if (mImsPollingThreadId == -1) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.info("Stop the IMS connection manager");
        }
        mImsPollingThreadId = -1;
        mImsPollingThread.interrupt();
        mImsPollingThread = null;

        if (mImsServicesStarted) {
            mImsModule.stopImsServices(reasonCode);
            mImsServicesStarted = false;
        }
    }

    @Override
    // @FIXME: This run method needs to be refactored as the current logic of polling is bit too
    // complex and can be made much more simpler.
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.debug("Start polling of the IMS connection");
            }

            long servicePollingPeriod = mRcsSettings.getImsServicePollingPeriod();
            long regBaseTime = mRcsSettings.getRegisterRetryBaseTime();
            long regMaxTime = mRcsSettings.getRegisterRetryMaxTime();
            Random random = new Random();
            int nbFailures = 0;

            while (mImsPollingThreadId == Thread.currentThread().getId()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Polling: check IMS connection");
                }

                // Connection management
                try {
                    // Test IMS registration
                    if (!mCurrentNetworkInterface.isRegistered()) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Not yet registered to IMS: try registration");
                        }

                        // Try to register to IMS
                        mCurrentNetworkInterface.register(mDnsResolvedFields);

                        // InterruptedException thrown by stopImsConnection() may be caught by one
                        // of the methods used in currentNetworkInterface.register() above
                        if (mImsPollingThreadId != Thread.currentThread().getId()) {
                            if (sLogger.isActivated()) {
                                sLogger.debug("IMS connection polling thread race condition");
                            }
                            break;
                        }

                        if (mImsModule.isInitializationFinished() && !mImsServicesStarted) {
                            if (sLogger.isActivated()) {
                                sLogger.debug("Registered to the IMS with success: start IMS services");
                            }
                            mImsModule.startImsServices();
                            mImsServicesStarted = true;
                        }

                        // Reset number of failures
                        nbFailures = 0;
                    } else {
                        if (mImsModule.isInitializationFinished()) {
                            if (!mImsServicesStarted) {
                                if (sLogger.isActivated()) {
                                    sLogger.debug("Already registered to IMS: start IMS services");
                                }
                                mImsModule.startImsServices();
                                mImsServicesStarted = true;
                            } else {
                                if (sLogger.isActivated()) {
                                    sLogger.debug("Already registered to IMS: check IMS services");
                                }
                                mImsModule.checkImsServices();
                            }
                        } else {
                            if (sLogger.isActivated()) {
                                sLogger.debug("Already registered to IMS: IMS services not yet started");
                            }
                        }
                    }
                } catch (ContactManagerException e) {
                    sLogger.error("Can't register to the IMS!", e);
                    mCurrentNetworkInterface.getSipManager().closeStack();
                    /* Increment number of failures */
                    nbFailures++;
                    /* Force to perform a new DNS lookup */
                    mDnsResolvedFields = null;
                } catch (PayloadException e) {
                    sLogger.error("Can't register to the IMS!", e);
                    mCurrentNetworkInterface.getSipManager().closeStack();
                    /* Increment number of failures */
                    nbFailures++;
                    /* Force to perform a new DNS lookup */
                    mDnsResolvedFields = null;
                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }
                    mCurrentNetworkInterface.getSipManager().closeStack();
                    /* Increment number of failures */
                    nbFailures++;
                    /* Force to perform a new DNS lookup */
                    mDnsResolvedFields = null;
                }

                // InterruptedException thrown by stopImsConnection() may be caught by one
                // of the methods used in currentNetworkInterface.register() above
                if (mImsPollingThreadId != Thread.currentThread().getId()) {
                    sLogger.debug("IMS connection polling thread race condition");
                    break;
                }

                // Make a pause before the next polling
                try {
                    if (!mCurrentNetworkInterface.isRegistered()) {
                        final long retryAfterHeaderDuration = mCurrentNetworkInterface
                                .getRetryAfterHeaderDuration();
                        if (retryAfterHeaderDuration > 0) {
                            Thread.sleep(retryAfterHeaderDuration);
                        } else {
                            // Pause before the next register attempt
                            double w = Math
                                    .min(regMaxTime, (regBaseTime * Math.pow(2, nbFailures)));
                            double coeff = (random.nextInt(51) + 50) / 100.0; // Coeff between 50%
                                                                              // and
                                                                              // 100%
                            long retryPeriod = (long) (coeff * w);
                            if (sLogger.isActivated()) {
                                sLogger.debug(new StringBuilder("Wait ").append(retryPeriod)
                                        .append("ms before retry registration (failures=")
                                        .append(nbFailures).append(", coeff=").append(coeff)
                                        .append(')').toString());
                            }
                            Thread.sleep(retryPeriod);
                        }
                    } else if (!mImsServicesStarted) {
                        if (sLogger.isActivated()) {
                            sLogger.debug(new StringBuilder("Wait ").append(DEFAULT_RETRY_PERIOD)
                                    .append("ms before retry to start services").toString());
                        }
                        Thread.sleep(DEFAULT_RETRY_PERIOD);
                    } else {
                        // Pause before the next service check
                        Thread.sleep(servicePollingPeriod);
                    }
                } catch (InterruptedException e) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("IMS connection polling is interrupted", e);
                    }
                    break;
                }
            }
            if (sLogger.isActivated()) {
                sLogger.debug("IMS connection polling is terminated");
            }
        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to poll for ims connection!", e);
        }
    }

    /**
     * Battery level listener class
     */
    private class BatteryLevelListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            mCore.scheduleCoreOperation(new Runnable() {
                @Override
                public void run() {
                    try {
                        MinimumBatteryLevel batteryLimit = mRcsSettings.getMinBatteryLevel();
                        if (MinimumBatteryLevel.NEVER_STOP == batteryLimit) {
                            mDisconnectedByBattery = false;
                            return;

                        }
                        int batteryLevel = intent.getIntExtra("level", 0);
                        int batteryPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 1);
                        if (sLogger.isActivated()) {
                            sLogger.info(new StringBuilder("Battery level: ").append(batteryLevel)
                                    .append("% plugged: ").append(batteryPlugged).toString());
                        }
                        if (batteryLevel <= batteryLimit.toInt() && batteryPlugged == 0) {
                            if (!mDisconnectedByBattery) {
                                mDisconnectedByBattery = true;

                                disconnectFromImsByBatteryLow();
                            }
                        } else {
                            if (mDisconnectedByBattery) {
                                mDisconnectedByBattery = false;

                                // Reconnect with a connection event
                                connectionEvent(new Intent(ConnectivityManager.CONNECTIVITY_ACTION));
                            }
                        }
                    } catch (ContactManagerException e) {
                        sLogger.error(
                                new StringBuilder(
                                        "Unable to handle connection event for intent action : ")
                                        .append(intent.getAction()).toString(), e);
                    } catch (PayloadException e) {
                        sLogger.error(
                                new StringBuilder(
                                        "Unable to handle connection event for intent action : ")
                                        .append(intent.getAction()).toString(), e);
                    } catch (NetworkException e) {
                        if (sLogger.isActivated()) {
                            sLogger.debug(e.getMessage());
                        }
                    } catch (CertificateException e) {
                        sLogger.error(
                                new StringBuilder(
                                        "Unable to handle connection event for intent action : ")
                                        .append(intent.getAction()).toString(), e);
                    } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                        sLogger.error(
                                new StringBuilder(
                                        "Unable to handle connection event for intent action : ")
                                        .append(intent.getAction()).toString(), e);
                    }
                }
            });
        }
    }

    /**
     * @return true is device is in roaming
     */
    public boolean isInRoaming() {
        NetworkInfo networkInfo = mCnxManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isRoaming();
        }
        return false;
    }
}
