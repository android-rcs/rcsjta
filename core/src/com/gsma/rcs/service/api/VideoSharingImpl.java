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

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.service.ImsServiceSession.InvitationStatus;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingError;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.gsma.rcs.core.ims.service.richcall.video.VideoStreamingSessionListener;
import com.gsma.rcs.provider.sharing.VideoSharingPersistedStorageAccessor;
import com.gsma.rcs.provider.sharing.VideoSharingStateAndReasonCode;
import com.gsma.rcs.service.broadcaster.IVideoSharingEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.IVideoPlayer;
import com.gsma.services.rcs.sharing.video.IVideoSharing;
import com.gsma.services.rcs.sharing.video.VideoCodec;
import com.gsma.services.rcs.sharing.video.VideoDescriptor;
import com.gsma.services.rcs.sharing.video.VideoSharing.ReasonCode;
import com.gsma.services.rcs.sharing.video.VideoSharing.State;

import android.os.RemoteException;

/**
 * Video sharing session
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingImpl extends IVideoSharing.Stub implements VideoStreamingSessionListener {

    private final String mSharingId;

    private long mStartTime = 0;

    private final IVideoSharingEventBroadcaster mBroadcaster;

    private final RichcallService mRichcallService;

    private final VideoSharingPersistedStorageAccessor mPersistentStorage;

    private final VideoSharingServiceImpl mVideoSharingService;

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    private static final Logger sLogger = Logger.getLogger(VideoSharingImpl.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param sharingId Unique Id of video sharing
     * @param richcallService RichcallService
     * @param broadcaster IVideoSharingEventBroadcaster
     * @param persistentStorage VideoSharingPersistedStorageAccessor
     * @param videoSharingService VideoSharingServiceImpl
     */
    public VideoSharingImpl(String sharingId, RichcallService richcallService,
            IVideoSharingEventBroadcaster broadcaster,
            VideoSharingPersistedStorageAccessor persistentStorage,
            VideoSharingServiceImpl videoSharingService) {
        mSharingId = sharingId;
        mRichcallService = richcallService;
        mBroadcaster = broadcaster;
        mPersistentStorage = persistentStorage;
        mVideoSharingService = videoSharingService;
    }

    /*
     * TODO: Fix reasoncode mapping in the switch.
     */
    private VideoSharingStateAndReasonCode toStateAndReasonCode(ContentSharingError error) {
        int code = error.getErrorCode();
        switch (code) {
            case ContentSharingError.SESSION_INITIATION_FAILED:
            case ContentSharingError.SEND_RESPONSE_FAILED:
                return new VideoSharingStateAndReasonCode(State.FAILED,
                        ReasonCode.FAILED_INITIATION);
            case ContentSharingError.SESSION_INITIATION_CANCELLED:
            case ContentSharingError.SESSION_INITIATION_DECLINED:
                return new VideoSharingStateAndReasonCode(State.REJECTED,
                        ReasonCode.REJECTED_BY_REMOTE);
            case ContentSharingError.MEDIA_TRANSFER_FAILED:
            case ContentSharingError.MEDIA_STREAMING_FAILED:
            case ContentSharingError.UNSUPPORTED_MEDIA_TYPE:
            case ContentSharingError.MEDIA_PLAYER_NOT_INITIALIZED:
                return new VideoSharingStateAndReasonCode(State.FAILED, ReasonCode.FAILED_SHARING);
            default:
                throw new IllegalArgumentException(new StringBuilder("Unknown errorCode=").append(
                        code).toString());
        }
    }

    private void handleSessionRejected(ReasonCode reasonCode, ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Session rejected; reasonCode=".concat(String.valueOf(reasonCode)));
        }
        synchronized (mLock) {
            mVideoSharingService.removeVideoSharing(mSharingId);
            setStateAndReasonCode(contact, State.REJECTED, reasonCode);
        }
    }

    private void setStateAndReasonCode(ContactId contact, State state, ReasonCode reasonCode) {
        long duration = 0;

        switch (state) {
            case STARTED:
                mStartTime = System.currentTimeMillis();
                break;
            case ABORTED:
            case FAILED:
                duration = mStartTime > 0 ? System.currentTimeMillis() - mStartTime : 0;
                //$FALL-THROUGH$
            default:
                break;
        }

        if (mPersistentStorage.setStateReasonCodeAndDuration(state, reasonCode, duration)) {
            mBroadcaster.broadcastStateChanged(contact, mSharingId, state, reasonCode);
        }
    }

    /**
     * Returns the sharing ID of the video sharing
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
     * Returns the remote contact ID
     * 
     * @return ContactId
     * @throws RemoteException
     */
    public ContactId getRemoteContact() throws RemoteException {
        try {
            VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
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
     * Returns the state of the sharing
     * 
     * @return State
     * @throws RemoteException
     * @see State
     */
    public int getState() throws RemoteException {
        try {
            VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
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
     * Returns the reason code of the state of the video sharing
     * 
     * @return ReasonCode
     * @throws RemoteException
     * @see ReasonCode
     */
    public int getReasonCode() throws RemoteException {
        try {
            VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
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
            VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
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
     * Accepts video sharing invitation
     * 
     * @param player Video player
     * @throws RemoteException
     */
    public void acceptInvitation(final IVideoPlayer player) throws RemoteException {
        if (player == null) {
            throw new ServerApiIllegalArgumentException("player must not be null!");
        }
        mRichcallService.scheduleImageShareOperation(new Runnable() {
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.debug("Accept session invitation");
                }
                try {
                    final VideoStreamingSession session = mRichcallService
                            .getVideoSharingSession(mSharingId);
                    if (session == null) {
                        sLogger.debug("Cannot accept sharing: no session with ID="
                                .concat(mSharingId));
                    }
                    session.setPlayer(player);
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
     * Rejects video sharing invitation
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
                    final VideoStreamingSession session = mRichcallService
                            .getVideoSharingSession(mSharingId);
                    if (session == null) {
                        sLogger.debug("Cannot reject sharing: so session with ID="
                                .concat(mSharingId));
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
        mRichcallService.scheduleVideoShareOperation(new Runnable() {
            public void run() {
                try {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Abort session");
                    }
                    final VideoStreamingSession session = mRichcallService
                            .getVideoSharingSession(mSharingId);
                    if (session == null) {
                        sLogger.debug("No ongoing session with sharing ID:" + mSharingId
                                + " is found so nothing to abort!");
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

    /**
     * Return the video encoding (eg. H.264)
     * 
     * @return Encoding
     * @throws RemoteException
     */
    public String getVideoEncoding() throws RemoteException {
        try {
            final VideoStreamingSession session = mRichcallService
                    .getVideoSharingSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getVideoEncoding();
            }
            IVideoPlayer player = session.getPlayer();
            if (player == null) {
                throw new ServerApiGenericException(new StringBuilder(
                        "Cannot get video encoding for session with sharing ID:")
                        .append(mSharingId).toString());
            }
            VideoCodec codec = player.getCodec();
            if (codec == null) {
                throw new ServerApiGenericException(new StringBuilder(
                        "Cannot get video codec for session with sharing ID:").append(mSharingId)
                        .toString());
            }
            return codec.getEncoding();

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
     * Returns the local timestamp of when the video sharing was initiated for outgoing video
     * sharing or the local timestamp of when the video sharing invitation was received for incoming
     * video sharings.
     * 
     * @return Timestamp in milliseconds
     * @throws RemoteException
     */
    public long getTimestamp() throws RemoteException {
        try {
            final VideoStreamingSession session = mRichcallService
                    .getVideoSharingSession(mSharingId);
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
     * Returns the duration of the video sharing
     * 
     * @return Duration in milliseconds
     * @throws RemoteException
     */
    public long getDuration() throws RemoteException {
        try {
            final VideoStreamingSession session = mRichcallService
                    .getVideoSharingSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getDuration();
            }
            return mStartTime > 0 ? System.currentTimeMillis() - mStartTime : 0;

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
     * Returns the video descriptor
     * 
     * @return Video descriptor
     * @throws RemoteException
     * @see VideoDescriptor
     */
    public VideoDescriptor getVideoDescriptor() throws RemoteException {
        try {
            final VideoStreamingSession session = mRichcallService
                    .getVideoSharingSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getVideoDescriptor();
            }
            IVideoPlayer player = session.getPlayer();
            if (player != null) {
                VideoCodec codec = player.getCodec();
                return new VideoDescriptor(codec.getWidth(), codec.getHeight());

            }
            VideoContent content = (VideoContent) session.getContent();
            return new VideoDescriptor(content.getWidth(), content.getHeight());

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

    @Override
    public void onSessionStarted(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Session started");
        }
        synchronized (mLock) {
            setStateAndReasonCode(contact, State.STARTED, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void onSessionAborted(ContactId contact, TerminationReason reason) {
        if (sLogger.isActivated()) {
            sLogger.debug("Session aborted, reason=".concat(String.valueOf(reason)));
        }
        synchronized (mLock) {
            mVideoSharingService.removeVideoSharing(mSharingId);
            switch (reason) {
                case TERMINATION_BY_SYSTEM:
                case TERMINATION_BY_TIMEOUT:
                    setStateAndReasonCode(contact, State.ABORTED, ReasonCode.ABORTED_BY_SYSTEM);
                    break;
                case TERMINATION_BY_CONNECTION_LOST:
                    setStateAndReasonCode(contact, State.FAILED, ReasonCode.FAILED_SHARING);
                    break;
                case TERMINATION_BY_USER:
                    setStateAndReasonCode(contact, State.ABORTED, ReasonCode.ABORTED_BY_USER);
                    break;
                case TERMINATION_BY_REMOTE:
                    setStateAndReasonCode(contact, State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
                    break;
                default:
                    throw new IllegalArgumentException(new StringBuilder(
                            "Unknown imsServiceSessionError=").append(reason).toString());
            }
        }
    }

    @Override
    public void onSharingError(ContactId contact, ContentSharingError error) {
        if (sLogger.isActivated()) {
            sLogger.debug("Sharing error ".concat(String.valueOf(error.getErrorCode())));
        }
        VideoSharingStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
        State state = stateAndReasonCode.getState();
        ReasonCode reasonCode = stateAndReasonCode.getReasonCode();
        synchronized (mLock) {
            mVideoSharingService.removeVideoSharing(mSharingId);
            setStateAndReasonCode(contact, state, reasonCode);
        }
    }

    @Override
    public void onSessionAccepting(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Accepting sharing");
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
    public void onInvitationReceived(ContactId contact, MmContent content, long timestamp) {
        if (sLogger.isActivated()) {
            sLogger.debug("Invited to video sharing session");
        }
        synchronized (mLock) {
            mPersistentStorage.addVideoSharing(contact, Direction.INCOMING, (VideoContent) content,
                    State.INVITED, ReasonCode.UNSPECIFIED, timestamp);
        }
        mBroadcaster.broadcastInvitation(mSharingId);
    }

    @Override
    public void onSessionRinging(ContactId contact) {
        synchronized (mLock) {
            setStateAndReasonCode(contact, State.RINGING, ReasonCode.UNSPECIFIED);
        }
    }
}
