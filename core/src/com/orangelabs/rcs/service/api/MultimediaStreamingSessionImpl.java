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

import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.extension.IMultimediaStreamingSession;
import com.gsma.services.rcs.extension.MultimediaSession;
import com.gsma.services.rcs.extension.MultimediaSession.ReasonCode;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.sip.SipService;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionError;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionListener;
import com.orangelabs.rcs.core.ims.service.sip.streaming.GenericSipRtpSession;
import com.orangelabs.rcs.core.ims.service.sip.streaming.TerminatingSipRtpSession;
import com.orangelabs.rcs.service.broadcaster.IMultimediaStreamingSessionEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Multimedia streaming session
 *
 * @author Jean-Marc AUFFRET
 */
public class MultimediaStreamingSessionImpl extends IMultimediaStreamingSession.Stub implements SipSessionListener {

	private final String mSessionId;

	private final IMultimediaStreamingSessionEventBroadcaster mBroadcaster;

	private final SipService mSipService;

	private final MultimediaSessionServiceImpl mMultimediaSessionService;

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
	 * @param sessionId Session Id
	 * @param broadcaster IMultimediaStreamingSessionEventBroadcaster
	 * @param sipService SipService
	 * @param multimediaSessionService MultimediaSessionServiceImpl
	 */
	public MultimediaStreamingSessionImpl(String sessionId,
			IMultimediaStreamingSessionEventBroadcaster broadcaster,
			SipService sipService,
			MultimediaSessionServiceImpl multimediaSessionService) {
		mSessionId = sessionId;
		mBroadcaster = broadcaster;
		mSipService = sipService;
		mMultimediaSessionService = multimediaSessionService;
	}

	private void handleSessionRejected(int reasonCode) {
		if (logger.isActivated()) {
			logger.info("Session rejected; reasonCode=" + reasonCode + ".");
		}
		String sessionId = getSessionId();
		synchronized (lock) {
			mMultimediaSessionService.removeMultimediaStreaming(sessionId);

			mBroadcaster.broadcastStateChanged(
					getRemoteContact(), sessionId, MultimediaSession.State.REJECTED, reasonCode);
		}
	}

    /**
	 * Returns the session ID of the multimedia session
	 * 
	 * @return Session ID
	 */
	public String getSessionId() {
		return mSessionId;
	}

	/**
	 * Returns the remote contact ID
	 * 
	 * @return ContactId
	 */
	public ContactId getRemoteContact() {
		GenericSipRtpSession session = mSipService.getGenericSipRtpSession(mSessionId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 as persisted storage not
			 * available for this service!
			 */
			throw new IllegalStateException(
					"Unable to retrieve contact since session with session ID '" + mSessionId
							+ "' not available.");
		}

		return session.getRemoteContact();
	}
	
