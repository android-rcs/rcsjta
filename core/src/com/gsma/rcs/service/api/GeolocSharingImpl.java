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
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.service.ImsServiceSession.InvitationStatus;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingError;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.gsma.rcs.core.ims.service.richcall.geoloc.GeolocTransferSessionListener;
import com.gsma.rcs.provider.sharing.GeolocSharingPersistedStorageAccessor;
import com.gsma.rcs.provider.sharing.GeolocSharingStateAndReasonCode;
import com.gsma.rcs.provider.sharing.RichCallHistory;
import com.gsma.rcs.service.broadcaster.IGeolocSharingEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing.ReasonCode;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing.State;
import com.gsma.services.rcs.sharing.geoloc.IGeolocSharing;

import android.os.RemoteException;

/**
 * Geoloc sharing implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class GeolocSharingImpl extends IGeolocSharing.Stub implements GeolocTransferSessionListener {

    private final String mSharingId;

    private final IGeolocSharingEventBroadcaster mBroadcaster;

    private final RichcallService mRichcallService;

    private final GeolocSharingPersistedStorageAccessor mPersistentStorage;

    private final GeolocSharingServiceImpl mGeolocSharingService;

    /**
     * Lock used for synchronisation
     */
    private final Object lock = new Object();

    /**
     * The logger
     */
    private final static Logger sLogger = Logger.getLogger(GeolocSharingImpl.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param sharingId Unique Id of Geoloc sharing
     * @param broadcaster IGeolocSharingEventBroadcaster
     * @param richcallService RichcallService
     * @param geolocSharingService GeolocSharingServiceImpl
     */
    public GeolocSharingImpl(String sharingId, IGeolocSharingEventBroadcaster broadcaster,
            RichcallService richcallService, GeolocSharingServiceImpl geolocSharingService,
            GeolocSharingPersistedStorageAccessor persistedStorage) {
        mSharingId = sharingId;
        mBroadcaster = broadcaster;
        mRichcallService = richcallService;
        mGeolocSharingService = geolocSharingService;
        mPersistentStorage = persistedStorage;
    }

    /**
     * Returns the sharing ID of the geoloc sharing
     * 
     * @return Sharing ID
     * @throws RemoteException
     */
    public String getSharingId() throws RemoteException {
        try {
            return mSharingId;

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
     * Gets the geolocation
     * 
     * @return Geolocation
     * @throws RemoteException
     */
    public Geoloc getGeoloc() throws RemoteException {
        try {
            GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getGeoloc();
            }

            return session.getGeoloc();

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
     * Returns the remote contact identifier
     * 
     * @return ContactId
     * @throws RemoteException
     */
    public ContactId getRemoteContact() throws RemoteException {
        try {
            GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getRemoteContact();
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
     * Returns the state of the geoloc sharing
     * 
     * @return State
     * @throws RemoteException
     */
    public int getState() throws RemoteException {
        try {
            GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getState().toInt();
            }
            if (session.isGeolocTransfered()) {
                return State.TRANSFERRED.toInt();
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
     * Returns the reason code of the state of the geoloc sharing
     * 
     * @return ReasonCode
     * @throws RemoteException
     */
    public int getReasonCode() throws RemoteException {
        try {
            GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getReasonCode().toInt();
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
     * Returns the direction of the sharing (incoming or outgoing)
     * 
     * @return Direction
     * @throws RemoteException
     * @see Direction
     */
    public int getDirection() throws RemoteException {
        try {
            GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getDirection().toInt();
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
     * Returns the local timestamp of when the geoloc sharing was initiated for outgoing geoloc
     * sharing or the local timestamp of when the geoloc sharing invitation was received for
     * incoming geoloc sharings.
     * 
     * @return long
     * @throws RemoteException
     */
    public long getTimestamp() throws RemoteException {
        try {
            GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getTimestamp();
            }
            return session.getTimestamp();

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
     * Accepts geoloc sharing invitation
     * 
     * @throws RemoteException
     */
    public void acceptInvitation() throws RemoteException {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Accept session invitation");
            }
            final GeolocTransferSession session = mRichcallService
                    .getGeolocTransferSession(mSharingId);
            if (session == null) {
                throw new ServerApiGenericException(new StringBuilder("Session with sharing ID '")
                        .append(mSharingId).append("' not available!").toString());
            }
            session.acceptSession();
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
     * Rejects geoloc sharing invitation
     * 
     * @throws RemoteException
     */
    public void rejectInvitation() throws RemoteException {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Reject session invitation");
            }
            final GeolocTransferSession session = mRichcallService
                    .getGeolocTransferSession(mSharingId);
            if (session == null) {
                throw new ServerApiGenericException(new StringBuilder("Session with sharing ID '")
                        .append(mSharingId).append("' not available!").toString());
            }
            session.rejectSession(InvitationStatus.INVITATION_REJECTED_DECLINE);
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
     * Aborts the sharing
     * 
     * @throws RemoteException
     */
    public void abortSharing() throws RemoteException {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Cancel session");
            }
            final GeolocTransferSession session = mRichcallService
                    .getGeolocTransferSession(mSharingId);
            if (session == null) {
                throw new ServerApiGenericException(new StringBuilder("Session with sharing ID '")
                        .append(mSharingId).append("' not available!").toString());
            }
            if (session.isGeolocTransfered()) {
                throw new ServerApiPermissionDeniedException(
                        "Cannot abort as geoloc is already transferred!");
            }
            new Thread() {
                public void run() {
                    // @FIXME:Terminate Session should not run on a new thread
                    try {
                        session.terminateSession(TerminationReason.TERMINATION_BY_USER);
                    } catch (SipPayloadException e) {
                        sLogger.error(
                                "Failed to terminate session with sharing ID : ".concat(mSharingId),
                                e);
                    } catch (SipNetworkException e) {
                        if (sLogger.isActivated()) {
                            sLogger.debug(e.getMessage());
                        }
                    } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                        sLogger.error(
                                "Failed to terminate session with sharing ID : ".concat(mSharingId),
                                e);
                    }
                }
            }.start();

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

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    /*
     * TODO : Fix reasoncode mapping in the switch.
     */
    private GeolocSharingStateAndReasonCode toStateAndReasonCode(ContentSharingError error) {
        int contentSharingError = error.getErrorCode();
        switch (contentSharingError) {
            case ContentSharingError.SESSION_INITIATION_FAILED:
            case ContentSharingError.SEND_RESPONSE_FAILED:
                return new GeolocSharingStateAndReasonCode(State.FAILED,
                        ReasonCode.FAILED_INITIATION);
            case ContentSharingError.SESSION_INITIATION_CANCELLED:
            case ContentSharingError.SESSION_INITIATION_DECLINED:
                return new GeolocSharingStateAndReasonCode(State.REJECTED,
                        ReasonCode.REJECTED_BY_REMOTE);
            case ContentSharingError.MEDIA_SAVING_FAILED:
            case ContentSharingError.MEDIA_TRANSFER_FAILED:
            case ContentSharingError.MEDIA_STREAMING_FAILED:
            case ContentSharingError.UNSUPPORTED_MEDIA_TYPE:
                return new GeolocSharingStateAndReasonCode(State.FAILED, ReasonCode.FAILED_SHARING);
            default:
                throw new IllegalArgumentException(
                        new StringBuilder(
                                "Unknown reason in GeolocSharingImpl.toStateAndReasonCode; contentSharingError=")
                                .append(contentSharingError).append("!").toString());
        }
    }

    private void setStateAndReasonCode(ContactId contact, State state, ReasonCode reasonCode) {
        if (mPersistentStorage.setStateAndReasonCode(state, reasonCode)) {
            mBroadcaster.broadcastStateChanged(contact, mSharingId, state, reasonCode);
        }
    }

    private void handleSessionRejected(ReasonCode reasonCode, ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Session rejected; reasonCode=" + reasonCode + ".");
        }
        synchronized (lock) {
            mGeolocSharingService.removeGeolocSharing(mSharingId);
            setStateAndReasonCode(contact, State.REJECTED, reasonCode);
        }
    }

    /**
     * Session is started
     */
    public void handleSessionStarted(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Session started.");
        }
        synchronized (lock) {
            setStateAndReasonCode(contact, State.STARTED, ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Session has been aborted
     * 
     * @param contact
     * @param reason Termination reason
     */
    public void handleSessionAborted(ContactId contact, TerminationReason reason) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Session aborted; reason=").append(reason).append(".")
                    .toString());
        }
        synchronized (lock) {
            mGeolocSharingService.removeGeolocSharing(mSharingId);
            switch (reason) {
                case TERMINATION_BY_TIMEOUT:
                case TERMINATION_BY_SYSTEM:
                    setStateAndReasonCode(contact, State.ABORTED, ReasonCode.ABORTED_BY_SYSTEM);
                    break;
                case TERMINATION_BY_CONNECTION_LOST:
                    setStateAndReasonCode(contact, State.FAILED, ReasonCode.FAILED_SHARING);
                    break;
                case TERMINATION_BY_USER:
                    setStateAndReasonCode(contact, State.ABORTED, ReasonCode.ABORTED_BY_USER);
                    break;
                case TERMINATION_BY_REMOTE:
                    /*
                     * TODO : Fix sending of SIP BYE by sender once transfer is completed and media
                     * session is closed. Then this check of state can be removed. Also need to
                     * check if it is storing and broadcasting right state and reasoncode.
                     */
                    if (State.TRANSFERRED != mPersistentStorage.getState()) {
                        setStateAndReasonCode(contact, State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(new StringBuilder(
                            "Unknown reason ; sessionAbortedReason=").append(reason).append("!")
                            .toString());
            }
        }
    }

    /**
     * Content sharing error
     * 
     * @param contact Remote contact
     * @param error Error
     */
    public void handleSharingError(ContactId contact, ContentSharingError error) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Sharing error ").append(error.getErrorCode())
                    .append(".").toString());
        }
        GeolocSharingStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
        State state = stateAndReasonCode.getState();
        ReasonCode reasonCode = stateAndReasonCode.getReasonCode();
        synchronized (lock) {
            mGeolocSharingService.removeGeolocSharing(mSharingId);
            setStateAndReasonCode(contact, state, reasonCode);
        }
    }

    /**
     * Content has been transfered
     * 
     * @param contact Remote contact
     * @param geoloc Geolocation
     * @param initiatedByRemote
     */
    public void handleContentTransfered(ContactId contact, Geoloc geoloc, boolean initiatedByRemote) {
        if (sLogger.isActivated()) {
            sLogger.info("Geoloc transferred.");
        }
        synchronized (lock) {
            mGeolocSharingService.removeGeolocSharing(mSharingId);
            if (initiatedByRemote) {
                if (RichCallHistory.getInstance().setGeolocSharingTransferred(mSharingId, geoloc)) {
                    mBroadcaster.broadcastStateChanged(contact, mSharingId, State.TRANSFERRED,
                            ReasonCode.UNSPECIFIED);
                }
            } else {
                if (mPersistentStorage.setStateAndReasonCode(State.TRANSFERRED,
                        ReasonCode.UNSPECIFIED)) {
                    mBroadcaster.broadcastStateChanged(contact, mSharingId, State.TRANSFERRED,
                            ReasonCode.UNSPECIFIED);
                }
            }
        }
    }

    @Override
    public void handleSessionAccepted(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Accepting sharing.");
        }
        synchronized (lock) {
            setStateAndReasonCode(contact, State.ACCEPTING, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleSessionRejected(ContactId contact, TerminationReason reason) {
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
    public void handleSessionInvited(ContactId contact, long timestamp) {
        synchronized (lock) {
            mPersistentStorage.addIncomingGeolocSharing(contact, State.INVITED,
                    ReasonCode.UNSPECIFIED, timestamp);
            mBroadcaster.broadcastInvitation(mSharingId);
        }
    }

    @Override
    public void handle180Ringing(ContactId contact) {
        synchronized (lock) {
            setStateAndReasonCode(contact, State.RINGING, ReasonCode.UNSPECIFIED);
        }
    }
}
