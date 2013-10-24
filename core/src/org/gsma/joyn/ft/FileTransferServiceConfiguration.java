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
package org.gsma.joyn.ft;

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
	 * File icon
	 */
	private boolean fileIcon;

	/**
	 * File icon size limit
	 */
	private long maxFileIconSize;
	
	/**
	 * Constructor
	 * 
	 * @param warnSize File transfer size threshold
	 * @param maxSize File transfer size limit
	 * @param autoAcceptMode File transfer auto accept mode
	 * @param fileIcon File icon
	 * @param maxFileIconSize File icon size limit
	 */
	public FileTransferServiceConfiguration(long warnSize, long maxSize, boolean autoAcceptMode, boolean fileIcon, long maxFileIconSize) {
		this.warnSize = warnSize;
		this.maxSize = maxSize;
		this.autoAcceptMode = autoAcceptMode;
		this.fileIcon = fileIcon;
		this.maxFileIconSize = maxFileIconSize;
    }	
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
	 */
	public FileTransferServiceConfiguration(Parcel source) {
		this.warnSize = source.readLong();
		this.maxSize = source.readLong();
		this.autoAcceptMode = source.readInt() != 0;
		this.fileIcon = source.readInt() != 0;
		this.maxFileIconSize = source.readLong();
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
    	dest.writeLong(warnSize);
    	dest.writeLong(maxSize);
    	dest.writeInt(autoAcceptMode ? 1 : 0);    	
    	dest.writeInt(fileIcon ? 1 : 0);    	
    	dest.writeLong(maxFileIconSize);
    }

    /**
     * Parcelable creator
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
	 * @return Returns true if automatically accepted else returns false
	 */
	public boolean isAutoAcceptMode() {
		return autoAcceptMode;
	}

	/**
	 * Is file icon supported 
	 * 
	 * @return Returns true if supported else returns false
	 */
	public boolean isFileIconSupported() {
		return autoAcceptMode;
	}

	/**
	 * Returns the file transfer size limit. It returns 0 if there is no limitation.
	 * 
	 * @return Size in kilobytes
	 */
	public long getMaxFileIconSize() {
		return maxFileIconSize;
	}	
}
