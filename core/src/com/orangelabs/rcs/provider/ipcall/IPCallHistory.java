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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ipcall.IPCallLog;
import com.orangelabs.rcs.core.content.AudioContent;
import com.orangelabs.rcs.core.content.VideoContent;
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

	/**
	 * Content resolver
	 */
	private ContentResolver cr;

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(IPCallHistory.class.getSimpleName());
	
	/**
	 * Empty constructor : prevent caller from creating multiple instances
	 */
	private IPCallHistory() {
	}
	
	/**
	 * Create instance
	 * 
	 * @param ctx Context
	 */
	public static synchronized void createInstance(Context ctx) {
		if (instance == null) {
			instance = new IPCallHistory(ctx);
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
     * @param ctx Application context
     */
	private IPCallHistory(Context ctx) {
		super();
		
        this.cr = ctx.getContentResolver();
    }
	
	/**
	 * Add a new entry in the call history 
	 * 
	 * @param contact Remote contact Id
	 * @param callId Call ID
	 * @param direction Direction 
	 * @param audiocontent Audio content
	 * @param videocontent Video content
	 * @param state Call state
	 * @param  Reason code
	 */
	public Uri addCall(ContactId contact, String callId, int direction, AudioContent audiocontent,
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
		return cr.insert(IPCallLog.CONTENT_URI, values);
	}

	/**
	 * Update the call state
	 * 
	 * @param callId Call ID
	 * @param state New state
	 * @param reasonCode Reason code
	 */
	public void setCallState(String callId, int state, int reasonCode) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Update call state of call ").append(callId)
					.append(" state=").append(state).append(", reasonCode=").append(reasonCode)
					.toString());
		}
		
		ContentValues values = new ContentValues();
		values.put(IPCallData.KEY_STATE, state);
		values.put(IPCallData.KEY_REASON_CODE, reasonCode);
		cr.update(Uri.withAppendedPath(IPCallLog.CONTENT_URI, callId), values, null, null);
	}

	/**
	 * Delete all entries in IP call history
	 */
	public void deleteAllEntries() {
		cr.delete(IPCallLog.CONTENT_URI, null, null);
	}
}
