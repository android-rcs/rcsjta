/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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
import com.gsma.rcs.core.Core;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.platform.registry.RegistryFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.LauncherUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * The user changed an account (modify, delete or add) <br>
 * We cannot prevent the user deleting the account but we can detect the deletion
 */
public class AccountChangedReceiver extends BroadcastReceiver {
    /**
     * Account has been manually deleted
     */
    private static final String REGISTRY_RCS_ACCOUNT_MANUALY_DELETED = "RcsAccountManualyDeleted";

    private static final Logger sLogger = Logger.getLogger(AccountChangedReceiver.class.getName());

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Core.getInstance().scheduleCoreOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    ContentResolver contentResolver = context.getContentResolver();
                    LocalContentResolver localContentResolver = new LocalContentResolver(
                            contentResolver);
                    RcsSettings rcsSettings = RcsSettings.getInstance(localContentResolver);
                    AndroidFactory.setApplicationContext(context, rcsSettings);
                    ContactManager contactManager = ContactManager.getInstance(context,
                            contentResolver, localContentResolver, rcsSettings);
                    RcsAccountManager accountUtility = RcsAccountManager.getInstance(context,
                            contactManager);

                    /* Verify that the RCS account is still here */
                    Account mAccount = accountUtility.getAccount(context
                            .getString(R.string.rcs_core_account_username));
                    if (mAccount == null) {
                        boolean logActivated = sLogger.isActivated();
                        if (logActivated) {
                            sLogger.debug("RCS account has been deleted: stop services");
                        }
                        /* Set the user account manually deleted flag */
                        if (rcsSettings.isUserProfileConfigured()) {
                            setAccountResetByEndUser(true);
                        }
                        /*
                         * RCS account was deleted. Warn the user we stop the service. The account
                         * will be recreated when the service will be restarted.
                         */
                        Toast.makeText(
                                context,
                                context.getString(R.string.rcs_core_account_stopping_after_deletion),
                                Toast.LENGTH_LONG).show();

                        /* Stop the service */
                        LauncherUtils.stopRcsService(context);
                    } else {
                        /* Set the user account manually deleted flag */
                        setAccountResetByEndUser(false);
                    }
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder(
                                    "Unable to handle connection event for intent action : ")
                                    .append(intent.getAction()).toString(), e);
                }
            }
        });
    }

    /**
     * Is user account reset by end user
     * 
     * @return Boolean
     */
    public static boolean isAccountResetByEndUser() {
        return RegistryFactory.getFactory()
                .readBoolean(REGISTRY_RCS_ACCOUNT_MANUALY_DELETED, false);
    }

    /**
     * Set user account reset by end user
     * 
     * @param value True if RCS account is reseted by end user
     */
    public static void setAccountResetByEndUser(boolean value) {
        RegistryFactory.getFactory().writeBoolean(REGISTRY_RCS_ACCOUNT_MANUALY_DELETED, value);
    }
}
