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
 *         Class to handle FtHttpResumeUpload data objects
 * 
 */
public class FtHttpResumeUpload extends FtHttpResume {

	/**
	 * The FT HTTP Transfer Id
	 */
	final private String tid;

	/**
	 * Creates a FT HTTP resume upload data object
	 * 
	 * 
	 * @param session
	 *            the {@code session} value.
	 * @param tid
	 *            the {@code tid} value.
	 * @param thumbnail
	 *            the {@code thumbnail} value.
	 * @param isGroup
	 *            the {@code isGroup} value.
	 */
	public FtHttpResumeUpload(HttpFileTransferSession session, String tid, byte[] thumbnail, boolean isGroup) {
		this(session.getContent().getUrl(), thumbnail, session.getContent(), tid, (isGroup) ? null : session.getRemoteContact(), session.getRemoteDisplayName(), session
				.getContributionID(), session.getSessionID(), session.getChatSessionID(), isGroup);
	}

	/**
	 * Creates a FT HTTP resume upload data object
	 * 
	 * @param file
	 *            the {@code file} value.
	 * @param thumbnail
	 *            the {@code thumbnail} value.
	 * @param content
     *            the {@code content} content.
	 * @param tid
	 *            the {@code tid} value.
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
	public FtHttpResumeUpload(String file, byte[] thumbnail, MmContent content, String tid, String contact,
            String displayName, String chatId, String sessionId, String chatSessionId, boolean isGroup) {
        super(FtHttpDirection.OUTGOING, file, content.getEncoding(), content.getSize(), thumbnail, contact,
               displayName, chatId, sessionId, chatSessionId, isGroup);
		if (tid == null)
			throw new IllegalArgumentException("Null tid");
		this.tid = tid;
	}

	/**
	 * Creates a FT HTTP resume upload data object
	 * 
	 * @param cursor
	 *            the {@code cursor} value.
	 */
	public FtHttpResumeUpload(FtHttpCursor cursor) {
		super(cursor);
		this.tid = cursor.getOuTid();
		if (this.tid == null)
			throw new IllegalArgumentException("Null TID");
	}

	public String getTid() {
		return tid;
	}

	@Override
	public String toString() {
		return "FtHttpResumeUpload [tid=" + tid + " file="+ getFilename() + "]";
	}

}
