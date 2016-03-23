/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import java.util.Set;

/**
 * Group chat event listener
 *
 * @author Jean-Marc AUFFRET
 */
public abstract class GroupChatListener {
    /**
     * Callback called when the group chat state is changed
     *
     * @param chatId chat id
     * @param state group chat state
     * @param reasonCode reason code
     */
    public abstract void onStateChanged(String chatId, State state, GroupChat.ReasonCode reasonCode);

    /**
     * Callback called when an Is-composing event has been received. If the remote is typing a
     * message the status is set to true, else it is false.
     *
     * @param chatId the chat ID
     * @param contact the contact ID
     * @param status Is-composing status
     */
    public abstract void onComposingEvent(String chatId, ContactId contact, boolean status);

    /**
     * Callback called when a message status/reasonCode is changed.
     *
     * @param chatId chat id
     * @param mimeType MIME-type
     * @param msgId message id
     * @param status message status
     * @param reasonCode reason code
     */
    public abstract void onMessageStatusChanged(String chatId, String mimeType, String msgId,
            Status status, ReasonCode reasonCode);

    /**
     * Callback called when a group delivery info status/reasonCode was changed for a single
     * recipient to a group message.
     *
     * @param chatId chat id
     * @param contact contact
     * @param mimeType MIME-type
     * @param msgId message id
     * @param status message status
     * @param reasonCode status reason code
     */
    public abstract void onMessageGroupDeliveryInfoChanged(String chatId, ContactId contact,
            String mimeType, String msgId, GroupDeliveryInfo.Status status,
            GroupDeliveryInfo.ReasonCode reasonCode);

    /**
     * Callback called when a participant status has been changed in a group chat.
     *
     * @param chatId chat id
     * @param contact contact id
     * @param status participant status
     */
    public abstract void onParticipantStatusChanged(String chatId, ContactId contact,
            ParticipantStatus status);

    /**
     * Callback called when a delete operation completed that resulted in that one or several group
     * chats was deleted specified by the chatIds parameter.
     *
     * @param chatIds chat ids of those deleted chats
     */
    public abstract void onDeleted(Set<String> chatIds);

    /**
     * Callback called when a delete operation completed that resulted in that one or several group
     * chat messages was deleted specified by the msgIds parameter corresponding to a specific group
     * chat.
     *
     * @param chatId chat id of those deleted messages
     * @param msgIds message ids of those deleted messages
     */
    public abstract void onMessagesDeleted(String chatId, Set<String> msgIds);
}
