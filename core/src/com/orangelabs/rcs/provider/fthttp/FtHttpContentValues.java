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

import android.content.ContentResolver;
import android.net.Uri;

import com.orangelabs.rcs.provider.base.AbstractContentValues;

/**
 * Content values wrapper for the {@code fthttp} table.
 */
public class FtHttpContentValues extends AbstractContentValues {
	@Override
	public Uri uri() {
		return FtHttpColumns.CONTENT_URI;
	}

	/**
	 * Update row(s) using the values stored by this object and the given selection.
	 * 
	 * @param contentResolver
	 *            The content resolver to use.
	 * @param where
	 *            The selection to use (can be {@code null}).
	 * @return the number of rows updated.
	 */
	public int update(ContentResolver contentResolver, FtHttpSelection where) {
		return contentResolver.update(uri(), values(), where == null ? null : where.sel(), where == null ? null : where.args());
	}

	/**
	 * Adds TID to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value}.
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putOuTid(String value) {
		mContentValues.put(FtHttpColumns.OU_TID, value);
		return this;
	}

	/**
	 * Adds URL to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value}.
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putInUrl(String value) {
		mContentValues.put(FtHttpColumns.IN_URL, value);
		return this;
	}

	/**
	 * Adds size to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value}.
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putSize(Long value) {
		mContentValues.put(FtHttpColumns.SIZE, value);
		return this;
	}

	/**
	 * Adds type (of transferred file) to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value}.
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putType(String value) {
		mContentValues.put(FtHttpColumns.TYPE, value);
		return this;
	}

	/**
	 * Adds contact to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value}.
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putContact(String value) {
		mContentValues.put(FtHttpColumns.CONTACT, value);
		return this;
	}

	/**
	 * Adds chat ID to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value}.
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putChatid(String value) {
		mContentValues.put(FtHttpColumns.CHATID, value);
		return this;
	}

	/**
	 * Adds file name to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value}.
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putFilename(String value) {
		if (value == null)
			throw new IllegalArgumentException("value for filename must not be null");
		mContentValues.put(FtHttpColumns.FILENAME, value);
		return this;
	}

	/**
	 * Adds direction to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value} (cannot be null).
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putDirection(FtHttpDirection value) {
		if (value == null)
			throw new IllegalArgumentException("value for direction must not be null");
		mContentValues.put(FtHttpColumns.DIRECTION, value.ordinal());
		return this;
	}

	/**
	 * Adds date to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value} (cannot be null).
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putDate(Date value) {
		if (value == null)
			throw new IllegalArgumentException("value for date must not be null");
		mContentValues.put(FtHttpColumns.DATE, value.getTime());
		return this;
	}

	public FtHttpContentValues putDate(long value) {
		mContentValues.put(FtHttpColumns.DATE, value);
		return this;
	}

	/**
	 * Adds display name to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value}.
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putDisplayName(String value) {
		mContentValues.put(FtHttpColumns.DISPLAY_NAME, value);
		return this;
	}

	/**
	 * Adds session ID to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value}.
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putSessionId(String value) {
		mContentValues.put(FtHttpColumns.SESSION_ID, value);
		return this;
	}

	/**
	 * Adds thumbnail byte array to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value}.
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putThumbnail(byte[] value) {
		mContentValues.put(FtHttpColumns.THUMBNAIL, value);
		return this;
	}

	/**
	 * Adds message ID to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value}.
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putMessageId(String value) {
		mContentValues.put(FtHttpColumns.MESSAGE_ID, value);
		return this;
	}
	
	/**
	 * Adds isGroup to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value}.
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
    public FtHttpContentValues putIsGroup(Boolean value) {
        mContentValues.put(FtHttpColumns.IS_GROUP, value);
        return this;
    }
    
	/**
	 * Adds Chat session ID to the set of content values.
	 * 
	 * @param value
	 *            The added {@code value}.
	 * @return Returns the {@code FthttpContentValues} wrapped by this object.
	 */
	public FtHttpContentValues putChatSessionId(String value) {
		mContentValues.put(FtHttpColumns.CHAT_SESSION_ID, value);
		return this;
	}
}
