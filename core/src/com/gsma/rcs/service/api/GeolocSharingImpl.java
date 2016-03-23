/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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
    private final Object mLock = new Object();

    private final static Logger sLogger = Logger.getLogger(GeolocSharingImpl.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param sharingId Unique Id of Geoloc sharing
     * @param broadcaster IGeolocSharingEventBroadcaster
     * @param richcallService RichcallService
     * @param geolocSharingService GeolocSharingServiceImpl
     * @param persistedStorage GeolocSharing persisted storage
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
            if (session.isGeolocTransferred()) {
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
        mRichcallService.scheduleImageShareOperation(new Runnable() {
            public void run() {
                try {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Accept session invitation");
                    }
                    final GeolocTransferSession session = mRichcallService
                            .getGeolocTransferSession(mSharingId);
                    if (session == null) {
                        sLogger.debug("Cannot accept sharing: no session with ID="
                                .concat(mSharingId));
                        return;
                    }
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
                            new StringBuilder("Failed to accept invitation with sharing ID: ")
                                    .append(mSharingId).toString(), e);
                }
            }
        });
    }

    /**
     * Rejects geoloc sharing invitation
     * 
     * @throws RemoteException
     */
    public void rejectInvitation() throws RemoteException {
        mRichcallService.scheduleImageShareOperation(new Runnable() {
            public void run() {
                try {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Reject session invitation");
                    }
                    final GeolocTransferSession session = mRichcallService
                            .getGeolocTransferSession(mSharingId);
                    if (session == null) {
                        sLogger.debug("Cannot reject sharing: no session with ID="
                                .concat(mSharingId));
                        return;
                    }
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
                            new StringBuilder("Failed to reject invitation with sharing ID: ")
                                    .append(mSharingId).toString(), e);
                }
            }
        });
    }

    /**
     * Aborts the sharing
     * 
     * @throws RemoteException
     */
    public void abortSharing() throws RemoteException {
        mRichcallService.scheduleGeolocShareOperation(new Runnable() {
            public void run() {
                try {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Abort session");
                    }
                    final GeolocTransferSession session = mRichcallService
                            .getGeolocTransferSession(mSharingId);
                    if (session == null) {
                        sLogger.debug("No ongoing session with sharing ID:" + mSharingId
                                + " is found so nothing to abort!");
                        return;
                    }
                    if (session.isGeolocTransferred()) {
                        sLogger.debug("Session with sharing ID:" + mSharingId
                                + " is already transferred so nothing to abort!");
                        return;
                    }
                    session.terminateSession(TerminationReason.TERMINATION_BY_USER);

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }

                } catch (PayloadException e) {
                    sLogger.error(
                            new StringBuilder("Failed to terminate session with sharing ID: ")
                                    .append(mSharingId).toString(), e);
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder("Failed to terminate session with sharing ID: ")
                                    .append(mSharingId).toString(), e);
                }
            }
        });
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
            sLogger.debug("Session rejected; reasonCode=" + reasonCode + ".");
        }
        synchronized (mLock) {
            mGeolocSharingService.removeGeolocSharing(mSharingId);
            setStateAndReasonCode(contact, State.REJECTED, reasonCode);
        }
    }

    @Override
    public void onSessionStarted(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Session started.");
        }
        synchronized (mLock) {
            setStateAndReasonCode(contact, State.STARTED, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void onSessionAborted(ContactId contact, TerminationReason reason) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Session aborted; reason=").append(reason).append(".")
                    .toString());
        }
        synchronized (mLock) {
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

    @Override
    public void onSharingError(ContactId contact, ContentSharingError error) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Sharing error ").append(error.getErrorCode())
                    .append(".").toString());
        }
        GeolocSharingStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
        State state = stateAndReasonCode.getState();
        ReasonCode reasonCode = stateAndReasonCode.getReasonCode();
        synchronized (mLock) {
            mGeolocSharingService.removeGeolocSharing(mSharingId);
            setStateAndReasonCode(contact, state, reasonCode);
        }
    }

    @Override
    public void onContentTransferred(ContactId contact, Geoloc geoloc, boolean initiatedByRemote) {
        if (sLogger.isActivated()) {
            sLogger.debug("Geoloc transferred.");
        }
        synchronized (mLock) {
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
    public void onSessionAccepting(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Accepting sharing.");
        }
        synchronized (mLock) {
            setStateAndReasonCode(contact, State.ACCEPTING, ReasonCode.UNSPECIFIED);
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
    public void onInvitationReceived(ContactId contact, long timestamp) {
        synchronized (mLock) {
            mPersistentStorage.addIncomingGeolocSharing(contact, State.INVITED,
                    ReasonCode.UNSPECIFIED, timestamp);
            mBroadcaster.broadcastInvitation(mSharingId);
        }
    }

    @Override
    public void onSessionRinging(ContactId contact) {
        synchronized (mLock) {
            setStateAndReasonCode(contact, State.RINGING, ReasonCode.UNSPECIFIED);
        }
    }
}
