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

package com.gsma.rcs.core.ims.service.im.chat;

import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Map;

/**
 * Group chat session listener
 */
public interface GroupChatSessionListener extends ChatSessionListener {

    /**
     * New conference event
     * 
     * @param contact Contact identifier
     * @param status ParticipantStatus for the contact
     * @param timestamp Local timestamp when got notification
     */
    public void handleConferenceEvent(ContactId contact, ParticipantStatus status, long timestamp);

    /**
     * A session invitation has been received
     * 
     * @param contact Remote contact
     * @param subject
     * @param participants Participants
     * @param timestamp Local timestamp when got invitation
     */
    public void handleSessionInvited(ContactId contact, String subject,
            Map<ContactId, GroupChat.ParticipantStatus> participants, long timestamp);

    /**
     * Chat is auto-accepted and the session is in the process of being started
     * 
     * @param contact Remote contact
     * @param subject
     * @param participants Participants
     * @param timestamp Local timestamp when got invitation
     */
    public void handleSessionAutoAccepted(ContactId contact, String subject,
            Map<ContactId, GroupChat.ParticipantStatus> participants, long timestamp);

    /**
     * One or several participants has been updated
     * 
     * @param updatedParticipants Updated participants
     * @param allParticipants All group participants
     */
    public void handleParticipantUpdates(Map<ContactId, ParticipantStatus> updatedParticipants,
            Map<ContactId, ParticipantStatus> allParticipants);
}
