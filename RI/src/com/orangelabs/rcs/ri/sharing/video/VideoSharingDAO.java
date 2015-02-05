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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.vsh.VideoSharingLog;

/**
 * Video Sharing Data Object
 * 
 * @author YPLO6403
 */
public class VideoSharingDAO implements Parcelable {

    private String mSharingId;

    private ContactId mContact;

    private int mState;

    private int mReasonCode;

    private Direction mDirection;

    private long mTimestamp;

    private long mDuration;

    private int mHeight;

    private int mWidth;

    private String mVideoEncoding;

    private static final String WHERE_CLAUSE = new StringBuilder(VideoSharingLog.SHARING_ID)
            .append("=?").toString();

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
        mState = source.readInt();
        mDirection = Direction.valueOf(source.readInt());
        mTimestamp = source.readLong();
        mDuration = source.readLong();
        mHeight = source.readInt();
        mWidth = source.readInt();
        mVideoEncoding = source.readString();

    }

    /**
     * Gets state
     * 
     * @return state
     */
    public int getState() {
        return mState;
    }

    /**
     * Gets reason code
     * 
     * @return reason code
     */
    public int getmReasonCode() {
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

    /**
     * Construct the Video Sharing data object from the provider
     * <p>
     * Note: to change with CR025 (enums)
     * 
     * @param context
     * @param sharingId the unique key field
     * @throws Exception
     */
    public VideoSharingDAO(final Context context, final String sharingId) throws Exception {
        Uri uri = VideoSharingLog.CONTENT_URI;
        String[] whereArgs = new String[] {
                sharingId
        };
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, WHERE_CLAUSE, whereArgs, null);
            if (!cursor.moveToFirst()) {
                throw new Exception("Sharing ID not found");
            }
            mSharingId = sharingId;
            String _contact = cursor.getString(cursor
                    .getColumnIndexOrThrow(VideoSharingLog.CONTACT));
            if (_contact != null) {
                ContactUtils contactUtils = ContactUtils.getInstance(context);
                mContact = contactUtils.formatContact(_contact);
            }
            mState = cursor.getInt(cursor.getColumnIndexOrThrow(VideoSharingLog.STATE));
            mDirection = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(VideoSharingLog.DIRECTION)));
            mTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(VideoSharingLog.TIMESTAMP));
            mDuration = cursor.getLong(cursor.getColumnIndexOrThrow(VideoSharingLog.DURATION));
            mHeight = cursor.getInt(cursor.getColumnIndexOrThrow(VideoSharingLog.HEIGHT));
            mWidth = cursor.getInt(cursor.getColumnIndexOrThrow(VideoSharingLog.WIDTH));
            mVideoEncoding = cursor.getString(cursor
                    .getColumnIndexOrThrow(VideoSharingLog.VIDEO_ENCODING));
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
        dest.writeInt(mState);
        dest.writeInt(mDirection.toInt());
        dest.writeLong(mTimestamp);
        dest.writeLong(mDuration);
        dest.writeInt(mHeight);
        dest.writeInt(mWidth);
        dest.writeString(mVideoEncoding);
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
}
