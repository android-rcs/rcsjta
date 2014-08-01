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
import com.gsma.services.rcs.extension.IMultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaSession;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionError;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionListener;
import com.orangelabs.rcs.core.ims.service.sip.messaging.GenericSipMsrpSession;
import com.orangelabs.rcs.core.ims.service.sip.messaging.OriginatingSipMsrpSession;
import com.orangelabs.rcs.service.broadcaster.IMultimediaMessagingSessionEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Multimedia messaging session
 *
 * @author Jean-Marc AUFFRET
 */
public class MultimediaMessagingSessionImpl extends IMultimediaMessagingSession.Stub implements SipSessionListener {

	/**
	 * Core session
	 */
	private GenericSipMsrpSession session;

	private final IMultimediaMessagingSessionEventBroadcaster mMultimediaMessagingSessionEventBroadcaster;
	/**
	 * Lock used for synchronisation
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
     * @param broadcaster IMultimediaMessagingSessionEventBroadcaster
     */
	public MultimediaMessagingSessionImpl(GenericSipMsrpSession session,
			IMultimediaMessagingSessionEventBroadcaster broadcaster) {
		this.session = session;
		mMultimediaMessagingSessionEventBroadcaster = broadcaster;
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
				if (session instanceof OriginatingSipMsrpSession) {
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
		if (session instanceof OriginatingSipMsrpSession) {
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
		
		// Test API permission
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
	public void rejectInvitation() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}

		// Test API permission
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
	public void abortSession() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Cancel session");
		}

		// Test API permission
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
     * Sends a message in real time
     * 
     * @param content Message content
	 * @return Returns true if sent successfully else returns false
	 * @throws ServerApiException
     */
    public boolean sendMessage(byte[] content) throws ServerApiException {
		// Test API permission
		ServerApiUtils.testApiExtensionPermission(session.getServiceId());

		return session.sendMessage(content);
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
			mMultimediaMessagingSessionEventBroadcaster.broadcastMultimediaMessagingStateChanged(getRemoteContact(),
					getSessionId(), STARTED);
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
			mMultimediaMessagingSessionEventBroadcaster.broadcastMultimediaMessagingStateChanged(getRemoteContact(),
					sessionId, ABORTED);

	        // Remove session from the list
	        MultimediaSessionServiceImpl.removeMessagingSipSession(sessionId);
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
			mMultimediaMessagingSessionEventBroadcaster.broadcastMultimediaMessagingStateChanged(getRemoteContact(),
					sessionId, ABORTED);

	        // Remove session from the list
	        MultimediaSessionServiceImpl.removeMessagingSipSession(sessionId);
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
					mMultimediaMessagingSessionEventBroadcaster.broadcastMultimediaMessagingStateChanged(getRemoteContact(), sessionId, FAILED /*, MultimediaSession.Error.INVITATION_DECLINED*/);
					break;
				case SipSessionError.MEDIA_FAILED:
					// TODO : Handle reason code in CR009
					mMultimediaMessagingSessionEventBroadcaster.broadcastMultimediaMessagingStateChanged(getRemoteContact(), sessionId, FAILED /*, MultimediaSession.Error.MEDIA_FAILED*/);
					break;
				default:
					// TODO : Handle reason code in CR009
					mMultimediaMessagingSessionEventBroadcaster.broadcastMultimediaMessagingStateChanged(getRemoteContact(), sessionId, FAILED /*, MultimediaSession.Error.SESSION_FAILED*/);
			}

	        // Remove session from the list
	        MultimediaSessionServiceImpl.removeMessagingSipSession(sessionId);
	    }
    }
    
    /**
     * Receive data
     * 
     * @param data Data
     */
    public void handleReceiveData(byte[] data) {
    	synchronized(lock) {
			// Notify event listeners
			mMultimediaMessagingSessionEventBroadcaster.broadcastNewMessage(getRemoteContact(),
					getSessionId(), data);
	    }  	
    }
}
