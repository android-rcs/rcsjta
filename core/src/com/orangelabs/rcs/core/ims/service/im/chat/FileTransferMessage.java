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
package com.orangelabs.rcs.core.ims.service.im.chat;

import java.util.Date;

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;

/**
 * File transfer message
 * 
 * @author Jean-Marc AUFFRET
 */ 
public class FileTransferMessage extends InstantMessage {
	/**
	 * MIME type
	 */
	public static final String MIME_TYPE = FileTransferHttpInfoDocument.MIME_TYPE;

	/**
	 * File info
	 */
	private String file = null;
		
    /**
     * Constructor for outgoing message
     * 
     * @param messageId Message Id
     * @param remote Remote user identifier
     * @param file File info
     * @param imdnDisplayedRequested Flag indicating that an IMDN "displayed" is requested
     * @param displayName the display name
	 */
	public FileTransferMessage(String messageId, ContactId remote, String file, boolean imdnDisplayedRequested, String displayName) {
        this(messageId, remote, file, imdnDisplayedRequested, null, displayName);
	}
	
	/**
     * Constructor for incoming message
     * 
     * @param messageId Message Id
     * @param remote Remote user identifier
     * @param file File info
     * @param imdnDisplayedRequested Flag indicating that an IMDN "displayed" is requested
	 * @param serverReceiptAt Receipt date of the message on the server
	 * @param displayName the display name
	 */
	public FileTransferMessage(String messageId, ContactId remote, String file, boolean imdnDisplayedRequested, Date serverReceiptAt, String displayName) {
		super(messageId, remote, null, imdnDisplayedRequested, serverReceiptAt, displayName);
		
		this.file = file;
	}

    /**
	 * Get file info
	 * 
	 * @return File info
	 */
	public String getFileInfo() {
		return file;
	}

	@Override
	public String toString() {
		return "FileTransferMessage [file=" + file + ", " + super.toString() + "]";
	}
	
	
}
