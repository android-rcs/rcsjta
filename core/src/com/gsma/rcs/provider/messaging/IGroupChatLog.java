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

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChat.ReasonCode;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;

import java.util.Map;
import java.util.Set;

/**
 * Interface for the chat table
 * 
 * @author LEMORDANT Philippe
 */
public interface IGroupChatLog {

    /**
     * Add group chat session
     * 
     * @param chatId Chat ID
     * @param contact Contact ID
     * @param subject Subject
     * @param participants map of participants and associated status
     * @param state State
     * @param reasonCode ReasonCode
     * @param direction Direction
     * @param timestamp Timestamp
     */
    public void addGroupChat(String chatId, ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, State state, ReasonCode reasonCode,
            Direction direction, long timestamp);

    /**
     * Accept next Group Chat invitation
     * 
     * @param chatId
     */
    public void acceptGroupChatNextInvitation(String chatId);

    /**
     * Set group chat state and reason code
     * 
     * @param chatId Chat ID
     * @param state Group chat state
     * @param reasonCode Group chat state reason code
     */
    public void setGroupChatStateAndReasonCode(String chatId, State state, ReasonCode reasonCode);

    /**
     * Set group chat participants, state and reason code
     * 
     * @param participants map of participants and associated status
     * @param chatId Chat ID
     * @param state Group chat state
     * @param reasonCode Group chat state reason code
     */
    public void setGroupChatParticipantsStateAndReasonCode(
            Map<ContactId, ParticipantStatus> participants, String chatId, State state,
            ReasonCode reasonCode);

    /**
     * Update group chat participants
     * 
     * @param chatId Chat ID
     * @param participants map of participants and associated status
     */
    public void updateGroupChatParticipants(String chatId,
            Map<ContactId, ParticipantStatus> participants);

    /**
     * Set group chat rejoin ID
     * 
     * @param chatId Chat ID
     * @param rejoinId Rejoin ID
     */
    public void setGroupChatRejoinId(String chatId, String rejoinId);

    /**
     * Get the group chat info
     * 
     * @param chatId Chat ID
     * @return Group chat info
     */
    public GroupChatInfo getGroupChatInfo(String chatId);

    /**
     * Is next group chat Invitation rejected
     * 
     * @param chatId Chat ID
     * @return true if next GC invitation should be rejected
     */
    public boolean isGroupChatNextInviteRejected(String chatId);

    /**
     * Set reject the next group chat invitation
     * 
     * @param chatId Chat ID
     */
    public void setRejectNextGroupChatNextInvitation(String chatId);

    /**
     * Get group chat state from its chat ID
     * 
     * @param chatId Chat ID of the group chat
     * @return State
     */
    public State getGroupChatState(String chatId);

    /**
     * Get group chat state reason code from its chat ID
     * 
     * @param chatId Chat ID of the group chat
     * @return Reason code of the state
     */
    public ReasonCode getGroupChatReasonCode(String chatId);

    /**
     * Get group chat participants from its chat ID
     * 
     * @param chatId Chat ID of the group chat
     * @return all group chat participants
     */
    public Map<ContactId, ParticipantStatus> getParticipants(String chatId);

    /**
     * Get cacheable group chat data from its chat ID
     * 
     * @param chatId
     * @return Cursor
     */
    public Cursor getCacheableGroupChatData(String chatId);

    /**
     * Retrieve all active group chats for auto-rejoin
     * 
     * @return Set of chat IDs of those group chats that has to be auto-rejoined
     */
    public Set<String> getChatIdsOfActiveGroupChatsForAutoRejoin();

    /**
     * Get group chat participants to be invited
     * 
     * @param chatId
     * @return Set of participants
     */
    public Set<ContactId> getGroupChatParticipantsToBeInvited(String chatId);

    /**
     * Checks if group chat is persisted
     * 
     * @param chatId
     * @return true if group chat is persisted
     */
    boolean isGroupChatPersisted(String chatId);
}
