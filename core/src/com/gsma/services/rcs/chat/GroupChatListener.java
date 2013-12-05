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
package com.gsma.services.rcs.chat;


/**
 * Group chat event listener

 * @author Jean-Marc AUFFRET
 */
public abstract class GroupChatListener extends IGroupChatListener.Stub {
	/**
	 * Callback called when the session is well established and messages
	 * may be exchanged with the group of participants
	 */
	public abstract void onSessionStarted();
	
	/**
	 * Callback called when the session has been aborted or terminated
	 */
	public abstract void onSessionAborted();

	/**
	 * Callback called when the session has failed
	 * 
	 * @param error Error
	 * @see GroupChat.Error
	 */
	public abstract void onSessionError(int error);
	
	/**
	 * Callback called when a new message has been received
	 * 
	 * @param message New chat message
	 * @see ChatMessage
	 * @see GeolocMessage
	 */
	public abstract void onNewMessage(ChatMessage message);

	/**
	 * Callback called when a message has been delivered to the remote
	 * 
	 * @param msgId Message ID
	 */
	public abstract void onReportMessageDelivered(String msgId);

	/**
	 * Callback called when a message has been displayed by the remote
	 * 
	 * @param msgId Message ID
	 */
	public abstract void onReportMessageDisplayed(String msgId);
	
	/**
	 * Callback called when a message has failed to be delivered to the remote
	 * 
	 * @param msgId Message ID
	 */
	public abstract void onReportMessageFailed(String msgId);
	
	/**
	 * Callback called when an Is-composing event has been received. If the
	 * remote is typing a message the status is set to true, else it is false.
	 * 
	 * @param contact Contact
	 * @param status Is-composing status
	 */
	public abstract void onComposingEvent(String contact, boolean status);

	/**
	 * Callback called when a new participant has joined the group chat
	 *  
	 * @param contact Contact
	 * @param contactDisplayname Contact displayname
	 */
	public abstract void onParticipantJoined(String contact, String contactDisplayname);
	
	/**
	 * Callback called when a participant has left voluntary the group chat
	 *  
	 * @param contact Contact
	 */
	public abstract void onParticipantLeft(String contact);

	/**
	 * Callback called when a participant is disconnected from the group chat
	 * 
	 * @param contact Contact
	 */
	public abstract void onParticipantDisconnected(String contact);
}
