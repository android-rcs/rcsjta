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

package com.gsma.rcs.ri.messaging.filetransfer;

import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * File transfer Data Object
 * 
 * @author Philippe LEMORDANT
 */
public class FileTransferDAO implements Parcelable {

    private final String mTransferId;
    private final ContactId mContact;
    private final Uri mFile;
    private final String mFilename;
    private final String mChatId;
    private final String mMimeType;
    private final FileTransfer.State mState;
    private final ReadStatus mReadStatus;
    private final Direction mDirection;
    private final long mTimestamp;
    private final long mTimestampSent;
    private final long mTimestampDelivered;
    private final long mTimestampDisplayed;
    private final long mSizeTransferred;
    private final long mSize;
    private final Uri mThumbnail;
    private final long mFileExpiration;
    private final long mFileIconExpiration;
    private final FileTransfer.ReasonCode mReasonCode;
    private static ContentResolver sContentResolver;
    private final FileTransfer.Disposition mDisposition;
    private final String mIconMimeType;
    private final boolean mExpiredDelivery;

    public String getIconMimeType() {
        return mIconMimeType;
    }

    public FileTransfer.State getState() {
        return mState;
    }

    public ReadStatus getReadStatus() {
        return mReadStatus;
    }

    public long getTimestampSent() {
        return mTimestampSent;
    }

    public long getTimestampDelivered() {
        return mTimestampDelivered;
    }

    public long getTimestampDisplayed() {
        return mTimestampDisplayed;
    }

    public long getSizeTransferred() {
        return mSizeTransferred;
    }

    public String getTransferId() {
        return mTransferId;
    }

    public ContactId getContact() {
        return mContact;
    }

    public Uri getFile() {
        return mFile;
    }

    public String getFilename() {
        return mFilename;
    }

