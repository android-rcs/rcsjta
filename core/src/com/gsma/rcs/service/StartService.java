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

package com.gsma.rcs.service;

import com.gsma.rcs.R;
import com.gsma.rcs.addressbook.AccountChangedReceiver;
import com.gsma.rcs.addressbook.AuthenticationService;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.platform.registry.AndroidRegistryFactory;
import com.gsma.rcs.provider.BackupRestoreDb;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.ConfigurationMode;
import com.gsma.rcs.provisioning.ProvisioningInfo;
import com.gsma.rcs.provisioning.https.HttpsProvisioningService;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService;

import android.accounts.Account;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

/**
 * RCS start service.
 * 
 * @author hlxn7157
 */
public class StartService extends Service {

    /**
     * RCS new user account
     */
    public static final String REGISTRY_NEW_USER_ACCOUNT = "NewUserAccount";

    private LocalContentResolver mLocalContentResolver;

    /**
     * Connection manager
     */
    private ConnectivityManager mConnMgr;

    /**
     * Network state listener
     */
    private BroadcastReceiver mNetworkStateListener;

    /**
     * Last User account
     */
    private String mLastUserAccount;

    /**
     * Current User account
     */
    private String mCurrentUserAccount;

    /**
     * Launch boot flag
     */
    boolean mBoot = false;

    /**
     * Launch user flag
     */
    boolean mUser = false;

    private RcsSettings mRcsSettings;

    private MessagingLog mMessagingLog;

    private ContactsManager mContactManager;

    private static final Logger sLogger = Logger.getLogger(StartService.class.getSimpleName());

    private static final String INTENT_KEY_BOOT = "boot";
    private static final String INTENT_KEY_USER = "user";

    @Override
    public void onCreate() {
        Context context = getApplicationContext();
        ContentResolver contentResolver = context.getContentResolver();
        mLocalContentResolver = new LocalContentResolver(context);
        mRcsSettings = RcsSettings.createInstance(mLocalContentResolver);
        mMessagingLog = MessagingLog.createInstance(context, mLocalContentResolver, mRcsSettings);

        mContactManager = ContactsManager.createInstance(context, contentResolver,
                mLocalContentResolver, mRcsSettings);

        ConfigurationMode mode = mRcsSettings.getConfigurationMode();
        if (sLogger.isActivated()) {
            sLogger.debug("onCreate ConfigurationMode=".concat(mode.toString()));
        }
        // In manual configuration, use a network listener to start RCS core when the data will be
        // ON
        if (ConfigurationMode.MANUAL.equals(mode)) {
            registerNetworkStateListener();
        }
    }

    @Override
    public void onDestroy() {
        // Unregister network state listener
        if (mNetworkStateListener != null) {
            try {
                unregisterReceiver(mNetworkStateListener);
            } catch (IllegalArgumentException e) {
                // Nothing to do
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Start RCS service");
        }
        new Thread() {
            @Override
            public void run() {
                // Check boot
                if (intent != null) {
                    mBoot = intent.getBooleanExtra(INTENT_KEY_BOOT, false);
                    mUser = intent.getBooleanExtra(INTENT_KEY_USER, false);
                }
                if (checkAccount(mLocalContentResolver)) {
                    launchRcsService(mBoot, mUser);
                } else {
                    // User account can't be initialized (no radio to read IMSI, .etc)
                    if (sLogger.isActivated()) {
                        sLogger.error("Can't create the user account");
                    }
                    // Exit service
                    stopSelf();
                }
            }
        }.start();

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    /**
     * Register a broadcast receiver for network state changes
     */
    private void registerNetworkStateListener() {
        // Get connectivity manager
        if (mConnMgr == null) {
            mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        // Instantiate the network listener
        mNetworkStateListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                if (intent == null) {
                    return;
                }
                final String action = intent.getAction();
                if (action == null) {
                    return;
                }
                new Thread() {
                    public void run() {
                        connectionEvent(action);
                    }
                }.start();
            }
        };
        // Register network state listener
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkStateListener, intentFilter);
    }

