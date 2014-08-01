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

package com.orangelabs.rcs.service.api;
import static com.gsma.services.rcs.extension.MultimediaSession.State.ABORTED;
import static com.gsma.services.rcs.extension.MultimediaSession.State.FAILED;
import static com.gsma.services.rcs.extension.MultimediaSession.State.STARTED;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.extension.IMultimediaStreamingSession;
import com.gsma.services.rcs.extension.MultimediaSession;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionError;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionListener;
import com.orangelabs.rcs.core.ims.service.sip.streaming.GenericSipRtpSession;
import com.orangelabs.rcs.core.ims.service.sip.streaming.OriginatingSipRtpSession;
import com.orangelabs.rcs.service.broadcaster.IMultimediaStreamingSessionEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Multimedia streaming session
 *
 * @author Jean-Marc AUFFRET
 */
public class MultimediaStreamingSessionImpl extends IMultimediaStreamingSession.Stub implements SipSessionListener {

	/**
	 * Core session
	 */
	private GenericSipRtpSession session;

	private final IMultimediaStreamingSessionEventBroadcaster mMultimediaStreamingSessionEventBroadcaster;

	/**
	 * Lock used for synchronization
	 */
	private final Object lock = new Object();
	
    /**
	 * The logger
	 */
	private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Constructor
     *
     * @param session Session
     * @param broadcaster IMultimediaStreamingSessionEventBroadcaster
     */
	public MultimediaStreamingSessionImpl(GenericSipRtpSession session,
			IMultimediaStreamingSessionEventBroadcaster broadcaster) {
		this.session = session;
		mMultimediaStreamingSessionEventBroadcaster = broadcaster;
		session.addListener(this);
	}

    /**
	 * Returns the session ID of the multimedia session
	 * 
	 * @return Session ID
	 */
	public String getSessionId() {
		return session.getSessionID();
	}

	/**
	 * Returns the remote contact ID
	 * 
	 * @return ContactId
	 */
	public ContactId getRemoteContact() {
		return session.getRemoteContact();
	}
	
