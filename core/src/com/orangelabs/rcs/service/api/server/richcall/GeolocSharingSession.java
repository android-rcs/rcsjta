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

package com.orangelabs.rcs.service.api.server.richcall;

import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSessionListener;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.provider.sharing.RichCall;
import com.orangelabs.rcs.provider.sharing.RichCallData;
import com.orangelabs.rcs.service.api.client.SessionState;
import com.orangelabs.rcs.service.api.client.messaging.GeolocMessage;
import com.orangelabs.rcs.service.api.client.messaging.GeolocPush;
import com.orangelabs.rcs.service.api.client.richcall.IGeolocSharingEventListener;
import com.orangelabs.rcs.service.api.client.richcall.IGeolocSharingSession;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Geoloc sharing session
 *
 * @author jexa7410
 */
public class GeolocSharingSession extends IGeolocSharingSession.Stub implements GeolocTransferSessionListener {

	/**
	 * Core session
	 */
	private GeolocTransferSession session;

	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IGeolocSharingEventListener> listeners = new RemoteCallbackList<IGeolocSharingEventListener>();

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
	public GeolocSharingSession(GeolocTransferSession session) {
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
	 * Accept the session invitation
	 */
	public void acceptSession() {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}

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

		// Update rich messaging history
		RichCall.getInstance().setStatus(session.getSessionID(), RichCallData.STATUS_CANCELED);

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

		if (session.isGeolocTransfered()) {
			// Automatically closed after transfer
			return;
		}
		
		// Abort the session
		session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
	}

	/**
     * Add session listener
     *
     * @param listener Listener
     */
	public void addSessionListener(IGeolocSharingEventListener listener) {
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
	public void removeSessionListener(IGeolocSharingEventListener listener) {
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
	
			// Update rich call history
			RichCall.getInstance().setStatus(session.getSessionID(), RichCallData.STATUS_CANCELED);
			
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
	        RichCallApiService.removeGeolocSharingSession(session.getSessionID());
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
			
	  		if (session.isGeolocTransfered()) {
				// The geoloc has been received, so only remove session from the list
		        RichCallApiService.removeGeolocSharingSession(session.getSessionID());
	  			return;
	  		}
			
	  		// Update rich call history
	  		RichCall.getInstance().setStatus(session.getSessionID(), RichCallData.STATUS_FAILED);
	
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
	        RichCallApiService.removeGeolocSharingSession(session.getSessionID());
	    }
    }
    
    /**
     * Content sharing error
     *
     * @param error Error
     */
    public void handleSharingError(ContentSharingError error) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Sharing error " + error.getErrorCode());
			}
	
			// Update rich messaging history 
			RichCall.getInstance().setStatus(session.getSessionID(), RichCallData.STATUS_FAILED); 
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleSharingError(error.getErrorCode());
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        RichCallApiService.removeGeolocSharingSession(session.getSessionID());
	    }
    }

    /**
     * Geoloc has been transfered
     *
     * @param geoloc Geoloc info
     */
    public void handleContentTransfered(GeolocPush geoloc) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Geoloc transfered");
			}
	
			// Update rich call history 
			RichCall.getInstance().setStatus(session.getSessionID(), RichCallData.STATUS_TERMINATED); 
			
			// Update rich messaging history
			String msgId = ChatUtils.generateMessageId();
			GeolocMessage geolocMsg = new GeolocMessage(msgId, session.getRemoteContact(), geoloc, false, null);
			RichMessaging.getInstance().addIncomingGeoloc(geolocMsg);
			
			// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleGeolocTransfered(geoloc);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }
    }
}
