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

package com.orangelabs.rcs.core.ims.service.im.chat;

import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;

/**
 * Chat session listener
 * 
 * @author Jean-Marc AUFFRET
 */
public interface ChatSessionListener extends ImsSessionListener {
	/**
	 * New message received
	 * 
	 * @param message Message
	 */
    public void handleReceiveMessage(InstantMessage message);
    
    /**
     * IM error
     * 
     * @param error Error
     */
    public void handleImError(ChatError error);
    
    /**
     * Is composing event
     * 
     * @param contactId Contact identifier
     * @param status Status
     */
    public void handleIsComposingEvent(ContactId contactId, boolean status);

    /**
     * New conference event
     * 
	 * @param contactId Contact identifier
	 * @param contactDisplayname Contact display name
     * @param state State associated to the contact
     */
    public void handleConferenceEvent(ContactId contactId, String contactDisplayname, String state);

    /**
     * New message failure status notifying the failure of sending
     *
     * @param msgId Message ID
     */
    public void handleSendMessageFailure(String msgId);

    /**
     * New message delivery status that are received as part of imdn notification
     * 
	 * @param msgId Message ID
     * @param status Delivery status
     * @param contactId the remote contact identifier
     */
    public void handleMessageDeliveryStatus(String msgId, String status, ContactId contactId);
    
    /**
     * Request to add participant is successful
     */
    public void handleAddParticipantSuccessful();
    
    /**
     * Request to add participant has failed
     * 
     * @param reason Error reason
     */
    public void handleAddParticipantFailed(String reason);
    
    /**
     * New geoloc message received
     * 
     * @param geoloc Geoloc message
     */
    public void handleReceiveGeoloc(GeolocMessage geoloc);
    
	/**
	 * Participant status changed
	 * 
	 * @param participantInfo
	 *            the participant information
	 */
    public void handleParticipantStatusChanged(ParticipantInfo participantInfo);
}
