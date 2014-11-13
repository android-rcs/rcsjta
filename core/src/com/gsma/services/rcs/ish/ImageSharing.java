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
package com.gsma.services.rcs.ish;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contacts.ContactId;

import android.net.Uri;

/**
 * Image sharing
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharing {

    /**
     * Image sharing state
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
    	 * Image has been transferred with success
    	 */
    	public final static int TRANSFERRED = 5;

    	/**
    	 * Sharing has been rejected
    	 */
    	public final static int REJECTED = 6;

    	/**
    	 * Ringing
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
     * Reason code associated with the image share state.
     */
    public static class ReasonCode {

        /**
         * No specific reason code specified.
         */
        public static final int UNSPECIFIED = 0;

        /**
         * Image share is aborted by local user.
         */
        public static final int ABORTED_BY_USER = 1;

        /**
         * Image share is aborted by remote user.
         */
        public static final int ABORTED_BY_REMOTE = 2;

        /**
         * Image share is aborted by system.
         */
        public static final int ABORTED_BY_SYSTEM = 3;

        /**
         * Image share is rejected because already taken by the secondary device.
         */
        public static final int REJECTED_BY_SECONDARY_DEVICE = 4;

        /**
         * Incoming image was rejected due to time out.
         */
        public static final int REJECTED_TIME_OUT = 5;

        /**
         * Incoming image was rejected as is cannot be received due to lack of local storage space.
         */
        public static final int REJECTED_LOW_SPACE = 6;

        /**
         * Incoming image was rejected as it was too big to be received.
         */
        public static final int REJECTED_MAX_SIZE = 7;

        /**
         * Incoming image was rejected because max number of sharing sessions is achieved.
         */
        public static final int REJECTED_MAX_SHARING_SESSIONS = 8;

        /**
         * Incoming image was rejected by local user.
         */
        public static final int REJECTED_BY_USER = 9;

        /**
         * Incoming image was rejected by remote.
         */
        public static final int REJECTED_BY_REMOTE = 10;

        /**
         * Image share initiation failed;
         */
        public static final int FAILED_INITIATION = 11;

        /**
         * Sharing of the image share has failed.
         */
        public static final int FAILED_SHARING = 12;

        /**
         * Saving of the image share has failed.
         */
        public static final int FAILED_SAVING = 13;
    }
    
    /**
     * Image sharing error
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

    	/**
    	 * Image saving has failed 
       	 */
    	public final static int SAVING_FAILED = 2;
    	
        private Error() {
        }    	
    }

    /**
     * Image sharing interface
     */
    private IImageSharing sharingInf;
    
    /**
     * Constructor
     * 
     * @param sharingInf Image sharing interface
     */
    ImageSharing(IImageSharing sharingInf) {
    	this.sharingInf = sharingInf;
    }
    	
    /**
	 * Returns the sharing ID of the image sharing
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
	 * Returns the URI of the file to be shared
	 *
	 * @return Uri
	 * @throws RcsServiceException
	 */
	public Uri getFile() throws RcsServiceException {
		try {
			return sharingInf.getFile();
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
     * Returns the complete filename including the path of the file to be transferred
     *
     * @return Filename
	 * @throws RcsServiceException
     */
	public String getFileName() throws RcsServiceException {
		try {
			return sharingInf.getFileName();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}

	/**
     * Returns the size of the file to be transferred
     *
     * @return Size in bytes
	 * @throws RcsServiceException
     */
	public long getFileSize() throws RcsServiceException {
		try {
			return sharingInf.getFileSize();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}	

    /**
     * Returns the MIME type of the file to be transferred
     * 
     * @return Type
	 * @throws RcsServiceException
     */
    public String getMimeType() throws RcsServiceException {
		try {
			return sharingInf.getMimeType();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
    }

	/**
	 * Returns the state of the sharing
	 * 
	 * @return State
	 * @see ImageSharing.State
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
	 * Returns the reason code of the state of the sharing
	 *
	 * @return ReasonCode
	 * @see ImageSharing.ReasonCode
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
	 * @see ImageSharing.Direction
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
	 * Accepts image sharing invitation
	 * 
	 * @throws RcsServiceException
	 */
	public void acceptInvitation() throws RcsServiceException {
		try {
			sharingInf.acceptInvitation();
		} catch(Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects image sharing invitation
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
