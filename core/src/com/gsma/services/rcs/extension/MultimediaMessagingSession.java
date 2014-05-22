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
package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.JoynServiceException;

/**
 * This class maintains the information related to a multimedia
 * session for a real time messaging service. 
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaMessagingSession extends MultimediaSession {
    /**
     * Messaging session interface
     */
    private IMultimediaMessagingSession sessionIntf;

    /**
     * Constructor
     * 
     * @param sessionInf Multimedia session interface
     */
    MultimediaMessagingSession(IMultimediaMessagingSession sessionIntf) {
    	super();
    	
    	this.sessionIntf = sessionIntf;
    }
    
    /**
	 * Returns the session ID of the multimedia session
	 * 
	 * @return Session ID
	 * @throws JoynServiceException
	 */
	public String getSessionId() throws JoynServiceException {
		try {
			return sessionIntf.getSessionId();
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
			return sessionIntf.getRemoteContact();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the service ID
	 * 
	 * @return Service ID
	 * @throws JoynServiceException
	 */
	public String getServiceId() throws JoynServiceException {
		try {
			return sessionIntf.getServiceId();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the state of the session
	 * 
	 * @return State
	 * @see MultimediaSession.State
	 * @throws JoynServiceException
	 */
	public int getState() throws JoynServiceException {
		try {
			return sessionIntf.getState();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the direction of the session (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see MultimediaSession.Direction
	 * @throws JoynServiceException
	 */
	public int getDirection() throws JoynServiceException {
		try {
			return sessionIntf.getDirection();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}	
	
	/**
	 * Accepts session invitation.
	 * 
	 * @throws JoynServiceException
	 */
	public void acceptInvitation() throws JoynServiceException {
		try {
			sessionIntf.acceptInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects session invitation
	 * 
	 * @throws JoynServiceException
	 */
	public void rejectInvitation() throws JoynServiceException {
		try {
			sessionIntf.rejectInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Aborts the session
	 * 
	 * @throws JoynServiceException
	 */
	public void abortSession() throws JoynServiceException {
		try {
			sessionIntf.abortSession();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
    
    /**
     * Sends a message in real time
     * 
     * @param content Message content
	 * @return Returns true if sent successfully else returns false
     * @throws JoynServiceException
     */
    public boolean sendMessage(byte[] content) throws JoynServiceException {
		try {
			return sessionIntf.sendMessage(content);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
    }    

	/**
	 * Adds a listener on session events
	 * 
	 * @param listener Session event listener
	 * @throws JoynServiceException
	 */
	public void addEventListener(MultimediaMessagingSessionListener listener) throws JoynServiceException {
		try {
			sessionIntf.addEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Removes a listener on session events
	 * 
	 * @param listener Session event listener
	 * @throws JoynServiceException
	 */
	public void removeEventListener(MultimediaMessagingSessionListener listener) throws JoynServiceException {
		try {
			sessionIntf.removeEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
}
