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

import android.net.Uri;

import com.gsma.services.rcs.ft.FileTransferLog;

/**
 * File transfer data constants
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferData {
	/**
	 * Database URI
	 */
	protected static final Uri CONTENT_URI = Uri
			.parse("content://com.orangelabs.rcs.filetransfer/filetransfer");

	/**
	 * Unique file transfer identifier
	 */
	static final String KEY_FT_ID = FileTransferLog.FT_ID;

	/**
	 * Id of chat
	 */
	static final String KEY_CHAT_ID = FileTransferLog.CHAT_ID;

	/**
	 * Date of the transfer
	 */
	static final String KEY_TIMESTAMP = FileTransferLog.TIMESTAMP;
	
	/**
	 * Time when file is sent. If 0 means not sent.
	 */
    static final String KEY_TIMESTAMP_SENT = FileTransferLog.TIMESTAMP_SENT;
    
	/**
	 * Time when file is delivered. If 0 means not delivered.
	 */
    static final String KEY_TIMESTAMP_DELIVERED = FileTransferLog.TIMESTAMP_DELIVERED;
    
	/**
	 * Time when file is displayed.
	 */
    static final String KEY_TIMESTAMP_DISPLAYED = FileTransferLog.TIMESTAMP_DISPLAYED;	

	/**
	 * ContactId formatted number of remote contact or null if the filetransfer
	 * is an outgoing group file transfer.
	 */
	static final String KEY_CONTACT = FileTransferLog.CONTACT;
	
	/**
	 * @see FileTransfer.State for possible states.
	 */
	static final String KEY_STATE = FileTransferLog.STATE;

	/**
	 * Reason code associated with the file transfer state.
	 *
	 * @see FileTransfer.ReasonCode for possible reason codes.
	 */
	static final String KEY_REASON_CODE = FileTransferLog.REASON_CODE;
	
	/**
	 * @see com.gsma.services.rcs.RcsCommon.ReadStatus for the list of status.
	 */
	static final String KEY_READ_STATUS = FileTransferLog.READ_STATUS;

	/**
	 * Multipurpose Internet Mail Extensions (MIME) type of message
	 */
	static final String KEY_MIME_TYPE = FileTransferLog.MIME_TYPE;
	
	/**
	 * URI of the file
	 */
	static final String KEY_FILE = FileTransferLog.FILE;

	/**
	 * Filename
	 */
	static final String KEY_FILENAME = FileTransferLog.FILENAME;
	
	/**
	 * Size transferred in bytes
	 */
	static final String KEY_TRANSFERRED = FileTransferLog.TRANSFERRED;
	
	/**
	 * File size in bytes
	 */
	static final String KEY_FILESIZE = FileTransferLog.FILESIZE;

	/**
	 * Incoming transfer or outgoing transfer
	 *
	 * @see com.gsma.services.rcs.RcsCommon.Direction for the list of directions
	 */
	static final String KEY_DIRECTION = FileTransferLog.DIRECTION;

	/**
	 * Column name KEY_FILEICON : the URI of the file icon
	 */
	static final String KEY_FILEICON =  FileTransferLog.FILEICON;

	/**
	 * URI of the file icon
	 */
	static final String KEY_FILEICON_MIME_TYPE = FileTransferLog.FILEICON_MIME_TYPE;

	static final String KEY_UPLOAD_TID = "upload_tid";

	static final String KEY_DOWNLOAD_URI = "download_uri";
}
