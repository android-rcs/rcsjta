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

package com.gsma.rcs.addressbook;

import com.gsma.rcs.R;
import com.gsma.rcs.provider.contact.ContactManager;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Groups;

/**
 * RCS account manager
 */
public class RcsAccountManager {

    private static volatile RcsAccountManager sInstance;

    private final Context mContext;

    private final AccountManager mAccountManager;

    private final ContentResolver mContentResolver;

    private final ContactManager mContactManager;

    private static final int VERSION_CODE_LOLLIPOP = 21;

    /**
     * Account manager type
     */
    public static final String ACCOUNT_MANAGER_TYPE = "com.gsma.rcs";

    private static final String CONTACTSCONTRACT_GROUPS_COLUMN_TITLE_RES = "title_res";
    private static final String CONTACTSCONTRACT_GROUPS_COLUMN_RES_PACKAGE = "res_package";

    /**
     * Constructor
     * 
     * @param context Application context
     * @param contactManager accessor for contact provider
     */
    public RcsAccountManager(Context context, ContactManager contactManager) {
        mContext = context;
        mAccountManager = AccountManager.get(context);
        mContentResolver = mContext.getContentResolver();
        mContactManager = contactManager;
    }

    /**
     * Creates a singleton instance of RcsAccountManager
     * 
     * @param context Application context
     * @param contactManager accessor for contact provider
     * @return singleton instance of RcsAccountManager
     */
    public static RcsAccountManager createInstance(Context context, ContactManager contactManager) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (RcsAccountManager.class) {
            if (sInstance == null) {
                sInstance = new RcsAccountManager(context, contactManager);
            }
            return sInstance;
        }
    }

    /**
     * Gets the account for the specified name
     * 
     * @param username The username
     * @return The account or null if it does not exist
     */
    public Account getAccount(String username) {
        for (Account account : mAccountManager.getAccountsByType(ACCOUNT_MANAGER_TYPE)) {
            if (username.equals(account.name)) {
                return account;
            }
        }
        return null;
    }

    /**
     * Creates the RCS account if it does not already exist
     * 
     * @param username The user name
     * @param enableSync true to enable synchronization
     * @throws RcsAccountException thrown if RCS account failed to be created
     */
    public void createRcsAccount(String username, boolean enableSync) throws RcsAccountException {
        /* Save the account info into the AccountManager if needed */
        Account account = getAccount(username);
        if (account == null) {
            account = new Account(username, ACCOUNT_MANAGER_TYPE);
            AccountManager accountManager = AccountManager.get(mContext);
            boolean resource = accountManager.addAccountExplicitly(account, null, null);
            if (!resource) {
                throw new RcsAccountException("Failed to create RCS account for username '"
                        + username + "'!");
            }
        }

        /* Set contacts sync for this account. */
        if (enableSync) {
            ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
        }
        ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, enableSync);

        /* Insert RCS group if it does not exist */
        if (ContactManager.INVALID_ID == mContactManager.getRcsGroupIdFromContactsContractGroups()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Groups.ACCOUNT_NAME, username);
            contentValues.put(Groups.ACCOUNT_TYPE, ACCOUNT_MANAGER_TYPE);
            contentValues.put(Groups.GROUP_VISIBLE, false);
            contentValues.put(Groups.TITLE, mContext.getString(R.string.rcs_core_account_id));
            if (Build.VERSION.SDK_INT >= VERSION_CODE_LOLLIPOP) {
                contentValues.put(CONTACTSCONTRACT_GROUPS_COLUMN_TITLE_RES,
                        R.string.rcs_core_account_id);
                contentValues.put(CONTACTSCONTRACT_GROUPS_COLUMN_RES_PACKAGE,
                        mContext.getPackageName());

            }
            contentValues.put(Groups.GROUP_IS_READ_ONLY, 1);
            mContentResolver.insert(Groups.CONTENT_URI, contentValues);
        }

        /* Create the "Me" item */
        mContactManager.createMyContact();
    }

    /**
     * Checks if sync is enabled.
     * 
     * @param username The username
     * @return True if sync is enabled
     */
    public boolean isSyncEnabled(String username) {
        Account account = getAccount(username);
        if (account == null) {
            return false;
        }
        return ContentResolver.getSyncAutomatically(account, ContactsContract.AUTHORITY);
    }

    /**
     * Removes all RCS accounts with the exception of the excludeUsername account
     * 
     * @param excludeUsername The username for which the account should not be removed (can be null)
     */
    public void removeRcsAccount(String excludeUsername) {
        for (Account account : mAccountManager.getAccountsByType(ACCOUNT_MANAGER_TYPE)) {
            if (!account.name.equals(excludeUsername)) {
                mAccountManager.removeAccount(account, null, null);
            }
        }
    }
}
