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

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Geoloc sharing
 * 
 * @author Jean-Marc AUFFRET
 */
/**
 * @author yplo6403
 *
 */
public class GeolocSharing {

    /**
     * Geoloc sharing state
     */
    public static class State {
    	/**
    	 * Sharing invitation received
    	 */
    	public final static int INVITED = 0;
    	
    	/**
    	 * Sharing invitation sent
    	 */
    	public final static int INITIATING = 1;
    	
    	/**
    	 * Sharing is started
    	 */
    	public final static int STARTED = 2;
    	
    	/**
    	 * Sharing has been aborted
    	 */
    	public final static int ABORTED = 3;
    	
    	/**
    	 * Sharing has failed 
    	 */
    	public final static int FAILED = 4;

    	/**
    	 * Sharing has been transferred
    	 */
    	public final static int TRANSFERRED = 5;

    	/**
    	 * Sharing invitation was rejected
    	 */
    	public final static int REJECTED = 6;

    	/**
    	 * Call ringing
    	 */
    	public final static int RINGING = 7;

    	/**
    	 * Sharing has been accepted and is in the process of becoming started
    	 */
    	public final static int ACCEPTING = 8;

        private State() {
        }    	
    }

    /**
     * Reason code
     *
     */
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
    	 * Geolocation share is rejected because already taken by the secondary device.
    	 */
    	public final static int REJECTED_BY_SECONDARY_DEVICE = 4;

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
    private IGeolocSharing mSharingInf;
    
    /**
     * Constructor
     * 
     * @param sharingInf Geoloc sharing interface
     */
    /* package private */GeolocSharing(IGeolocSharing sharingInf) {
    	mSharingInf = sharingInf;
    }
    	
    /**
	 * Returns the sharing ID of the geoloc sharing
	 * 
	 * @return Sharing ID
	 * @throws RcsServiceException
	 */
	public String getSharingId() throws RcsServiceException {
		try {
			return mSharingInf.getSharingId();
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
			return mSharingInf.getRemoteContact();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the geolocation info
	 * 
	 * @return Geoloc object
	 * @throws RcsServiceException
	 * @see Geoloc
	 */
	public Geoloc getGeoloc() throws RcsServiceException {
		try {
			return mSharingInf.getGeoloc();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the state of the sharing
	 * 
	 * @return State
	 * @see GeolocSharing.State
	 * @throws RcsServiceException
	 */
	public int getState() throws RcsServiceException {
		try {
			return mSharingInf.getState();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}		

	/**
	 * Returns the reason code of the state of the sharing
	 *
	 * @return ReasonCode
	 * @see GeolocSharing.ReasonCode
	 * @throws RcsServiceException
	 */
	public int getReasonCode() throws RcsServiceException {
		try {
			return mSharingInf.getReasonCode();
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the direction of the sharing (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see Direction
	 * @throws RcsServiceException
	 */
	public int getDirection() throws RcsServiceException {
		try {
			return mSharingInf.getDirection();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}	
	
	/**
	 * Accepts geoloc sharing invitation
	 * 
	 * @throws RcsServiceException
	 */
	public void acceptInvitation() throws RcsServiceException {
		try {
			mSharingInf.acceptInvitation();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects geoloc sharing invitation
	 * 
	 * @throws RcsServiceException
	 */
	public void rejectInvitation() throws RcsServiceException {
		try {
			mSharingInf.rejectInvitation();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Aborts the sharing
	 * 
	 * @throws RcsServiceException
	 */
	public void abortSharing() throws RcsServiceException {
		try {
			mSharingInf.abortSharing();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
}