    /**
     * Connection event
     * 
     * @param action Connectivity action
     */
    private void connectionEvent(String action) {
        // Try to start the service only if a data connectivity is available
        if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            return;
        }
        NetworkInfo networkInfo = mConnMgr.getActiveNetworkInfo();
        if (networkInfo == null) {
            return;
        }
        if (!networkInfo.isConnected()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Device disconnected");
            }
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Device connected - Launch RCS service");
        }

        // Start the RCS core service
        LauncherUtils.launchRcsCoreService(getApplicationContext(), mRcsSettings);

        // Stop Network listener
        if (mNetworkStateListener == null) {
            return;
        }
        try {
            unregisterReceiver(mNetworkStateListener);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
        mNetworkStateListener = null;
    }

    private void broadcastServiceProvisioned() {
        Intent serviceProvisioned = new Intent(RcsService.ACTION_SERVICE_PROVISIONING_DATA_CHANGED);
        IntentUtils.tryToSetReceiverForegroundFlag(serviceProvisioned);
        getApplicationContext().sendBroadcast(serviceProvisioned);
    }

    /**
     * Check account
     * 
     * @return true if an account is available
     */
    private boolean checkAccount(LocalContentResolver localContentResolver) {
        Context ctx = getApplicationContext();
        AndroidFactory.setApplicationContext(ctx, mRcsSettings);

        // Read the current and last end user account
        mCurrentUserAccount = LauncherUtils.getCurrentUserAccount(ctx);
        mLastUserAccount = LauncherUtils.getLastUserAccount(ctx);

        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            // Use StringBuilder since argument may be null
            sLogger.info(new StringBuilder("Last user account is ").append(mLastUserAccount)
                    .toString());
            sLogger.info(new StringBuilder("Current user account is ").append(mCurrentUserAccount)
                    .toString());
        }

        // Check the current SIM
        if (mCurrentUserAccount == null) {
            if (isFirstLaunch()) {
                // If it's a first launch the IMSI is necessary to initialize the service the first
                // time
                return false;
            } else {
                // Set the user account ID from the last used IMSI
                mCurrentUserAccount = mLastUserAccount;
            }
        }

        // On the first launch and if SIM card has changed
        if (isFirstLaunch()) {
            // Set new user flag
            setNewUserAccount(true);
        } else if (hasChangedAccount()) {
            // keep a maximum of saved accounts
            BackupRestoreDb.cleanBackups(mCurrentUserAccount);
            // Backup last account settings
            if (mLastUserAccount != null) {
                if (logActivated) {
                    sLogger.info("Backup ".concat(mLastUserAccount));
                }
                BackupRestoreDb.backupAccount(mLastUserAccount);
            }

            // Reset RCS account
            LauncherUtils.resetRcsConfig(ctx, mLocalContentResolver, mRcsSettings, mMessagingLog,
                    mContactManager);

            // Restore current account settings
            if (logActivated) {
                sLogger.info("Restore ".concat(mCurrentUserAccount));
            }
            BackupRestoreDb.restoreAccount(mCurrentUserAccount);
            // Send service provisioned intent as the configuration settings
            // are now loaded by means of restoring previous values that were backed
            // up during SIM Swap.
            broadcastServiceProvisioned();

            // Activate service if new account
            mRcsSettings.setServiceActivationState(true);

            // Set new user flag
            setNewUserAccount(true);
        } else {
            // Set new user flag
            setNewUserAccount(false);
        }

        // Check if the RCS account exists
        Account account = AuthenticationService.getAccount(ctx,
                getString(R.string.rcs_core_account_username));
        if (account == null) {
            // No account exists
            if (logActivated) {
                sLogger.debug("The RCS account does not exist");
            }
            if (AccountChangedReceiver.isAccountResetByEndUser()) {
                // It was manually destroyed by the user
                if (logActivated) {
                    sLogger.debug("It was manually destroyed by the user, we do not recreate it");
                }
                return false;
            } else {
                if (logActivated) {
                    sLogger.debug("Recreate a new RCS account");
                }
                AuthenticationService.createRcsAccount(ctx, localContentResolver,
                        getString(R.string.rcs_core_account_username), true, mRcsSettings,
                        mContactManager);
            }
        } else if (hasChangedAccount()) {
            // Account has changed (i.e. new SIM card): delete the current account and create a
            // new one
            if (logActivated) {
                sLogger.debug(new StringBuilder("Deleting the old RCS account for ").append(
                        mLastUserAccount).toString());
            }
            mContactManager.deleteRCSEntries();
            AuthenticationService.removeRcsAccount(ctx, null);

            if (logActivated) {
                sLogger.debug(new StringBuilder("Creating a new RCS account for ").append(
                        mCurrentUserAccount).toString());
            }
            AuthenticationService.createRcsAccount(ctx, localContentResolver,
                    getString(R.string.rcs_core_account_username), true, mRcsSettings,
                    mContactManager);
        }

        // Save the current end user account
        LauncherUtils.setLastUserAccount(ctx, mCurrentUserAccount);

        return true;
    }

    /**
     * Launch the RCS service.
     * 
     * @param boot indicates if RCS is launched from the device boot
     * @param user indicates if RCS is launched from the user interface
     */
    private void launchRcsService(boolean boot, boolean user) {
        ConfigurationMode mode = mRcsSettings.getConfigurationMode();
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug(new StringBuilder("Launch RCS service: HTTPS=").append(mode)
                    .append(", boot=").append(boot).append(", user=").append(user).toString());
        }

        Context context = getApplicationContext();

        if (!ConfigurationMode.AUTO.equals(mode)) {
            // Manual provisioning: accept terms and conditions
            mRcsSettings.setProvisioningTermsAccepted(true);
            // No auto config: directly start the RCS core service
            LauncherUtils.launchRcsCoreService(context, mRcsSettings);
            return;
        }
        // HTTPS auto config
        String version = mRcsSettings.getProvisioningVersion();
        // Check the last provisioning version
        if (ProvisioningInfo.Version.RESETED_NOQUERY.equals(version)) {
            // (-1) : RCS service is permanently disabled. SIM change is required
            if (hasChangedAccount()) {
                // Start provisioning as a first launch
                HttpsProvisioningService.startHttpsProvisioningService(context, true, user);
            } else {
                if (logActivated) {
                    sLogger.debug("Provisioning is blocked with this account");
                }
            }

        } else if (isFirstLaunch() || hasChangedAccount()) {
            // First launch: start the auto config service with special tag
            HttpsProvisioningService.startHttpsProvisioningService(context, true, user);

        } else if (ProvisioningInfo.Version.DISABLED_NOQUERY.equals(version)) {
            // -2 : RCS client and configuration query is disabled
            if (user) {
                // Only start query if requested by user action
                HttpsProvisioningService.startHttpsProvisioningService(context, false, user);
            }

        } else {
            // Start or restart the HTTP provisioning service
            HttpsProvisioningService.startHttpsProvisioningService(context, false, user);
            if (ProvisioningInfo.Version.DISABLED_DORMANT.equals(version)) {
                // -3 : RCS client is disabled but configuration query is not
            } else {
                // Start the RCS core service
                LauncherUtils.launchRcsCoreService(context, mRcsSettings);
            }
        }
    }

    /**
     * Is the first RCs is launched ?
     * 
     * @return true if it's the first time RCS is launched
     */
    private boolean isFirstLaunch() {
        return mLastUserAccount == null;
    }

    /**
     * Check if RCS account has changed since the last time we started the service
     * 
     * @return true if the active account was changed
     */
    private boolean hasChangedAccount() {
        if (mLastUserAccount == null) {
            return true;

        } else if (mCurrentUserAccount == null) {
            return false;

        } else {
            return (!mCurrentUserAccount.equalsIgnoreCase(mLastUserAccount));
        }
    }

    /**
     * Set true if new user account
     * 
     * @param value true if new user account
     */
    private void setNewUserAccount(boolean value) {
        SharedPreferences preferences = getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME,
                Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(REGISTRY_NEW_USER_ACCOUNT, value);
        editor.commit();
    }

    /**
     * Check if new user account
     * 
     * @param context Application context
     * @return true if new user account
     */
    public static boolean getNewUserAccount(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        return preferences.getBoolean(REGISTRY_NEW_USER_ACCOUNT, false);
    }

    /**
     * Launch the RCS start service
     * 
     * @param context
     * @param boot start RCS service upon boot
     * @param user start RCS service upon user action
     */
    static void LaunchRcsStartService(Context context, boolean boot, boolean user) {
        if (sLogger.isActivated())
            sLogger.debug(new StringBuilder("Launch RCS service (boot=").append(boot)
                    .append(") (user=").append(user).append(")").toString());
        Intent intent = new Intent(context, StartService.class);
        intent.putExtra(INTENT_KEY_BOOT, boot);
        intent.putExtra(INTENT_KEY_USER, user);
        context.startService(intent);
    }
}
