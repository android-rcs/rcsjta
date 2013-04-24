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

package com.orangelabs.rcs.service.api.client.presence;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Photo icon
 * 
 * @author jexa7410
 */
public class PhotoIcon implements Parcelable {
	/**
	 * Photo content
	 */
	private byte[] content = null;
	
	/**
	 * Image type
	 */
	private String type = "image/jpeg";
	
	/**
	 * Width
	 */
	private int width = 0;
	
	/**
	 * Height
	 */
	private int height = 0;
	
	/**
	 * Etag
	 */
	private String etag = null;
	
	/**
	 * Constructor
	 * 
	 * @param content Photo content
	 * @param width Width
	 * @param height Height
	 * @param etag Etag value
	 */
	public PhotoIcon(byte[] content, int width, int height, String etag) {
		this.content = content;
		this.width = width;
		this.height = height;
		this.etag = etag;
	}

	/**
	 * Constructor
	 * 
	 * @param content Photo content
	 * @param width Width
	 * @param height Height
	 */
	public PhotoIcon(byte[] content, int width, int height) {
		this(content, width, height, null);
	}
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
	 */
	public PhotoIcon(Parcel source) {
		int length = source.readInt();
		if (length > 0) {
			this.content =  new byte[length];		
			source.readByteArray(this.content);
		} else {
			this.content =  null;		
		}
		this.width = source.readInt();
		this.height = source.readInt();
		this.type = source.readString();
		this.etag = source.readString();
    }

	/**
	 * Describe the kinds of special objects contained in this Parcelable's
	 * marshalled representation
	 * 
	 * @return Integer
	 */
	public int describeContents() {
        return 0;
    }

	/**
	 * Write parcelable object
	 * 
	 * @param dest The Parcel in which the object should be written
	 * @param flags Additional flags about how the object should be written
	 */
    public void writeToParcel(Parcel dest, int flags) {
    	if (content != null) {
	    	dest.writeInt(content.length);
			dest.writeByteArray(content);
	    } else {
	    	dest.writeInt(0);
	    }
    	dest.writeInt(width);    	
    	dest.writeInt(height);
    	dest.writeString(type);
    	dest.writeString(etag);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<PhotoIcon> CREATOR
            = new Parcelable.Creator<PhotoIcon>() {
        public PhotoIcon createFromParcel(Parcel source) {
            return new PhotoIcon(source);
        }

        public PhotoIcon[] newArray(int size) {
            return new PhotoIcon[size];
        }
    };	
    
    /**
	 * Returns the Etag value
	 * 
	 * @return Tag
	 */
	public String getEtag() {
		return etag;
	}

	/**
	 * Set the Etag value
	 * 
	 * @param etag Etag
	 */
	public void setEtag(String etag) {
		this.etag = etag;
	}

	/**
	 * Returns the image type
	 * 
	 * @return Type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the icon width
	 * 
	 * @return Width in pixel
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Returns the icon height
	 * 
	 * @return Height in pixel
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Returns the icon size
	 * 
	 * @return Size in bytes
	 */
	public long getSize() {
		if (content != null) {
			return content.length;
		} else {
			return 0L;
		}
	}

	/**
	 * Returns the photo content
	 * 
	 * @return Photo content
	 */
	public byte[] getContent() {
		return content;
	}

	/**
	 * Returns the resolution
	 * 
	 * @return String as [width]x[height]
	 */
	public String getResolution() {
		return width + "x" + height;
	}

	/**
	 * Returns a string representation of the object
	 * 
	 * @return String
	 */
	public String toString() {
		return "width=" + width + ", height=" + height + ", size=" + getSize() + ", etag=" + etag;
	}
}
