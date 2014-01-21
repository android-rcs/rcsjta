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
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.ft.FileTransfer;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Rich messaging history for chats and file transfers.
 * 
 * @author Jean-Marc AUFFRET
 */
public class RichMessagingHistory {
	/**
	 * Current instance
	 */
	private static RichMessagingHistory instance = null;

	/**
	 * Content resolver
	 */
	private ContentResolver cr;
	
	/**
	 * Chat database URI
	 */
	private Uri chatDatabaseUri = ChatData.CONTENT_URI;

	/**
	 * Message database URI
	 */
	private Uri msgDatabaseUri = MessageData.CONTENT_URI;

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
			instance = new RichMessagingHistory(ctx);
		}
	}
	
	/**
	 * Returns instance
	 * 
	 * @return Instance
	 */
	public static RichMessagingHistory getInstance() {
		return instance;
	}
	
	/**
     * Constructor
     * 
     * @param ctx Application context
     */
	private RichMessagingHistory(Context ctx) {
		super();
		
        this.cr = ctx.getContentResolver();
        this.maxLogEntries = RcsSettings.getInstance().getMaxChatLogEntriesPerContact();
	}

	/*--------------------- Group chat methods -----------------------*/

	/**
	 * Add group chat session
	 * 
	 * @param chatId Chat ID
	 * @param subject Subject
	 * @param participants List of participants
	 * @param status Status
	 * @param direction Direction
	 */
	public void addGroupChat(String chatId, String subject, List<String> participants, int status, int direction) {
		if (logger.isActivated()){
			logger.debug("Add group chat entry: chatID=" + chatId);
		}
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_CHAT_ID, chatId);
		values.put(ChatData.KEY_STATUS, status);
		values.put(ChatData.KEY_SUBJECT, subject);
		values.put(ChatData.KEY_PARTICIPANTS, RichMessagingHistory.getParticipants(participants));
		values.put(ChatData.KEY_DIRECTION, direction);
		values.put(ChatData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
		cr.insert(chatDatabaseUri, values);
	}

	/**
	 * Get list of participants into a string
	 * 
	 * @param participants List of participants
	 * @return String (contacts are comma separated)
	 */
	private static String getParticipants(List<String> participants) {
		StringBuffer result = new StringBuffer();
		for(String contact : participants){
			if (contact != null) {
				result.append(PhoneUtils.extractNumberFromUri(contact)+";");
			}
		}
		return result.toString();
	}

	/**
	 * Get list of participants from a string
	 * 
	 * @param String  participants (contacts are comma separated)
	 * @return String[] contacts or null if 
	 */
	private static List<String> getParticipants(String participants) {
		ArrayList<String> result = new ArrayList<String>();
		if (participants!=null && participants.trim().length()>0) {
			String[] items = participants.split(";", 0);
			for (int i = 0; i < items.length; i++) {
				if (items[i] != null) {
					result.add(items[i]);
				}
			}
		}
		return result;
	}

	/**
	 * Update group chat status
	 * 
	 * @param chatId Chat ID
	 * @param status Status
	 */
	public void updateGroupChatStatus(String chatId, int status) {
		if (logger.isActivated()) {
			logger.debug("Update group chat status to " + status);
		}
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_STATUS, status);
		cr.update(chatDatabaseUri, 
				values, 
				ChatData.KEY_CHAT_ID + " = '" + chatId + "'", 
				null);
	}
	
	/**
	 * Update group chat rejoin ID
	 * 
	 * @param chatId Chat ID
	 * @param rejoingId Rejoin ID
	 * @param status Status
	 */
	public void updateGroupChatRejoinId(String chatId, String rejoingId) {
		if (logger.isActivated()) {
			logger.debug("Update group chat rejoin ID to " + rejoingId);
		}
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_REJOIN_ID, rejoingId);
		cr.update(chatDatabaseUri, 
				values, 
				ChatData.KEY_CHAT_ID + " = '" + chatId + "'", 
				null);
	}
	
	/**
	 * Get the group chat info
	 * 
	 * @param chatId Chat ID
	 * @result Group chat info
	 */
	public GroupChatInfo getGroupChatInfo(String chatId) {
		if (logger.isActivated()) {
			logger.debug("Get group chat info for " + chatId);
		}
    	GroupChatInfo result = null;
    	Cursor cursor = cr.query(chatDatabaseUri, 
    			new String[] {
    				ChatData.KEY_CHAT_ID,
    				ChatData.KEY_REJOIN_ID,
    				ChatData.KEY_PARTICIPANTS,
    				ChatData.KEY_SUBJECT
    			},
    			"(" + ChatData.KEY_CHAT_ID + "='" + chatId + "')", 
				null, 
    			ChatData.KEY_TIMESTAMP + " DESC");
    	
    	if (cursor.moveToFirst()) {
    		String participants = cursor.getString(2); 
        	List<String> list = RichMessagingHistory.getParticipants(participants);		
        	result = new GroupChatInfo(
    				cursor.getString(0),
    				cursor.getString(1),
    				chatId,
    				list,
    				cursor.getString(3));
    	}
    	cursor.close();
    	return result;
	}
	
	/**
	 * Get the group chat participants who have been connected to the chat
	 * 
	 * @param chatId Chat ID
	 * @result List of contacts
	 */
	public List<String> getGroupChatConnectedParticipants(String chatId) {
		if (logger.isActivated()) {
			logger.debug("Get connected participants for " + chatId);
		}
		List<String> result = new ArrayList<String>();
     	Cursor cursor = cr.query(msgDatabaseUri, 
    			new String[] {
    				MessageData.KEY_CONTACT
    			},
    			"(" + MessageData.KEY_CHAT_ID + "='" + chatId + "') AND (" + 
    				MessageData.KEY_TYPE + "=" + ChatLog.Message.Type.SYSTEM + ")",
    			null, 
    			MessageData.KEY_TIMESTAMP + " DESC");
    	while(cursor.moveToNext()) {
    		String participant = cursor.getString(0);
    		if ((participant != null) && (!result.contains(participant))) {
    			result.add(participant);
    		}
    	}
    	cursor.close();
    	return result;
	}

	/*--------------------- Chat messages methods -----------------------*/

	/**
	 * Add a spam message
	 * 
	 * @param msg Chat message
	 */
	public void addSpamMessage(InstantMessage msg) {
		addChatMessage(msg, ChatLog.Message.Type.SPAM, ChatLog.Message.Direction.INCOMING);
	}
	
	/**
	 * Add a chat message
	 * 
	 * @param msg Chat message
	 * @param direction Direction
	 */
	public void addChatMessage(InstantMessage msg, int direction) {
		addChatMessage(msg, ChatLog.Message.Type.CONTENT, direction);
	}
	
	/**
	 * Add a chat message
	 * 
	 * @param msg Chat message
	 * @param type Message type
	 * @param direction Direction
	 */
	private void addChatMessage(InstantMessage msg, int type, int direction) {
		String contact = PhoneUtils.extractNumberFromUri(msg.getRemote());
		if (logger.isActivated()){
			logger.debug("Add chat message: contact=" + contact + ", msg=" + msg.getMessageId());
		}

		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, contact);
		values.put(MessageData.KEY_MSG_ID, msg.getMessageId());
		values.put(MessageData.KEY_CONTACT, contact);
		values.put(MessageData.KEY_DIRECTION, direction);
		values.put(MessageData.KEY_TYPE, type);

		if (msg instanceof GeolocMessage) {
			GeolocPush geoloc = ((GeolocMessage)msg).getGeoloc();
			Geoloc geolocApi = new Geoloc(geoloc.getLabel(),
					geoloc.getLatitude(), geoloc.getLongitude(), geoloc.getAltitude(),
					geoloc.getExpiration(), geoloc.getAccuracy());
			String content = com.gsma.services.rcs.chat.GeolocMessage.geolocToString(geolocApi);		
			values.put(MessageData.KEY_CONTENT, content);
			values.put(MessageData.KEY_CONTENT_TYPE, com.gsma.services.rcs.chat.GeolocMessage.MIME_TYPE);
		} else {
			values.put(MessageData.KEY_CONTENT, msg.getTextMessage());
			values.put(MessageData.KEY_CONTENT_TYPE, com.gsma.services.rcs.chat.ChatMessage.MIME_TYPE);
		}
		
		if (direction == ChatLog.Message.Direction.INCOMING) {
			// Receive message
			values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
			values.put(MessageData.KEY_TIMESTAMP_SENT, 0);
			values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);		
			if (msg.isImdnDisplayedRequested()) {
				values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.UNREAD_REPORT);
			} else {
				values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.UNREAD);
			}
		} else {
			// Send message
			values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
			values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getDate().getTime());
			values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);		
			values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.SENT);
		}
		cr.insert(msgDatabaseUri, values);
	}
	
	/**
	 * Add a group chat message
	 * 
	 * @param chatId Chat ID
	 * @param msg Chat message
	 * @param direction Direction
	 */
	public void addGroupChatMessage(String chatId, InstantMessage msg, int direction) {
		if (logger.isActivated()){
			logger.debug("Add group chat message: chatID=" + chatId + ", msg=" + msg.getMessageId());
		}
		
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, chatId);
		values.put(MessageData.KEY_MSG_ID, msg.getMessageId());
		values.put(MessageData.KEY_CONTACT, PhoneUtils.extractNumberFromUri(msg.getRemote()));
		values.put(MessageData.KEY_DIRECTION, direction);
		values.put(MessageData.KEY_TYPE, ChatLog.Message.Type.CONTENT);
		
		if (msg instanceof GeolocMessage) {
			GeolocPush geoloc = ((GeolocMessage)msg).getGeoloc();
			Geoloc geolocApi = new Geoloc(geoloc.getLabel(),
					geoloc.getLatitude(), geoloc.getLongitude(), geoloc.getAltitude(),
					geoloc.getExpiration(), geoloc.getAccuracy());
			String content = com.gsma.services.rcs.chat.GeolocMessage.geolocToString(geolocApi);		
			values.put(MessageData.KEY_CONTENT, content);
			values.put(MessageData.KEY_CONTENT_TYPE, com.gsma.services.rcs.chat.GeolocMessage.MIME_TYPE);
		} else {
			values.put(MessageData.KEY_CONTENT, msg.getTextMessage());
			values.put(MessageData.KEY_CONTENT_TYPE, com.gsma.services.rcs.chat.ChatMessage.MIME_TYPE);
		}

		if (direction == ChatLog.Message.Direction.INCOMING) {
			// Receive message
			values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
			values.put(MessageData.KEY_TIMESTAMP_SENT, 0);
			values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);		
			if (msg.isImdnDisplayedRequested()) {
				values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.UNREAD_REPORT);
			} else {
				values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.UNREAD);
			}
		} else {
			// Send message
			values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
			values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getDate().getTime());
			values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);		
			values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.SENT);
		}
		cr.insert(msgDatabaseUri, values);
	}
	
	/**
	 * Add group chat system message
	 * 
	 * @param chatId Chat ID
	 * @param contact Contact
	 * @param status Status
	 */
	public void addGroupChatSystemMessage(String chatId, String contact, int status) {
		if (logger.isActivated()){
			logger.debug("Add group chat system message: chatID=" + chatId + ", contact=" + contact + ", status=" + status);
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, chatId);
		values.put(MessageData.KEY_CONTACT, contact);
		values.put(MessageData.KEY_TYPE, ChatLog.Message.Type.SYSTEM);
		values.put(MessageData.KEY_STATUS, status);
		values.put(MessageData.KEY_DIRECTION, ChatLog.Message.Direction.IRRELEVANT);
		values.put(ChatData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
		cr.insert(msgDatabaseUri, values);
	}

	/**
	 * Update chat message status
	 * 
	 * @param msgId Message ID
	 * @param status Message status
	 */
	public void updateChatMessageStatus(String msgId, int status) {
		if (logger.isActivated()) {
			logger.debug("Update chat message: msgID=" + msgId + ", status=" + status);
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_STATUS, status);
		if (status == ChatLog.Message.Status.Content.UNREAD) {
			// Delivered
			values.put(MessageData.KEY_TIMESTAMP_DELIVERED, Calendar.getInstance().getTimeInMillis());
		} else
		if (status == ChatLog.Message.Status.Content.READ) {
			// Displayed
			values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, Calendar.getInstance().getTimeInMillis());
		}
		cr.update(msgDatabaseUri, 
				values, 
				MessageData.KEY_MSG_ID + " = '" + msgId + "'", 
				null);
	}
	
	/**
	 * Update chat message delivery status
	 * 
	 * @param msgId Message ID
	 * @param status Delivery status
	 */
	public void updateChatMessageDeliveryStatus(String msgId, String status) {
		if (logger.isActivated()) {
			logger.debug("Update chat delivery status: msgID=" + msgId + ", status=" + status);
		}
    	if (status.equals(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
    		RichMessagingHistory.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.UNREAD);
    	} else
    	if (status.equals(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
    		RichMessagingHistory.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.READ);
    	} else 
    	if (status.equals(ImdnDocument.DELIVERY_STATUS_ERROR)) {
    		RichMessagingHistory.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
    	}
	}
    
    /*--------------------- File transfer methods ----------------------*/

	/**
	 * Add outgoing file transfer
	 * 
	 * @param contact Contact
	 * @param sessionId Session ID
	 * @param direction Direction
	 * @param content File content 
	 */
	public void addFileTransfer(String contact, String sessionId, int direction, MmContent content) {
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
		values.put(FileTransferData.KEY_DIRECTION, direction);
		values.put(FileTransferData.KEY_SIZE, 0);
		values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());
		
		long date = Calendar.getInstance().getTimeInMillis();
		if (direction == FileTransfer.Direction.INCOMING) {
			// Receive file
			values.put(FileTransferData.KEY_TIMESTAMP, date);
			values.put(FileTransferData.KEY_TIMESTAMP_SENT, 0);
			values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);		
			values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INVITED);			
		} else {
			// Send file
			values.put(FileTransferData.KEY_TIMESTAMP, date);
			values.put(FileTransferData.KEY_TIMESTAMP_SENT, date);
			values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);		
			values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INITIATED);
		}
		
		
		
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
		if (status == FileTransfer.State.DELIVERED) {
			// Delivered
			values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, Calendar.getInstance().getTimeInMillis());
		} else
		if (status == FileTransfer.State.DISPLAYED) {
			// Displayed
			values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, Calendar.getInstance().getTimeInMillis());
		}
		cr.update(ftDatabaseUri, 
				values, 
				FileTransferData.KEY_SESSION_ID + " = '" + sessionId + "'", 
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
				FileTransferData.KEY_SESSION_ID + " = '" + sessionId + "'", 
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
		values.put(FileTransferData.KEY_STATUS, FileTransfer.State.TRANSFERRED);
		cr.update(ftDatabaseUri, 
				values, 
				FileTransferData.KEY_SESSION_ID + " = '" + sessionId + "'", 
				null);
	}
	
    /**
     * Get file transfer ID from a received message
     *
     * @param msgId Message ID
     * @return Chat session ID of the file transfer
     */
    public String getFileTransferId(String msgId) {
        String result = null;
/* TODO       Cursor cursor = cr.query(msgDatabaseUri, 
                new String[] {
        		ChatData.KEY_CHAT_ID
                },
                "(" + ChatData.KEY_ID + "='" + msgId + "') AND (" + 
                		MessageData.KEY_TYPE + "=" + ChatLog.Message.Type.FILE_TRANSFER + ")",
                null, 
                FileTransferData.KEY_TIMESTAMP + " DESC");
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();*/
        return result;
    }
    
    /**
     * Update file transfer ChatId
     *
     * @param sessionId Session Id
     * @param chatId chat Id
     * @param msgId msgId of the corresponding chat
     */
    public void updateFileTransferChatId(String sessionId, String chatId, String msgId) {
 /* TODO       ContentValues values = new ContentValues();
        values.put(RichMessagingData.KEY_CHAT_ID, chatId);
        values.put(RichMessagingData.KEY_MESSAGE_ID , msgId);
        cr.update(databaseUri, 
                values, 
                RichMessagingData.KEY_CHAT_SESSION_ID + " = " + sessionId, 
                null);*/
    }    
}
