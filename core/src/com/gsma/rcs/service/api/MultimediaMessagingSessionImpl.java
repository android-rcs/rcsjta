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

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.service.ImsServiceSession.InvitationStatus;
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
import android.os.RemoteException;

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

    private final MultimediaSessionStorageAccessor mPersistedStorage;

    private final Object mLock = new Object();

    private static final Logger sLogger = Logger.getLogger(MultimediaMessagingSessionImpl.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param sessionId Session ID
     * @param broadcaster IMultimediaMessagingSessionEventBroadcaster
     * @param sipService SipService
     * @param multimediaSessionService MultimediaSessionServiceImpl
     * @param direction
     * @param contact remote contact
     * @param serviceId
     * @param state State of the multimedia session
     */
    public MultimediaMessagingSessionImpl(String sessionId,
            IMultimediaMessagingSessionEventBroadcaster broadcaster, SipService sipService,
            MultimediaSessionServiceImpl multimediaSessionService, Direction direction,
            ContactId contact, String serviceId, State state) {
        mSessionId = sessionId;
        mBroadcaster = broadcaster;
        mSipService = sipService;
        mMultimediaSessionService = multimediaSessionService;
        mPersistedStorage = new MultimediaSessionStorageAccessor(direction, contact, serviceId,
                state);
    }

    private void handleSessionRejected(ReasonCode reasonCode, ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Session rejected; reasonCode=" + reasonCode + ".");
        }
        String sessionId = getSessionId();
        synchronized (mLock) {
            mMultimediaSessionService.removeMultimediaMessaging(sessionId);
            setStateAndReason(contact, State.REJECTED, reasonCode);
        }
    }

    private void removeSession(ContactId contact, State state, ReasonCode reasonCode) {
        mMultimediaSessionService.removeMultimediaMessaging(mSessionId);
        setStateAndReason(contact, state, reasonCode);
    }

    private void setStateAndReason(ContactId contact, State state, ReasonCode reason) {
        mPersistedStorage.setStateAndReasonCode(state, reason);
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
     * @throws RemoteException
     */
    public ContactId getRemoteContact() throws RemoteException {
        try {
            GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
            if (session == null) {
                return mPersistedStorage.getRemoteContact();
            }
            return session.getRemoteContact();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the state of the session
     * 
     * @return State
     * @throws RemoteException
     */
    public int getState() throws RemoteException {
        try {
            GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
            if (session == null) {
                return mPersistedStorage.getState().toInt();
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

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the reason code of the state of the multimedia messaging session
     * 
     * @return ReasonCode
     * @throws RemoteException
     */
    public int getReasonCode() throws RemoteException {
        try {
            GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
            if (session == null) {
                return mPersistedStorage.getReasonCode().toInt();
            }
            return ReasonCode.UNSPECIFIED.toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the direction of the session (incoming or outgoing)
     * 
     * @return Direction
     * @throws RemoteException
     * @see Direction
     */
    public int getDirection() throws RemoteException {
        try {
            GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
            if (session == null) {
                return mPersistedStorage.getDirection().toInt();
            }
            if (session.isInitiatedByRemote()) {
                return Direction.INCOMING.toInt();
            }
            return Direction.OUTGOING.toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the service ID
     * 
     * @return Service ID
     * @throws RemoteException
     */
    public String getServiceId() throws RemoteException {
        try {
            GenericSipMsrpSession session = mSipService.getGenericSipMsrpSession(mSessionId);
            if (session == null) {
                return mPersistedStorage.getServiceId();
            }
            return session.getServiceId();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Accepts session invitation
     * 
     * @throws RemoteException
     */
    public void acceptInvitation() throws RemoteException {
        mSipService.scheduleMultimediaMessagingOperation(new Runnable() {
            public void run() {
                try {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Accept session invitation");
                    }
                    final GenericSipMsrpSession session = mSipService
                            .getGenericSipMsrpSession(mSessionId);
                    if (session == null) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Cannot accept: no session ID=".concat(mSessionId));
                        }
                        return;
                    }
                    ServerApiUtils.testApiExtensionPermission(session.getServiceId());
                    session.acceptSession();

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder("Failed to accept session with ID: ").append(
                                    mSessionId).toString(), e);
                }
            }
        });
    }

    /**
     * Rejects session invitation
     * 
     * @throws RemoteException
     */
    public void rejectInvitation() throws RemoteException {
        mSipService.scheduleMultimediaMessagingOperation(new Runnable() {
            public void run() {
                try {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Reject session invitation");
                    }
                    final GenericSipMsrpSession session = mSipService
                            .getGenericSipMsrpSession(mSessionId);
                    if (session == null) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Cannot reject: no session ID=".concat(mSessionId));
                        }
                        return;
                    }
                    ServerApiUtils.testApiExtensionPermission(session.getServiceId());
                    session.rejectSession(InvitationStatus.INVITATION_REJECTED_DECLINE);

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder("Failed to reject session with ID: ").append(
                                    mSessionId).toString(), e);
                }
            }
        });
    }

    /**
     * Aborts the session
     * 
     * @throws RemoteException
     */
    public void abortSession() throws RemoteException {
        mSipService.scheduleMultimediaMessagingOperation(new Runnable() {
            public void run() {
                try {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Abort session");
                    }
                    final GenericSipMsrpSession session = mSipService
                            .getGenericSipMsrpSession(mSessionId);
                    if (session == null) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("No ongoing session with id:" + mSessionId
                                    + " is found so nothing to abort!");
                        }
                        return;
                    }
                    if (session.isSessionInterrupted()) {
                        sLogger.debug("Session with sharing ID:" + mSessionId
                                + " is already aborted!");
                        return;
                    }
                    ServerApiUtils.testApiExtensionPermission(session.getServiceId());
                    session.terminateSession(TerminationReason.TERMINATION_BY_USER);

                } catch (PayloadException e) {
                    sLogger.error(
                            new StringBuilder("Failed to abort session with ID: ").append(
                                    mSessionId).toString(), e);

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder("Failed to abort session with ID: ").append(
                                    mSessionId).toString(), e);
                }
            }
        });
    }

    /**
     * Sends a message in real time
     * 
     * @param content Message content
     * @throws RemoteException
     */
    public void sendMessage(final byte[] content) throws RemoteException {
        if (content == null || content.length == 0) {
            throw new ServerApiIllegalArgumentException("content must not be null or empty!");
        }
        mSipService.scheduleMultimediaMessagingOperation(new Runnable() {
            public void run() {
                try {
                    GenericSipMsrpSession session = mSipService
                            .getGenericSipMsrpSession(mSessionId);
                    if (session == null) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Session with session ID '" + mSessionId
                                    + "' not available!");
                        }
                        return;
                    }
                    ServerApiUtils.testApiExtensionPermission(session.getServiceId());

                    /* Do not consider max message size if null */
                    if (session.getMaxMessageSize() != 0
                            && content.length > session.getMaxMessageSize()) {
                        sLogger.error("Failed to send message: max length exceeded!");
                        return;
                    }
                    session.sendMessage(content);

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(new StringBuilder(
                            "Failed to send message within session with ID: ").append(mSessionId)
                            .toString(), e);
                }
            }
        });
    }

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    @Override
    public void onSessionStarted(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Session started");
        }
        synchronized (mLock) {
            setStateAndReason(contact, State.STARTED, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void onSessionAborted(ContactId contact, TerminationReason reason) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Session aborted (terminationReason ").append(reason)
                    .append(")").toString());
        }
        synchronized (mLock) {
            switch (reason) {
                case TERMINATION_BY_SYSTEM:
                case TERMINATION_BY_TIMEOUT:
                    removeSession(contact, State.ABORTED, ReasonCode.ABORTED_BY_SYSTEM);
                    break;
                case TERMINATION_BY_USER:
                    removeSession(contact, State.ABORTED, ReasonCode.ABORTED_BY_USER);
                    break;
                case TERMINATION_BY_CONNECTION_LOST:
                    removeSession(contact, State.FAILED, ReasonCode.FAILED_SESSION);
                    break;
                case TERMINATION_BY_REMOTE:
                    removeSession(contact, State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
                    break;
                case TERMINATION_BY_INACTIVITY:
                    removeSession(contact, State.ABORTED, ReasonCode.ABORTED_BY_INACTIVITY);
                    break;
                default:
                    throw new IllegalArgumentException(new StringBuilder(
                            "Unknown TerminationReason=").append(reason).toString());
            }
        }
    }

    @Override
    public void onSessionError(ContactId contact, SipSessionError error) {
        if (sLogger.isActivated()) {
            sLogger.debug("Session error " + error.getErrorCode());
        }
        synchronized (mLock) {
            mMultimediaSessionService.removeMultimediaMessaging(mSessionId);

            switch (error.getErrorCode()) {
                case SipSessionError.SESSION_INITIATION_DECLINED:
                    setStateAndReason(contact, State.REJECTED, ReasonCode.REJECTED_BY_REMOTE);
                    break;
                case SipSessionError.MEDIA_FAILED:
                    setStateAndReason(contact, State.FAILED, ReasonCode.FAILED_MEDIA);
                    break;
                case SipSessionError.SESSION_INITIATION_CANCELLED:
                case SipSessionError.SESSION_INITIATION_FAILED:
                    setStateAndReason(contact, State.FAILED, ReasonCode.FAILED_INITIATION);
                    break;
                default:
                    setStateAndReason(contact, State.FAILED, ReasonCode.FAILED_SESSION);
            }
        }
    }

    @Override
    public void onDataReceived(ContactId contact, byte[] data) {
        synchronized (mLock) {
            mBroadcaster.broadcastMessageReceived(contact, mSessionId, data);
        }
    }

    @Override
    public void onSessionAccepting(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Accepting session");
        }
        synchronized (mLock) {
            setStateAndReason(contact, State.ACCEPTING, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void onSessionRejected(ContactId contact, TerminationReason reason) {
        switch (reason) {
            case TERMINATION_BY_USER:
                handleSessionRejected(ReasonCode.REJECTED_BY_USER, contact);
                break;
            case TERMINATION_BY_SYSTEM:
                /* Intentional fall through */
            case TERMINATION_BY_CONNECTION_LOST:
                handleSessionRejected(ReasonCode.REJECTED_BY_SYSTEM, contact);
                break;
            case TERMINATION_BY_TIMEOUT:
                handleSessionRejected(ReasonCode.REJECTED_BY_TIMEOUT, contact);
                break;
            case TERMINATION_BY_REMOTE:
                handleSessionRejected(ReasonCode.REJECTED_BY_REMOTE, contact);
                break;
            default:
                throw new IllegalArgumentException(new StringBuilder(
                        "Unknown reason RejectedReason=").append(reason).append("!").toString());
        }
    }

    @Override
    public void onInvitationReceived(ContactId contact, Intent sessionInvite) {
        if (sLogger.isActivated()) {
            sLogger.debug("Invited to multimedia messaging session");
        }
        mBroadcaster.broadcastInvitation(getSessionId(), sessionInvite);
    }

    @Override
    public void onSessionRinging(ContactId contact) {
        synchronized (mLock) {
            setStateAndReason(contact, State.RINGING, ReasonCode.UNSPECIFIED);
        }
    }

}