	/**
	 * Returns the state of the session
	 * 
	 * @return State
	 */
	public int getState() {
		int result = MultimediaSession.State.INACTIVE;
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null) {
			if (dialogPath.isSessionCancelled()) {
				// Session canceled
				result = MultimediaSession.State.ABORTED;
			} else
			if (dialogPath.isSessionEstablished()) {
				// Session started
				result = MultimediaSession.State.STARTED;
			} else
			if (dialogPath.isSessionTerminated()) {
				// Session terminated
				result = MultimediaSession.State.TERMINATED;
			} else {
				// Session pending
				if (session instanceof OriginatingSipRtpSession) {
					result = MultimediaSession.State.INITIATED;
				} else {
					result = MultimediaSession.State.INVITED;
				}
			}
		}
		return result;	
	}	

	/**
	 * Returns the direction of the session (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see MultimediaSession.Direction
	 */
	public int getDirection() {
		if (session instanceof OriginatingSipRtpSession) {
			return MultimediaSession.Direction.OUTGOING;
		} else {
			return MultimediaSession.Direction.INCOMING;
		}
	}		
	
	/**
	 * Returns the service ID
	 * 
	 * @return Service ID
	 */
	public String getServiceId() {
		return session.getServiceId();
	}	
	
	/**
	 * Accepts session invitation
	 * 
	 * @throws ServerApiException
	 */
	public void acceptInvitation() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}
		
		// Test security extension
		ServerApiUtils.testApiExtensionPermission(session.getServiceId());

		// Accept invitation
        Thread t = new Thread() {
    		public void run() {
    			session.acceptSession();
    		}
    	};
    	t.start();
	}

	/**
	 * Rejects session invitation
	 * 
	 * @throws ServerApiException 
	 */
	public void rejectInvitation() throws ServerApiException  {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}

		// Test security extension
		ServerApiUtils.testApiExtensionPermission(session.getServiceId());

		// Reject invitation
        Thread t = new Thread() {
    		public void run() {
    			session.rejectSession(603);
    		}
    	};
    	t.start();
    }

	/**
	 * Aborts the session
	 * 
	 * @throws ServerApiException
	 */
	public void abortSession() throws ServerApiException  {
		if (logger.isActivated()) {
			logger.info("Cancel session");
		}

		// Test security extension
		ServerApiUtils.testApiExtensionPermission(session.getServiceId());

		// Abort the session
        Thread t = new Thread() {
    		public void run() {
    			session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
    		}
    	};
    	t.start();
	}

    /**
     * Sends a payload in real time
     * 
     * @param content Payload content
	 * @return Returns true if sent successfully else returns false
	 * @throws ServerApiException 
     */
    public boolean sendPayload(byte[] content) throws ServerApiException  {
		// Test security extension
		ServerApiUtils.testApiExtensionPermission(session.getServiceId());

		if (session != null) {
    		return session.sendPlayload(content);
    	} else {
    		return false;	
    	}
    }	
	
    /*------------------------------- SESSION EVENTS ----------------------------------*/

	/**
	 * Session is started
	 */
    public void handleSessionStarted() {
		if (logger.isActivated()) {
			logger.info("Session started");
		}
    	synchronized(lock) {
			// Notify event listeners
			mMultimediaStreamingSessionEventBroadcaster.broadcastMultimediaStreamingStateChanged(
					getRemoteContact(), getSessionId(), STARTED);
	    }
    }
    
    /**
     * Session has been aborted
     * 
	 * @param reason Termination reason
     */
    public void handleSessionAborted(int reason) {
		if (logger.isActivated()) {
			logger.info("Session aborted (reason " + reason + ")");
		}
    	synchronized(lock) {
			// Notify event listeners
			String sessionId = getSessionId();
			mMultimediaStreamingSessionEventBroadcaster.broadcastMultimediaStreamingStateChanged(
					getRemoteContact(), sessionId, ABORTED);

	        // Remove session from the list
	        MultimediaSessionServiceImpl.removeStreamingSipSession(sessionId);
	    }
    }
    
    /**
     * Session has been terminated by remote
     */
    public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session terminated by remote");
		}
    	synchronized(lock) {
			// Notify event listeners
			// TODO : Handle reason code in CR009
			String sessionId = getSessionId();
			mMultimediaStreamingSessionEventBroadcaster.broadcastMultimediaStreamingStateChanged(getRemoteContact(), sessionId, ABORTED);

	        // Remove session from the list
	        MultimediaSessionServiceImpl.removeStreamingSipSession(sessionId);
	    }
    }
    
    /**
     * Session error
     *
     * @param error Error
     */
    public void handleSessionError(SipSessionError error) {
		if (logger.isActivated()) {
			logger.info("Session error " + error.getErrorCode());
		}
    	synchronized(lock) {
			// Notify event listeners
			String sessionId = getSessionId();
			switch (error.getErrorCode()) {
				case SipSessionError.SESSION_INITIATION_DECLINED:
					// TODO : Handle reason code in CR009
					mMultimediaStreamingSessionEventBroadcaster.broadcastMultimediaStreamingStateChanged(getRemoteContact(), sessionId, FAILED /*, MultimediaSession.Error.INVITATION_DECLINED*/);
					break;
				case SipSessionError.MEDIA_FAILED:
					// TODO : Handle reason code in CR009
					mMultimediaStreamingSessionEventBroadcaster.broadcastMultimediaStreamingStateChanged(getRemoteContact(), sessionId, FAILED /*, MultimediaSession.Error.MEDIA_FAILED*/);
					break;
				default:
					// TODO : Handle reason code in CR009
					mMultimediaStreamingSessionEventBroadcaster.broadcastMultimediaStreamingStateChanged(getRemoteContact(), sessionId, FAILED /*, MultimediaSession.Error.SESSION_FAILED*/);
			}

	        // Remove session from the list
	        MultimediaSessionServiceImpl.removeStreamingSipSession(sessionId);
	    }
    }
    
    /**
     * Receive data
     * 
     * @param data Data
     */
    public void handleReceiveData(byte[] data) {
		synchronized (lock) {
			// Notify event listeners
			mMultimediaStreamingSessionEventBroadcaster.broadcastNewPayload(getRemoteContact(),
					getSessionId(), data);
		}
    }
}
