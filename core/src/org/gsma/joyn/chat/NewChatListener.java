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
package org.gsma.joyn.chat;

/**
 * New chat invitation event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class NewChatListener extends INewChatListener.Stub {
	/**
	 * Callback called when a new chat invitation has been received
	 * 
	 * @param contact Remote contact
	 * @param message Chat message
	 * @see ChatMessage
	 */
	public abstract void onNewSingleChat(String contact, ChatMessage message);
	
	/**
	 * Callback called when a new group chat invitation has been received
	 * 
	 * @param chatId Chat ID
	 */
	public abstract void onNewGroupChat(String chatId);
}
