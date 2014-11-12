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
 * One-to-One Chat event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class OneToOneChatListener extends IOneToOneChatListener.Stub {

	/**
	 * Callback called when a message status/reasonCode is changed.
	 * 
	 * @param contact Contact ID
	 * @param msgId Message Id
	 * @param status Status
	 * @param reasonCode Reason code
	 */
	public abstract void onMessageStatusChanged(ContactId contact, String msgId, int status,
			int reasonCode);

	/**
	 * Callback called when an Is-composing event has been received. If the
	 * remote is typing a message the status is set to true, else it is false.
	 * 
	 * @param contact Contact ID
	 * @param status Is-composing status
	 */
	public abstract void onComposingEvent(ContactId contact, boolean status);
}
