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

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.HttpFileTransferSession;

import android.net.Uri;

/**
 * @author YPLO6403
 * 
 *         Class to handle FtHttpResumeDownload data objects
 * 
 */
public final class FtHttpResumeDownload extends FtHttpResume {

	/**
	 * The URI to download file from
	 */
	final private Uri downloadServerAddress;

	/**
	 * Creates a FT HTTP resume download data object (immutable)
	 * 
	 * @param session
	 *            the {@code session} instance.
	 * @param downloadServerAddress
	 *            the {@code downloadServerAddress} instance.
	 * @param file
	 *            the {@code file} value.
	 * @param filetransferId
	 *            the {@code filetransferId} value.
	 * @param fileicon
	 *            the {@code fileicon} value.
	 * @param isGroup
	 *            the {@code isGroup} value.
	 */
	public FtHttpResumeDownload(HttpFileTransferSession session, Uri downloadServerAddress, Uri file, String filetransferId, Uri fileicon,
			boolean isGroup) {
		this(downloadServerAddress, file, fileicon, session.getContent(), session.getRemoteContact(), session.getRemoteDisplayName(),
				session.getContributionID(), filetransferId, session.getChatSessionID(), isGroup);
	}

	/**
	 * Creates a FT HTTP resume download data object
	 * 
	 * @param downloadServerAddress
	 *            the {@code downloadServerAddress} instance.
	 * @param file
	 *            the {@code file} value.
	 * @param fileicon
	 *            the {@code fileicon} value.
	 * @param content
	 *            the {@code content} content.
	 * @param contactId
	 *            the {@code contactId} value.
	 * @param displayName
	 *            the {@code displayName} value.
	 * @param chatId
	 *            the {@code chatId} value.
	 * @param filetransferId
	 *            the {@code filetransferId} value.
	 * @param chatSessionId
	 *            the {@code chatSessionId} value.
	 * @param isGroup
	 *            the {@code isGroup} value.
	 */
	public FtHttpResumeDownload(Uri downloadServerAddress, Uri file, Uri fileicon, MmContent content, ContactId contactId,
			String displayName, String chatId, String filetransferId, String chatSessionId, boolean isGroup) {
		super(FtHttpDirection.INCOMING, file, content.getName(), content.getEncoding(), content.getSize(), fileicon, contactId, displayName, chatId, filetransferId, chatSessionId, isGroup);
		this.downloadServerAddress = downloadServerAddress;
		if (downloadServerAddress == null || filetransferId == null)
			throw new IllegalArgumentException("Invalid argument");
	}

	public Uri getDownloadServerAddress() {
		return downloadServerAddress;
	}

	@Override
	public String toString() {
		return "FtHttpResumeDownload [downloadServerAddress=" + downloadServerAddress + ", file=" + getFileUri() + ",getFileName()=" + getFileName()
				+ ", getSize()=" + getSize() + ", getFileicon()=" + getFileicon() + ", getContact()=" + getContact()
				+ ", getChatId()=" + getChatId() + ", getFileTransferId()=" + getFileTransferId() + ", getChatSessionId()="
				+ getChatSessionId() + ", isGroup()=" + isGroup() + "]";
	}

}
