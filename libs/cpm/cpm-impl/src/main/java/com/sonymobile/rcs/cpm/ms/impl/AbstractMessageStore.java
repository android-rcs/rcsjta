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

package com.sonymobile.rcs.cpm.ms.impl;

import com.sonymobile.rcs.cpm.ms.MessageStore;
import com.sonymobile.rcs.cpm.ms.MessageStoreEvent;
import com.sonymobile.rcs.cpm.ms.MessageStoreEvent.EventType;
import com.sonymobile.rcs.cpm.ms.MessageStoreListener;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractMessageStore implements MessageStore {

    private boolean mInited;

    private boolean mAvailable;

    private Collection<MessageStoreListener> mListeners = new ArrayList<MessageStoreListener>();

    protected void setInited(boolean inited) {
        this.mInited = inited;
    }

    protected void setAvailable(boolean a) {
        EventType t = null;
        if (a && !mInited) {
            mInited = true;
            t = EventType.STARTED;
        } else if (a != mAvailable) {
            if (a)
                t = EventType.RECONNECTED;
            else
                t = EventType.DISCONNECTED;
        }
        this.mAvailable = a;
        if (t != null)
            fireMessageStoreEvent(t);
    }

    @Override
    public void addMessageStoreListener(MessageStoreListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void removeMessageStoreListener(MessageStoreListener listener) {
        mListeners.remove(listener);
    }

    private void fireMessageStoreEvent(EventType eventType) {
        for (MessageStoreListener l : mListeners) {
            l.onMessageStoreEvent(new MessageStoreEvent(this, eventType));
        }
    }

}