    public String getChatId() {
        return mChatId;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public Direction getDirection() {
        return mDirection;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public long getSize() {
        return mSize;
    }

    public Uri getThumbnail() {
        return mThumbnail;
    }

    public FileTransfer.Disposition getDisposition() {
        return mDisposition;
    }

    /**
     * Returns the time when the file on the content server is no longer valid to download.
     * 
     * @return time
     */
    public long getFileExpiration() {
        return mFileExpiration;
    }

    /**
     * Returns the time when the file icon on the content server is no longer valid to download.
     * 
     * @return time
     */
    public long getFileIconExpiration() {
        return mFileIconExpiration;
    }

    public FileTransfer.ReasonCode getReasonCode() {
        return mReasonCode;
    }

    private FileTransferDAO(FileTransfer.Disposition disposition, String transferId,
            ContactId contact, Uri file, String filename, String chatId, String mimeType,
            FileTransfer.State state, ReadStatus readStatus, Direction direction, long timestamp,
            long timestampSent, long timestampDelivered, long timestampDisplayed,
            long sizeTransferred, long size, Uri thumbnail, long fileExpiration,
            long fileIconExpiration, FileTransfer.ReasonCode reasonCode, boolean expiredDelivery,
            String iconMimeType) {
        mDisposition = disposition;
        mTransferId = transferId;
        mContact = contact;
        mFile = file;
        mFilename = filename;
        mChatId = chatId;
        mMimeType = mimeType;
        mState = state;
        mReadStatus = readStatus;
        mDirection = direction;
        mTimestamp = timestamp;
        mTimestampSent = timestampSent;
        mTimestampDelivered = timestampDelivered;
        mTimestampDisplayed = timestampDisplayed;
        mSizeTransferred = sizeTransferred;
        mSize = size;
        mThumbnail = thumbnail;
        mFileExpiration = fileExpiration;
        mFileIconExpiration = fileIconExpiration;
        mReasonCode = reasonCode;
        mExpiredDelivery = expiredDelivery;
        mIconMimeType = iconMimeType;
    }

    /**
     * Constructor
     * 
     * @param source Parcelable source
     */
    public FileTransferDAO(Parcel source) {
        mTransferId = source.readString();
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
        mChatId = source.readString();
        mMimeType = source.readString();
        mState = FileTransfer.State.valueOf(source.readInt());
        mReadStatus = ReadStatus.valueOf(source.readInt());
        mDirection = Direction.valueOf(source.readInt());
        mTimestamp = source.readLong();
        mTimestampSent = source.readLong();
        mTimestampDelivered = source.readLong();
        mTimestampDisplayed = source.readLong();
        mSizeTransferred = source.readLong();
        mSize = source.readLong();
        boolean containsThumbnail = source.readInt() != 0;
        if (containsThumbnail) {
            mThumbnail = Uri.parse(source.readString());
        } else {
            mThumbnail = null;
        }
        mReasonCode = FileTransfer.ReasonCode.valueOf(source.readInt());
        mFileExpiration = source.readLong();
        mFileIconExpiration = source.readLong();
        mExpiredDelivery = source.readInt() == 1;
        boolean containsIconMimeType = source.readInt() != 0;
        if (containsIconMimeType) {
            mIconMimeType = source.readString();
        } else {
            mIconMimeType = null;
        }
        mDisposition = FileTransfer.Disposition.valueOf(source.readInt());
    }

    @Override
    public String toString() {
        return "FileTransferDAO [ftId=" + mTransferId + ", contact=" + mContact + ", filename="
                + mFilename + ", chatId=" + mChatId + ", mimeType=" + mMimeType + ", state="
                + mState + ", size=" + mSize + ", expiration=" + mFileExpiration + ", thumbnail="
                + mThumbnail + ", iconExpiration=" + mFileIconExpiration + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTransferId);
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
        dest.writeString(mChatId);
        dest.writeString(mMimeType);
        dest.writeInt(mState.toInt());
        dest.writeInt(mReadStatus.toInt());
        dest.writeInt(mDirection.toInt());
        dest.writeLong(mTimestamp);
        dest.writeLong(mTimestampSent);
        dest.writeLong(mTimestampDelivered);
        dest.writeLong(mTimestampDisplayed);
        dest.writeLong(mSizeTransferred);
        dest.writeLong(mSize);
        if (mThumbnail != null) {
            dest.writeInt(1);
            dest.writeString(mThumbnail.toString());
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(mReasonCode.toInt());
        dest.writeLong(mFileExpiration);
        dest.writeLong(mFileIconExpiration);
        dest.writeInt(mExpiredDelivery ? 1 : 0);
        if (mIconMimeType != null) {
            dest.writeInt(1);
            dest.writeString(mIconMimeType);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(mDisposition.toInt());
    }

    public static final Parcelable.Creator<FileTransferDAO> CREATOR = new Parcelable.Creator<FileTransferDAO>() {
        @Override
        public FileTransferDAO createFromParcel(Parcel in) {
            return new FileTransferDAO(in);
        }

        @Override
        public FileTransferDAO[] newArray(int size) {
            return new FileTransferDAO[size];
        }
    };

    /**
     * Gets instance of File Transfer from RCS provider
     * 
     * @param context the context
     * @param fileTransferId the file tr ansfer ID
     * @return instance or null if entry not found
     */
    public static FileTransferDAO getFileTransferDAO(final Context context,
            final String fileTransferId) {
        if (sContentResolver == null) {
            sContentResolver = context.getContentResolver();
        }
        Cursor cursor = null;
        try {
            cursor = sContentResolver.query(
                    Uri.withAppendedPath(FileTransferLog.CONTENT_URI, fileTransferId), null, null,
                    null, null);
            if (cursor == null) {
                throw new SQLException(
                        "Failed to find Filetransfer with ID: ".concat(fileTransferId));
            }
            if (!cursor.moveToFirst()) {
                return null;
            }
            String chatId = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.CHAT_ID));
            String number = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.CONTACT));
            ContactId contact = null;
            if (number != null) {
                contact = ContactUtil.formatContact(number);
            }
            Uri file = Uri.parse(cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferLog.FILE)));
            String filename = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferLog.FILENAME));
            String mimeType = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferLog.MIME_TYPE));
            FileTransfer.State state = FileTransfer.State.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(FileTransferLog.STATE)));
            ReadStatus readStatus = ReadStatus.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(FileTransferLog.READ_STATUS)));
            Direction direction = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(FileTransferLog.DIRECTION)));
            long timestamp = cursor
                    .getLong(cursor.getColumnIndexOrThrow(FileTransferLog.TIMESTAMP));
            long timestampSent = cursor.getLong(cursor
                    .getColumnIndexOrThrow(FileTransferLog.TIMESTAMP_SENT));
            long timestampDelivered = cursor.getLong(cursor
                    .getColumnIndexOrThrow(FileTransferLog.TIMESTAMP_DELIVERED));
            long timestampDisplayed = cursor.getLong(cursor
                    .getColumnIndexOrThrow(FileTransferLog.TIMESTAMP_DISPLAYED));
            long sizeTransferred = cursor.getLong(cursor
                    .getColumnIndexOrThrow(FileTransferLog.TRANSFERRED));
            long size = cursor.getLong(cursor.getColumnIndexOrThrow(FileTransferLog.FILESIZE));
            FileTransfer.Disposition disposition = FileTransfer.Disposition.valueOf(cursor
                    .getInt(cursor.getColumnIndexOrThrow(FileTransferLog.DISPOSITION)));
            String fileIcon = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferLog.FILEICON));
            Uri thumbnail = null;
            if (fileIcon != null) {
                thumbnail = Uri.parse(fileIcon);
            }
            FileTransfer.ReasonCode reasonCode = FileTransfer.ReasonCode.valueOf(cursor
                    .getInt(cursor.getColumnIndexOrThrow(FileTransferLog.REASON_CODE)));
            long fileExpiration = cursor.getLong(cursor
                    .getColumnIndexOrThrow(FileTransferLog.FILE_EXPIRATION));
            long fileIconExpiration = cursor.getLong(cursor
                    .getColumnIndexOrThrow(FileTransferLog.FILEICON_EXPIRATION));
            boolean expiredDelivery = cursor.getInt(cursor
                    .getColumnIndexOrThrow(FileTransferLog.EXPIRED_DELIVERY)) == 1;
            String iconMimeType = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileTransferLog.FILEICON_MIME_TYPE));
            return new FileTransferDAO(disposition, fileTransferId, contact, file, filename,
                    chatId, mimeType, state, readStatus, direction, timestamp, timestampSent,
                    timestampDelivered, timestampDisplayed, sizeTransferred, size, thumbnail,
                    fileExpiration, fileIconExpiration, reasonCode, expiredDelivery, iconMimeType);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean isOneToOne() {
        return mContact != null && mContact.toString().equals(mChatId);
    }

    public boolean isExpiredDelivery() {
        return mExpiredDelivery;
    }
}
