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

import org.gsma.joyn.JoynServiceException;

/**
 * Chat
 * 
 * @author Jean-Marc AUFFRET
 */
public class Chat {
    /**
     * Chat interface
     */
    private IChat chatInf;
    
    /**
     * Constructor
     * 
     * @param chatIntf Chat interface
     */
    Chat(IChat chatIntf) {
    	this.chatInf = chatIntf;
    }

    /**
     * Returns the remote contact
     * 
     * @return Contact
	 * @throws JoynServiceException
     */
    public String getRemoteContact() throws JoynServiceException {
		try {
			return chatInf.getRemoteContact();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
    }
    
	/**
     * Sends a chat message
     * 
     * @param message Message
     * @return Unique message ID
   	 * @throws JoynServiceException
     */
    public String sendMessage(String message) throws JoynServiceException {
		try {
			String msgId = chatInf.sendMessage(message);
			if (msgId == null) {
				throw new JoynServiceException("Send message has failed");
			}
			return msgId;
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}    	
    }
	
    /**
     * Sends a displayed delivery report for a given message ID
     * 
     * @param msgId Message ID
	 * @throws JoynServiceException
     */
    public void sendDisplayedDeliveryReport(String msgId) throws JoynServiceException {
		try {
			chatInf.sendDisplayedDeliveryReport(msgId);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}    	
    }
	
    /**
     * Sends an Is-composing event. The status is set to true when
     * typing a message, else it is set to false.
     * 
     * @param status Is-composing status
	 * @throws JoynServiceException
     */
    public void sendIsComposingEvent(boolean status) throws JoynServiceException {
		try {
			chatInf.sendIsComposingEvent(status);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}    	
    }
	
    /**
     * Adds a listener on chat events
     *  
     * @param listener Chat event listener
	 * @throws JoynServiceException
     */
    public void addEventListener(ChatListener listener) throws JoynServiceException {
		try {
			chatInf.addEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}    	
    }
	
    /**
     * Removes a listener on chat events
     * 
     * @param listener Chat event listener
	 * @throws JoynServiceException
     */
    public void removeEventListener(ChatListener listener) throws JoynServiceException {
		try {
			chatInf.removeEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}    	    	
    }
}
