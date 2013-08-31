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

import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.ft.FileTransfer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.orangelabs.rcs.core.content.MmContent;
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
		values.put(ChatData.KEY_PARTICIPANTS, RichMessaging.getParticipants(participants));
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
        	List<String> list = RichMessaging.getParticipants(participants);		
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
		// TODO
	}
	
	/**
	 * Add a chat message
	 * 
	 * @param msg Chat message
	 * @param direction Direction
	 */
	public void addChatMessage(InstantMessage msg, int direction) {
		String contact = PhoneUtils.extractNumberFromUri(msg.getRemote());
		if (logger.isActivated()){
			logger.debug("Add chat message: contact=" + contact + ", msg=" + msg.getMessageId());
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, contact);
		values.put(MessageData.KEY_MSG_ID, msg.getMessageId());
		values.put(MessageData.KEY_TYPE, ChatLog.Message.Type.CONTENT);
		values.put(MessageData.KEY_CONTACT, contact);
		values.put(MessageData.KEY_CONTENT, msg.getTextMessage());
		values.put(MessageData.KEY_CONTENT_TYPE, InstantMessage.MIME_TYPE);
		values.put(MessageData.KEY_DIRECTION, direction);
		values.put(ChatData.KEY_TIMESTAMP, msg.getDate().getTime());
		
		if (direction == ChatLog.Message.Direction.INCOMING) {
			if (msg.isImdnDisplayedRequested()) {
				values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.UNREAD_REPORT);
			} else {
				values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.UNREAD);
			}
		} else {
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
		values.put(MessageData.KEY_TYPE, ChatLog.Message.Type.CONTENT);
		values.put(MessageData.KEY_CONTACT, PhoneUtils.extractNumberFromUri(msg.getRemote()));
		values.put(MessageData.KEY_CONTENT, msg.getTextMessage());
		values.put(MessageData.KEY_CONTENT_TYPE, InstantMessage.MIME_TYPE);
		values.put(MessageData.KEY_DIRECTION, direction);
		values.put(ChatData.KEY_TIMESTAMP, msg.getDate().getTime());
		
		if (direction == ChatLog.Message.Direction.INCOMING) {
			if (msg.isImdnDisplayedRequested()) {
				values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.UNREAD_REPORT);
			} else {
				values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.UNREAD);
			}
		} else {
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
    		RichMessaging.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.UNREAD);
    	} else
    	if (status.equals(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
    		RichMessaging.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.READ);
    	} else 
    	if (status.equals(ImdnDocument.DELIVERY_STATUS_ERROR)) {
    		RichMessaging.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
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
}
