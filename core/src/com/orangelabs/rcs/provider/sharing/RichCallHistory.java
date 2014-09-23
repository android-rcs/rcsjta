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

import java.util.Calendar;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ish.ImageSharing;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Rich call history
 * 
 * @author Jean-Marc AUFFRET
 */
public class RichCallHistory {
	/**
	 * Current instance
	 */
	private static RichCallHistory instance;

	/**
	 * Content resolver
	 */
	private ContentResolver cr;
	
	/**
	 * Database URI for image sharing
	 */
	private Uri ishDatabaseUri = ImageSharingData.CONTENT_URI;
	
	/**
	 * Database URI for video sharing
	 */
	private Uri vshDatabaseUri = VideoSharingData.CONTENT_URI;
	
	/**
	 * The logger
	 */
	private final static Logger logger = Logger.getLogger(RichCallHistory.class.getSimpleName());
	
	private static final String WHERE_CLAUSE_ISH = new StringBuilder(ImageSharingData.KEY_SESSION_ID).append("=?").toString();
	
	/**
	 * Empty constructor : prevent caller from creating multiple instances
	 */
	private RichCallHistory() {
	}

	/**
	 * Create instance
	 * 
	 * @param ctx Context
	 */
	public static synchronized void createInstance(Context ctx) {
		if (instance == null) {
			instance = new RichCallHistory(ctx);
		}
	}
	
	/**
	 * Returns instance
	 * 
	 * @return Instance
	 */
	public static RichCallHistory getInstance() {
		return instance;
	}
	
	/**
     * Constructor
     * 
     * @param ctx Application context
     */
	private RichCallHistory(Context ctx) {
		super();
		
        this.cr = ctx.getContentResolver();
    }
	
	/*--------------------- Video sharing methods ----------------------*/
	
