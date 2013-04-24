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

package com.orangelabs.rcs.provider.sharing;

import com.orangelabs.rcs.service.api.client.eventslog.EventsLogApi;

import android.net.Uri;

/**
 * Rich call history data constants
 * 
 * @author mhsm6403
 */
public class RichCallData {
	// Database URI
	public static final Uri CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.csh/csh");
	
	// Column names
	public static final String KEY_ID = "_id";
	public static final String KEY_CONTACT = "contact";
	public static final String KEY_DESTINATION = "destination";
	public static final String KEY_MIME_TYPE = "mime_type";
	public static final String KEY_NAME = "name";
	public static final String KEY_SIZE = "size";
	public static final String KEY_DATA = "_data";
	public static final String KEY_TIMESTAMP = "_date";
	public static final String KEY_NUMBER_MESSAGES ="number_of_messages";
	public static final String KEY_STATUS = "status";
	public static final String KEY_SESSION_ID = "sessionId";
	
	// Event direction
	public static final int EVENT_INCOMING = 1;
	public static final int EVENT_OUTGOING = 2;	
	
	// Status values
	public static final int STATUS_STARTED = EventsLogApi.STATUS_STARTED; 
	public static final int STATUS_FAILED = EventsLogApi.STATUS_FAILED;
	public static final int STATUS_CANCELED = EventsLogApi.STATUS_CANCELED;
	public static final int STATUS_TERMINATED = EventsLogApi.STATUS_TERMINATED;
}
