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

package com.sonymobile.rcs.cpm.ms.impl.sync;

import com.sonymobile.rcs.cpm.ms.CpmObjectType;
import com.sonymobile.rcs.cpm.ms.MessageStore;
import com.sonymobile.rcs.cpm.ms.sync.MutableReport;
import com.sonymobile.rcs.cpm.ms.sync.SyncMediator;
import com.sonymobile.rcs.cpm.ms.sync.SyncReport;
import com.sonymobile.rcs.cpm.ms.sync.SyncStrategy;
import com.sonymobile.rcs.cpm.ms.sync.SynchronizationListener;

import java.io.Closeable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Logger;

public class DefaultSyncMediator implements SyncMediator {

    private static DefaultSyncMediator sInstance = null;

    private final MessageStore mLocalStore;

    private final MessageStore mRemoteStore;

    private Collection<SynchronizationListener> mSyncListeners = new LinkedList<SynchronizationListener>();

    private SyncStrategy mStrategy = new DefaultSyncStrategy();

    private MutableReport mCurrentReport;

    private static Logger LOG = Logger.getLogger(DefaultSyncMediator.class.getName());

    public DefaultSyncMediator(MessageStore localStore, MessageStore remoteStore) {
        super();
        if (sInstance != null) {
            LOG.warning("DefaultSynchronizationManager Instance already exists");
        }
        sInstance = this;
        this.mLocalStore = localStore;
        this.mRemoteStore = remoteStore;
    }

    @Override
    public SyncReport getCurrentReport() {
        return mCurrentReport;
    }

    @Override
    public void setStrategy(SyncStrategy strategy) {
        this.mStrategy = strategy;
    }

    @Override
    public SyncStrategy getStrategy() {
        return mStrategy;
    }

    @Override
    public SyncReport execute(CpmObjectType... itemTypes) {
        mLocalStore.setFilter(itemTypes);
        mRemoteStore.setFilter(itemTypes);

        mCurrentReport = new MutableReport();

        try {
            // INIT RESOURCES
            if (!mLocalStore.isConnected()) {
                mLocalStore.open();
            }
            if (!mRemoteStore.isConnected()) {
                mRemoteStore.open();
            }
            // END INIT

            fireSyncStarted(mCurrentReport);
            mStrategy.execute(mLocalStore, mRemoteStore, mCurrentReport);

            mCurrentReport.setSuccess(true);

        } catch (Exception e) {

            mCurrentReport.setException(e);

        } finally {
            // TODO manage close resources elsewhere
            closeResources(mRemoteStore, mLocalStore);
            mCurrentReport.setStopped();
            fireSyncFinished(mCurrentReport);
        }

        return mCurrentReport;
    }

    /**
     * Same as execute() but will raise the exception. Should be used for testing.
     * 
     * @param itemType
     * @throws Exception
     */
    public void executeUnsafe(CpmObjectType... itemTypes) throws Exception {
        SyncReport report = execute(itemTypes);
        if (report.getException() != null) {
            throw report.getException();
        }
        if (!report.isSuccess()) {
            throw new Exception("Sync not successful, exception not raised");
        }
    }

    @Override
    public MessageStore getRemoteStore() {
        return mRemoteStore;
    }

    @Override
    public MessageStore getLocalStore() {
        return mLocalStore;
    }

    @Override
    public void addSynchronizationListener(SynchronizationListener listener) {
        mSyncListeners.add(listener);
    }

    @Override
    public void removeSynchronizationListener(SynchronizationListener listener) {
        mSyncListeners.remove(listener);
    }

    private void fireSyncStarted(MutableReport r) {
        for (SynchronizationListener l : mSyncListeners) {
            l.onSyncStarted(r);
        }
    }

    private void fireSyncFinished(MutableReport report) {
        for (SynchronizationListener l : mSyncListeners) {
            l.onSyncStopped(report);
        }
    }

    private void closeResources(Closeable... res) {
        for (int i = 0; i < res.length; i++) {
            try {
                res[i].close();
            } catch (Exception e) {
                // Nothing we can do about it
                LOG.severe("Error while closing resource " + res[i]);
            }
        }
    }

}
