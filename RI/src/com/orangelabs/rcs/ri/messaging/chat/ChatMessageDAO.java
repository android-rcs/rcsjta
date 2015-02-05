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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;

/**
 * CHAT Message Data Object
 * 
 * @author YPLO6403
 */
public class ChatMessageDAO implements Parcelable {

    private String mMsgId;

    private ContactId mContact;

    private String mChatId;

    private int mStatus;

    private int mReasonCode;

    private ReadStatus mReadStatus;

    private Direction mDirection;

    private String mMimeType;

    private String mContent;

    private long mTimestamp;

    private long timestampSent;

    private long mTimestampDelivered;

    private long mTimestampDisplayed;

    private static final String WHERE_CLAUSE = ChatLog.Message.MESSAGE_ID.concat("=?");

    public int getStatus() {
        return mStatus;
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

    public int getReasonCode() {
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
        mStatus = source.readInt();
        mReadStatus = ReadStatus.valueOf(source.readInt());
        mDirection = Direction.valueOf(source.readInt());
        mTimestamp = source.readLong();
        timestampSent = source.readLong();
        mTimestampDelivered = source.readLong();
        mTimestampDisplayed = source.readLong();
        mReasonCode = source.readInt();
    }

    /**
     * Construct the CHAT Message data object from the provider
     * <p>
     * Note: to change with CR025 (enums)
     * 
     * @param context
     * @param messageId the unique key field
     * @throws Exception
     */
    public ChatMessageDAO(final Context context, final String messageId) throws Exception {
        Uri uri = ChatLog.Message.CONTENT_URI;
        String[] whereArgs = new String[] {
            messageId
        };
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, WHERE_CLAUSE, whereArgs, null);
            if (!cursor.moveToFirst()) {
                throw new IllegalArgumentException("messageId no found");
            }
            mMsgId = messageId;
            mChatId = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CHAT_ID));
            String contact = cursor
                    .getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CONTACT));
            if (contact != null) {
                ContactUtils contactUtils = ContactUtils.getInstance(context);
                mContact = contactUtils.formatContact(contact);
            }
            mMimeType = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.MIME_TYPE));
            mContent = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CONTENT));
            mStatus = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.Message.STATUS));
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
            mReasonCode = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.Message.REASON_CODE));
        } catch (Exception e) {
            throw e;
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
        dest.writeInt(mStatus);
        dest.writeInt(mReadStatus.toInt());
        dest.writeInt(mDirection.toInt());
        dest.writeLong(mTimestamp);
        dest.writeLong(timestampSent);
        dest.writeLong(mTimestampDelivered);
        dest.writeLong(mTimestampDisplayed);
        dest.writeInt(mReasonCode);
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

}
