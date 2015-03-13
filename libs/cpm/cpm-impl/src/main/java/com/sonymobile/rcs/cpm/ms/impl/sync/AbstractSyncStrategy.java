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

import com.sonymobile.rcs.cpm.ms.CpmMessageStoreException;
import com.sonymobile.rcs.cpm.ms.MessageStore;
import com.sonymobile.rcs.cpm.ms.sync.MutableReport;
import com.sonymobile.rcs.cpm.ms.sync.SyncOperationNotAllowed;
import com.sonymobile.rcs.cpm.ms.sync.SyncStrategy;

public abstract class AbstractSyncStrategy implements SyncStrategy {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    protected MessageStore localStore;

    protected MessageStore remoteStore;

    protected MutableReport report;

    @Override
    public void execute(MessageStore localStore, MessageStore remoteStore, MutableReport report)
            throws CpmMessageStoreException {
        this.localStore = localStore;
        this.remoteStore = remoteStore;
        this.report = report;

        execute();

        // TODO clear exception handling
        localStore.applyChanges();
        remoteStore.applyChanges();

        report.setProgress(report.getProgressMax());
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void cancel() throws CpmMessageStoreException {
        throw new SyncOperationNotAllowed("Not implemented for this strategy :" + getName());
    }

    @Override
    public void pause() throws CpmMessageStoreException {
        throw new SyncOperationNotAllowed("Not implemented for this strategy :" + getName());
    }

    @Override
    public void resume() throws CpmMessageStoreException {
        throw new SyncOperationNotAllowed("Not implemented for this strategy :" + getName());
    }

    public abstract void execute() throws CpmMessageStoreException;

}
