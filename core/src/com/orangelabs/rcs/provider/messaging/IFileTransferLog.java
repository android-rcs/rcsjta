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

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.provider.fthttp.FtHttpResume;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeUpload;

import android.net.Uri;

import java.util.List;

/**
 * Interface for the ft table
 * 
 * @author LEMORDANT Philippe
 * 
 */
public interface IFileTransferLog {

	/**
	 * Add outgoing file transfer
	 * 
	 * @param contact
	 *            Contact ID
	 * @param fileTransferId
	 *            File Transfer ID
	 * @param direction
	 *            Direction
	 * @param content
	 *            File content
	 * @param fileIcon
	 *            Fileicon content
	 * @param state
	 *            File transfer state
	 * @param reasonCode
	 *            Reason code
	 */
	public void addFileTransfer(ContactId contact, String fileTransferId, int direction,
			MmContent content, MmContent fileIcon, int state, int reasonCode);

	/**
	 * Add an outgoing File Transfer supported by Group Chat
	 * 
	 * @param chatSessionId
	 *            the identity of the group chat
	 * @param fileTransferId
	 *            the identity of the file transfer
	 * @param content
	 *            the File content
	 * @param Fileicon
	 *            the fileIcon content
	 */
	public void addOutgoingGroupFileTransfer(String chatId, String fileTransferId,
			MmContent content, MmContent fileIcon);

	/**
	 * Add incoming group file transfer
	 * 
	 * @param contact
	 *            Contact ID
	 * @param fileTransferId
	 *            File transfer ID
	 * @param chatId
	 *            Chat ID
	 * @param content
	 *            File content
	 * @param fileIcon
	 *            Fileicon contentID
	 * @param state
	 *            File transfer state
	 * @param reasonCode
	 *            Reason code
	 */
	public void addIncomingGroupFileTransfer(String chatId, ContactId contact, String fileTransferId, MmContent content,
			MmContent fileIcon, int state, int reasonCode);

	/**
	 * Update file transfer state
	 * 
	 * @param fileTransferId
	 *            File transfer ID
	 * @param state
	 *            File transfer state
	 * @param reasonCode
	 *            File transfer state reason code
	 */
	public void updateFileTransferStateAndReasonCode(String fileTransferId,
			int state, int reasonCode);

	/**
	 * Update file transfer read status
	 * 
	 * @param fileTransferId
	 *            File transfer ID
	 */
	public void markFileTransferAsRead(String fileTransferId);

	/**
	 * Update file transfer download progress
	 * 
	 * @param fileTransferId
	 *            File transfer ID
	 * @param currentSize
	 *            Current size
	 */
	public void updateFileTransferProgress(String fileTransferId, long currentSize);

	/**
	 * Update file transfer URI
	 * 
	 * @param fileTransferId
	 *            File transfer ID
	 * @param content
	 *            the MmContent of received file
	 */
	public void updateFileTransferred(String fileTransferId, MmContent content);

	/**
	 * Tells if the MessageID corresponds to that of a file transfer
	 * 
	 * @param fileTransferId
	 *            File Transfer Id
	 * @return boolean If there is File Transfer corresponding to msgId
	 */
	public boolean isFileTransfer(String fileTransferId);

	/**
	 * Update file transfer ChatId
	 * 
	 * @param fileTransferId
	 *            File transfer ID
	 * @param chatId
	 *            chat Id
	 */
	public void updateFileTransferChatId(String fileTransferId, String chatId);

	/**
	 * Set file upload TID
	 *
	 * @param fileTransferId
	 *            File transfer ID
	 * @param tId
	 *            TID
	 */
	public void setFileUploadTId(String fileTransferId, String tId);

	/**
	 * Set file download server uri
	 *
	 * @param fileTransferId
	 *            File transfer ID
	 * @param downloadAddress
	 *            Download Address
	 */
	public void setFileDownloadAddress(String fileTransferId, Uri downloadAddress);

	/**
	 * Retrieve file transfers paused by SYSTEM on connection loss
	 */
	public List<FtHttpResume> retrieveFileTransfersPausedBySystem();

	/**
	 * Retrieve resumable file upload
	 *
	 * @param tId Unique Id used while uploading
	 */
	public FtHttpResumeUpload retrieveFtHttpResumeUpload(String tId);
}
