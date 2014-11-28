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

import java.util.Set;

import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatInfo;

/**
 * Interface for the chat table
 * 
 * @author LEMORDANT Philippe
 * 
 */
public interface IGroupChatLog {

	/**
	 * Add group chat session
	 * 
	 * @param chatId
	 *            Chat ID
	 * @param contact
	 *            Contact ID
	 * @param subject
	 *            Subject
	 * @param participants
	 *            List of participants
	 * @param state
	 *            State
	 * @param reasonCode
	 *            ReasonCode
	 * @param direction
	 *            Direction
	 */
	public void addGroupChat(String chatId, ContactId contact, String subject,
			Set<ParticipantInfo> participants, int state, int reasonCode, int direction);

	/**
	 * Accept next Group Chat invitation
	 * 
	 * @param chatId
	 */
	public void acceptGroupChatNextInvitation(String chatId);

	/**
	 * Update group chat status
	 * 
	 * @param chatId
	 *            Chat ID
	 * @param state
	 *            Group chat state
	 * @param reasonCode
	 *            Group chat state reason code
	 */
	public void updateGroupChatStateAndReasonCode(String chatId, int state, int reasonCode);

	/**
	 * Update group chat set of participants
	 * 
	 * @param chatId
	 *            Chat ID
	 * @param participants
	 *            The set of participants
	 */
	public void updateGroupChatParticipant(String chatId, Set<ParticipantInfo> participants);

	/**
	 * Update group chat rejoin ID
	 * 
	 * @param chatId
	 *            Chat ID
	 * @param rejoinId
	 *            Rejoin ID
	 * @param status
	 *            Status
	 */
	public void updateGroupChatRejoinIdOnSessionStart(String chatId, String rejoinId);

	/**
	 * Get the group chat info
	 * 
	 * @param chatId
	 *            Chat ID
	 * @result Group chat info
	 */
	public GroupChatInfo getGroupChatInfo(String chatId);

	/**
	 * Get the group chat participants
	 * 
	 * @param chatId
	 *            Chat ID
	 * @result List of contacts
	 */
	public Set<ParticipantInfo> getGroupChatConnectedParticipants(String chatId);

	/**
	 * Is next group chat Invitation rejected
	 * 
	 * @param chatId
	 *            Chat ID
	 * @return true if next GC invitation should be rejected
	 */
	public boolean isGroupChatNextInviteRejected(String chatId);
}
