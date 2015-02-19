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
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingError;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.richcall.geoloc.GeolocSharingPersistedStorageAccessor;
import com.gsma.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.gsma.rcs.core.ims.service.richcall.geoloc.GeolocTransferSessionListener;
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

import javax2.sip.message.Response;

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
     */
    public String getSharingId() {
        return mSharingId;
    }

    /**
     * Gets the geolocation
     * 
     * @return Geolocation
     */
    public Geoloc getGeoloc() {
        GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
        if (session == null) {
            return mPersistentStorage.getGeoloc();
        }

        return session.getGeoloc();
    }

    /**
     * Returns the remote contact identifier
     * 
     * @return ContactId
     */
    public ContactId getRemoteContact() {
        GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
        if (session == null) {
            return mPersistentStorage.getRemoteContact();
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
            return mPersistentStorage.getState().toInt();
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
     * Returns the reason code of the state of the geoloc sharing
     * 
     * @return ReasonCode
     */
    public int getReasonCode() {
        GeolocTransferSession session = mRichcallService.getGeolocTransferSession(mSharingId);
        if (session == null) {
            return mPersistentStorage.getReasonCode().toInt();
        }
        return ReasonCode.UNSPECIFIED.toInt();
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
            return mPersistentStorage.getDirection().toInt();
        }
        if (session.isInitiatedByRemote()) {
            return Direction.INCOMING.toInt();
        }
        return Direction.OUTGOING.toInt();
    }

    public long getTimestamp() {
        return mPersistentStorage.getTimestamp();
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
    /*
     * TODO : Fix reasoncode mapping in the switch.
     */
    private ReasonCode sessionAbortedReasonToReasonCode(int sessionAbortedReason) {
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

    /*
     * TODO : Fix reasoncode mapping in the switch.
     */
    private GeolocSharingStateAndReasonCode toStateAndReasonCode(ContentSharingError error) {
        int contentSharingError = error.getErrorCode();
        switch (contentSharingError) {
            case ContentSharingError.SESSION_INITIATION_FAILED:
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

    private void handleSessionRejected(ReasonCode reasonCode, ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session rejected; reasonCode=" + reasonCode + ".");
        }
        synchronized (lock) {
            mGeolocSharingService.removeGeolocSharing(mSharingId);
            mPersistentStorage.setStateAndReasonCode(State.REJECTED, reasonCode);
            mBroadcaster.broadcastStateChanged(contact, mSharingId, State.REJECTED, reasonCode);
        }
    }

    /**
     * Session is started
     */
    public void handleSessionStarted(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session started.");
        }
        synchronized (lock) {
            mPersistentStorage.setStateAndReasonCode(State.STARTED, ReasonCode.UNSPECIFIED);
            mBroadcaster.broadcastStateChanged(contact, mSharingId, State.STARTED,
                    ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Session has been aborted
     * 
     * @param contact
     * @param reason Termination reason
     */
    public void handleSessionAborted(ContactId contact, int reason) {
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Session aborted; reason=").append(reason).append(".")
                    .toString());
        }
        ReasonCode reasonCode = sessionAbortedReasonToReasonCode(reason);
        synchronized (lock) {
            mGeolocSharingService.removeGeolocSharing(mSharingId);
            mPersistentStorage.setStateAndReasonCode(State.ABORTED, reasonCode);
            mBroadcaster.broadcastStateChanged(contact, mSharingId, State.ABORTED, reasonCode);
        }
    }

    /**
     * Session has been terminated by remote
     */
    public void handleSessionTerminatedByRemote(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session terminated by remote");
        }
        synchronized (lock) {
            mGeolocSharingService.removeGeolocSharing(mSharingId);
            /*
             * TODO : Fix sending of SIP BYE by sender once transfer is completed and media session
             * is closed. Then this check of state can be removed. Also need to check if it is
             * storing and broadcasting right state and reasoncode.
             */
            if (State.TRANSFERRED != mPersistentStorage.getState()) {
                mPersistentStorage.setStateAndReasonCode(State.ABORTED,
                        ReasonCode.ABORTED_BY_REMOTE);
                mBroadcaster.broadcastStateChanged(contact, mSharingId, State.ABORTED,
                        ReasonCode.ABORTED_BY_REMOTE);
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
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Sharing error ").append(error.getErrorCode())
                    .append(".").toString());
        }
        GeolocSharingStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
        State state = stateAndReasonCode.getState();
        ReasonCode reasonCode = stateAndReasonCode.getReasonCode();
        synchronized (lock) {
            mGeolocSharingService.removeGeolocSharing(mSharingId);
            mPersistentStorage.setStateAndReasonCode(state, reasonCode);
            mBroadcaster.broadcastStateChanged(contact, mSharingId, state, reasonCode);
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
        if (logger.isActivated()) {
            logger.info("Geoloc transferred.");
        }
        synchronized (lock) {
            mGeolocSharingService.removeGeolocSharing(mSharingId);
            if (initiatedByRemote) {
                RichCallHistory.getInstance().setGeolocSharingTransferred(mSharingId, geoloc);
            } else {
                mPersistentStorage.setStateAndReasonCode(State.TRANSFERRED, ReasonCode.UNSPECIFIED);
            }
            mBroadcaster.broadcastStateChanged(contact, mSharingId, State.TRANSFERRED,
                    ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleSessionAccepted(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Accepting sharing.");
        }
        synchronized (lock) {
            mPersistentStorage.setStateAndReasonCode(State.ACCEPTING, ReasonCode.UNSPECIFIED);
            mBroadcaster.broadcastStateChanged(contact, mSharingId, State.ACCEPTING,
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
    public void handleSessionInvited(ContactId contact) {
        synchronized (lock) {
            mPersistentStorage.addIncomingGeolocSharing(contact, State.INVITED,
                    ReasonCode.UNSPECIFIED);
            mBroadcaster.broadcastInvitation(mSharingId);
        }
    }

    @Override
    public void handle180Ringing(ContactId contact) {
        synchronized (lock) {
            mPersistentStorage.setStateAndReasonCode(State.RINGING, ReasonCode.UNSPECIFIED);
            mBroadcaster.broadcastStateChanged(contact, mSharingId, State.RINGING,
                    ReasonCode.UNSPECIFIED);
        }
    }
}
