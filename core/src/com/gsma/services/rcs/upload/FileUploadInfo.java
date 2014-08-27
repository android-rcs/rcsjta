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
	private Uri file;

	/**
	 * Validity of the file
	 */
	private long validity;

	/**
	 * Original filename
	 */
	private String filename;

	/**
	 * File size
	 */
	private long size;

	/**
	 * File MIME type
	 */
	private String mimeType;
	
	/**
	 * URI of the file icon on the content server
	 */
	private Uri fileicon = null;

	/**
	 * Validity of the file icon
	 */
	private long fileiconValidity;

	/**
	 * File icon size
	 */
	private long fileiconSize;

	/**
	 * File icon MIME type
	 */
	private String fileiconMimeType;

    /**
     * Constructor for outgoing message
     * 
     * @hide
	 */
	public FileUploadInfo(Uri file, long validity, String filename, long size, String type, Uri fileicon, long fileiconValidity, long fileiconSize, String fileiconType) {
		this.file = file;
		this.validity = validity;
		this.filename = filename;
		this.size = size;
		this.mimeType = type;
		this.fileicon = fileicon;
		this.fileiconValidity = fileiconValidity;
		this.fileiconSize = fileiconSize;
		this.fileiconMimeType = fileiconType;
	}
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public FileUploadInfo(Parcel source) {
		this.file = Uri.parse(source.readString());
		this.validity = source.readLong();
		this.filename = source.readString();
		this.size = source.readLong();
		this.mimeType = source.readString();
		this.fileicon = Uri.parse(source.readString());
		this.fileiconValidity = source.readLong();
		this.fileiconSize = source.readLong();
		this.fileiconMimeType = source.readString();
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
    	dest.writeString(file.toString());
    	dest.writeLong(validity);
    	dest.writeString(filename);
    	dest.writeLong(size);
    	dest.writeString(mimeType);
    	dest.writeString(fileicon.toString());
    	dest.writeLong(fileiconValidity);
    	dest.writeLong(fileiconSize);
    	dest.writeString(fileiconMimeType);
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
		return file;
	}

	/**
	 * Returns the validity of the file on the content server. This corresponds
	 * to the date and time from when the file will be removed on the content server.
	 * 
	 * @return Duration
	 */
	public long getValidity() {
		return validity;
	}

	/**
	 * Returns the original filename
	 * 
	 * @return String
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Returns the size of the file
	 *  
	 * @return Size
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Returns the MIME type of the file
	 * 
	 * @return Content type
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Returns URI of the file icon on the content server
	 *  
	 * @return Uri
	 */	
	public Uri getFileicon() {
		return fileicon;
	}

	/**
	 * Returns the validity of the file icon on the content server. This corresponds
	 * to the date and time from when the file icon will be removed on the content server.
	 * 
	 * @return Duration
	 */
	public long getFileiconValidity() {
		return fileiconValidity;
	}

	/**
	 * Returns the size of the file icon
	 *  
	 * @return Size
	 */
	public long getFileiconSize() {
		return fileiconSize;
	}

	/**
	 * Returns the MIME type of the file icon
	 * 
	 * @return Content type
	 */
	public String getFileiconMimeType() {
		return fileiconMimeType;
	}
}
