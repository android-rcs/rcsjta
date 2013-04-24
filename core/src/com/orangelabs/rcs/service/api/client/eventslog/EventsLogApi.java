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

package com.orangelabs.rcs.service.api.client.eventslog;

import java.util.Date;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.orangelabs.rcs.provider.eventlogs.EventLogData;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.provider.messaging.RichMessagingData;
import com.orangelabs.rcs.provider.sharing.RichCall;
import com.orangelabs.rcs.service.api.client.ClientApi;
import com.orangelabs.rcs.service.api.client.messaging.GeolocMessage;
import com.orangelabs.rcs.service.api.client.messaging.GeolocPush;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.utils.PhoneUtils;

/**
 * Events log API
 */
public class EventsLogApi extends ClientApi {

	/**
	 * Row id in provider
	 */
	public static final int ID_COLUMN = 0;
	
	/**
	 * Entry type
	 */
	public static final int TYPE_COLUMN = 1;

	/**
	 * Id of the chat session 
	 */
	public static final int CHAT_SESSION_ID_COLUMN = 2;
	
	/**
	 * Timestamp for this entry
	 */
	public static final int DATE_COLUMN = 3;
	
	/**
	 * Contact this entry refers to
	 */
	public static final int CONTACT_COLUMN = 4;
	
	/**
	 * Status of this entry
	 */
	public static final int STATUS_COLUMN = 5;
	
	/**
	 * Entry data
	 * 
	 * <br>Holds text for chat/SMS messages, path to the file for file transfers/richcalls, duration for calls 
	 */
	public static final int DATA_COLUMN = 6;
	
	/**
	 * Message Id
	 * 
	 * <br>Holds IMDN id for chat messages or file transfer session id for file transfers
	 */
	public static final int MESSAGE_ID_COLUMN = 7;

    /**
     * Mime-type of the content
     * 
     * <br>Only relevant for file transfer
     */
    public static final int MIMETYPE_COLUMN = 8;

    /**
     * Name of the content
     * 
     * <br>Only relevant for file transfer
     */
    public static final int NAME_COLUMN = 9;

    /**
     * Size already transfered
     * 
     * <br>Only relevant for file transfer or rich call
     */
    public static final int SIZE_COLUMN = 10;

    /**
     * Total size of the file
     *   
     * <br>Only relevant for file transfer or rich call
     */
    public static final int TOTAL_SIZE_COLUMN = 11;

	/**
	 * Spam flag
	 */
	public static final int IS_SPAM_COLUMN = 12;

	/**
	 * Id of the chat 
	 */
	public static final int CHAT_ID_COLUMN = 13;

	/**
	 * Rejoin ID 
	 */
	public static final int CHAT_REJOIN_ID_COLUMN = 14;

	
	// Entry types
	
	// One to one chat
	public static final int TYPE_INCOMING_CHAT_MESSAGE = 0;
	public static final int TYPE_OUTGOING_CHAT_MESSAGE = 1;
	public static final int TYPE_CHAT_SYSTEM_MESSAGE = 2;
	
	// Group chat
	public static final int TYPE_INCOMING_GROUP_CHAT_MESSAGE = 3;
	public static final int TYPE_OUTGOING_GROUP_CHAT_MESSAGE = 4;
	public static final int TYPE_GROUP_CHAT_SYSTEM_MESSAGE = 5;
	
	// File transfer
	public static final int TYPE_INCOMING_FILE_TRANSFER = 6;
	public static final int TYPE_OUTGOING_FILE_TRANSFER = 7;
	
	// Rich call
	public static final int TYPE_INCOMING_RICH_CALL = 8;
	public static final int TYPE_OUTGOING_RICH_CALL = 9;	
	
	// SMS
	public static final int TYPE_INCOMING_SMS = 10; 
	public static final int TYPE_OUTGOING_SMS = 11;

	// Geoloc
	public static final int TYPE_INCOMING_GEOLOC = 12;
	public static final int TYPE_OUTGOING_GEOLOC = 13;
	public static final int TYPE_INCOMING_GROUP_GEOLOC = 14;
	public static final int TYPE_OUTGOING_GROUP_GEOLOC = 15;
	
	// Possible status values
	
