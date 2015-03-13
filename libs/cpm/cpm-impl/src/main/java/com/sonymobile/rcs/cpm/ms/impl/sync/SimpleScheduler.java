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

import com.sonymobile.rcs.cpm.ms.MessageStore;
import com.sonymobile.rcs.cpm.ms.MessageStoreEvent;
import com.sonymobile.rcs.cpm.ms.MessageStoreListener;
import com.sonymobile.rcs.cpm.ms.sync.ExecutionTrigger;
import com.sonymobile.rcs.cpm.ms.sync.SyncMediator;

import java.util.HashSet;
import java.util.Set;

public class SimpleScheduler implements SyncScheduler, MessageStoreListener, Runnable {

    private Set<ExecutionTrigger> mSyncTriggers = new HashSet<ExecutionTrigger>();

    private long mInterval = 10000;

    private SyncMediator mMediator;

    private Thread mThread = null;

    private boolean mActive = true;

    @Override
    public long getInterval() {
        return mInterval;
    }

    @Override
    public void setInterval(long time) {
        mInterval = time;
    }

    @Override
    public boolean isActive() {
        return mActive;
    }

    @Override
    public synchronized void setActive(boolean active) {
        if (!active && mThread != null && mThread.isAlive()) {
            mThread.interrupt();
            mThread = null;
        } else if (active) {
            start();
        }
        mActive = active;
    }

    /**
     * Override if the process responsible of the execution is
     */
    @Override
    public void requestExecution() {
        mMediator.execute();
    }

    @Override
    public void setMediator(SyncMediator mediator) {
        mMediator = mediator;
        mMediator.getLocalStore().addMessageStoreListener(this);
        mMediator.getRemoteStore().addMessageStoreListener(this);
    }

    @Override
    public void run() {
        while (true) {
            if (!hasExecutionTrigger(ExecutionTrigger.ON_INTERVAL)) {
                break;
            }

            requestExecution();

            try {
                Thread.sleep(mInterval);
            } catch (InterruptedException e) {
                break;
            }
        }
        stop();
    }

    private synchronized void start() {
        // stop for restart
        stop();

        if (hasExecutionTrigger(ExecutionTrigger.ON_INTERVAL)) {
            mThread = new Thread(this);
            mThread.start();
        }
    }

    private void stop() {
        if (mThread != null && mThread.isAlive()) {
            mThread.interrupt();
            mThread = null;
        }
    }

    @Override
    public boolean hasExecutionTrigger(ExecutionTrigger t) {
        return mSyncTriggers.contains(t);
    }

    @Override
    public void addExecutionTriggers(ExecutionTrigger... triggers) {
        for (ExecutionTrigger t : triggers) {
            mSyncTriggers.add(t);
        }
        if (hasExecutionTrigger(ExecutionTrigger.ON_INTERVAL)) {
            start();
        }
    }

    @Override
    public void removeExecutionTriggers(ExecutionTrigger... triggers) {
        for (ExecutionTrigger t : triggers) {
            mSyncTriggers.remove(t);
        }
        if (!hasExecutionTrigger(ExecutionTrigger.ON_INTERVAL)) {
            stop();
        }
    }

    @Override
    public void onMessageStoreEvent(MessageStoreEvent event) {
        switch (event.getType()) {
            case STARTED:
                if (hasExecutionTrigger(ExecutionTrigger.ON_INIT)) {
                    requestExecution();
                }
                break;
            case DISCONNECTED:
                break;
            case RECONNECTED:
                if (((MessageStore) event.getSource()).isConnected()
                        && hasExecutionTrigger(ExecutionTrigger.ON_RECONNECT)) {
                    requestExecution();
                }
                break;
            case STOPPED:

                break;
            case CHANGED:
                if (hasExecutionTrigger(ExecutionTrigger.ON_NOTIFY)) {
                    requestExecution();
                }
                break;
            default:
                break;
        }
    }

}
