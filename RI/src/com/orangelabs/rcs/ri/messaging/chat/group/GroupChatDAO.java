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
import com.gsma.services.rcs.chat.GroupChat.ReasonCode;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.ri.utils.ContactUtil;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Group CHAT Data Object
 * 
 * @author YPLO6403
 */
public class GroupChatDAO {

    private final String mChatId;

    private final Direction mDirection;

    private final ContactId mContact;

    private final String mParticipants;

    private final GroupChat.State mState;

    private final String mSubject;

    private final long mTimestamp;

    private static ContentResolver sContentResolver;

    private final GroupChat.ReasonCode mReasonCode;

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

    public ContactId getContact() {
        return mContact;
    }

    private GroupChatDAO(String chatId, ContactId contact, Direction direction,
            String participants, State state, String subject, long timestamp, ReasonCode reasonCode) {
        mChatId = chatId;
        mContact = contact;
        mDirection = direction;
        mParticipants = participants;
        mState = state;
        mSubject = subject;
        mTimestamp = timestamp;
        mReasonCode = reasonCode;
    }

    @Override
    public String toString() {
        return "GroupChatDAO [chatId=" + mChatId + ", direction=" + mDirection + ", state="
                + mState + ", subject=" + mSubject + ", participants=" + mParticipants + "]";
    }

    /**
     * Gets instance of Group Chat from RCS provider
     * 
     * @param ctx The context
     * @param chatId The chat ID
     * @return instance or null if entry not found
     */
    public static GroupChatDAO getGroupChatDao(Context ctx, String chatId) {
        if (sContentResolver == null) {
            sContentResolver = ctx.getContentResolver();
        }
        Cursor cursor = null;
        try {
            cursor = sContentResolver.query(
                    Uri.withAppendedPath(ChatLog.GroupChat.CONTENT_URI, chatId), null, null, null,
                    null);
            if (!cursor.moveToFirst()) {
                return null;
            }
            String subject = cursor.getString(cursor
                    .getColumnIndexOrThrow(ChatLog.GroupChat.SUBJECT));
            State state = GroupChat.State.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ChatLog.GroupChat.STATE)));
            Direction dir = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ChatLog.GroupChat.DIRECTION)));
            long timestamp = cursor.getLong(cursor
                    .getColumnIndexOrThrow(ChatLog.GroupChat.TIMESTAMP));
            String participants = cursor.getString(cursor
                    .getColumnIndexOrThrow(ChatLog.GroupChat.PARTICIPANTS));
            ReasonCode reasonCode = GroupChat.ReasonCode.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ChatLog.GroupChat.REASON_CODE)));
            String contact = cursor.getString(cursor
                    .getColumnIndexOrThrow(ChatLog.GroupChat.CONTACT));
            ContactId contactId = null;
            if (contact != null) {
                contactId = ContactUtil.formatContact(contact);
            }
            return new GroupChatDAO(chatId, contactId, dir, participants, state, subject,
                    timestamp, reasonCode);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Checks if chatId is a group chat Id
     * 
     * @param chatId the chat ID
     * @param contact The contact
     * @return True chatId is a group chat Id
     */
    public static boolean isGroupChat(String chatId, ContactId contact) {
        return (contact == null) || !chatId.equals(contact.toString());
    }
}
