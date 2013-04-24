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

package com.orangelabs.rcs.provider.sharing;

import java.util.Calendar;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.orangelabs.rcs.core.content.LiveVideoContent;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Rich call history. This content provider removes old messages if there is no enough space.
 * 
 * @author mhsm6403
 */
public class RichCall {
	/**
	 * Current instance
	 */
	private static RichCall instance = null;

	/**
	 * Content resolver
	 */
	private ContentResolver cr;
	
	/**
	 * Database URI
	 */
	private Uri databaseUri = RichCallData.CONTENT_URI;
	
	/**
	 * Max log entries
	 */
	private int maxLogEntries;
	
	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	/**
	 * Create instance
	 * 
	 * @param ctx Context
	 */
	public static synchronized void createInstance(Context ctx) {
		if (instance == null) {
			instance = new RichCall(ctx);
		}
	}
	
	/**
	 * Returns instance
	 * 
	 * @return Instance
	 */
	public static RichCall getInstance() {
		return instance;
	}
	
	/**
     * Constructor
     * 
     * @param ctx Application context
     */
	private RichCall(Context ctx) {
		super();
		
        this.cr = ctx.getContentResolver();
        this.maxLogEntries = RcsSettings.getInstance().getMaxRichcallLogEntriesPerContact();
    }
	
