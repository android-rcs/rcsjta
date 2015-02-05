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

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatMessage;

import android.database.Cursor;

/**
 * Interface for the message table
 * 
 * @author LEMORDANT Philippe
 * 
 */
public interface IMessageLog {

	/**
	 * Add a spam message
	 * 
	 * @param msg
	 *            Chat message
	 */
	public void addOneToOneSpamMessage(ChatMessage msg);

	/**
	 * Add a chat message
	 * 
	 * @param msg
	 *            Chat message
	 * @param imdnDisplayedRequested
	 *            IMDN display report requested
	 */
	public void addIncomingOneToOneChatMessage(ChatMessage msg, boolean imdnDisplayedRequested);

	/**
	 * Add a chat message
	 * 
	 * @param msg
	 *            Chat message
	 * @param status
	 *            Message status
	 * @param reasonCode
	 *            Status reason code
	 */
	public void addOutgoingOneToOneChatMessage(ChatMessage msg, int status, int reasonCode);

	/**
	 * Add a group chat message
	 * 
	 * @param chatId
	 *            Chat ID
	 * @param msg
	 *            Chat message
	 * @param direction
	 *            Direction
	 * @param status
	 *            Message status
	 * @param reasonCode
	 *            Status reason code
	 */
	public void addGroupChatMessage(String chatId, ChatMessage msg, Direction direction,
			int status, int reasonCode);

	/**
	 * Add group chat system message
	 * 
	 * @param chatId
	 *            Chat ID
	 * @param contact
	 *            Contact ID
	 * @param status
	 *            Status
	 */
	public void addGroupChatEvent(String chatId, ContactId contact, int status);

	/**
	 * Update chat message read status
	 * 
	 * @param msgId
	 *            Message ID
	 * @param status
	 *            Message status
	 */
	public void markMessageAsRead(String msgId);

	/**
	 * Set chat message status and reason code
	 * 
	 * @param msgId
	 *            Message ID
	 * @param status
	 *            Message status
	 * @param reasonCode
	 *            Message status reason code
	 */
	public void setChatMessageStatusAndReasonCode(String msgId, int status, int reasonCode);

	/**
	 * Mark incoming chat message status as received
	 * 
	 * @param msgId
	 *            Message ID
	 */
	public void markIncomingChatMessageAsReceived(String msgId);

	/**
	 * Check if the message is already persisted in db
	 * 
	 * @param msgId
	 *            message ID
	 * @return true if the message already exists in db
	 */
	public boolean isMessagePersisted(String msgId);

	/**
	 * Check if the message is read by remote contact
	 * 
	 * @param msgId
	 *            message ID
	 * @return true is read
	 */
	public boolean isMessageRead(String msgId);

	/**
	 * Returns the timestamp_sent of a message
	 * 
	 * @param msgId
	 * @return timestamp_sent
	 */
	public long getMessageSentTimestamp(String msgId);

	/**
	 * Returns the timestamp of a message
	 * 
	 * @param msgId
	 * @return timestamp
	 */
	public long getMessageTimestamp(String msgId);

	/**
	 * Get message state from its unique ID
	 * 
	 * @param msgId
	 * @return State
	 */
	public int getMessageStatus(String msgId);

	/**
	 * Get message reason code from its unique ID
	 * 
	 * @param msgId
	 * @return reason code of the state
	 */
	public int getMessageReasonCode(String msgId);

	/**
	 * Get message MIME-type from its unique ID
	 * 
	 * @param msgId
	 * @return MIME-type
	 */
	public String getMessageMimeType(String msgId);

	/**
	 * Get cacheable message data from its unique ID
	 * 
	 * @param msgId
	 * @return Cursor
	 */
	public Cursor getCacheableChatMessageData(String msgId);

	/**
	 * Get chat message content
	 * 
	 * @param msgId
	 * @return Content of chat message
	 */
	public String getChatMessageContent(String msgId);
}
