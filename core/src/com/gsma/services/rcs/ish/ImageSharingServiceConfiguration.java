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
package com.gsma.services.rcs.ish;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Image sharing service configuration
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingServiceConfiguration implements Parcelable {
		
	/**
	 * Image size limit
	 */
	private long maxSize;
	
	/**
	 * Constructor
	 * 
	 * @param maxSize Image size limit
     * @hide
	 */
	public ImageSharingServiceConfiguration(long maxSize) {
		this.maxSize = maxSize;
    }	
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public ImageSharingServiceConfiguration(Parcel source) {
		this.maxSize = source.readLong();
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
    	dest.writeLong(maxSize);
    }

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<ImageSharingServiceConfiguration> CREATOR
            = new Parcelable.Creator<ImageSharingServiceConfiguration>() {
        public ImageSharingServiceConfiguration createFromParcel(Parcel source) {
            return new ImageSharingServiceConfiguration(source);
        }

        public ImageSharingServiceConfiguration[] newArray(int size) {
            return new ImageSharingServiceConfiguration[size];
        }
    };	
		
	/**
	 * Returns the maximum authorized size of the image that can be sent. It
	 * returns 0 if there is no limitation.
	 * 
	 * @return Size in kilobytes
	 */
	public long getMaxSize() {
		return maxSize;
	}
}
