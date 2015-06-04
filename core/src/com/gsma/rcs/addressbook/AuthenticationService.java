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

package com.gsma.rcs.addressbook;

import com.gsma.rcs.R;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Groups;

/**
 * This class is a Service to authenticate the user's account information.
 */
public class AuthenticationService extends Service {

    private RcsContactsAccountAuthenticator mAuthenticator;

    /**
     * Account manager type
     */
    public static final String ACCOUNT_MANAGER_TYPE = "com.gsma.rcs";

    private static final Logger sLogger = Logger.getLogger(AuthenticationService.class.getName());

    private static final String CONTACTSCONTRACT_GROUPS_COLUMN_TITLE_RES = "title_res";
    private static final String CONTACTSCONTRACT_GROUPS_COLUMN_RES_PACKAGE = "res_package";

    /**
     * Get the account for the specified name
     * 
     * @param context The context
     * @param username The username
     * @return The account
     */
    public static Account getAccount(Context context, String username) {
        AccountManager accountManager = AccountManager.get(context);
        for (Account account : accountManager.getAccountsByType(ACCOUNT_MANAGER_TYPE)) {
            if (username.equals(account)) {
                return account;
            }
        }
        return null;
    }

    /**
     * Create the RCS account if it does not already exist
     * 
     * @param context The context
     * @param localContentResolver Local content resolver
     * @param username The username
     * @param enableSync true to enable synchronization
     * @param rcsSettings RCS settings accessor
     * @param contactManager Contact manager accessor
     */
    public static void createRcsAccount(Context context, LocalContentResolver localContentResolver,
            String username, boolean enableSync, RcsSettings rcsSettings,
            ContactManager contactManager) {
        /* Save the account info into the AccountManager if needed */
        Account account = getAccount(context, username);
        if (account == null) {
            account = new Account(username, ACCOUNT_MANAGER_TYPE);
            AccountManager accountManager = AccountManager.get(context);
            boolean resource = accountManager.addAccountExplicitly(account, null, null);
            if (!resource) {
                if (sLogger.isActivated()) {
                    sLogger.error("Unable to create account for ".concat(username));
                }
                return;
            }
        }

        /* Set contacts sync for this account. */
        if (enableSync) {
            ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
        }
        ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, enableSync);

        /* Insert RCS group if it does not exist */
        if (ContactManager.INVALID_ID == contactManager.getRcsGroupIdFromContactsContractGroups()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Groups.ACCOUNT_NAME, username);
            contentValues.put(Groups.ACCOUNT_TYPE, ACCOUNT_MANAGER_TYPE);
            contentValues.put(Groups.GROUP_VISIBLE, false);
            contentValues.put(Groups.TITLE, context.getString(R.string.rcs_core_account_id));
            if (Build.VERSION.SDK_INT >= 21) {
                contentValues.put(CONTACTSCONTRACT_GROUPS_COLUMN_TITLE_RES,
                        R.string.rcs_core_account_id);
                contentValues.put(CONTACTSCONTRACT_GROUPS_COLUMN_RES_PACKAGE,
                        context.getPackageName());

            }
            contentValues.put(Groups.GROUP_IS_READ_ONLY, 1);
            context.getContentResolver().insert(Groups.CONTENT_URI, contentValues);
        }

        /* Create the "Me" item */
        contactManager.createMyContact();
    }

    /**
     * Check if sync is enabled.
     * 
     * @param context The context
     * @param username The username
     * @return True if sync is enabled
     */
    public static boolean isSyncEnabled(Context context, String username) {
        Account account = getAccount(context, username);
        if (account == null) {
            return false;
        }
        return ContentResolver.getSyncAutomatically(account, ContactsContract.AUTHORITY);
    }

    /**
     * Remove all RCS accounts with the exception of the excludeUsername account
     * 
     * @param context The context
     * @param excludeUsername The username for which the account should not be removed (can be null)
     */
    public static void removeRcsAccount(Context context, String excludeUsername) {
        AccountManager accountManager = AccountManager.get(context);
        for (Account account : accountManager.getAccountsByType(ACCOUNT_MANAGER_TYPE)) {
            if (!account.name.equals(excludeUsername)) {
                accountManager.removeAccount(account, null, null);
            }
        }
    }

    /**
     * Called by the system when the service is first created.
     */
    @Override
    public void onCreate() {
        mAuthenticator = new RcsContactsAccountAuthenticator(this);
    }

    /**
     * When binding to the service, return an interface to Authenticator Service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        if (AccountManager.ACTION_AUTHENTICATOR_INTENT.equals(intent.getAction())) {
            return mAuthenticator.getIBinder();
        }
        if (sLogger.isActivated()) {
            sLogger.error("Bound with unknown intent: ".concat(intent.toString()));
        }
        return null;
    }

    /**
     * This class is used for creating AccountAuthenticators.
     */
    final static class RcsContactsAccountAuthenticator extends AbstractAccountAuthenticator {
        private final Context mContext;

        public RcsContactsAccountAuthenticator(Context context) {
            super(context);
            mContext = context;
        }

        /**
         * Adds an account of the specified accountType.
         */
        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                String authTokenType, String[] requiredFeatures, Bundle options)
                throws NetworkErrorException {
            /*
             * Launch the login activity to add the account, letting it know that we got there by
             * trying to add an account so it can check for an existing account.
             */
            Bundle bundle = new Bundle();
            Intent intent = new Intent(mContext, SetupRcsAccount.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }

        /**
         * Returns a Bundle that contains the Intent of the activity that can be used to edit the
         * properties.
         */
        @Override
        public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
            throw new UnsupportedOperationException();
        }

        /**
         * Checks that the user knows the credentials of an account.
         */
        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
                Bundle options) {
            return null;
        }

        /**
         * Gets the authtoken for an account.
         */
        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                String authTokenType, Bundle loginOptions) throws NetworkErrorException {
            return null;
        }

        /**
         * Ask the authenticator for a localized label for the given authTokenType.
         */
        @Override
        public String getAuthTokenLabel(String authTokenType) {
            return null;
        }

        /**
         * Update the locally stored credentials for an account.
         */
        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
                String authTokenType, Bundle loginOptions) {
            return null;
        }

        /**
         * Checks if the account supports all the specified authenticator specific features.
         */
        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
                String[] features) throws NetworkErrorException {
            Bundle result = new Bundle();
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
            return result;
        }
    }

}
