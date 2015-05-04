/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.cpm.ms;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener {

    // Content provider authority
    public static final String HISTORY_AUTHORITY = "com.gsma.services.rcs.provider.history";

    // Account
    public static final String ACCOUNT = "Me";
    public static final String ACCOUNT_TYPE = "sonymobile.com";

    Account mAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccount = createSyncAccount(this);

        // setContentView(R.layout.main_activity);

        // Button bu = (Button) findViewById(R.id.button1);

        // bu.setOnClickListener(this);
    }

    /**
     * Create a new dummy account for the sync adapter
     * 
     * @param context The application context
     */
    public static Account createSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data If successful, return the
         * Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            /*
             * If you don't set android:syncable="true" in in your <provider> element in the
             * manifest, then call context.setIsSyncable(account, AUTHORITY, 1) here.
             */
        } else {
            /*
             * The account exists or some other error occurred. Log this, report it, or handle it
             * internally.
             */
        }
        return newAccount;
    }

    @Override
    public void onClick(View v) {
        Log.i("MainActivity", "Click");

        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        ContentResolver.requestSync(mAccount, HISTORY_AUTHORITY, settingsBundle);
    }
}
