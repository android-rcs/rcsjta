/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.services.rcs.upload;

import com.gsma.services.rcs.filetransfer.FileTransferLog;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * File upload info
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUploadInfo implements Parcelable {
    /**
     * URI of the file on the content server
     */
    private Uri mFile;

    /**
     * Time in milliseconds for when file on the content server is no longer valid to download
     */
    private long mExpiration;

    /**
     * Original filename
     */
    private String mFileName;

    /**
     * File size
     */
    private long mSize;

    /**
     * File MIME type
     */
    private String mMimeType;

    /**
     * URI of the file icon on the content server
     */
    private final Uri mFileIcon;

    /**
     * Time in milliseconds for when file icon on the content server is no longer valid to download
     * or 0 if not applicable
     */
    private final long mFileIconExpiration;

    /**
     * File icon size
     */
    private final long mFileIconSize;

    /**
     * File icon MIME type
     */
    private final String mFileIconMimeType;

    /**
     * Constructor for outgoing message
     * 
     * @param file
     * @param validity
     * @param filename
     * @param size
     * @param type
     * @param fileIcon
     * @param fileIconValidity
     * @param fileIconSize
     * @param fileIconType
     * @hide
     */
    public FileUploadInfo(Uri file, long validity, String filename, long size, String type,
            Uri fileIcon, long fileIconValidity, long fileIconSize, String fileIconType) {
        mFile = file;
        mExpiration = validity;
        mFileName = filename;
        mSize = size;
        mMimeType = type;
        mFileIcon = fileIcon;
        mFileIconExpiration = fileIconValidity;
        mFileIconSize = fileIconSize;
        mFileIconMimeType = fileIconType;
    }

    /**
     * Constructor for outgoing message
     * 
     * @param file
     * @param validity
     * @param filename
     * @param size
     * @param type
     * @hide
     */
    public FileUploadInfo(Uri file, long validity, String filename, long size, String type) {
        mFile = file;
        mExpiration = validity;
        mFileName = filename;
        mSize = size;
        mMimeType = type;
        mFileIcon = null;
        mFileIconExpiration = FileTransferLog.UNKNOWN_EXPIRATION;
        mFileIconSize = 0L;
        mFileIconMimeType = null;
    }

    /**
     * Constructor
     * 
     * @param source Parcelable source
     * @hide
     */
    public FileUploadInfo(Parcel source) {
        mFile = Uri.parse(source.readString());
        mExpiration = source.readLong();
        mFileName = source.readString();
        mSize = source.readLong();
        mMimeType = source.readString();
        boolean containsFileIcon = source.readInt() != 0;
        if (containsFileIcon) {
            mFileIcon = Uri.parse(source.readString());
        } else {
            mFileIcon = null;
        }
        mFileIconExpiration = source.readLong();
        mFileIconSize = source.readLong();
        mFileIconMimeType = source.readString();
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable's marshalled
     * representation
     * 
     * @return Integer
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    /**
     * Write parcelable object
     * 
     * @param dest The Parcel in which the object should be written
     * @param flags Additional flags about how the object should be written
     * @hide
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mFile.toString());
        dest.writeLong(mExpiration);
        dest.writeString(mFileName);
        dest.writeLong(mSize);
        dest.writeString(mMimeType);
        if (mFileIcon != null) {
            dest.writeInt(1);
            dest.writeString(mFileIcon.toString());
        } else {
            dest.writeInt(0);
        }
        dest.writeLong(mFileIconExpiration);
        dest.writeLong(mFileIconSize);
        dest.writeString(mFileIconMimeType);
    }

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<FileUploadInfo> CREATOR = new Parcelable.Creator<FileUploadInfo>() {
        public FileUploadInfo createFromParcel(Parcel source) {
            return new FileUploadInfo(source);
        }

        public FileUploadInfo[] newArray(int size) {
            return new FileUploadInfo[size];
        }
    };

    /**
     * Returns URI of the file on the content server
     * 
     * @return Uri
     */
    public Uri getFile() {
        return mFile;
    }

    /**
     * Returns the timestamp for when the file on the content server is no longer valid to download.
     * 
     * @return timestamp in milliseconds
     */
    public long getExpiration() {
        return mExpiration;
    }

    /**
     * Returns the original filename
     * 
     * @return String
     */
    public String getFileName() {
        return mFileName;
    }

    /**
     * Returns the size of the file
     * 
     * @return Size
     */
    public long getSize() {
        return mSize;
    }

    /**
     * Returns the MIME type of the file
     * 
     * @return MIME type
     */
    public String getMimeType() {
        return mMimeType;
    }

    /**
     * Returns URI of the file icon on the content server
     * 
     * @return Uri or null if no file icon defined
     */
    public Uri getFileIcon() {
        return mFileIcon;
    }

    /**
     * Returns the timestamp for when the file icon on the content server is no longer valid to
     * download.
     * 
     * @return timestamp in milliseconds of 0 if no file icon defined
     */
    public long getFileIconExpiration() {
        return mFileIconExpiration;
    }

    /**
     * Returns the size of the file icon
     * 
     * @return Size or 0 if no file icon defined
     */
    public long getFileIconSize() {
        return mFileIconSize;
    }

    /**
     * Returns the MIME type of the file icon
     * 
     * @return MIME type or null if no file icon defined
     */
    public String getFileIconMimeType() {
        return mFileIconMimeType;
    }
}
