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

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;

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
	public void addSpamMessage(InstantMessage msg);

	/**
	 * Add a chat message
	 * 
	 * @param msg
	 *            Chat message
	 */
	public void addIncomingOneToOneChatMessage(InstantMessage msg);

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
	public void addOutgoingOneToOneChatMessage(InstantMessage msg, int status, int reasonCode);

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
	public void addGroupChatMessage(String chatId, InstantMessage msg, int direction, int status, int reasonCode);

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
	public void addGroupChatSystemMessage(String chatId, ContactId contact, int status);

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
	 * Update chat message status
	 * 
	 * @param msgId
	 *            Message ID
	 * @param status
	 *            Message status
	 * @param reasonCode
	 *            Message status reason code
	 */
	public void updateChatMessageStatusAndReasonCode(String msgId, int status, int reasonCode);

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
}
