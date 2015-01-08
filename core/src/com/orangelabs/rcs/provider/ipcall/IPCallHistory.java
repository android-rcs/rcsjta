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
package com.orangelabs.rcs.provider.ipcall;

import java.util.Calendar;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ipcall.IPCallLog;
import com.orangelabs.rcs.core.content.AudioContent;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.provider.LocalContentResolver;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * IP call history
 * 
 * @author owom5460
 */
public class IPCallHistory {
	/**
	 * Current instance
	 */
	private static IPCallHistory instance;

	private final LocalContentResolver mLocalContentResolver;

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(IPCallHistory.class.getSimpleName());

	private static final int FIRST_COLUMN_IDX = 0;

	/**
	 * Get IPCall session info from its unique Id
	 * 
	 * @param columnName
	 * @param callId
	 * @return Cursor the caller of this method has to close the cursor if a
	 *         cursor is returned
	 */
	private Cursor getIPCallData(String columnName, String callId) {
		String[] projection = new String[] {
			columnName
		};
		Cursor cursor = null;
		try {
			cursor = mLocalContentResolver.query(
					Uri.withAppendedPath(IPCallLog.CONTENT_URI, callId), projection, null, null,
					null);
			if (cursor.moveToFirst()) {
				return cursor;
			}
			throw new SQLException("No row returned while querying for IP call data with callId : "
					+ callId);

		} catch (RuntimeException e) {
			if (cursor != null) {
				cursor.close();
			}
			throw e;
		}
	}

	private int getDataAsInt(Cursor cursor) {
		try {
			return cursor.getInt(FIRST_COLUMN_IDX);

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Create instance
	 * 
	 * @param localContentResolver Local content resolver
	 */
	public static synchronized void createInstance(LocalContentResolver localContentResolver) {
		if (instance == null) {
			instance = new IPCallHistory(localContentResolver);
		}
	}
	
	/**
	 * Returns instance
	 * 
	 * @return Instance
	 */
	public static IPCallHistory getInstance() {
		return instance;
	}
	
	/**
     * Constructor
     * 
     * @param localContentResolver Local content resolver
     */
	private IPCallHistory(LocalContentResolver localContentResolver) {
		super();
		mLocalContentResolver = localContentResolver;
	}
	
	/**
	 * Add a new entry in the call history
	 * 
	 * @param callId Call ID
	 * @param contact Remote contact Id
	 * @param direction Direction 
	 * @param audiocontent Audio content
	 * @param videocontent Video content
	 * @param state Call state
	 * @param  Reason code
	 */
	public Uri addCall(String callId, ContactId contact, int direction, AudioContent audiocontent,
			VideoContent videocontent, int state, int reasonCode) {
		if(logger.isActivated()){
			logger.debug(new StringBuilder("Add new call entry for contact ").append(contact)
					.append(": call=").append(callId).append(", state=").append(state)
					.append(", reasonCode =").append(reasonCode).toString());
		}

		ContentValues values = new ContentValues();
		values.put(IPCallData.KEY_CALL_ID, callId );
		values.put(IPCallData.KEY_CONTACT, contact.toString());
		values.put(IPCallData.KEY_DIRECTION, direction);
		values.put(IPCallData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
		values.put(IPCallData.KEY_STATE, state);
		values.put(IPCallData.KEY_REASON_CODE, reasonCode);
		if (videocontent != null) {
			values.put(IPCallData.KEY_VIDEO_ENCODING, videocontent.getEncoding());
			values.put(IPCallData.KEY_WIDTH, videocontent.getWidth());
			values.put(IPCallData.KEY_HEIGHT, videocontent.getHeight());
		}
		if (audiocontent != null) {
			values.put(IPCallData.KEY_AUDIO_ENCODING, audiocontent.getEncoding());
		}
		return mLocalContentResolver.insert(IPCallLog.CONTENT_URI, values);
	}

	/**
	 * Set the call state and reason code
	 * 
	 * @param callId Call ID
	 * @param state New state
	 * @param reasonCode Reason code
	 */
	public void setCallStateAndReasonCode(String callId, int state, int reasonCode) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Update call state of call ").append(callId)
					.append(" state=").append(state).append(", reasonCode=").append(reasonCode)
					.toString());
		}
		
		ContentValues values = new ContentValues();
		values.put(IPCallData.KEY_STATE, state);
		values.put(IPCallData.KEY_REASON_CODE, reasonCode);
		mLocalContentResolver.update(Uri.withAppendedPath(IPCallLog.CONTENT_URI, callId),
				values, null, null);
	}

	/**
	 * Delete all entries in IP call history
	 */
	public void deleteAllEntries() {
		mLocalContentResolver.delete(IPCallLog.CONTENT_URI, null, null);
	}

	/**
	 * Get IPCall session state from unique Id
	 * 
	 * @param callId
	 * @return State
	 */
	public int getState(String callId) {
		if (logger.isActivated()) {
			logger.debug("Get IP call state for callId ".concat(callId));
		}
		return getDataAsInt(getIPCallData(IPCallData.KEY_STATE, callId));
	}

	/**
	 * Get IPCall session reason code from unique Id
	 * 
	 * @param callId
	 * @return Reason code
	 */
	public int getReasonCode(String callId) {
		if (logger.isActivated()) {
			logger.debug("Get IP call reason code for callId ".concat(callId));
		}
		return getDataAsInt(getIPCallData(IPCallData.KEY_REASON_CODE, callId));
	}

	/**
	 * Get IPCall session info from its unique Id
	 * 
	 * @param callId
	 * @return Cursor the caller of this method has to close the cursor if a
	 *         cursor is returned
	 */
	public Cursor getCacheableIPCallData(String callId) {
		Cursor cursor = null;
		try {
			cursor = mLocalContentResolver.query(
					Uri.withAppendedPath(IPCallLog.CONTENT_URI, callId), null, null, null, null);
			if (cursor.moveToFirst()) {
				return cursor;
			}
			throw new SQLException("No row returned while querying for IP call data with callId : "
					+ callId);

		} catch (RuntimeException e) {
			if (cursor != null) {
				cursor.close();
			}
			throw e;
		}
	}
}
