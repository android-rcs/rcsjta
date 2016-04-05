/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.ri.sharing.image;

import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.image.ImageSharingLog;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Image Sharing Data Object
 *
 * @author YPLO6403
 */
public class ImageSharingDAO implements Parcelable {

    private final String mSharingId;

    private final ContactId mContact;

    private final Uri mFile;

    private final String mFilename;

    private final String mMimeType;

    private final ImageSharing.State mState;

    private final Direction mDirection;

    private final long mTimestamp;

    private final long mSizeTransferred;

    private final long mSize;

    private final ImageSharing.ReasonCode mReasonCode;

    private static ContentResolver sContentResolver;

    private ImageSharingDAO(String sharingId, ContactId contact, Uri file, String filename,
            String mimeType, ImageSharing.State state, Direction direction, long timestamp,
            long sizeTransferred, long size, ImageSharing.ReasonCode reasonCode) {
        mSharingId = sharingId;
        mContact = contact;
        mFile = file;
        mFilename = filename;
        mMimeType = mimeType;
        mState = state;
        mDirection = direction;
        mTimestamp = timestamp;
        mSizeTransferred = sizeTransferred;
        mSize = size;
        mReasonCode = reasonCode;
    }

    /**
     * Constructor
     *
     * @param source Parcelable source
     */
    public ImageSharingDAO(Parcel source) {
        mSharingId = source.readString();
        boolean containsContactId = source.readInt() != 0;
        if (containsContactId) {
            mContact = ContactId.CREATOR.createFromParcel(source);
        } else {
            mContact = null;
        }
        boolean containsFile = source.readInt() != 0;
        if (containsFile) {
            mFile = Uri.parse(source.readString());
        } else {
            mFile = null;
        }
        mFilename = source.readString();
        mMimeType = source.readString();
        mState = ImageSharing.State.valueOf(source.readInt());
        mDirection = Direction.valueOf(source.readInt());
        mTimestamp = source.readLong();
        mSizeTransferred = source.readLong();
        mSize = source.readLong();
        mReasonCode = ImageSharing.ReasonCode.valueOf(source.readInt());
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
        if (mFile != null) {
            dest.writeInt(1);
            dest.writeString(mFile.toString());
        } else {
            dest.writeInt(0);
        }
        dest.writeString(mFilename);
        dest.writeString(mMimeType);
        dest.writeInt(mState.toInt());
        dest.writeInt(mDirection.toInt());
        dest.writeLong(mTimestamp);
        dest.writeLong(mSizeTransferred);
        dest.writeLong(mSize);
        dest.writeInt(mReasonCode.toInt());
    }

    /**
     * Gets state
     *
     * @return state
     */
    public ImageSharing.State getState() {
        return mState;
    }

    /**
     * Gets transferred size
     *
     * @return size
     */
    public long getSizeTransferred() {
        return mSizeTransferred;
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

    /**
     * Gets file URI
     *
     * @return file URI
     */
    public Uri getFile() {
        return mFile;
    }

    /**
     * Gets file name
     *
     * @return file name
     */
    public String getFilename() {
        return mFilename;
    }

    /**
     * Gets mime type
     *
     * @return mime type
     */
    public String getMimeType() {
        return mMimeType;
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
     * Gets size
     *
     * @return size
     */
    public long getSize() {
        return mSize;
    }

    /**
     * Gets reason code
     *
     * @return reason code
     */
    public ImageSharing.ReasonCode getReasonCode() {
        return mReasonCode;
    }

    /**
     * Gets instance of Image sharing from RCS provider
     *
     * @param ctx the context
     * @param sharingId the sharing ID
     * @return instance or null if entry not found
     */
    public static ImageSharingDAO getImageSharingDAO(Context ctx, String sharingId) {
        if (sContentResolver == null) {
            sContentResolver = ctx.getContentResolver();
        }
        Cursor cursor = null;
        try {
            cursor = sContentResolver.query(
                    Uri.withAppendedPath(ImageSharingLog.CONTENT_URI, sharingId), null, null, null,
                    null);
            if (cursor == null) {
                throw new SQLException("Query failed!");
            }
            if (!cursor.moveToFirst()) {
                throw new SQLException("Failed to find Image Sharing with ID: ".concat(sharingId));
            }
            String number = cursor.getString(cursor.getColumnIndexOrThrow(ImageSharingLog.CONTACT));
            ContactId contact = ContactUtil.formatContact(number);
            Uri file = Uri.parse(cursor.getString(cursor
                    .getColumnIndexOrThrow(ImageSharingLog.FILE)));
            String filename = cursor.getString(cursor
                    .getColumnIndexOrThrow(ImageSharingLog.FILENAME));
            String mimeType = cursor.getString(cursor
                    .getColumnIndexOrThrow(ImageSharingLog.MIME_TYPE));
            ImageSharing.State state = ImageSharing.State.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ImageSharingLog.STATE)));
            Direction dir = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ImageSharingLog.DIRECTION)));
            long timestamp = cursor
                    .getLong(cursor.getColumnIndexOrThrow(ImageSharingLog.TIMESTAMP));
            long sizeTransferred = cursor.getLong(cursor
                    .getColumnIndexOrThrow(ImageSharingLog.TRANSFERRED));
            long size = cursor.getLong(cursor.getColumnIndexOrThrow(ImageSharingLog.FILESIZE));
            ImageSharing.ReasonCode reason = ImageSharing.ReasonCode.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ImageSharingLog.REASON_CODE)));
            return new ImageSharingDAO(sharingId, contact, file, filename, mimeType, state, dir,
                    timestamp, sizeTransferred, size, reason);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public String toString() {
        return "ImageSharingDAO [sharingId=" + mSharingId + ", contact=" + mContact + ", file="
                + mFile + ", filename=" + mFilename + ", mimeType=" + mMimeType + ", state="
                + mState + ", size=" + mSize + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * public CREATOR field that generates instances of Parcelable class from a ImageSharingDAO.
     */
    public static final Parcelable.Creator<ImageSharingDAO> CREATOR = new Parcelable.Creator<ImageSharingDAO>() {
        @Override
        public ImageSharingDAO createFromParcel(Parcel in) {
            return new ImageSharingDAO(in);
        }

        @Override
        public ImageSharingDAO[] newArray(int size) {
            return new ImageSharingDAO[size];
        }
    };

}
