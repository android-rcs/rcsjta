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

package com.orangelabs.rcs.service;

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

import com.gsma.services.rcs.RcsService;
import com.orangelabs.rcs.R;
import com.orangelabs.rcs.addressbook.AccountChangedReceiver;
import com.orangelabs.rcs.addressbook.AuthenticationService;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.registry.AndroidRegistryFactory;
import com.orangelabs.rcs.provider.BackupRestoreDb;
import com.orangelabs.rcs.provider.LocalContentResolver;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.ConfigurationMode;
import com.orangelabs.rcs.provisioning.ProvisioningInfo;
import com.orangelabs.rcs.provisioning.https.HttpsProvisioningService;
import com.orangelabs.rcs.utils.IntentUtils;
import com.orangelabs.rcs.utils.logger.Logger;

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
    private ConnectivityManager connMgr = null;

    /**
     * Network state listener
     */
    private BroadcastReceiver networkStateListener = null;

    /**
     * Last User account
     */
    private String lastUserAccount = null;

    /**
     * Current User account
     */
    private String currentUserAccount = null;

    /**
     * Launch boot flag
     */
	boolean boot = false;
	
	 /**
     * Launch user flag
     */
	boolean user = false;
	
    /**
     * The logger
     */
    private static Logger logger = Logger.getLogger(StartService.class.getSimpleName());
    
    private static final String INTENT_KEY_BOOT = "boot";
    private static final String INTENT_KEY_USER = "user";


    @Override
    public void onCreate() {
        // Instantiate RcsSettings
        Context ctx = getApplicationContext();
        mLocalContentResolver = new LocalContentResolver(ctx.getContentResolver());
        RcsSettings.createInstance(getApplicationContext());
        ConfigurationMode mode = RcsSettings.getInstance().getConfigurationMode();
    	if (logger.isActivated()) {
    		logger.debug("onCreate ConfigurationMode="+mode);
        }
        // In manual configuration, use a network listener to start RCS core when the data will be ON 
    	if (ConfigurationMode.MANUAL.equals(mode)) {
        	registerNetworkStateListener();
        }
    }

    @Override
    public void onDestroy() {
        // Unregister network state listener
        if (networkStateListener != null) {
        	try {
	            unregisterReceiver(networkStateListener);
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
		if (logger.isActivated()) {
			logger.debug("Start RCS service");
		}
		new Thread() {
			@Override
			public void run() {
				// Check boot
				if (intent != null) {
					boot = intent.getBooleanExtra(INTENT_KEY_BOOT, false);
					user = intent.getBooleanExtra(INTENT_KEY_USER, false);
				}
				if (checkAccount(mLocalContentResolver)) {
					launchRcsService(boot, user);
				} else {
					// User account can't be initialized (no radio to read IMSI, .etc)
					if (logger.isActivated()) {
						logger.error("Can't create the user account");
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
        if (connMgr == null) {
            connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        // Instantiate the network listener
        networkStateListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                new Thread() {
                    public void run() {
                        connectionEvent(intent.getAction());
                    }
                }.start();
            }
        };
        // Register network state listener
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateListener, intentFilter);
    }

    /**
     * Connection event
     *
     * @param action Connectivity action
     */
    private void connectionEvent(String action) {
        if (logger.isActivated()) {
            logger.debug("Connection event " + action);
        }
        // Try to start the service only if a data connectivity is available
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if ((networkInfo != null) && networkInfo.isConnected()) {
                if (logger.isActivated()) {
                    logger.debug("Device connected - Launch RCS service");
                }
                
                // Start the RCS core service
                LauncherUtils.launchRcsCoreService(getApplicationContext());
                
                // Stop Network listener
                if (networkStateListener != null) {
                	try {
	                	unregisterReceiver(networkStateListener);
	    	        } catch (IllegalArgumentException e) {
	    	        	// Nothing to do
	    	        }
                	networkStateListener = null;
                }
            }
        }
    }

	private void broadcastServiceProvisioned() {
		Intent serviceProvisioned = new Intent(RcsService.ACTION_SERVICE_PROVISIONED);
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
        AndroidFactory.setApplicationContext(ctx);
        
        // Read the current and last end user account
        currentUserAccount = LauncherUtils.getCurrentUserAccount(ctx);
        lastUserAccount = LauncherUtils.getLastUserAccount(ctx);
        if (logger.isActivated()) {
            logger.info("Last user account is " + lastUserAccount);
            logger.info("Current user account is " + currentUserAccount);
        }

        // Check the current SIM
        if (currentUserAccount == null) {
            if (isFirstLaunch()) {
                // If it's a first launch the IMSI is necessary to initialize the service the first time
                return false;
            } else {
                // Set the user account ID from the last used IMSI
                currentUserAccount = lastUserAccount;
            }
        }

        // On the first launch and if SIM card has changed
        if (isFirstLaunch()) {
            // Set new user flag
            setNewUserAccount(true);
        } else
        if (hasChangedAccount()) {
        	// keep a maximum of saved accounts
    		BackupRestoreDb.cleanBackups(currentUserAccount);
        	// Backup last account settings
        	if (lastUserAccount != null) {
        		if (logger.isActivated()) {
        			logger.info("Backup " + lastUserAccount);
        		}
        		BackupRestoreDb.backupAccount(lastUserAccount);
        	}
        	
            // Reset RCS account 
            LauncherUtils.resetRcsConfig(ctx, mLocalContentResolver);

            // Restore current account settings
    		if (logger.isActivated()) {
    			logger.info("Restore " + currentUserAccount);
    		}
    		BackupRestoreDb.restoreAccount(currentUserAccount);
    		// Send service provisioned intent as the configuration settings
    		// are now loaded by means of restoring previous values that were backed
    		// up during SIM Swap.
    		broadcastServiceProvisioned();

            // Activate service if new account
            RcsSettings.getInstance().setServiceActivationState(true);

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
            if (logger.isActivated()) {
                logger.debug("The RCS account does not exist");
            }
            if (AccountChangedReceiver.isAccountResetByEndUser()) {
                // It was manually destroyed by the user
                if (logger.isActivated()) {
                    logger.debug("It was manually destroyed by the user, we do not recreate it");
                }
                return false;
            } else {
                if (logger.isActivated()) {
                    logger.debug("Recreate a new RCS account");
                }
                AuthenticationService.createRcsAccount(ctx, localContentResolver,
                        getString(R.string.rcs_core_account_username), true);
            }
        } else {
            // Account exists: checks if it has changed
            if (hasChangedAccount()) {
                // Account has changed (i.e. new SIM card): delete the current account and create a new one
                if (logger.isActivated()) {
                    logger.debug("Deleting the old RCS account for " + lastUserAccount);
                }
                ContentResolver contentResolver = ctx.getContentResolver();
                ContactsManager.createInstance(ctx, contentResolver, localContentResolver);
                ContactsManager.getInstance().deleteRCSEntries();
                AuthenticationService.removeRcsAccount(ctx, null);
    
                if (logger.isActivated()) {
                    logger.debug("Creating a new RCS account for " + currentUserAccount);
                }
                AuthenticationService.createRcsAccount(ctx, localContentResolver,
                        getString(R.string.rcs_core_account_username), true);
            }
        }

        // Save the current end user account
        LauncherUtils.setLastUserAccount(ctx, currentUserAccount);

        return true;
    }

    /**
     * Launch the RCS service.
     *
     * @param boot indicates if RCS is launched from the device boot
     * @param user indicates if RCS is launched from the user interface
     */
	private void launchRcsService(boolean boot, boolean user) {
		ConfigurationMode mode = RcsSettings.getInstance().getConfigurationMode();

		if (logger.isActivated())
			logger.debug("Launch RCS service: HTTPS=" + mode + ", boot=" + boot + ", user=" + user);

		if (ConfigurationMode.AUTO.equals(mode)) {
			// HTTPS auto config
			String version = RcsSettings.getInstance().getProvisioningVersion();
			// Check the last provisioning version
			if (ProvisioningInfo.Version.RESETED_NOQUERY.equals(version)) {
				// (-1) : RCS service is permanently disabled. SIM change is required
				if (hasChangedAccount()) {
					// Start provisioning as a first launch
					HttpsProvisioningService.startHttpsProvisioningService(getApplicationContext(), true, user);
				} else {
					if (logger.isActivated()) {
						logger.debug("Provisioning is blocked with this account");
					}
				}
			} else {
				if (isFirstLaunch() || hasChangedAccount()) {
					// First launch: start the auto config service with special tag
					HttpsProvisioningService.startHttpsProvisioningService(getApplicationContext(), true, user);
				} else {
					if (ProvisioningInfo.Version.DISABLED_NOQUERY.equals(version)) {
						// -2 : RCS client and configuration query is disabled
						if (user) {
							// Only start query if requested by user action
							HttpsProvisioningService.startHttpsProvisioningService(getApplicationContext(), false, user);
						}
					} else {
						// Start or restart the HTTP provisioning service
						HttpsProvisioningService.startHttpsProvisioningService(getApplicationContext(), false, user);
						if (ProvisioningInfo.Version.DISABLED_DORMANT.equals(version)) {
							// -3 : RCS client is disabled but configuration query is not
						} else {
							// Start the RCS core service
							LauncherUtils.launchRcsCoreService(getApplicationContext());
						}
					}
				}
			}
		} else {
			// No auto config: directly start the RCS core service
			LauncherUtils.launchRcsCoreService(getApplicationContext());
		}
	}

    /**
     * Is the first RCs is launched ?
     *
     * @return true if it's the first time RCS is launched
     */
    private boolean isFirstLaunch() {
        return (lastUserAccount == null);
    }

    /**
     * Check if RCS account has changed since the last time we started the service
     *
     * @return true if the active account was changed
     */
    private boolean hasChangedAccount() {
        if (lastUserAccount == null) {
            return true;
        } else
        if (currentUserAccount == null) {
            return false;
        } else {
            return (!currentUserAccount.equalsIgnoreCase(lastUserAccount));
        }
    }

    /**
     * Set true if new user account
     *
     * @param value true if new user account
     */
    private void setNewUserAccount(boolean value) {
        SharedPreferences preferences = getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
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
        SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        return preferences.getBoolean(REGISTRY_NEW_USER_ACCOUNT, false);
    }
    
	/**
	 * Launch the RCS start service
	 * 
	 * @param context
	 * @param boot
	 *            start RCS service upon boot
	 * @param user
	 *            start RCS service upon user action
	 */
	static void LaunchRcsStartService(Context context, boolean boot, boolean user) {
		if (logger.isActivated())
			logger.debug("Launch RCS service (boot=" + boot + ") (user="+user+")");
		Intent intent = new Intent(context, StartService.class);
		intent.putExtra(INTENT_KEY_BOOT, boot);
		intent.putExtra(INTENT_KEY_USER, user);
		context.startService(intent);
	}
}