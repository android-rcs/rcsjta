/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.orangelabs.rcs.core.ims.service.im.filetransfer;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ft.FileTransferLog;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.utils.ContactUtils;

import android.database.Cursor;
import android.net.Uri;

/**
 * FileTransferPersistedStorageAccessor helps in retrieving persisted data
 * related to a file transfer from the persisted storage. It can utilize caching
 * for such data that will not be changed after creation of the File transfer to
 * speed up consecutive access.
 */
public class FileTransferPersistedStorageAccessor {

	private final String mFileTransferId;

	private final MessagingLog mMessagingLog;

	private ContactId mContact;

	/**
	 * TODO: Change type to enum in CR031 implementation
	 */
	private Integer mDirection;

	private String mChatId;

	private String mFileName;

	private Long mFileSize;

	private String mMimeType;

	private Uri mFile;

	private Uri mFileIcon;

	public FileTransferPersistedStorageAccessor(String fileTransferId, MessagingLog messagingLog) {
		mFileTransferId = fileTransferId;
		mMessagingLog = messagingLog;
	}

	public FileTransferPersistedStorageAccessor(String fileTransferId, ContactId contact,
			int direction, String chatId, Uri file, Uri fileIcon, String fileName, String mimeType,
			long fileSize, MessagingLog messagingLog) {
		mFileTransferId = fileTransferId;
		mContact = contact;
		mDirection = direction;
		mChatId = chatId;
		mFile = file;
		mFileIcon = fileIcon;
		mFileName = fileName;
		mMimeType = mimeType;
		mFileSize = fileSize;
		mMessagingLog = messagingLog;
	}

	private void cacheData() {
		Cursor cursor = null;
		try {
			cursor = mMessagingLog.getCacheableFileTransferData(mFileTransferId);
			String contact = cursor.getString(cursor
					.getColumnIndexOrThrow(FileTransferLog.CONTACT));
			if (contact != null) {
				mContact = ContactUtils.createContactId(contact);
			}
			mDirection = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.DIRECTION));
			mChatId = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.CHAT_ID));
			mFileName = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILENAME));
			mMimeType = cursor.getString(cursor
					.getColumnIndexOrThrow(FileTransferLog.MIME_TYPE));
			mFile = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILE)));
			String fileIcon = cursor.getString(cursor
					.getColumnIndexOrThrow(FileTransferLog.FILEICON));
			if (fileIcon != null) {
				mFileIcon = Uri.parse(fileIcon);
			}
			mFileSize = cursor.getLong(cursor.getColumnIndexOrThrow(FileTransferLog.FILESIZE));
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public String getChatId() {
		/*
		 * Utilizing cache here as chatId can't be changed in persistent storage
		 * after entry insertion anyway so no need to query for it multiple
		 * times.
		 */
		if (mChatId == null) {
			cacheData();
		}
		return mChatId;
	}

	public ContactId getRemoteContact() {
		/*
		 * Utilizing cache here as contact can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mContact == null) {
			cacheData();
		}
		return mContact;
	}

	public Uri getFile() {
		/*
		 * Utilizing cache here as file can't be changed in persistent storage
		 * after entry insertion anyway so no need to query for it multiple
		 * times.
		 */
		if (mFile == null) {
			cacheData();
		}
		return mFile;
	}

	public String getFileName() {
		/*
		 * Utilizing cache here as file name can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mFileName == null) {
			cacheData();
		}
		return mFileName;
	}

	public long getFileSize() {
		/*
		 * Utilizing cache here as file size can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mFileSize == null) {
			cacheData();
		}
		return mFileSize;
	}

	public String getMimeType() {
		/*
		 * Utilizing cache here as mime type can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mMimeType == null) {
			cacheData();
		}
		return mMimeType;
	}

	public Uri getFileIcon() {
		/*
		 * Utilizing cache here as file icon can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mFileIcon == null) {
			cacheData();
		}
		return mFileIcon;
	}

	public int getState() {
		return mMessagingLog.getFileTransferState(mFileTransferId);
	}

	public int getReasonCode() {
		return mMessagingLog.getFileTransferStateReasonCode(mFileTransferId);
	}

	public int getDirection() {
		/*
		 * Utilizing cache here as direction can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mDirection == null) {
			cacheData();
		}
		return mDirection;
	}

	public void setStateAndReasonCode(int state, int reasonCode) {
		mMessagingLog.setFileTransferStateAndReasonCode(mFileTransferId, state, reasonCode);
	}

	public void setProgress(long currentSize) {
		mMessagingLog.setFileTransferProgress(mFileTransferId, currentSize);
	}

	public void setTransferred(MmContent content) {
		mMessagingLog.setFileTransferred(mFileTransferId, content);
	}

	public void addFileTransfer(ContactId contact, int direction, MmContent content,
			MmContent fileIcon, int status, int reasonCode) {
		mMessagingLog.addFileTransfer(mFileTransferId, contact, direction, content, fileIcon,
				status, reasonCode);
	}

	public void addIncomingGroupFileTransfer(String chatId, ContactId contact, MmContent content,
			MmContent fileicon, int state, int reasonCode) {
		mMessagingLog.addIncomingGroupFileTransfer(mFileTransferId, chatId, contact, content,
				fileicon, state, reasonCode);
	}
}
