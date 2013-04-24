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

package com.orangelabs.rcs.provider.messaging;

import android.net.Uri;

/**
 * Rich messaging history data constants
 * 
 * @author mhsm6403
 */
public class RichMessagingData {
	// Database URI
	public static final Uri CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.messaging/messaging");
	
	// Fields for chat
	public static final String KEY_ID = "_id";
	public static final String KEY_TYPE = "type";
	public static final String KEY_CHAT_SESSION_ID = "chat_session_id";
	public static final String KEY_TIMESTAMP = "_date";
	public static final String KEY_CONTACT = "contact";
	public static final String KEY_STATUS = "status";
	public static final String KEY_DATA = "_data";
	public static final String KEY_MESSAGE_ID = "message_id";
	public static final String KEY_IS_SPAM = "is_spam";
	public static final String KEY_CHAT_ID = "chat_id";
	public static final String KEY_CHAT_REJOIN_ID = "chat_rejoin_id";
	
	// Fields for file transfer
	public static final String KEY_MIME_TYPE = "mime_type";
	public static final String KEY_NAME = "name";
	public static final String KEY_SIZE = "size";
	public static final String KEY_TOTAL_SIZE = "total_size";	
	
	public static final String KEY_NUMBER_MESSAGES ="number_of_messages";
}
