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

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.gsh.GeolocSharing;
import com.gsma.services.rcs.gsh.IGeolocSharing;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSessionListener;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.OriginatingGeolocTransferSession;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.service.broadcaster.IGeolocSharingEventBroadcaster;
import com.orangelabs.rcs.utils.IdGenerator;
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
	 * Lock used for synchronisation
	 */
	private final Object lock = new Object();

	private final IGeolocSharingEventBroadcaster mGeolocSharingEventBroadcaster;

	/**
	 * The logger
	 */
	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Constructor
	 *
	 * @param session Session
	 * @param broadcaster IGeolocSharingEventBroadcaster
	 */
	public GeolocSharingImpl(GeolocTransferSession session,
			IGeolocSharingEventBroadcaster broadcaster) {
		this.session = session;
		mGeolocSharingEventBroadcaster = broadcaster;

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
					geoloc.getLatitude(), geoloc.getLongitude(),
					geoloc.getExpiration(), geoloc.getAccuracy());
	    	return geolocApi;
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the remote contact identifier
	 * 
	 * @return ContactId
	 */
	public ContactId getRemoteContact() {
		return session.getRemoteContact();
	}
	
	/**
	 * Returns the state of the geoloc sharing
	 * 
	 * @return State 
	 */
	public int getState() {
		int result = GeolocSharing.State.INACTIVE;
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
		if (session.isInitiatedByRemote()) {
			return GeolocSharing.Direction.INCOMING;
		} else {
			return GeolocSharing.Direction.OUTGOING;
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
			mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
					getSharingId(), GeolocSharing.State.STARTED);
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
			mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
					getSharingId(), GeolocSharing.State.ABORTED);
	
	        // Remove session from the list
	        GeolocSharingServiceImpl.removeGeolocSharingSession(session.getSessionID());
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
			// Check if the geoloc has been transferred or not
	  		if (session.isGeolocTransfered()) {
		        // Remove session from the list
	  			GeolocSharingServiceImpl.removeGeolocSharingSession(session.getSessionID());
	  		} else {
				// Notify event listeners
				mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
						getSharingId(), GeolocSharing.State.ABORTED);

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
		if (logger.isActivated()) {
			logger.info("Sharing error " + error.getErrorCode());
		}
    	synchronized(lock) {
			if (error.getErrorCode() == ContentSharingError.SESSION_INITIATION_CANCELLED) {
				// Do nothing here, this is an aborted event
				return;
			}
			// Notify event listeners
			switch (error.getErrorCode()) {
				case ContentSharingError.SESSION_INITIATION_DECLINED:
					// TODO : Handle reason code in CR009
					mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(), getSharingId(), GeolocSharing.State.FAILED /*, GeolocSharing.Error.INVITATION_DECLINED*/);
					break;
				case ContentSharingError.MEDIA_SAVING_FAILED:
				case ContentSharingError.MEDIA_TRANSFER_FAILED:
					// TODO : Handle reason code in CR009
					mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(), getSharingId(), GeolocSharing.State.FAILED /*, GeolocSharing.Error.SHARING_FAILED*/);
					break;
				default:
					// TODO : Handle reason code in CR009
					mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(), getSharingId(), GeolocSharing.State.FAILED /*, GeolocSharing.Error.SHARING_FAILED*/);
			}
	
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
		if (logger.isActivated()) {
			logger.info("Geoloc transferred");
		}
    	synchronized(lock) {
			// Update rich messaging history
			String msgId = IdGenerator.generateMessageID();
			ContactId contact = getRemoteContact();
			// TODO FUSION check display name parameter
			GeolocMessage geolocMsg = new GeolocMessage(msgId, contact, geoloc, false, null);
			if (session instanceof OriginatingGeolocTransferSession) { 
				MessagingLog.getInstance().addChatMessage(geolocMsg, ChatLog.Message.Direction.OUTGOING);
			} else {
				MessagingLog.getInstance().addChatMessage(geolocMsg, ChatLog.Message.Direction.INCOMING);
			}

			// Notify event listeners
			mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(contact, getSharingId(), GeolocSharing.State.TRANSFERRED);
	    }
    }
}
