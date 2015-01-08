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

package com.orangelabs.rcs.core.ims.service.richcall.image;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ish.ImageSharingLog;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;
import com.orangelabs.rcs.utils.ContactUtils;

import android.database.Cursor;
import android.net.Uri;

/**
 * ImageSharingPersistedStorageAccessor helps in retrieving persisted data
 * related to a image share from the persisted storage. It can utilize caching
 * for such data that will not be changed after creation of the Image sharing to
 * speed up consecutive access.
 */
public class ImageSharingPersistedStorageAccessor {

	private final String mSharingId;

	private final RichCallHistory mRichCallLog;

	private ContactId mContact;

	/**
	 * TODO: Change type to enum in CR031 implementation
	 */
	private Integer mDirection;

	private String mFileName;

	private Long mFileSize;

	private String mMimeType;

	private Uri mFile;

	public ImageSharingPersistedStorageAccessor(String sharingId, RichCallHistory richCallLog) {
		mSharingId = sharingId;
		mRichCallLog = richCallLog;
	}

	public ImageSharingPersistedStorageAccessor(String sharingId, ContactId contact, int direction,
			Uri file, String fileName, String mimeType, long fileSize, RichCallHistory richCallLog) {
		mSharingId = sharingId;
		mContact = contact;
		mDirection = direction;
		mFile = file;
		mFileName = fileName;
		mFileSize = fileSize;
		mMimeType = mimeType;
		mRichCallLog = richCallLog;
	}

	private void cacheData() {
		Cursor cursor = null;
		try {
			cursor = mRichCallLog.getCacheableImageTransferData(mSharingId);
			String contact = cursor.getString(cursor
					.getColumnIndexOrThrow(ImageSharingLog.CONTACT));
			if (contact != null) {
				mContact = ContactUtils.createContactId(contact);
			}
			mDirection = cursor.getInt(cursor.getColumnIndexOrThrow(ImageSharingLog.DIRECTION));
			mFileName = cursor.getString(cursor.getColumnIndexOrThrow(ImageSharingLog.FILENAME));
			mMimeType = cursor.getString(cursor.getColumnIndexOrThrow(ImageSharingLog.MIME_TYPE));
			mFileSize = cursor.getLong(cursor.getColumnIndexOrThrow(ImageSharingLog.FILESIZE));
			mFile = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(ImageSharingLog.FILE)));
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
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

	public int getState() {
		return mRichCallLog.getImageSharingState(mSharingId);
	}

	public int getReasonCode() {
		return mRichCallLog.getImageSharingReasonCode(mSharingId);
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
		mRichCallLog.setImageSharingStateAndReasonCode(mSharingId, state, reasonCode);
	}

	public void setProgress(long currentSize) {
		mRichCallLog.setImageSharingProgress(mSharingId, currentSize);
	}

	public Uri addImageSharing(ContactId contact, int direction, MmContent content, int status,
			int reasonCode) {
		return mRichCallLog.addImageSharing(mSharingId, contact, direction, content, status, reasonCode);
	}
}
