/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/
package com.gsma.services.rcs.extension;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Multimedia session configuration
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaSessionServiceConfiguration implements Parcelable {
	/**
	 * Max length of a message
	 */
	private int maxMsgLength;
	
	/**
	 * Constructor
	 * 
	 * @param maxMsgLength Max length of a message
     * @hide
	 */
	public MultimediaSessionServiceConfiguration(int maxMsgLength) {
		this.maxMsgLength = maxMsgLength;
    }	
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public MultimediaSessionServiceConfiguration(Parcel source) {
		this.maxMsgLength = source.readInt();
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
    	dest.writeInt(maxMsgLength);
    }

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<MultimediaSessionServiceConfiguration> CREATOR
            = new Parcelable.Creator<MultimediaSessionServiceConfiguration>() {
        public MultimediaSessionServiceConfiguration createFromParcel(Parcel source) {
            return new MultimediaSessionServiceConfiguration(source);
        }

        public MultimediaSessionServiceConfiguration[] newArray(int size) {
            return new MultimediaSessionServiceConfiguration[size];
        }
    };	
	
	/**
	 * Return maximum length of a multimedia message
	 * 
	 * @return Number of bytes
	 */
	public int getMessageMaxLength() {
		return maxMsgLength;
	}
}