	/**
	 * Add a new video sharing in the call history 
	 * 
	 * @param contact Remote contact ID
	 * @param sessionId Session ID
	 * @param direction Call event direction
	 * @param content Shared content
	 * @param state Call state
	 * @param reasonCode Reason Code
	 */
	public Uri addVideoSharing(ContactId contact, String sessionId, int direction, VideoContent content,
			int state, int reasonCode) {
		if(logger.isActivated()){
			logger.debug(new StringBuilder("Add new video sharing for contact ").append(contact)
					.append(": session=").append(sessionId).append(", state=").append(state)
					.append(", reasonCode=").append(reasonCode).toString());
		}

		ContentValues values = new ContentValues();
		values.put(VideoSharingData.KEY_SESSION_ID, sessionId);
		values.put(VideoSharingData.KEY_CONTACT, contact.toString());
		values.put(VideoSharingData.KEY_DIRECTION, direction);
		values.put(VideoSharingData.KEY_STATE, state);
		values.put(VideoSharingData.KEY_REASON_CODE, reasonCode);
		values.put(VideoSharingData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
		values.put(VideoSharingData.KEY_DURATION, 0);
		values.put(VideoSharingData.KEY_VIDEO_ENCODING, content.getEncoding());
		values.put(VideoSharingData.KEY_WIDTH, content.getWidth());
		values.put(VideoSharingData.KEY_HEIGHT, content.getHeight());
		return cr.insert(vshDatabaseUri, values);
	}

	/**
	 * Update the video sharing status
	 * 
	 * @param sessionId Session ID of the entry
	 * @param state New state
	 * @param reasonCode Reason Code
	 */
	public void setVideoSharingState(String sessionId, int state, int reasonCode) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Update video sharing state of session ")
					.append(sessionId).append(" state=").append(state).append(", reasonCode=")
					.append(reasonCode).toString());
		}
		ContentValues values = new ContentValues();
		values.put(VideoSharingData.KEY_STATE, state);
		values.put(VideoSharingData.KEY_REASON_CODE, reasonCode);
		cr.update(vshDatabaseUri, values, VideoSharingData.KEY_SESSION_ID + " = '" + sessionId + "'", null);
	}

	/**
	 * Update the video sharing duration at the end of the call
	 * 
	 * @param sessionId Session ID of the entry
	 * @param duration Duration
	 */
	public void setVideoSharingDuration(String sessionId, long duration) {
		if (logger.isActivated()) {
			logger.debug("Update duration of session " + sessionId + " to " + duration);
		}
		ContentValues values = new ContentValues();
		values.put(VideoSharingData.KEY_DURATION, duration);
		cr.update(vshDatabaseUri, values, VideoSharingData.KEY_SESSION_ID + " = '" + sessionId + "'", null);
	}
	
	/*--------------------- Image sharing methods ----------------------*/

	/**
	 * Add a new image sharing in the call history 
	 * 
	 * @param contact Remote contact ID
	 * @param sessionId Session ID
	 * @param direction Call event direction
	 * @param content Shared content
	 * @param status Call status
	 * @param reasonCode Reason Code
	 */
	public Uri addImageSharing(ContactId contact, String sessionId, int direction, MmContent content,
			int status, int reasonCode) {
		if(logger.isActivated()){
			logger.debug("Add new image sharing for contact " + contact + ": session=" + sessionId + ", status=" + status);
		}

		ContentValues values = new ContentValues();
		values.put(ImageSharingData.KEY_SESSION_ID, sessionId);
		values.put(ImageSharingData.KEY_CONTACT, contact.toString());
		values.put(ImageSharingData.KEY_DIRECTION, direction);
		values.put(ImageSharingData.KEY_FILE, content.getUri().toString());
		values.put(ImageSharingData.KEY_NAME, content.getName());
		values.put(ImageSharingData.KEY_MIME_TYPE, content.getEncoding());
		values.put(ImageSharingData.KEY_SIZE, 0);
		values.put(ImageSharingData.KEY_TOTAL_SIZE, content.getSize());
		values.put(ImageSharingData.KEY_STATE, status);
		values.put(ImageSharingData.KEY_REASON_CODE, reasonCode);
		values.put(ImageSharingData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
		return cr.insert(ishDatabaseUri, values);
	}

	/**
	 * Update the image sharing state
	 * 
	 * @param sessionId Session ID of the entry
	 * @param state New state
	 * @param reasonCode Reason Code
	 */
	public void setImageSharingState(String sessionId, int state, int reasonCode) {
		if (logger.isActivated()) {
			logger.debug("Update status of image session " + sessionId + " to " + state);
		}
		ContentValues values = new ContentValues();
		values.put(ImageSharingData.KEY_STATE, state);
		values.put(ImageSharingData.KEY_REASON_CODE, reasonCode);
		if (state == ImageSharing.State.TRANSFERRED) {
			// Update the size of bytes if fully transferred
			long total = getImageSharingTotalSize(sessionId);
			if (total != 0) {
				values.put(ImageSharingData.KEY_SIZE, total);
			}
		}
		String[] whereArgs = new String[] { sessionId };
		cr.update(ishDatabaseUri, values, WHERE_CLAUSE_ISH, whereArgs);
	}
	
	/**
     * Read the total size of transferred image
     *
     * @param sessionId the session identifier
     * @return the total size (or 0 if failed)
     */
	public long getImageSharingTotalSize(String sessionId ) {
		Cursor c = null;
		try {
			String[] projection = new String[] { ImageSharingData.KEY_TOTAL_SIZE };
			String[] whereArg = new String[] { sessionId };
			c = cr.query(ishDatabaseUri, projection, WHERE_CLAUSE_ISH, whereArg, null);
			if (c.moveToFirst()) {
				return c.getLong(0);
			}
		} catch (Exception e) {
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return 0L;
	}
	
	/**
	 * Update the image sharing progress
	 * 
	 * @param sessionId Session ID of the entry
	 * @param current Current size
	 * @param total Total size
	 */
	public void setImageSharingProgress(String sessionId, long current, long total) {
		ContentValues values = new ContentValues();
		values.put(ImageSharingData.KEY_SIZE, current);
		values.put(ImageSharingData.KEY_TOTAL_SIZE, total);
		values.put(ImageSharingData.KEY_STATE, ImageSharing.State.STARTED);
		values.put(ImageSharingData.KEY_REASON_CODE, ImageSharing.ReasonCode.UNSPECIFIED);
		String[] whereArgs = new String[] { sessionId };
		cr.update(ishDatabaseUri, values, WHERE_CLAUSE_ISH, whereArgs);
	}

	/**
	 * Delete all entries in Rich Call history
	 */
	public void deleteAllEntries() {
		cr.delete(ImageSharingData.CONTENT_URI, null, null);
		cr.delete(VideoSharingData.CONTENT_URI, null, null);
	}	
}
