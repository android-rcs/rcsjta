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
package org.gsma.joyn.vsh;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Video sharing service configuration
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingServiceConfiguration implements Parcelable {
	/**
	 * Maximum duration of the video sharing
	 */
	private long maxTime;
	
	/**
	 * Constructor
	 * 
	 * @param maxTime Maximum authorized duration of the video sharing
	 */
	public VideoSharingServiceConfiguration(long maxTime) {
		this.maxTime = maxTime;
    }	
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
	 */
	public VideoSharingServiceConfiguration(Parcel source) {
		this.maxTime = source.readLong();
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
    	dest.writeLong(maxTime);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<VideoSharingServiceConfiguration> CREATOR
            = new Parcelable.Creator<VideoSharingServiceConfiguration>() {
        public VideoSharingServiceConfiguration createFromParcel(Parcel source) {
            return new VideoSharingServiceConfiguration(source);
        }

        public VideoSharingServiceConfiguration[] newArray(int size) {
            return new VideoSharingServiceConfiguration[size];
        }
    };	

    /**
	 * Returns the maximum authorized duration of the video sharing. It returns 0 if
	 * there is no limitation.
	 * 
	 * @return Duration in seconds 
	 */
	public long getMaxTime() {
		return maxTime;
	}
}
