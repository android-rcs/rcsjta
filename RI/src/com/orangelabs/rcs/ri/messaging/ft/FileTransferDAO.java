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
package com.orangelabs.rcs.ri.messaging.ft;

import java.io.Serializable;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import com.gsma.services.rcs.ft.FileTransferLog;

/**
 * @author YPLO6403
 * 
 */
public class FileTransferDAO implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String ftId;
	private String contact;
	private String filename;
	private String chatId;
	private String mimeType;
	private int state;
	private int readStatus;
	private int direction;
	private long timestamp;
	private long timestampSent;
	private long timestampDelivered;
	private long timestampDisplayed;
	private long sizeTransferred;
	private long size;
	private String thumbnail;

	private static final String WHERE_CLAUSE = new StringBuilder(FileTransferLog.FT_ID).append("=?").toString();

	public int getState() {
		return state;
	}

	public void setStatus(int state) {
		this.state = state;
	}

	public int getReadStatus() {
		return readStatus;
	}

	public void setReadStatus(int readStatus) {
		this.readStatus = readStatus;
	}

	public long getTimestampSent() {
		return timestampSent;
	}

	public void setTimestampSent(long timestampSent) {
		this.timestampSent = timestampSent;
	}

	public long getTimestampDelivered() {
		return timestampDelivered;
	}

	public void setTimestampDelivered(long timeStampDelivered) {
		this.timestampDelivered = timeStampDelivered;
	}

	public long getTimestampDisplayed() {
		return timestampDisplayed;
	}

	public void setTimestampDisplayed(long timestampDisplayed) {
		this.timestampDisplayed = timestampDisplayed;
	}

	public long getSizeTransferred() {
		return sizeTransferred;
	}

	public void setSizeTransferred(long sizeTransferred) {
		this.sizeTransferred = sizeTransferred;
	}

	public String getFtId() {
		return ftId;
	}

	public String getContact() {
		return contact;
	}

	public String getFilename() {
		return filename;
	}

	public String getChatId() {
		return chatId;
	}

	public String getMimeType() {
		return mimeType;
	}

	public int getDirection() {
		return direction;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public long getSize() {
		return size;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	/**
	 * Construct the File Transfer data object from the provider
	 * <p>
	 * Note: to change with CR025 (enums)
	 * 
	 * @param contentResolver
	 * @param fileTransferId
	 *            the unique key field
	 * @throws Exception 
	 */
	public FileTransferDAO(final ContentResolver contentResolver, final String fileTransferId) throws Exception {
		Uri uri = FileTransferLog.CONTENT_URI;
		String[] whereArgs = new String[] { fileTransferId };
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, WHERE_CLAUSE, whereArgs, null);
			if (cursor.moveToFirst()) {
				ftId = fileTransferId;
				chatId = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.CHAT_ID));
				contact = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.CONTACT_NUMBER));
				filename = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILENAME));
				mimeType = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.MIME_TYPE));
				state = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.STATE));
				readStatus = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.READ_STATUS));
				direction = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.DIRECTION));
				timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(FileTransferLog.TIMESTAMP));
				timestampSent = cursor.getLong(cursor.getColumnIndexOrThrow(FileTransferLog.TIMESTAMP_SENT));
				timestampDelivered = cursor.getLong(cursor.getColumnIndexOrThrow(FileTransferLog.TIMESTAMP_DELIVERED));
				timestampDisplayed = cursor.getLong(cursor.getColumnIndexOrThrow(FileTransferLog.TIMESTAMP_DISPLAYED));
				sizeTransferred = cursor.getLong(cursor.getColumnIndexOrThrow(FileTransferLog.TRANSFERRED));
				size = cursor.getLong(cursor.getColumnIndexOrThrow(FileTransferLog.FILESIZE));
				thumbnail = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILEICON));
				return;
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@Override
	public String toString() {
		return "FileTransferDAO [ftId=" + ftId + ", contact=" + contact + ", filename=" + filename + ", chatId=" + chatId
				+ ", mimeType=" + mimeType + ", state=" + state + ", size=" + size + "]";
	};

}
