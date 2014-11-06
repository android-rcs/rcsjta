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

import javax2.sip.message.Response;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.gsh.GeolocSharing;
import com.gsma.services.rcs.gsh.GeolocSharing.ReasonCode;
import com.gsma.services.rcs.gsh.IGeolocSharing;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
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

	private final String mSharingId;

	private final IGeolocSharingEventBroadcaster mBroadcaster;

	private final RichcallService mRichcallService;

	private final GeolocSharingServiceImpl mGeolocSharingService;

	/**
	 * Lock used for synchronisation
	 */
	private final Object lock = new Object();

	/**
	 * The logger
	 */
	private final static Logger logger = Logger.getLogger(GeolocSharingImpl.class.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param sharingId Unique Id of Geoloc sharing
	 * @param broadcaster IGeolocSharingEventBroadcaster
	 * @param richcallService RichcallService
	 * @param geolocSharingService GeolocSharingServiceImpl
	 */
	public GeolocSharingImpl(String sharingId, IGeolocSharingEventBroadcaster broadcaster,
			RichcallService richcallService,
			GeolocSharingServiceImpl geolocSharingService) {
		mSharingId = sharingId;
		mBroadcaster = broadcaster;
		mRichcallService = richcallService;
		mGeolocSharingService = geolocSharingService;
	}

	/**
	 * Returns the sharing ID of the geoloc sharing
	 * 
	 * @return Sharing ID
	 */
	public String getSharingId() {
		return mSharingId;
	}
	
	/**
     * Returns the geolocation info
     *
     * @return Geoloc object
     */
	public Geoloc getGeoloc() {
		GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
		if (session == null) {
			/*
			 * TODO : Use persisted storage access GeolocSharing provider
			 * implemented in CR025
			 */
			return null;
		}
		GeolocPush geoloc = session.getGeoloc();
		if (geoloc != null) {
			com.gsma.services.rcs.Geoloc geolocApi = new com.gsma.services.rcs.Geoloc(
					geoloc.getLabel(), geoloc.getLatitude(), geoloc.getLongitude(),
					geoloc.getExpiration(), geoloc.getAccuracy());
			return geolocApi;
		}
		return null;
	}
	
	/**
	 * Returns the remote contact identifier
	 * 
	 * @return ContactId
	 */
	public ContactId getRemoteContact() {
		GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
		if (session == null) {
			/*
			 * TODO : Use persisted storage access GeolocSharing provider
			 * implemented in CR025
			 */
			return null;
		}
		return session.getRemoteContact();
	}
	
	/**
	 * Returns the state of the geoloc sharing
	 * 
	 * @return State
	 */
	public int getState() {
		GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
		if (session == null) {
			/*
			 * TODO : Use persisted storage access GeolocSharing provider
			 * implemented in CR025
			 */
			return -1;
		}
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null && dialogPath.isSessionEstablished()) {
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
		GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
		if (session == null) {
			/*
			 * TODO : Use persisted storage access GeolocSharing provider
			 * implemented in CR025
			 */
			return -1;
		}
		return ReasonCode.UNSPECIFIED;
	}
	
	/**
	 * Returns the direction of the sharing (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see Direction
	 */
	public int getDirection() {
		GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
		if (session == null) {
			/*
			 * TODO : Use persisted storage access GeolocSharing provider
			 * implemented in CR025
			 */
			return -1;
		}
		if (session.isInitiatedByRemote()) {
			return Direction.INCOMING;
		}
		return Direction.OUTGOING;
	}

	/**
	 * Accepts geoloc sharing invitation
	 */
	public void acceptInvitation() {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}
		final GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 implementation
			 */
			throw new IllegalStateException("Session with sharing ID '" + mSharingId
					+ "' not available.");
		}
		// Accept invitation
        new Thread() {
    		public void run() {
    			session.acceptSession();
    		}
    	}.start();
	}
	
	/**
	 * Rejects geoloc sharing invitation
	 */
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}
		final GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 implementation
			 */
			throw new IllegalStateException("Session with sharing ID '" + mSharingId
					+ "' not available.");
		}
		// Reject invitation
        new Thread() {
    		public void run() {
    			session.rejectSession(Response.DECLINE);
    		}
    	}.start();
	}

	/**
	 * Aborts the sharing
	 */
	public void abortSharing() {
		if (logger.isActivated()) {
			logger.info("Cancel session");
		}
		final GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 implementation
			 */
			throw new IllegalStateException("Session with sharing ID '" + mSharingId
					+ "' not available.");
		}
		if (session.isGeolocTransfered()) {
			// Automatically closed after transfer
			return;
		}
		// Abort the session
        new Thread() {
    		public void run() {
    			session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
    		}
    	}.start();		
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
		synchronized (lock) {
			mGeolocSharingService.removeGeolocSharing(mSharingId);

			/* TODO: Will be added with Geoloc sharing content provider CR025. */
			//RichCallHistory.getInstance().setGeolocSharingState(sharingId,
			//		GeolocSharing.State.REJECTED, reasonCode);
			mBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
					mSharingId, GeolocSharing.State.REJECTED, reasonCode);
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
			mBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
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
			mGeolocSharingService.removeGeolocSharing(mSharingId);

			/* TODO: Will be added with Geoloc sharing content provider CR025. */
			//RichCallHistory.getInstance().setGeolocSharingState(sharingId,
			//		GeolocSharing.State.ABORTED, reasonCode);
			mBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
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
		synchronized (lock) {
			mGeolocSharingService.removeGeolocSharing(mSharingId);
			GeolocTransferSession session = mRichcallService
					.getGeolocTransferSession(mSharingId);
			if (!session.isGeolocTransfered()) {
				/* TODO: Will be added with Geoloc sharing content provider CR025. */
				//RichCallHistory.getInstance().setGeolocSharingState(sharingId,
				//		GeolocSharing.State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
				mBroadcaster.broadcastGeolocSharingStateChanged(
						getRemoteContact(), mSharingId, GeolocSharing.State.ABORTED,
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
		synchronized (lock) {
			mGeolocSharingService.removeGeolocSharing(mSharingId);

			/* TODO: Will be added with Geoloc sharing content provider CR025. */
			//RichCallHistory.getInstance().setGeolocSharingState(sharingId,
			//		stateAndReasonCode.getState(), stateAndReasonCode.getReasonCode());
			mBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
					mSharingId, stateAndReasonCode.getState(), stateAndReasonCode.getReasonCode());
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
    		mGeolocSharingService.removeGeolocSharing(mSharingId);

			// Update rich messaging history
			String msgId = IdGenerator.generateMessageID();
			ContactId contact = getRemoteContact();
			// TODO FUSION check display name parameter
			GeolocMessage geolocMsg = new GeolocMessage(msgId, contact, geoloc, false, null);
			GeolocTransferSession session = mRichcallService
					.getGeolocTransferSession(mSharingId);
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

			mBroadcaster.broadcastGeolocSharingStateChanged(contact,
					mSharingId, GeolocSharing.State.TRANSFERRED, ReasonCode.UNSPECIFIED);
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
			mBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
					mSharingId, GeolocSharing.State.ACCEPTING, ReasonCode.UNSPECIFIED);
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
		mBroadcaster.broadcastInvitation(mSharingId);
	}

	@Override
	public void handle180Ringing() {
		synchronized(lock) {
			/* TODO: Will be added with Geoloc sharing content provider CR025. */
			//RichCallHistory.getInstance().setGeolocSharingState(sharingId,
			//		GeolocSharing.State.RINGING, ReasonCode.UNSPECIFIED);
			mBroadcaster.broadcastGeolocSharingStateChanged(getRemoteContact(),
					mSharingId, GeolocSharing.State.RINGING, ReasonCode.UNSPECIFIED);
	    }
	}
}
