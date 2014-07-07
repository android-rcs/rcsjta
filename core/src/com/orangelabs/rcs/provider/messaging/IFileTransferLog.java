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

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.content.MmContent;

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
	 * @param contactId
	 *            Contact ID
	 * @param fileTransferId
	 *            File Transfer ID
	 * @param direction
	 *            Direction
	 * @param content
	 *            File content
	 * @param thumbnail
	 *            Thumbnail content
	 */
	public void addFileTransfer(ContactId contactId, String fileTransferId, int direction, MmContent content, MmContent thumbnail);

	/**
	 * Add an outgoing File Transfer supported by Group Chat
	 * 
	 * @param chatSessionId
	 *            the identity of the group chat
	 * @param fileTransferId
	 *            the identity of the file transfer
	 * @param content
	 *            the File content
	 * @param thumbnail
	 *            The thumbnail content
	 */
	public void addOutgoingGroupFileTransfer(String chatId, String fileTransferId, MmContent content, MmContent thumbnail);

	/**
	 * Add incoming group file transfer
	 * 
	 * @param contactId
	 *            Contact ID
	 * @param fileTransferId
	 *            File transfer ID
	 * @param chatId
	 *            Chat ID
	 * @param content
	 *            File content
	 * @param thumbnail
	 *            Thumbnail content
	 */
	public void addIncomingGroupFileTransfer(String chatId, ContactId contactId, String fileTransferId, MmContent content,
			MmContent thumbnail);

	/**
	 * Update file transfer status
	 * 
	 * @param fileTransferId
	 *            File transfer ID
	 * @param status
	 *            New status
	 * @param contact
	 *            the contact
	 */
	public void updateFileTransferStatus(String fileTransferId, int status);

	/**
	 * Update file transfer status
	 * 
	 * @param fileTransferId
	 *            File transfer ID
	 * @param status
	 *            New status
	 * @param contact
	 *            the contact
	 */
	public void markFileTransferAsRead(String fileTransferId);

	/**
	 * Update file transfer download progress
	 * 
	 * @param fileTransferId
	 *            File transfer ID
	 * @param size
	 *            Downloaded size
	 * @param totalSize
	 *            Total size to download
	 */
	public void updateFileTransferProgress(String fileTransferId, long size, long totalSize);

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

}
