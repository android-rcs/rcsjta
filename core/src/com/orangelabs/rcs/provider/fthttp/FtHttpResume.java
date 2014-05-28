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
package com.orangelabs.rcs.provider.fthttp;

import android.net.Uri;

import java.util.Date;

/**
 * @author YPLO6403
 * 
 *         FtHttpResume is the abstract base class for all FT HTTP resume classes
 */
public abstract class FtHttpResume {

	/**
	 * The date of creation
	 */
	final private Date date;

	/**
	 * The direction
	 */
	final private FtHttpDirection ftHttpDirection;

	/**
	 * Uri of file
	 */
	final private Uri file;

	/**
	 * The file name
	 */
	final private String fileName;

    /**
     * The mime type of the file to download
     */
    final private String mimeType;
 
    /**
     * The size of the file to download
     */
    final private long size;

	/**
	 * The fileicon URI
	 */
	final private Uri fileicon;

	/**
	 * The remote contact number
	 */
	final private String contact;

	/**
	 * the display name
	 */
	final private String displayName;

	/**
	 * the Chat Id
	 */
	final private String chatId;

	/**
	 * the file transfer Id
	 */
	final private String fileTransferId;

	/**
	 * the Chat session Id
	 */
	final private String chatSessionId;

	/**
	 * Is FT initiated from Group Chat
	 */
	final private boolean isGroup;

	/**
	 * Works just like FtHttpResume(Direction,String,String,String,String,String,String,String,boolean,Date) except the date
	 * is always null
	 * 
	 * @see #FtHttpResume(FtHttpDirection,String,String,long,String,String,String,String,String,String,boolean,Date)
	 */
	public FtHttpResume(FtHttpDirection ftHttpDirection, Uri file, String fileName, String mimeType, long size,
            Uri fileicon, String contact, String displayName, String chatId, String fileTransferId,
            String chatSessionId, boolean isGroup) {
        this(ftHttpDirection, file, fileName, mimeType, size, fileicon, contact, displayName, chatId,
        		fileTransferId, chatSessionId, isGroup, null);
	}

	/**
	 * Creates an instance of FtHttpResume Data Object
	 * 
	 * @param ftHttpDirection
	 *            the {@code direction} value.
	 * @param file
	 *            the {@code Uri of file} value.
	 * @param fileName
	 *            the {@code fileName} value.
     * @param mimeType
     *            the {@code mimeType} value.
     * @param size
     *            the {@code size} value.
	 * @param fileicon
	 *            the {@code fileicon} value.
	 * @param contact
	 *            the {@code contact} value.
	 * @param displayName
	 *            the {@code displayName} value.
	 * @param chatId
	 *            the {@code chatId} value.
	 * @param fileTransferId
	 *            the {@code fileTransferId} value.
	 * @param chatSessionId
	 *            the {@code chatSessionId} value.
	 * @param isGroup
	 *            the {@code isGroup} value.
	 * @param date
	 *            the {@code date} value.
	 */
	public FtHttpResume(FtHttpDirection ftHttpDirection,  Uri file, String fileName, String mimeType, long size,
	        Uri fileicon, String contact, String displayName, String chatId, String fileTransferId,
	        String chatSessionId, boolean isGroup, Date date) {
		if (size <= 0 || ftHttpDirection == null || mimeType == null || file == null)
			throw new IllegalArgumentException("Null argument");
		this.date = date;
		this.ftHttpDirection = ftHttpDirection;
		this.file = file;
		this.fileName = fileName;
        this.mimeType = mimeType;
        this.size = size;
		this.fileicon = fileicon;
		this.contact = contact;
		this.displayName = displayName;
		this.chatId = chatId;
		this.fileTransferId = fileTransferId;
		this.chatSessionId = chatSessionId;
		this.isGroup = isGroup;
	}

	public Date getDate() {
		return date;
	}

	public FtHttpDirection getDirection() {
		return ftHttpDirection;
	}

	public Uri getFileUri() {
		return file;
	}

	public String getFileName() {
		return fileName;
	}

    public String getMimetype() {
        return mimeType;
    }

    public long getSize() {
        return size;
    }

	public Uri getFileicon() {
		return fileicon;
	}

	public String getContact() {
		return contact;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getChatId() {
		return chatId;
	}

	public String getFileTransferId() {
		return fileTransferId;
	}

	public String getChatSessionId() {
		return chatSessionId;
	}

	public boolean isGroup() {
		return isGroup;
	}

	@Override
	public String toString() {
		return "FtHttpResume [date=" + date + ", dir=" + ftHttpDirection + ", file=" + file + ", fileName=" + fileName + ",fileicon="+fileicon+"]";
	}

}
