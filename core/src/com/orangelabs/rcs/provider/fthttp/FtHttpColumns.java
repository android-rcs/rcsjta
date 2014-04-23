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
	String TABLE = "fthttp";
	Uri CONTENT_URI = Uri.parse(FtHttpProvider.CONTENT_URI_BASE + "/" + TABLE);

	String _ID = BaseColumns._ID;
	String OU_TID = "ou_tid";
    String IN_URL = "in_url";
    String SIZE = "size";
    String TYPE = "type";
    String CONTACT = "contact";
    String CHATID = "chatid";
    String FILENAME = "filename";
    String DIRECTION = "direction";
    String DATE = "date";
    String DISPLAY_NAME = "display_name";
    String SESSION_ID = "session_id";
    String THUMBNAIL = "thumbnail";
    String MESSAGE_ID = "message_id";
	String IS_GROUP = "is_group";
	String CHAT_SESSION_ID = "chat_session_id";
	
    String DEFAULT_ORDER = _ID;

}