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

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;

/**
 * Message data constants
 * 
 * @author Jean-Marc AUFFRET
 */
public class MessageData {

    /**
     * Unique history ID
     */
    static final String KEY_BASECOLUMN_ID = ChatLog.Message.BASECOLUMN_ID;

    /**
     * Id of chat room
     */
    static final String KEY_CHAT_ID = ChatLog.Message.CHAT_ID;

    /**
     * ContactId formatted number of remote contact or null if the message is an outgoing group chat
     * message.
     */
    static final String KEY_CONTACT = ChatLog.Message.CONTACT;

    /**
     * Id of the message
     */
    static final String KEY_MESSAGE_ID = ChatLog.Message.MESSAGE_ID;

    /**
     * Content of the message (as defined by one of the mimetypes in ChatLog.Message.Mimetype)
     */
    static final String KEY_CONTENT = ChatLog.Message.CONTENT;

    /**
     * Multipurpose Internet Mail Extensions (MIME) type of message
     */
    static final String KEY_MIME_TYPE = ChatLog.Message.MIME_TYPE;

    /**
     * Status direction of message.
     * 
     * @see Direction
     */
    static final String KEY_DIRECTION = ChatLog.Message.DIRECTION;

    /**
     * @see Status
     */
    static final String KEY_STATUS = ChatLog.Message.STATUS;

    /**
     * Reason code associated with the message status.
     * 
     * @see ReasonCode
     */
    static final String KEY_REASON_CODE = ChatLog.Message.REASON_CODE;

    /**
     * This is set on the receiver side when the message has been displayed.
     * 
     * @see com.gsma.services.rcs.RcsCommon.ReadStatus for the list of status.
     */
    static final String KEY_READ_STATUS = ChatLog.Message.READ_STATUS;

    /**
     * Time when message inserted
     */
    static final String KEY_TIMESTAMP = ChatLog.Message.TIMESTAMP;

    /**
     * Time when message sent. If 0 means not sent.
     */
    static final String KEY_TIMESTAMP_SENT = ChatLog.Message.TIMESTAMP_SENT;

    /**
     * Time when message delivered. If 0 means not delivered
     */
    static final String KEY_TIMESTAMP_DELIVERED = ChatLog.Message.TIMESTAMP_DELIVERED;

    /**
     * Time when message displayed. If 0 means not displayed.
     */
    static final String KEY_TIMESTAMP_DISPLAYED = ChatLog.Message.TIMESTAMP_DISPLAYED;
}
