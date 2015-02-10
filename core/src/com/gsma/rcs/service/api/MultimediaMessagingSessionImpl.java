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
import com.gsma.rcs.core.ims.service.sip.messaging.GenericSipMsrpSession;
import com.gsma.rcs.service.broadcaster.IMultimediaMessagingSessionEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.IMultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaSession.ReasonCode;
import com.gsma.services.rcs.extension.MultimediaSession.State;

import android.content.Intent;

import javax2.sip.message.Response;

/**
 * Multimedia messaging session
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaMessagingSessionImpl extends IMultimediaMessagingSession.Stub implements
        SipSessionListener {

    private final String mSessionId;

    private final IMultimediaMessagingSessionEventBroadcaster mBroadcaster;

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
     * @param sessionId Session ID
     * @param broadcaster IMultimediaMessagingSessionEventBroadcaster
     * @param sipService SipService
     * @param multimediaSessionService MultimediaSessionServiceImpl
     */
    public MultimediaMessagingSessionImpl(String sessionId,
            IMultimediaMessagingSessionEventBroadcaster broadcaster, SipService sipService,
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
            mBroadcaster.broadcastStateChanged(contact, sessionId, State.REJECTED, reasonCode);

            mMultimediaSessionService.removeMultimediaMessaging(sessionId);
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
        GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
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
        GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
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
     * Returns the reason code of the state of the multimedia messaging session
     * 
     * @return ReasonCode
     */
    public int getReasonCode() {
        GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
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
        GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
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
        GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
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
        final GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new ServerApiException("Session with session ID '" + mSessionId
                    + "' not available.");
        }

        // Test API permission
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

        final GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new ServerApiException("Session with session ID '" + mSessionId
                    + "' not available.");
        }

        // Test API permission
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
        final GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new ServerApiException("Session with session ID '" + mSessionId
                    + "' not available.");
        }

        // Test API permission
        ServerApiUtils.testApiExtensionPermission(session.getServiceId());

        // Abort the session
        new Thread() {
            public void run() {
                session.abortSession(TerminationReason.TERMINATION_BY_USER);
            }
        }.start();
    }

    /**
     * Sends a message in real time
     * 
     * @param content Message content
     * @throws ServerApiException
     */
    public void sendMessage(byte[] content) throws ServerApiException {
        GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new ServerApiException("Session with session ID '" + mSessionId
                    + "' not available.");
        }

        // Test API permission
        ServerApiUtils.testApiExtensionPermission(session.getServiceId());

        /* TODO: This exception handling is not correct. Will be fixed CR037. */
        // Do not consider max message size if null
        if (session.getMaxMessageSize() != 0 && content.length > session.getMaxMessageSize()) {
            throw new ServerApiException("Max message length exceeded!");
        }
        /* TODO: This exception handling is not correct. Will be fixed CR037. */
        if (!session.sendMessage(content)) {
            throw new ServerApiException("Unable to send message!");
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
        ReasonCode reasonCode = mMultimediaSessionService.sessionAbortedReasonToReasonCode(reason);
        synchronized (mLock) {
            mMultimediaSessionService.removeMultimediaMessaging(mSessionId);
            mBroadcaster.broadcastStateChanged(contact, mSessionId, State.ABORTED, reasonCode);
        }
    }

    /**
     * Session has been terminated by remote
     */
    public void handleSessionTerminatedByRemote(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session terminated by remote");
        }
        synchronized (mLock) {
            mMultimediaSessionService.removeMultimediaMessaging(mSessionId);
            mBroadcaster.broadcastStateChanged(contact, mSessionId, State.ABORTED,
                    ReasonCode.REJECTED_BY_REMOTE);
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
            mMultimediaSessionService.removeMultimediaMessaging(mSessionId);

            switch (error.getErrorCode()) {
                case SipSessionError.SESSION_INITIATION_DECLINED:
                    mBroadcaster.broadcastStateChanged(contact, mSessionId, State.REJECTED,
                            ReasonCode.REJECTED_BY_REMOTE);
                    break;
                case SipSessionError.MEDIA_FAILED:
                    mBroadcaster.broadcastStateChanged(contact, mSessionId, State.FAILED,
                            ReasonCode.FAILED_MEDIA);
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
            mBroadcaster.broadcastMessageReceived(contact, mSessionId, data);
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
            logger.info("Invited to multimedia messaging session");
        }
        mBroadcaster.broadcastInvitation(getSessionId(), sessionInvite);
    }

    @Override
    public void handle180Ringing(ContactId contact) {
        synchronized (mLock) {
            mBroadcaster.broadcastStateChanged(contact, mSessionId, State.RINGING,
                    ReasonCode.UNSPECIFIED);
        }
    }

}
