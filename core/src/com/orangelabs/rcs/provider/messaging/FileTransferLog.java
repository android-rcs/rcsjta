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

package com.orangelabs.rcs.provider.messaging;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.RcsCommon.ReadStatus;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ft.FileTransfer;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.provider.fthttp.FtHttpResume;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeDownload;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeUpload;
import com.orangelabs.rcs.utils.ContactUtils;
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

	private static final String SELECTION_FILE_BY_T_ID = new StringBuilder(FileTransferData.KEY_UPLOAD_TID).append("=?").toString();

	private static final String SELECTION_BY_PAUSED_BY_SYSTEM = new StringBuilder(
			FileTransferData.KEY_STATE).append("=").append(FileTransfer.State.PAUSED)
			.append(" AND ").append(FileTransferData.KEY_REASON_CODE).append("=")
			.append(FileTransfer.ReasonCode.PAUSED_BY_SYSTEM).toString();

	private static final String ORDER_BY_TIMESTAMP_ASC = MessageData.KEY_TIMESTAMP.concat(" ASC");

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
		this.groupChatDeliveryInfoLog = groupChatDeliveryInfoLog;
	}

	@Override
	public void addFileTransfer(ContactId contact, String fileTransferId, int direction,
			MmContent content, MmContent fileIcon, int state, int reasonCode) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add file transfer entry: fileTransferId=")
					.append(fileTransferId).append(", contact=").append(contact)
					.append(", filename=").append(content.getName()).append(", size=")
					.append(content.getSize()).append(", MIME=").append(content.getEncoding())
					.append(", state=").append(state).append(", reasonCode=").append(reasonCode).toString());
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_FT_ID, fileTransferId);
		values.put(FileTransferData.KEY_CHAT_ID, contact.toString());
		values.put(FileTransferData.KEY_CONTACT, contact.toString());
		values.put(FileTransferData.KEY_FILE, content.getUri().toString());
		values.put(FileTransferData.KEY_NAME, content.getName());
		values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
		values.put(FileTransferData.KEY_DIRECTION, direction);
		values.put(FileTransferData.KEY_SIZE, 0);
		values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());
		if (fileIcon != null) {
			values.put(FileTransferData.KEY_FILEICON, fileIcon.getUri().toString());
		}

		long date = Calendar.getInstance().getTimeInMillis();
		values.put(FileTransferData.KEY_READ_STATUS, ReadStatus.UNREAD);
		values.put(FileTransferData.KEY_STATE, state);
		values.put(FileTransferData.KEY_REASON_CODE, reasonCode);
		if (direction == Direction.INCOMING) {
			// Receive file
			values.put(FileTransferData.KEY_TIMESTAMP, date);
			values.put(FileTransferData.KEY_TIMESTAMP_SENT, 0);
			values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
		} else {
			// Send file
			values.put(FileTransferData.KEY_TIMESTAMP, date);
			values.put(FileTransferData.KEY_TIMESTAMP_SENT, date);
			values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
		}
		cr.insert(ftDatabaseUri, values);
	}

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
		values.put(FileTransferData.KEY_DIRECTION, Direction.OUTGOING);
		values.put(FileTransferData.KEY_SIZE, 0);
		values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());
		long date = Calendar.getInstance().getTimeInMillis();
		values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD);
		// Send file
		values.put(FileTransferData.KEY_TIMESTAMP, date);
		values.put(FileTransferData.KEY_TIMESTAMP_SENT, date);
		values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
		values.put(FileTransferData.KEY_STATE, FileTransfer.State.INITIATED);
		values.put(FileTransferData.KEY_REASON_CODE, FileTransfer.ReasonCode.UNSPECIFIED);
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
			if (logger.isActivated()) {
				logger.error("Group file transfer with fileTransferId '" + fileTransferId + "' could not be added to database!", e);
			}
			cr.delete(ftDatabaseUri, FileTransferData.KEY_FT_ID + "='" + fileTransferId + "'", null);
			cr.delete(Uri.withAppendedPath(GroupChatDeliveryInfoData.CONTENT_MSG_URI, fileTransferId), null, null);
			/* TODO: Throw exception */
		}
	}

	@Override
	public void addIncomingGroupFileTransfer(String chatId, ContactId contact,
			String fileTransferId, MmContent content, MmContent fileIcon, int state, int reasonCode) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add incoming file transfer entry: fileTransferId=")
					.append(fileTransferId).append(", chatId=").append(chatId).append(", contact=")
					.append(contact).append(", filename=").append(content.getName())
					.append(", size=").append(content.getSize()).append(", MIME=")
					.append(content.getEncoding()).append(", state=").append(state)
					.append(", reasonCode=").append(reasonCode).toString());
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_FT_ID, fileTransferId);
		values.put(FileTransferData.KEY_CHAT_ID, chatId);
		values.put(FileTransferData.KEY_FILE, content.getUri().toString());
		values.put(FileTransferData.KEY_CONTACT, contact.toString());
		values.put(FileTransferData.KEY_NAME, content.getName());
		values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
		values.put(FileTransferData.KEY_DIRECTION, Direction.INCOMING);
		values.put(FileTransferData.KEY_SIZE, 0);
		values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());
		values.put(FileTransferData.KEY_READ_STATUS, ReadStatus.UNREAD);
		values.put(FileTransferData.KEY_STATE, state);
		values.put(FileTransferData.KEY_REASON_CODE, reasonCode);

		long date = Calendar.getInstance().getTimeInMillis();
		values.put(FileTransferData.KEY_TIMESTAMP, date);
		values.put(FileTransferData.KEY_TIMESTAMP_SENT, 0);
		values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
		if (fileIcon != null) {
			values.put(FileTransferData.KEY_FILEICON, fileIcon.getUri().toString());
		}

		cr.insert(ftDatabaseUri, values);
	}

	@Override
	public void updateFileTransferStateAndReasonCode(String fileTransferId, int state,
			int reasonCode) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("updateFileTransferStatus: fileTransferId=")
					.append(fileTransferId).append(", state=").append(state)
					.append(", reasonCode=").append(reasonCode).toString());
		}

		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_STATE, state);
		values.put(FileTransferData.KEY_REASON_CODE, reasonCode);
		if (state == FileTransfer.State.DELIVERED) {
			values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, Calendar.getInstance()
					.getTimeInMillis());
		} else if (state == FileTransfer.State.DISPLAYED) {
			values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, Calendar.getInstance()
					.getTimeInMillis());
		}
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] { fileTransferId });
	}

	@Override
	public void markFileTransferAsRead(String fileTransferId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("markFileTransferAsRead  (fileTransferId=").append(fileTransferId).append(")")
					.toString());
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_READ_STATUS, ReadStatus.READ);
		if (cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] { fileTransferId }) < 1) {
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("There was no file with fileTransferId '" + fileTransferId + "' to mark as read.");
			}
		}
	}

	@Override
	public void updateFileTransferProgress(String fileTransferId, long currentSize) {
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_SIZE, currentSize);
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] {
			fileTransferId
		});
	}

	@Override
	public void updateFileTransferred(String fileTransferId, MmContent content) {
		if (logger.isActivated()) {
			logger.debug("updateFileTransferUri (fileTransferId=" + fileTransferId + ") (uri=" + content.getUri() + ")");
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_STATE, FileTransfer.State.TRANSFERRED);
		values.put(FileTransferData.KEY_REASON_CODE, FileTransfer.ReasonCode.UNSPECIFIED);
		values.put(FileTransferData.KEY_SIZE, content.getSize());
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] {
			fileTransferId
		});
	}

	@Override
	public boolean isFileTransfer(String fileTransferId) {
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

	@Override
	public void updateFileTransferChatId(String fileTransferId, String chatId) {
		if (logger.isActivated()) {
			logger.debug("updateFileTransferChatId (chatId=" + chatId + ") (fileTransferId=" + fileTransferId + ")");
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_CHAT_ID, chatId);
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] { fileTransferId });
	}

	@Override
	public void setFileUploadTId(String fileTransferId, String tId) {
		if (logger.isActivated()) {
			logger.debug("updateFileUploadTId (tId=" + tId + ") (fileTransferId=" + fileTransferId
					+ ")");
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_UPLOAD_TID, tId);
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] {
			fileTransferId
		});
	}

	@Override
	public void setFileDownloadAddress(String fileTransferId, Uri downloadAddress) {
		if (logger.isActivated()) {
			logger.debug("updateFileDownloadAddress (downloadAddress=" + downloadAddress
					+ ") (fileTransferId=" + fileTransferId + ")");
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_DOWNLOAD_URI, downloadAddress.toString());
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] {
			fileTransferId
		});
	}

	@Override
	public List<FtHttpResume> retrieveFileTransfersPausedBySystem() {
		Cursor cursor = null;
		try {
			cursor = cr.query(ftDatabaseUri, null, SELECTION_BY_PAUSED_BY_SYSTEM,
					null, ORDER_BY_TIMESTAMP_ASC);
			if (!cursor.moveToFirst()) {
				return new ArrayList<FtHttpResume>();
			}

			int sizeColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_SIZE);
			int mimeTypeColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_MIME_TYPE);
			int contactColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CONTACT);
			int chatIdColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CHAT_ID);
			int fileColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILE);
			int fileNameColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_NAME);
			int directionColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_DIRECTION);
			int fileTransferIdColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FT_ID);
			int fileIconColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILEICON);
			int downloadServerAddressColumnIdx = cursor
					.getColumnIndexOrThrow(FileTransferData.KEY_DOWNLOAD_URI);
			int tIdColumnIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_UPLOAD_TID);

			List<FtHttpResume> fileTransfers = new ArrayList<FtHttpResume>();
			do {
				long size = cursor.getLong(sizeColumnIdx);
				String mimeType = cursor.getString(mimeTypeColumnIdx);
				String fileTransferId = cursor.getString(fileTransferIdColumnIdx);
				ContactId contact = null;
				String phoneNumber = cursor.getString(contactColumnIdx);
				try {
					contact = ContactUtils.createContactId(phoneNumber);
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Cannot parse contact '" + phoneNumber
								+ "' for file transfer with transfer ID '" + fileTransferId + "'");
					}
					continue;
				}
				String chatId = cursor.getString(chatIdColumnIdx);
				String file = cursor.getString(fileColumnIdx);
				String fileName = cursor.getString(fileNameColumnIdx);
				int direction = cursor.getInt(directionColumnIdx);
				String fileIcon = cursor.getString(fileIconColumnIdx);
				boolean isGroup = !contact.toString().equals(chatId);
				if (direction == Direction.INCOMING) {
					String downloadServerAddress = cursor.getString(downloadServerAddressColumnIdx);
					MmContent content = ContentManager.createMmContent(Uri.parse(file), size,
							fileName);
					Uri fileIconUri = fileIcon != null ? Uri.parse(fileIcon) : null;
					fileTransfers.add(new FtHttpResumeDownload(Uri.parse(downloadServerAddress),
							Uri.parse(file), fileIconUri, content, contact, chatId, fileTransferId,
							isGroup));
				} else {
					String tId = cursor.getString(tIdColumnIdx);
					MmContent content = ContentManager.createMmContentFromMime(Uri.parse(file),
							mimeType, size, fileName);
					Uri fileIconUri = fileIcon != null ? Uri.parse(fileIcon) : null;
					fileTransfers.add(new FtHttpResumeUpload(content, fileIconUri, tId, contact,
							chatId, fileTransferId, isGroup));
				}
			} while (cursor.moveToNext());
			return fileTransfers;

		} catch (SQLException e) {
			if (logger.isActivated()) {
				logger.error("Unable to retrieve resumable file transfers!", e);
			}
			return new ArrayList<FtHttpResume>();

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@Override
	public FtHttpResumeUpload retrieveFtHttpResumeUpload(String tId) {
		Cursor cursor = null;
		try {
			cursor = cr.query(ftDatabaseUri, null, SELECTION_FILE_BY_T_ID, new String[] {
				tId
			}, null);

			if (!cursor.moveToFirst()) {
				return null;
			}
			String fileName = cursor.getString(cursor
					.getColumnIndexOrThrow(FileTransferData.KEY_NAME));
			long size = cursor.getLong(cursor.getColumnIndexOrThrow(FileTransferData.KEY_SIZE));
			String mimeType = cursor.getString(cursor
					.getColumnIndexOrThrow(FileTransferData.KEY_MIME_TYPE));
			String fileTransferId = cursor.getString(cursor
					.getColumnIndexOrThrow(FileTransferData.KEY_FT_ID));
			ContactId contact = null;
			String phoneNumber = cursor.getString(cursor
					.getColumnIndexOrThrow(FileTransferData.KEY_CONTACT));
			try {
				contact = ContactUtils.createContactId(phoneNumber);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Cannot parse contact '" + phoneNumber
							+ "' for file transfer with transfer ID '" + fileTransferId + "'");
				}
				return null;

			}
			String chatId = cursor.getString(cursor
					.getColumnIndexOrThrow(FileTransferData.KEY_CHAT_ID));
			String file = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILE));
			String fileIcon = cursor.getString(cursor
					.getColumnIndexOrThrow(FileTransferData.KEY_FILEICON));
			boolean isGroup = !contact.toString().equals(chatId);
			MmContent content = ContentManager.createMmContentFromMime(Uri.parse(file), mimeType,
					size, fileName);
			Uri fileIconUri = fileIcon != null ? Uri.parse(fileIcon) : null;
			return new FtHttpResumeUpload(content, fileIconUri, tId, contact, chatId,
					fileTransferId, isGroup);

		} catch (SQLException e) {
			if (logger.isActivated()) {
				logger.error(e.getMessage(), e);
			}
			return null;

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}
