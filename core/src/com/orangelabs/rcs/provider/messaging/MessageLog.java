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

package com.orangelabs.rcs.provider.messaging;

import java.util.Calendar;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.RcsCommon.ReadStatus;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
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
	 * Add incoming one-to-one chat message
	 * 
	 * @param msg Chat message
	 * @param status Status
	 * @param reasonCode Reason code
	 */
	private void addIncomingOneToOneMessage(InstantMessage msg, int status, int reasonCode) {
		ContactId contact = msg.getRemote();
		String msgId = msg.getMessageId();
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add incoming chat message: contact=")
			.append(contact).append(", msg=").append(msgId)
			.append(", status=").append(status).append(", reasonCode=").append(reasonCode)
			.toString());
		}

		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, contact.toString());
		values.put(MessageData.KEY_MSG_ID, msgId);
		values.put(MessageData.KEY_CONTACT, contact.toString());
		values.put(MessageData.KEY_DIRECTION, Direction.INCOMING);
		values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD);

		if (msg instanceof GeolocMessage) {
			values.put(MessageData.KEY_CONTENT_TYPE, MimeType.GEOLOC_MESSAGE);
			GeolocPush geoloc = ((GeolocMessage)msg).getGeoloc();
			Geoloc geolocData = new Geoloc(geoloc.getLabel(), geoloc.getLatitude(),
					geoloc.getLongitude(), geoloc.getExpiration(), geoloc.getAccuracy());
			values.put(MessageData.KEY_CONTENT, geolocToString(geolocData));
		} else {
			values.put(MessageData.KEY_CONTENT_TYPE, MimeType.TEXT_MESSAGE);
			values.put(MessageData.KEY_CONTENT, msg.getTextMessage());
		}

		values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
		values.put(MessageData.KEY_TIMESTAMP_SENT, 0);
		values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);

		values.put(MessageData.KEY_STATUS, status);
		values.put(MessageData.KEY_REASON_CODE, reasonCode);
		cr.insert(msgDatabaseUri, values);
	}

	/**
	 * Add outgoing one-to-one chat message
	 *
	 * @param msg Chat message
	 * @param status Status
	 * @param reasonCode Reason code
	 */
	@Override
	public void addOutgoingOneToOneChatMessage(InstantMessage msg, int status, int reasonCode) {
		ContactId contact = msg.getRemote();
		String msgId = msg.getMessageId();
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add outgoing chat message: contact=").append(contact)
					.append(", msg=").append(msgId).append(", status=").append(status)
					.append(", reasonCode=").append(reasonCode).toString());
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, contact.toString());
		values.put(MessageData.KEY_MSG_ID, msgId);
		values.put(MessageData.KEY_CONTACT, contact.toString());
		values.put(MessageData.KEY_DIRECTION, Direction.OUTGOING);
		values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD);

		if (msg instanceof GeolocMessage) {
			values.put(MessageData.KEY_CONTENT_TYPE, MimeType.GEOLOC_MESSAGE);
			GeolocPush geoloc = ((GeolocMessage)msg).getGeoloc();
			Geoloc geolocData = new Geoloc(geoloc.getLabel(), geoloc.getLatitude(),
					geoloc.getLongitude(), geoloc.getExpiration(), geoloc.getAccuracy());
			values.put(MessageData.KEY_CONTENT, geolocToString(geolocData));
		} else {
			values.put(MessageData.KEY_CONTENT_TYPE, MimeType.TEXT_MESSAGE);
			values.put(MessageData.KEY_CONTENT, msg.getTextMessage());
		}

		values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
		values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getDate().getTime());
		values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);

		values.put(MessageData.KEY_STATUS, status);
		values.put(MessageData.KEY_REASON_CODE, reasonCode);
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
		addIncomingOneToOneMessage(msg, ChatLog.Message.Status.Content.REJECTED,
				ChatLog.Message.ReasonCode.REJECTED_SPAM);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.orangelabs.rcs.provider.messaging.IMessageLog#addChatMessage(com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage,
	 * int)
	 */
	@Override
	public void addIncomingOneToOneChatMessage(InstantMessage msg) {
			if (msg.isImdnDisplayedRequested()) {
				addIncomingOneToOneMessage(msg,
						ChatLog.Message.Status.Content.DISPLAY_REPORT_REQUESTED,
						ChatLog.Message.ReasonCode.UNSPECIFIED);

			} else {
				addIncomingOneToOneMessage(msg, ChatLog.Message.Status.Content.RECEIVED,
						ChatLog.Message.ReasonCode.UNSPECIFIED);
			}

	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#addGroupChatMessage(java.lang.String,
	 * com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage, int, int, int)
	 */
	@Override
	public void addGroupChatMessage(String chatId, InstantMessage msg, int direction, int status, int reasonCode) {
		String msgId = msg.getMessageId();
		if (logger.isActivated()) {
			logger.debug("Add group chat message: chatID=" + chatId + ", msg=" + msgId + ", dir="
					+ direction + ", contact=" + msg.getRemote());
		}

		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, chatId);
		values.put(MessageData.KEY_MSG_ID, msgId);
		if (msg.getRemote() != null) {
			values.put(MessageData.KEY_CONTACT, msg.getRemote().toString());
		}
		values.put(MessageData.KEY_DIRECTION, direction);
		values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD);
		values.put(MessageData.KEY_STATUS, status);
		values.put(MessageData.KEY_REASON_CODE, reasonCode);

		//file transfer are not handled here but in FileTransferLog; therefore FileTransferMessages are not to be processed here
		if (msg instanceof GeolocMessage) {
			values.put(MessageData.KEY_CONTENT_TYPE, MimeType.GEOLOC_MESSAGE);
			GeolocPush geoloc = ((GeolocMessage) msg).getGeoloc();
			Geoloc geolocData = new Geoloc(geoloc.getLabel(), geoloc.getLatitude(), geoloc.getLongitude(), geoloc.getExpiration(),
					geoloc.getAccuracy());
			values.put(MessageData.KEY_CONTENT, geolocToString(geolocData));
		} else {
			values.put(MessageData.KEY_CONTENT_TYPE, MimeType.TEXT_MESSAGE);
			values.put(MessageData.KEY_CONTENT, msg.getTextMessage());
		}

		if (direction == Direction.INCOMING) {
			// Receive message
			values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
			values.put(MessageData.KEY_TIMESTAMP_SENT, 0);
			values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
		} else {
			// Send message
			values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
			values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getDate().getTime());
			values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
			values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
		}
		cr.insert(msgDatabaseUri, values);

		if (direction == Direction.OUTGOING) {
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
		values.put(MessageData.KEY_CONTENT_TYPE, MimeType.GROUPCHAT_EVENT);
		values.put(MessageData.KEY_STATUS, status);
		values.put(MessageData.KEY_REASON_CODE, ChatLog.Message.ReasonCode.UNSPECIFIED);
		values.put(MessageData.KEY_DIRECTION, Direction.IRRELEVANT);
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
		values.put(MessageData.KEY_READ_STATUS, ReadStatus.READ);
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
	 * @see com.orangelabs.rcs.provider.messaging.IMessageLog#
	 * updateChatMessageStatusAndReasonCode(java.lang.String, int, int)
	 */
	@Override
	public void updateChatMessageStatusAndReasonCode(String msgId, int status, int reasonCode) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Update chat message: msgID=").append(msgId)
					.append(", status=").append(status).append("reasonCode=").append(reasonCode)
					.toString());
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_STATUS, status);
		values.put(MessageData.KEY_REASON_CODE, reasonCode);
		if (status == ChatLog.Message.Status.Content.DELIVERED) {
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
		updateChatMessageStatusAndReasonCode(msgId, ChatLog.Message.Status.Content.RECEIVED, ChatLog.Message.ReasonCode.UNSPECIFIED);
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
}
