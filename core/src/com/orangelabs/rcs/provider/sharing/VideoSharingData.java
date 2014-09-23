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

import com.gsma.services.rcs.vsh.VideoSharingLog;

import android.net.Uri;

/**
 * Video sharing data constants
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingData {
	/**
	 * Database URI
	 */
	protected static final Uri CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.vsh/vsh");
	
	/**
	 * Column name
	 */
	static final String KEY_ID = VideoSharingLog.ID;

	/**
	 * Column name
	 */
	static final String KEY_SESSION_ID = VideoSharingLog.SHARING_ID;

	/**
	 * Column name
	 */
	static final String KEY_CONTACT = VideoSharingLog.CONTACT;

	/**
	 * Column name
	 */
	static final String KEY_TIMESTAMP = VideoSharingLog.TIMESTAMP;

	/**
	 * Column name
	 */
	static final String KEY_STATE = VideoSharingLog.STATE;

	/**
	 * Column name
	 */
	static final String KEY_REASON_CODE = VideoSharingLog.REASON_CODE;

	/**
	 * Column name
	 */
	static final String KEY_DIRECTION = VideoSharingLog.DIRECTION;	

	/**
	 * Column name
	 */
	static final String KEY_DURATION = VideoSharingLog.DURATION;	

	static final String KEY_VIDEO_ENCODING = VideoSharingLog.VIDEO_ENCODING;

	static final String KEY_WIDTH = VideoSharingLog.WIDTH;

	static final String KEY_HEIGHT = VideoSharingLog.HEIGHT;
}
