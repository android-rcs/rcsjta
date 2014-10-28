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

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * This class maintains the information related to a multimedia
 * session for a real time streaming service. 
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaStreamingSession extends MultimediaSession {
    /**
     * Streaming session interface
     */
    private IMultimediaStreamingSession sessionIntf;

    /**
     * Constructor
     * 
     * @param sessionInf Multimedia session interface
     */
    MultimediaStreamingSession(IMultimediaStreamingSession sessionIntf) {
    	super();
    	
    	this.sessionIntf = sessionIntf;
    }
    
    /**
	 * Returns the session ID of the multimedia session
	 * 
	 * @return Session ID
	 * @throws RcsServiceException
	 */
	public String getSessionId() throws RcsServiceException {
		try {
			return sessionIntf.getSessionId();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the remote contact identifier
	 * 
	 * @return ContactId
	 * @throws RcsServiceException
	 */
	public ContactId getRemoteContact() throws RcsServiceException {
		try {
			return sessionIntf.getRemoteContact();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the service ID
	 * 
	 * @return Service ID
	 * @throws RcsServiceException
	 */
	public String getServiceId() throws RcsServiceException {
		try {
			return sessionIntf.getServiceId();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the state of the session
	 * 
	 * @return State
	 * @see MultimediaSession.State
	 * @throws RcsServiceException
	 */
	public int getState() throws RcsServiceException {
		try {
			return sessionIntf.getState();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the reason code of the state of the session
	 *
	 * @return ReasonCode
	 * @see MultimediaSession.ReasonCode
	 * @throws RcsServiceException
	 */
	public int getReasonCode() throws RcsServiceException {
		try {
			return sessionIntf.getReasonCode();
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the direction of the session (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see MultimediaSession.Direction
	 * @throws RcsServiceException
	 */
	public int getDirection() throws RcsServiceException {
		try {
			return sessionIntf.getDirection();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}	
	
	/**
	 * Accepts session invitation.
	 * 
	 * @throws RcsServiceException
	 */
	public void acceptInvitation() throws RcsServiceException {
		try {
			sessionIntf.acceptInvitation();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects session invitation
	 * 
	 * @throws RcsServiceException
	 */
	public void rejectInvitation() throws RcsServiceException {
		try {
			sessionIntf.rejectInvitation();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
	
	/**
	 * Aborts the session
	 * 
	 * @throws RcsServiceException
	 */
	public void abortSession() throws RcsServiceException {
		try {
			sessionIntf.abortSession();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Sends a payload in real time
	 * 
	 * @param content Payload content
	 * @throws RcsServiceException
	 */
	public void sendPayload(byte[] content) throws RcsServiceException {
		try {
			sessionIntf.sendPayload(content);
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
}
