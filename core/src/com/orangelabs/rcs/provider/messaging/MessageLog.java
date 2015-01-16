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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.RcsCommon.ReadStatus;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.provider.LocalContentResolver;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Class to interface the message table
 *
 */
public class MessageLog implements IMessageLog {

	private LocalContentResolver mLocalContentResolver;

	private GroupChatLog groupChatLog;

	private GroupDeliveryInfoLog groupChatDeliveryInfoLog;
	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(MessageLog.class.getSimpleName());

	private static final String[] PROJECTION_MESSAGE_ID = new String[] { MessageData.KEY_MESSAGE_ID };

	private static final int FIRST_COLUMN_IDX = 0;

	/**
	 * Constructor
	 *
	 * @param localContentResolver
	 *            Local content resolver
	 * @param groupChatLog
	 * @param groupChatDeliveryInfoLog
	 */
	/* package private */MessageLog(LocalContentResolver localContentResolver, GroupChatLog groupChatLog, GroupDeliveryInfoLog groupChatDeliveryInfoLog) {
		mLocalContentResolver = localContentResolver;
		this.groupChatLog = groupChatLog;
		this.groupChatDeliveryInfoLog = groupChatDeliveryInfoLog;
	}

	private void addIncomingOneToOneMessage(ChatMessage msg, int status,
			int reasonCode) {
		ContactId contact = msg.getRemoteContact();
		String msgId = msg.getMessageId();
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add incoming chat message: contact=")
					.append(contact).append(", msg=").append(msgId).append(", status=")
					.append(status).append(", reasonCode=").append(reasonCode).append(".")
					.toString());
		}

		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, contact.toString());
		values.put(MessageData.KEY_MESSAGE_ID, msgId);
		values.put(MessageData.KEY_CONTACT, contact.toString());
		values.put(MessageData.KEY_DIRECTION, Direction.INCOMING);
		values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD);
		String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(msg.getMimeType());
		values.put(MessageData.KEY_MIME_TYPE, apiMimeType);
		values.put(MessageData.KEY_CONTENT, ChatUtils.networkContentToPersistedContent(apiMimeType,
				msg.getContent()));

		values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
		values.put(MessageData.KEY_TIMESTAMP_SENT, 0);
		values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);

		values.put(MessageData.KEY_STATUS, status);
		values.put(MessageData.KEY_REASON_CODE, reasonCode);
		mLocalContentResolver.insert(ChatLog.Message.CONTENT_URI, values);
	}

	/**
	 * Add outgoing one-to-one chat message
	 *
	 * @param msg Chat message
	 * @param status Status
	 * @param reasonCode Reason code
	 */
	@Override
	public void addOutgoingOneToOneChatMessage(ChatMessage msg, int status,
			int reasonCode) {
		ContactId contact = msg.getRemoteContact();
		String msgId = msg.getMessageId();
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add outgoing chat message: contact=")
					.append(contact).append(", msg=").append(msgId).append(", status=")
					.append(status).append(", reasonCode=").append(reasonCode).append(".")
					.toString());
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, contact.toString());
		values.put(MessageData.KEY_MESSAGE_ID, msgId);
		values.put(MessageData.KEY_CONTACT, contact.toString());
		values.put(MessageData.KEY_DIRECTION, Direction.OUTGOING);
		values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD);
		String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(msg.getMimeType());
		values.put(MessageData.KEY_MIME_TYPE, apiMimeType);
		values.put(MessageData.KEY_CONTENT,
				ChatUtils.networkContentToPersistedContent(apiMimeType, msg.getContent()));

		values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
		values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getDate().getTime());
		values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);

		values.put(MessageData.KEY_STATUS, status);
		values.put(MessageData.KEY_REASON_CODE, reasonCode);
		mLocalContentResolver.insert(ChatLog.Message.CONTENT_URI, values);
	}

	@Override
	public void addOneToOneSpamMessage(ChatMessage msg) {
		addIncomingOneToOneMessage(msg, ChatLog.Message.Status.Content.REJECTED,
				ChatLog.Message.ReasonCode.REJECTED_SPAM);
	}

	/**
	 * Add incoming one-to-one chat message
	 *
	 * @param msg Chat message
	 * @param imdnDisplayedRequested Indicates whether IMDN display was requested
	 */
	@Override
	public void addIncomingOneToOneChatMessage(ChatMessage msg, boolean imdnDisplayedRequested) {
		if (imdnDisplayedRequested) {
			addIncomingOneToOneMessage(msg,
					ChatLog.Message.Status.Content.DISPLAY_REPORT_REQUESTED,
					ChatLog.Message.ReasonCode.UNSPECIFIED);

		} else {
			addIncomingOneToOneMessage(msg, ChatLog.Message.Status.Content.RECEIVED,
					ChatLog.Message.ReasonCode.UNSPECIFIED);
		}
	}

	/**
	 * Add group chat message
	 *
	 * @param chatId Chat ID
	 * @param msg Chat message
	 * @param msg direction Direction
	 * @param status Status
	 * @param reasonCode Reason code
	 */
	@Override
	public void addGroupChatMessage(String chatId, ChatMessage msg, int direction, int status, int reasonCode) {
		String msgId = msg.getMessageId();
		ContactId contact = msg.getRemoteContact() ;
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add group chat message; chatId=").append(chatId)
					.append(", msg=").append(msgId).append(", dir=").append(direction)
					.append(", contact=").append(contact).append(".").toString());
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, chatId);
		values.put(MessageData.KEY_MESSAGE_ID, msgId);
		if (contact != null) {
			values.put(MessageData.KEY_CONTACT, contact.toString());
		}
		values.put(MessageData.KEY_DIRECTION, direction);
		values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD);
		values.put(MessageData.KEY_STATUS, status);
		values.put(MessageData.KEY_REASON_CODE, reasonCode);
		String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(msg.getMimeType());
		values.put(MessageData.KEY_MIME_TYPE, apiMimeType);
		values.put(MessageData.KEY_CONTENT,
				ChatUtils.networkContentToPersistedContent(apiMimeType, msg.getContent()));

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
		mLocalContentResolver.insert(ChatLog.Message.CONTENT_URI, values);

		if (direction == Direction.OUTGOING) {
			try {
				int deliveryStatus = com.gsma.services.rcs.GroupDeliveryInfoLog.Status.NOT_DELIVERED;
				if (RcsSettings.getInstance().isAlbatrosRelease()) {
					deliveryStatus = com.gsma.services.rcs.GroupDeliveryInfoLog.Status.UNSUPPORTED;
				}
				Set<ParticipantInfo> participants = groupChatLog.getGroupChatConnectedParticipants(chatId);
				for (ParticipantInfo participant : participants) {
					groupChatDeliveryInfoLog.addGroupChatDeliveryInfoEntry(chatId,
							participant.getContact(), msgId, deliveryStatus,
							com.gsma.services.rcs.GroupDeliveryInfoLog.ReasonCode.UNSPECIFIED);
				}
			} catch (Exception e) {
				mLocalContentResolver.delete(Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, msgId), null, null);
				mLocalContentResolver.delete(Uri.withAppendedPath(GroupDeliveryInfoData.CONTENT_URI, msgId), null, null);
				/* TODO: Throw exception */
				if (logger.isActivated()) {
					logger.warn("Group chat message with msgId '" + msgId + "' could not be added to database!");
				}
			}
		}
	}

	@Override
	public void addGroupChatEvent(String chatId, ContactId contact, int status) {
		if (logger.isActivated()) {
			logger.debug("Add group chat system message: chatID=" + chatId + ", contact=" + contact + ", status=" + status);
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_CHAT_ID, chatId);
		if (contact != null) {
			values.put(MessageData.KEY_CONTACT, contact.toString());
		}
		values.put(MessageData.KEY_MESSAGE_ID, IdGenerator.generateMessageID());
		values.put(MessageData.KEY_MIME_TYPE, MimeType.GROUPCHAT_EVENT);
		values.put(MessageData.KEY_STATUS, status);
		values.put(MessageData.KEY_REASON_CODE, ChatLog.Message.ReasonCode.UNSPECIFIED);
		values.put(MessageData.KEY_DIRECTION, Direction.IRRELEVANT);
		values.put(ChatData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
		values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD);
		values.put(MessageData.KEY_TIMESTAMP_SENT, 0);
		values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
		mLocalContentResolver.insert(ChatLog.Message.CONTENT_URI, values);
	}

	@Override
	public void markMessageAsRead(String msgId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Marking chat message as read: msgID=").append(msgId).toString());
		}
		ContentValues values = new ContentValues();
		values.put(MessageData.KEY_READ_STATUS, ReadStatus.READ);
		values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, Calendar.getInstance().getTimeInMillis());

		if (mLocalContentResolver.update(Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, msgId), values, null, null) < 1) {
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("There was no message with msgId '" + msgId + "' to mark as read.");
			}
		}
	}

	@Override
	public void setChatMessageStatusAndReasonCode(String msgId, int status, int reasonCode) {
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

		if (mLocalContentResolver.update(Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, msgId), values, null, null) < 1) {
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("There was no message with msgId '" + msgId + "' to update status for.");
			}
		}
	}

	@Override
	public void markIncomingChatMessageAsReceived(String msgId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Mark incoming chat message status as received for msgID=").append(msgId).toString());
		}
		setChatMessageStatusAndReasonCode(msgId, ChatLog.Message.Status.Content.RECEIVED, ChatLog.Message.ReasonCode.UNSPECIFIED);
	}

	@Override
	public boolean isMessagePersisted(String msgId) {
		Cursor cursor = null;
		try {
			cursor = mLocalContentResolver.query(Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, msgId), PROJECTION_MESSAGE_ID, null, null, null);
			return cursor.moveToFirst();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private Cursor getMessageData(String columnName, String msgId) throws SQLException {
		String[] projection = new String[] {
			columnName
		};
		Cursor cursor = null;
		try {
			cursor = mLocalContentResolver.query(
					Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, msgId), projection, null,
					null, null);
			if (cursor.moveToFirst()) {
				return cursor;
			}

			throw new SQLException("No row returned while querying for message data with msgId : "
					+ msgId);

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

	private long getDataAsLong(Cursor cursor) {
		try {
			return cursor.getLong(FIRST_COLUMN_IDX);

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private String getDataAsString(Cursor cursor) {
		try {
			return cursor.getString(FIRST_COLUMN_IDX);

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@Override
	public boolean isMessageRead(String msgId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Is message read for ").append(msgId).toString());
		}
		return (getDataAsInt(getMessageData(MessageData.KEY_READ_STATUS, msgId)) == 1);
	}

	@Override
	public long getMessageSentTimestamp(String msgId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Get message sent timestamp for ").append(msgId)
					.toString());
		}
		return getDataAsLong(getMessageData(MessageData.KEY_TIMESTAMP_SENT, msgId));
	}

	@Override
	public long getMessageDeliveredTimestamp(String msgId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Get message delivered timestamp for ").append(msgId)
					.toString());
		}
		return getDataAsLong(getMessageData(MessageData.KEY_TIMESTAMP_DELIVERED, msgId));
	}

	@Override
	public long getMessageDisplayedTimestamp(String msgId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Get message displayed timestamp for ").append(msgId)
					.toString());
		}
		return getDataAsLong(getMessageData(MessageData.KEY_TIMESTAMP_DISPLAYED, msgId));
	}

	@Override
	public int getMessageStatus(String msgId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Get message status for ").append(msgId).toString());
		}
		return getDataAsInt(getMessageData(MessageData.KEY_STATUS, msgId));
	}

	@Override
	public int getMessageReasonCode(String msgId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Get message reason code for ").append(msgId).toString());
		}
		return getDataAsInt(getMessageData(MessageData.KEY_REASON_CODE, msgId));
	}

	@Override
	public String getMessageMimeType(String msgId) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Get message MIME-type for ").append(msgId).toString());
		}
		return getDataAsString(getMessageData(MessageData.KEY_MIME_TYPE, msgId));
	}

	@Override
	public Cursor getCacheableChatMessageData(String msgId) {
		Cursor cursor = null;
		try {
			cursor = mLocalContentResolver.query(
					Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, msgId), null, null, null,
					null);
			if (cursor.moveToFirst()) {
				return cursor;
			}

			throw new SQLException("No row returned while querying for message data with msgId : "
					+ msgId);

		} catch (RuntimeException e) {
			if (cursor != null) {
				cursor.close();
			}
			throw e;
		}
	}
}
