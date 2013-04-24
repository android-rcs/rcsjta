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

package com.orangelabs.rcs.service.api.server.sip;

import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.sip.GenericSipSession;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionError;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionListener;
import com.orangelabs.rcs.service.api.client.sip.ISipSession;
import com.orangelabs.rcs.service.api.client.sip.ISipSessionEventListener;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * SIP session
 *
 * @author jexa7410
 */
public class SipSession extends ISipSession.Stub implements SipSessionListener {

	/**
	 * Core session
	 */
	private GenericSipSession session;

	/**
	 * List of listeners
	 */
	private RemoteCallbackList<ISipSessionEventListener> listeners = new RemoteCallbackList<ISipSessionEventListener>();

	/**
	 * Lock used for synchronisation
	 */
	private Object lock = new Object();
	
    /**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param session Session
     */
	public SipSession(GenericSipSession session) {
		this.session = session;
		session.addListener(this);
	}

    /**
     * Get session ID
     *
     * @return Session ID
     */
	public String getSessionID() {
		return session.getSessionID();
	}

    /**
     * Get remote contact
     *
     * @return Contact
     */
	public String getRemoteContact() {
		return session.getRemoteContact();
	}
	
	/**
	 * Get session state
	 * 
	 * @return State (see class SessionState) 
	 * @see SessionState
	 */
	public int getSessionState() {
		return ServerApiUtils.getSessionState(session);
	}	

	/**
	 * Get feature tag of the service
	 * 
	 * @return Feature tag
	 */
	public String getFeatureTag() {
		return session.getFeatureTag();
	}	
	
	/**
     * Get local SDP
     * 
     * @return SDP
     */
    public String getLocalSdp() throws RemoteException {
    	return session.getLocalSdp();
    }

    /**
     * Get remote SDP
     * 
     * @return SDP
     */
    public String getRemoteSdp() throws RemoteException {
    	return session.getRemoteSdp();
    }
	
	/**
	 * Accept the session invitation
	 * 
	 * @param sdp SDP answer
	 */
	public void acceptSession(String sdp) {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}

		// Set local SDP
		session.setLocalSdp(sdp);
		
		// Accept invitation
		session.acceptSession();
	}

	/**
	 * Reject the session invitation
	 */
	public void rejectSession() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}

		// Reject invitation
		session.rejectSession(603);
	}

	/**
	 * Cancel the session
	 */
	public void cancelSession() {
		if (logger.isActivated()) {
			logger.info("Cancel session");
		}

		// Abort the session
		session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
	}

	/**
     * Add session listener
     *
     * @param listener Listener
     */
	public void addSessionListener(ISipSessionEventListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an event listener");
		}

    	synchronized(lock) {
    		listeners.register(listener);
    	}
	}

	/**
     * Remove session listener
     *
     * @param listener Listener
     */
	public void removeSessionListener(ISipSessionEventListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove an event listener");
		}

    	synchronized(lock) {
    		listeners.unregister(listener);
    	}
	}

	/**
	 * Session is started
	 */
    public void handleSessionStarted() {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session started");
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleSessionStarted();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }
    }
    
    /**
     * Session has been aborted
     * 
	 * @param reason Termination reason
     */
    public void handleSessionAborted(int reason) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session aborted (reason " + reason + ")");
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleSessionAborted(reason);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        SipApiService.removeSipSession(session.getSessionID());
	    }
    }
    
    /**
     * Session has been terminated by remote
     */
    public void handleSessionTerminatedByRemote() {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session terminated by remote");
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleSessionTerminatedByRemote();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        SipApiService.removeSipSession(session.getSessionID());
	    }
    }
    
    /**
     * Session error
     *
     * @param error Error
     */
    public void handleSessionError(SipSessionError error) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session error " + error.getErrorCode());
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleSessionError(error.getErrorCode());
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        SipApiService.removeSipSession(session.getSessionID());
	    }
    }
}
