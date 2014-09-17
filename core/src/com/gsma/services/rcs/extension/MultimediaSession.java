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
package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.contacts.ContactId;

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
    	 * Session is in unknown state
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
    	public final static int ABORTED = 4;
    	
        /**
         * Session has been terminated
         */
        public static final int TERMINATED = 5;

        /**
    	 * Session has failed 
    	 */
    	public final static int FAILED = 6;

        /**
    	 * Session has been rejected.
    	 */
    	public final static int REJECTED = 7;

        /**
    	 * Call ringing
    	 */
    	public final static int RINGING = 8;

    	/**
    	 * Session has been accepted and is in the process of becoming started
    	 */
    	public final static int ACCEPTING = 9;

        private State() {
        }    	
    }

    /**
     * Multimedia session reason code
     */
    public static class ReasonCode {

        /**
         * No specific reason code specified.
         */
        public final static int UNSPECIFIED = 0;

        /**
         * Session invitation was rejected due to time out.
         */
        public final static int REJECTED_TIME_OUT = 1;

        /**
         * Session invitation was rejected by local user.
         */
        public final static int REJECTED_BY_USER = 2;

        /**
         * Session invitation was rejected by remote.
         */
        public final static int REJECTED_BY_REMOTE = 3;

        /**
         * Session failed.
         */
        public final static int FAILED_SESSION = 4;

        /**
         * Media failed.
         */
        public final static int FAILED_MEDIA = 5;
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
	 * Returns the remote contact identifier
	 * 
	 * @return ContactId
	 * @throws JoynServiceException
	 */
	public abstract ContactId getRemoteContact() throws JoynServiceException;
	
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
