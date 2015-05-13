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

package com.orangelabs.rcs.ri.sharing.video;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.VideoSharing;
import com.gsma.services.rcs.sharing.video.VideoSharingLog;

import com.orangelabs.rcs.ri.sharing.image.ImageSharingDAO;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.LogUtils;

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

    private static final String LOGTAG = LogUtils.getTag(ImageSharingDAO.class.getSimpleName());

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
    public VideoSharing.ReasonCode getmReasonCode() {
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

    private VideoSharingDAO(ContentResolver resolver, String sharingId) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, sharingId),
                    null, null, null, null);
            if (!cursor.moveToFirst()) {
                throw new SQLException("Failed to find Video Sharing with ID: ".concat(sharingId));
            }
            mSharingId = sharingId;
            String contact = cursor
                    .getString(cursor.getColumnIndexOrThrow(VideoSharingLog.CONTACT));
            mContact = ContactUtil.formatContact(contact);
            mState = VideoSharing.State.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(VideoSharingLog.STATE)));
            mDirection = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(VideoSharingLog.DIRECTION)));
            mTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(VideoSharingLog.TIMESTAMP));
            mDuration = cursor.getLong(cursor.getColumnIndexOrThrow(VideoSharingLog.DURATION));
            mHeight = cursor.getInt(cursor.getColumnIndexOrThrow(VideoSharingLog.HEIGHT));
            mWidth = cursor.getInt(cursor.getColumnIndexOrThrow(VideoSharingLog.WIDTH));
            mVideoEncoding = cursor.getString(cursor
                    .getColumnIndexOrThrow(VideoSharingLog.VIDEO_ENCODING));
            mReasonCode = VideoSharing.ReasonCode.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(VideoSharingLog.REASON_CODE)));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public String toString() {
        return "VideoSharingDAO [sharingId=" + mSharingId + ", contact=" + mContact + ", state="
                + mState + ", duration=" + mDuration + "]";
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
    };

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
     * @param context
     * @param sharingId
     * @return instance or null if entry not found
     */
    public static VideoSharingDAO getVideoSharingDAO(Context context, String sharingId) {
        if (sContentResolver == null) {
            sContentResolver = context.getContentResolver();
        }
        try {
            return new VideoSharingDAO(sContentResolver, sharingId);
        } catch (SQLException e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, e.getMessage());
            }
            return null;
        }
    }
}
