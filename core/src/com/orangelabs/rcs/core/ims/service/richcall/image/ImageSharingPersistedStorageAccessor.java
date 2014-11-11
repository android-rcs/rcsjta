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
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;

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

	public ContactId getRemoteContact() {
		/*
		 * Utilizing cache here as contact can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mContact == null) {
			mContact = mRichCallLog.getImageSharingRemoteContact(mSharingId);
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
			mFile = mRichCallLog.getImage(mSharingId);
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
			mFileName = mRichCallLog.getImageSharingName(mSharingId);
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
			mFileSize = mRichCallLog.getImageSharingSize(mSharingId);
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
			mMimeType = mRichCallLog.getImageSharingMimeType(mSharingId);
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
			mDirection = mRichCallLog.getImageSharingDirection(mSharingId);
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
