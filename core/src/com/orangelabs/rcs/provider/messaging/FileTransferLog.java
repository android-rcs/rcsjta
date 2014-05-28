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

package com.orangelabs.rcs.provider.messaging;

import java.io.File;
import java.util.Calendar;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.ft.FileTransfer;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Class to interface the ft table
 * 
 */
public class FileTransferLog implements IFileTransferLog {

	/**
	 * File transfer database URI
	 */
	private Uri ftDatabaseUri = FileTransferData.CONTENT_URI;

	private static final String SELECTION_FILE_BY_FT_ID = new StringBuilder(FileTransferData.KEY_FT_ID).append("=?").toString();

	/**
	 * Content resolver
	 */
	private ContentResolver cr;
	private GroupChatLog groupChatLog;
	private GroupChatDeliveryInfoLog groupChatDeliveryInfoLog;
	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(FileTransferLog.class.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param cr
	 *            Content resolver
	 * @param groupChatLog
	 * @param groupChatDeliveryInfoLog
	 */
	/* package private */FileTransferLog(ContentResolver cr, GroupChatLog groupChatLog,
			GroupChatDeliveryInfoLog groupChatDeliveryInfoLog) {
		this.cr = cr;
		this.groupChatLog = groupChatLog;
	}

	@Override
	public void addFileTransfer(String contact, String fileTransferId, int direction, MmContent content, MmContent thumbnail) {
		contact = PhoneUtils.extractNumberFromUri(contact);
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add file transfer entry: fileTransferId=").append(fileTransferId).append(", contact=")
					.append(contact).append(", filename=").append(content.getName()).append(", size=").append(content.getSize())
					.append(", MIME=").append(content.getEncoding()).toString());
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_FT_ID, fileTransferId);
		values.put(FileTransferData.KEY_CHAT_ID, contact);
		values.put(FileTransferData.KEY_CONTACT, contact);
		if (direction == FileTransfer.Direction.OUTGOING) {
			values.put(FileTransferData.KEY_FILE, content.getUri().toString());
		}
		values.put(FileTransferData.KEY_NAME, content.getName());
		values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
		values.put(FileTransferData.KEY_DIRECTION, direction);
		values.put(FileTransferData.KEY_SIZE, 0);
		values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());
		if (thumbnail != null) {
			values.put(FileTransferData.KEY_FILEICON, thumbnail.getUri().toString());
		}

		long date = Calendar.getInstance().getTimeInMillis();
		values.put(FileTransferData.KEY_READ_STATUS, FileTransfer.ReadStatus.UNREAD);
		if (direction == FileTransfer.Direction.INCOMING) {
			// Receive file
			values.put(FileTransferData.KEY_TIMESTAMP, date);
			values.put(FileTransferData.KEY_TIMESTAMP_SENT, 0);
			values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
			values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INVITED);
		} else {
			// Send file
			values.put(FileTransferData.KEY_TIMESTAMP, date);
			values.put(FileTransferData.KEY_TIMESTAMP_SENT, date);
			values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
			values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INITIATED);
		}
		cr.insert(ftDatabaseUri, values);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#addOutgoingGroupFileTransfer(java.lang.String, java.lang.String,
	 * com.orangelabs.rcs.core.content.MmContent, com.orangelabs.rcs.core.content.MmContent)
	 */
	@Override
	public void addOutgoingGroupFileTransfer(String chatId, String fileTransferId, MmContent content, MmContent thumbnail) {
		if (logger.isActivated()) {
			logger.debug("addOutgoingGroupFileTransfer: fileTransferId=" + fileTransferId + ", chatId=" + chatId + " filename="
					+ content.getName() + ", size=" + content.getSize() + ", MIME=" + content.getEncoding());
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_FT_ID, fileTransferId);
		values.put(FileTransferData.KEY_CHAT_ID, chatId);
		values.put(FileTransferData.KEY_FILE, content.getUri().toString());
		values.put(FileTransferData.KEY_NAME, content.getName());
		values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
		values.put(FileTransferData.KEY_DIRECTION, FileTransfer.Direction.OUTGOING);
		values.put(FileTransferData.KEY_SIZE, 0);
		values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());
		long date = Calendar.getInstance().getTimeInMillis();
		values.put(MessageData.KEY_READ_STATUS, ChatLog.Message.ReadStatus.UNREAD);
		// Send file
		values.put(FileTransferData.KEY_TIMESTAMP, date);
		values.put(FileTransferData.KEY_TIMESTAMP_SENT, date);
		values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
		values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INITIATED);
		if (thumbnail != null) {
			values.put(FileTransferData.KEY_FILEICON, thumbnail.getUri().toString());
		}
		cr.insert(ftDatabaseUri, values);

		try {
			Set<ParticipantInfo> participants = groupChatLog.getGroupChatConnectedParticipants(chatId);
			for (ParticipantInfo participant : participants) {
				groupChatDeliveryInfoLog.addGroupChatDeliveryInfoEntry(chatId, fileTransferId, participant.getContact());
			}
		} catch (Exception e) {
			cr.delete(ftDatabaseUri, FileTransferData.KEY_FT_ID + "='" + fileTransferId + "'", null);
			cr.delete(Uri.withAppendedPath(GroupChatDeliveryInfoData.CONTENT_MSG_URI, fileTransferId), null, null);
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("Group file transfer with fileTransferId '" + fileTransferId + "' could not be added to database!");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#addIncomingGroupFileTransfer(java.lang.String, java.lang.String,
	 * java.lang.String, com.orangelabs.rcs.core.content.MmContent, com.orangelabs.rcs.core.content.MmContent)
	 */
	@Override
	public void addIncomingGroupFileTransfer(String chatId, String contact, String fileTransferId, MmContent content,
			MmContent thumbnail) {
		contact = PhoneUtils.extractNumberFromUri(contact);
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add incoming file transfer entry: fileTransferId=").append(fileTransferId)
					.append(", chatId=").append(chatId).append(", contact=").append(contact).append(", filename=")
					.append(content.getName()).append(", size=").append(content.getSize()).append(", MIME=")
					.append(content.getEncoding()).toString());
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_FT_ID, fileTransferId);
		values.put(FileTransferData.KEY_CHAT_ID, chatId);
		values.put(FileTransferData.KEY_CONTACT, contact);
		values.put(FileTransferData.KEY_NAME, content.getName());
		values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
		values.put(FileTransferData.KEY_DIRECTION, FileTransfer.Direction.INCOMING);
		values.put(FileTransferData.KEY_SIZE, 0);
		values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());
		values.put(FileTransferData.KEY_READ_STATUS, FileTransfer.ReadStatus.UNREAD);

		long date = Calendar.getInstance().getTimeInMillis();
		values.put(FileTransferData.KEY_TIMESTAMP, date);
		values.put(FileTransferData.KEY_TIMESTAMP_SENT, 0);
		values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
		values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INVITED);
		if (thumbnail != null) {
			values.put(FileTransferData.KEY_FILEICON, thumbnail.getUri().toString());
		}

		cr.insert(ftDatabaseUri, values);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#updateFileTransferStatus(java.lang.String, int)
	 */
	@Override
	public void updateFileTransferStatus(String fileTransferId, int status) {
		if (logger.isActivated()) {
			logger.debug("updateFileTransferStatus (status=" + status + ") (fileTransferId=" + fileTransferId + ")");
		}
		// TODO FUSION to check
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_STATUS, status);
		if (status == FileTransfer.State.DELIVERED) {
			// Delivered
			values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, Calendar.getInstance().getTimeInMillis());
		} else if (status == FileTransfer.State.DISPLAYED) {
			// Displayed
			values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, Calendar.getInstance().getTimeInMillis());
		}
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] { fileTransferId });
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#markFileTransferAsRead(java.lang.String)
	 */
	@Override
	public void markFileTransferAsRead(String fileTransferId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("markFileTransferAsRead  (fileTransferId=").append(fileTransferId).append(")")
					.toString());
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_READ_STATUS, FileTransfer.ReadStatus.READ);
		if (cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] { fileTransferId }) < 1) {
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("There was no file with fileTransferId '" + fileTransferId + "' to mark as read.");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#updateFileTransferProgress(java.lang.String, long, long)
	 */
	@Override
	public void updateFileTransferProgress(String fileTransferId, long size, long totalSize) {
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_SIZE, size);
		values.put(FileTransferData.KEY_TOTAL_SIZE, totalSize);
		values.put(FileTransferData.KEY_STATUS, FileTransfer.State.STARTED);
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] { fileTransferId });
	}

	/* (non-Javadoc)
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#updateFileTransferred(java.lang.String, com.orangelabs.rcs.core.content.MmContent)
	 */
	@Override
	public void updateFileTransferred(String fileTransferId, MmContent content) {
		if (logger.isActivated()) {
			logger.debug("updateFileTransferUri (fileTransferId=" + fileTransferId + ") (uri=" + content.getUri() + ")");
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_FILE, content.getUri().toString());
		values.put(FileTransferData.KEY_STATUS, FileTransfer.State.TRANSFERRED);
		values.put(FileTransferData.KEY_SIZE,content.getSize());
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] { fileTransferId });
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#isFileTransfer(java.lang.String)
	 */
	@Override
	public boolean isFileTransfer(String fileTransferId) {
		if (logger.isActivated()) {
			logger.debug("isFileTransfer (fileTransferId=" + fileTransferId + ")");
		}
		Cursor cursor = null;
		try {
			cursor = cr.query(ftDatabaseUri, new String[] { FileTransferData.KEY_ID }, SELECTION_FILE_BY_FT_ID,
					new String[] { fileTransferId }, null);

			return cursor.moveToFirst();
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Exception occured while determing if it is file transfer", e);
			}
			return false;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IFileTransferLog#updateFileTransferChatId(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateFileTransferChatId(String fileTransferId, String chatId) {
		if (logger.isActivated()) {
			logger.debug("updateFileTransferChatId (chatId=" + chatId + ") (fileTransferId=" + fileTransferId + ")");
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_CHAT_ID, chatId);
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] { fileTransferId });
	}

}
