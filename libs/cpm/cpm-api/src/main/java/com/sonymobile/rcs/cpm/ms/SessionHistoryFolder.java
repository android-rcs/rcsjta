/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.cpm.ms;

import java.util.Set;

/**
 * The name of a session history folder SHALL be the Contribution-ID of the corresponding CPM
 * Session. A session history folder SHALL contains:
 * <ul>
 * <li>One session info object, and
 * <li>Zero or more message object(s), and/or
 * <li>Zero or more file transfer object(s), and/or
 * <li>Zero or more stand-alone Media Object(s), and/or
 * <li>Zero or more group state objects
 * </ul>
 * <p>
 * The name of the session history folder MUST be unique within the scope of the parent Conversation
 * History Object.
 * </p>
 */
public interface SessionHistoryFolder extends CpmObjectFolder {

    /**
     * Convenience method that extracts the session info object (with all its headers) as a
     * "cpm object" Note that the details of the session are also exposed in this interface
     * 
     * @return
     */
    public SessionInfo getSessionInfoObject() throws CpmMessageStoreException;

    /**
     * Set to the address of the initiator of the CPM Session, retrieved from the authenticated
     * originator’s CPM Address in the SIP INVITE request.
     * 
     * @return the from header
     * @throws CpmMessageStoreException
     */
    public String getFrom() throws CpmMessageStoreException;

    /**
     * Returns the Subject header Optional
     * 
     * @return the subject
     */
    public String getSubject() throws CpmMessageStoreException;

    /**
     * Returns the contribution ID header
     * 
     * @return
     */
    public String getId();

    /**
     * Set to the InReplyTo -Contribution -ID of the SIP INVITE request. Optional
     * 
     * @return
     */
    public String getInReplyToContributionId();

    /**
     * Returns the parent Conversation history folder
     * 
     * @return the conversation
     */
    public ConversationHistoryFolder getConversation();

    // /**
    // *
    // * Creates a group state
    // *
    // * @param type @see GroupState
    // * @param timestamp the timestamp
    // * @param lastFocusSessionId the last focus session id
    // * @param participants the participant list
    // * @return the created group state
    // * @throws MessageStoreException
    // */
    // public CPMGroupState addGroupState(int type, Date timestamp, String lastFocusSessionId,
    // Participant... participants) throws CPMMessageStoreException;

    /**
     * @return all the group states for this session
     * @throws MessageStoreException
     */
    public Set<CpmGroupState> getGroupStates() throws CpmMessageStoreException;

}

/*
 * C.1 Header Definitions C.1.1 Conversation-ID A Conversation-ID header in a SIP MESSAGE request or
 * SIP INVITE request indicates the CPM Conversation Identity associated with a CPM Standalone
 * Message, CPM File Transfer, or CPM Session. A sending CPM functional component MUST include a
 * Conversation-ID header in each SIP MESSAGE request or SIP INVITE request that are associated with
 * a CPM Standalone Message, a CPM File Transfer, or a CPM Session. The sending CPM functional
 * component MUST ensure that the included CPM Conversation Identity is globally unique. Use of
 * cryptographically random identifiers ([RFC4086]) in the generation of Conversation-IDs is
 * RECOMMENDED. Conversation-IDs are case-sensitive and are simply compared byte-by-byte. Examples:
 * Conversation-ID: f81d4fae-7dec-11d0-a765-00a0c91e6bf6 C.1.2 Contribution-ID A Contribution-ID
 * header in a SIP MESSAGE request or SIP INVITE request indicates the CPM Contribution Identity
 * associated with a CPM Standalone Message, CPM File Transfer, or CPM Session. A sending CPM
 * functional component MUST include a Contribution-ID header in each SIP MESSAGE request or SIP
 * INVITE request that are associated with a CPM Standalone Message, a CPM File Transfer, or a CPM
 * Session. The sending CPM functional component MUST ensure that the included CPM Contribution
 * Identity is unique within the context of the encompassing CPM Conversation. The algorithm to be
 * used for creating a Contribution-ID SHALL follow the recommendations in [draft-session-id].
 * Example: Contribution-ID: abcdef-1234-5678-90ab-cdef01234567 C.1.3 InReplyTo-Contribution-ID An
 * InReplyTo-Contribution-ID header in a SIP MESSAGE request or SIP INVITE request indicates the CPM
 * Contribution Identity associated with a CPM Standalone Message, CPM File Transfer, or CPM
 * Session, to which this request is a reply to. When a CPM User replies to a CPM Standalone
 * Message, CPM File Transfer, or CPM Session, the replying CPM Client MUST include a
 * InReplyTo-Contribution-ID header in the SIP MESSAGE request or SIP INVITE request associated with
 * the reply CPM Standalone Message, the reply CPM File Transfer or the reply CPM Session, and
 * populate it with the CPM Contribution Identity of the CPM Standalone Message, CPM File Transfer
 * or CPM Session being replied to. Example: InReplyTo-Contribution-ID:
 * 01234567-89ab-cdef-0123-456789abcdef
 */
