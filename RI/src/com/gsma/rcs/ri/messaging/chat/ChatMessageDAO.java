/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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
 ******************************************************************************/

package com.gsma.rcs.ri.messaging.chat;

import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

/**
 * CHAT Message Data Object
 * 
 * @author YPLO6403
 */
public class ChatMessageDAO {

    private final String mMsgId;
    private final ContactId mContact;
    private final String mChatId;
    private final Message.Content.Status mStatus;
    private final Message.GroupChatEvent.Status mChatEvent;
    private final Message.Content.ReasonCode mReasonCode;
    private final ReadStatus mReadStatus;
    private final Direction mDirection;
    private final String mMimeType;
    private final String mContent;
    private final long mTimestamp;
    private final long mTimestampSent;
    private final long mTimestampDelivered;
    private final long mTimestampDisplayed;
    private static ContentResolver sContentResolver;
    private final boolean mExpiredDelivery;

    public Message.Content.Status getStatus() {
        return mStatus;
    }

    public Message.GroupChatEvent.Status getChatEvent() {
        return mChatEvent;
    }

    public ReadStatus getReadStatus() {
        return mReadStatus;
    }

    public long getTimestampSent() {
        return mTimestampSent;
    }

    public long getTimestampDelivered() {
        return mTimestampDelivered;
    }

    public long getTimestampDisplayed() {
        return mTimestampDisplayed;
    }

    public ContactId getContact() {
        return mContact;
    }

    public String getChatId() {
        return mChatId;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public Direction getDirection() {
        return mDirection;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public String getContent() {
        return mContent;
    }

    public Message.Content.ReasonCode getReasonCode() {
        return mReasonCode;
    }

    public boolean isExpiredDelivery() {
        return mExpiredDelivery;
    }

    @Override
    public String toString() {
        return "ChatMessageDAO [msgId=" + mMsgId + ", contact=" + mContact + ", chatId=" + mChatId
                + ", direction=" + mDirection + ", mimeType=" + mMimeType + ", body='" + mContent
                + "']";
    }

    private ChatMessageDAO(String msgId, ContactId contact, String chatId, Status status,
            GroupChatEvent.Status chatEvent, ReasonCode reasonCode, ReadStatus readStatus,
            Direction direction, String mimeType, String content, long timestamp,
            long timestampSent, long timestampDelivered, long timestampDisplayed,
            boolean expiredDelivery) {
        mMsgId = msgId;
        mContact = contact;
        mChatId = chatId;
        mStatus = status;
        mChatEvent = chatEvent;
        mReasonCode = reasonCode;
        mReadStatus = readStatus;
        mDirection = direction;
        mMimeType = mimeType;
        mContent = content;
        mTimestamp = timestamp;
        mTimestampSent = timestampSent;
        mTimestampDelivered = timestampDelivered;
        mTimestampDisplayed = timestampDisplayed;
        mExpiredDelivery = expiredDelivery;
    }

    /**
     * Gets instance of chat message from RCS provider
     * 
     * @param ctx the context
     * @param msgId the message ID
     * @return instance or null if entry not found
     */
    public static ChatMessageDAO getChatMessageDAO(Context ctx, String msgId) {
        if (sContentResolver == null) {
            sContentResolver = ctx.getContentResolver();
        }
        Cursor cursor = null;
        try {
            cursor = sContentResolver.query(
                    Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, msgId), null, null, null,
                    null);
            if (cursor == null) {
                throw new SQLException("Cannot query chat message ID=" + msgId);
            }
            if (!cursor.moveToFirst()) {
                return null;
            }
            String chatId = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CHAT_ID));
            String contact = cursor
                    .getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CONTACT));
            ContactId contactId = null;
            if (contact != null) {
                contactId = ContactUtil.formatContact(contact);
            }

            String mimeType = cursor.getString(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.MIME_TYPE));
            String content = null;
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.Message.STATUS));
            GroupChatEvent.Status chatEvent = null;
            ReasonCode reasonCode = null;
            Message.Content.Status contentStatus;
            if (Message.MimeType.GROUPCHAT_EVENT.equals(mimeType)) {
                chatEvent = GroupChatEvent.Status.valueOf(status);
                contentStatus = null;
            } else {
                content = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CONTENT));
                contentStatus = Message.Content.Status.valueOf(status);
                reasonCode = Message.Content.ReasonCode.valueOf(cursor.getInt(cursor
                        .getColumnIndexOrThrow(ChatLog.Message.REASON_CODE)));
            }
            ReadStatus readStatus = ReadStatus.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.READ_STATUS)));
            Direction dir = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.DIRECTION)));
            long timestamp = cursor
                    .getLong(cursor.getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP));
            long timestampSent = cursor.getLong(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP_SENT));
            long timestampDelivered = cursor.getLong(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP_DELIVERED));
            long timestampDisplayed = cursor.getLong(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP_DISPLAYED));
            boolean expiredDelivery = cursor.getInt(cursor
                    .getColumnIndexOrThrow(Message.EXPIRED_DELIVERY)) == 1;
            return new ChatMessageDAO(msgId, contactId, chatId, contentStatus, chatEvent,
                    reasonCode, readStatus, dir, mimeType, content, timestamp, timestampSent,
                    timestampDelivered, timestampDisplayed, expiredDelivery);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
