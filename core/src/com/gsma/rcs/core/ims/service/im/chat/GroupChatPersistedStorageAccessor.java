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

package com.gsma.rcs.core.ims.service.im.chat;

import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtils;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.GroupChat;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.chat.ChatLog.Message.Status;
import com.gsma.services.rcs.chat.GroupChat.ReasonCode;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;

import android.database.Cursor;

import java.util.Set;

/**
 * GroupChatPersistedStorageAccessor helps in retrieving persisted data related to a group chat from
 * the persisted storage. It can utilize caching for such data that will not be changed after
 * creation of the group chat to speed up consecutive access.
 */
public class GroupChatPersistedStorageAccessor {

    private final String mChatId;

    private final MessagingLog mMessagingLog;

    private String mSubject;

    private Direction mDirection;

    private ContactId mContact;

    public GroupChatPersistedStorageAccessor(String chatId, MessagingLog messagingLog) {
        mChatId = chatId;
        mMessagingLog = messagingLog;
    }

    public GroupChatPersistedStorageAccessor(String chatId, String subject, Direction direction,
            MessagingLog messagingLog) {
        mChatId = chatId;
        mSubject = subject;
        mDirection = direction;
        mMessagingLog = messagingLog;
    }

    private void cacheData() {
        Cursor cursor = null;
        try {
            cursor = mMessagingLog.getCacheableGroupChatData(mChatId);
            mSubject = cursor.getString(cursor.getColumnIndexOrThrow(GroupChat.SUBJECT));
            mDirection = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(GroupChat.DIRECTION)));
            String contact = cursor.getString(cursor.getColumnIndexOrThrow(GroupChat.CONTACT));
            if (contact != null) {
                mContact = ContactUtils.createContactId(contact);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public Direction getDirection() {
        /*
         * Utilizing cache here as direction can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mDirection == null) {
            cacheData();
        }
        return mDirection;
    }

    public int getState() {
        return mMessagingLog.getGroupChatState(mChatId);
    }

    public int getReasonCode() {
        return mMessagingLog.getGroupChatReasonCode(mChatId);
    }

    public String getSubject() {
        /*
         * Utilizing cache here as subject can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mSubject == null) {
            cacheData();
        }
        return mSubject;
    }

    public ContactId getRemoteContact() {
        /* Remote contact is null for outgoing group chat */
        if (Direction.OUTGOING == getDirection()) {
            return null;
        }
        /*
         * Utilizing cache here as remote contact can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mContact == null) {
            cacheData();
        }
        return mContact;
    }

    public Set<ParticipantInfo> getParticipants() {
        return mMessagingLog.getGroupChatParticipants(mChatId);
    }

    public int getMaxParticipants() {
        return RcsSettings.getInstance().getMaxChatParticipants();
    }

    public void setStateAndReasonCode(State state, ReasonCode reasonCode) {
        mMessagingLog.setGroupChatStateAndReasonCode(mChatId, state, reasonCode);
    }

    public void setMessageStatusAndReasonCode(String msgId, Status status,
            Message.ReasonCode reasonCode) {
        mMessagingLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode);
    }

    public void setDeliveryInfoStatusAndReasonCode(String msgId, ContactId contact, int status,
            int reasonCode) {
        mMessagingLog.setGroupChatDeliveryInfoStatusAndReasonCode(msgId, contact, status,
                reasonCode);
    }

    public boolean isDeliveredToAllRecipients(String msgId) {
        return mMessagingLog.isDeliveredToAllRecipients(msgId);
    }

    public boolean isDisplayedByAllRecipients(String msgId) {
        return mMessagingLog.isDisplayedByAllRecipients(msgId);
    }

    public void setRejoinId(String rejoinId) {
        mMessagingLog.setGroupChatRejoinId(mChatId, rejoinId);
    }

    public void addGroupChat(ContactId contact, String subject, Set<ParticipantInfo> participants,
            State state, ReasonCode reasonCode, Direction direction) {
        mMessagingLog.addGroupChat(mChatId, contact, subject, participants, state, reasonCode,
                direction);
    }

    public void addGroupChatEvent(String chatId, ContactId contact, GroupChatEvent event) {
        mMessagingLog.addGroupChatEvent(mChatId, contact, event);
    }

    public void addGroupChatMessage(ChatMessage msg, Direction direction, Status status,
            Message.ReasonCode reasonCode) {
        mMessagingLog.addGroupChatMessage(mChatId, msg, direction, status, reasonCode);
    }

    public void setRejectNextGroupChatNextInvitation() {
        mMessagingLog.setRejectNextGroupChatNextInvitation(mChatId);
    }

    public void setFileTransferStateAndReasonCode(String fileTransferId, int state, int reasonCode) {
        mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId, state, reasonCode);
    }
}