	// Sessions
	public static final int STATUS_STARTED = 0;
	public static final int STATUS_TERMINATED = 1;
	public static final int STATUS_FAILED = 2;
	public static final int STATUS_IN_PROGRESS = 3;
    public static final int STATUS_CANCELED = 20;
	public static final int STATUS_TERMINATED_BY_USER = 21;
	public static final int STATUS_TERMINATED_BY_REMOTE = 22;
	
	// Messages
	public static final int STATUS_SENT = 4;
	public static final int STATUS_RECEIVED = 5;
	public static final int STATUS_MISSED = 6;

	// IMDN
	public static final int STATUS_DELIVERED = 7; // sender side
	public static final int STATUS_DISPLAYED = 8; // the IMDN "displayed" report has been received (sender side) or the user has read the received message (receiver side)  
	public static final int STATUS_ALL_DISPLAYED = 9; // sender side
	public static final int STATUS_REPORT_REQUESTED = 10; // receiver side : the sender has requested a "displayed" report when the message will be displayed

	// Possible data for chat system event
	public static final int EVENT_JOINED_CHAT = 12; // Contact has joined
	public static final int EVENT_LEFT_CHAT = 13; // Contact is departed
	public static final int EVENT_INVITED = 14; // Contact is invited
	public static final int EVENT_INITIATED = 15; // Contact is inviting
	public static final int EVENT_DISCONNECT_CHAT = 16; // Contact is booted
	public static final int EVENT_FAILED = 17; // Contact has declined the invitation or any other reason
	public static final int EVENT_BUSY = 18; // Contact is busy
	public static final int EVENT_DECLINED = 19; // Contact has declined the invitation
	public static final int EVENT_PENDING = 20; // Contact invitation is pending
	
	// Is spam
	public static final int MESSAGE_IS_NOT_SPAM = 0;
	public static final int MESSAGE_IS_SPAM = 1;
	
	/**
	 * Mode One to one chat
	 */
	public static final int MODE_ONE_TO_ONE_CHAT = 32;

	/**
	 * Mode group chat
	 */
	public static final int MODE_GROUP_CHAT = 33;
	
	/**
	 * Mode spam box
	 */
	public static final int MODE_SPAM_BOX = 34;

	/**
	 * Each mode below is valued according to the binary representation of a variable representing the selected mode.
	 * *****************************
	 * Bit representation of the selected mode variable value :
	 * sms/mms 			(SMS) 	= bit0
	 * File Transfer	(FT) 	= bit1
	 * Chat				(CHAT)	= bit2
	 * ContentSharing 	(RC) 	= bit3
	 * *****************************
	 * For exemple if selected modes are Chat and ContentSharing, the mode value will be 1100 => 12 which is MODE_RC_CHAT
	 */
	public static final int MODE_RC_CHAT_FT_SMS = 15;
	public static final int MODE_RC_CHAT_FT = 14;
	public static final int MODE_RC_CHAT_SMS = 13;
	public static final int MODE_RC_CHAT = 12;
	public static final int MODE_RC_FT_SMS = 11;
	public static final int MODE_RC_FT = 10;
	public static final int MODE_RC_SMS = 9;
	public static final int MODE_RC = 8;
	public static final int MODE_CHAT_FT_SMS = 7;
	public static final int MODE_CHAT_FT = 6;
	public static final int MODE_CHAT_SMS = 5;
	public static final int MODE_CHAT = 4;
	public static final int MODE_FT_SMS = 3;
	public static final int MODE_FT = 2;
	public static final int MODE_SMS = 1;
	public static final int MODE_NONE = 0;

    /**
     * Constructor
     * 
     * @param ctx Application context
     */
    public EventsLogApi(Context ctx) {
    	super(ctx);

    	RichCall.createInstance(ctx);
    	RichMessaging.createInstance(ctx);
    }

    /**
     * Get the events log content provider base uri
     * 
     * @param mode
     * @return uri
     */
    public Uri getEventLogContentProviderUri(int mode){
    	return ContentUris.withAppendedId(EventLogData.CONTENT_URI, mode); 
    }
    
