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

package com.orangelabs.rcs.ri.messaging.chat.group;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.GroupChat;

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
 * Group CHAT Data Object
 * 
 * @author YPLO6403
 */
public class GroupChatDAO implements Parcelable {

    private final String mChatId;

    private final Direction mDirection;

    private final String mParticipants;

    private final GroupChat.State mState;

    private final String mSubject;

    private final long mTimestamp;

    private static ContentResolver sContentResolver;

    private final GroupChat.ReasonCode mReasonCode;

    private static final String LOGTAG = LogUtils.getTag(GroupChatDAO.class.getSimpleName());

    private static final String[] PROJECTION_CHAT_ID = new String[] {
        ChatLog.GroupChat.CHAT_ID
    };

    public GroupChat.State getState() {
        return mState;
    }

    public String getChatId() {
        return mChatId;
    }

    public String getParticipants() {
        return mParticipants;
    }

    public String getSubject() {
        return mSubject;
    }

    public Direction getDirection() {
        return mDirection;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public GroupChat.ReasonCode getReasonCode() {
        return mReasonCode;
    }

    /**
     * Constructor
     * 
     * @param source Parcelable source
     */
    public GroupChatDAO(Parcel source) {
        mChatId = source.readString();
        mState = GroupChat.State.valueOf(source.readInt());
        mDirection = Direction.valueOf(source.readInt());
        mTimestamp = source.readLong();
        mSubject = source.readString();
        mParticipants = source.readString();
        mReasonCode = GroupChat.ReasonCode.valueOf(source.readInt());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mChatId);
        dest.writeInt(mState.toInt());
        dest.writeInt(mDirection.toInt());
        dest.writeLong(mTimestamp);
        dest.writeString(mSubject);
        dest.writeString(mParticipants);
        dest.writeInt(mReasonCode.toInt());
    };

    /**
     * Construct the Group CHAT data object from the provider
     * <p>
     * Note: to change with CR025 (enums)
     * 
     * @param resolver
     * @param chatId
     */
    private GroupChatDAO(ContentResolver resolver, String chatId) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(Uri.withAppendedPath(ChatLog.GroupChat.CONTENT_URI, chatId),
                    null, null, null, null);
            if (!cursor.moveToFirst()) {
                throw new SQLException("Failed to find group chat with ID: ".concat(chatId));
            }
            mChatId = chatId;
            mSubject = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.SUBJECT));
            mState = GroupChat.State.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ChatLog.GroupChat.STATE)));
            mDirection = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ChatLog.GroupChat.DIRECTION)));
            mTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.TIMESTAMP));
            mParticipants = cursor.getString(cursor
                    .getColumnIndexOrThrow(ChatLog.GroupChat.PARTICIPANTS));
            mReasonCode = GroupChat.ReasonCode.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ChatLog.GroupChat.REASON_CODE)));
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

    public static final Parcelable.Creator<GroupChatDAO> CREATOR = new Parcelable.Creator<GroupChatDAO>() {
        @Override
        public GroupChatDAO createFromParcel(Parcel in) {
            return new GroupChatDAO(in);
        }

        @Override
        public GroupChatDAO[] newArray(int size) {
            return new GroupChatDAO[size];
        }
    };

    @Override
    public String toString() {
        return "GroupChatDAO [chatId=" + mChatId + ", direction=" + mDirection + ", state="
                + mState + ", subject=" + mSubject + ", participants=" + mParticipants + "]";
    }

    /**
     * Gets instance of Group Chat from RCS provider
     * 
     * @param context
     * @param chatId
     * @return instance or null if entry not found
     */
    public static GroupChatDAO getGroupChatDao(Context context, String chatId) {
        if (sContentResolver == null) {
            sContentResolver = context.getContentResolver();
        }
        try {
            return new GroupChatDAO(sContentResolver, chatId);
        } catch (SQLException e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, e.getMessage());
            }
            return null;
        }
    }

    /**
     * Checks if group chat exists for chat ID
     * 
     * @param context
     * @param chatId
     * @return true if group chat exists for chat ID
     */
    public static boolean isGroupChat(Context context, String chatId) {
        if (sContentResolver == null) {
            sContentResolver = context.getContentResolver();
        }
        Cursor cursor = null;
        try {
            cursor = sContentResolver.query(
                    Uri.withAppendedPath(ChatLog.GroupChat.CONTENT_URI, chatId),
                    PROJECTION_CHAT_ID, null, null, null);
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
