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
package com.gsma.services.rcs.ft;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * File transfer service configuration
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferServiceConfiguration implements Parcelable {
	/**
	 * File transfer size threshold
	 */
	private long warnSize;
		
	/**
	 * File transfer size limit
	 */
	private long maxSize;
	
	/**
	 * File transfer auto accept mode
	 */
	private boolean autoAcceptMode;
	
	/**
	 * File transfer auto accept mode in roaming
	 */
	private boolean autoAcceptModeInRoaming;

	/**
	 * File transfer default auto accept mode changeable
	 */
	private boolean autoAcceptModeChangeable;
	
	/**
	 * 
	 */
	private int maxFileTransfers;

	/**
	 * the image resize option for file transfer in the range: ALWAYS_PERFORM, ONLY_ABOVE_MAX_SIZE, ASK
	 */
	private int imageResizeOption;
	
	/**
	 * Constructor
	 * 
	 * @param warnSize
	 *            File transfer size threshold
	 * @param maxSize
	 *            File transfer size limit
	 * @param autoAcceptModeChangeable
	 *            File transfer default auto accept mode changeable
	 * @param autoAcceptMode
	 *            File transfer auto accept mode
	 * @param autoAcceptModeInRoaming
	 *            File transfer auto accept mode in roaming
	 * @param maxFileTransfers
	 * 			the maximum number of simultaneous file transfers
	 * @param imageResizeOption
	 * 			the image resize option for file transfer in the range: ALWAYS_PERFORM, ONLY_ABOVE_MAX_SIZE, ASK
	 * @hide
	 */
	public FileTransferServiceConfiguration(long warnSize, long maxSize, boolean autoAcceptModeChangeable, boolean autoAcceptMode,
			boolean autoAcceptModeInRoaming, int maxFileTransfers, int imageResizeOption) {
		this.warnSize = warnSize;
		this.maxSize = maxSize;
		this.autoAcceptModeChangeable = autoAcceptModeChangeable;
		this.autoAcceptMode = autoAcceptMode;
		this.autoAcceptModeInRoaming = autoAcceptModeInRoaming;
		this.maxFileTransfers = maxFileTransfers;
		this.imageResizeOption = imageResizeOption;
	}
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public FileTransferServiceConfiguration(Parcel source) {
		this.warnSize = source.readLong();
		this.maxSize = source.readLong();
		this.autoAcceptMode = source.readInt() != 0;
		this.autoAcceptModeInRoaming = source.readInt() != 0;
		this.autoAcceptModeChangeable = source.readInt() != 0;
		this.maxFileTransfers = source.readInt();
		this.imageResizeOption = source.readInt();
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
		dest.writeLong(warnSize);
		dest.writeLong(maxSize);
		dest.writeInt(autoAcceptMode ? 1 : 0);
		dest.writeInt(autoAcceptModeInRoaming ? 1 : 0);
		dest.writeInt(autoAcceptModeChangeable ? 1 : 0);
		dest.writeInt(maxFileTransfers);
		dest.writeInt(imageResizeOption);
	}

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<FileTransferServiceConfiguration> CREATOR
            = new Parcelable.Creator<FileTransferServiceConfiguration>() {
        public FileTransferServiceConfiguration createFromParcel(Parcel source) {
            return new FileTransferServiceConfiguration(source);
        }

        public FileTransferServiceConfiguration[] newArray(int size) {
            return new FileTransferServiceConfiguration[size];
        }
    };	
	
	/**
	 * Returns the file transfer size threshold when the user should be warned about
	 * the potential charges associated to the transfer of a large file. It returns
	 * 0 if there no need to warn.
	 * 
	 * @return Size in kilobytes 
	 */
	public long getWarnSize() {
		return warnSize;
	}
			
	/**
	 * Returns the file transfer size limit. It returns 0 if there is no limitation.
	 * 
	 * @return Size in kilobytes
	 */
	public long getMaxSize() {
		return maxSize;
	}
	
	/**
	 * Is file transfer invitation automatically accepted 
	 * 
	 * @return Returns true if File Transfer is automatically accepted else returns false
	 */
	public boolean isAutoAcceptEnabled() {
		return autoAcceptMode;
	}

	/**
	 * Is file transfer invitation automatically accepted while in roaming.
	 * <p>
	 * This parameter is only applicable if auto accept is active for File Transfer in normal conditions (see isAutoAcceptEnabled).
	 * 
	 * @return Returns true if File Transfer is automatically accepted while in roaming else returns false
	 */
	public boolean isAutoAcceptInRoamingEnabled() {
		return autoAcceptModeInRoaming;
	}
	
	/**
	 * is default Auto Accept mode (both in normal or roaming modes) changeable
	 * 
	 * @return True if client is allowed to change the default Auto Accept mode (both in normal or roaming modes)
	 */
	public boolean isAutoAcceptModeChangeable() {
		return autoAcceptModeChangeable;
	}

	/**
	 * @return returns the max number of simultaneous file transfers
	 */
	public int getMaxFileTransfers() {
		return maxFileTransfers;
	}
	
	/**
	 * @return the image resize option for file transfer in the range: ALWAYS_PERFORM, ONLY_ABOVE_MAX_SIZE, ASK
	 */
	public int getImageResizeOption() {
		return imageResizeOption;
	}
}
