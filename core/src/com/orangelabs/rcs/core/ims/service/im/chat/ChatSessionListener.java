/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.service.im.chat;

import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.service.api.client.messaging.GeolocMessage;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;

/**
 * Chat session listener
 * 
 * @author JM. Auffret
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
     * @param contact Contact
     * @param status Status
     */
    public void handleIsComposingEvent(String contact, boolean status);

    /**
     * New conference event
     * 
	 * @param contact Contact
	 * @param contactDisplayname Contact display name
     * @param state State associated to the contact
     */
    public void handleConferenceEvent(String contact, String contactDisplayname, String state);

    /**
     * New message delivery status
     * 
	 * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(String msgId, String status);
    
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
}
