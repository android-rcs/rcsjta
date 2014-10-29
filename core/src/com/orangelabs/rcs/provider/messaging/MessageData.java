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
package com.orangelabs.rcs.provider.messaging;

import android.net.Uri;

import com.gsma.services.rcs.chat.ChatLog;

/**
 * Message data constants
 * 
 * @author Jean-Marc AUFFRET
 */
public class MessageData {
	/**
	 * Database URI
	 */
	protected static final Uri CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.chat/message");
	
	/**
	 * Column name
	 */
	static final String KEY_ID = ChatLog.Message.ID;
	
	/**
	 * Column name
	 */
	static final String KEY_CHAT_ID = ChatLog.Message.CHAT_ID;

	/**
	 * Column name
	 */
	static final String KEY_CONTACT = ChatLog.Message.CONTACT;

	/**
	 * Column name
	 */
	static final String KEY_MSG_ID = ChatLog.Message.MESSAGE_ID;

	/**
	 * Column name
	 */
	static final String KEY_CONTENT = ChatLog.Message.CONTENT;

	/**
	 * Column name
	 */
	static final String KEY_CONTENT_TYPE = ChatLog.Message.MIME_TYPE;

	/**
	 * Column name
	 */
	static final String KEY_DIRECTION = ChatLog.Message.DIRECTION;	

	/**
	 * Column name
	 */
	static final String KEY_STATUS = ChatLog.Message.MESSAGE_STATUS;

	/**
	 * Column name
	 */
	static final String KEY_REASON_CODE = ChatLog.Message.REASON_CODE;

	/**
	 * Column name
	 */
	static final String KEY_READ_STATUS = ChatLog.Message.READ_STATUS;

	/**
	 * Column name
	 */
	static final String KEY_TIMESTAMP = ChatLog.Message.TIMESTAMP;
	
	/**
	 * Column name
	 */
    static final String KEY_TIMESTAMP_SENT = ChatLog.Message.TIMESTAMP_SENT;
    
	/**
	 * Column name
	 */
    static final String KEY_TIMESTAMP_DELIVERED = ChatLog.Message.TIMESTAMP_DELIVERED;
    
	/**
	 * Column name
	 */
    static final String KEY_TIMESTAMP_DISPLAYED = ChatLog.Message.TIMESTAMP_DISPLAYED;
}
