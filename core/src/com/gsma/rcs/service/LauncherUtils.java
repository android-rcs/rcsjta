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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

import com.gsma.rcs.addressbook.AccountChangedReceiver;
import com.gsma.rcs.addressbook.AuthenticationService;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.platform.registry.AndroidRegistryFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.ipcall.IPCallHistory;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.sharing.RichCallHistory;
import com.gsma.rcs.provisioning.https.HttpsProvisioningService;
import com.gsma.rcs.utils.logger.Logger;

/**
 * Launcher utility functions
 * 
 * @author hlxn7157
 */
public class LauncherUtils {

    private static final long DEFAULT_PROVISIONING_VALIDITY = 24 * 3600 * 1000L;

    /**
     * Last user account used
     */
    public static final String REGISTRY_LAST_USER_ACCOUNT = "LastUserAccount";

    /**
     * Key for storing the latest positive provisioning version
     */
    private static final String REGISTRY_PROVISIONING_VERSION = "ProvisioningVersion";

    /**
     * Key for storing the latest positive provisioning validity
     */
    private static final String REGISTRY_PROVISIONING_VALIDITY = "ProvisioningValidity";

    /**
     * Key for storing the expiration date of the provisioning
     */
    private static final String REGISTRY_PROVISIONING_EXPIRATION = "ProvisioningExpiration";

    /**
     * Logger
     */
    private static final Logger sLogger = Logger.getLogger(LauncherUtils.class.getName());

    /**
     * Launch the RCS service
     * 
     * @param context application context
     * @param boot Boot flag
     * @param user restart is required by user
     * @param rcsSettings
     */
    public static void launchRcsService(Context context, boolean boot, boolean user,
            RcsSettings rcsSettings) {
        // Set the logger properties
        Logger.activationFlag = rcsSettings.isTraceActivated();
        Logger.traceLevel = rcsSettings.getTraceLevel();

        if (rcsSettings.isServiceActivated()) {
            StartService.LaunchRcsStartService(context, boot, user);
        }
    }

