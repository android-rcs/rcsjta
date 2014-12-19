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
package com.gsma.services.rcs.ipcall;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * IP call
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCall {

    /**
     * IP call state
     */
    public static class State {
    	/**
    	 * Call invitation received
    	 */
    	public final static int INVITED = 0;
    	
    	/**
    	 * Call invitation sent
    	 */
    	public final static int INITIATED = 1;
    	
    	/**
    	 * Call is started
    	 */
    	public final static int STARTED = 2;
    	
    	/**
    	 * call has been aborted
    	 */
    	public final static int ABORTED = 3;

    	/**
    	 * Call has failed
    	 */
    	public final static int FAILED = 4;

    	/**
    	 * Call rejected
    	 */
    	public final static int REJECTED = 5;

    	/**
    	 * Call on hold
    	 */
    	public final static int HOLD = 6;

    	/**
    	 * Call has been accepted and is in the process of becoming started
    	 */
    	public final static int ACCEPTING = 7;

    	/**
    	 * Call ringing
    	 */
    	public final static int RINGING = 8;

    	private State() {
        }    	
    }
    
    /**
     * Reason code associated with the ip call state.
     */
    public static class ReasonCode {
        /**
         * No specific reason code specified.
         */
        public static final int UNSPECIFIED = 0;

        /**
         * IP call share is aborted by local user.
         */
        public static final int ABORTED_BY_USER = 1;

        /**
         * IP call share is aborted by remote user.
         */
        public static final int ABORTED_BY_REMOTE = 2;

        /**
         * IP call is aborted by system.
         */
        public static final int ABORTED_BY_SYSTEM = 3;

        /**
         * IP call is rejected because already taken by the secondary device.
         */
        public static final int REJECTED_BY_SECONDARY_DEVICE = 4;

        /**
         * IP call invitation was rejected due to max number of sessions reached.
         */
        public static final int REJECTED_MAX_SESSIONS = 5;

        /**
         * IP call invitation was rejected by local user.
         */
        public static final int REJECTED_BY_USER = 6;

        /**
         * IP call invitation was rejected by remote.
         */
        public static final int REJECTED_BY_REMOTE = 7;

        /**
         * IP call has been rejected due to time out.
         */
        public static final int REJECTED_TIME_OUT = 8;

        /**
         * IP call initiation failed.
         */
        public static final int FAILED_INITIATION = 9;

        /**
         * IP call failed.
         */
        public static final int FAILED_IPCALL = 10;
    }
    
    /**
     * Call error
     */
    public static class Error {
    	/**
    	 * Call has failed
    	 */
    	public final static int CALL_FAILED = 0;
    	
    	/**
    	 * Call invitation has been declined by remote
    	 */
    	public final static int INVITATION_DECLINED = 1;
    	
        private Error() {
        }    	
    }

    /**
     * IP call interface
     */
    private IIPCall callInf;
    
    /**
     * Constructor
     * 
     * @param callInf IP call interface
     */
    /* package private */IPCall(IIPCall callInf) {
    	this.callInf = callInf;
    }
    	
    /**
	 * Returns the call ID of call
	 * 
	 * @return Call ID
	 * @throws RcsServiceException
	 */
	public String getCallId() throws RcsServiceException {
		try {
			return callInf.getCallId();
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
			return callInf.getRemoteContact();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the state of the call
	 * 
	 * @return State
	 * @see IPCall.State
	 * @throws RcsServiceException
	 */
	public int getState() throws RcsServiceException {
		try {
			return callInf.getState();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}		

	/**
	 * Returns the reason code of the state of the call
	 *
	 * @return ReasonCode
	 * @see IPCall.ReasonCode
	 * @throws RcsServiceException
	 */
	public int getReasonCode() throws RcsServiceException {
		try {
			return callInf.getReasonCode();
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the direction of the call (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see com.gsma.services.rcs.RcsCommon.Direction
	 * @throws RcsServiceException
	 */
	public int getDirection() throws RcsServiceException {
		try {
			return callInf.getDirection();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}	
	
	/**
	 * Accepts call invitation
	 * 
	 * @param player IP call player
	 * @param renderer IP call renderer
	 * @throws RcsServiceException
	 */
	public void acceptInvitation(IPCallPlayer player, IPCallRenderer renderer) throws RcsServiceException {
		try {
			callInf.acceptInvitation(player, renderer);
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects call invitation
	 * 
	 * @throws RcsServiceException
	 */
	public void rejectInvitation() throws RcsServiceException {
		try {
			callInf.rejectInvitation();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Aborts the call
	 * 
	 * @throws RcsServiceException
	 */
	public void abortCall() throws RcsServiceException {
		try {
			callInf.abortCall();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Is video activated
	 * 
	 * @return Boolean
	 * @throws RcsServiceException
	 */
	public boolean isVideo() throws RcsServiceException {
		try {
			return callInf.isVideo();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Add video stream
	 * 
	 * @throws RcsServiceException
	 */
	public void addVideo() throws RcsServiceException {
		try {
			callInf.addVideo();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Remove video stream
	 * 
	 * @throws RcsServiceException
	 */
	public void removeVideo() throws RcsServiceException {
		try {
			callInf.removeVideo();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Is call on hold
	 * 
	 * @return Boolean
	 * @throws RcsServiceException
	 */
	public boolean isOnHold() throws RcsServiceException {
		try {
			return callInf.isOnHold();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Puts the call on hold
	 * 
	 * @throws RcsServiceException
	 */
	public void holdCall() throws RcsServiceException {
		try {
			callInf.holdCall();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Continues the call that hold's on
	 * 
	 * @throws RcsServiceException
	 */
	public void continueCall() throws RcsServiceException {
		try {
			callInf.continueCall();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the video codec used during sharing
	 *
	 * @return VideoCodec
	 * @throws RcsServiceException
	 */
	public VideoCodec getVideoCodec() throws RcsServiceException {
		try {
			return callInf.getVideoCodec();
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the audio codec used during sharing
	 *
	 * @return AudioCodec
	 * @throws RcsServiceException
	 */
	public AudioCodec getAudioCodec() throws RcsServiceException {
		try {
			return callInf.getAudioCodec();
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
}
