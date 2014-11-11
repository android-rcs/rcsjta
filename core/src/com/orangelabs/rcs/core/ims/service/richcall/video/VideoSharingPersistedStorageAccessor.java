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

package com.orangelabs.rcs.core.ims.service.richcall.video;

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;

import android.net.Uri;

/**
 * VideoSharingPersistedStorageAccessor helps in retrieving persisted data
 * related to a video share from the persisted storage. It can utilize caching
 * for such data that will not be changed after creation of the video sharing to
 * speed up consecutive access.
 */
public class VideoSharingPersistedStorageAccessor {

	private final String mSharingId;

	private final RichCallHistory mRichCallLog;

	private ContactId mContact;

	/**
	 * TODO: Change type to enum in CR031 implementation
	 */
	private Integer mDirection;

	public VideoSharingPersistedStorageAccessor(String sharingId, RichCallHistory richCallLog) {
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
			mContact = mRichCallLog.getVideoSharingRemoteContact(mSharingId);
		}
		return mContact;
	}

	public int getState() {
		return mRichCallLog.getVideoSharingState(mSharingId);
	}

	public int getReasonCode() {
		return mRichCallLog.getVideoSharingReasonCode(mSharingId);
	}

	public int getDirection() {
		/*
		 * Utilizing cache here as direction can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mDirection == null) {
			mDirection = mRichCallLog.getVideoSharingDirection(mSharingId);
		}
		return mDirection;
	}

	public void setStateAndReasonCode(int state, int reasonCode) {
		mRichCallLog.setVideoSharingStateAndReasonCode(mSharingId, state, reasonCode);
	}

	public void setDuration(long duration) {
		mRichCallLog.setVideoSharingDuration(mSharingId, duration);
	}

	public Uri addVideoSharing(ContactId contact, int direction, VideoContent content, int state,
			int reasonCode) {
		return mRichCallLog.addVideoSharing(mSharingId, contact, direction, content, state,
				reasonCode);
	}
}
