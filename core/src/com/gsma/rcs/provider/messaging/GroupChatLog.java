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
import android.net.Uri;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class interfaces the chat table
 */
public class GroupChatLog implements IGroupChatLog {

    private final Context mCtx;

    private final LocalContentResolver mLocalContentResolver;

    private final static String SELECT_CHAT_ID_STATUS_REJECTED = new StringBuilder(
            ChatData.KEY_STATE).append("=").append(State.ABORTED.toInt()).append(" AND ")
            .append(ChatData.KEY_REASON_CODE).append("=")
            .append(ReasonCode.ABORTED_BY_USER.toInt()).append(" AND ")
            .append(ChatData.KEY_USER_ABORTION).append("=")
            .append(UserAbortion.SERVER_NOT_NOTIFIED.toInt()).toString();

    private static final String SELECT_ACTIVE_GROUP_CHATS = new StringBuilder(ChatData.KEY_STATE)
            .append("=").append(State.STARTED.toInt()).toString();

    // @formatter:off
    private static final String[] PROJECTION_GC_INFO = new String[] {
        ChatData.KEY_CHAT_ID, 
        ChatData.KEY_REJOIN_ID, 
        ChatData.KEY_PARTICIPANTS,
        ChatData.KEY_SUBJECT, 
        ChatData.KEY_TIMESTAMP
    };
    // @formatter:on

    private static final String[] PROJECTION_CHAT_ID = new String[] {
        ChatData.KEY_CHAT_ID
    };

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

