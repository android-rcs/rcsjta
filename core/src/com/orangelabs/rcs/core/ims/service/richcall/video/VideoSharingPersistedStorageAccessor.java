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

import android.database.Cursor;
import android.net.Uri;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.vsh.VideoDescriptor;
import com.gsma.services.rcs.vsh.VideoSharingLog;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;
import com.orangelabs.rcs.utils.ContactUtils;

/**
 * VideoSharingPersistedStorageAccessor helps in retrieving persisted data related to a video share
 * from the persisted storage. It can utilize caching for such data that will not be changed after
 * creation of the video sharing to speed up consecutive access.
 */
public class VideoSharingPersistedStorageAccessor {

	private final String mSharingId;

	private final RichCallHistory mRichCallLog;

	private ContactId mContact;

	private Direction mDirection;

	private String mVideoEncoding;

	private VideoDescriptor mVideoDescriptor;

	private Long mTimestamp;

	/**
	 * Constructor
	 * 
	 * @param sharingId
	 * @param richCallLog
	 */
	public VideoSharingPersistedStorageAccessor(String sharingId, RichCallHistory richCallLog) {
		mSharingId = sharingId;
		mRichCallLog = richCallLog;
	}

	/**
	 * Constructor
	 * 
	 * @param sharingId
	 * @param contact
	 * @param direction
	 * @param richCallLog
	 * @param videoEncoding
	 * @param height
	 * @param width
	 * @param timestamp
	 */
	public VideoSharingPersistedStorageAccessor(String sharingId, ContactId contact,
			Direction direction, RichCallHistory richCallLog, String videoEncoding, int height,
			int width, long timestamp) {
		mSharingId = sharingId;
		mContact = contact;
		mDirection = direction;
		mRichCallLog = richCallLog;
		mVideoEncoding = videoEncoding;
		mVideoDescriptor = new VideoDescriptor(width, height);
		mTimestamp = timestamp;
	}

	private void cacheData() {
		Cursor cursor = null;
		try {
			cursor = mRichCallLog.getCacheableVideoSharingData(mSharingId);
			String contact = cursor
					.getString(cursor.getColumnIndexOrThrow(VideoSharingLog.CONTACT));
			if (contact != null) {
				mContact = ContactUtils.createContactId(contact);
			}
			mDirection = Direction.valueOf(cursor.getInt(cursor
					.getColumnIndexOrThrow(VideoSharingLog.DIRECTION)));
			mVideoEncoding = cursor.getString(cursor
					.getColumnIndexOrThrow(VideoSharingLog.VIDEO_ENCODING));
			int width = cursor.getInt(cursor.getColumnIndexOrThrow(VideoSharingLog.WIDTH));
			int height = cursor.getInt(cursor.getColumnIndexOrThrow(VideoSharingLog.HEIGHT));
			mVideoDescriptor = new VideoDescriptor(width, height);
			mTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(VideoSharingLog.TIMESTAMP));
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Gets remote contact
	 * 
	 * @return remote contact
	 */
	public ContactId getRemoteContact() {
		/*
		 * Utilizing cache here as contact can't be changed in persistent storage after entry
		 * insertion anyway so no need to query for it multiple times.
		 */
		if (mContact == null) {
			cacheData();
		}
		return mContact;
	}

	/**
	 * Gets video sharing session state
	 * 
	 * @return state
	 */
	public int getState() {
		return mRichCallLog.getVideoSharingState(mSharingId);
	}

	/**
	 * Gets video sharing reason code
	 * 
	 * @return reason code
	 */
	public int getReasonCode() {
		return mRichCallLog.getVideoSharingReasonCode(mSharingId);
	}

	/**
	 * Gets direction
	 * 
	 * @return direction
	 */
	public Direction getDirection() {
		/*
		 * Utilizing cache here as direction can't be changed in persistent storage after entry
		 * insertion anyway so no need to query for it multiple times.
		 */
		if (mDirection == null) {
			cacheData();
		}
		return mDirection;
	}

	/**
	 * Sets state, reason code and duration
	 * 
	 * @param state
	 * @param reasonCode
	 * @param duration
	 */
	public void setStateReasonCodeAndDuration(int state, int reasonCode, long duration) {
		mRichCallLog.setVideoSharingStateReasonCodeAndDuration(mSharingId, state, reasonCode,
				duration);
	}

	/**
	 * Add video sharing session
	 * 
	 * @param contact
	 * @param direction
	 * @param content
	 * @param state
	 * @param reasonCode
	 * @return the URI of the newly inserted item
	 */
	public Uri addVideoSharing(ContactId contact, Direction direction, VideoContent content,
			int state, int reasonCode) {
		return mRichCallLog.addVideoSharing(mSharingId, contact, direction, content, state,
				reasonCode);
	}

	/**
	 * Gets video encoding
	 * 
	 * @return video encoding
	 */
	public String getVideoEncoding() {
		/*
		 * Utilizing cache here as video encoding can't be changed in persistent storage after entry
		 * insertion anyway so no need to query for it multiple times.
		 */
		if (mVideoEncoding == null) {
			cacheData();
		}
		return mVideoEncoding;
	}

	/**
	 * Gets video descriptor
	 * 
	 * @return descriptor
	 */
	public VideoDescriptor getVideoDescriptor() {
		/*
		 * Utilizing cache here as video descriptor can't be changed in persistent storage after
		 * entry insertion anyway so no need to query for it multiple times.
		 */
		if (mVideoDescriptor == null) {
			cacheData();
		}
		return mVideoDescriptor;
	}

	/**
	 * Returns the local timestamp of when the video sharing was initiated for outgoing video
	 * sharing or the local timestamp of when the video sharing invitation was received for incoming
	 * video sharings.
	 * 
	 * @return timestamp
	 */
	public long getTimestamp() {
		/*
		 * Utilizing cache here as direction can't be changed in persistent storage after entry
		 * insertion anyway so no need to query for it multiple times.
		 */
		if (mTimestamp == null) {
			cacheData();
		}
		return mTimestamp;
	}

	/**
	 * Returns the duration of the video sharing
	 * 
	 * @return duration
	 */
	public long getDuration() {
		return mRichCallLog.getVideoSharingDuration(mSharingId);
	}
}
