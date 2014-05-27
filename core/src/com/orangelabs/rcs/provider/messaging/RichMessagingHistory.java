/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.orangelabs.rcs.provider.messaging;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Pair;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.ft.FileTransfer;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.chat.FileTransferMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.event.User;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Rich messaging history for chats and file transfers.
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 *
 */
public class RichMessagingHistory {

	private static final String SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT = new StringBuilder(
			GroupChatDeliveryInfoData.KEY_MSG_ID).append("=? AND ").append(GroupChatDeliveryInfoData.KEY_CONTACT)
			.append("=?").toString();

	private static final String SELECTION_CONTACTS_NOT_RECEIVED_MESSAGE = new StringBuilder(
			GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS).append("=")
			.append(ChatLog.GroupChatDeliveryInfo.DeliveryStatus.NOT_DELIVERED).append(" OR (")
			.append(GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS).append("=")
			.append(ChatLog.GroupChatDeliveryInfo.DeliveryStatus.FAILED).append(" AND ")
			.append(GroupChatDeliveryInfoData.KEY_REASON_CODE).append(" IN (")
			.append(ChatLog.GroupChatDeliveryInfo.ReasonCode.DELIVERY_ERROR).append(",")
			.append(ChatLog.GroupChatDeliveryInfo.ReasonCode.DELIVERY_AND_DISPLAY_ERROR)
			.append("))").toString();

	private static final String SELECTION_FILE_BY_FT_ID = new StringBuilder(
			FileTransferData.KEY_FT_ID).append("=?").toString();

