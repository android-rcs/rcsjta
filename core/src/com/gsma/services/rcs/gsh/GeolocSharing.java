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
package com.gsma.services.rcs.gsh;

import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Geoloc sharing
 * 
 * @author Jean-Marc AUFFRET
 */
public class GeolocSharing {

    /**
     * Geoloc sharing state
     */
    public static class State {
    	/**
    	 * Unknown state
    	 */
    	public final static int UNKNOWN = 0;

    	/**
    	 * Sharing invitation received
    	 */
    	public final static int INVITED = 1;
    	
    	/**
    	 * Sharing invitation sent
    	 */
    	public final static int INITIATED = 2;
    	
    	/**
    	 * Sharing is started
    	 */
    	public final static int STARTED = 3;
    	
    	/**
    	 * Sharing has been aborted
    	 */
    	public final static int ABORTED = 4;
    	
    	/**
    	 * Sharing has failed 
    	 */
    	public final static int FAILED = 5;

    	/**
    	 * Sharing has been terminated
    	 */
    	public final static int TERMINATED = 6;

    	/**
    	 * Sharing invitation was rejected
    	 */
    	public final static int REJECTED = 7;

    	/**
    	 * Call ringing
    	 */
    	public final static int RINGING = 8;

    	/**
    	 * Sharing has been accepted and is in the process of becoming started
    	 */
    	public final static int ACCEPTING = 9;

        private State() {
        }    	
    }

    public static class ReasonCode {
    	/**
    	 * No specific reason code specified.
    	 */
    	public final static int UNSPECIFIED = 0;

    	/**
    	 * Geolocation share is aborted by local user.
    	 */
    	public final static int ABORTED_BY_USER = 1;

    	/**
    	 * Geolocation share is aborted by remote user.
    	 */
    	public final static int ABORTED_BY_REMOTE = 2;

    	/**
    	 * Geolocation share is aborted by system.
    	 */
    	public final static int ABORTED_BY_SYSTEM = 3;

    	/**
    	 * Geolocation share is aborted because already taken by the secondary device.
    	 */
    	public final static int ABORTED_BY_SECONDARY_DEVICE = 4;

    	/**
    	 * Geolocation share invitation was rejected due to to many open sharing sessions.
    	 */
    	public final static int REJECTED_MAX_SHARING_SESSIONS = 5;

    	/**
    	 * Geolocation share invitation was rejected by local user.
    	 */
    	public final static int REJECTED_BY_USER = 6;

    	/**
    	 * Geolocation share invitation was rejected by remote.
    	 */
    	public final static int REJECTED_BY_REMOTE = 7;

    	/**
    	 * Geolocation share invitation was rejected due to time out.
    	 */
    	public final static int REJECTED_TIME_OUT = 8;

    	/**
    	 * Geolocation share initiation failed.
    	 */
    	public final static int FAILED_INITIATION = 9;

    	/**
    	 * Sharing of the geolocation has failed.
    	 */
    	public final static int FAILED_SHARING = 10;
    }
    
    /**
     * Geoloc sharing error
     */
    public static class Error {
    	/**
    	 * Sharing has failed
    	 */
    	public final static int SHARING_FAILED = 0;
    	
    	/**
    	 * Sharing invitation has been declined by remote
    	 */
    	public final static int INVITATION_DECLINED = 1;
    	
        private Error() {
        }    	
    }

    /**
     * Geoloc sharing interface
     */
    private IGeolocSharing sharingInf;
    
    /**
     * Constructor
     * 
     * @param sharingInf Geoloc sharing interface
     */
    GeolocSharing(IGeolocSharing sharingInf) {
    	this.sharingInf = sharingInf;
    }
    	
    /**
	 * Returns the sharing ID of the geoloc sharing
	 * 
	 * @return Sharing ID
	 * @throws JoynServiceException
	 */
	public String getSharingId() throws JoynServiceException {
		try {
			return sharingInf.getSharingId();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the remote contact identifier
	 * 
	 * @return ContactId
	 * @throws JoynServiceException
	 */
	public ContactId getRemoteContact() throws JoynServiceException {
		try {
			return sharingInf.getRemoteContact();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
     * Returns the geolocation info
     *
     * @return Geoloc object
	 * @throws JoynServiceException
	 * @see Geoloc
     */
	public Geoloc getGeoloc() throws JoynServiceException {
		try {
			return sharingInf.getGeoloc();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the state of the sharing
	 * 
	 * @return State
	 * @see GeolocSharing.State
	 * @throws JoynServiceException
	 */
	public int getState() throws JoynServiceException {
		try {
			return sharingInf.getState();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}		
		
	/**
	 * Returns the direction of the sharing (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see GeolocSharing.Direction
	 * @throws JoynServiceException
	 */
	public int getDirection() throws JoynServiceException {
		try {
			return sharingInf.getDirection();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}	
	
	/**
	 * Accepts geoloc sharing invitation
	 * 
	 * @throws JoynServiceException
	 */
	public void acceptInvitation() throws JoynServiceException {
		try {
			sharingInf.acceptInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects geoloc sharing invitation
	 * 
	 * @throws JoynServiceException
	 */
	public void rejectInvitation() throws JoynServiceException {
		try {
			sharingInf.rejectInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Aborts the sharing
	 * 
	 * @throws JoynServiceException
	 */
	public void abortSharing() throws JoynServiceException {
		try {
			sharingInf.abortSharing();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
}
