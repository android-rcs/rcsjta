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

package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * One-to-One Chat
 * 
 * @author Jean-Marc AUFFRET
 */
public class OneToOneChat {

	/**
	 * Chat interface
	 */
	private IOneToOneChat chatInf;

	/**
	 * Constructor
	 * 
	 * @param chatIntf Chat interface
	 */
	OneToOneChat(IOneToOneChat chatIntf) {
		this.chatInf = chatIntf;
	}

	/**
	 * Returns the remote contact
	 * 
	 * @return ContactId
	 * @throws RcsServiceException
	 */
	public ContactId getRemoteContact() throws RcsServiceException {
		try {
			return chatInf.getRemoteContact();
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Sends a chat message
	 * 
	 * @param message Message
	 * @return Chat message
	 * @throws RcsServiceException
	 */
	public ChatMessage sendMessage(String message) throws RcsServiceException {
		try {
			return chatInf.sendMessage(message);
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Sends a geoloc message
	 * 
	 * @param geoloc Geoloc info
	 * @return Geoloc message
	 * @throws RcsServiceException
	 */
	public GeolocMessage sendMessage(Geoloc geoloc) throws RcsServiceException {
		try {
			return chatInf.sendMessage2(geoloc);
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Sends an Is-composing event. The status is set to true when typing a
	 * message, else it is set to false.
	 * 
	 * @param status Is-composing status
	 * @throws RcsServiceException
	 */
	public void sendIsComposingEvent(boolean status) throws RcsServiceException {
		try {
			chatInf.sendIsComposingEvent(status);
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
}
