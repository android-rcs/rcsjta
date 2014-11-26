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

package com.orangelabs.rcs.provider.sharing;

import com.gsma.services.rcs.ish.ImageSharingLog;

/**
 * Image sharing data constants
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingData {

	/**
	 * Unique sharing identifier
	 */
	static final String KEY_SHARING_ID = ImageSharingLog.SHARING_ID;

	/**
	 * Date of the sharing
	 */
	static final String KEY_TIMESTAMP = ImageSharingLog.TIMESTAMP;

	/**
	 * ContactId formatted number of the remote contact
	 */
	static final String KEY_CONTACT = ImageSharingLog.CONTACT;
	
	/**
	 * @see ImageSharing.State
	 */
	static final String KEY_STATE = ImageSharingLog.STATE;

	/**
	 * Reason code associated with the image sharing state.
	 *
	 * @see ImageSharing.ReasonCode
	 */
	static final String KEY_REASON_CODE = ImageSharingLog.REASON_CODE;

	/**
	 * Multipurpose Internet Mail Extensions (MIME) type of file
	 */
	static final String KEY_MIME_TYPE = ImageSharingLog.MIME_TYPE;
	
	/**
	 * URI of the file
	 */
	static final String KEY_FILE = ImageSharingLog.FILE;

	/**
	 * Filename
	 */
	static final String KEY_FILENAME = ImageSharingLog.FILENAME;

	/**
	 * Size transferred in bytes
	 */
	static final String KEY_TRANSFERRED = ImageSharingLog.TRANSFERRED;

	/**
	 * File size in bytes
	 */
	static final String KEY_FILESIZE = ImageSharingLog.FILESIZE;

	/**
	 * Incoming sharing or outgoing sharing.
	 *
	 * @see com.gsma.services.rcs.RcsCommon.Direction for the list of directions
	 */
	static final String KEY_DIRECTION = ImageSharingLog.DIRECTION;	
}
