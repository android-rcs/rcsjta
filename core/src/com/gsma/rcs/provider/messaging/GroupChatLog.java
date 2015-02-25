/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChat.ReasonCode;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class interfaces the chat table
 */
public class GroupChatLog implements IGroupChatLog {

    private final static String ORDER_BY_TIMESTAMP_DESC = ChatData.KEY_TIMESTAMP.concat(" DESC");

    private final Context mCtx;

    private final LocalContentResolver mLocalContentResolver;

    private final static String SELECT_CHAT_ID = ChatData.KEY_CHAT_ID.concat("=?");

    private final static String SELECT_CHAT_ID_STATUS_REJECTED = new StringBuilder(
            ChatData.KEY_CHAT_ID).append("=? AND ").append(ChatData.KEY_STATE).append("=")
            .append(State.ABORTED.toInt()).append(" AND ").append(ChatData.KEY_REASON_CODE)
            .append("=").append(ReasonCode.ABORTED_BY_USER.toInt()).append(" AND ")
            .append(ChatData.KEY_USER_ABORTION).append("=")
            .append(UserAbortion.SERVER_NOT_NOTIFIED.toInt()).toString();

    private static final String SELECT_ACTIVE_GROUP_CHATS = new StringBuilder(ChatData.KEY_STATE)
            .append("=").append(State.STARTED.toInt()).toString();

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(GroupChatLog.class.getSimpleName());

    private static final int FIRST_COLUMN_IDX = 0;

    private static enum UserAbortion {

        SERVER_NOTIFIED(0), SERVER_NOT_NOTIFIED(1);

        private final int mValue;

