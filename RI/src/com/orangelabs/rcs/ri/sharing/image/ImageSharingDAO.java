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

package com.orangelabs.rcs.ri.sharing.image;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;
import com.gsma.services.rcs.sharing.image.ImageSharingLog;

/**
 * Image Sharing Data Object
 * 
 * @author YPLO6403
 */
public class ImageSharingDAO implements Parcelable {

    private String mSharingId;

    private ContactId mContact;

    private Uri mFile;

    private String mFilename;

    private String mMimeType;

    private int mState;

    private Direction mDirection;

    private long mTimestamp;

    private long mSizeTransferred;

    private long mSize;

    private int mReasonCode;

    private static final String WHERE_CLAUSE = new StringBuilder(ImageSharingLog.SHARING_ID)
            .append("=?").toString();

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
        mState = source.readInt();
        mDirection = Direction.valueOf(source.readInt());
        mTimestamp = source.readLong();
        mSizeTransferred = source.readLong();
        mSize = source.readLong();
        mReasonCode = source.readInt();
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
        dest.writeInt(mState);
        dest.writeInt(mDirection.toInt());
        dest.writeLong(mTimestamp);
        dest.writeLong(mSizeTransferred);
        dest.writeLong(mSize);
        dest.writeInt(mReasonCode);
    };

    /**
     * Gets state
     * 
     * @return state
     */
    public int getState() {
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
    public int getReasonCode() {
        return mReasonCode;
    }

    /**
     * Construct the Image Sharing data object from the provider
     * <p>
     * Note: to change with CR025 (enums)
     * 
     * @param context
     * @param sharingId the unique key field
     * @throws Exception
     */
    public ImageSharingDAO(final Context context, final String sharingId) throws Exception {
        Uri uri = ImageSharingLog.CONTENT_URI;
        String[] whereArgs = new String[] {
                sharingId
        };
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, WHERE_CLAUSE, whereArgs, null);
            if (!cursor.moveToFirst()) {
                throw new IllegalArgumentException("Sharing ID not found");

            }
            mSharingId = sharingId;
            String _contact = cursor.getString(cursor
                    .getColumnIndexOrThrow(ImageSharingLog.CONTACT));
            if (_contact != null) {
                ContactUtil contactUtil = ContactUtil.getInstance(context);
                mContact = contactUtil.formatContact(_contact);
            }
            mFile = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(ImageSharingLog.FILE)));
            mFilename = cursor.getString(cursor.getColumnIndexOrThrow(ImageSharingLog.FILENAME));
            mMimeType = cursor.getString(cursor.getColumnIndexOrThrow(ImageSharingLog.MIME_TYPE));
            mState = cursor.getInt(cursor.getColumnIndexOrThrow(ImageSharingLog.STATE));
            mDirection = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(ImageSharingLog.DIRECTION)));
            mTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(ImageSharingLog.TIMESTAMP));
            mSizeTransferred = cursor.getLong(cursor
                    .getColumnIndexOrThrow(ImageSharingLog.TRANSFERRED));
            mSize = cursor.getLong(cursor.getColumnIndexOrThrow(ImageSharingLog.FILESIZE));
            mReasonCode = cursor.getInt(cursor.getColumnIndexOrThrow(ImageSharingLog.REASON_CODE));
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
        return "ImageSharingDAO [sharingId=" + mSharingId + ", contact=" + mContact + ", file="
                + mFile + ", filename=" + mFilename + ", mimeType=" + mMimeType + ", state="
                + mState + ", size=" + mSize + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * public CREATOR field that generates instances of Parcelable class from a VideoSharingDAO.
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
