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
package com.gsma.services.rcs.vsh;

import com.gsma.services.rcs.JoynServiceException;
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
    	 * Inactive state
    	 */
    	public final static int INACTIVE = 0;

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
    	public final static int ABORTED = 5;
    	
        /**
         * Sharing has been terminated
         */
        public static final int TERMINATED = 6;

        /**
    	 * Sharing has failed 
    	 */
    	public final static int FAILED = 7;
    	
    	private State() {
        }    	
    }
    
    /**
     * Direction of the sharing
     */
    public static class Direction {
        /**
         * Incoming sharing
         */
        public static final int INCOMING = 0;
        
        /**
         * Outgoing sharing
         */
        public static final int OUTGOING = 1;
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
        public static final int H264 = 0;
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
	 * Returns the video codec
	 * 
	 * @return Video codec
	 * @see VideoCodec
	 * @throws JoynServiceException
	 */
	public VideoCodec getVideoCodec() throws JoynServiceException {
		try {
			return sharingInf.getVideoCodec();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the state of the sharing
	 * 
	 * @return State
	 * @see VideoSharing.State
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
	 * @see VideoSharing.Direction
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
	 * Accepts video sharing invitation
	 * 
	 * @param renderer Video renderer
	 * @throws JoynServiceException
	 */
	public void acceptInvitation(VideoRenderer renderer) throws JoynServiceException {
		try {
			sharingInf.acceptInvitation(renderer);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects video sharing invitation
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

	/**
	 * Adds a listener on video sharing events
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public void addEventListener(VideoSharingListener listener) throws JoynServiceException {
		try {
			sharingInf.addEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Removes a listener from video sharing
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public void removeEventListener(VideoSharingListener listener) throws JoynServiceException {
		try {
			sharingInf.removeEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
}
