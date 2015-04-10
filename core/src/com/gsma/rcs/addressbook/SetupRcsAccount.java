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
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.LauncherUtils;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

/**
 * Setup RCS account activity
 */
public class SetupRcsAccount extends android.accounts.AccountAuthenticatorActivity {

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Context ctx = getApplicationContext();
        ContentResolver contentResolver = ctx.getContentResolver();
        LocalContentResolver localContentResolver = new LocalContentResolver(contentResolver);
        RcsSettings rcsSettings = RcsSettings.createInstance(localContentResolver);
        ContactsManager contactManager = ContactsManager.createInstance(ctx, contentResolver,
                localContentResolver, rcsSettings);

        AuthenticationService.createRcsAccount(this, localContentResolver,
                getString(R.string.rcs_core_account_username), true, rcsSettings, contactManager);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            AccountAuthenticatorResponse response = extras
                    .getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME,
                    getString(R.string.rcs_core_account_username));
            result.putString(AccountManager.KEY_ACCOUNT_TYPE,
                    AuthenticationService.ACCOUNT_MANAGER_TYPE);
            response.onResult(result);

            // Start the service
            LauncherUtils.launchRcsService(ctx, false, false, rcsSettings);
        }
        finish();
    }

}
