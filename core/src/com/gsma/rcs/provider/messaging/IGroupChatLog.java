/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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
    void addGroupChat(String chatId, ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, State state, ReasonCode reasonCode,
            Direction direction, long timestamp);

    /**
     * Accept next Group Chat invitation
     * 
     * @param chatId Chat ID of the group chat
     */
    void acceptGroupChatNextInvitation(String chatId);

    /**
     * Set group chat state and reason code
     * 
     * @param chatId Chat ID
     * @param state Group chat state
     * @param reasonCode Group chat state reason code
     * @return True if an entry was updated, otherwise false
     */
    boolean setGroupChatStateAndReasonCode(String chatId, State state, ReasonCode reasonCode);

    /**
     * Set group chat participants, state and reason code
     * 
     * @param chatId Chat ID
     * @param participants map of participants and associated status
     * @param state Group chat state
     * @param reasonCode Group chat state reason code
     * @return True if an entry was updated, otherwise false
     */
    boolean setGroupChatParticipantsStateAndReasonCode(String chatId,
            Map<ContactId, ParticipantStatus> participants, State state, ReasonCode reasonCode);

    /**
     * Set group chat participants
     * 
     * @param chatId Chat ID
     * @param participants map of participants and associated status
     * @return True if an entry was updated, otherwise false
     */
    boolean setGroupChatParticipants(String chatId, Map<ContactId, ParticipantStatus> participants);

    /**
     * Set group chat rejoin ID
     * 
     * @param chatId Chat ID
     * @param rejoinId Rejoin ID
     * @param updateStateToStarted True if session state must be updated to started
     * @return True if an entry was updated, otherwise false
     */
    boolean setGroupChatRejoinId(String chatId, String rejoinId, boolean updateStateToStarted);

    /**
     * Get the group chat info
     * 
     * @param chatId Chat ID
     * @return Group chat info
     */
    GroupChatInfo getGroupChatInfo(String chatId);

    /**
     * Is next group chat Invitation rejected
     * 
     * @param chatId Chat ID
     * @return true if next GC invitation should be rejected
     */
    boolean isGroupChatNextInviteRejected(String chatId);

    /**
     * Set reject the next group chat invitation
     * 
     * @param chatId Chat ID
     * @return True if an entry was updated, otherwise false
     */
    boolean setRejectNextGroupChatNextInvitation(String chatId);

    /**
     * Get group chat state from its chat ID
     * 
     * @param chatId Chat ID of the group chat
     * @return State
     */
    State getGroupChatState(String chatId);

    /**
     * Get group chat state reason code from its chat ID
     * 
     * @param chatId Chat ID of the group chat
     * @return Reason code of the state
     */
    ReasonCode getGroupChatReasonCode(String chatId);

    /**
     * Get group chat participants from its chat ID
     * 
     * @param chatId Chat ID of the group chat
     * @return all group chat participants
     */
    Map<ContactId, ParticipantStatus> getParticipants(String chatId);

    /**
     * Get group chat participants from its chat ID
     * 
     * @param chatId Chat ID of the group chat
     * @param statuses participant status to match
     * @return all group chat participants matching any of the specified participant statuses
     */
    Map<ContactId, ParticipantStatus> getParticipants(String chatId, Set<ParticipantStatus> statuses);

    /**
     * Get group chat data from its chat ID
     * 
     * @param chatId Chat ID of the group chat
     * @return Cursor or null if no data exists
     */
    Cursor getGroupChatData(String chatId);

    /**
     * Retrieve all active group chats for auto-rejoin
     * 
     * @return Set of chat IDs of those group chats that has to be auto-rejoined
     */
    Set<String> getChatIdsOfActiveGroupChatsForAutoRejoin();

    /**
     * Checks if group chat is persisted
     * 
     * @param chatId Chat ID of the group chat
     * @return true if group chat is persisted
     */
    boolean isGroupChatPersisted(String chatId);
}