        private static SparseArray<UserAbortion> mValueToEnum = new SparseArray<UserAbortion>();
        static {
            for (UserAbortion entry : UserAbortion.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private UserAbortion(int value) {
            mValue = value;
        }

        public final int toInt() {
            return mValue;
        }
    }

    /**
     * Constructor
     * 
     * @param localContentResolver Local content resolver
     */
    /* package private */GroupChatLog(Context ctx, LocalContentResolver localContentResolver) {
        mCtx = ctx;
        mLocalContentResolver = localContentResolver;
    }

    /**
     * Convert participants to string representation
     * 
     * @param participants the participants
     * @return the string with comma separated values of key pairs formatted as follows: "key=value"
     */
    private static String convert(Map<ContactId, ParticipantStatus> participants) {
        StringBuilder builder = new StringBuilder();
        int size = participants.size();

        for (Map.Entry<ContactId, ParticipantStatus> participant : participants.entrySet()) {
            builder.append(participant.getKey());
            builder.append('=');
            builder.append(participant.getValue().toInt());
            if (--size != 0) {

                builder.append(',');
            }
        }
        return builder.toString();
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#addGroupChat(java.lang.String,
     * java.lang.String, java.util.Set, int, int)
     */
    public void addGroupChat(String chatId, ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, State state, ReasonCode reasonCode,
            Direction direction, long timestamp) {
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("addGroupChat; chatID=").append(chatId)
                    .append(", subject=").append(subject).append(", state=").append(state)
                    .append(" reasonCode=").append(reasonCode).append(", direction=")
                    .append(direction).append(", timestamp=").append(timestamp).toString());
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_CHAT_ID, chatId);
        if (contact != null) {
            values.put(ChatData.KEY_CONTACT, contact.toString());
        }
        values.put(ChatData.KEY_STATE, state.toInt());
        values.put(ChatData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(ChatData.KEY_SUBJECT, subject);

        values.put(ChatData.KEY_PARTICIPANTS, convert(participants));
        values.put(ChatData.KEY_DIRECTION, direction.toInt());
        values.put(ChatData.KEY_TIMESTAMP, timestamp);
        values.put(ChatData.KEY_USER_ABORTION, UserAbortion.SERVER_NOTIFIED.toInt());
        mLocalContentResolver.insert(ChatData.CONTENT_URI, values);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#acceptGroupChatNextInvitation(java.lang
     * .String)
     */
    @Override
    public void acceptGroupChatNextInvitation(String chatId) {
        if (logger.isActivated()) {
            logger.debug("acceptGroupChatNextInvitation (chatId=" + chatId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_USER_ABORTION, UserAbortion.SERVER_NOTIFIED.toInt());
        String[] selectionArgs = {
            chatId
        };
        mLocalContentResolver.update(ChatData.CONTENT_URI, values, SELECT_CHAT_ID_STATUS_REJECTED,
                selectionArgs);
        if (logger.isActivated()) {
            logger.debug("acceptGroupChatNextInvitation (chatID=" + chatId + ")");
        }
    }

    @Override
    public void setGroupChatStateAndReasonCode(String chatId, State state, ReasonCode reasonCode) {
        if (logger.isActivated()) {
            logger.debug("updateGroupChatStatus (chatId=" + chatId + ") (state=" + state
                    + ") (reasonCode=" + reasonCode + ")");
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_STATE, state.toInt());
        values.put(ChatData.KEY_REASON_CODE, reasonCode.toInt());
        String selectionArgs[] = new String[] {
            chatId
        };
        mLocalContentResolver.update(ChatData.CONTENT_URI, values, SELECT_CHAT_ID, selectionArgs);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#updateGroupChatParticipant(java.lang.
     * String, java.util.Set)
     */
    @Override
    public void updateGroupChatParticipants(String chatId,
            Map<ContactId, ParticipantStatus> participants) {
        String encodedParticipants = convert(participants);
        if (logger.isActivated()) {
            logger.debug("updateGroupChatParticipant (chatId=" + chatId + ") (participants="
                    + encodedParticipants + ")");
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_PARTICIPANTS, encodedParticipants);
        mLocalContentResolver.update(ChatData.CONTENT_URI, values, ChatData.KEY_CHAT_ID + " = '"
                + chatId + "'", null);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#updateGroupChatRejoinIdOnSessionStart
     * (java.lang.String, java.lang.String)
     */
    @Override
    public void setGroupChatRejoinId(String chatId, String rejoinId) {
        if (logger.isActivated()) {
            logger.debug("Update group chat rejoin ID to " + rejoinId);
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_REJOIN_ID, rejoinId);
        values.put(ChatData.KEY_STATE, State.STARTED.toInt());
        mLocalContentResolver.update(ChatData.CONTENT_URI, values, ChatData.KEY_CHAT_ID + " = '"
                + chatId + "'", null);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#getGroupChatInfo(java.lang.String)
     */
    @Override
    public GroupChatInfo getGroupChatInfo(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat info for " + chatId);
        }
        GroupChatInfo result = null;
        Cursor cursor = null;

        // @formatter:off
        String[] projection = new String[] {
                ChatData.KEY_CHAT_ID, ChatData.KEY_REJOIN_ID, ChatData.KEY_PARTICIPANTS,
                ChatData.KEY_SUBJECT, ChatData.KEY_TIMESTAMP
        };
        // @formatter:on
        String[] selArgs = new String[] {
            chatId
        };
        try {
            cursor = mLocalContentResolver.query(ChatData.CONTENT_URI, projection, SELECT_CHAT_ID,
                    selArgs, ORDER_BY_TIMESTAMP_DESC);
            if (cursor.moveToFirst()) {
                Map<ContactId, ParticipantStatus> participants = GroupChat.getParticipants(mCtx,
                        cursor.getString(2));
                result = new GroupChatInfo(cursor.getString(0), cursor.getString(1), chatId,
                        participants, cursor.getString(3), cursor.getLong(4));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    /**
     * Get the participants associated with a group chat
     * 
     * @param chatId ChatId identifying the group chat
     * @return participants for the group chat
     */
    public Map<ContactId, ParticipantStatus> getParticipants(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat participants for ".concat(chatId));
        }
        try {
            return GroupChat.getParticipants(mCtx,
                    getDataAsString(getGroupChatData(ChatData.KEY_PARTICIPANTS, chatId)));
        } catch (SQLException exception) {
            /* No row returned, that's OK we return no participants. */
            // TODO: will be fixed in CR037
            return new HashMap<ContactId, ParticipantStatus>();
        }
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#isGroupChatNextInviteRejected(java.lang
     * .String)
     */
    @Override
    public boolean isGroupChatNextInviteRejected(String chatId) {
        String[] projection = {
            ChatData.KEY_CHAT_ID
        };
        String[] selectionArgs = {
            chatId
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(ChatData.CONTENT_URI, projection,
                    SELECT_CHAT_ID_STATUS_REJECTED, selectionArgs, ORDER_BY_TIMESTAMP_DESC);
            if (cursor.getCount() != 0) {
                return true;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#getGroupChatData( java.lang.String,
     * java.lang.String)
     */
    private Cursor getGroupChatData(String columnName, String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat info for ".concat(chatId));
        }
        String[] projection = new String[] {
            columnName
        };
        String[] selArgs = new String[] {
            chatId
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(ChatData.CONTENT_URI, projection, SELECT_CHAT_ID,
                    selArgs, ORDER_BY_TIMESTAMP_DESC);
            if (cursor.moveToFirst()) {
                return cursor;
            }

            throw new SQLException(
                    "No row returned while querying for group chat data with chatId : " + chatId);

        } catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    private String getDataAsString(Cursor cursor) {
        try {
            return cursor.getString(FIRST_COLUMN_IDX);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private int getDataAsInt(Cursor cursor) {
        try {
            return cursor.getInt(FIRST_COLUMN_IDX);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private long getDataAsLong(Cursor cursor) {
        try {
            return cursor.getLong(FIRST_COLUMN_IDX);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#getGroupChatState (java.lang.String)
     */
    public State getGroupChatState(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat state for ".concat(chatId));
        }
        return State.valueOf(getDataAsInt(getGroupChatData(ChatData.KEY_STATE, chatId)));
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#getGroupChatReasonCode (java.lang.String)
     */
    public ReasonCode getGroupChatReasonCode(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat reason code for ".concat(chatId));
        }
        return ReasonCode.valueOf(getDataAsInt(getGroupChatData(ChatData.KEY_REASON_CODE, chatId)));
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#setRejectNextGroupChatNextInvitation
     * (java.lang.String)
     */
    public void setRejectNextGroupChatNextInvitation(String chatId) {
        if (logger.isActivated()) {
            logger.debug("setRejectNextGroupChatNextInvitation (chatId=" + chatId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_USER_ABORTION, UserAbortion.SERVER_NOT_NOTIFIED.toInt());
        mLocalContentResolver.update(ChatData.CONTENT_URI, values, ChatData.KEY_CHAT_ID + " = '"
                + chatId + "'", null);
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#
     * retrieveChatIdsOfActiveGroupChatsForAutoRejoin
     */
    public Set<String> getChatIdsOfActiveGroupChatsForAutoRejoin() {
        String[] projection = new String[] {
            ChatData.KEY_CHAT_ID
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(ChatData.CONTENT_URI, projection,
                    SELECT_ACTIVE_GROUP_CHATS, null, null);
            Set<String> activeGroupChats = new HashSet<String>();
            while (cursor.moveToNext()) {
                String chatId = cursor.getString(FIRST_COLUMN_IDX);
                activeGroupChats.add(chatId);
            }
            return activeGroupChats;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.provider.messaging.IGroupChatLog#getCacheableGroupChatData (
     * java.lang.String)
     */
    public Cursor getCacheableGroupChatData(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat info for ".concat(chatId));
        }
        String[] selArgs = new String[] {
            chatId
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(ChatData.CONTENT_URI, null, SELECT_CHAT_ID,
                    selArgs, ORDER_BY_TIMESTAMP_DESC);
            if (cursor.moveToFirst()) {
                return cursor;
            }

            throw new SQLException(
                    "No row returned while querying for group chat data with chatId : " + chatId);

        } catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    @Override
    public Set<ContactId> getGroupChatParticipantsToBeInvited(String chatId) {
        Set<ContactId> participantsToBeInvited = new HashSet<ContactId>();
        Map<ContactId, ParticipantStatus> participants = getParticipants(chatId);
        for (Map.Entry<ContactId, ParticipantStatus> participant : participants.entrySet()) {
            if (ParticipantStatus.INVITE_QUEUED == participant.getValue()) {
                participantsToBeInvited.add(participant.getKey());
            }
        }
        return participantsToBeInvited;
    }
}

