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

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.sip.SipService;
import com.gsma.rcs.core.ims.service.sip.SipSessionError;
import com.gsma.rcs.core.ims.service.sip.SipSessionListener;
import com.gsma.rcs.core.ims.service.sip.streaming.GenericSipRtpSession;
import com.gsma.rcs.service.broadcaster.IMultimediaStreamingSessionEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.IMultimediaStreamingSession;
import com.gsma.services.rcs.extension.MultimediaSession.ReasonCode;
import com.gsma.services.rcs.extension.MultimediaSession.State;

import android.content.Intent;

import javax2.sip.message.Response;

/**
 * Multimedia streaming session
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaStreamingSessionImpl extends IMultimediaStreamingSession.Stub implements
        SipSessionListener {

    private final String mSessionId;

    private final IMultimediaStreamingSessionEventBroadcaster mBroadcaster;

    private final SipService mSipService;

    private final MultimediaSessionServiceImpl mMultimediaSessionService;

    /**
     * mLock used for synchronization
     */
    private final Object mLock = new Object();

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
            IMultimediaStreamingSessionEventBroadcaster broadcaster, SipService sipService,
            MultimediaSessionServiceImpl multimediaSessionService) {
        mSessionId = sessionId;
        mBroadcaster = broadcaster;
        mSipService = sipService;
        mMultimediaSessionService = multimediaSessionService;
    }

    private void handleSessionRejected(ReasonCode reasonCode, ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session rejected; reasonCode=" + reasonCode + ".");
        }
        String sessionId = getSessionId();
        synchronized (mLock) {
            mMultimediaSessionService.removeMultimediaStreaming(sessionId);

            mBroadcaster.broadcastStateChanged(contact, sessionId, State.REJECTED, reasonCode);
        }
    }

    private void removeSessionAndBroadcast(ContactId contact, State state, ReasonCode reasonCode) {
        mMultimediaSessionService.removeMultimediaStreaming(mSessionId);
        mBroadcaster.broadcastStateChanged(contact, mSessionId, state, reasonCode);
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
             * TODO: Throw correct exception as part of CR037 as persisted storage not available for
             * this service!
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
             * TODO: Throw correct exception as part of CR037 as persisted storage not available for
             * this service!
             */
            throw new IllegalStateException(
                    "Unable to retrieve state since session with session ID '" + mSessionId
                            + "' not available.");
        }
        SipDialogPath dialogPath = session.getDialogPath();
        if (dialogPath != null && dialogPath.isSessionEstablished()) {
            return State.STARTED.toInt();
        } else if (session.isInitiatedByRemote()) {
            if (session.isSessionAccepted()) {
                return State.ACCEPTING.toInt();
            }
            return State.INVITED.toInt();
        }
        return State.INITIATING.toInt();
    }

    /**
     * Returns the reason code of the state of the multimedia streaming session
     * 
     * @return ReasonCode
     */
    public int getReasonCode() {
        GenericSipRtpSession session = mSipService.getGenericSipRtpSession(mSessionId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 as persisted storage not available for
             * this service!
             */
            throw new IllegalStateException(
                    "Unable to retrieve reason code since session with session ID '" + mSessionId
                            + "' not available.");
        }
        return ReasonCode.UNSPECIFIED.toInt();
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
             * TODO: Throw correct exception as part of CR037 as persisted storage not available for
             * this service!
             */
            throw new IllegalStateException(
                    "Unable to retrieve direction since session with session ID '" + mSessionId
                            + "' not available.");
        }
        if (session.isInitiatedByRemote()) {
            return Direction.INCOMING.toInt();
        }
        return Direction.OUTGOING.toInt();
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
             * TODO: Throw correct exception as part of CR037 as persisted storage not available for
             * this service!
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
    public void rejectInvitation() throws ServerApiException {
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
    public void abortSession() throws ServerApiException {
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
                session.abortSession(TerminationReason.TERMINATION_BY_USER);
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
    public void handleSessionStarted(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session started");
        }
        synchronized (mLock) {
            mBroadcaster.broadcastStateChanged(contact, mSessionId, State.STARTED,
                    ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Session has been aborted
     * 
     * @param reason Termination reason
     */
    public void handleSessionAborted(ContactId contact, TerminationReason reason) {
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Session aborted (terminationReason ").append(reason)
                    .append(")").toString());
        }
        synchronized (mLock) {
            switch (reason) {
                case TERMINATION_BY_SYSTEM:
                case TERMINATION_BY_TIMEOUT:
                    removeSessionAndBroadcast(contact, State.ABORTED, ReasonCode.ABORTED_BY_SYSTEM);
                    break;
                case TERMINATION_BY_USER:
                    removeSessionAndBroadcast(contact, State.ABORTED, ReasonCode.ABORTED_BY_USER);
                    break;
                case TERMINATION_BY_CONNECTION_LOST:
                    removeSessionAndBroadcast(contact, State.FAILED, ReasonCode.FAILED_SESSION);
                    break;
                case TERMINATION_BY_REMOTE:
                    removeSessionAndBroadcast(contact, State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown TerminationReason=".concat(String
                            .valueOf(reason)));
            }
        }
    }

    /**
     * Session error
     * 
     * @param contact Remote contact
     * @param error Error
     */
    public void handleSessionError(ContactId contact, SipSessionError error) {
        if (logger.isActivated()) {
            logger.info("Session error " + error.getErrorCode());
        }
        synchronized (mLock) {
            mMultimediaSessionService.removeMultimediaStreaming(mSessionId);

            switch (error.getErrorCode()) {
                case SipSessionError.SESSION_INITIATION_DECLINED:
                    mBroadcaster.broadcastStateChanged(contact, mSessionId, State.REJECTED,
                            ReasonCode.REJECTED_BY_REMOTE);
                    break;
                case SipSessionError.MEDIA_FAILED:
                    mBroadcaster.broadcastStateChanged(contact, mSessionId, State.FAILED,
                            ReasonCode.FAILED_MEDIA);
                    break;
                case SipSessionError.SESSION_INITIATION_CANCELLED:
                case SipSessionError.SESSION_INITIATION_FAILED:
                    mBroadcaster.broadcastStateChanged(contact, mSessionId, State.FAILED,
                            ReasonCode.FAILED_INITIATION);
                    break;
                default:
                    mBroadcaster.broadcastStateChanged(contact, mSessionId, State.FAILED,
                            ReasonCode.FAILED_SESSION);
            }
        }
    }

    /**
     * Receive data
     * 
     * @param data Data
     * @param contact
     */
    public void handleReceiveData(ContactId contact, byte[] data) {
        synchronized (mLock) {
            mBroadcaster.broadcastPayloadReceived(contact, mSessionId, data);
        }
    }

    @Override
    public void handleSessionAccepted(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Accepting session");
        }
        synchronized (mLock) {
            mBroadcaster.broadcastStateChanged(contact, mSessionId, State.ACCEPTING,
                    ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleSessionRejectedByUser(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_USER, contact);
    }

    /*
     * TODO : Fix reasoncode mapping between rejected_by_timeout and rejected_by_inactivity.
     */
    @Override
    public void handleSessionRejectedByTimeout(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_INACTIVITY, contact);
    }

    @Override
    public void handleSessionRejectedByRemote(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_REMOTE, contact);
    }

    @Override
    public void handleSessionInvited(ContactId contact, Intent sessionInvite) {
        if (logger.isActivated()) {
            logger.info("Invited to multimedia streaming session");
        }
        mBroadcaster.broadcastInvitation(mSessionId, sessionInvite);
    }

    @Override
    public void handle180Ringing(ContactId contact) {
        synchronized (mLock) {
            mBroadcaster.broadcastStateChanged(contact, mSessionId, State.RINGING,
                    ReasonCode.UNSPECIFIED);
        }
    }
}
