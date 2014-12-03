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

package com.orangelabs.rcs.addressbook;

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
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Groups;

import com.orangelabs.rcs.provider.LocalContentResolver;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * This class is a Service to authenticate the user's account information.
 */
public class AuthenticationService extends Service {

	/**
	 * Authenticator
	 */
    private RcsContactsAccountAuthenticator mAuthenticator;

    /**
     * Account manager type
     */
    public static final String ACCOUNT_MANAGER_TYPE = "com.orangelabs.rcs";
    
	/**
     * The logger
     */
    private static Logger logger = Logger.getLogger(AuthenticationService.class.getName());
    
    /**
     * Get the account for the specified name
     * 
     * @param context The context
     * @param username The username
     * @return The account
     */
    public static Account getAccount(Context context, String username) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] mAccounts = accountManager.getAccountsByType(AuthenticationService.ACCOUNT_MANAGER_TYPE);
        Account mAccount = null;
        int length = mAccounts.length;
        for (int i = 0; i < length; i++) {
            if (username.equals(mAccounts[i].name)) {
                mAccount = mAccounts[i];
                break;
            }
        }
        return mAccount;
    }

    /**
     * Create the RCS account if it does not already exist
     * 
     * @param context The context
     * @param localContentResolver Local content resolver
     * @param username The username
     * @param enableSync true to enable synchronization
     * @param showUngroupedContacts true to show ungrouped contacts
     */
    public static void createRcsAccount(Context context, LocalContentResolver localContentResolver, String username, boolean enableSync) {
    	ContactsManager.createInstance(context, context.getContentResolver(), localContentResolver);
    	
        // Save the account info into the AccountManager if needed
        Account mAccount = getAccount(context, username);
        if (mAccount == null) {
            mAccount = new Account(username, AuthenticationService.ACCOUNT_MANAGER_TYPE);
            AccountManager accountManager = AccountManager.get(context);
            boolean resource = accountManager.addAccountExplicitly(mAccount, null, null);
            if (!resource) {
            	if (logger.isActivated()){
            		logger.error("Unable to create account for " + username);
            	}
                return;
            }
        }

        // Set contacts sync for this account.
        if (enableSync){
        	ContentResolver.setIsSyncable(mAccount, ContactsContract.AUTHORITY, 1);
        }
        ContentResolver.setSyncAutomatically(mAccount, ContactsContract.AUTHORITY, enableSync);

        ContentValues contentValues = new ContentValues();
        contentValues.put(Groups.ACCOUNT_NAME, username);
        contentValues.put(Groups.ACCOUNT_TYPE, AuthenticationService.ACCOUNT_MANAGER_TYPE);
        contentValues.put(Groups.GROUP_VISIBLE, false);

        context.getContentResolver().insert(Groups.CONTENT_URI, contentValues);

        // Create the "Me" item
        ContactsManager.getInstance().createMyContact();
    }

    /**
     * Check if sync is enabled.
     * 
     * @param context The context
     * @param username The username
     */
    public static boolean isSyncEnabled(Context context, String username) {
        Account mAccount = getAccount(context, username);
        if (mAccount == null) {
            return false;
        }
        return ContentResolver.getSyncAutomatically(mAccount, ContactsContract.AUTHORITY);
    }

    /**
     * Remove all RCS accounts with the exception of the excludeUsername account
     * 
     * @param context The context
     * @param excludeUsername The username for which the account should not be
     *            removed (can be null)
     */
    public static void removeRcsAccount(Context context, String excludeUsername) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] mAccounts = accountManager.getAccountsByType(AuthenticationService.ACCOUNT_MANAGER_TYPE);
        int length = mAccounts.length;
        for (int i = 0; i < length; i++) {
            if (!mAccounts[i].name.equals(excludeUsername)) {
                accountManager.removeAccount(mAccounts[i], null, null);
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
     * When binding to the service, return an interface to Authenticator
     * Service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        if (AccountManager.ACTION_AUTHENTICATOR_INTENT.equals(intent.getAction())) {
            return mAuthenticator.getIBinder();
        }
        
        if (logger.isActivated()){
        	logger.error("Bound with unknown intent: " + intent);
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
             * Launch the login activity to add the account, letting it know
             * that we got there by trying to add an account so it can check for
             * an existing account.
             */
            Bundle mBundle = new Bundle();
            Intent mIntent = new Intent(mContext, SetupRcsAccount.class);
            mIntent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            mBundle.putParcelable(AccountManager.KEY_INTENT, mIntent);
            return mBundle;
        }
        
        

        /**
         * Returns a Bundle that contains the Intent of the activity that can be
         * used to edit the properties.
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
         * Ask the authenticator for a localized label for the given
         * authTokenType.
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
         * Checks if the account supports all the specified authenticator
         * specific features.
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