    @Override
    public void addGroupChat(String chatId, ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, State state, ReasonCode reasonCode,
            Direction direction, long timestamp) {
        String encodedParticipants = convert(participants);
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("addGroupChat; chatID=").append(chatId)
                    .append(", subject=").append(subject).append(", state=").append(state)
                    .append(" reasonCode=").append(reasonCode).append(", direction=")
                    .append(direction).append(", timestamp=").append(timestamp)
                    .append(", participants=").append(encodedParticipants).toString());
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_CHAT_ID, chatId);
        if (contact != null) {
            values.put(ChatData.KEY_CONTACT, contact.toString());
        }
        values.put(ChatData.KEY_STATE, state.toInt());
        values.put(ChatData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(ChatData.KEY_SUBJECT, subject);

        values.put(ChatData.KEY_PARTICIPANTS, encodedParticipants);
        values.put(ChatData.KEY_DIRECTION, direction.toInt());
        values.put(ChatData.KEY_TIMESTAMP, timestamp);
        values.put(ChatData.KEY_USER_ABORTION, UserAbortion.SERVER_NOTIFIED.toInt());
        mLocalContentResolver.insert(ChatData.CONTENT_URI, values);
    }

    @Override
    public void acceptGroupChatNextInvitation(String chatId) {
        if (logger.isActivated()) {
            logger.debug("acceptGroupChatNextInvitation (chatId=" + chatId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_USER_ABORTION, UserAbortion.SERVER_NOTIFIED.toInt());
        mLocalContentResolver.update(Uri.withAppendedPath(ChatData.CONTENT_URI, chatId), values,
                SELECT_CHAT_ID_STATUS_REJECTED, null);
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
        mLocalContentResolver.update(Uri.withAppendedPath(ChatData.CONTENT_URI, chatId), values,
                null, null);
    }

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
        mLocalContentResolver.update(Uri.withAppendedPath(ChatData.CONTENT_URI, chatId), values,
                null, null);
    }

    @Override
    public void setGroupChatRejoinId(String chatId, String rejoinId) {
        if (logger.isActivated()) {
            logger.debug("Update group chat rejoin ID to ".concat(rejoinId));
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_REJOIN_ID, rejoinId);
        values.put(ChatData.KEY_STATE, State.STARTED.toInt());
        mLocalContentResolver.update(Uri.withAppendedPath(ChatData.CONTENT_URI, chatId), values,
                null, null);
    }

    @Override
    public GroupChatInfo getGroupChatInfo(String chatId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(ChatData.CONTENT_URI, chatId), PROJECTION_GC_INFO, null,
                    null, null);
            // TODO check null cursor CR037
            if (!cursor.moveToFirst()) {
                return null;
            }
            int columnIdxChatId = cursor.getColumnIndexOrThrow(ChatData.KEY_CHAT_ID);
            int columnIdxRejoinId = cursor.getColumnIndexOrThrow(ChatData.KEY_REJOIN_ID);
            int columnIdxParticipants = cursor.getColumnIndexOrThrow(ChatData.KEY_PARTICIPANTS);
            int columnIdxSubject = cursor.getColumnIndexOrThrow(ChatData.KEY_SUBJECT);
            int columnIdxTimestamp = cursor.getColumnIndexOrThrow(ChatData.KEY_TIMESTAMP);
            Map<ContactId, ParticipantStatus> participants = GroupChat.getParticipants(mCtx,
                    cursor.getString(columnIdxParticipants));
            return new GroupChatInfo(cursor.getString(columnIdxChatId),
                    cursor.getString(columnIdxRejoinId), chatId, participants,
                    cursor.getString(columnIdxSubject), cursor.getLong(columnIdxTimestamp));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public Map<ContactId, ParticipantStatus> getParticipants(String chatId) {
        Map<ContactId, ParticipantStatus> participants = GroupChat.getParticipants(mCtx,
                getDataAsString(getGroupChatData(ChatData.KEY_PARTICIPANTS, chatId)));
        if (participants == null) {
            return new HashMap<ContactId, ParticipantStatus>();
        }
        return participants;
    }

    @Override
    public boolean isGroupChatNextInviteRejected(String chatId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(ChatData.CONTENT_URI, chatId), PROJECTION_CHAT_ID,
                    SELECT_CHAT_ID_STATUS_REJECTED, null, null);
            // TODO check null cursor CR037
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Cursor getGroupChatData(String columnName, String chatId) {
        String[] projection = new String[] {
            columnName
        };
        Cursor cursor = mLocalContentResolver.query(
                Uri.withAppendedPath(ChatData.CONTENT_URI, chatId), projection, null, null, null);
        // TODO check null cursor CR037
        if (cursor.moveToFirst()) {
            return cursor;
        }
        throw new SQLException(
                "No row returned while querying for group chat data with chatId : ".concat(chatId));
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

    public State getGroupChatState(String chatId) {
        return State.valueOf(getDataAsInt(getGroupChatData(ChatData.KEY_STATE, chatId)));
    }

    public ReasonCode getGroupChatReasonCode(String chatId) {
        return ReasonCode.valueOf(getDataAsInt(getGroupChatData(ChatData.KEY_REASON_CODE, chatId)));
    }

    public void setRejectNextGroupChatNextInvitation(String chatId) {
        if (logger.isActivated()) {
            logger.debug("setRejectNextGroupChatNextInvitation (chatId=" + chatId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_USER_ABORTION, UserAbortion.SERVER_NOT_NOTIFIED.toInt());
        mLocalContentResolver.update(Uri.withAppendedPath(ChatData.CONTENT_URI, chatId), values,
                null, null);
    }

    @Override
    public Set<String> getChatIdsOfActiveGroupChatsForAutoRejoin() {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(ChatData.CONTENT_URI, PROJECTION_CHAT_ID,
                    SELECT_ACTIVE_GROUP_CHATS, null, null);
            // TODO check null cursor CR037
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

    @Override
    public Cursor getCacheableGroupChatData(String chatId) {
        Cursor cursor = mLocalContentResolver.query(
                Uri.withAppendedPath(ChatData.CONTENT_URI, chatId), null, null, null, null);
        // TODO check null cursor CR03
        if (cursor.moveToFirst()) {
            return cursor;
        }
        throw new SQLException(
                "No row returned while querying for group chat data with chatId : ".concat(chatId));
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
