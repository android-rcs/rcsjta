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

import com.sonymobile.rcs.cpm.ms.impl.CommonMessageStoreImpl;
import com.sonymobile.rcs.cpm.ms.impl.sync.DefaultSyncMediator;
import com.sonymobile.rcs.cpm.ms.sync.SyncMediator;
import com.sonymobile.rcs.cpm.ms.sync.SyncReport;
import com.sonymobile.rcs.cpm.ms.sync.SynchronizationListener;
import com.sonymobile.rcs.imap.DefaultImapService;
import com.sonymobile.rcs.imap.ImapService;
import com.sonymobile.rcs.imap.IoService;
import com.sonymobile.rcs.imap.SocketIoService;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

public class MessageStoreSyncAdapter extends AbstractThreadedSyncAdapter implements
        SynchronizationListener {

    // Storage for an instance of the sync adapter
    private static MessageStoreSyncAdapter sSyncAdapter = null;
    // Object to use as a thread-safe lock
    private static final Object sSyncAdapterLock = new Object();

    private SyncMediator mSyncManager;

    public static final MessageStoreSyncAdapter getInstance(Context context) {
        /*
         * Create the sync adapter as a singleton. Set the sync adapter as syncable Disallow
         * parallel syncs
         */
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                try {
                    sSyncAdapter = new MessageStoreSyncAdapter(context, true);
                } catch (Exception e) {
                    // Unable to create adapter
                    e.printStackTrace();
                    throw new RuntimeException("Unable to create adapter", e);
                }

            }
            return sSyncAdapter;
        }
    }

    private MessageStoreSyncAdapter(Context context, boolean autoInitialize) throws Exception {
        super(context, autoInitialize);

        MessageStore localStore = null;// new LocalStore(new SyncDatabaseDelegate());

        IoService io = new SocketIoService("imap://192.168.1.8");

        ImapService imap = new DefaultImapService(io);

        imap.setAuthenticationDetails("user10@sonymobile.com", "1234", null, null, false);

        MessageStore remoteStore = new CommonMessageStoreImpl("cpmdefault", imap);

        mSyncManager = new DefaultSyncMediator(localStore, remoteStore);
        mSyncManager.addSynchronizationListener(this);
    }

    @Override
    public void onSyncStopped(SyncReport job) {
        Exception error = job.getException();
        if (error != null) {
            Log.e("SYNCADAPTER", "FINISHED", error);
        } else {
            Log.i("SYNCADAPTER", "FINISHED :" + job);
        }
    }

    @Override
    public void onSyncStarted(SyncReport job) {
        Log.i("SYNCADAPTER", "STARTED :" + job);
    }

    @Override
    public synchronized void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        // TODO Auto-generated method stub
        Log.i("SYNCADAPTER", "CALLED");
        try {

            Log.i("SYNCADAPTER", "CALLING :" + authority);

            mSyncManager.execute();

            Log.i("SYNCADAPTER", "EXECUTED");

        } catch (Exception e) {
            Log.e("SYNCADAPTER", "ERROR", e);
            e.printStackTrace();
        } finally {
        }
    }

}
