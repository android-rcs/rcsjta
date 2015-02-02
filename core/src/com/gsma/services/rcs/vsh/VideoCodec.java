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
package com.gsma.services.rcs.vsh;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Video codec
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoCodec implements Parcelable {
	/**
	 * Video encoding
	 */
	private final String mEncoding;
	
	/**
	 * Payload
	 */
	private final int mPayload;
	
	/**
	 * Clock rate
	 */
	private final int mClockRate;
	
	/**
	 * Frame rate
	 */
	private final int mFrameRate;
	
	/**
	 * Bit rate
	 */
	private final int mBitRate;

	/**
	 * Video frame width
	 */
	private final int mWidth;

	/**
	 * Video frame height
	 */
	private final int mHeight;
	
	/**
	 * Video parameters
	 */
	private final String mParameters;
	
    /**
     * Constructor
     *
     * @param encoding Video encoding
     * @param payload Payload
     * @param clockRate Clock rate
     * @param frameRate Frame rate
     * @param bitRate Bit rate
     * @param width Video width
     * @param height Video height
     * @param parameters Codec parameters
     * @hide
     */
    public VideoCodec(String encoding, int payload, int clockRate, int frameRate, int bitRate, int width, int height, String parameters) {
    	mEncoding = encoding;
    	mPayload = payload;
    	mClockRate = clockRate;
    	mFrameRate = frameRate;
    	mBitRate = bitRate;
    	mWidth = width;
    	mHeight = height;  	
    	mParameters = parameters;
    }
    
    /**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public VideoCodec(Parcel source) {
		mEncoding = source.readString();
    	mPayload = source.readInt();
    	mClockRate = source.readInt();
    	mFrameRate = source.readInt();
    	mBitRate = source.readInt();
    	mWidth = source.readInt();
    	mHeight = source.readInt();
		mParameters = source.readString();
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
    	dest.writeString(mEncoding);
    	dest.writeInt(mPayload);
    	dest.writeInt(mClockRate);
    	dest.writeInt(mFrameRate);
    	dest.writeInt(mBitRate);
    	dest.writeInt(mWidth);
    	dest.writeInt(mHeight);
    	dest.writeString(mParameters);
    }
    
    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<VideoCodec> CREATOR
            = new Parcelable.Creator<VideoCodec>() {
        public VideoCodec createFromParcel(Parcel source) {
            return new VideoCodec(source);
        }

        public VideoCodec[] newArray(int size) {
            return new VideoCodec[size];
        }
    };	

    /**
    * Returns the encoding name (e.g. H264)
    * 
    * @return Encoding name
    */
    public String getEncoding() {
    	return mEncoding;
    }
    
    /**
     * Returns the codec payload type (e.g. 96)
     * 
     * @return Payload type
     */
    public int getPayloadType() {
    	return mPayload;
    }
    
    /**
     * Returns the codec clock rate (e.g. 90000)
     * 
     * @return Clock rate
     */
    public int getClockRate() {
    	return mClockRate;
    }
    
    /**
     * Returns the codec frame rate (e.g. 10)
     * 
     * @return Frame rate
     */
    public int getFrameRate() {
    	return mFrameRate;
    }
    
    /**
     * Returns the codec bit rate (e.g. 64000)
     * 
     * @return Bit rate
     */
    public int getBitRate() {
    	return mBitRate;
    }
    
	/**
	 * Returns the width of video frame (e.g. 176)
	 * 
	 * @return Video width in pixels
	 */
    public int getWidth() {
    	return mWidth;
    }

	/**
	 * Returns the height of video frame (e.g. 144)
	 * 
	 * @return Video height in pixels
	 */
	public int getHeight() {
		return mHeight;
	}
    	
    /**
     * Returns the list of codec parameters (e.g. profile-level-id, packetization-mode).
     * Parameters are are semicolon separated.
     * 
     * @return Parameters
     */
    public String getParameters() {
    	return mParameters;    	
    }
}
