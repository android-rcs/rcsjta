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
package com.orangelabs.rcs.provider.fthttp;

import android.net.Uri;

import java.util.Date;

import com.gsma.services.rcs.contacts.ContactId;

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
	 * The direction TODO : The type of direction is temporarily changed to an
	 * int now and will be changed to an enum as part of the implementation of
	 * CR031.
	 */
	final private int direction;

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
	 * The fileIcon URI
	 */
	final private Uri fileIcon;

	/**
	 * The remote contact identifier
	 */
	final private ContactId contact;

	/**
	 * the Chat Id
	 */
	final private String chatId;

	/**
	 * the file transfer Id
	 */
	final private String fileTransferId;

	/**
	 * Is FT initiated from Group Chat
	 */
	final private boolean isGroup;

	/**
	 * Works just like FtHttpResume(int,Uri,String,String,long,Uri,ContactId,String,String,String,String,boolean,Date) except the date
	 * is always null
	 * 
	 * @see #FtHttpResume(int,Uri,String,String,long,Uri,ContactId,String,String,String,String,boolean,Date)
	 */
	public FtHttpResume(int direction, Uri file, String fileName,
			String mimeType, long size, Uri fileIcon, ContactId contact, String chatId,
			String fileTransferId, boolean isGroup) {
		this(direction, file, fileName, mimeType, size, fileIcon, contact, chatId,
				fileTransferId, isGroup, null);
	}

	/**
	 * Creates an instance of FtHttpResume Data Object
	 * 
	 * @param direction
	 *            the {@code direction} value.
	 * @param file
	 *            the {@code Uri of file} value.
	 * @param fileName
	 *            the {@code fileName} value.
     * @param mimeType
     *            the {@code mimeType} value.
     * @param size
     *            the {@code size} value.
	 * @param fileIcon
	 *            the {@code fileIcon} value.
	 * @param contact
	 *            the {@code contactId} value.
	 * @param chatId
	 *            the {@code chatId} value.
	 * @param fileTransferId
	 *            the {@code fileTransferId} value.
	 * @param isGroup
	 *            the {@code isGroup} value.
	 * @param date
	 *            the {@code date} value.
	 */
	public FtHttpResume(int direction, Uri file, String fileName, String mimeType, long size,
	        Uri fileIcon, ContactId contact, String chatId, String fileTransferId,
	        boolean isGroup, Date date) {
		if (size <= 0 || mimeType == null || file == null || fileName == null)
			throw new IllegalArgumentException("Null argument");
		this.date = date;
		this.direction = direction;
		this.file = file;
		this.fileName = fileName;
        this.mimeType = mimeType;
        this.size = size;
		this.fileIcon = fileIcon;
		this.contact = contact;
		this.chatId = chatId;
		this.fileTransferId = fileTransferId;
		this.isGroup = isGroup;
	}

	public Date getDate() {
		return date;
	}

	public int getDirection() {
		return direction;
	}

	public Uri getFile() {
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
		return fileIcon;
	}

	public ContactId getContact() {
		return contact;
	}

	public String getChatId() {
		return chatId;
	}

	public String getFileTransferId() {
		return fileTransferId;
	}

	public boolean isGroup() {
		return isGroup;
	}

	@Override
	public String toString() {
		return "FtHttpResume [date=" + date + ", dir=" + direction + ", file=" + file + ", fileName=" + fileName + ",fileIcon="+fileIcon+"]";
	}

}
