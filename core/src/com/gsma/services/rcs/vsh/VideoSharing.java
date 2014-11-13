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
package com.gsma.services.rcs.vsh;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Video sharing
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharing {

    /**
     * Video sharing state
     */
    public static class State {
    	/**
    	 * Sharing invitation received
    	 */
    	public final static int INVITED = 0;
    	
    	/**
    	 * Sharing invitation sent
    	 */
    	public final static int INITIATED = 1;
    	
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
    	 * Sharing has been rejected
    	 */
    	public final static int REJECTED = 5;

        /**
    	 * Ringing
    	 */
    	public final static int RINGING = 6;

    	/**
    	 * Sharing has been accepted and is in the process of becoming started
    	 */
    	public final static int ACCEPTING = 7;

    	private State() {
        }    	
    }

    /**
     * Reason code associated with the VIDEO share state.
     */
    public static class ReasonCode {

        /**
         * No specific reason code specified.
         */
        public static final int UNSPECIFIED = 0;

        /**
         * Video share is aborted by local user.
         */
        public static final int ABORTED_BY_USER = 1;

        /**
         * Video share is aborted by remote user.
         */
        public static final int ABORTED_BY_REMOTE = 2;

        /**
         * Video share is aborted by system.
         */
        public static final int ABORTED_BY_SYSTEM = 3;

        /**
         * Video share is rejected because already taken by the secondary device.
         */
        public static final int REJECTED_BY_SECONDARY_DEVICE = 4;

        /**
         * Video share invitation was rejected due to max number of sharing sessions
         * already are open.
         */
        public static final int REJECTED_MAX_SHARING_SESSIONS = 5;

        /**
         * Video share invitation was rejected by local user.
         */
        public static final int REJECTED_BY_USER = 6;

        /**
         * Video share invitation was rejected by remote.
         */
        public static final int REJECTED_BY_REMOTE = 7;

        /**
         * Video share been rejected due to time out.
         */
        public static final int REJECTED_TIME_OUT = 8;

        /**
         * Video share initiation failed.
         */
        public static final int FAILED_INITIATION = 9;

        /**
         * Sharing of the video share has failed.
         */
        public static final int FAILED_SHARING = 10;
    }
    
    /**
     * Video sharing error
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
     * Video encoding
     */
    public static class Encoding {
        /**
         * H264
         */
        public static final String H264 = "H264";
    }      
    
    /**
     * Video sharing interface
     */
    private IVideoSharing sharingInf;
    
    /**
     * Constructor
     * 
     * @param sharingInf Video sharing interface
     */
    VideoSharing(IVideoSharing sharingInf) {
    	this.sharingInf = sharingInf;
    }
    	
    /**
	 * Returns the sharing ID of the video sharing
	 * 
	 * @return Sharing ID
	 * @throws RcsServiceException
	 */
	public String getSharingId() throws RcsServiceException {
		try {
			return sharingInf.getSharingId();
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
			return sharingInf.getRemoteContact();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the video codec
	 * 
	 * @return Video codec
	 * @see VideoCodec
	 * @throws RcsServiceException
	 */
	public VideoCodec getVideoCodec() throws RcsServiceException {
		try {
			return sharingInf.getVideoCodec();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the state of the sharing
	 *
	 * @return State
	 * @see VideoSharing.State
	 * @throws RcsServiceException
	 */
	public int getState() throws RcsServiceException {
		try {
			return sharingInf.getState();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the reason code of the sharing
	 *
	 * @return ReasonCode
	 * @see VideoSharing.ReasonCode
	 * @throws RcsServiceException
	 */
	public int getReasonCode() throws RcsServiceException {
		try {
			return sharingInf.getReasonCode();
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the direction of the sharing (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see VideoSharing.Direction
	 * @throws RcsServiceException
	 */
	public int getDirection() throws RcsServiceException {
		try {
			return sharingInf.getDirection();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}	
	
	/**
	 * Accepts video sharing invitation
	 * 
	 * @param renderer Video renderer
	 * @throws RcsServiceException
	 */
	public void acceptInvitation(VideoRenderer renderer) throws RcsServiceException {
		try {
			sharingInf.acceptInvitation(renderer);
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects video sharing invitation
	 * 
	 * @throws RcsServiceException
	 */
	public void rejectInvitation() throws RcsServiceException {
		try {
			sharingInf.rejectInvitation();
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
			sharingInf.abortSharing();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
}
