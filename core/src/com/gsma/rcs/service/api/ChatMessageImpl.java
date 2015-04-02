/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.gsma.rcs.service.api;

import com.gsma.rcs.ExceptionUtil;
import com.gsma.rcs.ServerApiBaseException;
import com.gsma.rcs.ServerApiGenericException;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.contact.ContactId;

import android.os.RemoteException;

public class ChatMessageImpl extends IChatMessage.Stub {

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(ChatMessageImpl.class.getSimpleName());

    private final ChatMessagePersistedStorageAccessor mPersistentStorage;

    /**
     * Constructor
     * 
     * @param persistentStorage ChatMessagePersistedStorageAccessor
     */
    public ChatMessageImpl(ChatMessagePersistedStorageAccessor persistentStorage) {
        mPersistentStorage = persistentStorage;
    }

    public ContactId getContact() throws RemoteException {
        try {
            return mPersistentStorage.getRemoteContact();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    public String getId() {
        return mPersistentStorage.getId();
    }

    public String getContent() {
        return mPersistentStorage.getContent();
    }

    public String getMimeType() {
        return mPersistentStorage.getMimeType();
    }

    public int getDirection() {
        return mPersistentStorage.getDirection().toInt();
    }

    public long getTimestamp() {
        return mPersistentStorage.getTimestamp();
    }

    public long getTimestampSent() {
        return mPersistentStorage.getTimestampSent();
    }

    public long getTimestampDelivered() {
        return mPersistentStorage.getTimestampDelivered();
    }

    public long getTimestampDisplayed() {
        return mPersistentStorage.getTimestampDisplayed();
    }

    public int getStatus() {
        return mPersistentStorage.getStatus().toInt();
    }

    public int getReasonCode() {
        return mPersistentStorage.getReasonCode().toInt();
    }

    public String getChatId() {
        return mPersistentStorage.getChatId();
    }

    public boolean isRead() {
        return mPersistentStorage.isRead();
    }
}