    /**
     * Get one to one chat log
     * 
     * @return uri
     */
    public Uri getOneToOneChatLogContentProviderUri(){
    	return ContentUris.withAppendedId(EventLogData.CONTENT_URI, MODE_ONE_TO_ONE_CHAT);
    }
    
    
    /**
     * Get group chat log
     * 
     * @return uri
     */
    public Uri getGroupChatLogContentProviderUri(){
    	return ContentUris.withAppendedId(EventLogData.CONTENT_URI, MODE_GROUP_CHAT);
    }	
	
    /**
     * Get spam box log
     * 
     * @return uri
     */
    public Uri getSpamBoxLogContentProviderUri(){
    	return ContentUris.withAppendedId(EventLogData.CONTENT_URI, MODE_SPAM_BOX);
    }    

    /**
     * Clear the history of a given contact
     * 
     * @param contact
     */
    public void clearHistoryForContact(String contact){
    	RichMessaging.getInstance().clearHistory(PhoneUtils.formatNumberToInternational(contact));
    }
    
    /**
     * Delete a given log entry
     * 
     * @param id Item id
     */
    public void deleteLogEntry(long id){
    	RichMessaging.getInstance().deleteEntry(id);
    }

    /**
     * Delete a SMS entry
     * 
     * @param item id
     */
    public void deleteSmsEntry(long id){
    	ctx.getContentResolver().delete(ContentUris.withAppendedId(EventLogData.SMS_URI, id),null, null);
    }
    
    /**
     * Delete a MMS entry
     * 
     * @param item id
     */
    public void deleteMmsEntry(long id){
    	ctx.getContentResolver().delete(ContentUris.withAppendedId(EventLogData.MMS_URI, id),null, null);
    }

    /**
     * Delete a rich call entry
     * 
     * @param contact
     * @param date
     */
    public void deleteRichCallEntry(String contact, long date){
		RichCall.getInstance().removeCall(contact, date);
    }
    
    /**
     * Delete an IM entry
     * 
     * @param item id
     */
    public void deleteImEntry(long rowId){
    	RichMessaging.getInstance().deleteEntry(rowId);
    }
    
    /**
     * Delete the chat and file transfer log associated to a contact
     * 
     * @param contact
     */
    public void deleteMessagingLogForContact(String contact){
    	RichMessaging.getInstance().deleteContactHistory(contact);
    }

    /**
     * Delete a group chat conversation
     *
     * @param chatId Chat ID
     */
    public void deleteGroupChatConversation(String chatId){
        RichMessaging.getInstance().deleteGroupChatConversation(chatId);
    }
    

    /**
     * Delete an IM session
     * 
     * @param sessionId Session ID
     */
    public void deleteImSessionEntry(String sessionId){
    	RichMessaging.getInstance().deleteChatSession(sessionId);
    }

    /**
     * Get a cursor on the given chat session
     * 
     * @param sessionId
     * @return cursor
     */
    public Cursor getChatSessionCursor(String sessionId){
    	// Do not take the chat terminated messages
    	String chatTerminatedExcludedSelection = " AND NOT(("+RichMessagingData.KEY_TYPE + "=="+ TYPE_CHAT_SYSTEM_MESSAGE +") AND ("+RichMessagingData.KEY_STATUS+"=="+STATUS_TERMINATED +
    		" OR " + RichMessagingData.KEY_STATUS+"=="+STATUS_TERMINATED_BY_REMOTE + " OR " + RichMessagingData.KEY_STATUS + "== " + STATUS_TERMINATED_BY_USER + "))";
    	chatTerminatedExcludedSelection +=" AND NOT(("+RichMessagingData.KEY_TYPE + "=="+ TYPE_GROUP_CHAT_SYSTEM_MESSAGE +") AND ("+RichMessagingData.KEY_STATUS+"== "+STATUS_TERMINATED +
    		" OR " + RichMessagingData.KEY_STATUS + "== " + STATUS_TERMINATED_BY_REMOTE + " OR " + RichMessagingData.KEY_STATUS + "== " + STATUS_TERMINATED_BY_USER + "))";
    	return ctx.getContentResolver().query(RichMessagingData.CONTENT_URI, 
				null,
				RichMessagingData.KEY_CHAT_SESSION_ID + "='" + sessionId + "'"+chatTerminatedExcludedSelection, 
				null,
				RichMessagingData.KEY_TIMESTAMP + " ASC");
    }
    
