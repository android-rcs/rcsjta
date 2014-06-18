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

import android.net.Uri;

/**
 * Image sharing data constants
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingData {
	/**
	 * Database URI
	 */
	protected static final Uri CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.ish/ish");
	
	/**
	 * Column name
	 */
	static final String KEY_ID = ImageSharingLog.ID;
	
	/**
	 * Column name
	 */
	static final String KEY_SESSION_ID = ImageSharingLog.SHARING_ID;

	/**
	 * Column name
	 */
	static final String KEY_TIMESTAMP = ImageSharingLog.TIMESTAMP;

	/**
	 * Column name
	 */
	static final String KEY_CONTACT = ImageSharingLog.CONTACT_NUMBER;
	
	/**
	 * Column name
	 */
	static final String KEY_STATE = ImageSharingLog.STATE;

	/**
	 * Column name
	 */
	static final String KEY_REASON_CODE = ImageSharingLog.REASON_CODE;

	/**
	 * Column name
	 */
	static final String KEY_MIME_TYPE = ImageSharingLog.MIME_TYPE;
	
	/**
	 * Column name
	 */
	static final String KEY_FILE = ImageSharingLog.FILE;

	/**
	 * Column name
	 */
	static final String KEY_NAME = ImageSharingLog.FILENAME;
	
	/**
	 * Column name
	 */
	static final String KEY_SIZE = ImageSharingLog.TRANSFERRED;
	
	/**
	 * Column name
	 */
	static final String KEY_TOTAL_SIZE = ImageSharingLog.FILESIZE;	

	/**
	 * Column name
	 */
	static final String KEY_DIRECTION = ImageSharingLog.DIRECTION;	
}
