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
package com.orangelabs.rcs.provider.fthttp;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Columns for the {@code fthttp} table.
 */
public interface FtHttpColumns extends BaseColumns {
	static final String TABLE = "fthttp";
	static final Uri CONTENT_URI = Uri.parse(FtHttpProvider.CONTENT_URI_BASE + "/" + TABLE);

	static final String _ID = BaseColumns._ID;
	static final String OU_TID = "ou_tid";
	static final String IN_URL = "in_url";
	static final String SIZE = "size";
	static final String TYPE = "type";
	static final String CONTACT = "contact";
	static final String CHATID = "chatid";
	static final String FILENAME = "filename";
	static final String DIRECTION = "direction";
	static final String DATE = "date";
	static final String DISPLAY_NAME = "display_name";
	static final String SESSION_ID = "session_id";
	static final String THUMBNAIL = "thumbnail";
	static final String MESSAGE_ID = "message_id";
	static final String IS_GROUP = "is_group";
	static final String CHAT_SESSION_ID = "chat_session_id";
	
	static final String DEFAULT_ORDER = _ID;

	// @formatter:off
    static final String[] FULL_PROJECTION = new String[] {
            _ID,
            OU_TID,
            IN_URL,
            SIZE,
            TYPE,
            CONTACT,
            CHATID,
            FILENAME,
            DIRECTION,
            DATE,
            DISPLAY_NAME,
            SESSION_ID,
            THUMBNAIL,
            MESSAGE_ID,
            IS_GROUP,
            CHAT_SESSION_ID
    };
    // @formatter:on
}