    /**
     * Get a cursor on the given chat contact
     * 
     * @param contact
     * @return cursor
     */
    public Cursor getChatContactCursor(String contact){
    	// Do not take the chat terminated messages
    	String chatTerminatedExcludedSelection = " AND NOT(("+RichMessagingData.KEY_TYPE + "=="+ TYPE_CHAT_SYSTEM_MESSAGE +") AND ("+RichMessagingData.KEY_STATUS+"=="+STATUS_TERMINATED + " OR " + 
    	RichMessagingData.KEY_STATUS+"=="+STATUS_TERMINATED_BY_REMOTE + " OR " + RichMessagingData.KEY_STATUS + "==" + STATUS_TERMINATED_BY_USER + "))";
    	// Do not take the group chat entries concerning this contact
    	chatTerminatedExcludedSelection +=" AND NOT("+RichMessagingData.KEY_TYPE + "=="+ TYPE_GROUP_CHAT_SYSTEM_MESSAGE +")";
    	    	
		// take all concerning this contact
		return ctx.getContentResolver().query(RichMessagingData.CONTENT_URI, null,
				RichMessagingData.KEY_CONTACT + "='"
				+ PhoneUtils.formatNumberToInternational(contact) + "'"+chatTerminatedExcludedSelection, null,
				RichMessagingData.KEY_TIMESTAMP + " ASC");
    }

    /**
     * Get the last message from a given chat session
     * 
     * @param sessionId
     * @return Instant message
     */
    public InstantMessage getLastChatMessage(String sessionId) {
    	InstantMessage result = null;
    	Cursor cursor = ctx.getContentResolver().query(RichMessagingData.CONTENT_URI, 
    			new String[] {
    				RichMessagingData.KEY_MESSAGE_ID,
    				RichMessagingData.KEY_CONTACT,
    				RichMessagingData.KEY_DATA,
    				RichMessagingData.KEY_TIMESTAMP,
    				RichMessagingData.KEY_STATUS
    				},
    			RichMessagingData.KEY_CHAT_SESSION_ID + "='" + sessionId + "'", 
    			null, 
    			RichMessagingData.KEY_TIMESTAMP + " DESC");
    	while(cursor.moveToNext()) {
    		String msg = cursor.getString(2);
    		if ((msg != null) && (msg.length() > 0)) {
        		boolean imdnDisplayedRequested = (cursor.getInt(4) == EventsLogApi.STATUS_REPORT_REQUESTED);
        		result = new InstantMessage(cursor.getString(0), cursor.getString(1), msg, imdnDisplayedRequested, new Date(cursor.getLong(3)));
        		break;
    		}
    	}
    	cursor.close();
    	return result;
    }

    /**
     * Get the last incoming geoloc for a given contact
     * 
     * @param contact Contact
     * @return Geoloc info
     */
    public GeolocPush getLastGeoloc(String contact) {
		GeolocPush result = null;																	

		String sortOrder = RichMessagingData.KEY_TIMESTAMP + " DESC ";
		String where = RichMessagingData.KEY_CONTACT + "='" + PhoneUtils.extractNumberFromUri(contact) +
				"' AND " +	RichMessagingData.KEY_MIME_TYPE +" = '" + GeolocMessage.MIME_TYPE +
				"' AND (" +	RichMessagingData.KEY_TYPE +" = '" + EventsLogApi.TYPE_INCOMING_GEOLOC +
				"' OR " +	RichMessagingData.KEY_TYPE +" = '" + EventsLogApi.TYPE_INCOMING_GROUP_GEOLOC + "')";
		Cursor cursor = ctx.getContentResolver().query(RichMessagingData.CONTENT_URI,
    			new String[] {RichMessagingData.KEY_DATA},
				where,
    			null,
    			sortOrder);
	    	
    	if (cursor.moveToFirst()) {
			result = GeolocPush.formatStrToGeoloc(cursor.getString(0));																	
    	}
    	cursor.close();

    	return result;
    }
    
