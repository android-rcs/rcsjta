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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gsma.services.rcs.JoynContactFormatException;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferListener;
import com.gsma.services.rcs.ft.IFileTransfer;

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
    	 * Unknown state
    	 */
    	public final static int UNKNOWN = 0;

    	/**
    	 * Chat invitation received
    	 */
    	public final static int INVITED = 1;
    	
    	/**
    	 * Chat invitation sent
    	 */
    	public final static int INITIATED = 2;
    	
    	/**
    	 * Chat is started
    	 */
    	public final static int STARTED = 3;
    	
    	/**
    	 * Chat has been terminated
    	 */
    	public final static int TERMINATED = 4;
    	   	
    	/**
    	 * Chat has been aborted 
    	 */
    	public final static int ABORTED = 5;
    	
    	/**
    	 * Chat has been closed by the user. A user which has closed a
    	 * conversation voluntary can't rejoin it afterward.
    	 */
    	public final static int CLOSED_BY_USER = 6;

    	/**
    	 * Chat has failed 
    	 */
    	public final static int FAILED = 7;
    	
        private State() {
        }    	
    }
    
    /**
     * Direction of the group chat
     */
    public static class Direction {
        /**
         * Incoming chat
         */
        public static final int INCOMING = 0;
        
        /**
         * Outgoing chat
         */
        public static final int OUTGOING = 1;
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
    private IGroupChat chatInf;
    
    /**
     * Constructor
     * 
     * @param chatIntf Group chat interface
     */
    GroupChat(IGroupChat chatIntf) {
    	this.chatInf = chatIntf;
    }

    /**
     * Returns the chat ID
     * 
     * @return Chat ID
	 * @throws JoynServiceException
     */
	public String getChatId() throws JoynServiceException {
		try {
			return chatInf.getChatId();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the direction of the group chat (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see GroupChat.Direction
	 * @throws JoynServiceException
	 */
	public int getDirection() throws JoynServiceException {
		try {
			return chatInf.getDirection();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}	
	
	/**
	 * Returns the state of the group chat
	 * 
	 * @return State
	 * @see GroupChat.State
	 * @throws JoynServiceException
	 */
	public int getState() throws JoynServiceException {
		try {
			return chatInf.getState();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
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
	 * Returns the list of participants in the group conversation
	 * 
	 * @return List of participants
	 * @throws JoynServiceException
     * @see ParticipantInfo
	 */
	public List<ParticipantInfo> getParticipantInfo() throws JoynServiceException {
		try {
			return chatInf.getParticipantInfo();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the subject of the group chat
	 * 
	 * @return Subject
	 * @throws JoynServiceException
	 */
	public String getSubject() throws JoynServiceException {
		try {
			return chatInf.getSubject();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the list of connected participants. A participant is identified
	 * by its MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
	 * 
	 * @return List of participants
	 * @throws JoynServiceException
	 */
	public Set<String> getParticipants() throws JoynServiceException {
		try {
			return new HashSet<String>(chatInf.getParticipants());
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}		
	}
	
	/**
	 * Accepts chat invitation
	 *  
	 * @throws JoynServiceException
	 */
	public void acceptInvitation() throws JoynServiceException {
		try {
			chatInf.acceptInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects chat invitation
	 * 
	 * @throws JoynServiceException
	 */
	public void rejectInvitation() throws JoynServiceException {
		try {
			chatInf.rejectInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Sends a text message to the group
	 * 
	 * @param text Message
	 * @return Unique message ID or null in case of error
	 * @throws JoynServiceException
	 */
	public String sendMessage(String text) throws JoynServiceException {
		try {
			return chatInf.sendMessage(text);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}		
	}
	
	/**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc info
	 * @return Unique message ID or null in case of error
   	 * @throws JoynServiceException
     */
    public String sendGeoloc(Geoloc geoloc) throws JoynServiceException {
		try {
			return chatInf.sendGeoloc(geoloc);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}    	
    }	

    /**
     * Transfers a file to participants. The parameter filename contains the complete
     * path of the file to be transferred.
     * 
     * @param filename Filename to transfer
     * @param fileicon Filename of the file icon associated to the file to be transfered
     * @param listener File transfer event listener
     * @return File transfer
     * @throws JoynServiceException
	 * @throws JoynContactFormatException
     */
    public FileTransfer sendFile(String filename, String fileicon, FileTransferListener listener) throws JoynServiceException {
    	try {
			IFileTransfer ftIntf = chatInf.sendFile(filename, fileicon, listener);
			if (ftIntf != null) {
				return new FileTransfer(ftIntf);
			} else {
				return null;
			}
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		} 
	}	
    
	/**
	 * Sends an Is-composing event. The status is set to true when typing
	 * a message, else it is set to false.
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
	 * Adds participants to a group chat
	 * 
	 * @param participants List of participants
	 * @throws JoynServiceException
	 */
	public void addParticipants(Set<String> participants) throws JoynServiceException {
		try {
			chatInf.addParticipants(new ArrayList<String>(participants));
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}		
	}
	
	/**
	 * Returns the max number of participants in the group chat. This limit is
	 * read during the conference event subscription and overrides the provisioning
	 * parameter.
	 * 
	 * @return Number
	 * @throws JoynServiceException
	 */
	public int getMaxParticipants() throws JoynServiceException {
		try {
			return chatInf.getMaxParticipants();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}		
	}
	
	/**
	 * Quits a group chat conversation. The conversation will continue between
	 * other participants if there are enough participants.
	 * 
	 * @throws JoynServiceException
	 */
	public void quitConversation() throws JoynServiceException {
		try {
			chatInf.quitConversation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Adds a listener on chat events
	 * 
	 * @param listener Group chat event listener 
	 * @throws JoynServiceException
	 */
	public void addEventListener(GroupChatListener listener) throws JoynServiceException {
		try {
			chatInf.addEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}    	    			
	}
	
	/**
	 * Removes a listener on chat events
	 * 
	 * @param listener Group chat event listener 
	 * @throws JoynServiceException
	 */
	public void removeEventListener(GroupChatListener listener) throws JoynServiceException {
		try {
			chatInf.removeEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}    	    			
	}
}
