/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.orangelabs.rcs.ri.messaging.chat;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.LogUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * CHAT Message Data Object
 * 
 * @author YPLO6403
 */
public class ChatMessageDAO implements Parcelable {

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

    private final long timestampSent;

    private final long mTimestampDelivered;

    private final long mTimestampDisplayed;

    private static ContentResolver sContentResolver;

    private static final String LOGTAG = LogUtils.getTag(ChatMessageDAO.class.getSimpleName());

    public Message.Content.Status getStatus() {
        return mStatus;
    }

    public Message.GroupChatEvent.Status getChatEvent() {
        return mChatEvent;
    }

    public String getMsgId() {
        return mMsgId;
    }

    public ReadStatus getReadStatus() {
        return mReadStatus;
    }

    public long getTimestampSent() {
        return timestampSent;
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

    /**
     * Constructor
     * 
     * @param source Parcelable source
     */
    public ChatMessageDAO(Parcel source) {
        mMsgId = source.readString();
        boolean containsContactId = source.readInt() != 0;
        if (containsContactId) {
            mContact = ContactId.CREATOR.createFromParcel(source);
        } else {
            mContact = null;
        }
        mChatId = source.readString();
        mMimeType = source.readString();
        mContent = source.readString();
        if (Message.MimeType.GROUPCHAT_EVENT.equals(mMimeType)) {
            mChatEvent = GroupChatEvent.Status.valueOf(source.readInt());
            mStatus = null;
        } else {
            mStatus = Message.Content.Status.valueOf(source.readInt());
            mChatEvent = null;
        }
        mReadStatus = ReadStatus.valueOf(source.readInt());
        mDirection = Direction.valueOf(source.readInt());
        mTimestamp = source.readLong();
        timestampSent = source.readLong();
        mTimestampDelivered = source.readLong();
        mTimestampDisplayed = source.readLong();
        mReasonCode = Message.Content.ReasonCode.valueOf(source.readInt());
    }

    private ChatMessageDAO(ContentResolver resolver, String messageId) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, messageId),
                    null, null, null, null);
            if (!cursor.moveToFirst()) {
                throw new SQLException("Failed to find message with ID: ".concat(messageId));
            }
            mMsgId = messageId;
            mChatId = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CHAT_ID));
            String contact = cursor
                    .getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CONTACT));
            if (contact != null) {
                mContact = ContactUtil.formatContact(contact);
            } else {
                /* outgoing group chat message */
                mContact = null;
            }

            mMimeType = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.MIME_TYPE));
            mContent = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CONTENT));
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.Message.STATUS));
            if (Message.MimeType.GROUPCHAT_EVENT.equals(mMimeType)) {
                mChatEvent = GroupChatEvent.Status.valueOf(status);
                mStatus = null;
            } else {
                mStatus = Message.Content.Status.valueOf(status);
                mChatEvent = null;
            }

            mReadStatus = ReadStatus.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.READ_STATUS)));
            mDirection = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.DIRECTION)));
            mTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP));
            timestampSent = cursor.getLong(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP_SENT));
            mTimestampDelivered = cursor.getLong(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP_DELIVERED));
            mTimestampDisplayed = cursor.getLong(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP_DISPLAYED));
            mReasonCode = Message.Content.ReasonCode.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.REASON_CODE)));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mMsgId);
        if (mContact != null) {
            dest.writeInt(1);
            mContact.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeString(mChatId);
        dest.writeString(mMimeType);
        dest.writeString(mContent);
        if (Message.MimeType.GROUPCHAT_EVENT.equals(mMimeType)) {
            dest.writeInt(mChatEvent.toInt());
        } else {
            dest.writeInt(mStatus.toInt());
        }
        dest.writeInt(mReadStatus.toInt());
        dest.writeInt(mDirection.toInt());
        dest.writeLong(mTimestamp);
        dest.writeLong(timestampSent);
        dest.writeLong(mTimestampDelivered);
        dest.writeLong(mTimestampDisplayed);
        dest.writeInt(mReasonCode.toInt());
    };

    public static final Parcelable.Creator<ChatMessageDAO> CREATOR = new Parcelable.Creator<ChatMessageDAO>() {
        @Override
        public ChatMessageDAO createFromParcel(Parcel in) {
            return new ChatMessageDAO(in);
        }

        @Override
        public ChatMessageDAO[] newArray(int size) {
            return new ChatMessageDAO[size];
        }
    };

    @Override
    public String toString() {
        return "ChatMessageDAO [msgId=" + mMsgId + ", contact=" + mContact + ", chatId=" + mChatId
                + ", direction=" + mDirection + ", mimeType=" + mMimeType + ", body='" + mContent
                + "']";
    }

    /**
     * Gets instance of chat message from RCS provider
     * 
     * @param context
     * @param msgId
     * @return instance or null if entry not found
     */
    public static ChatMessageDAO getChatMessageDAO(Context context, String msgId) {
        if (sContentResolver == null) {
            sContentResolver = context.getContentResolver();
        }
        try {
            return new ChatMessageDAO(sContentResolver, msgId);
        } catch (SQLException e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, e.getMessage());
            }
            return null;
        }
    }

}