    /**
     * Get my last geoloc
     * 
     * @return Geoloc info
     */
    public GeolocPush getMyLastGeoloc() {
		GeolocPush result = null;																	

		String sortOrder = RichMessagingData.KEY_TIMESTAMP + " DESC ";
		String where = RichMessagingData.KEY_MIME_TYPE +" = '" + GeolocMessage.MIME_TYPE +
				"' AND (" +	RichMessagingData.KEY_TYPE +" = '" + EventsLogApi.TYPE_OUTGOING_GEOLOC +
				"' OR " +	RichMessagingData.KEY_TYPE +" = '" + EventsLogApi.TYPE_OUTGOING_GROUP_GEOLOC + "')";
		Cursor cursor = ctx.getContentResolver().query(RichMessagingData.CONTENT_URI,
    			new String[] {RichMessagingData.KEY_DATA},
				where,
    			null,
    			sortOrder);
	    	
    	if (cursor.moveToFirst()) {
			result = GeolocPush.formatStrToGeoloc(cursor.getString(0));																	
    	}
    	cursor.close();

    	return result;
    }
    
    /**
     * Get spam messages log (Spam Box)
     *
     * @return uri
     */
    public Uri getSpamMessagesProviderUri() {
        return getEventLogContentProviderUri(MODE_SPAM_BOX);
    }

    /**
     * Mark message as spam
     *
     * @param msgId
     * @param isSpam
     */
    public void markChatMessageAsSpam(String msgId, boolean isSpam){
    	RichMessaging.getInstance().markChatMessageAsSpam(msgId, isSpam);
    }

    /**
     * Mark message as read
     * 
     * @param msgId
     * @param isRead
     */
    public void markChatMessageAsRead(String msgId, boolean isRead){
    	RichMessaging.getInstance().markChatMessageAsRead(msgId, isRead);
    }
    
    /**
     * Get the number of unread messages for a given chat session
     * 
     * @param sessionId
     * @return number of unread messages in this chat session
     */
    public int getNumberOfUnreadChatMessages(String sessionId){
    	// Get incoming messages count
    	Cursor cursor = ctx.getContentResolver().query(RichMessagingData.CONTENT_URI, 
    			new String[]{RichMessagingData.KEY_ID},
    			RichMessagingData.KEY_CHAT_SESSION_ID + "='" + sessionId + "'" +
    					" AND ("+RichMessagingData.KEY_TYPE + " = " + EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE +
    					" OR "+RichMessagingData.KEY_TYPE + " = " + EventsLogApi.TYPE_INCOMING_GROUP_CHAT_MESSAGE + ")", 
    			null, 
    			null);
   		int messagesNumber = cursor.getCount();
    	cursor.close();
    	
    	// Get read messages count
    	cursor = ctx.getContentResolver().query(RichMessagingData.CONTENT_URI, 
    			new String[]{RichMessagingData.KEY_ID},
    			RichMessagingData.KEY_CHAT_SESSION_ID + "='" + sessionId + "'" +
    					" AND ("+RichMessagingData.KEY_STATUS +" = "+EventsLogApi.STATUS_DISPLAYED +
    					" OR "+RichMessagingData.KEY_STATUS + " = " + EventsLogApi.STATUS_ALL_DISPLAYED + ")" +
    					" AND ("+RichMessagingData.KEY_TYPE + " = " + EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE +
    					" OR "+RichMessagingData.KEY_TYPE + " = " + EventsLogApi.TYPE_INCOMING_GROUP_CHAT_MESSAGE + ")", 
    			null, 
    			null);
   		int readMessages = cursor.getCount();
    	cursor.close();
    	
    	// Result is the difference
    	return (messagesNumber - readMessages);
    }

    /**
     * Delete all spams
     */
    public void deleteAllSpams(){
    	RichMessaging.getInstance().deleteAllSpams();
    }

    /**
     * Delete spam message
     *
     * @param msgId Spam message Id
     */
    public void deleteSpamMessage(String msgId) {
        RichMessaging.getInstance().deleteSpamMessage(msgId);
    }

    // Changed by Deutsche Telekom
    /**
     * Clear the spam messages of a given contact
     *
     * @param contact
     */
    public void clearSpamMessagesForContact(String contact) {
        RichMessaging.getInstance().clearSpamMessages(PhoneUtils.formatNumberToInternational(contact));
    }

}
