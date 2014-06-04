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
 * session and offers methods to manage it. This is an abstract
 * class between a messaging session and a streaming session. 
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class MultimediaSession {
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
     * Constructor
     */
    MultimediaSession() {
    }
    
    /**
	 * Returns the session ID of the multimedia session
	 * 
	 * @return Session ID
	 * @throws JoynServiceException
	 */
	public abstract String getSessionId() throws JoynServiceException;
	
	/**
	 * Returns the remote contact
	 * 
	 * @return Contact
	 * @throws JoynServiceException
	 */
	public abstract String getRemoteContact() throws JoynServiceException;
	
	/**
	 * Returns the service ID
	 * 
	 * @return Service ID
	 * @throws JoynServiceException
	 */
	public abstract String getServiceId() throws JoynServiceException;
	
	/**
	 * Returns the state of the session
	 * 
	 * @return State
	 * @see MultimediaSession.State
	 * @throws JoynServiceException
	 */
	public abstract int getState() throws JoynServiceException;
	
	/**
	 * Returns the direction of the session (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see MultimediaSession.Direction
	 * @throws JoynServiceException
	 */
	public abstract int getDirection() throws JoynServiceException;	
	
	/**
	 * Accepts session invitation.
	 * 
	 * @throws JoynServiceException
	 */
	public abstract void acceptInvitation() throws JoynServiceException;
	
	/**
	 * Rejects session invitation
	 * 
	 * @throws JoynServiceException
	 */
	public abstract void rejectInvitation() throws JoynServiceException;
	
	/**
	 * Aborts the session
	 * 
	 * @throws JoynServiceException
	 */
	public abstract void abortSession() throws JoynServiceException;    
}
