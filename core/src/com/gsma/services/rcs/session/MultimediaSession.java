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
package com.gsma.services.rcs.session;

import com.gsma.services.rcs.JoynServiceException;

/**
 * This class maintains the information related to a multimedia
 * session and offers methods to manage it and to send messages
 * in real time. 
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaSession {
    /**
     * Multimedia session state
     */
    public static class State {
    	/**
    	 * Unknown state
    	 */
    	public final static int UNKNOWN = 0;

    	/**
    	 * Session invitation received
    	 */
    	public final static int INVITED = 1;
    	
    	/**
    	 * Session invitation sent
    	 */
    	public final static int INITIATED = 2;
    	
    	/**
    	 * Session is started
    	 */
    	public final static int STARTED = 3;
    	
    	/**
    	 * Session has been aborted or 
    	 */
    	public final static int ABORTED = 5;
    	
        /**
         * Session has been terminated
         */
        public static final int TERMINATED = 6;

        /**
    	 * Session has failed 
    	 */
    	public final static int FAILED = 7;
    	
        private State() {
        }    	
    }
    
    /**
     * Direction of the session
     */
    public static class Direction {
        /**
         * Incoming session
         */
        public static final int INCOMING = 0;
        
        /**
         * Outgoing session
         */
        public static final int OUTGOING = 1;
    }     
    
    /**
     * Session error
     */
    public static class Error {
    	/**
    	 * Session invitation has been declined by remote
    	 */
    	public final static int INVITATION_DECLINED = 0;

    	/**
    	 * Session has failed
    	 */
    	public final static int SESSION_FAILED = 1;

    	/**
    	 * Media has failed
    	 */
    	public final static int MEDIA_FAILED = 2;

    	private Error() {
        }    	
    }
    
    /**
     * Multimedia session interface
     */
    private IMultimediaSession sessionInf;

    /**
     * Constructor
     * 
     * @param sessionInf Multimedia session interface
     */
    MultimediaSession(IMultimediaSession sessionInf) {
    	this.sessionInf = sessionInf;
    }

    /**
	 * Returns the session ID of the multimedia session
	 * 
	 * @return Session ID
	 * @throws JoynServiceException
	 */
	public String getSessionId() throws JoynServiceException {
		try {
			return sessionInf.getSessionId();
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
			return sessionInf.getRemoteContact();
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
			return sessionInf.getServiceId();
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
			return sessionInf.getState();
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
			return sessionInf.getDirection();
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
			sessionInf.acceptInvitation();
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
			sessionInf.rejectInvitation();
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
			sessionInf.abortSession();
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
	public void addEventListener(MultimediaSessionListener listener) throws JoynServiceException {
		try {
			sessionInf.addEventListener(listener);
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
	public void removeEventListener(MultimediaSessionListener listener) throws JoynServiceException {
		try {
			sessionInf.removeEventListener(listener);
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
			return sessionInf.sendMessage(content);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
    }    
}
