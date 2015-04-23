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

package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.os.RemoteException;
import android.util.Log;

import java.util.HashSet;
import java.util.List;

/**
 * Group chat event listener implementation
 *
 * @hide
 */
public class GroupChatListenerImpl extends IGroupChatListener.Stub {

    private final GroupChatListener mListener;

    private final static String LOG_TAG = GroupChatListenerImpl.class.getName();

    GroupChatListenerImpl(GroupChatListener listener) {
        mListener = listener;
    }

    @Override
    public void onStateChanged(String chatId, int state, int reasonCode) {
        GroupChat.State rcsState;
        GroupChat.ReasonCode rcsReasonCode;
        try {
            rcsState = GroupChat.State.valueOf(state);
            rcsReasonCode = GroupChat.ReasonCode.valueOf(reasonCode);
        } catch (IllegalArgumentException e) {
            /*
             * Detected unknown state or reasonCode not part of standard coming from stack which a
             * client application can of course not handle since it is build only to handle the
             * possible enum values documented and specified in the api standard.
             */
            Log.e(LOG_TAG, e.getMessage());
            return;
        }
        mListener.onStateChanged(chatId, rcsState, rcsReasonCode);
    }

    @Override
    public void onComposingEvent(String chatId, ContactId contact, boolean status)
            throws RemoteException {
        mListener.onComposingEvent(chatId, contact, status);
    }

    @Override
    public void onMessageStatusChanged(String chatId, String mimeType, String msgId, int status,
            int reasonCode) {
        Status rcsStatus;
        Content.ReasonCode rcsReasonCode;
        try {
            rcsStatus = Status.valueOf(status);
            rcsReasonCode = Content.ReasonCode.valueOf(reasonCode);
        } catch (IllegalArgumentException e) {
            /*
             * Detected unknown state or reasonCode not part of standard coming from stack which a
             * client application can of course not handle since it is build only to handle the
             * possible enum values documented and specified in the api standard.
             */
            Log.e(LOG_TAG, e.getMessage());
            return;
        }
        mListener.onMessageStatusChanged(chatId, mimeType, msgId, rcsStatus, rcsReasonCode);
    }

    @Override
    public void onMessageGroupDeliveryInfoChanged(String chatId, ContactId contact,
            String mimeType, String msgId, int status, int reasonCode) {
        GroupDeliveryInfo.Status rcsStatus;
        GroupDeliveryInfo.ReasonCode rcsReasonCode;
        try {
            rcsStatus = GroupDeliveryInfo.Status.valueOf(status);
            rcsReasonCode = GroupDeliveryInfo.ReasonCode.valueOf(reasonCode);
        } catch (IllegalArgumentException e) {
            /*
             * Detected unknown state or reasonCode not part of standard coming from stack which a
             * client application can of course not handle since it is build only to handle the
             * possible enum values documented and specified in the api standard.
             */
            Log.e(LOG_TAG, e.getMessage());
            return;
        }
        mListener.onMessageGroupDeliveryInfoChanged(chatId, contact, mimeType, msgId, rcsStatus,
                rcsReasonCode);
    }

    @Override
    public void onParticipantStatusChanged(String chatId, ContactId contact, int status) {
        try {
            mListener
                    .onParticipantStatusChanged(chatId, contact, ParticipantStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            /*
             * Detected unknown status not part of standard coming from stack which a client
             * application can not handle since it is built only to handle the possible enum values
             * documented and specified in the api standard.
             */
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    /**
     * This feature to be implemented in CR005
     */
    @Override
    public void onDeleted(List<String> chatIds) throws RemoteException {
        mListener.onDeleted(new HashSet<String>(chatIds));
    }

    /**
     * This feature to be implemented in CR005
     */
    @Override
    public void onMessagesDeleted(String chatId, List<String> msgIds) throws RemoteException {
        mListener.onMessagesDeleted(chatId, new HashSet<String>(msgIds));
    }
}
