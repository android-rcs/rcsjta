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
	 * Validity of the file
	 */
	private long mValidity;

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
	 * Validity of the file icon
	 */
	private final long mFileIconValidity;

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
     * @hide
	 */
	public FileUploadInfo(Uri file, long validity, String filename, long size, String type, Uri fileIcon, long fileIconValidity, long fileIconSize, String fileIconType) {
		mFile = file;
		mValidity = validity;
		mFileName = filename;
		mSize = size;
		mMimeType = type;
		mFileIcon = fileIcon;
		mFileIconValidity = fileIconValidity;
		mFileIconSize = fileIconSize;
		mFileIconMimeType = fileIconType;
	}
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public FileUploadInfo(Parcel source) {
		mFile = Uri.parse(source.readString());
		mValidity = source.readLong();
		mFileName = source.readString();
		mSize = source.readLong();
		mMimeType = source.readString();
		mFileIcon = Uri.parse(source.readString());
		mFileIconValidity = source.readLong();
		mFileIconSize = source.readLong();
		mFileIconMimeType = source.readString();
    }
	
	/**
	 * Describe the kinds of special objects contained in this Parcelable's
	 * marshalled representation
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
    	dest.writeLong(mValidity);
    	dest.writeString(mFileName);
    	dest.writeLong(mSize);
    	dest.writeString(mMimeType);
    	dest.writeString(mFileIcon.toString());
    	dest.writeLong(mFileIconValidity);
    	dest.writeLong(mFileIconSize);
    	dest.writeString(mFileIconMimeType);
    }

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<FileUploadInfo> CREATOR
            = new Parcelable.Creator<FileUploadInfo>() {
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
	 * Returns the validity of the file on the content server. This corresponds
	 * to the date and time from when the file will be removed on the content server.
	 * 
	 * @return Duration
	 */
	public long getValidity() {
		return mValidity;
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
	 * @return Content type
	 */
	public String getMimeType() {
		return mMimeType;
	}

	/**
	 * Returns URI of the file icon on the content server
	 *  
	 * @return Uri
	 */	
	public Uri getFileIcon() {
		return mFileIcon;
	}

	/**
	 * Returns the validity of the file icon on the content server. This corresponds
	 * to the date and time from when the file icon will be removed on the content server.
	 * 
	 * @return Duration
	 */
	public long getFileIconValidity() {
		return mFileIconValidity;
	}

	/**
	 * Returns the size of the file icon
	 *  
	 * @return Size
	 */
	public long getFileIconSize() {
		return mFileIconSize;
	}

	/**
	 * Returns the MIME type of the file icon
	 * 
	 * @return Content type
	 */
	public String getFileIconMimeType() {
		return mFileIconMimeType;
	}
}
