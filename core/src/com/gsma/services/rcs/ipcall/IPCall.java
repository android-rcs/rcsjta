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

import com.gsma.services.rcs.JoynServiceException;
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
    	 * Unknown state
    	 */
    	public final static int UNKNOWN = 0;

    	/**
    	 * Call invitation received
    	 */
    	public final static int INVITED = 1;
    	
    	/**
    	 * Call invitation sent
    	 */
    	public final static int INITIATED = 2;
    	
    	/**
    	 * Call is started
    	 */
    	public final static int STARTED = 3;
    	
    	/**
    	 * call has been aborted
    	 */
    	public final static int ABORTED = 4;

    	/**
    	 * Call has failed
    	 */
    	public final static int FAILED = 5;

    	/**
    	 * Call has been terminated
    	 */
    	public static final int TERMINATED = 6;

    	/**
    	 * Call rejected
    	 */
    	public final static int REJECTED = 7;

    	/**
    	 * Call on hold
    	 */
    	public final static int HOLD = 8;

    	/**
    	 * Call about to be accepted
    	 */
    	public final static int ACCEPTING = 9;

    	/**
    	 * Call ringing
    	 */
    	public final static int RINGING = 10;

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
         * IP call is aborted because already taken by the secondary device.
         */
        public static final int ABORTED_BY_SECONDARY_DEVICE = 4;

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
    IPCall(IIPCall callInf) {
    	this.callInf = callInf;
    }
    	
    /**
	 * Returns the call ID of call
	 * 
	 * @return Call ID
	 * @throws JoynServiceException
	 */
	public String getCallId() throws JoynServiceException {
		try {
			return callInf.getCallId();
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
			return callInf.getRemoteContact();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the state of the call
	 * 
	 * @return State
	 * @see IPCall.State
	 * @throws JoynServiceException
	 */
	public int getState() throws JoynServiceException {
		try {
			return callInf.getState();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}		
		
	/**
	 * Returns the direction of the call (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see IPCall.Direction
	 * @throws JoynServiceException
	 */
	public int getDirection() throws JoynServiceException {
		try {
			return callInf.getDirection();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}	
	
	/**
	 * Accepts call invitation
	 * 
	 * @param player IP call player
	 * @param renderer IP call renderer
	 * @throws JoynServiceException
	 */
	public void acceptInvitation(IPCallPlayer player, IPCallRenderer renderer) throws JoynServiceException {
		try {
			callInf.acceptInvitation(player, renderer);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects call invitation
	 * 
	 * @throws JoynServiceException
	 */
	public void rejectInvitation() throws JoynServiceException {
		try {
			callInf.rejectInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Aborts the call
	 * 
	 * @throws JoynServiceException
	 */
	public void abortCall() throws JoynServiceException {
		try {
			callInf.abortCall();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Is video activated
	 * 
	 * @return Boolean
	 * @throws JoynServiceException
	 */
	public boolean isVideo() throws JoynServiceException {
		try {
			return callInf.isVideo();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Add video stream
	 * 
	 * @throws JoynServiceException
	 */
	public void addVideo() throws JoynServiceException {
		try {
			callInf.addVideo();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Remove video stream
	 * 
	 * @throws JoynServiceException
	 */
	public void removeVideo() throws JoynServiceException {
		try {
			callInf.removeVideo();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Is call on hold
	 * 
	 * @return Boolean
	 * @throws JoynServiceException
	 */
	public boolean isOnHold() throws JoynServiceException {
		try {
			return callInf.isOnHold();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Puts the call on hold
	 * 
	 * @throws JoynServiceException
	 */
	public void holdCall() throws JoynServiceException {
		try {
			callInf.holdCall();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Continues the call that hold's on
	 * 
	 * @throws JoynServiceException
	 */
	public void continueCall() throws JoynServiceException {
		try {
			callInf.continueCall();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the video codec used during sharing
	 *
	 * @return VideoCodec
	 * @throws JoynServiceException
	 */
	public VideoCodec getVideoCodec() throws JoynServiceException {
		try {
			return callInf.getVideoCodec();
		} catch (Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the audio codec used during sharing
	 *
	 * @return AudioCodec
	 * @throws JoynServiceException
	 */
	public AudioCodec getAudioCodec() throws JoynServiceException {
		try {
			return callInf.getAudioCodec();
		} catch (Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
}
