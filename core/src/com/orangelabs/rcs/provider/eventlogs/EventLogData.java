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

package com.orangelabs.rcs.provider.eventlogs;

import android.net.Uri;
import android.provider.CallLog.Calls;

/**
 * Event log data constants
 * 
 * @author mhsm6403
 */
public class EventLogData {
	
	// Virtual Database URI
	public static final Uri CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.eventlogs/");

	public static final String SMS_MIMETYPE = "sms/text";
	public static final String MMS_MIMETYPE = "mms/text";

	public static final Uri SMS_URI = Uri.parse("content://sms");
	public static final Uri MMS_URI = Uri.parse("content://mms");
	
	/**
	 * Metadata of the event cursor. 
	 */
	public static final String KEY_EVENT_ROW_ID = "_id";
	public static final String KEY_EVENT_TYPE = "type";
	public static final String KEY_EVENT_SESSION_ID = "session_id";
	public static final String KEY_EVENT_DATE = "_date";
	public static final String KEY_EVENT_CONTACT = "contact";
	public static final String KEY_EVENT_STATUS = "status";
	public static final String KEY_EVENT_DATA = "_data";
	public static final String KEY_EVENT_MESSAGE_ID = "message_id";
	public static final String KEY_EVENT_IS_SPAM = "is_spam";
	public static final String KEY_EVENT_MIMETYPE = "mime_type";
	public static final String KEY_EVENT_NAME = "name";
	public static final String KEY_EVENT_SIZE = "size";
	public static final String KEY_EVENT_TOTAL_SIZE = "total_size";
	public static final String KEY_EVENT_CHAT_ID = "chat_id";
	
	/**
	 * Values representing the direction of the event.
	 * Values are based on Calls table values:
	 * Calls.INCOMING_TYPE, Calls.OUTGOING_TYPE, Calls.MISSED_TYPE
	 */
	public static final int VALUE_EVENT_DEST_INCOMING = Calls.INCOMING_TYPE;
	public static final int VALUE_EVENT_DEST_OUTGOING = Calls.OUTGOING_TYPE;
	public static final int VALUE_EVENT_DEST_MISSED = Calls.MISSED_TYPE;
}
