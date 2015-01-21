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

/**
 * Intent for one-to-one chat conversation
 * 
 * @author Jean-Marc AUFFRET
 */
public class OneToOneChatIntent {

	/**
	 * Broadcast action: a new chat message has been received.
	 * <p>
	 * Intent includes the following extras:
	 * <ul>
	 * <li> {@link #EXTRA_MESSAGE_ID} containing the message ID of chat message.
	 * </ul>
	 */
	public final static String ACTION_NEW_ONE_TO_ONE_CHAT_MESSAGE = "com.gsma.services.rcs.chat.action.NEW_ONE_TO_ONE_CHAT_MESSAGE";

	/**
	 * MIME-type of received message
	 */
	public final static String EXTRA_MIME_TYPE = "mimeType";

	/**
	 * Message ID of received message
	 */
	public final static String EXTRA_MESSAGE_ID = "messageId";
}
