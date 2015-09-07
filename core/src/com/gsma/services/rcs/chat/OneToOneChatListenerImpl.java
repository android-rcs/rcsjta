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

package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.contact.ContactId;

import android.os.RemoteException;
import android.util.Log;

import java.util.HashSet;
import java.util.List;

/**
 * Chat event listener implementation
 * 
 * @hide
 */
public class OneToOneChatListenerImpl extends IOneToOneChatListener.Stub {

    private final OneToOneChatListener mListener;

    private final static String LOG_TAG = OneToOneChatListenerImpl.class.getSimpleName();

    OneToOneChatListenerImpl(OneToOneChatListener listener) {
        mListener = listener;
    }

    @Override
    public void onMessageStatusChanged(ContactId contact, String mimeType, String msgId,
            int status, int reasonCode) {
        Status rcsStatus;
        ReasonCode rcsReasonCode;
        try {
            rcsStatus = Status.valueOf(status);
            rcsReasonCode = ReasonCode.valueOf(reasonCode);
        } catch (IllegalArgumentException e) {
            /*
             * Detected unknown state or reasonCode not part of standard coming from stack which a
             * client application can of course not handle since it is build only to handle the
             * possible enum values documented and specified in the api standard.
             */
            Log.e(LOG_TAG, e.getMessage());
            return;
        }
        mListener.onMessageStatusChanged(contact, mimeType, msgId, rcsStatus, rcsReasonCode);
    }

    @Override
    public void onComposingEvent(ContactId contact, boolean status) throws RemoteException {
        mListener.onComposingEvent(contact, status);
    }

    /**
     * This feature to be implemented in CR005
     */
    @Override
    public void onMessagesDeleted(ContactId contact, List<String> msgIds) throws RemoteException {
        mListener.onMessagesDeleted(contact, new HashSet<String>(msgIds));
    }
}
