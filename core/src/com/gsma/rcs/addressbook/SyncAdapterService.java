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

import com.gsma.rcs.ServerApiServiceNotRegisteredException;
import com.gsma.rcs.core.Core;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.service.api.ServerApiUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;

import java.util.Set;

/**
 * Service to handle account sync. This is invoked with an intent with action
 * ACTION_AUTHENTICATOR_INTENT. It instantiates the sync adapter and returns its IBinder.
 */
public class SyncAdapterService extends Service {

    /**
     * Contacts sync adapter.
     */
    private RcsContactsSyncAdapter mSyncAdapter;

    /**
     * Android content sync adapter
     */
    public static final String ANDROID_CONTENT_SYNCADPTER = "android.content.SyncAdapter";

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate() {
        mSyncAdapter = new RcsContactsSyncAdapter(this);
    }

    /**
     * When binding to the service, return an interface to our sync adpater.
     */
    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getAction().equals(ANDROID_CONTENT_SYNCADPTER)) {
            return mSyncAdapter.getSyncAdapterBinder();
        }
        if (logger.isActivated()) {
            logger.error("Bound with unknown intent: " + intent);
        }
        return null;
    }

    /**
     * Sync adapter that spawns a thread to invoke a sync operation.
     */
    private class RcsContactsSyncAdapter extends AbstractThreadedSyncAdapter {

        public RcsContactsSyncAdapter(Context context) {
            super(context, true /* autoInitialize */);

        }

        /**
         * Perform a sync for this account.
         */
        @Override
        public void onPerformSync(Account account, Bundle extras, String authority,
                ContentProviderClient provider, SyncResult syncResult) {

            if (logger.isActivated()) {
                logger.debug("Performing a refresh on contact capabilities");
            }

            // Test IMS connection
            try {
                ServerApiUtils.testIms();
            } catch (ServerApiServiceNotRegisteredException e) {
                if (logger.isActivated()) {
                    logger.debug("IMS connection failed");
                }
                syncResult.stats.numIoExceptions++;
                return;
            }

            // Update all contacts capabilities
            Set<ContactId> contacts = ContactsManager.getInstance().getAllContacts();
            Core.getInstance().getCapabilityService().requestContactCapabilities(contacts);
        }
    }
}
