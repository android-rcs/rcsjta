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
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.GroupDeliveryInfo;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.GroupChat;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChat.ReasonCode;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import android.database.Cursor;

import java.util.Map;

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

    private long mTimestamp;

    private final RcsSettings mRcsSettings;

    public GroupChatPersistedStorageAccessor(String chatId, MessagingLog messagingLog,
            RcsSettings rcsSettings) {
        mChatId = chatId;
        mMessagingLog = messagingLog;
        mRcsSettings = rcsSettings;
    }

    public GroupChatPersistedStorageAccessor(String chatId, String subject, Direction direction,
            MessagingLog messagingLog, RcsSettings rcsSettings, long timestamp) {
        mChatId = chatId;
        mSubject = subject;
        mDirection = direction;
        mMessagingLog = messagingLog;
        mRcsSettings = rcsSettings;
        mTimestamp = timestamp;
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
                mContact = ContactUtil.createContactIdFromTrustedData(contact);
            }
            mTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(GroupChat.TIMESTAMP));
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

    public State getState() {
        return mMessagingLog.getGroupChatState(mChatId);
    }

    public ReasonCode getReasonCode() {
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

    public long getTimestamp() {
        /*
         * Utilizing cache here as timestamp can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mTimestamp == 0) {
            cacheData();
        }
        return mTimestamp;
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

    public Map<ContactId, ParticipantStatus> getParticipants() {
        return mMessagingLog.getParticipants(mChatId);
    }

    public int getMaxParticipants() {
        return mRcsSettings.getMaxChatParticipants();
    }

    public void setStateAndReasonCode(State state, ReasonCode reasonCode) {
        mMessagingLog.setGroupChatStateAndReasonCode(mChatId, state, reasonCode);
    }

    public void setMessageStatusAndReasonCode(String msgId, Status status,
            Content.ReasonCode reasonCode) {
        mMessagingLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode);
    }

    public void setDeliveryInfoStatusAndReasonCode(String msgId, ContactId contact,
            GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode) {
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

    public void addGroupChat(ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, State state, ReasonCode reasonCode,
            Direction direction, long timestamp) {
        mContact = contact;
        mSubject = subject;
        mDirection = direction;
        mTimestamp = timestamp;
        mMessagingLog.addGroupChat(mChatId, contact, subject, participants, state, reasonCode,
                direction, timestamp);
    }

    public void addGroupChatEvent(String chatId, ContactId contact, GroupChatEvent.Status status,
            long timestamp) {
        mContact = contact;
        mTimestamp = timestamp;
        mMessagingLog.addGroupChatEvent(mChatId, contact, status, timestamp);
    }

    public void addGroupChatMessage(ChatMessage msg, Direction direction, Status status,
            Content.ReasonCode reasonCode) {
        mMessagingLog.addGroupChatMessage(mChatId, msg, direction, status, reasonCode);
    }

    public void setRejectNextGroupChatNextInvitation() {
        mMessagingLog.setRejectNextGroupChatNextInvitation(mChatId);
    }

    public void setFileTransferStateAndReasonCode(String fileTransferId, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode) {
        mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId, state, reasonCode);
    }

    public void dequeueChatMessage(ChatMessage message) {
        mMessagingLog.dequeueChatMessage(message);
    }

    public void setParticipantsStateAndReasonCode(Map<ContactId, ParticipantStatus> participants,
            State state, ReasonCode reasonCode) {
        mMessagingLog.setGroupChatParticipantsStateAndReasonCode(participants, mChatId, state,
                reasonCode);
    }
}
