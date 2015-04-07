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

    private final MultimediaSessionStorageAccessor mMultimediaSessionStorageAccessor;

    private final Object mLock = new Object();

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    /**
     * Constructor
     * 
     * @param sessionId Session ID
     * @param broadcaster IMultimediaMessagingSessionEventBroadcaster
     * @param sipService SipService
     * @param multimediaSessionService MultimediaSessionServiceImpl
     * @param direction
     * @param contact
     * @param serviceId
     */
    public MultimediaMessagingSessionImpl(String sessionId,
            IMultimediaMessagingSessionEventBroadcaster broadcaster, SipService sipService,
            MultimediaSessionServiceImpl multimediaSessionService, Direction direction,
            ContactId contact, String serviceId) {
        mSessionId = sessionId;
        mBroadcaster = broadcaster;
        mSipService = sipService;
        mMultimediaSessionService = multimediaSessionService;
        mMultimediaSessionStorageAccessor = new MultimediaSessionStorageAccessor(direction,
                contact, serviceId);
    }

    private void handleSessionRejected(ReasonCode reasonCode, ContactId contact) {
        if (mLogger.isActivated()) {
            mLogger.info("Session rejected; reasonCode=" + reasonCode + ".");
        }
        String sessionId = getSessionId();
        synchronized (mLock) {
            mMultimediaSessionService.removeMultimediaMessaging(sessionId);
            mMultimediaSessionStorageAccessor.setState(State.REJECTED);
            mMultimediaSessionStorageAccessor.setReasonCode(reasonCode);
            mBroadcaster.broadcastStateChanged(contact, sessionId, State.REJECTED, reasonCode);
        }
    }

    private void removeSessionAndBroadcast(ContactId contact, State state, ReasonCode reasonCode) {
        mMultimediaSessionService.removeMultimediaMessaging(mSessionId);
        mMultimediaSessionStorageAccessor.setState(state);
        mMultimediaSessionStorageAccessor.setReasonCode(reasonCode);
        mBroadcaster.broadcastStateChanged(contact, mSessionId, state, reasonCode);
    }

    private void setStateAndReasonThenBroadcast(ContactId contact, State state, ReasonCode reason) {
        mMultimediaSessionStorageAccessor.setState(state);
        mMultimediaSessionStorageAccessor.setReasonCode(reason);
        mBroadcaster.broadcastStateChanged(contact, mSessionId, state, reason);
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
            return mMultimediaSessionStorageAccessor.getRemoteContact();
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
            return mMultimediaSessionStorageAccessor.getState().toInt();
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
            return mMultimediaSessionStorageAccessor.getReasonCode().toInt();
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
            return mMultimediaSessionStorageAccessor.getDirection().toInt();
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
            return mMultimediaSessionStorageAccessor.getServiceId();
        }
        return session.getServiceId();
    }

    /**
     * Accepts session invitation
     * 
     * @throws ServerApiException
     */
    public void acceptInvitation() throws ServerApiException {
        if (mLogger.isActivated()) {
            mLogger.info("Accept session invitation");
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
        if (mLogger.isActivated()) {
            mLogger.info("Reject session invitation");
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
        if (mLogger.isActivated()) {
            mLogger.info("Cancel session");
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
        if (mLogger.isActivated()) {
            mLogger.info("Session started");
        }
        synchronized (mLock) {
            setStateAndReasonThenBroadcast(contact, State.STARTED, ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Session has been aborted
     * 
     * @param reason Termination reason
     */
    public void handleSessionAborted(ContactId contact, TerminationReason reason) {
        if (mLogger.isActivated()) {
            mLogger.info(new StringBuilder("Session aborted (terminationReason ").append(reason)
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
        if (mLogger.isActivated()) {
            mLogger.info("Session error " + error.getErrorCode());
        }
        synchronized (mLock) {
            mMultimediaSessionService.removeMultimediaMessaging(mSessionId);

            switch (error.getErrorCode()) {
                case SipSessionError.SESSION_INITIATION_DECLINED:
                    setStateAndReasonThenBroadcast(contact, State.REJECTED,
                            ReasonCode.REJECTED_BY_REMOTE);
                    break;
                case SipSessionError.MEDIA_FAILED:
                    setStateAndReasonThenBroadcast(contact, State.FAILED, ReasonCode.FAILED_MEDIA);
                    break;
                case SipSessionError.SESSION_INITIATION_CANCELLED:
                case SipSessionError.SESSION_INITIATION_FAILED:
                    setStateAndReasonThenBroadcast(contact, State.FAILED,
                            ReasonCode.FAILED_INITIATION);
                    break;
                default:
                    setStateAndReasonThenBroadcast(contact, State.FAILED, ReasonCode.FAILED_SESSION);
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
        if (mLogger.isActivated()) {
            mLogger.info("Accepting session");
        }
        synchronized (mLock) {
            setStateAndReasonThenBroadcast(contact, State.ACCEPTING, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleSessionRejectedByUser(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_USER, contact);
    }

    @Override
    public void handleSessionRejectedByTimeout(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_TIMEOUT, contact);
    }

    @Override
    public void handleSessionRejectedByRemote(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_REMOTE, contact);
    }

    @Override
    public void handleSessionInvited(ContactId contact, Intent sessionInvite) {
        if (mLogger.isActivated()) {
            mLogger.info("Invited to multimedia messaging session");
        }
        mBroadcaster.broadcastInvitation(getSessionId(), sessionInvite);
    }

    @Override
    public void handle180Ringing(ContactId contact) {
        synchronized (mLock) {
            setStateAndReasonThenBroadcast(contact, State.RINGING, ReasonCode.UNSPECIFIED);
        }
    }

}
