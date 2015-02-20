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

import com.gsma.services.rcs.chat.ChatLog;

import android.net.Uri;

/**
 * Chat data constants
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatData {
    /**
     * Database URIs
     */
    protected static final Uri CONTENT_URI = Uri.parse("content://com.gsma.rcs.chat/groupchat");

    /**
     * Base column unique id
     */
    static final String KEY_BASECOLUMN_ID = ChatLog.GroupChat.BASECOLUMN_ID;

    /**
     * Id for chat room
     */
    static final String KEY_CHAT_ID = ChatLog.GroupChat.CHAT_ID;

    /**
     * Column name
     */
    static final String KEY_REJOIN_ID = "rejoin_id";

    /**
     * State of chat room.
     * 
     * @see State
     */
    static final String KEY_STATE = ChatLog.GroupChat.STATE;

    /**
     * Reason code associated with the group chat state.
     * 
     * @see ReasonCode
     */
    static final String KEY_REASON_CODE = ChatLog.GroupChat.REASON_CODE;

    /**
     * Subject of the group chat room
     */
    static final String KEY_SUBJECT = ChatLog.GroupChat.SUBJECT;

    /**
     * List of participants and associated status stored as a String parseable with the
     * ChatLog.GroupChat.getParticipantInfos() method.
     */
    static final String KEY_PARTICIPANTS = ChatLog.GroupChat.PARTICIPANTS;

    /**
     * Status direction of group chat
     * 
     * @see Direction
     */
    static final String KEY_DIRECTION = ChatLog.GroupChat.DIRECTION;

    /**
     * Timestamp of the invitation
     */
    static final String KEY_TIMESTAMP = ChatLog.GroupChat.TIMESTAMP;

    /**
     * Column name : Departed by user
     */
    public static final String KEY_USER_ABORTION = "user_abortion";

    /**
     * ContactId formatted number of the inviter of the group chat or null if this is a group chat
     * initiated by the local user (ie outgoing group chat).
     */
    public static final String KEY_CONTACT = ChatLog.GroupChat.CONTACT;
}