	private static final String SELECTION_MSG_BY_MSG_ID = new StringBuilder(MessageData.KEY_MSG_ID)
			.append("=?").toString();

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
	 * Delivery info database URI
	 */
	private Uri GroupChatDeliveryInfoDatabaseUri = GroupChatDeliveryInfoData.CONTENT_URI;

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(RichMessagingHistory.class.getSimpleName());
	
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
	}

	/*--------------------- Group chat methods -----------------------*/

	/**
	 * Convert a set of ParticipantInfo into a string
	 * 
	 * @param participants
	 *            the participant information
	 * @return the string with comma separated values of key pairs formatted as follows: "key=value"
	 */
	private static String writeParticipantInfo(Set<ParticipantInfo> participants) {
		if (participants == null || participants.size() == 0) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		int size = participants.size();
		for (ParticipantInfo participant : participants) {
			// set key
			sb.append(participant.getContact());
			sb.append('=');
			// set value
			sb.append(participant.getStatus());
			if (--size != 0) {
				// Not last item : add separator
				sb.append(',');
			}
		}
		return sb.toString();
	}
	
	/**
	 * Add group chat session
	 * 
	 * @param chatId Chat ID
	 * @param subject Subject
	 * @param participants List of participants
	 * @param status Status
	 * @param direction Direction
	 */
	public void addGroupChat(String chatId, String subject, Set<ParticipantInfo> participants, int status, int direction) {
		if (logger.isActivated()) {
			logger.debug("addGroupChat (chatID=" + chatId + ") (subject=" + subject + ") (status=" + status + ") (dir=" + direction
					+ ")");
		}
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_CHAT_ID, chatId);
		values.put(ChatData.KEY_STATUS, status);
		values.put(ChatData.KEY_SUBJECT, subject);
		values.put(ChatData.KEY_PARTICIPANTS, writeParticipantInfo(participants));
		values.put(ChatData.KEY_DIRECTION, direction);
		values.put(ChatData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
		cr.insert(chatDatabaseUri, values);
	}

	/**
	 * Update group chat status
	 * 
	 * @param chatId Chat ID
	 * @param status Status
	 */
	public void updateGroupChatStatus(String chatId, int status) {
		if (logger.isActivated()) {
			logger.debug("updateGroupChatStatus (chatId=" + chatId + ") (status=" + status + ")");
		}
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_STATUS, status);
		cr.update(chatDatabaseUri, 
				values, 
				ChatData.KEY_CHAT_ID + " = '" + chatId + "'", 
				null);
	}
	
	/**
	 * Update group chat set of participants
	 * 
	 * @param chatId
	 *            Chat ID
	 * @param participants
	 *            The set of participants
	 */
	public void updateGroupChatParticipant(String chatId, Set<ParticipantInfo> participants) {
		if (logger.isActivated()) {
			logger.debug("updateGroupChatParticipant (chatId=" + chatId + ") (participants=" + participants + ")");
		}
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_PARTICIPANTS, writeParticipantInfo(participants));
		cr.update(chatDatabaseUri, values, ChatData.KEY_CHAT_ID + " = '" + chatId + "'", null);
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
		Cursor cursor = null;

		// @formatter:off
		String[] projection = new String[] { ChatData.KEY_CHAT_ID, ChatData.KEY_REJOIN_ID, ChatData.KEY_PARTICIPANTS,
				ChatData.KEY_SUBJECT };
		// @formatter:on
		String selection = ChatData.KEY_CHAT_ID + "= ?";
		String[] selArgs = new String[] { chatId };
		try {
			cursor = cr.query(chatDatabaseUri, projection, selection, selArgs, ChatData.KEY_TIMESTAMP + " DESC");
			if (cursor.moveToFirst()) {
				// Decode list of participants
				Set<ParticipantInfo> participants = ChatLog.GroupChat.getParticipantInfo(cursor.getString(2));
				result = new GroupChatInfo(cursor.getString(0), cursor.getString(1), chatId, participants, cursor.getString(3));
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}
	
	/**
	 * Get the group chat participants
	 * 
	 * @param chatId Chat ID
	 * @result List of contacts
	 */
	public Set<ParticipantInfo> getGroupChatConnectedParticipants(String chatId) {
		if (logger.isActivated()) {
			logger.debug("Get connected participants for " + chatId);
		}
		Set<ParticipantInfo> result = new HashSet<ParticipantInfo>();
		String[] projection = new String[] { ChatData.KEY_PARTICIPANTS };
		String selection = ChatData.KEY_CHAT_ID + "= ?";
		String[] selArgs = new String[] { chatId };
		Cursor cursor = null;
		try {
			cursor = cr.query(chatDatabaseUri, projection, selection, selArgs, ChatData.KEY_TIMESTAMP + " DESC");
			if (cursor.moveToFirst()) {
				// Decode list of participants
				Set<ParticipantInfo> participants = ChatLog.GroupChat.getParticipantInfo(cursor.getString(0));
				if (participants != null) {
					for (ParticipantInfo participantInfo : participants) {
						// Only consider participants who have not declined or left GC
						switch (participantInfo.getStatus()) {
						case ParticipantInfo.Status.DEPARTED:
						case ParticipantInfo.Status.DECLINED:
							break;
						default:
							result.add(participantInfo);
							break;
						}
					}
				}
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null)
				cursor.close();
		}
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
		if (logger.isActivated()) {
			logger.debug("Add chat message: contact=" + contact + ", msg=" + msg.getMessageId() + ", dir=" + direction);
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, contact);
		values.put(MessageData.KEY_MSG_ID, msg.getMessageId());
		values.put(MessageData.KEY_CONTACT, contact);
		values.put(MessageData.KEY_DIRECTION, direction);
		values.put(MessageData.KEY_TYPE, type);
		values.put(MessageData.KEY_READ_STATUS, ChatLog.Message.ReadStatus.UNREAD);

		if (msg instanceof GeolocMessage) {
			values.put(MessageData.KEY_CONTENT_TYPE, com.gsma.services.rcs.chat.GeolocMessage.MIME_TYPE);
			GeolocPush geoloc = ((GeolocMessage) msg).getGeoloc();
			Geoloc geolocData = new Geoloc(geoloc.getLabel(), geoloc.getLatitude(), geoloc.getLongitude(), geoloc.getExpiration(),
					geoloc.getAccuracy());
			values.put(MessageData.KEY_CONTENT, geolocToString(geolocData));
		} else if (msg instanceof FileTransferMessage) {
			values.put(MessageData.KEY_CONTENT_TYPE, FileTransferMessage.MIME_TYPE);
			values.put(MessageData.KEY_CONTENT, ((FileTransferMessage) msg).getFileInfo());
		} else {
			values.put(MessageData.KEY_CONTENT_TYPE, com.gsma.services.rcs.chat.ChatMessage.MIME_TYPE);
			values.put(MessageData.KEY_CONTENT, msg.getTextMessage());
		}
		
		if (direction == ChatLog.Message.Direction.INCOMING) {
			// Receive message
			values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
			values.put(MessageData.KEY_TIMESTAMP_SENT, 0);
			values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);		
			if (msg.isImdnDisplayedRequested()) {
				values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.DISPLAY_REPORT_REQUESTED);
			} else {
				values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.RECEIVED);
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
     * Format geoloc object to string
     * 
     * @param geoloc Geoloc object
     * @return String
     */
	private static String geolocToString(Geoloc geoloc) {
		String label = geoloc.getLabel();
		if (label == null) {
			label = "";
		}
		return label + "," + geoloc.getLatitude() + "," + geoloc.getLongitude() + "," + geoloc.getExpiration() + ","
				+ geoloc.getAccuracy();
	}
	
	/**
	 * Add a group chat message
	 * 
	 * @param chatId Chat ID
	 * @param msg Chat message
	 * @param direction Direction
	 */
	public void addGroupChatMessage(String chatId, InstantMessage msg, int direction) {
		String msgId = msg.getMessageId();
		if (logger.isActivated()) {
			logger.debug("Add group chat message: chatID=" + chatId + ", msg=" + msgId + ", dir=" + direction);
		}
		
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, chatId);
		values.put(MessageData.KEY_MSG_ID, msgId);
		values.put(MessageData.KEY_CONTACT, PhoneUtils.extractNumberFromUri(msg.getRemote()));
		values.put(MessageData.KEY_DIRECTION, direction);
		values.put(MessageData.KEY_TYPE, ChatLog.Message.Type.CONTENT);
		values.put(MessageData.KEY_READ_STATUS, ChatLog.Message.ReadStatus.UNREAD);
		
		if (msg instanceof GeolocMessage) {
			values.put(MessageData.KEY_CONTENT_TYPE, com.gsma.services.rcs.chat.GeolocMessage.MIME_TYPE);
			GeolocPush geoloc = ((GeolocMessage)msg).getGeoloc();
			Geoloc geolocData = new Geoloc(geoloc.getLabel(),
					geoloc.getLatitude(), geoloc.getLongitude(),
					geoloc.getExpiration(), geoloc.getAccuracy());
			values.put(MessageData.KEY_CONTENT, geolocToString(geolocData));
		} else if (msg instanceof FileTransferMessage) {
			values.put(MessageData.KEY_CONTENT_TYPE, FileTransferMessage.MIME_TYPE);
			values.put(MessageData.KEY_CONTENT, ((FileTransferMessage) msg).getFileInfo()); 
		} else {
			values.put(MessageData.KEY_CONTENT_TYPE, com.gsma.services.rcs.chat.ChatMessage.MIME_TYPE);
			values.put(MessageData.KEY_CONTENT, msg.getTextMessage());
		}

		if (direction == ChatLog.Message.Direction.INCOMING) {
			// Receive message
			values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
			values.put(MessageData.KEY_TIMESTAMP_SENT, 0);
			values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);		
			if (msg.isImdnDisplayedRequested()) {
				values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.DISPLAY_REPORT_REQUESTED);
			} else {
				values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.RECEIVED);
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

		if (direction == ChatLog.Message.Direction.OUTGOING) {
			try {
				Set<ParticipantInfo> participants = getGroupChatConnectedParticipants(chatId);
				for (ParticipantInfo participant : participants) {
					addGroupChatDeliveryInfoEntry(chatId, msgId, participant.getContact());
				}
			} catch (Exception e) {
				cr.delete(msgDatabaseUri, MessageData.KEY_MSG_ID + "='" + msgId + "'", null);
				cr.delete(Uri.withAppendedPath(GroupChatDeliveryInfoData.CONTENT_MSG_URI, msgId),
						null, null);
				/* TODO: Throw exception */
				if (logger.isActivated()) {
					logger.warn("Group chat message with msgId '" + msgId
							+ "' could not be added to database!");
				}
			}
		}
	}
	
	/**
	 * Add group chat system message
	 * 
	 * @param chatId Chat ID
	 * @param contact Contact
	 * @param status Status
	 */
	public void addGroupChatSystemMessage(String chatId, String contact, int status) {
		if (logger.isActivated()) {
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
	 * Update chat message read status
	 *
	 * @param msgId Message ID
	 * @param status Message status
	 */
	public void markMessageAsRead(String msgId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Marking chat message as read: msgID=").append(msgId)
					.toString());
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_READ_STATUS, ChatLog.Message.ReadStatus.READ);
		values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, Calendar.getInstance().getTimeInMillis());

		if (cr.update(msgDatabaseUri, values, SELECTION_MSG_BY_MSG_ID, new String[] {
			msgId
		}) < 1) {
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("There was no message with msgId '" + msgId + "' to mark as read.");
			}
		}
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
		if (status == ChatLog.Message.Status.Content.DELIVERED) {
			// Delivered
			values.put(MessageData.KEY_TIMESTAMP_DELIVERED, Calendar.getInstance()
					.getTimeInMillis());
		}

		if (cr.update(msgDatabaseUri, values, SELECTION_MSG_BY_MSG_ID, new String[] {
			msgId
		}) < 1) {
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("There was no message with msgId '" + msgId + "' to update status for.");
			}
		}
	}

	/**
	 * Update chat message delivery status
	 *
	 * @param msgId Message ID
	 * @param status Delivery status
	 */
	public void updateIncomingChatMessageDeliveryStatus(String msgId, String status) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Update chat delivery status: msgID=").append(msgId)
					.append(", status=").append(status).toString());
		}
    	if (status.equals(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
    		RichMessagingHistory.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.RECEIVED);
    	}
	}

	/**
	 * Update chat message delivery status
	 * 
	 * @param msgId Message ID
	 * @param status Delivery status
	 */
	public void updateOutgoingChatMessageDeliveryStatus(String msgId, String status) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Update chat delivery status: msgID=").append(msgId)
					.append(", status=").append(status).toString());
		}
		RichMessagingHistory richMessagingHistory = RichMessagingHistory.getInstance();
		if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
			richMessagingHistory.updateChatMessageStatus(msgId,
					ChatLog.Message.Status.Content.DELIVERED);
		} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
			richMessagingHistory
					.markMessageAsRead(msgId);
		} else if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)) {
			richMessagingHistory.updateChatMessageStatus(msgId,
					ChatLog.Message.Status.Content.FAILED);
		} else if (ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)) {
			richMessagingHistory.updateChatMessageStatus(msgId,
					ChatLog.Message.Status.Content.FAILED);
		} else if (ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
			richMessagingHistory.updateChatMessageStatus(msgId,
					ChatLog.Message.Status.Content.FAILED);
		}
	}

	/**
	 * Update chat message delivery status in cases of failure
	 *
	 * @param msgId Message ID
	 * @param status Delivery status
	 */
	public void updateChatMessageDeliveryStatus(String msgId, String status) {
		if (logger.isActivated()) {
			logger.debug("Update chat delivery status: msgID=" + msgId + ", status=" + status);
		}
		RichMessagingHistory richMessagingHistory = RichMessagingHistory.getInstance();
		if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)) {
			richMessagingHistory.updateChatMessageStatus(msgId,
					ChatLog.Message.Status.Content.FAILED);
		} else if (ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)) {
			richMessagingHistory.updateChatMessageStatus(msgId,
					ChatLog.Message.Status.Content.FAILED);
		} else if (ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
			richMessagingHistory.updateChatMessageStatus(msgId,
					ChatLog.Message.Status.Content.FAILED);
		}
	}

    /**
     * Check if it's a new message
     * 
     * @param chatId chat ID
     * @param msgId message ID
     * @return true if new message
     */
	public boolean isNewMessage(String chatId, String msgId) {
		Cursor cursor = null;
		try {
			cursor = cr.query(msgDatabaseUri, new String[] { MessageData.KEY_MSG_ID }, "(" + MessageData.KEY_CHAT_ID + " = '"
					+ chatId + "') AND (" + MessageData.KEY_MSG_ID + " = '" + msgId + "')", null, null);
			return cursor.getCount() == 0;
		} catch (Exception e) {
			return false;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}
	
	/*--------------------- File transfer methods ----------------------*/

	/**
	 * Add outgoing file transfer
	 * 
	 * @param contact Contact
	 * @param fileTransferId File Transfer ID
	 * @param direction Direction
	 * @param content File content 
	 * @param thumbnail Thumbnail content
	 */
	public void addFileTransfer(String contact, String fileTransferId, int direction, MmContent content, MmContent thumbnail) {
		contact = PhoneUtils.extractNumberFromUri(contact);
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add file transfer entry: fileTransferId=").append(fileTransferId)
					.append(", contact=").append(contact).append(", filename=")
					.append(content.getName()).append(", size=").append(content.getSize())
					.append(", MIME=").append(content.getEncoding()).toString());
 		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_FT_ID, fileTransferId);
		values.put(FileTransferData.KEY_CHAT_ID, contact);
		values.put(FileTransferData.KEY_CONTACT, contact);
		values.put(FileTransferData.KEY_NAME, content.getUrl());
		values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
		values.put(FileTransferData.KEY_DIRECTION, direction);
		values.put(FileTransferData.KEY_SIZE, 0);
		values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());
		if (thumbnail != null) {
			values.put(FileTransferData.KEY_FILEICON, Uri.fromFile(new File(thumbnail.getUrl())).toString());
		}
		
		long date = Calendar.getInstance().getTimeInMillis();
		values.put(FileTransferData.KEY_READ_STATUS, FileTransfer.ReadStatus.UNREAD);
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
	 * Add an outgoing File Transfer supported by Group Chat
	 * 
	 * @param chatSessionId
	 *            the identity of the group chat
	 * @param fileTransferId
	 *            the identity of the file transfer
	 * @param content
	 *            the File content
	 * @param thumbnail
	 *            The thumbnail content
	 */
	public void addOutgoingGroupFileTransfer(String chatId, String fileTransferId, MmContent content, MmContent thumbnail) {
		if (logger.isActivated()) {
			logger.debug("addOutgoingGroupFileTransfer: fileTransferId=" + fileTransferId + ", chatId=" + chatId + " filename=" + content.getName()
					+ ", size=" + content.getSize() + ", MIME=" + content.getEncoding());
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_FT_ID, fileTransferId);
		values.put(FileTransferData.KEY_CHAT_ID, chatId);
		values.put(FileTransferData.KEY_NAME, content.getUrl());
		values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
		values.put(FileTransferData.KEY_DIRECTION, FileTransfer.Direction.OUTGOING);
		values.put(FileTransferData.KEY_SIZE, 0);
		values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());
		long date = Calendar.getInstance().getTimeInMillis();
		values.put(MessageData.KEY_READ_STATUS, ChatLog.Message.ReadStatus.UNREAD);
		// Send file
		values.put(FileTransferData.KEY_TIMESTAMP, date);
		values.put(FileTransferData.KEY_TIMESTAMP_SENT, date);
		values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
		values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INITIATED);
		if (thumbnail != null) {
			values.put(FileTransferData.KEY_FILEICON, Uri.fromFile(new File(thumbnail.getUrl())).toString());
		}
		cr.insert(ftDatabaseUri, values);

		try {
			Set<ParticipantInfo> participants = getGroupChatConnectedParticipants(chatId);
			for (ParticipantInfo participant : participants) {
				addGroupChatDeliveryInfoEntry(chatId, fileTransferId, participant.getContact());
			}
		} catch (Exception e) {
			cr.delete(ftDatabaseUri, FileTransferData.KEY_FT_ID + "='" + fileTransferId + "'", null);
			cr.delete(
					Uri.withAppendedPath(GroupChatDeliveryInfoData.CONTENT_MSG_URI, fileTransferId),
					null, null);
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("Group file transfer with fileTransferId '" + fileTransferId
						+ "' could not be added to database!");
			}
		}
	}

	/**
	 * Add incoming group file transfer
	 *
	 * @param contact Contact
	 * @param fileTransferId File transfer ID
	 * @param chatId Chat ID
	 * @param content File content
	 * @param thumbnail Thumbnail content
	 */
	public void addIncomingGroupFileTransfer(String chatId, String contact, String fileTransferId, MmContent content, MmContent thumbnail) {
		contact = PhoneUtils.extractNumberFromUri(contact);
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add incoming file transfer entry: fileTransferId=")
					.append(fileTransferId).append(", chatId=").append(chatId).append(", contact=")
					.append(contact).append(", filename=").append(content.getName())
					.append(", size=").append(content.getSize()).append(", MIME=")
					.append(content.getEncoding()).toString());
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_FT_ID, fileTransferId);
		values.put(FileTransferData.KEY_CHAT_ID, chatId);
		values.put(FileTransferData.KEY_CONTACT, contact);
		values.put(FileTransferData.KEY_NAME, content.getUrl());
		values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
		values.put(FileTransferData.KEY_DIRECTION, FileTransfer.Direction.INCOMING);
		values.put(FileTransferData.KEY_SIZE, 0);
		values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());
		values.put(FileTransferData.KEY_READ_STATUS, FileTransfer.ReadStatus.UNREAD);

		long date = Calendar.getInstance().getTimeInMillis();
		values.put(FileTransferData.KEY_TIMESTAMP, date);
		values.put(FileTransferData.KEY_TIMESTAMP_SENT, 0);
		values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
		values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INVITED);
		if (thumbnail != null) {
			values.put(FileTransferData.KEY_FILEICON, Uri.fromFile(new File(thumbnail.getUrl())).toString());
		}

		cr.insert(ftDatabaseUri, values);
	}

	/**
	 * Update file transfer status
	 * 
	 * @param fileTransferId File transfer ID
	 * @param status New status
	 * @param contact the contact
	 */
	public void updateFileTransferStatus(String fileTransferId, int status) {
		if (logger.isActivated()) {
			logger.debug("updateFileTransferStatus (status=" + status + ") (fileTransferId=" + fileTransferId + ")");
		}
		// TODO FUSION to check
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_STATUS, status);
		if (status == FileTransfer.State.DELIVERED) {
			// Delivered
			values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, Calendar.getInstance().getTimeInMillis());
		} else if (status == FileTransfer.State.DISPLAYED) {
			// Displayed
			values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, Calendar.getInstance().getTimeInMillis());
		}
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] {
			fileTransferId
		});
	}

	/**
	 * Update file transfer status
	 *
	 * @param fileTransferId File transfer ID
	 * @param status New status
	 * @param contact the contact
	 */
	public void markFileTransferAsRead(String fileTransferId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("markFileTransferAsRead  (fileTransferId=")
					.append(fileTransferId).append(")").toString());
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_READ_STATUS, FileTransfer.ReadStatus.READ);
		if (cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] {
			fileTransferId
		}) < 1) {
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("There was no file with fileTransferId '" + fileTransferId
						+ "' to mark as read.");
			}
		}
	}
	
	/**
	 * Update file transfer download progress
	 * 
	 * @param fileTransferId File transfer ID
	 * @param size Downloaded size
	 * @param totalSize Total size to download 
	 */
	public void updateFileTransferProgress(String fileTransferId, long size, long totalSize) {
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_SIZE, size);
		values.put(FileTransferData.KEY_TOTAL_SIZE, totalSize);
		values.put(FileTransferData.KEY_STATUS, FileTransfer.State.STARTED);
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] {
			fileTransferId
		});
	}

	/**
	 * Update file transfer URL
	 * 
	 * @param fileTransferId File transfer ID
	 * @param url File URL
	 */
	public void updateFileTransferUrl(String fileTransferId, String url) {
		if (logger.isActivated()) {
			logger.debug("updateFileTransferUrl (fileTransferId=" + fileTransferId + ") (url=" + url + ")");
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_NAME, url);
		values.put(FileTransferData.KEY_STATUS, FileTransfer.State.TRANSFERRED);
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] {
			fileTransferId
		});
	}
	
	/**
	 * Tells if the MessageID corresponds to that of a file transfer
	 *
	 * @param fileTransferId File Transfer Id
	 * @return boolean If there is File Transfer corresponding to msgId
	 */
	public boolean isFileTransfer(String fileTransferId) {
		if (logger.isActivated()) {
			logger.debug("isFileTransfer (fileTransferId=" + fileTransferId + ")");
		}
		Cursor cursor = null;
		try {
			cursor = cr.query(ftDatabaseUri, new String[] {
				FileTransferData.KEY_ID
			}, SELECTION_FILE_BY_FT_ID, new String[] {
				fileTransferId
			}, null);

			return cursor.moveToFirst();
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Exception occured while determing if it is file transfer", e);
			}
			return false;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Update file transfer ChatId
	 * 
	 * @param fileTransferId File transfer ID
	 * @param chatId chat Id
	 */
	public void updateFileTransferChatId(String fileTransferId, String chatId) {
		// TODO FUSION
		if (logger.isActivated()) {
			logger.debug("updateFileTransferChatId (chatId=" + chatId + ") (fileTransferId=" + fileTransferId + ")");
		}
		ContentValues values = new ContentValues();
		values.put(FileTransferData.KEY_CHAT_ID, chatId);
		cr.update(ftDatabaseUri, values, SELECTION_FILE_BY_FT_ID, new String[] {
			fileTransferId
		});
	}

    /**
     * Is next group chat Invitation rejected
     * 
     * @param chatId Chat ID
     * @return true if next GC invitation should be rejected
     */
	public boolean isGroupChatNextInviteRejected(String chatId) {
		String selection = ChatData.KEY_CHAT_ID + " = ? AND " //
				+ ChatData.KEY_STATUS + " = ? AND "//
				+ ChatData.KEY_REJECT_GC + " = 1";
		String[] selectionArgs = { chatId, "" + GroupChat.State.CLOSED_BY_USER };
		Cursor cursor = null;
		try {
			cursor = cr.query(chatDatabaseUri, null, selection, selectionArgs, ChatData.KEY_TIMESTAMP + " DESC");
			if (cursor.getCount() != 0) {
				return true;
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return false;
	}
	
	/**
	 * Accept next Group Chat invitation
	 * @param chatId
	 */
	public void acceptGroupChatNextInvitation(String chatId) {
		if (logger.isActivated()) {
			logger.debug("acceptGroupChatNextInvitation (chatId=" + chatId + ")");
		}
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_REJECT_GC, "0");
		// @formatter:off
		String selection = ChatData.KEY_CHAT_ID + " = ? AND " 
							+ ChatData.KEY_STATUS + " = ? AND "
							+ ChatData.KEY_REJECT_GC + " = 1";
		// @formatter:on
		String[] selectionArgs = { chatId, "" + GroupChat.State.CLOSED_BY_USER };
		cr.update(chatDatabaseUri, values, selection, selectionArgs);
		if (logger.isActivated()) {
			logger.debug("acceptGroupChatNextInvitation (chatID=" + chatId + ")");
		}
	}
	
    /**
     * A delivery report "displayed" is requested for a given chat message
     * 
     * @param msgId Message ID
     */
	public void setChatMessageDeliveryRequested(String msgId) {
		if (logger.isActivated()) {
			logger.debug("Set chat delivery requested: msgID=" + msgId);
		}
		// TODO FUSION will be changed with CR013 read/displayed/delivered ?
		updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.DISPLAY_REPORT_REQUESTED);
	}
	
	/**
	 * Get state event log value from conference state name
	 * 
	 * @param state
	 *            Conference state
	 * @return event log value
	 * 
	 */
	private int getEventLogValue(String state) {
		int event = ParticipantInfo.Status.UNKNOWN;
		if (state.equals(User.STATE_BOOTED)) {
			// Contact has lost the session and may rejoin the session after
			event = ParticipantInfo.Status.BOOTED;
		} else if (state.equals(User.STATE_DEPARTED)) {
			// Contact has left voluntary the session
			event = ParticipantInfo.Status.DEPARTED;
		} else if (state.equals(User.STATE_DISCONNECTED)) {
			// Contact has left voluntary the session
			event = ParticipantInfo.Status.DISCONNECTED;
		} else if (state.equals(User.STATE_CONNECTED)) {
			// Contact has joined the session
			event = ParticipantInfo.Status.CONNECTED;
		} else if (state.equals(User.STATE_BUSY)) {
			// Contact is busy
			event = ParticipantInfo.Status.BUSY;
		} else if (state.equals(User.STATE_PENDING)) {
			// Contact is busy
			event = ParticipantInfo.Status.PENDING;
		} else if (state.equals(User.STATE_DECLINED)) {
			// Contact has declined the invitation
			event = ParticipantInfo.Status.DECLINED;
		} else if (state.equals(User.STATE_FAILED)) {
			// Any SIP error related to the contact invitation
			event = ParticipantInfo.Status.FAILED;
		}
		return event;
	}
	
	/**
	 * Has the last known state changed for a participant
	 * 
	 * @param chatId
	 * @param participant
	 * @param lastState
	 * @return true if the state has changed for the participant since the last time
	 */
	public boolean hasLastKnownStateForParticipantChanged(String chatId, String participant, String lastState) {
		int lastKnownState = -1;
		String selection = MessageData.KEY_CHAT_ID + " = ? AND " //
				+ MessageData.KEY_TYPE + " = ? AND "//
				+ MessageData.KEY_CONTACT + " = ? ";
		String[] selectionArgs = { chatId, "" + ChatLog.Message.Type.SYSTEM, participant };
		Cursor cursor = null;
		try {
			cursor = cr.query(msgDatabaseUri, new String[] { MessageData.KEY_STATUS }, selection, selectionArgs,
					MessageData.KEY_TIMESTAMP + " DESC");
			if (cursor.moveToNext()) {
				lastKnownState = cursor.getInt(0);
			}
		} catch (Exception e) {
			return true;
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return (lastKnownState == -1 // There was no known state yet
		|| getEventLogValue(lastState) != lastKnownState); // Or the state has changed
	}

	/**
	 * Delete all entries in Chat, Message and FileTransfer Logs
	 */
	public void deleteAllEntries() {
		cr.delete(ChatData.CONTENT_URI, null, null);
		cr.delete(MessageData.CONTENT_URI, null, null);
		cr.delete(FileTransferData.CONTENT_URI, null, null);
		cr.delete(GroupChatDeliveryInfoData.CONTENT_URI, null, null);
	}

	/**
	 * Add a new entry (chat message or file transfer)
	 *
	 * @param chatId Chat ID of a chat session
	 * @param msgId Message ID of a chat message
	 * @param contact Contact phone number
	 * @param deliverySupported If delivery report is supported
	 * @param displaySupported If display report is supported
	 */
	public Uri addGroupChatDeliveryInfoEntry(String chatId, String msgId, String contact) {

		String contactNumber = PhoneUtils.extractNumberFromUri(contact);
		if (logger.isActivated()) {
			logger.debug("Add new entry: chatID=" + chatId + ", messageID=" + msgId
					+ ", contactNumber=" + contactNumber);
		}
		ContentValues values = new ContentValues();
		values.put(GroupChatDeliveryInfoData.KEY_CHAT_ID, chatId);
		values.put(GroupChatDeliveryInfoData.KEY_MSG_ID, msgId);
		values.put(GroupChatDeliveryInfoData.KEY_CONTACT, contactNumber);
		values.put(GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS, ChatLog.GroupChatDeliveryInfo.DeliveryStatus.NOT_DELIVERED);
		values.put(GroupChatDeliveryInfoData.KEY_REASON_CODE, ChatLog.GroupChatDeliveryInfo.ReasonCode.NONE);
		values.put(GroupChatDeliveryInfoData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(GroupChatDeliveryInfoData.KEY_TIMESTAMP_DISPLAYED, 0);
		return cr.insert(GroupChatDeliveryInfoDatabaseUri, values);
	}

	/**
	 * Set delivery status for outgoing group chat messages and files
	 *
	 * @param msgID Message ID
	 * @param deliveryStatus Delivery status of entry
	 * @param contact The contact for which the entry is to be updated
	 */
	private void updateGroupChatDeliveryInfoStatus(String msgId, int deliveryStatus, String contact) {
		ContentValues values = new ContentValues();
		values.put(GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS, deliveryStatus);
		values.put(GroupChatDeliveryInfoData.KEY_TIMESTAMP_DELIVERED, System.currentTimeMillis());
		if (deliveryStatus != ChatLog.GroupChatDeliveryInfo.DeliveryStatus.FAILED) {
			values.put(GroupChatDeliveryInfoData.KEY_REASON_CODE,
					ChatLog.GroupChatDeliveryInfo.ReasonCode.NONE);
		} else {
			Pair<Integer, Integer> statusAndReasonCode = getGroupChatDeliveryInfoStatus(msgId,
					contact);
			if (ChatLog.GroupChatDeliveryInfo.DeliveryStatus.DELIVERED == statusAndReasonCode.first) {
				values.put(GroupChatDeliveryInfoData.KEY_REASON_CODE,
						ChatLog.GroupChatDeliveryInfo.ReasonCode.DISPLAY_ERROR);

			} else if (ChatLog.GroupChatDeliveryInfo.DeliveryStatus.FAILED == statusAndReasonCode.first
					&& ChatLog.GroupChatDeliveryInfo.ReasonCode.DELIVERY_ERROR == statusAndReasonCode.second) {
				values.put(GroupChatDeliveryInfoData.KEY_REASON_CODE,
						ChatLog.GroupChatDeliveryInfo.ReasonCode.DELIVERY_AND_DISPLAY_ERROR);
			} else {
				values.put(GroupChatDeliveryInfoData.KEY_REASON_CODE,
						ChatLog.GroupChatDeliveryInfo.ReasonCode.DELIVERY_ERROR);
			}
		}
		String[] selectionArgs = new String[] {
				msgId, contact
		};
		if (cr.update(GroupChatDeliveryInfoDatabaseUri, values,
				SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT, selectionArgs) < 1) {
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("There was not group chat delivery into for msgId '" + msgId
						+ "' and contact '" + contact + "' to update!");
			}
		}
	}

	/**
	 * Set delivery status for outgoing group chat messages and files
	 * 
	 * @param msgID Message ID
	 * @param status Delivery status
	 * @param contact The contact for which the entry is to be updated
	 */
	public void updateGroupChatDeliveryInfoStatus(String msgId, String status, String contact) {
		if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
			updateGroupChatDeliveryInfoStatus(msgId,
					ChatLog.GroupChatDeliveryInfo.DeliveryStatus.DELIVERED, contact);
		} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
			updateGroupChatDeliveryInfoStatus(msgId,
					ChatLog.GroupChatDeliveryInfo.DeliveryStatus.DISPLAYED, contact);
		} else if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)) {
			updateGroupChatDeliveryInfoStatus(msgId,
					ChatLog.GroupChatDeliveryInfo.DeliveryStatus.FAILED, contact);
		} else if (ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)) {
			updateGroupChatDeliveryInfoStatus(msgId,
					ChatLog.GroupChatDeliveryInfo.DeliveryStatus.FAILED, contact);
		} else if (ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
			updateGroupChatDeliveryInfoStatus(msgId,
					ChatLog.GroupChatDeliveryInfo.DeliveryStatus.FAILED, contact);
		}
	}

	/**
	 * Get status for individual contact
	 *
	 * @param msgID Message ID
	 * @param contact Contact for which the status should be retrieved
	 * @return int Status
	 */
	public Pair<Integer, Integer> getGroupChatDeliveryInfoStatus(String msgId, String contact) {
		Cursor cursor = null;
		try {
			String[] selectionArgs = new String[] {
					msgId, contact
			};
			String[] projection = new String[] {
				GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS,
				GroupChatDeliveryInfoData.KEY_REASON_CODE,
			};
			cursor = cr.query(GroupChatDeliveryInfoDatabaseUri, projection,
					SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT, selectionArgs, null);
			if (!cursor.moveToFirst()) {
				if (logger.isActivated()) {
					logger.warn("There was no group chat delivery info for msgId '" + msgId
							+ "' and contact '" + contact + "' to get status from!");
					return new Pair<Integer, Integer>(
							ChatLog.GroupChatDeliveryInfo.DeliveryStatus.NOT_DELIVERED,
							ChatLog.GroupChatDeliveryInfo.ReasonCode.NONE);
				}
			}
			int deliveryStatus = cursor.getInt(cursor.getColumnIndexOrThrow(GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS));
			int statusReason = cursor.getInt(cursor.getColumnIndexOrThrow(GroupChatDeliveryInfoData.KEY_REASON_CODE));
			return new Pair<Integer, Integer>(deliveryStatus, statusReason);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Check if all recipients have received message
	 *
	 * @param msgId Message ID
	 * @return true If it is last contact to receive message
	 */
	public boolean isDeliveredToAllRecipients(String msgId) {
		Cursor cursor = null;
		try {
			cursor = cr.query(Uri.withAppendedPath(GroupChatDeliveryInfoData.CONTENT_MSG_URI, msgId), null,
					SELECTION_CONTACTS_NOT_RECEIVED_MESSAGE, null, null);
			return !cursor.moveToFirst();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Check if all recipients have displayed message
	 *
	 * @param msgId Message ID
	 * @return true If it is last contact to display message
	 */
	public boolean isDisplayedByAllRecipients(String msgId) {
		Cursor cursor = null;
		try {
			cursor = cr.query(Uri.withAppendedPath(GroupChatDeliveryInfoData.CONTENT_MSG_URI, msgId), null,
					GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS + "!= "
							+ ChatLog.GroupChatDeliveryInfo.DeliveryStatus.DISPLAYED, null, null);
			return !cursor.moveToFirst();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}
