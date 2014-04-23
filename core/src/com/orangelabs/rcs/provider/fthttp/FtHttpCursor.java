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

import java.util.Date;

import android.database.Cursor;

import com.orangelabs.rcs.provider.base.AbstractCursor;

/**
 * Cursor wrapper for the {@code fthttp} table.
 */
public class FtHttpCursor extends AbstractCursor {

	public FtHttpCursor(Cursor cursor) {
		super(cursor);
	}

	/**
	 * Get the {@code ou_tid} value. Can be {@code null}.
	 */
	public String getOuTid() {
		Integer index = getCachedColumnIndexOrThrow(FtHttpColumns.OU_TID);
		return getString(index);
	}

	/**
	 * Get the {@code in_url} value. Can be {@code null}.
	 */
	public String getInUrl() {
		Integer index = getCachedColumnIndexOrThrow(FtHttpColumns.IN_URL);
		return getString(index);
	}

	/**
	 * Get the {@code size} value. Can be {@code null}.
	 */
	public Long getSize() {
		return getLongOrNull(FtHttpColumns.SIZE);
	}

	/**
	 * Get the {@code type} value. Can be {@code null}.
	 */
	public String getType() {
		Integer index = getCachedColumnIndexOrThrow(FtHttpColumns.TYPE);
		return getString(index);
	}

	/**
	 * Get the {@code contact} value. Can be {@code null}.
	 */
	public String getContact() {
		Integer index = getCachedColumnIndexOrThrow(FtHttpColumns.CONTACT);
		return getString(index);
	}

	/**
	 * Get the {@code chatid} value. Can be {@code null}.
	 */
	public String getChatid() {
		Integer index = getCachedColumnIndexOrThrow(FtHttpColumns.CHATID);
		return getString(index);
	}

	/**
	 * Get the {@code filename} value. Cannot be {@code null}.
	 */
	public String getFilename() {
		Integer index = getCachedColumnIndexOrThrow(FtHttpColumns.FILENAME);
		return getString(index);
	}

	/**
	 * Get the {@code direction} value. Cannot be {@code null}.
	 */
	public FtHttpDirection getDirection() {
		Integer intValue = getIntegerOrNull(FtHttpColumns.DIRECTION);
		if (intValue == null)
			return null;
		return FtHttpDirection.values()[intValue];
	}

	/**
	 * Get the {@code date} value. Can be {@code null}.
	 */
	public Date getDate() {
		return getDate(FtHttpColumns.DATE);
	}

	/**
	 * Get the {@code display_name} value. Can be {@code null}.
	 */
	public String getDisplayName() {
		Integer index = getCachedColumnIndexOrThrow(FtHttpColumns.DISPLAY_NAME);
		return getString(index);
	}

	/**
	 * Get the {@code session_id} value. Can be {@code null}.
	 */
	public String getSessionId() {
		Integer index = getCachedColumnIndexOrThrow(FtHttpColumns.SESSION_ID);
		return getString(index);
	}

	/**
	 * Get the {@code thumbnail} value. Can be {@code null}.
	 */
	public byte[] getThumbnail() {
		Integer index = getCachedColumnIndexOrThrow(FtHttpColumns.THUMBNAIL);
		return getBlob(index);
	}

	/**
	 * Get the {@code message_id} value. Can be {@code null}.
	 */
	public String getMessageId() {
		Integer index = getCachedColumnIndexOrThrow(FtHttpColumns.MESSAGE_ID);
		return getString(index);
	}

	public boolean isGroup() {
		return getBoolean(FtHttpColumns.IS_GROUP);
	}
	
	/**
	 * Get the {@code chat_session_id} value. Can be {@code null}.
	 */
	public String getChatSessionId() {
		Integer index = getCachedColumnIndexOrThrow(FtHttpColumns.CHAT_SESSION_ID);
		return getString(index);
	}
}
