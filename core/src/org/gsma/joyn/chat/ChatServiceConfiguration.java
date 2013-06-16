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
 * Chat service configuration
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatServiceConfiguration {
	/**
	 * Returns the IM Warn SF configuration. True if user should be informed when
	 * sending message to offline user.
	 * 
	 * @return Boolean
	 */
	public boolean isImWarnSf() {
		return true; // TODO
	}
	
	/**
	 * Returns the time after inactive chat session could be closed
	 * 
	 * @return Timeout in seconds
	 */
	public int getChatSessionTimeout() {
		return 300; //TODO
	}
	
	/**
	 * Returns maximum participants in group chat session
	 * 
	 * @return Number
	 */
	public int getGroupChatMaxParticipantsNumber() {
		return 10; // TODO
	}
	
	/**
	 * Return maximum single chat message’s length can have
	 * 
	 * @return Number of bytes
	 */
	public long getSingleChatMessageMaxLength() {
		return 255; // TODO
	}
	
	/**
	 * Returns the maximum single group chat message’s length can have
	 * 
	 * @return Number of bytes
	 */
	public long getGroupChatMessageMaxLength() {
		return 255; // TODO
	}
	
	/**
	 * Returns the max number of simultaneous single chats
	 * 
	 * @return Number
	 */
	public int getMaxSingleChats() {
		return 10; // TODO
	}
	
	/**
	 * Returns the max number of simultaneous group chats
	 * 
	 * @return Number
	 */
	public int getMaxGroupChats() {
		return 10; // TODO
	}
	
	/**
	 * Returns the SMS fallback configuration. True if SMS fallback procedure
	 * activated, else returns False.
	 * 
	 * @return Boolean
	 */
	public boolean isSmsFallback() {
		return true; // TODO
	}
	
	/**
	 * Returns True if auto accept mode activated for chat, else returns False.
	 * 
	 * @return Boolean
	 */
	public boolean isChatAutoAcceptMode() {
		return false; // TODO
	}
	
	/**
	 * Returns True if auto accept mode activated for group chat, else returns False.
	 * 
	 * @return Boolean
	 */
	public boolean isGroupChatAutoAcceptMode() {
		return false; // TODO
	}
	
	/**
	 * Activates or deactivates the displayed delivery report on received chat messages.
	 * 
	 * @param state State
	 */
	public void setDisplayedDeliveryReport(boolean state) {
		// TODO
	}
}
