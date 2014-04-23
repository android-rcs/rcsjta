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
package com.orangelabs.rcs.provider.fthttp;

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
	 * The filename
	 */
	final private String filename;

    /**
     * The mime type of the file to download
     */
    final private String mimeType;
 
    /**
     * The size of the file to download
     */
    final private Long size;

	/**
	 * The thumbnail
	 */
	final private byte[] thumbnail;

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
	 * the session Id
	 */
	final private String sessionId;

	/**
	 * the Chat session Id
	 */
	final private String chatSessionId;

	/**
	 * Is FT initiated from Group Chat
	 */
	final private boolean isGroup;

	/**
	 * Works just like FtHttpResume(Direction,String,byte[],String,String,String,String,String,boolean,Date) except the date
	 * is always null
	 * 
	 * @see #FtHttpResume(FtHttpDirection,String,String,Long,byte[],String,String,String,String,String,boolean,Date)
	 */
	public FtHttpResume(FtHttpDirection ftHttpDirection, String filename, String mimeType, Long size,
            byte[] thumbnail, String contact, String displayName, String chatId, String sessionId,
            String chatSessionId, boolean isGroup) {
        this(ftHttpDirection, filename, mimeType, size, thumbnail, contact, displayName, chatId,
                sessionId, chatSessionId, isGroup, null);
	}

	/**
	 * Creates an instance of FtHttpResume Data Object
	 * 
	 * @param ftHttpDirection
	 *            the {@code direction} value.
	 * @param filename
	 *            the {@code filename} value.
     * @param mimeType
     *            the {@code mimeType} value.
     * @param size
     *            the {@code size} value.
	 * @param thumbnail
	 *            the {@code thumbnail} byte array.
	 * @param contact
	 *            the {@code contact} value.
	 * @param displayName
	 *            the {@code displayName} value.
	 * @param chatId
	 *            the {@code chatId} value.
	 * @param sessionId
	 *            the {@code sessionId} value.
	 * @param chatSessionId
	 *            the {@code chatSessionId} value.
	 * @param isGroup
	 *            the {@code isGroup} value.
	 * @param date
	 *            the {@code date} value.
	 */
	public FtHttpResume(FtHttpDirection ftHttpDirection, String filename, String mimeType, Long size,
	        byte[] thumbnail, String contact, String displayName, String chatId, String sessionId,
	        String chatSessionId, boolean isGroup, Date date) {
		if (size <= 0 || ftHttpDirection == null || mimeType == null || filename == null)
			throw new IllegalArgumentException("Null argument");
		this.date = date;
		this.ftHttpDirection = ftHttpDirection;
		this.filename = filename;
        this.mimeType = mimeType;
        this.size = size;
		this.thumbnail = thumbnail;
		this.contact = contact;
		this.displayName = displayName;
		this.chatId = chatId;
		this.sessionId = sessionId;
		this.chatSessionId = chatSessionId;
		this.isGroup = isGroup;
	}

	/**
	 * Creates a FtHttpResumeUploadGc data object
	 * 
	 * @param cursor
	 *            the {@code cursor} value.
	 */
	public FtHttpResume(FtHttpCursor cursor) {
		if (cursor.getSize() <= 0 || cursor.getDirection() == null || cursor.getFilename() == null || cursor.getType() == null)
			throw new IllegalArgumentException("Null argument");
		this.date = cursor.getDate();
		this.ftHttpDirection = cursor.getDirection();
		this.filename = cursor.getFilename();
        this.mimeType = cursor.getType();
        this.size = cursor.getSize();
		this.thumbnail = cursor.getThumbnail();
		this.contact = cursor.getContact();
		this.displayName = cursor.getDisplayName();
		this.chatId = cursor.getChatid();
		this.sessionId = cursor.getSessionId();
		this.chatSessionId = cursor.getChatSessionId();
		this.isGroup = cursor.isGroup();
	}

	public Date getDate() {
		return date;
	}

	public FtHttpDirection getDirection() {
		return ftHttpDirection;
	}

	public String getFilename() {
		return filename;
	}

    public String getMimetype() {
        return mimeType;
    }

    public Long getSize() {
        return size;
    }

	public byte[] getThumbnail() {
		return thumbnail;
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

	public String getSessionId() {
		return sessionId;
	}

	public String getChatSessionId() {
		return chatSessionId;
	}

	public boolean isGroup() {
		return isGroup;
	}

	@Override
	public String toString() {
		return "FtHttpResume [date=" + date + ", dir=" + ftHttpDirection + ", file=" + filename + "]";
	}

}
