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

import com.gsma.rcs.utils.logger.Logger;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

/**
 * This class is a Service to authenticate the user's account information.
 */
public class AuthenticationService extends Service {

    private RcsContactsAccountAuthenticator mAuthenticator;

    private static final Logger sLogger = Logger.getLogger(AuthenticationService.class.getName());

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
