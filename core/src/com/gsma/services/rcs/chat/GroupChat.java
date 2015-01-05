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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Group chat
 * 
 * @author Jean-Marc AUFFRET
 */
public class GroupChat {
    /**
     * Group chat state
     */
    public static class State {
    	/**
    	 * Chat invitation received
    	 */
    	public final static int INVITED = 0;
    	
    	/**
    	 * Chat invitation sent
    	 */
    	public final static int INITIATING = 1;
    	
    	/**
    	 * Chat is started
    	 */
    	public final static int STARTED = 2;
    	   	
    	/**
    	 * Chat has been aborted 
    	 */
    	public final static int ABORTED = 3;

    	/**
    	 * Chat has failed 
    	 */
    	public final static int FAILED = 4;

    	/**
    	 * Chat has been accepted and is in the process of becoming started.
    	 */
    	public final static int ACCEPTING = 5;

    	/**
    	 * Chat invitation was rejected.
    	 */
    	public final static int REJECTED = 6;
    	
        private State() {
        }    	
    }
    
    /**
     * Group chat state reason code
     */
    public static class ReasonCode {

        /**
         * No specific reason code specified.
         */
        public final static int UNSPECIFIED = 0;

        /**
         * Group chat is aborted by local user.
         */
        public final static int ABORTED_BY_USER = 1;

        /**
         * Group chat is aborted by remote user.
         */

        public final static int ABORTED_BY_REMOTE = 2;

        /**
         * Group chat is aborted by system.
         */
        public final static int ABORTED_BY_SYSTEM = 3;

        /**
         * Group chat is rejected because already taken by the secondary device.
         */
        public final static int REJECTED_BY_SECONDARY_DEVICE = 4;

        /**
         * Group chat invitation was rejected as it was detected as spam.
         */
        public final static int REJECTED_SPAM = 5;

        /**
         * Group chat invitation was rejected due to max number of chats open already.
         */
        public final static int REJECTED_MAX_CHATS = 6;

        /**
         * Group chat invitation was rejected by local user.
         */
        public final static int REJECTED_BY_USER = 7;

        /**
         * Group chat invitation was rejected by remote.
         */
        public final static int REJECTED_BY_REMOTE = 8;

        /**
         * Group chat invitation was rejected due to time out.
         */
        public final static int REJECTED_TIME_OUT = 9;

        /**
         * Group chat initiation failed.
         */
        public final static int FAILED_INITIATION = 10;
    }
    
    /**
     * Group chat error
     */
    public static class Error {
    	/**
    	 * Group chat has failed
    	 */
    	public final static int CHAT_FAILED = 0;
    	
    	/**
    	 * Group chat invitation has been declined by remote
    	 */
    	public final static int INVITATION_DECLINED = 1;

    	/**
    	 * Chat conversation not found
    	 */
    	public final static int CHAT_NOT_FOUND = 2;
    	    	
        private Error() {
        }    	
    }

    /**
     * Group chat interface
     */
    private final IGroupChat mGroupChatInf;
    
    /**
     * Constructor
     * 
     * @param chatIntf Group chat interface
     */
    /* package private */GroupChat(IGroupChat chatIntf) {
    	mGroupChatInf = chatIntf;
    }

    /**
     * Returns the chat ID
     * 
     * @return Chat ID
     * @throws RcsServiceException
     */
	public String getChatId() throws RcsServiceException {
		try {
			return mGroupChatInf.getChatId();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the direction of the group chat (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see com.gsma.services.rcs.RcsCommon.Direction
	 * @throws RcsServiceException
	 */
	public int getDirection() throws RcsServiceException {
		try {
			return mGroupChatInf.getDirection();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}	
	
	/**
	 * Returns the state of the group chat
	 * 
	 * @return State
	 * @see GroupChat.State
	 * @throws RcsServiceException
	 */
	public int getState() throws RcsServiceException {
		try {
			return mGroupChatInf.getState();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}		

	/**
	 * Returns the reason code of the state of the group chat
	 *
	 * @return ReasonCode
	 * @see GroupChat.ReasonCode
	 * @throws RcsServiceException
	 */
	public int getReasonCode() throws RcsServiceException {
		try {
			return mGroupChatInf.getReasonCode();
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	
	/**
	 * Returns the remote contact
	 * 
	 * @return Contact
	 * @throws RcsServiceException
	 */
	public ContactId getRemoteContact() throws RcsServiceException {
		try {
			return mGroupChatInf.getRemoteContact();
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the subject of the group chat
	 * 
	 * @return Subject
	 * @throws RcsServiceException
	 */
	public String getSubject() throws RcsServiceException {
		try {
			return mGroupChatInf.getSubject();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the list of connected participants. A participant is identified
	 * by its MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
	 * 
	 * @return List of participants
	 * @throws RcsServiceException
	 */
	public Set<ParticipantInfo> getParticipants() throws RcsServiceException {
		try {
			return new HashSet<ParticipantInfo>(mGroupChatInf.getParticipants());
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}		
	}

	/**
	 * Sends a text message to the group
	 * 
	 * @param text Message
	 * @return ChatMessage
	 * @throws RcsServiceException
	 */
	public ChatMessage sendMessage(String text) throws RcsServiceException {
		try {
			return new ChatMessage(mGroupChatInf.sendMessage(text));
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}		
	}
	
	/**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc info
     * @return ChatMessage
     * @throws RcsServiceException
     */
    public ChatMessage sendMessage(Geoloc geoloc) throws RcsServiceException {
		try {
			return new ChatMessage(mGroupChatInf.sendMessage2(geoloc));
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}    	
    }	

	/**
	 * Sends an Is-composing event. The status is set to true when typing
	 * a message, else it is set to false.
	 * 
	 * @param status Is-composing status
	 * @throws RcsServiceException
	 */
	public void sendIsComposingEvent(boolean status) throws RcsServiceException {
		try {
			mGroupChatInf.sendIsComposingEvent(status);
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}		
	}
	
	/**
	 * Adds participants to a group chat
	 * 
	 * @param participants List of participants
	 * @throws RcsServiceException
	 */
	public void addParticipants(Set<ContactId> participants) throws RcsServiceException {
		try {
			mGroupChatInf.addParticipants(new ArrayList<ContactId>(participants));
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}		
	}
	
	/**
	 * Returns the max number of participants in the group chat. This limit is
	 * read during the conference event subscription and overrides the provisioning
	 * parameter.
	 * 
	 * @return Number
	 * @throws RcsServiceException
	 */
	public int getMaxParticipants() throws RcsServiceException {
		try {
			return mGroupChatInf.getMaxParticipants();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}		
	}
	
	/**
	 * Leaves a group chat willingly and permanently. The group chat will
	 * continue between other participants if there are enough participants.
	 * 
	 * @throws RcsServiceException
	 */
	public void leave() throws RcsServiceException {
		try {
			mGroupChatInf.leave();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * open the chat conversation.<br>
	 * Note: if it is an incoming pending chat session and the parameter IM SESSION START is 0 then the session is accepted now.
	 * 
	 * @throws RcsServiceException
	 */
	public void openChat() throws RcsServiceException {
		try {
			mGroupChatInf.openChat();
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
}
