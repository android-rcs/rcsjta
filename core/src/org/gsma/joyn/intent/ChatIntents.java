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
package org.gsma.joyn.intent;

/**
 * Intents for chat service
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatIntents {
	/**
	 * Load the chat application to view a chat conversation. This
	 * Intent takes into parameter an URI on the chat conversation
	 * (i.e. content://chats/chat_ID). If no parameter found the main
	 * entry of the chat application is displayed.
	 */
	public static final String ACTION_VIEW_CHAT = "org.gsma.joyn.action.VIEW_CHAT";

	/**
	 * Load the chat application to start a new conversation with a
	 * given contact. This Intent takes into parameter a contact URI
	 * (i.e. content://contacts/people/contact_ID). If no parameter the
	 * main entry of the chat application is displayed.
	 */
	public static final String ACTION_INITIATE_CHAT = "org.gsma.joyn.action.INITIATE_CHAT";

	/**
	 * Load the group chat application. This Intent takes into parameter an
	 * URI on the group chat conversation (i.e. content://chats/chat_ID). If
	 * no parameter found the main entry of the group chat application is displayed.
	 */
	public static final String ACTION_VIEW_GROUP_CHAT = "org.gsma.joyn.action.VIEW_GROUP_CHAT";

	/**
	 * Load the group chat application to start a new conversation with a
	 * group of contacts. This Intent takes into parameter a list of contact
	 * URIs. If no parameter the main entry of the group chat application is displayed.
	 */
	public static final String ACTION_INITIATE_GROUP_CHAT = "org.gsma.joyn.action.INITIATE_GROUP_CHAT";

	private ChatIntents() {
    }    	
}
