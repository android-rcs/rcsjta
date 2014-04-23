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
	 * the message Id
	 */
	final private String messageId;

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
	public FtHttpResumeDownload(HttpFileTransferSession session, String filename, String messageId, byte[] thumbnail,
			boolean isGroup) {
		this(filename, thumbnail, session.getContent(), messageId, session.getRemoteContact(), session.getRemoteDisplayName(),
				session.getContributionID(), session.getSessionID(), session.getChatSessionID(), isGroup);
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
	public FtHttpResumeDownload(String file, byte[] thumbnail, MmContent content, String messageId, String contact,
			String displayName, String chatId, String sessionId, String chatSessionId, boolean isGroup) {
		super(FtHttpDirection.INCOMING, file, content.getEncoding(), content.getSize(), thumbnail, contact, displayName, chatId, sessionId, chatSessionId, isGroup);
		this.url = content.getUrl();
		this.messageId = messageId;
		if (url == null || messageId == null)
			throw new IllegalArgumentException("Invalid argument");
	}

	/**
	 * Creates a FT HTTP resume download data object
	 * 
	 * @param cursor
	 *            the {@code cursor} value.
	 */
	public FtHttpResumeDownload(FtHttpCursor cursor) {
		super(cursor);
		this.url = cursor.getInUrl();
		this.messageId = cursor.getMessageId();
		if (this.url == null || messageId == null)
			throw new IllegalArgumentException("Null argument");
	}

	public String getUrl() {
		return url;
	}

	public String getMessageId() {
		return messageId;
	}

	@Override
	public String toString() {
		return "FtHttpResumeDownload [file=" + getFilename() + ", mimeType=" + getMimetype() + ", size=" + getSize() + ", messageId=" + messageId
				+ "]";
	}

}