	/**
	 * Add a new entry in the call history 
	 * 
	 * @param contact Remote contact
	 * @param sessionId Session ID
	 * @param direction Call event direction
	 * @param content Shared content
	 * @param status Call status
	 */
	public Uri addCall(String contact, String sessionId, int direction, MmContent content, int status) {
		if(logger.isActivated()){
			logger.debug("Add new call entry for contact " + contact + ": session=" + sessionId + ", status=" + status);
		}

		contact = PhoneUtils.extractNumberFromUri(contact);
		ContentValues values = new ContentValues();
		values.put(RichCallData.KEY_SESSION_ID, sessionId);
		values.put(RichCallData.KEY_CONTACT, contact);
		values.put(RichCallData.KEY_DESTINATION, direction);
		values.put(RichCallData.KEY_NAME, content.getName());
		values.put(RichCallData.KEY_MIME_TYPE, content.getEncoding());
		if (!content.getUrl().equalsIgnoreCase(LiveVideoContent.URL)){
			// In case of live video content, we do not want to save the uri, as it is meaningless (we won't be able to replay it)
			values.put(RichCallData.KEY_DATA, content.getUrl());
		}else{
			values.put(RichCallData.KEY_DATA, "");
		}
		values.put(RichCallData.KEY_SIZE, content.getKbSize());
		values.put(RichCallData.KEY_NUMBER_MESSAGES, purge(contact)+1);
		values.put(RichCallData.KEY_STATUS, status);
		values.put(RichCallData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
		return cr.insert(databaseUri, values);
	}
	
	/**
	 * Purge older calls. The first call is removed if no enough place for new calls.
	 * 
	 * @param contact Contact id
	 * @return the new number a call rows after recycling (normally: current-1)
	 */
	private int purge(String contact){
		// Get first and last message dates for the contact
		Cursor extrem = cr.query(databaseUri, new String[] {
				"min("+RichCallData.KEY_TIMESTAMP+")", 
				"max("+RichCallData.KEY_TIMESTAMP+")"}, 
				RichCallData.KEY_CONTACT +" = \'"+contact+"\'", null, null);
		long minDate = -1 ,maxDate = -1;
		if (extrem.moveToFirst()) {
			minDate = extrem.getLong(0);
			maxDate = extrem.getLong(1);
		}
		extrem.close();
		if (logger.isActivated()) {
			logger.debug("Recycler: minDate=" + minDate + ", maxDate=" + maxDate);
		}
		
		// If no entry for this contact return 0
		if (minDate == -1 && maxDate == -1) {
			return 0;
		}
		
		// Get the current number of messages in the database for the contact */
		Cursor c = cr.query(databaseUri, new String[] {
				RichCallData.KEY_NUMBER_MESSAGES},
				RichCallData.KEY_CONTACT + " = \'" + contact + "\'"
				+" AND "+  RichCallData.KEY_TIMESTAMP + " = " + maxDate, 
				null,
				null);
		int numberOfMessagesForContact = 0;
		if (c.moveToFirst()) {
			numberOfMessagesForContact = c.getInt(0);
			if(logger.isActivated()){
				logger.debug("Recycler : number of messages for this contact = "+numberOfMessagesForContact);
			}
			if(numberOfMessagesForContact<maxLogEntries){
				/* Enough place for another message... do nothing return */
				if(logger.isActivated()){
					logger.debug("Recycler : Enough place for another message, do nothing return");
				}
				c.close();
				return numberOfMessagesForContact;
			}
			if(logger.isActivated()){
				logger.debug("Recycler : Not enough place for another message, we will have to remove something");
			}
			/* Not enough place for another message... we will have to remove something (the older one) */
			int removedMessages = cr.delete(databaseUri, 
					RichCallData.KEY_CONTACT +" = \'"+contact+"\'" 
					+" AND "+RichCallData.KEY_TIMESTAMP+ " = "+minDate, null);
			if(logger.isActivated()){
				logger.debug("Recycler : messages removed : "+removedMessages);
			}
			/* We also will have to set the new number of messages after removing, for the last entry */
			ContentValues values = new ContentValues();
			numberOfMessagesForContact-=removedMessages;
			if(logger.isActivated()){
				logger.debug("Recycler : new number of message after deletion : "+numberOfMessagesForContact);
			}
			/* 
			 * It 's pretty useless to do the following because normally, 
			 * the next entry would be add with the good number after existing recycler.
			 * But if this function is used externally just for recycling, let's do it...  
			 */
			values.put(RichCallData.KEY_NUMBER_MESSAGES, numberOfMessagesForContact);
			int updatedRows = cr.update(databaseUri, values, 
					RichCallData.KEY_CONTACT +" = \'"+contact+"\' " 
					+" AND "+RichCallData.KEY_TIMESTAMP+ " = "+maxDate, null);
			if(logger.isActivated()){
				logger.debug("Recycler : updated rows for the contact (must be 1 or 0 if no more messages) : "+updatedRows);
			}
		}
		c.close();
		return numberOfMessagesForContact;
	}
	
	/**
	 * Delete entry from its date in the call history
	 * 
	 * @param contact Contact id
	 * @param date Date
	 */
	public void removeCall(String contact, long date) {
		if (logger.isActivated()) {
			logger.debug("Delete call for contact " + contact + " at date " + date);
		}	

		// Count entries to be deleted
		Cursor count = cr.query(databaseUri, null, 
				RichCallData.KEY_CONTACT + " = \'"+ contact+"\'"
				+ " AND "+ RichCallData.KEY_TIMESTAMP+ " = "+ date,
				null, 
				null);
		int toBeDeletedRows = count.getCount();
		count.close();
		if (toBeDeletedRows == 0) {
			if (logger.isActivated()) {
				logger.debug("No entry to be deleted");
			}	
			return;
		}
		
		if (logger.isActivated()) {
			logger.debug("DeleteCall: rows to be deleted (should be 1): "+toBeDeletedRows);
		}	
		
		// Manage recycling
		Cursor c  = cr.query(databaseUri, new String[]{
				RichCallData.KEY_TIMESTAMP,
				RichCallData.KEY_NUMBER_MESSAGES}, 
				RichCallData.KEY_CONTACT+" = \'"+contact+"\'", 
				null, RichCallData.KEY_TIMESTAMP + " DESC");
		if (c.moveToFirst()) {
			long maxDate = c.getLong(0);
			int numberForLast = c.getInt(1);

			ContentValues values = new ContentValues();
			values.put(RichCallData.KEY_NUMBER_MESSAGES, numberForLast-toBeDeletedRows);
			// If last entry for this contact equals to this csh message
			if (date==maxDate) {
				// Update the previous one
				if(c.moveToNext()){
					maxDate = c.getLong(0);
				}
			}
			
			// If no more message exists after deleting this one for this contact, the
			// update is useless because it will be made on the message to be deleted.
			int updatedRows = cr.update(databaseUri, values, 
					RichCallData.KEY_TIMESTAMP+ " = "+maxDate
					+" AND "+RichCallData.KEY_CONTACT+" = \'"+contact+"\'", null);
			if(logger.isActivated()){
				logger.debug("DeleteCall : recycling updated rows (should be 1) : "+updatedRows);
			}
		}
		c.close();
		
		int deletedRows = cr.delete(databaseUri, 
				RichCallData.KEY_CONTACT + " = \'"+ contact+"\'"
				+ " AND "+ RichCallData.KEY_TIMESTAMP+ " = "+ date, null);
		if(logger.isActivated()){
			logger.debug("DeleteCall : deleted rows (should be 1) : "+deletedRows);
		}
	}

	/**
	 * Update the status of an entry in the call history
	 * 
	 * @param sessionId Session ID of the entry
	 * @param status New status
	 */
	public void setStatus(String sessionId, int status) {
		if (logger.isActivated()) {
			logger.debug("Update status of session " + sessionId + " to " + status);
		}
		ContentValues values = new ContentValues();
		values.put(RichCallData.KEY_STATUS, status);
		cr.update(databaseUri, values, RichCallData.KEY_SESSION_ID + " = " + sessionId, null);
	}
}
