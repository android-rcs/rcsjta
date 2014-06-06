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
package com.gsma.services.rcs.ipcall;

import com.gsma.services.rcs.JoynServiceException;

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
    	 * Inactive state
    	 */
    	public final static int INACTIVE = 0;

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
    	public final static int ABORTED = 5;
    	
        /**
         * Call has been terminated
         */
        public static final int TERMINATED = 6;

        /**
    	 * Call has failed 
    	 */
    	public final static int FAILED = 7;
    	
        /**
    	 * Call on hold 
    	 */
    	public final static int HOLD = 8;

    	private State() {
        }    	
    }
    
    /**
     * Direction of the call
     */
    public static class Direction {
        /**
         * Incoming call
         */
        public static final int INCOMING = 0;
        
        /**
         * Outgoing call
         */
        public static final int OUTGOING = 1;
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
	 * Returns the remote contact
	 * 
	 * @return Contact
	 * @throws JoynServiceException
	 */
	public String getRemoteContact() throws JoynServiceException {
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
	 * Adds a listener on IP call events
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public void addEventListener(IPCallListener listener) throws JoynServiceException {
		try {
			callInf.addEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Removes a listener from IP call events
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public void removeEventListener(IPCallListener listener) throws JoynServiceException {
		try {
			callInf.removeEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
}
