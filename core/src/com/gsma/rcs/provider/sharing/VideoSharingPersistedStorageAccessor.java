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

package com.gsma.rcs.provider.sharing;

import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.VideoDescriptor;
import com.gsma.services.rcs.sharing.video.VideoSharing.ReasonCode;
import com.gsma.services.rcs.sharing.video.VideoSharing.State;

import android.database.Cursor;
import android.net.Uri;

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

    private long mTimestamp;

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
            cursor = mRichCallLog.getVideoSharingData(mSharingId);
            if (!cursor.moveToNext()) {
                throw new ServerApiPersistentStorageException(new StringBuilder(
                        "Data not found for video sharing ").append(mSharingId).toString());
            }
            String contact = cursor.getString(cursor
                    .getColumnIndexOrThrow(VideoSharingData.KEY_CONTACT));
            if (contact != null) {
                mContact = ContactUtil.createContactIdFromTrustedData(contact);
            }
            mDirection = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(VideoSharingData.KEY_DIRECTION)));
            mVideoEncoding = cursor.getString(cursor
                    .getColumnIndexOrThrow(VideoSharingData.KEY_VIDEO_ENCODING));
            int width = cursor.getInt(cursor.getColumnIndexOrThrow(VideoSharingData.KEY_WIDTH));
            int height = cursor.getInt(cursor.getColumnIndexOrThrow(VideoSharingData.KEY_HEIGHT));
            mVideoDescriptor = new VideoDescriptor(width, height);
            mTimestamp = cursor.getLong(cursor
                    .getColumnIndexOrThrow(VideoSharingData.KEY_TIMESTAMP));
        } finally {
            CursorUtil.close(cursor);
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
    public State getState() {
        State state = mRichCallLog.getVideoSharingState(mSharingId);
        if (state == null) {
            throw new ServerApiPersistentStorageException(new StringBuilder(
                    "State not found for video sharing ").append(mSharingId).toString());
        }
        return state;
    }

    /**
     * Gets video sharing reason code
     * 
     * @return reason code
     */
    public ReasonCode getReasonCode() {
        ReasonCode reasonCode = mRichCallLog.getVideoSharingReasonCode(mSharingId);
        if (reasonCode == null) {
            throw new ServerApiPersistentStorageException(new StringBuilder(
                    "Reason code not found for video sharing ").append(mSharingId).toString());
        }
        return reasonCode;
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
     * @return true if updated
     */
    public boolean setStateReasonCodeAndDuration(State state, ReasonCode reasonCode, long duration) {
        return mRichCallLog.setVideoSharingStateReasonCodeAndDuration(mSharingId, state,
                reasonCode, duration);
    }

    /**
     * Add video sharing session
     * 
     * @param contact
     * @param direction
     * @param content
     * @param state
     * @param reasonCode
     * @param timestamp Local timestamp of the video sharing
     * @return the URI of the newly inserted item
     */
    public Uri addVideoSharing(ContactId contact, Direction direction, VideoContent content,
            State state, ReasonCode reasonCode, long timestamp) {
        mContact = contact;
        mDirection = direction;
        mTimestamp = timestamp;
        return mRichCallLog.addVideoSharing(mSharingId, contact, direction, content, state,
                reasonCode, timestamp);
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
         * Utilizing cache here as timestamp can't be changed in persistent storage after entry
         * insertion anyway so no need to query for it multiple times.
         */
        if (mTimestamp == 0) {
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
        Long duration = mRichCallLog.getVideoSharingDuration(mSharingId);
        if (duration == null) {
            throw new ServerApiPersistentStorageException(new StringBuilder(
                    "Duration not found for video sharing ").append(mSharingId).toString());
        }
        return duration;
    }
}
