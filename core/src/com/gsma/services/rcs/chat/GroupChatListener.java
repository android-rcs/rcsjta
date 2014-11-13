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

import com.gsma.services.rcs.contacts.ContactId;


/**
 * Group chat event listener

 * @author Jean-Marc AUFFRET
 */
public abstract class GroupChatListener extends IGroupChatListener.Stub {
	/**
	 * Callback called when the group chat state is changed
	 *
	 * @param chatId chat id
	 * @param state group chat state
	 * @param reasonCode reason code
	 */
	public abstract void onStateChanged(String chatId, int state, int reasonCode);

	/**
	 * Callback called when an Is-composing event has been received. If the
	 * remote is typing a message the status is set to true, else it is false.
	 * 
	 * @param chatId
	 * @param contact Contact ID
	 * @param status Is-composing status
	 */
	public abstract void onComposingEvent(String chatId, ContactId contact, boolean status);

	/**
	 * Callback called when a message status/reasonCode is changed.
	 *
	 * @param chatId chat id
	 * @param msgId message id
	 * @param status message status
	 * @param reasonCode reason code
	 */
	public abstract void onMessageStatusChanged(String chatId, String msgId, int status,
			int reasonCode);

	/**
	 * Callback called when a group delivery info status/reasonCode was changed for a single recipient to a group message.
	 *
	 * @param chatId chat id
	 * @param contact contact
	 * @param msgId message id
	 * @param status message status
	 * @param reasonCode status reason code
	 */
	public abstract void onMessageGroupDeliveryInfoChanged(String chatId, ContactId contact, String msgId, int status, int reasonCode);

	/**
	 * Callback called when a participant status has been changed in a group chat.
	 *
	 * @param chatId chat id
	 * @param info participant info
	 */
	public abstract void onParticipantInfoChanged(String chatId, ParticipantInfo info);
}