	/**
	 * Returns the state of the session
	 * 
	 * @return State
	 */
	public int getState() {
		GenericSipRtpSession session = mSipService.getGenericSipRtpSession(mSessionId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 as persisted storage not
			 * available for this service!
			 */
			throw new IllegalStateException(
					"Unable to retrieve state since session with session ID '" + mSessionId
							+ "' not available.");
		}
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null && dialogPath.isSessionEstablished()) {
			return MultimediaSession.State.STARTED;
		} else if (session.isInitiatedByRemote()) {
			if (session.isSessionAccepted()) {
				return MultimediaSession.State.ACCEPTING;
			}
			return MultimediaSession.State.INVITED;
		}
		return MultimediaSession.State.INITIATING;
	}

	/**
	 * Returns the reason code of the state of the multimedia streaming session
	 *
	 * @return ReasonCode
	 */
	public int getReasonCode() {
		return ReasonCode.UNSPECIFIED;
	}

	/**
	 * Returns the direction of the session (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see Direction
	 */
	public int getDirection() {
		GenericSipRtpSession session = mSipService.getGenericSipRtpSession(mSessionId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 as persisted storage not
			 * available for this service!
			 */
			throw new IllegalStateException(
					"Unable to retrieve direction since session with session ID '" + mSessionId
							+ "' not available.");
		}
		if (session.isInitiatedByRemote()) {
			return Direction.INCOMING;
		}
		return Direction.OUTGOING;
	}
	
	/**
	 * Returns the service ID
	 * 
	 * @return Service ID
	 */
	public String getServiceId() {
		GenericSipRtpSession session = mSipService.getGenericSipRtpSession(mSessionId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 as persisted storage not
			 * available for this service!
			 */
			throw new IllegalStateException(
					"Unable to retrieve service Id since session with session ID '" + mSessionId
							+ "' not available.");
		}

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
		final GenericSipRtpSession session = mSipService.getGenericSipRtpSession(mSessionId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 implementation
			 */
			throw new ServerApiException("Session with session ID '" + mSessionId
					+ "' not available.");
		}

		// Test security extension
		ServerApiUtils.testApiExtensionPermission(session.getServiceId());

		// Accept invitation
        new Thread() {
    		public void run() {
    			session.acceptSession();
    		}
    	}.start();
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
		final GenericSipRtpSession session = mSipService.getGenericSipRtpSession(mSessionId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 implementation
			 */
			throw new ServerApiException("Session with session ID '" + mSessionId
					+ "' not available.");
		}

		// Test security extension
		ServerApiUtils.testApiExtensionPermission(session.getServiceId());

		// Reject invitation
        new Thread() {
    		public void run() {
    			session.rejectSession(Response.DECLINE);
    		}
    	}.start();
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
		final GenericSipRtpSession session = mSipService.getGenericSipRtpSession(mSessionId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 implementation
			 */
			throw new ServerApiException("Session with session ID '" + mSessionId
					+ "' not available.");
		}

		// Test security extension
		ServerApiUtils.testApiExtensionPermission(session.getServiceId());

		// Abort the session
        new Thread() {
    		public void run() {
    			session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
    		}
    	}.start();
	}

	/**
	 * Sends a payload in real time
	 * 
	 * @param content Payload content
	 * @throws ServerApiException
	 */
	public void sendPayload(byte[] content) throws ServerApiException {
		GenericSipRtpSession session = mSipService.getGenericSipRtpSession(mSessionId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 implementation
			 */
			throw new ServerApiException("Session with session ID '" + mSessionId
					+ "' not available.");
		}

		// Test security extension
		ServerApiUtils.testApiExtensionPermission(session.getServiceId());

		/* TODO: This exception handling is not correct. Will be fixed CR037. */
		if (!session.sendPlayload(content)) {
			throw new ServerApiException("Unable to send payload!");
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
		synchronized (lock) {
			mBroadcaster.broadcastStateChanged(
					getRemoteContact(), getSessionId(), MultimediaSession.State.STARTED,
					ReasonCode.UNSPECIFIED);
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
		synchronized (lock) {
			mMultimediaSessionService.removeMultimediaStreaming(mSessionId);

			mBroadcaster.broadcastStateChanged(
					getRemoteContact(), mSessionId, MultimediaSession.State.ABORTED,
					ReasonCode.UNSPECIFIED);
		}
	}

	/**
	 * Session has been terminated by remote
	 */
	public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session terminated by remote");
		}
		String mSessionId = getSessionId();
		synchronized (lock) {
			mMultimediaSessionService.removeMultimediaStreaming(mSessionId);

			mBroadcaster.broadcastStateChanged(
					getRemoteContact(), mSessionId, MultimediaSession.State.ABORTED,
					ReasonCode.UNSPECIFIED);
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
		synchronized (lock) {
			mMultimediaSessionService.removeMultimediaStreaming(mSessionId);

			switch (error.getErrorCode()) {
				case SipSessionError.SESSION_INITIATION_DECLINED:
					mBroadcaster
							.broadcastStateChanged(getRemoteContact(),
									mSessionId, MultimediaSession.State.REJECTED,
									ReasonCode.REJECTED_BY_REMOTE);
					break;
				case SipSessionError.MEDIA_FAILED:
					mBroadcaster
							.broadcastStateChanged(getRemoteContact(),
									mSessionId, MultimediaSession.State.FAILED,
									ReasonCode.FAILED_MEDIA);
					break;
				default:
					mBroadcaster
							.broadcastStateChanged(getRemoteContact(),
									mSessionId, MultimediaSession.State.FAILED,
									ReasonCode.FAILED_SESSION);
			}
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
			mBroadcaster.broadcastPayloadReceived(getRemoteContact(),
					mSessionId, data);
		}
    }

	@Override
	public void handleSessionAccepted() {
		if (logger.isActivated()) {
			logger.info("Accepting session");
		}
		synchronized (lock) {
			mBroadcaster.broadcastStateChanged(
					getRemoteContact(), mSessionId, MultimediaSession.State.ACCEPTING,
					ReasonCode.UNSPECIFIED);
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
		if (logger.isActivated()) {
			logger.info("Invited to multimedia streaming session");
		}
		GenericSipRtpSession session = mSipService.getGenericSipRtpSession(mSessionId);
		mBroadcaster.broadcastInvitation(
				mSessionId, ((TerminatingSipRtpSession)session).getSessionInvite());
	}

	@Override
	public void handle180Ringing() {
		synchronized (lock) {
			mBroadcaster.broadcastStateChanged(
					getRemoteContact(), getSessionId(), MultimediaSession.State.RINGING,
					ReasonCode.UNSPECIFIED);
		}
	}
}
