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

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.HttpFileTransferSession;

/**
 * @author YPLO6403
 * 
 *         Class to handle FtHttpResumeDownload data objects
 * 
 */
public final class FtHttpResumeDownload extends FtHttpResume {

	/**
	 * The URL to download file
	 */
	final private String url;

	/**
	 * Creates a FT HTTP resume download data object (immutable)
	 * 
	 * @param session
	 *            the {@code session} instance.
	 * @param filename
	 *            the {@code filename} value.
	 * @param messageId
	 *            the {@code messageId} value.
	 * @param thumbnail
	 *            the {@code thumbnail} value.
	 * @param isGroup
	 *            the {@code isGroup} value.
	 */
	public FtHttpResumeDownload(HttpFileTransferSession session, String filename, String filetransferId, String thumbnail,
			boolean isGroup) {
		this(filename, thumbnail, session.getContent(), session.getRemoteContact(), session.getRemoteDisplayName(),
				session.getContributionID(), filetransferId, session.getChatSessionID(), isGroup);
	}

	/**
	 * Creates a FT HTTP resume download data object
	 * 
	 * @param file
	 *            the {@code file} value.
	 * @param thumbnail
	 *            the {@code thumbnail} value.
	 * @param content
	 *            the {@code content} content.
	 * @param messageId
	 *            the {@code messageId} value.
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
	 */
	public FtHttpResumeDownload(String file, String thumbnail, MmContent content, String contact,
			String displayName, String chatId, String filetransferId, String chatSessionId, boolean isGroup) {
		super(FtHttpDirection.INCOMING, file, content.getEncoding(), content.getSize(), thumbnail, contact, displayName, chatId, filetransferId, chatSessionId, isGroup);
		this.url = content.getUrl();
		if (url == null || filetransferId == null)
			throw new IllegalArgumentException("Invalid argument");
	}

	public String getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return "FtHttpResumeDownload [url=" + url + ", getFilepath()=" + getFilepath()
				+ ", getSize()=" + getSize() + ", getThumbnail()=" + getThumbnail() + ", getContact()=" + getContact()
				+ ", getChatId()=" + getChatId() + ", getFileTransferId()=" + getFileTransferId() + ", getChatSessionId()="
				+ getChatSessionId() + ", isGroup()=" + isGroup() + "]";
	}

}
