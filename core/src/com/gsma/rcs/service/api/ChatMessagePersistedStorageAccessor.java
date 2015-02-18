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

import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.utils.ContactUtils;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Status;
import com.gsma.services.rcs.contacts.ContactId;

import android.database.Cursor;

/**
 * ChatMessagePersistedStorageAccessor helps in retrieving persisted data related to a chat message
 * from the persisted storage. It can utilize caching for such data that will not be changed after
 * creation of the group chat to speed up consecutive access.
 */
public class ChatMessagePersistedStorageAccessor {

    private final MessagingLog mMessagingLog;

    private final String mId;

    private ContactId mRemoteContact;

    private String mContent;

    private String mMimeType;

    private String mChatId;

    private boolean mRead;

    private Direction mDirection;

    private long mTimestampDelivered;

    private long mTimestampDisplayed;

    /**
     * Constructor for outgoing message
     * 
     * @param messagingLog MessagingLog
     * @param id Message Id
     */
    public ChatMessagePersistedStorageAccessor(MessagingLog messagingLog, String id) {
        mMessagingLog = messagingLog;
        mId = id;
    }

    /**
     * Constructor for outgoing message
     * 
     * @param messagingLog MessagingLog
     * @param id Message Id
     * @param remoteContact Contact Id
     * @param content Message content
     * @param mimeType Mime type
     * @param chatId Chat ID
     * @param direction Direction
     */
    public ChatMessagePersistedStorageAccessor(MessagingLog messagingLog, String id,
            ContactId remoteContact, String content, String mimeType, String chatId,
            Direction direction) {
        mMessagingLog = messagingLog;
        mId = id;
        mRemoteContact = remoteContact;
        mContent = content;
        mChatId = chatId;
        mMimeType = mimeType;
        mDirection = direction;
    }

    private void cacheData() {
        Cursor cursor = null;
        try {
            cursor = mMessagingLog.getCacheableChatMessageData(mId);
            String contact = cursor.getString(cursor.getColumnIndexOrThrow(Message.CONTACT));
            if (contact != null) {
                mRemoteContact = ContactUtils.createContactId(contact);
            }
            mDirection = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(Message.DIRECTION)));
            mContent = cursor.getString(cursor.getColumnIndexOrThrow(Message.CONTENT));
            mChatId = cursor.getString(cursor.getColumnIndexOrThrow(Message.CHAT_ID));
            mMimeType = cursor.getString(cursor.getColumnIndexOrThrow(Message.MIME_TYPE));
            if (!mRead) {
                mRead = ReadStatus.READ.toInt() == cursor.getInt(cursor
                        .getColumnIndexOrThrow(Message.READ_STATUS));
            }
            if (mTimestampDelivered <= 0) {
                mTimestampDelivered = cursor.getLong(cursor
                        .getColumnIndexOrThrow(Message.TIMESTAMP_DELIVERED));
            }
            if (mTimestampDisplayed <= 0) {
                mTimestampDisplayed = cursor.getLong(cursor
                        .getColumnIndexOrThrow(Message.TIMESTAMP_DISPLAYED));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String getId() {
        return mId;
    }

    public ContactId getRemoteContact() {
        if (mRemoteContact == null) {
            cacheData();
        }
        return mRemoteContact;
    }

    public String getContent() {
        if (mContent == null) {
            cacheData();
        }
        return mContent;
    }

    public String getMimeType() {
        if (mMimeType == null) {
            cacheData();
        }
        return mMimeType;
    }

    public Direction getDirection() {
        if (mDirection == null) {
            cacheData();
        }
        return mDirection;
    }

    public long getTimestamp() {
        return mMessagingLog.getMessageTimestamp(mId);
    }

    public long getTimestampSent() {
        return mMessagingLog.getMessageSentTimestamp(mId);
    }

    public long getTimestampDelivered() {
        /*
         * Utilizing cache here as Timestamp delivered can't be changed in persistent storage after
         * it has been set to some value bigger than zero, so no need to query for it multiple
         * times.
         */
        if (mTimestampDelivered == 0) {
            cacheData();
        }
        return mTimestampDelivered;
    }

    public long getTimestampDisplayed() {
        /*
         * Utilizing cache here as Timestamp displayed can't be changed in persistent storage after
         * it has been set to some value bigger than zero, so no need to query for it multiple
         * times.
         */
        if (mTimestampDisplayed == 0) {
            cacheData();
        }
        return mTimestampDisplayed;
    }

    public Status getStatus() {
        return mMessagingLog.getMessageStatus(mId);
    }

    public ReasonCode getReasonCode() {
        return mMessagingLog.getMessageReasonCode(mId);
    }

    public String getChatId() {
        if (mChatId == null) {
            cacheData();
        }
        return mChatId;
    }

    public boolean isRead() {
        /*
         * No need to read from provider unless incoming and not already marked as read.
         */
        if (Direction.INCOMING == mDirection && !mRead) {
            cacheData();
        }
        return mRead;
    }
}
