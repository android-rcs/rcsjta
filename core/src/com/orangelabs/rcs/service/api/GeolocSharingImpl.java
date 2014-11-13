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

import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.gsh.GeolocSharing;
import com.gsma.services.rcs.gsh.GeolocSharing.ReasonCode;
import com.gsma.services.rcs.gsh.IGeolocSharing;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSessionListener;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.OriginatingGeolocTransferSession;
import com.orangelabs.rcs.provider.sharing.GeolocSharingStateAndReasonCode;
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
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null  && dialogPath.isSessionEstablished()) {
				return GeolocSharing.State.STARTED;

		} else if (session.isInitiatedByRemote()) {
			if (session.isSessionAccepted()) {
				return GeolocSharing.State.ACCEPTING;
			}

			return GeolocSharing.State.INVITED;
		}

		return GeolocSharing.State.INITIATED;
	}

	/**
	 * Returns the reason code of the state of the geoloc sharing
	 *
	 * @return ReasonCode
	 */
	public int getReasonCode() {
		return ReasonCode.UNSPECIFIED;
	}
	
	/**
	 * Returns the direction of the sharing (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see Direction
	 */
	public int getDirection() {
		if (session.isInitiatedByRemote()) {
			return Direction.INCOMING;
		} else {
			return Direction.OUTGOING;
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

	private int sessionAbortedReasonToReasonCode(int sessionAbortedReason) {
		switch (sessionAbortedReason) {
			case ImsServiceSession.TERMINATION_BY_TIMEOUT:
			case ImsServiceSession.TERMINATION_BY_SYSTEM:
				return ReasonCode.ABORTED_BY_SYSTEM;
			case ImsServiceSession.TERMINATION_BY_USER:
				return ReasonCode.ABORTED_BY_USER;
			default:
				throw new IllegalArgumentException(
						"Unknown reason in GeolocSharingImpl.sessionAbortedReasonToReasonCode; sessionAbortedReason="
								+ sessionAbortedReason + "!");
		}
	}

	private GeolocSharingStateAndReasonCode toStateAndReasonCode(ContentSharingError error) {
		switch (error.getErrorCode()) {
			case ContentSharingError.SESSION_INITIATION_FAILED:
				return new GeolocSharingStateAndReasonCode(GeolocSharing.State.FAILED,
						ReasonCode.FAILED_INITIATION);
			case ContentSharingError.SESSION_INITIATION_CANCELLED:
			case ContentSharingError.SESSION_INITIATION_DECLINED:
				return new GeolocSharingStateAndReasonCode(GeolocSharing.State.REJECTED,
						ReasonCode.REJECTED_BY_REMOTE);
			case ContentSharingError.MEDIA_SAVING_FAILED:
			case ContentSharingError.MEDIA_TRANSFER_FAILED:
			case ContentSharingError.MEDIA_STREAMING_FAILED:
			case ContentSharingError.UNSUPPORTED_MEDIA_TYPE:
				return new GeolocSharingStateAndReasonCode(GeolocSharing.State.FAILED,
						ReasonCode.FAILED_SHARING);
			default:
				throw new IllegalArgumentException(
						"Unknown reason in GeolocSharingImpl.toStateAndReasonCode; error=" + error
								+ "!");
		}
	}

	private void handleSessionRejected(int reasonCode) {
		if (logger.isActivated()) {
			logger.info("Session rejected; reasonCode=" + reasonCode + ".");
		}
		String sharingId = getSharingId();
		synchronized (lock) {
			GeolocSharingServiceImpl.removeGeolocSharingSession(sharingId);

			/* TODO: Will be added with Geoloc sharing content provider CR025. */
			//RichCallHistory.getInstance().setGeolocSharingState(sharingId,
			//		GeolocSharing.State.REJECTED, reasonCode);
			mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
					sharingId, GeolocSharing.State.REJECTED, reasonCode);
		}
	}

	/**
	 * Session is started
	 */
    public void handleSessionStarted() {
		if (logger.isActivated()) {
			logger.info("Session started");
		}
    	synchronized(lock) {
			/* TODO: Will be added with Geoloc sharing content provider CR025. */
			//RichCallHistory.getInstance().setGeolocSharingState(sharingId,
			//		GeolocSharing.State.STARTED, ReasonCode.UNSPECIFIED);
			mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
					getSharingId(), GeolocSharing.State.STARTED, ReasonCode.UNSPECIFIED);
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
		int reasonCode = sessionAbortedReasonToReasonCode(reason);
		synchronized (lock) {
			GeolocSharingServiceImpl.removeGeolocSharingSession(session.getSessionID());

			/* TODO: Will be added with Geoloc sharing content provider CR025. */
			//RichCallHistory.getInstance().setGeolocSharingState(sharingId,
			//		GeolocSharing.State.ABORTED, reasonCode);
			mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
					getSharingId(), GeolocSharing.State.ABORTED, reasonCode);
		}
	}

	/**
	 * Session has been terminated by remote
	 */
	public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session terminated by remote");
		}
		String sharingId = getSharingId();
		synchronized (lock) {
			GeolocSharingServiceImpl.removeGeolocSharingSession(sharingId);
			if (!session.isGeolocTransfered()) {
				/* TODO: Will be added with Geoloc sharing content provider CR025. */
				//RichCallHistory.getInstance().setGeolocSharingState(sharingId,
				//		GeolocSharing.State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
				mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(
						getRemoteContact(), sharingId, GeolocSharing.State.ABORTED,
						ReasonCode.ABORTED_BY_REMOTE);

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
		GeolocSharingStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
		String sharingId = getSharingId();
		synchronized (lock) {
			GeolocSharingServiceImpl.removeGeolocSharingSession(sharingId);

			/* TODO: Will be added with Geoloc sharing content provider CR025. */
			//RichCallHistory.getInstance().setGeolocSharingState(sharingId,
			//		stateAndReasonCode.getState(), stateAndReasonCode.getReasonCode());
			mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
					sharingId, stateAndReasonCode.getState(), stateAndReasonCode.getReasonCode());
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
		String sharingId = getSharingId();
    	synchronized(lock) {
			GeolocSharingServiceImpl.removeGeolocSharingSession(sharingId);

			// Update rich messaging history
			String msgId = IdGenerator.generateMessageID();
			ContactId contact = getRemoteContact();
			// TODO FUSION check display name parameter
			GeolocMessage geolocMsg = new GeolocMessage(msgId, contact, geoloc, false, null);
			if (session instanceof OriginatingGeolocTransferSession) { 
				MessagingLog.getInstance()
						.addOutgoingOneToOneChatMessage(geolocMsg,
								ChatLog.Message.Status.Content.SENT,
								ChatLog.Message.ReasonCode.UNSPECIFIED);
				/*
				 * TODO: Important notice. This will be changed with CR025, as
				 * geoloc sharing object will not be persisted in the message
				 * log.
				 */
			} else {
				MessagingLog.getInstance().addIncomingOneToOneChatMessage(geolocMsg);
			}

			mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(contact,
					sharingId, GeolocSharing.State.TRANSFERRED, ReasonCode.UNSPECIFIED);
	    }
    }

	@Override
	public void handleSessionAccepted() {
		if (logger.isActivated()) {
			logger.info("Accepting sharing");
		}
		synchronized (lock) {
			/* TODO: Will be added with Geoloc sharing content provider CR025. */
			// RichCallHistory.getInstance().setGeolocSharingState(sharingId,
			// GeolocSharing.State.ACCEPTING, ReasonCode.UNSPECIFIED);
			mGeolocSharingEventBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
					getSharingId(), GeolocSharing.State.ACCEPTING, ReasonCode.UNSPECIFIED);
		}
	}

	@Override
	public void handleSessionRejectedByUser() {
		handleSessionRejected(ReasonCode.REJECTED_BY_USER);
	}

	@Override
	public void handleSessionRejectedByTimeout() {
		handleSessionRejected(ReasonCode.REJECTED_TIME_OUT);
	}

	@Override
	public void handleSessionRejectedByRemote() {
		handleSessionRejected(ReasonCode.REJECTED_BY_REMOTE);
	}

	@Override
	public void handleSessionInvited() {
		mGeolocSharingEventBroadcaster.broadcastInvitation(getSharingId());
	}
}