    /**
     * Launch the RCS core service
     * 
     * @param context Application context
     * @param rcsSettings
     */
    public static void launchRcsCoreService(Context context, RcsSettings rcsSettings) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("Launch core service");
        }
        if (!rcsSettings.isServiceActivated()) {
            if (logActivated) {
                sLogger.debug("RCS service is disabled");
            }
            return;

        }
        if (!rcsSettings.isUserProfileConfigured()) {
            if (logActivated) {
                sLogger.debug("RCS service not configured");
            }
            return;

        }
        if (!rcsSettings.isProvisioningTermsAccepted()) {
            if (logActivated) {
                sLogger.debug("Provisioning terms are not accepted");
            }
            return;

        }
        context.startService(new Intent(context, RcsCoreService.class));
    }

    /**
     * Stop the RCS service
     * 
     * @param context Application context
     */
    public static void stopRcsService(Context context) {
        if (sLogger.isActivated()) {
            sLogger.debug("Stop RCS service");
        }
        context.stopService(new Intent(context, StartService.class));
        context.stopService(new Intent(context, HttpsProvisioningService.class));
        context.stopService(new Intent(context, RcsCoreService.class));
    }

    /**
     * Stop the RCS core service (but keep provisioning)
     * 
     * @param context Application context
     */
    public static void stopRcsCoreService(Context context) {
        if (sLogger.isActivated()) {
            sLogger.debug("Stop RCS core service");
        }
        context.stopService(new Intent(context, StartService.class));
        context.stopService(new Intent(context, RcsCoreService.class));
    }

    /**
     * Reset RCS configuration
     * 
     * @param ctx Application context
     * @param localContentResolver Local content resolver
     * @param rcsSettings
     * @param mMessagingLog
     * @param contactManager
     */
    public static void resetRcsConfig(Context ctx, LocalContentResolver localContentResolver,
            RcsSettings rcsSettings, MessagingLog mMessagingLog, ContactsManager contactManager) {
        if (sLogger.isActivated()) {
            sLogger.debug("Reset RCS config");
        }
        // Stop the Core service
        ctx.stopService(new Intent(ctx, RcsCoreService.class));

        // Reset user profile
        rcsSettings.resetUserProfile();

        // Clear all entries in chat, message and file transfer tables
        mMessagingLog.deleteAllEntries();

        // Clear all entries in IP call table
        IPCallHistory.createInstance(localContentResolver);
        IPCallHistory.getInstance().deleteAllEntries();

        // Clear all entries in Rich Call tables (image and video)
        RichCallHistory.createInstance(localContentResolver);
        RichCallHistory.getInstance().deleteAllEntries();

        /*
         * Clean the previous account RCS databases : because they may not be overwritten in the
         * case of a very new account or if the back-up files of an older one have been destroyed.
         */
        contactManager.deleteRCSEntries();

        // Remove the RCS account
        AuthenticationService.removeRcsAccount(ctx, null);
        // Ensure that factory is set up properly to avoid NullPointerException in
        // AccountChangedReceiver.setAccountResetByEndUser
        AndroidFactory.setApplicationContext(ctx, rcsSettings);
        AccountChangedReceiver.setAccountResetByEndUser(false);

        // Clean terms status
        rcsSettings.setProvisioningTermsAccepted(false);

        // Set the configuration validity flag to false
        rcsSettings.setConfigurationValid(false);
    }

    /**
     * Get the last user account
     * 
     * @param context Application context
     * @return last user account
     */
    public static String getLastUserAccount(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        return preferences.getString(REGISTRY_LAST_USER_ACCOUNT, null);
    }

    /**
     * Set the last user account
     * 
     * @param context Application context
     * @param value last user account
     */
    public static void setLastUserAccount(Context context, String value) {
        SharedPreferences preferences = context.getSharedPreferences(
                AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(REGISTRY_LAST_USER_ACCOUNT, value);
        editor.commit();
    }

    /**
     * Get current user account
     * 
     * @param context Application context
     * @return current user account
     */
    public static String getCurrentUserAccount(Context context) {
        TelephonyManager mgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String currentUserAccount = mgr.getSubscriberId();
        mgr = null;
        return currentUserAccount;
    }

    /**
     * Get the latest positive provisioning version
     * 
     * @param context Application context
     * @return the latest positive provisioning version
     */
    public static String getProvisioningVersion(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        return preferences.getString(REGISTRY_PROVISIONING_VERSION, "0");
    }

    /**
     * Save the latest positive provisioning version in shared preferences
     * 
     * @param context Application context
     * @param value the latest positive provisioning version
     */
    public static void saveProvisioningVersion(Context context, String value) {
        try {
            int vers = Integer.parseInt(value);
            if (vers > 0) {
                SharedPreferences preferences = context.getSharedPreferences(
                        AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(REGISTRY_PROVISIONING_VERSION, value);
                editor.commit();
            }
        } catch (NumberFormatException e) {
        }
    }

    /**
     * Get the expiration date of the provisioning
     * 
     * @param context Application context
     * @return the expiration date in milliseconds or 0 if not applicable
     */
    public static long getProvisioningExpirationDate(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        return preferences.getLong(REGISTRY_PROVISIONING_EXPIRATION, 0L);
    }

    /**
     * Get the expiration date of the provisioning
     * 
     * @param context Application context
     * @return the expiration date in milliseconds
     */
    public static long getProvisioningValidity(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        return preferences.getLong(REGISTRY_PROVISIONING_VALIDITY, DEFAULT_PROVISIONING_VALIDITY);
    }

    /**
     * Save the provisioning validity in shared preferences
     * 
     * @param context
     * @param validity validity of the provisioning expressed in milliseconds
     */
    public static void saveProvisioningValidity(Context context, long validity) {
        if (validity <= 0L) {
            return;
        }
        // Calculate next expiration date in msec
        long next = System.currentTimeMillis() + validity;
        SharedPreferences preferences = context.getSharedPreferences(
                AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(REGISTRY_PROVISIONING_VALIDITY, validity);
        editor.putLong(REGISTRY_PROVISIONING_EXPIRATION, next);
        editor.commit();
    }
}
