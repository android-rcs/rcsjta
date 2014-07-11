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

import java.util.Calendar;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.service.im.chat.FileTransferMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Class to interface the message table
 * 
 */
public class MessageLog implements IMessageLog {

	/**
	 * Message database URI
	 */
	private Uri msgDatabaseUri = MessageData.CONTENT_URI;

	private static final String SELECTION_MSG_BY_MSG_ID = new StringBuilder(MessageData.KEY_MSG_ID).append("=?").toString();

	/**
	 * Content resolver
	 */
	private ContentResolver cr;

	private GroupChatLog groupChatLog;

	private GroupChatDeliveryInfoLog groupChatDeliveryInfoLog;
	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(MessageLog.class.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param cr
	 *            Content resolver
	 * @param groupChatLog
	 * @param groupChatDeliveryInfoLog
	 */
	/* package private */MessageLog(ContentResolver cr, GroupChatLog groupChatLog, GroupChatDeliveryInfoLog groupChatDeliveryInfoLog) {
		this.cr = cr;
		this.groupChatLog = groupChatLog;
		this.groupChatDeliveryInfoLog = groupChatDeliveryInfoLog;
	}

	/**
	 * Format geoloc object to string
	 * 
	 * @param geoloc
	 *            Geoloc object
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
	 * Add a chat message
	 * 
	 * @param msg
	 *            Chat message
	 * @param type
	 *            Message type
	 * @param direction
	 *            Direction
	 */
	private void addChatMessage(InstantMessage msg, int type, int direction) {
		if (logger.isActivated()) {
			logger.debug("Add chat message: contact=" + msg.getRemote() + ", msg=" + msg.getMessageId() + ", dir=" + direction);
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, msg.getRemote().toString());
		values.put(MessageData.KEY_MSG_ID, msg.getMessageId());
		values.put(MessageData.KEY_CONTACT, msg.getRemote().toString());
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.orangelabs.rcs.provider.messaging.IMessageLog#addSpamMessage(com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage)
	 */
	@Override
	public void addSpamMessage(InstantMessage msg) {
		addChatMessage(msg, ChatLog.Message.Type.SPAM, ChatLog.Message.Direction.INCOMING);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.orangelabs.rcs.provider.messaging.IMessageLog#addChatMessage(com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage,
	 * int)
	 */
	@Override
	public void addChatMessage(InstantMessage msg, int direction) {
		addChatMessage(msg, ChatLog.Message.Type.CONTENT, direction);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#addGroupChatMessage(java.lang.String,
	 * com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage, int)
	 */
	@Override
	public void addGroupChatMessage(String chatId, InstantMessage msg, int direction) {
		String msgId = msg.getMessageId();
		if (logger.isActivated()) {
			logger.debug("Add group chat message: chatID=" + chatId + ", msg=" + msgId + ", dir=" + direction+ ", contact="+msg.getRemote());
		}

		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, chatId);
		values.put(MessageData.KEY_MSG_ID, msgId);
		if (msg.getRemote() != null) {
			values.put(MessageData.KEY_CONTACT, msg.getRemote().toString());
		}
		values.put(MessageData.KEY_DIRECTION, direction);
		values.put(MessageData.KEY_TYPE, ChatLog.Message.Type.CONTENT);
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
			values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.RECEIVED);
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
				Set<ParticipantInfo> participants = groupChatLog.getGroupChatConnectedParticipants(chatId);
				for (ParticipantInfo participant : participants) {
					groupChatDeliveryInfoLog.addGroupChatDeliveryInfoEntry(chatId, msgId, participant.getContact());
				}
			} catch (Exception e) {
				cr.delete(msgDatabaseUri, MessageData.KEY_MSG_ID + "='" + msgId + "'", null);
				cr.delete(Uri.withAppendedPath(GroupChatDeliveryInfoData.CONTENT_MSG_URI, msgId), null, null);
				/* TODO: Throw exception */
				if (logger.isActivated()) {
					logger.warn("Group chat message with msgId '" + msgId + "' could not be added to database!");
				}
			}
		}
	}

	@Override
	public void addGroupChatSystemMessage(String chatId, ContactId contact, int status) {
		if (logger.isActivated()) {
			logger.debug("Add group chat system message: chatID=" + chatId + ", contact=" + contact + ", status=" + status);
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, chatId);
		if (contact != null) {
			values.put(MessageData.KEY_CONTACT, contact.toString());
		}
		values.put(MessageData.KEY_TYPE, ChatLog.Message.Type.SYSTEM);
		values.put(MessageData.KEY_STATUS, status);
		values.put(MessageData.KEY_DIRECTION, ChatLog.Message.Direction.IRRELEVANT);
		values.put(ChatData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
		cr.insert(msgDatabaseUri, values);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#markMessageAsRead(java.lang.String)
	 */
	@Override
	public void markMessageAsRead(String msgId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Marking chat message as read: msgID=").append(msgId).toString());
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_READ_STATUS, ChatLog.Message.ReadStatus.READ);
		values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, Calendar.getInstance().getTimeInMillis());

		if (cr.update(msgDatabaseUri, values, SELECTION_MSG_BY_MSG_ID, new String[] { msgId }) < 1) {
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("There was no message with msgId '" + msgId + "' to mark as read.");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#updateChatMessageStatus(java.lang.String, int)
	 */
	@Override
	public void updateChatMessageStatus(String msgId, int status) {
		if (logger.isActivated()) {
			logger.debug("Update chat message: msgID=" + msgId + ", status=" + status);
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_STATUS, status);
		if (status == ChatLog.Message.Status.Content.DELIVERED) {
			// Delivered
			values.put(MessageData.KEY_TIMESTAMP_DELIVERED, Calendar.getInstance().getTimeInMillis());
		}

		if (cr.update(msgDatabaseUri, values, SELECTION_MSG_BY_MSG_ID, new String[] { msgId }) < 1) {
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("There was no message with msgId '" + msgId + "' to update status for.");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#markIncomingChatMessageAsReceived(java.lang.String)
	 */
	@Override
	public void markIncomingChatMessageAsReceived(String msgId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Mark incoming chat message status as received for msgID=").append(msgId).toString());
		}
		updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.RECEIVED);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#updateOutgoingChatMessageDeliveryStatus(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void updateOutgoingChatMessageDeliveryStatus(String msgId, String status) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Update chat delivery status: msgID=").append(msgId).append(", status=").append(status)
					.toString());
		}
		if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
			updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.DELIVERED);
		} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
			markMessageAsRead(msgId);
		} else if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)) {
			updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
		} else if (ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)) {
			updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
		} else if (ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
			updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#updateChatMessageDeliveryStatus(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateChatMessageDeliveryStatus(String msgId, String status) {
		if (logger.isActivated()) {
			logger.debug("Update chat delivery status: msgID=" + msgId + ", status=" + status);
		}
		if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)) {
			updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
		} else if (ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)) {
			updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
		} else if (ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
			updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#isNewMessage(java.lang.String, java.lang.String)
	 */
	@Override
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
	
	public boolean isNewMessage(ContactId contact, String msgId) {
		return isNewMessage(contact.toString(), msgId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#setChatMessageDeliveryRequested(java.lang.String)
	 */
	@Override
	public void setChatMessageDeliveryRequested(String msgId) {
		if (logger.isActivated()) {
			logger.debug("Set chat delivery requested: msgID=" + msgId);
		}
		updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.DISPLAY_REPORT_REQUESTED);
	}
}
