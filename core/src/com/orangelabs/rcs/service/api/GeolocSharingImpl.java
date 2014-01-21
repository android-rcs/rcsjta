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
package com.orangelabs.rcs.service.api;

import com.gsma.services.rcs.gsh.IGeolocSharing;
import com.gsma.services.rcs.gsh.IGeolocSharingListener;

import android.os.RemoteCallbackList;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.gsh.GeolocSharing;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSessionListener;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.OriginatingGeolocTransferSession;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Geoloc sharing implementation
 *  
 * @author Jean-Marc AUFFRET
 */
public class GeolocSharingImpl extends IGeolocSharing.Stub implements GeolocTransferSessionListener {
	
	/**
	 * Core session
	 */
	private GeolocTransferSession session;
	
	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IGeolocSharingListener> listeners = new RemoteCallbackList<IGeolocSharingListener>();
	
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
	public GeolocSharingImpl(GeolocTransferSession session) {
		this.session = session;
		
		session.addListener(this);
	}

	/**
	 * Returns the sharing ID of the geoloc sharing
	 * 
	 * @return Sharing ID
	 */
	public String getSharingId() {
		return session.getSessionID();
	}
	
	/**
     * Returns the geolocation info
     *
     * @return Geoloc object
     */
	public Geoloc getGeoloc()  {
		GeolocPush geoloc = session.getGeoloc();
		if (geoloc != null) {
			com.gsma.services.rcs.chat.Geoloc geolocApi = new com.gsma.services.rcs.chat.Geoloc(geoloc.getLabel(),
					geoloc.getLatitude(), geoloc.getLongitude(), geoloc.getAltitude(),
					geoloc.getExpiration(), geoloc.getAccuracy());
	    	return geolocApi;
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the remote contact
	 * 
	 * @return Contact
	 */
	public String getRemoteContact() {
		return PhoneUtils.extractNumberFromUri(session.getRemoteContact());
	}
	
	/**
	 * Returns the state of the geoloc sharing
	 * 
	 * @return State 
	 */
	public int getState() {
		int result = GeolocSharing.State.UNKNOWN;
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null) {
			if (dialogPath.isSessionCancelled()) {
				// Session canceled
				result = GeolocSharing.State.ABORTED;
			} else
			if (dialogPath.isSessionEstablished()) {
				// Session started
				result = GeolocSharing.State.STARTED;
			} else
			if (dialogPath.isSessionTerminated()) {
				// Session terminated
				if (session.isGeolocTransfered()) {
					result = GeolocSharing.State.TRANSFERRED;
				} else {
					result = GeolocSharing.State.ABORTED;
				}
			} else {
				// Session pending
				if (session instanceof OriginatingGeolocTransferSession) {
					result = GeolocSharing.State.INITIATED;
				} else {
					result = GeolocSharing.State.INVITED;
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns the direction of the sharing (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see GeolocSharing.Direction
	 */
	public int getDirection() {
		if (session instanceof OriginatingGeolocTransferSession) {
			return GeolocSharing.Direction.OUTGOING;
		} else {
			return GeolocSharing.Direction.INCOMING;
		}
	}		
		
	/**
	 * Accepts geoloc sharing invitation
	 */
	public void acceptInvitation() {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}

		// Accept invitation
        Thread t = new Thread() {
    		public void run() {
    			session.acceptSession();
    		}
    	};
    	t.start();
	}
	
	/**
	 * Rejects geoloc sharing invitation
	 */
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}

		// Reject invitation
        Thread t = new Thread() {
    		public void run() {
    			session.rejectSession(603);
    		}
    	};
    	t.start();
	}

	/**
	 * Aborts the sharing
	 */
	public void abortSharing() {
		if (logger.isActivated()) {
			logger.info("Cancel session");
		}

		if (session.isGeolocTransfered()) {
			// Automatically closed after transfer
			return;
		}
		
		// Abort the session
        Thread t = new Thread() {
    		public void run() {
    			session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
    		}
    	};
    	t.start();		
	}

	/**
	 * Adds a listener on geoloc sharing events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener(IGeolocSharingListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an event listener");
		}

    	synchronized(lock) {
    		listeners.register(listener);
    	}
	}
	
	/**
	 * Removes a listener on geoloc sharing events
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener(IGeolocSharingListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove an event listener");
		}

    	synchronized(lock) {
    		listeners.unregister(listener);
    	}
	}
	
    /*------------------------------- SESSION EVENTS ----------------------------------*/
	
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
	            	listeners.getBroadcastItem(i).onSharingStarted();
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
	            	listeners.getBroadcastItem(i).onSharingAborted();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        GeolocSharingServiceImpl.removeGeolocSharingSession(session.getSessionID());
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
			
			// Check if the geoloc has been transferred or not
	  		if (session.isGeolocTransfered()) {
		        // Remove session from the list
	  			GeolocSharingServiceImpl.removeGeolocSharingSession(session.getSessionID());
	  		} else {
		  		// Notify event listeners
				final int N = listeners.beginBroadcast();
		        for (int i=0; i < N; i++) {
		            try {
		            	listeners.getBroadcastItem(i).onSharingAborted();
		            } catch(Exception e) {
		            	if (logger.isActivated()) {
		            		logger.error("Can't notify listener", e);
		            	}
		            }
		        }
		        listeners.finishBroadcast();

		        // Remove session from the list
		        GeolocSharingServiceImpl.removeGeolocSharingSession(session.getSessionID());
	  		}
	    }
    }
    
    /**
     * Content sharing error
     *
     * @param error Error
     */
    public void handleSharingError(ContentSharingError error) {
    	synchronized(lock) {
			if (error.getErrorCode() == ContentSharingError.SESSION_INITIATION_CANCELLED) {
				// Do nothing here, this is an aborted event
				return;
			}

			if (logger.isActivated()) {
				logger.info("Sharing error " + error.getErrorCode());
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	int code;
	            	switch(error.getErrorCode()) {
            			case ContentSharingError.SESSION_INITIATION_DECLINED:
	            			code = GeolocSharing.Error.INVITATION_DECLINED;
	            			break;
	            		case ContentSharingError.MEDIA_SAVING_FAILED:
	            		case ContentSharingError.MEDIA_TRANSFER_FAILED:
	            			code = GeolocSharing.Error.SHARING_FAILED;
	            			break;
	            		default:
	            			code = GeolocSharing.Error.SHARING_FAILED;
	            	}
	            	listeners.getBroadcastItem(i).onSharingError(code);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        GeolocSharingServiceImpl.removeGeolocSharingSession(session.getSessionID());
	    }
    }
    
    /**
     * Content has been transfered
     * 
     * @param geoloc Geoloc info
     */
    public void handleContentTransfered(GeolocPush geoloc) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Geoloc transferred");
			}
			
			// Update rich messaging history
			String msgId = ChatUtils.generateMessageId();
			GeolocMessage geolocMsg = new GeolocMessage(msgId, session.getRemoteContact(), geoloc, false);
			if (session instanceof OriginatingGeolocTransferSession) { 
				RichMessagingHistory.getInstance().addChatMessage(geolocMsg, ChatLog.Message.Direction.OUTGOING);
			} else {
				RichMessagingHistory.getInstance().addChatMessage(geolocMsg, ChatLog.Message.Direction.INCOMING);
			}
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	com.gsma.services.rcs.chat.Geoloc geolocApi = new com.gsma.services.rcs.chat.Geoloc(geoloc.getLabel(),
	        				geoloc.getLatitude(), geoloc.getLongitude(), geoloc.getAltitude(),
	        				geoloc.getExpiration(), geoloc.getAccuracy());
	            	listeners.getBroadcastItem(i).onGeolocShared(geolocApi);
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
