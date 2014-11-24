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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.ft.FileTransferLog;

/**
 * File transfer Data Object
 * 
 * @author YPLO6403
 * 
 */
public class FileTransferDAO implements Parcelable {
	
	private String transferId;
	private ContactId contact;
	private Uri file;
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
	private Uri thumbnail;
	private int reasonCode;

	private static final String WHERE_CLAUSE = new StringBuilder(FileTransferLog.FT_ID).append("=?").toString();

	public int getState() {
		return state;
	}

	public int getReadStatus() {
		return readStatus;
	}

	public long getTimestampSent() {
		return timestampSent;
	}

	public long getTimestampDelivered() {
		return timestampDelivered;
	}

	public long getTimestampDisplayed() {
		return timestampDisplayed;
	}

	public long getSizeTransferred() {
		return sizeTransferred;
	}

	public String getTransferId() {
		return transferId;
	}

	public ContactId getContact() {
		return contact;
	}

	public Uri getFile() {
		return file;
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

	public Uri getThumbnail() {
		return thumbnail;
	}

	/**
	 * Constructor
	 * 
	 * @param source
	 *            Parcelable source
	 */
	public FileTransferDAO(Parcel source) {
		transferId = source.readString();
		boolean containsContactId = source.readInt() != 0;
		if (containsContactId) {
			contact = ContactId.CREATOR.createFromParcel(source);
		} else {
			contact = null;
		}
		boolean containsFile = source.readInt() != 0;
		if (containsFile) {
			file = Uri.parse(source.readString());
		} else {
			file = null;
		}
		filename = source.readString();
		chatId = source.readString();
		mimeType = source.readString();
		state = source.readInt();
		readStatus = source.readInt();
		direction = source.readInt();
		timestamp = source.readLong();
		timestampSent = source.readLong();
		timestampDelivered = source.readLong();
		timestampDisplayed = source.readLong();
		sizeTransferred = source.readLong();
		size = source.readLong();
		boolean containsThumbnail = source.readInt() != 0;
		if (containsThumbnail) {
			thumbnail = Uri.parse(source.readString());
		} else {
			thumbnail = null;
		}
		reasonCode = source.readInt();
	}
	
	/**
	 * Construct the File Transfer data object from the provider
	 * <p>
	 * Note: to change with CR025 (enums)
	 * 
	 * @param context
	 * @param fileTransferId
	 *            the unique key field
	 * @throws Exception 
	 */
	public FileTransferDAO(final Context context, final String fileTransferId) throws Exception {
		Uri uri = FileTransferLog.CONTENT_URI;
		String[] whereArgs = new String[] { fileTransferId };
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(uri, null, WHERE_CLAUSE, whereArgs, null);
			if (cursor.moveToFirst()) {
				transferId = fileTransferId;
				chatId = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.CHAT_ID));
				String _contact = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.CONTACT));
				if (_contact != null) {
					ContactUtils contactUtils = ContactUtils.getInstance(context);
					contact = contactUtils.formatContact(_contact);
				}
				file = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILE)));
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
				String fileicon = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILEICON));
				if (fileicon != null) {
					thumbnail = Uri.parse(fileicon);
				}
				reasonCode = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.REASON_CODE));
			} else {
				throw new IllegalArgumentException("Filetransfer ID not found"); 
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
		return "FileTransferDAO [ftId=" + transferId + ", contact=" + contact + ", filename=" + filename + ", chatId=" + chatId
				+ ", mimeType=" + mimeType + ", state=" + state + ", size=" + size + "]";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(transferId);
		if (contact != null) {
			dest.writeInt(1);
			contact.writeToParcel(dest, flags);
		} else {
			dest.writeInt(0);
		}
		if (file != null) {
			dest.writeInt(1);
			dest.writeString(file.toString());
		} else {
			dest.writeInt(0);
		}
		dest.writeString(filename);
		dest.writeString(chatId);
		dest.writeString(mimeType);
		dest.writeInt(state);
		dest.writeInt(readStatus);
		dest.writeInt(direction);
		dest.writeLong(timestamp);
		dest.writeLong(timestampSent);
		dest.writeLong(timestampDelivered);
		dest.writeLong(timestampDisplayed);
		dest.writeLong(sizeTransferred);
		dest.writeLong(size);
		if (thumbnail != null) {
			dest.writeInt(1);
			dest.writeString(thumbnail.toString());
		} else {
			dest.writeInt(0);
		}
		dest.writeInt(reasonCode);
	};

	public static final Parcelable.Creator<FileTransferDAO> CREATOR = new Parcelable.Creator<FileTransferDAO>() {
		@Override
		public FileTransferDAO createFromParcel(Parcel in) {
			return new FileTransferDAO(in);
		}

		@Override
		public FileTransferDAO[] newArray(int size) {
			return new FileTransferDAO[size];
		}
	};

	public int getReasonCode() {
		return reasonCode;
	}
}
