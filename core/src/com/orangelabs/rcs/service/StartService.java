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

package com.orangelabs.rcs.service;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.addressbook.AccountChangedReceiver;
import com.orangelabs.rcs.addressbook.AuthenticationService;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.registry.AndroidRegistryFactory;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData;
import com.orangelabs.rcs.provisioning.https.HttpsProvisioningService;
import com.orangelabs.rcs.service.api.client.ClientApiIntents;
import com.orangelabs.rcs.service.api.client.ClientApiUtils;
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
    private boolean boot = false;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    @Override
    public void onCreate() {
        // Instantiate RcsSettings
        RcsSettings.createInstance(getApplicationContext());

        // Use a network listener to start RCS Core when the data will be ON 
        if (RcsSettings.getInstance().getAutoConfigMode() == RcsSettingsData.NO_AUTO_CONFIG) {
            // Get connectivity manager
            if (connMgr == null) {
                connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            }
            
            // Instantiate the network listener
	        networkStateListener = new BroadcastReceiver() {
	            @Override
	            public void onReceive(Context context, final Intent intent) {
	                Thread t = new Thread() {
	                    public void run() {
	                        connectionEvent(intent.getAction());
	                    }
	                };
	                t.start();
	            }
	        };
	
	        // Register network state listener
	        IntentFilter intentFilter = new IntentFilter();
	        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
	        registerReceiver(networkStateListener, intentFilter);
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (logger.isActivated()) {
            logger.debug("Start RCS service");
        }

        // Check boot
        if (intent != null) {
            boot = intent.getBooleanExtra("boot", false);
        }

        if (checkAccount()) {
            launchRcsService(boot);
        } else {
            // User account can't be initialized (no radio to read IMSI, .etc)
            if (logger.isActivated()) {
                logger.error("Can't create the user account");
            }

            // Send service intent 
            Intent stopIntent = new Intent(ClientApiIntents.SERVICE_STATUS);
            stopIntent.putExtra("status", ClientApiIntents.SERVICE_STATUS_STOPPED);
            sendBroadcast(stopIntent);

            // Exit service
            stopSelf();
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
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

    /**
     * Set the country code
     */
    private void setCountryCode() {
        // Get country code 
        TelephonyManager mgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        String countryCodeIso = mgr.getSimCountryIso();
        if (countryCodeIso == null) {
        	if (logger.isActivated()) {
        		logger.error("Can't read country code from SIM");
        	}
            return;
        }

        // Parse country table to resolve the area code and country code
        try {
            XmlResourceParser parser = getResources().getXml(R.xml.country_table);
            parser.next();
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("Data")) {
                        if (parser.getAttributeValue(null, "code").equalsIgnoreCase(countryCodeIso)) {
                        	String countryCode = parser.getAttributeValue(null, "cc");
                            if (countryCode != null) {
                                if (!countryCode.startsWith("+")) {
                                    countryCode = "+" + countryCode;
                                }
                                if (logger.isActivated()) {
                                    logger.info("Set country code to " + countryCode);
                                }
                                RcsSettings.getInstance().setCountryCode(countryCode);
                            }

                        	String areaCode = parser.getAttributeValue(null, "tc");
                            if (areaCode != null) {
                                if (logger.isActivated()) {
                                    logger.info("Set area code to " + areaCode);
                                }
                                RcsSettings.getInstance().setCountryAreaCode(areaCode);
                            }
                            return;
                        }
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
        	if (logger.isActivated()) {
        		logger.error("Can't parse country code from XML file", e);
        	}
        } catch (IOException e) {
        	if (logger.isActivated()) {
        		logger.error("Can't read country code from XML file", e);
        	}
        }
    }

    /**
     * Check account
     *
     * @return true if an account is available
     */
    private boolean checkAccount() {
        AndroidFactory.setApplicationContext(getApplicationContext());
        
        // Read the current and last end user account
        currentUserAccount = LauncherUtils.getCurrentUserAccount(getApplicationContext());
        lastUserAccount = LauncherUtils.getLastUserAccount(getApplicationContext());
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
            // Set the country code
            setCountryCode();

            // Set new user flag
            setNewUserAccount(true);
        } else
        if (hasChangedAccount()) {
        	// Backup last account settings
        	if (lastUserAccount != null) {
        		if (logger.isActivated()) {
        			logger.info("Backup " + lastUserAccount);
        		}
        		RcsSettings.getInstance().backupAccountSettings(lastUserAccount);
        	}
        	
            // Set the country code
            setCountryCode();

            // Reset RCS account 
            LauncherUtils.resetRcsConfig(getApplicationContext());

            // Restore current account settings
    		if (logger.isActivated()) {
    			logger.info("Restore " + currentUserAccount);
    		}
            RcsSettings.getInstance().restoreAccountSettings(currentUserAccount);
            
            // Activate service if new account
            RcsSettings.getInstance().setServiceActivationState(true);

            // Set new user flag
            setNewUserAccount(true);
        } else {
            // Set new user flag
            setNewUserAccount(false);
        }
        
        // Check if the RCS account exists
        Account account = AuthenticationService.getAccount(getApplicationContext(),
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
                AuthenticationService.createRcsAccount(getApplicationContext(),
                        getString(R.string.rcs_core_account_username), true);
            }
        } else {
            // Account exists: checks if it has changed
            if (hasChangedAccount()) {
                // Account has changed (i.e. new SIM card): delete the current account and create a new one
                if (logger.isActivated()) {
                    logger.debug("Deleting the old RCS account for " + lastUserAccount);
                }
                ContactsManager.createInstance(getApplicationContext());
                ContactsManager.getInstance().deleteRCSEntries();
                AuthenticationService.removeRcsAccount(getApplicationContext(), null);
    
                if (logger.isActivated()) {
                    logger.debug("Creating a new RCS account for " + currentUserAccount);
                }
                AuthenticationService.createRcsAccount(getApplicationContext(),
                        getString(R.string.rcs_core_account_username), true);
            }
        }

        // Save the current end user account
        LauncherUtils.setLastUserAccount(getApplicationContext(), currentUserAccount);

        return true;
    }

    /**
     * Launch the RCS service.
     *
     * @param boot indicates if RCS is launched from the device boot
     */
    private void launchRcsService(boolean boot) {
        int mode = RcsSettings.getInstance().getAutoConfigMode();

        if (logger.isActivated()) {
            logger.debug("Launch RCS service: HTTPS="
                + (mode == RcsSettingsData.HTTPS_AUTO_CONFIG)
                + ", boot=" + boot);
        }

        if (mode == RcsSettingsData.HTTPS_AUTO_CONFIG) {
            // HTTPS auto config

        	// Check the last provisioning version
            if (RcsSettings.getInstance().getProvisioningVersion().equals("-1")) {
                if (hasChangedAccount()) {
                    // Start provisioning as a first launch
                    Intent provisioningIntent = new Intent(ClientApiUtils.PROVISIONING_SERVICE_NAME);
                    provisioningIntent.putExtra(HttpsProvisioningService.FIRST_KEY, true);
                    startService(provisioningIntent);
                } else {
                    if (logger.isActivated()) {
                        logger.debug("Provisioning is blocked with this account");
                    }
                }
            } else {
                if (isFirstLaunch() || hasChangedAccount()) {
                    // First launch: start the auto config service with special tag
                    Intent provisioningIntent = new Intent(ClientApiUtils.PROVISIONING_SERVICE_NAME);
                    provisioningIntent.putExtra(HttpsProvisioningService.FIRST_KEY, true);
                    startService(provisioningIntent);
                } else
                if (boot) {
                    // Boot: start the auto config service
                    Intent provisioningIntent = new Intent(ClientApiUtils.PROVISIONING_SERVICE_NAME);
                    provisioningIntent.putExtra(HttpsProvisioningService.FIRST_KEY, false);
                    startService(provisioningIntent);
                } else {
                    // Start the RCS core service
                    LauncherUtils.launchRcsCoreService(getApplicationContext());
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
}