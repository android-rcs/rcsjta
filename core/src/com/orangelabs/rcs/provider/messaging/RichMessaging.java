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

package com.orangelabs.rcs.provider.messaging;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.gsma.joyn.chat.Chat;
import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.ft.FileTransfer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.event.User;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Rich messaging history for chats and file transfers.
 * 
 * @author Jean-Marc AUFFRET
 */
public class RichMessaging {
	/**
	 * Current instance
	 */
	private static RichMessaging instance = null;

	/**
	 * Content resolver
	 */
	private ContentResolver cr;
	
	/**
	 * Chat database URI
	 */
	private Uri chatDatabaseUri = ChatData.CONTENT_URI;

	/**
	 * File transfer database URI
	 */
	private Uri ftDatabaseUri = FileTransferData.CONTENT_URI;

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
			instance = new RichMessaging(ctx);
		}
	}
	
	/**
	 * Returns instance
	 * 
	 * @return Instance
	 */
	public static RichMessaging getInstance() {
		return instance;
	}
	
	/**
     * Constructor
     * 
     * @param ctx Application context
     */
	private RichMessaging(Context ctx) {
		super();
		
        this.cr = ctx.getContentResolver();
        this.maxLogEntries = RcsSettings.getInstance().getMaxChatLogEntriesPerContact();
	}
	
	/*--------------------- Chat methods -----------------------*/

	/**
	 * Get list of participants into a string
	 * 
	 * @param session Chat session
	 * @return String (contacts are comma separated)
	 */
	private String getParticipants(ChatSession session) {
		StringBuffer participants = new StringBuffer();
		if (session.isGroupChat()) {
			for(String contact : session.getParticipants().getList()){
				if (contact!=null) {
					participants.append(PhoneUtils.extractNumberFromUri(contact)+";");
				}
			}
		} else {
			participants.append(PhoneUtils.extractNumberFromUri(session.getRemoteContact()));
		}
		return participants.toString();
	}

	/**
	 * Get type corresponding to a chat system event
	 * 
	 * @param session Chat session
	 * @return Type
	 */
	private int getChatSystemEventType(ChatSession session) {
		if (session.isGroupChat()) {
			return ChatLog.Chat.Type.GROUP_CHAT;
		} else {
			return ChatLog.Chat.Type.SINGLE_CHAT;
		}
	}
	
	/**
	 * Add a new conference event
	 * 
	 * @param session Chat session
	 * @param contact Contact
	 * @param state Conference state
	 */
	public void addConferenceEvent(ChatSession session, String contact, String state) {
		int event = -1;
		if (state.equals(User.STATE_BOOTED)) {
			// Contact has lost the session and may rejoin the session after
			event = ChatLog.Message.Status.System.DISCONNECT;
		} else
		if (state.equals(User.STATE_DEPARTED)) {
			// Contact has left voluntary the session
			event = ChatLog.Message.Status.System.GONE;
		} else
		if (state.equals(User.STATE_DISCONNECTED)) {
			// Contact has left voluntary the session
			event = ChatLog.Message.Status.System.GONE;
		} else
		if (state.equals(User.STATE_CONNECTED)) {
			// Contact has joined the session
			event = ChatLog.Message.Status.System.JOINED;
		} else
		if (state.equals(User.STATE_BUSY)) {
			// Contact is busy
			event = ChatLog.Message.Status.System.BUSY;
		} else
		if (state.equals(User.STATE_PENDING)) {
			// Contact is busy
			event = ChatLog.Message.Status.System.PENDING;
		} else
		if (state.equals(User.STATE_DECLINED)) {
			// Contact has declined the invitation
			event = ChatLog.Message.Status.System.DECLINED;
		} else
		if (state.equals(User.STATE_FAILED)) {
			// Contact has declined the invitation or any SIP error related to the contact invitation 
			event = ChatLog.Message.Status.System.FAILED;
		}
		
		if (event != -1) {
			addEntry(ChatLog.Chat.Type.GROUP_CHAT,
					session.getSessionID(), session.getContributionID(),
					null, contact, null, null, null, 0, null, event);
		}
	}
	
	/**
	 * Add an incoming chat session
	 * 
	 * @param session Chat session
	 */
	public void addIncomingChatSession(ChatSession session){
		// Add session entry
		String sessionId = session.getSessionID();
		String chatId = session.getContributionID();
		String participants = getParticipants(session);
		String subject = session.getSubject();
		int type = getChatSystemEventType(session);
		addEntry(type, sessionId, chatId, null, participants, subject, null, null, 0, null, Chat.State.INVITED);

		// Add first message entry
		InstantMessage firstMsg = session.getFirstMessage();
		if (firstMsg != null) {
			addIncomingChatMessage(firstMsg, session);
		}
	}
		
	/**
	 * Add outgoing chat session
	 * 
	 * @param session Chat session 
	 */
	public void addOutgoingChatSession(ChatSession session){
		// Add session entry
		String sessionId = session.getSessionID();
		String chatId = session.getContributionID();
		String participants = getParticipants(session);
		String subject = session.getSubject();
		int type = getChatSystemEventType(session);
		addEntry(type, sessionId, chatId, null, participants, subject, null, null, 0, null, Chat.State.INITIATED);

		// Add first message entry
		InstantMessage firstMsg = session.getFirstMessage();
		if (firstMsg != null) {
			addOutgoingChatMessage(firstMsg, session);
		}
	}
		
	/**
	 * Add incoming chat message
	 * 
	 * @param msg Chat message
	 * @param session Chat session
	 */
	public void addIncomingChatMessage(InstantMessage msg, ChatSession session) {
/*		// Add message entry
		int type;
		if (session.isGroupChat()) {
			type = EventsLogApi.TYPE_INCOMING_GROUP_CHAT_MESSAGE;
		} else {
			type = EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE;
		}		
		int status = EventsLogApi.STATUS_RECEIVED;
		if (msg.isImdnDisplayedRequested()){
			status = EventsLogApi.STATUS_REPORT_REQUESTED;
		}
		addEntry(type, session.getSessionID(), session.getContributionID(), msg.getMessageId(),
				msg.getRemote(), msg.getTextMessage(), InstantMessage.MIME_TYPE, msg.getRemote(),
				msg.getTextMessage().getBytes().length, msg.getDate(), status);*/
	}
	
	/**
	 * Add outgoing chat message
	 * 
	 * @param msg Chat message
	 * @param session Chat session
	 */
	public void addOutgoingChatMessage(InstantMessage msg, ChatSession session){
/*		// Add session entry
		int type;
		if (session.isGroupChat()){
			type = EventsLogApi.TYPE_OUTGOING_GROUP_CHAT_MESSAGE;
		} else {
			type = EventsLogApi.TYPE_OUTGOING_CHAT_MESSAGE;
		}
		addEntry(type, session.getSessionID(), session.getContributionID(),
				msg.getMessageId(), msg.getRemote(), msg.getTextMessage(),
				InstantMessage.MIME_TYPE, msg.getRemote(),
				msg.getTextMessage().getBytes().length, msg.getDate(), EventsLogApi.STATUS_SENT);*/			
	}
	
	/**
	 * Add incoming geoloc message
	 * 
	 * @param geoloc Geoloc message
	 * @param session Chat session
	 */
	public void addIncomingGeoloc(GeolocMessage geoloc, ChatSession session) {
/*		// Add message entry
		int type;
		if (session.isGroupChat()) {
			type = EventsLogApi.TYPE_INCOMING_GROUP_GEOLOC;
		} else {
			type = EventsLogApi.TYPE_INCOMING_GEOLOC;
		}		
		int status = EventsLogApi.STATUS_RECEIVED;
		if (geoloc.isImdnDisplayedRequested()){
			status = EventsLogApi.STATUS_REPORT_REQUESTED;
		}
		String geolocData = GeolocPush.formatGeolocToStr(geoloc.getGeoloc());
		addEntry(type, session.getSessionID(), session.getContributionID(), geoloc.getMessageId(),
				geoloc.getRemote(), geolocData, GeolocMessage.MIME_TYPE, geoloc.getRemote(),
				geolocData.length(), geoloc.getDate(), status);*/
	}
	
	/**
	 * Add incoming geoloc message out of a session
	 * 
	 * @param geoloc Geoloc message
	 */
	public void addIncomingGeoloc(GeolocMessage geoloc) {
/*		// Add message entry
		int type = EventsLogApi.TYPE_INCOMING_GEOLOC;
		int status = EventsLogApi.STATUS_RECEIVED;
		String geolocData = GeolocPush.formatGeolocToStr(geoloc.getGeoloc());
		addEntry(type, null, null, geoloc.getMessageId(),
				geoloc.getRemote(), geolocData, GeolocMessage.MIME_TYPE, geoloc.getRemote(),
				geolocData.length(), geoloc.getDate(), status);*/
	}

	/**
	 * Add outgoing geoloc message
	 * 
	 * @param geoloc Geoloc message
	 * @param session Chat session
	 */
	public void addOutgoingGeoloc(GeolocMessage geoloc, ChatSession session) {
/*		// Add session entry
		int type;
		if (session.isGroupChat()){
			type = EventsLogApi.TYPE_OUTGOING_GROUP_GEOLOC;
		} else {
			type = EventsLogApi.TYPE_OUTGOING_GEOLOC;
		}
		String geolocData = GeolocPush.formatGeolocToStr(geoloc.getGeoloc());
		addEntry(type, session.getSessionID(), session.getContributionID(),
				geoloc.getMessageId(), geoloc.getRemote(), geolocData,
				GeolocMessage.MIME_TYPE, geoloc.getRemote(),
				geolocData.length(), geoloc.getDate(), EventsLogApi.STATUS_SENT);*/			
	}
	
	/**
	 * Add outgoing geoloc message
	 * 
	 * @param geoloc Geoloc message
	 */
	public void addOutgoingGeoloc(GeolocMessage geoloc) {
/*		// Add session entry
		int type = EventsLogApi.TYPE_OUTGOING_GEOLOC;
		String geolocData = GeolocPush.formatGeolocToStr(geoloc.getGeoloc());
		addEntry(type, null, null,
				geoloc.getMessageId(), geoloc.getRemote(), geolocData,
				GeolocMessage.MIME_TYPE, geoloc.getRemote(),
				geolocData.length(), geoloc.getDate(), EventsLogApi.STATUS_SENT);*/			
	}

	/**
	 * Set the delivery status of a chat message
	 * 
	 * @param msgId Message ID
	 * @param status Status
	 */
	public void setChatMessageDeliveryStatus(String msgId, String status) {
/*		if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
			setChatMessageDeliveryStatus(msgId, EventsLogApi.STATUS_DISPLAYED);
		} else
		if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
			setChatMessageDeliveryStatus(msgId, EventsLogApi.STATUS_DELIVERED);			
		} else
		if ((status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_ERROR)) ||
				(status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_FAILED)) ||
					(status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_FORBIDDEN))) {
			setChatMessageDeliveryStatus(msgId, EventsLogApi.STATUS_FAILED);
		}*/
	}

	/**
	 * A delivery report "displayed" is requested for a given chat message
	 * 
	 * @param msgId Message ID
	 */
	public void setChatMessageDeliveryRequested(String msgId) {
//		setChatMessageDeliveryStatus(msgId, EventsLogApi.STATUS_REPORT_REQUESTED);
	}
		
	/**
	 * Set the delivery status of a chat message
	 * 
	 * @param msgId Message ID
	 * @param status Status
	 */
	private void setChatMessageDeliveryStatus(String msgId, int status) {
/*		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_STATUS, status);
		cr.update(databaseUri, 
				values, 
				ChatData.KEY_MESSAGE_ID + " = \'" + msgId + "\' and "
			       + ChatData.KEY_STATUS + " !=  " + EventsLogApi.STATUS_DISPLAYED, 
				null);*/
	}
	
	/**
	 * Add chat session termination
	 * 
	 * @param session Chat session
	 */
	public void addChatSessionTermination(ChatSession session) {
/*		String sessionId = session.getSessionID();
		String chatId = session.getContributionID();
		String participants = getParticipants(session);
		int type = getChatSystemEventType(session);
		addEntry(type, sessionId, chatId, null, participants, null, null, null, 0, new Date(), EventsLogApi.STATUS_TERMINATED);*/
	}
	
	/**
	 * Add chat session termination by end user
	 * 
	 * @param session Chat session
	 */
	public void addChatSessionTerminationByUser(ChatSession session) {
/*		String sessionId = session.getSessionID();
		String chatId = session.getContributionID();
		String participants = getParticipants(session);
		int type = getChatSystemEventType(session);
		addEntry(type, sessionId, chatId, null, participants, null, null, null, 0, new Date(), EventsLogApi.STATUS_TERMINATED_BY_USER);*/
	}

	/**
	 * Add chat session termination by remote
	 * 
	 * @param session Chat session
	 */
	public void addChatSessionTerminationByRemote(ChatSession session) {
/*		String sessionId = session.getSessionID();
		String chatId = session.getContributionID();
		String participants = getParticipants(session);
		int type = getChatSystemEventType(session);
		addEntry(type, sessionId, chatId, null, participants, null, null, null, 0, new Date(), EventsLogApi.STATUS_TERMINATED_BY_REMOTE);*/
	}

	/**
	 * Add a chat session error
	 * 
	 * @param session Chat session
	 */
	public void addChatSessionError(ChatSession session) {
/*		String sessionId = session.getSessionID();
		String chatId = session.getContributionID();
		String participants = getParticipants(session);
		int type = getChatSystemEventType(session);
		addEntry(type, sessionId, chatId, null, participants, null, null, null, 0, new Date(), EventsLogApi.STATUS_FAILED);*/
	}
	
	/**
	 * Mark a chat session as started
	 * 
	 * @param session Chat session
	 */
	public void markChatSessionStarted(ChatSession session) {
/*		int type = getChatSystemEventType(session);
		String participants = getParticipants(session);
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_CHAT_REJOIN_ID, session.getImSessionIdentity());
		values.put(ChatData.KEY_CONTACT, participants);
		cr.update(databaseUri, 
				values, 
				"(" + ChatData.KEY_CHAT_SESSION_ID +" = \"" + session.getSessionID() +
				"\") AND (" + ChatData.KEY_TYPE + " =" + type + ")", 
				null);*/		
	}	
	
	/**
	 * Mark first message as failed
	 * 
	 * @param sessionId Session ID
	 */
	public void markFirstMessageFailed(String sessionId) {
/*		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_STATUS, EventsLogApi.STATUS_FAILED);
		cr.update(databaseUri, 
				values, 
				ChatData.KEY_CHAT_SESSION_ID +" = \""+sessionId+"\"" + " AND " +
				ChatData.KEY_TYPE + " = " + EventsLogApi.TYPE_OUTGOING_CHAT_MESSAGE, 
				null);*/
	}
	
	/**
	 * Mark a chat message as failed
	 * 
	 * @param msgId Message ID
	 */
	public void markChatMessageFailed(String msgId) {
/*		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_STATUS, EventsLogApi.STATUS_FAILED);
		cr.update(databaseUri, 
				values, 
				ChatData.KEY_MESSAGE_ID +" = \""+msgId+"\"", 
				null);*/
	}	
	
	/**
	 * Mark a chat message as read or not
	 * 
	 * @param msgId Message ID
	 * @param isRead Read flag
	 */
	public void markChatMessageAsRead(String msgId, boolean isRead) {
/*		ContentValues values = new ContentValues();
		if (isRead){
			values.put(ChatData.KEY_STATUS, EventsLogApi.STATUS_DISPLAYED);
		}else{
			values.put(ChatData.KEY_STATUS, EventsLogApi.STATUS_RECEIVED);
		}
		cr.update(databaseUri, 
				values, 
				ChatData.KEY_MESSAGE_ID +" = \""+msgId+"\"", 
				null);*/
	}
	
	/**
	 * Add a spam message
	 * 
	 * @param msg Chat message
	 */
	public void addSpamMessage(InstantMessage msg) {
/*		// TODO: 2 queries may be avoided
		String id = SessionIdGenerator.getNewId();
		addEntry(EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE, id, id, msg.getMessageId(), msg.getRemote(), msg.getTextMessage(), InstantMessage.MIME_TYPE, msg.getRemote(), msg.getTextMessage().getBytes().length, msg.getDate(), EventsLogApi.STATUS_RECEIVED);
		markChatMessageAsSpam(msg.getMessageId(), true);*/
	}
	
	/**
	 * Add incoming chat message
	 * 
	 * @param msg Chat message
	 * @param chatId Chat ID
	 */
	public void addIncomingChatMessage(InstantMessage msg, String chatId) {
/*		int status = EventsLogApi.STATUS_RECEIVED;
		if (msg.isImdnDisplayedRequested()){
			status = EventsLogApi.STATUS_REPORT_REQUESTED;
		}
		addEntry(EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE, SessionIdGenerator.getNewId(), chatId, msg.getMessageId(), msg.getRemote(), msg.getTextMessage(), InstantMessage.MIME_TYPE, msg.getRemote(), msg.getTextMessage().getBytes().length, msg.getDate(), status);*/
	}
	
	/**
	 * Mark a chat message as spam or not
	 * 
	 * @param msgId Message ID
	 * @param isSpam Spam flag
	 */
	public void markChatMessageAsSpam(String msgId, boolean isSpam) {
/*		ContentValues values = new ContentValues();
		if (isSpam){
			values.put(ChatData.KEY_IS_SPAM, EventsLogApi.MESSAGE_IS_SPAM);
		}else{
			values.put(ChatData.KEY_IS_SPAM, EventsLogApi.MESSAGE_IS_NOT_SPAM);
		}
		cr.update(databaseUri, 
				values, 
				ChatData.KEY_MESSAGE_ID +" = \""+msgId+"\"" +" AND " + ChatData.KEY_TYPE + " = " + EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE, 
				null);*/
	}
	
	/**
	 * Delete all spam messages
	 */
	public void deleteAllSpams(){
/*		Cursor c = cr.query(databaseUri,
				new String[]{ChatData.KEY_ID},
				ChatData.KEY_IS_SPAM + " = \"" + EventsLogApi.MESSAGE_IS_SPAM +"\"",
				null,
				null);
		while (c.moveToNext()){
			long rowId = c.getLong(0);
			deleteEntry(rowId);
		}
		c.close();*/
	}

    /**
     * Delete spam message
     *
     * @param Message ID
     */
    public void deleteSpamMessage(String msgId) {
/*        Cursor c = cr.query(databaseUri,
                new String[]{ChatData.KEY_ID},
                ChatData.KEY_MESSAGE_ID +" = \""+msgId+"\"" +" AND " + ChatData.KEY_IS_SPAM + " = \"" + EventsLogApi.MESSAGE_IS_SPAM +"\"",
                null,
                null);
        if (c != null) {
            if (c.moveToFirst()) {
                long rowId = c.getLong(0);
                if (logger.isActivated()) {
                    logger.debug("Deleting spam message { msgId: " + msgId + " }");
                }
                deleteEntry(rowId);
            }
            c.close();
        }*/
    }

    /**
     * Clear spam messages for a given contact
     *
     * @param contact Contact
     */
    public void clearSpamMessages(String contact) {
/*        int deletedRows = cr.delete(databaseUri, ChatData.KEY_CONTACT+"= \""+contact+"\"" + " AND " + ChatData.KEY_IS_SPAM + " = \"" + EventsLogApi.MESSAGE_IS_SPAM +"\"", null);
        if (logger.isActivated()) {
            logger.debug("Clear spam messages of contact " + contact + ": deleted rows =" + deletedRows);
        }*/
    }

	/**
	 * Add a new entry (chat event, chat message or file transfer)
	 * 
	 * @param type Type of entry
	 * @param sessionId Session ID of a chat session or a file transfer session
	 * @param chatId Chat ID of a chat session
	 * @param messageId Message ID of a chat message 
	 * @param contact Contact phone number
	 * @param data Content of the entry (an URI for FT or a simple text for IM)
	 * @param mimeType MIME type for a file transfer
	 * @param name Name of the transfered file
	 * @param size Size of the  transfered file
	 * @param status Status of the entry
	 * @return URI of the new entry
	 */
	private Uri addEntry(int type, String sessionId, String chatId, String messageId, String contact, String data, String mimeType, String name, long size, Date date, int status) {
/*		contact = PhoneUtils.extractNumberFromUri(contact);
		
		if (logger.isActivated()){
			logger.debug("Add new entry: type=" + type + ", sessionID=" + sessionId +
					", chatID=" + chatId + ", messageID=" + messageId + ", contact=" + contact +
					", MIME=" + mimeType + ", status=" + status);
		}
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_TYPE, type);
		values.put(ChatData.KEY_CHAT_SESSION_ID, sessionId);
		values.put(ChatData.KEY_CHAT_ID, chatId);
		values.put(ChatData.KEY_MESSAGE_ID, messageId);
		values.put(ChatData.KEY_CONTACT, contact);
		values.put(ChatData.KEY_MIME_TYPE, mimeType);
		values.put(ChatData.KEY_TOTAL_SIZE, size);
		values.put(ChatData.KEY_NAME, name);
		values.put(ChatData.KEY_DATA, data);
		values.put(ChatData.KEY_STATUS, status);
		values.put(ChatData.KEY_NUMBER_MESSAGES, recycler(contact)+1);
		values.put(ChatData.KEY_IS_SPAM, EventsLogApi.MESSAGE_IS_NOT_SPAM);
		if(date == null) {
			values.put(ChatData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
		} else {
			values.put(ChatData.KEY_TIMESTAMP, date.getTime());
		}
		return cr.insert(databaseUri, values);*/
		return null;
	}
		
	/**
	 * Delete history associated to a contact
	 * 
	 * @param contact Contact
	 */
	public void deleteContactHistory(String contact) {
/*		String excludeGroupChat = ChatData.KEY_TYPE + "<>" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + " AND " +
			ChatData.KEY_TYPE + "<>" + EventsLogApi.TYPE_INCOMING_GROUP_CHAT_MESSAGE + " AND " +
			ChatData.KEY_TYPE + "<>" + EventsLogApi.TYPE_OUTGOING_GROUP_CHAT_MESSAGE;
		
		
		// Delete entries
		int deletedRows = cr.delete(databaseUri, 
				ChatData.KEY_CONTACT + " = \'" + contact + "\' AND " + excludeGroupChat, 
				null);
		if(logger.isActivated()){
			logger.debug("DeleteSession: deleted rows : "+deletedRows);
		}*/	
	}

    /**
     * Delete a group chat conversation
     *
     * @param chatId chat ID
     */
    public void deleteGroupChatConversation(String chatId) {
        // Delete entry
        int deletedRows = cr.delete(chatDatabaseUri,
                ChatData.KEY_CHAT_ID + "=\"" + chatId + "\"",
                null);
        if (logger.isActivated()) {
            logger.debug("Delete group chat conversation: " + deletedRows + " rows deleted");
        }
    }

	/**
	 * Delete a chat session. If file transfer is enclosed in the session, it will also be deleted.
	 * 
	 * @param sessionId Session ID
	 */
	public void deleteChatSession(String sessionId) {
/*		// Count entries to be deleted
		Cursor count = cr.query(databaseUri, 
				null, 
				ChatData.KEY_CHAT_SESSION_ID + " = " + sessionId, 
				null, 
				null);
		int toBeDeletedRows = count.getCount();
		
		String contact = null;
		
		boolean isGroupChat = false;
		if (count.moveToFirst()){
			contact = count.getString(count.getColumnIndex(ChatData.KEY_CONTACT));
			int type = count.getInt(count.getColumnIndex(ChatData.KEY_TYPE));
			if (type>=EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE && type <=EventsLogApi.TYPE_OUTGOING_GROUP_CHAT_MESSAGE){
				isGroupChat = true;
			}
		}
		if (logger.isActivated()){
			logger.debug("DeleteSession: rows to be deleted : "+toBeDeletedRows);
		}	
		count.close();
		if (toBeDeletedRows == 0){
			return;
		}
		
		if (!isGroupChat){
			// Manage recycling
			Cursor c  = cr.query(databaseUri, new String[]{
					ChatData.KEY_TIMESTAMP,
					ChatData.KEY_NUMBER_MESSAGES, 
					ChatData.KEY_CHAT_SESSION_ID}, 
					ChatData.KEY_CONTACT+" = \'"+contact+"\'", 
					null, 
					ChatData.KEY_TIMESTAMP + " DESC");
			if(c.moveToFirst()){
				long maxDate = c.getLong(0);
				int numberForLast = c.getInt(1);
				String lastSessionId = c.getString(2);
				ContentValues values = new ContentValues();
				values.put(ChatData.KEY_NUMBER_MESSAGES, numberForLast-toBeDeletedRows);

				if(sessionId.equals(lastSessionId)){
					// Find the last message from another session for the same contact
					if(logger.isActivated()){
						logger.debug("DeleteSession : the deleted session is the last one... looking for the previous one for this contact");
					}
					while(c.moveToNext()){
						if(!sessionId.equals(c.getString(2))){
							maxDate = c.getLong(0);
							if(logger.isActivated()){
								logger.debug("DeleteSession : find the previous session with date "+maxDate);
							}
							break;
						}
					}
				}
				if(logger.isActivated()){
					logger.debug("DeleteSession : updating the row of date "+maxDate);
				}
				// If no more session exists after deleting this one for this contact,
				// the update is useless because it will be made on the session to be deleted.
				int updatedRows = cr.update(databaseUri, 
						values, 
						ChatData.KEY_TIMESTAMP+ " = "+maxDate +" AND "+ChatData.KEY_CONTACT+" = \'"+contact+"\'", 
						null);
				if(logger.isActivated()){
					logger.debug("DeleteSession : recycling updated rows (should be 1) : "+updatedRows);
				}
			}
			c.close();
		}
		
		// Delete entry
		int deletedRows = cr.delete(databaseUri, 
				ChatData.KEY_CHAT_SESSION_ID + " = " + sessionId, 
				null);
		if(logger.isActivated()){
			logger.debug("DeleteSession: deleted rows : "+deletedRows);
		}*/	
	}
	
	/**
	 * Clear the history for a given contact
	 * 
	 * @param contact Contact
	 */
	public void clearHistory(String contact) {
		int deletedRows = cr.delete(chatDatabaseUri, ChatData.KEY_CONTACT+"='"+contact+"'", null);
		if (logger.isActivated()) {
			logger.debug("Clear history of contact " + contact + ": deleted rows =" + deletedRows);
		}
	}
	
	/**
	 * Delete an entry (chat message or file transfer) from its row id
	 *  
	 * @param rowId Row ID
	 */
	public void deleteEntry(long rowId) {
		Cursor count = cr.query(Uri.withAppendedPath(chatDatabaseUri, ""+rowId),
				null,
				null,
				null,
				null);
		if(count.getCount()==0) {
			count.close();
			return;
		}
		count.moveToFirst();
		String contactNumber = count.getString(count.getColumnIndexOrThrow((ChatData.KEY_CONTACT)));

		// Manage recycling
		Cursor c  = cr.query(chatDatabaseUri, new String[]{
				ChatData.KEY_TIMESTAMP,
				ChatData.KEY_NUMBER_MESSAGES, 
				ChatData.KEY_CHAT_SESSION_ID, 
				ChatData.KEY_DATA}, 
				ChatData.KEY_CONTACT + " = \'"+contactNumber + "\'", 
				null, 
				ChatData.KEY_TIMESTAMP + " DESC");
		
		// Get the first last entry for this contact
		if(c.moveToFirst()){
			long maxDate = c.getLong(0);
			int numberForLast = c.getInt(1);
			String lastSessionId = c.getString(2);
			
			// We are going to delete one message
			ContentValues values = new ContentValues();
			values.put(ChatData.KEY_NUMBER_MESSAGES, numberForLast-1);
			
			// Check if this message has the same sessionID, timestamp and content as the one to be deleted
			String sessionId = count.getString(count.getColumnIndexOrThrow(ChatData.KEY_CHAT_SESSION_ID));
			long date = count.getLong(count.getColumnIndexOrThrow(ChatData.KEY_TIMESTAMP));
			String message = "" + count.getString(count.getColumnIndexOrThrow(ChatData.KEY_DATA));
			if(sessionId.equals(lastSessionId) && (date == maxDate) && message.equals("" + c.getString(3))){
				/* It's the lastest message for this contact, 
				 * find the previous message for the same contact */
				if(logger.isActivated()){
					logger.debug("DeleteSession : the deleted message is the last one... looking for the previous one for this contact");
				}
				// Find the date of the previous message
				if(c.moveToNext()){
					maxDate = c.getLong(0);
					if (logger.isActivated()) {
						logger.debug("DeleteSession : find the previous message with date "+ maxDate);
					}
				}
			}
			if(logger.isActivated()){
				logger.debug("DeleteSession : updating the row of date "+maxDate);
			}
			/* Update the first message or the previous one with the new number of message for this contact. 
			 * 
			 * TODO :
			 * If the first message is the message to be deleted and no more messages are available for the same contact, 
			 * then the update is useless because it will be made on the message to be deleted.
			 */
			int updatedRows = cr.update(chatDatabaseUri, values, 
					ChatData.KEY_TIMESTAMP+ " = "+maxDate
					+" AND "+ChatData.KEY_CONTACT+" = \'"
					+contactNumber
					+"\'", 
					null);
			if(logger.isActivated()){
				logger.debug("DeleteSession : recycling updated rows (should be 1) : "+updatedRows);
			}
		}
		count.close();
		c.close();
		
		// Delete entry
		int deletedRows = cr.delete(Uri.withAppendedPath(
				chatDatabaseUri, ""+rowId), 
				null, 
				null);
		if(logger.isActivated()){
			logger.debug("DeleteSession: deleted rows : "+deletedRows);
		}	
	}
	
	/**
	 * Check if a session is terminated
	 * 
	 * @param sessionId Session ID
	 * @return Boolean
	 */
	public boolean isSessionTerminated(String sessionId) {
/*		Cursor cursor = cr.query(databaseUri, 
				new String[]{ChatData.KEY_STATUS}, 
				ChatData.KEY_CHAT_SESSION_ID + " = " + sessionId, 
				null, 
				ChatData.KEY_TIMESTAMP + " DESC");
		if(cursor.moveToFirst()){
			int status = cursor.getInt(0);
			if ((status==EventsLogApi.STATUS_TERMINATED) || (status==EventsLogApi.STATUS_TERMINATED_BY_REMOTE) || (status==EventsLogApi.STATUS_TERMINATED_BY_USER)) {
				cursor.close();
				return true;
			}
		}
		cursor.close();*/
		return false;
	}
	
	/**
	 * Get all outgoing messages still marked undisplayed for a given contact
	 * 
	 * @param contact Contact
	 * @return list of ids of the undisplayed messages
	 */
	public List<String> getAllOutgoingUndisplayedMessages(String contact){
		List<String> msgIds = new ArrayList<String>();
/*		Cursor cursor = cr.query(databaseUri, 
				new String[]{ChatData.KEY_MESSAGE_ID}, 
				ChatData.KEY_CONTACT + "=?" + " AND " + ChatData.KEY_TYPE + "=?" + " AND (" + ChatData.KEY_STATUS + "=?" + " OR " + ChatData.KEY_STATUS + "=?)", 
				new String[]{contact, String.valueOf(EventsLogApi.TYPE_OUTGOING_CHAT_MESSAGE), String.valueOf(EventsLogApi.STATUS_SENT), String.valueOf(EventsLogApi.STATUS_DELIVERED)}, 
				null);
		while (cursor.moveToNext()){
			msgIds.add(cursor.getString(0));
		}
		cursor.close();*/
		return msgIds;
	}

	/**
	 * Get the group chat ID from its session ID
	 * 
	 * @param sessionId Session ID
	 * @result Chat ID or null
	 */
	public String getGroupChatId(String sessionId) {
		String result = null;
/*    	Cursor cursor = cr.query(databaseUri, 
    			new String[] {
    				ChatData.KEY_CHAT_ID
    			},
    			"(" + ChatData.KEY_CHAT_SESSION_ID + "='" + sessionId + "') AND (" + 
    				ChatData.KEY_TYPE + "=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + ") AND (" +
    				ChatData.KEY_CHAT_ID + " NOT NULL)", 
    			null, 
    			ChatData.KEY_TIMESTAMP + " DESC");
    	if (cursor.moveToFirst()) {
    		result = cursor.getString(0);
    	}
    	cursor.close();*/
    	return result;
	}	

	/**
	 * Get the group chat rejoin ID
	 * 
	 * @param chatId Chat ID
	 * @result Rejoin ID or null
	 */
	public String getGroupChatRejoinId(String chatId) {
		String result = null;
/*    	Cursor cursor = cr.query(databaseUri, 
    			new String[] {
    				ChatData.KEY_CHAT_REJOIN_ID
    			},
    			"(" + ChatData.KEY_CHAT_ID + "='" + chatId + "') AND (" + 
    				ChatData.KEY_TYPE + "=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + ") AND (" +
    				ChatData.KEY_CHAT_REJOIN_ID + " NOT NULL)", 
    			null, 
    			ChatData.KEY_TIMESTAMP + " DESC");
    	if (cursor.moveToFirst()) {
    		result = cursor.getString(0);
    	}
    	cursor.close();*/
    	return result;
	}	

	/**
	 * Get the group chat info
	 * 
	 * @param chatId Chat ID
	 * @result Group chat info
	 */
	public GroupChatInfo getGroupChatInfo(String chatId) {
		GroupChatInfo result = null;
/*    	Cursor cursor = cr.query(databaseUri, 
    			new String[] {
    				ChatData.KEY_CHAT_SESSION_ID,
    				ChatData.KEY_CHAT_REJOIN_ID,
    				ChatData.KEY_CONTACT,
    				ChatData.KEY_DATA
    			},
    			"(" + ChatData.KEY_CHAT_ID + "='" + chatId + "') AND (" + 
    				ChatData.KEY_TYPE + "=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + ") AND ((" +
    				ChatData.KEY_STATUS + "=" + EventsLogApi.EVENT_INITIATED + ") OR (" +
    				ChatData.KEY_STATUS + "=" + EventsLogApi.EVENT_INVITED + "))", 
    				null, 
    			ChatData.KEY_TIMESTAMP + " DESC");
    	
    	if (cursor.moveToFirst()) {
    		String participants = cursor.getString(2); 
        	List<String> list = new ArrayList<String>();
        	if (participants != null) {
        		String[] contacts = participants.split(";");
        		for(int i=0; i < contacts.length; i++) {
        			list.add(contacts[i]);
        		}
        	}    		
        	result = new GroupChatInfo(
    				cursor.getString(0),
    				cursor.getString(1),
    				chatId,
    				list,
    				cursor.getString(3));
    	}
    	cursor.close();*/
    	return result;
	}

	/**
	 * Get group chat status
	 * 
	 * @param chatId Chat ID
	 * @return Status
	 */
	public int getGroupChatStatus(String chatId) {
		int result = -1;
/*    	Cursor cursor = cr.query(databaseUri, 
    			new String[] {
    				ChatData.KEY_STATUS
    			},
    			"(" + ChatData.KEY_CHAT_ID + "='" + chatId + "') AND (" + 
    				ChatData.KEY_TYPE + "=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + ")", 
    			null, 
    			ChatData.KEY_TIMESTAMP + " DESC");
    	if (cursor.moveToFirst()) {
    		result = cursor.getInt(0);
    	}
    	cursor.close();*/
    	return result;
	}	

	/**
	 * Get the group chat participants who have been connected to the chat
	 * 
	 * @param chatId Chat ID
	 * @result List of contacts
	 */
	public List<String> getGroupChatConnectedParticipants(String chatId) {
    	List<String> result = new ArrayList<String>();
/*    	Cursor cursor = cr.query(databaseUri, 
    			new String[] {
    				ChatData.KEY_CONTACT
    			},
    			"(" + ChatData.KEY_CHAT_ID + "='" + chatId + "') AND (" + 
    				ChatData.KEY_TYPE + "=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + ") AND (" +
    				ChatData.KEY_STATUS + "=" + EventsLogApi.EVENT_JOINED_CHAT + ")", 
    			null, 
    			ChatData.KEY_TIMESTAMP + " DESC");
    	while(cursor.moveToNext()) {
    		String participant = cursor.getString(0);
    		if (!result.contains(participant)) {
    			result.add(participant);
    		}
    	}
    	cursor.close();*/
    	return result;
	}
	
	/**
	 * Get the group chat subject
	 * 
	 * @param chatId Chat ID
	 * @result Subject or null
	 */
	public String getGroupChatSubject(String chatId) {
		String result = null;
/*    	Cursor cursor = cr.query(databaseUri, 
    			new String[] {
    				ChatData.KEY_DATA
    			},
    			"(" + ChatData.KEY_CHAT_ID + "='" + chatId + "') AND (" + 
    				ChatData.KEY_TYPE + "=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + ") AND (" +
                    ChatData.KEY_STATUS + "=" + EventsLogApi.EVENT_INITIATED + " OR " +
                    ChatData.KEY_STATUS + "=" + EventsLogApi.EVENT_INVITED + ")",
    			null, 
    			ChatData.KEY_TIMESTAMP + " DESC");
    	if (cursor.moveToFirst()) {
    		result = cursor.getString(0);
    	}
    	cursor.close();*/
    	return result;
	}
	
	/*--------------------- File transfer methods ----------------------*/

	/**
	 * Add outgoing file transfer
	 * 
	 * @param contact Contact
	 * @param sessionId Session ID
	 * @param direction Direction
	 * @param content File content 
	 * @param status Status
	 */
	public void addFileTransfer(String contact, String sessionId, int direction, MmContent content, int status) {
		contact = PhoneUtils.extractNumberFromUri(contact);
		if (logger.isActivated()){
			logger.debug("Add file transfer entry: sessionID=" + sessionId +
					", contact=" + contact +
					", filename=" + content.getName() +
					", size=" + content.getSize() +
					", MIME=" + content.getEncoding());
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_SESSION_ID, sessionId);
		values.put(FileTransferData.KEY_CONTACT, contact);
		values.put(FileTransferData.KEY_NAME, content.getUrl());
		values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
		values.put(FileTransferData.KEY_STATUS, status);
		values.put(FileTransferData.KEY_DIRECTION, direction);
		values.put(FileTransferData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
		values.put(FileTransferData.KEY_SIZE, 0);
		values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());
		cr.insert(ftDatabaseUri, values);
	}

	/**
	 * Update file transfer status
	 * 
	 * @param sessionId Session ID
	 * @param status New status
	 */
	public void updateFileTransferStatus(String sessionId, int status) {
		if (logger.isActivated()) {
			logger.debug("Update file transfer status to " + status);
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_STATUS, status);
		cr.update(ftDatabaseUri, 
				values, 
				FileTransferData.KEY_SESSION_ID + " = " + sessionId, 
				null);
	}
	
	/**
	 * Update file transfer download progress
	 * 
	 * @param sessionId Session ID
	 * @param size Downloaded size
	 * @param totalSize Total size to download 
	 */
	public void updateFileTransferProgress(String sessionId, long size, long totalSize) {
		if (logger.isActivated()) {
			logger.debug("Update file transfer progress");
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_SIZE, size);
		values.put(FileTransferData.KEY_TOTAL_SIZE, totalSize);
		values.put(FileTransferData.KEY_STATUS, FileTransfer.State.STARTED);
		cr.update(ftDatabaseUri, 
				values, 
				FileTransferData.KEY_SESSION_ID + " = " + sessionId, 
				null);
	}

	/**
	 * Update file transfer URL
	 * 
	 * @param sessionId Session ID
	 * @param url File URL
	 */
	public void updateFileTransferUrl(String sessionId, String url) {
		if (logger.isActivated()) {
			logger.debug("Update file transfer URL to " + url);
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_NAME, url);
		values.put(FileTransferData.KEY_STATUS, FileTransfer.State.TRANSFERED);
		cr.update(ftDatabaseUri, 
				values, 
				FileTransferData.KEY_SESSION_ID + " = " + sessionId, 
				null);
	}
	
	/**
	 * Delete a file transfer session
	 * 
	 * @param sessionId Session ID
	 */
	public void deleteFileTransferSession(String sessionId, String contact) {
		if (logger.isActivated()) {
			logger.debug("Delete a file transfer session " + sessionId);
		}
		Cursor count = cr.query(ftDatabaseUri, null, FileTransferData.KEY_SESSION_ID + " = " + sessionId, null, null);
		count.getCount();
		count.close();
	}
}
