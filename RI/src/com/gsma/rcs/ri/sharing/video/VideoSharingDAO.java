/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.sharing.video;

import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.VideoSharing;
import com.gsma.services.rcs.sharing.video.VideoSharingLog;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

/**
 * Video Sharing Data Object
 * 
 * @author YPLO6403
 */
public class VideoSharingDAO implements Parcelable {

    private final String mSharingId;

    private final ContactId mContact;

    private final VideoSharing.State mState;

    private final VideoSharing.ReasonCode mReasonCode;

    private final Direction mDirection;

    private final long mTimestamp;

    private final long mDuration;

    private final int mHeight;

    private final int mWidth;

    private final String mVideoEncoding;

    private static ContentResolver sContentResolver;

    private VideoSharingDAO(String sharingId, ContactId contact, VideoSharing.State state,
            VideoSharing.ReasonCode reasonCode, Direction direction, long timestamp, long duration,
            int height, int width, String videoEncoding) {
        mSharingId = sharingId;
        mContact = contact;
        mState = state;
        mReasonCode = reasonCode;
        mDirection = direction;
        mTimestamp = timestamp;
        mDuration = duration;
        mHeight = height;
        mWidth = width;
        mVideoEncoding = videoEncoding;
    }

    /**
     * Constructor
     * 
     * @param source Parcelable source
     */
    public VideoSharingDAO(Parcel source) {
        mSharingId = source.readString();
        boolean containsContactId = source.readInt() != 0;
        if (containsContactId) {
            mContact = ContactId.CREATOR.createFromParcel(source);
        } else {
            mContact = null;
        }
        mState = VideoSharing.State.valueOf(source.readInt());
        mDirection = Direction.valueOf(source.readInt());
        mTimestamp = source.readLong();
        mDuration = source.readLong();
        mHeight = source.readInt();
        mWidth = source.readInt();
        mVideoEncoding = source.readString();
        mReasonCode = VideoSharing.ReasonCode.valueOf(source.readInt());
    }

    /**
     * Gets state
     * 
     * @return state
     */
    public VideoSharing.State getState() {
        return mState;
    }

    /**
     * Gets reason code
     * 
     * @return reason code
     */
    public VideoSharing.ReasonCode getReasonCode() {
        return mReasonCode;
    }

    /**
     * Gets sharing ID
     * 
     * @return sharingId
     */
    public String getSharingId() {
        return mSharingId;
    }

    /**
     * Gets remote contact
     * 
     * @return contact
     */
    public ContactId getContact() {
        return mContact;
    }

    public Direction getDirection() {
        return mDirection;
    }

    /**
     * Gets date of the sharing
     * 
     * @return time stamp
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    /**
     * Gets duration
     * 
     * @return duration
     */
    public long getDuration() {
        return mDuration;
    }

    /**
     * Gets height
     * 
     * @return height
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Gets width
     * 
     * @return width
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Gets video encoding name (e.g. H264).
     * 
     * @return video encoding
     */
    public String getVideoEncoding() {
        return mVideoEncoding;
    }

    @Override
    public String toString() {
        return "VideoSharingDAO [sharingId=" + mSharingId + ", contact=" + mContact + ", state="
                + mState + ", duration=" + DateUtils.formatElapsedTime(mDuration / 1000) + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSharingId);
        if (mContact != null) {
            dest.writeInt(1);
            mContact.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(mState.toInt());
        dest.writeInt(mDirection.toInt());
        dest.writeLong(mTimestamp);
        dest.writeLong(mDuration);
        dest.writeInt(mHeight);
        dest.writeInt(mWidth);
        dest.writeString(mVideoEncoding);
        dest.writeInt(mReasonCode.toInt());
    }

    /**
     * public CREATOR field that generates instances of Parcelable class from a VideoSharingDAO.
     */
    public static final Parcelable.Creator<VideoSharingDAO> CREATOR = new Parcelable.Creator<VideoSharingDAO>() {
        @Override
        public VideoSharingDAO createFromParcel(Parcel in) {
            return new VideoSharingDAO(in);
        }

        @Override
        public VideoSharingDAO[] newArray(int size) {
            return new VideoSharingDAO[size];
        }
    };

    /**
     * Gets instance of Video sharing from RCS provider
     * 
     * @param context the context
     * @param sharingId the sharing ID
     * @return instance or null if entry not found
     */
    public static VideoSharingDAO getVideoSharingDAO(Context context, String sharingId) {
        if (sContentResolver == null) {
            sContentResolver = context.getContentResolver();
        }
        Cursor cursor = null;
        try {
            cursor = sContentResolver.query(
                    Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, sharingId), null, null, null,
                    null);
            if (cursor == null) {
                throw new SQLException("Query failed!");
            }
            if (!cursor.moveToFirst()) {
                throw new SQLException("Failed to find Video Sharing with ID: ".concat(sharingId));
            }
            String number = cursor.getString(cursor.getColumnIndexOrThrow(VideoSharingLog.CONTACT));
            ContactId contact = ContactUtil.formatContact(number);
            VideoSharing.State state = VideoSharing.State.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(VideoSharingLog.STATE)));
            Direction dir = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(VideoSharingLog.DIRECTION)));
            long timestamp = cursor
                    .getLong(cursor.getColumnIndexOrThrow(VideoSharingLog.TIMESTAMP));
            long duration = cursor.getLong(cursor.getColumnIndexOrThrow(VideoSharingLog.DURATION));
            int height = cursor.getInt(cursor.getColumnIndexOrThrow(VideoSharingLog.HEIGHT));
            int width = cursor.getInt(cursor.getColumnIndexOrThrow(VideoSharingLog.WIDTH));
            String videoEncoding = cursor.getString(cursor
                    .getColumnIndexOrThrow(VideoSharingLog.VIDEO_ENCODING));
            VideoSharing.ReasonCode reason = VideoSharing.ReasonCode.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(VideoSharingLog.REASON_CODE)));
            return new VideoSharingDAO(sharingId, contact, state, reason, dir, timestamp, duration,
                    height, width, videoEncoding);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
