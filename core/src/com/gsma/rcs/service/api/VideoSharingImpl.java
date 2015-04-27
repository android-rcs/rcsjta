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
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
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
import com.gsma.services.rcs.sharing.video.VideoSharing;
import com.gsma.services.rcs.sharing.video.VideoSharing.ReasonCode;
import com.gsma.services.rcs.sharing.video.VideoSharing.State;

import android.os.RemoteException;

import javax2.sip.message.Response;

/**
 * Video sharing session
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingImpl extends IVideoSharing.Stub implements VideoStreamingSessionListener {

    private final String mSharingId;

    private final IVideoSharingEventBroadcaster mBroadcaster;

    private final RichcallService mRichcallService;

    private final VideoSharingPersistedStorageAccessor mPersistentStorage;

    private final VideoSharingServiceImpl mVideoSharingService;

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    /**
     * The logger
     */
    private final Logger mLogger = Logger.getLogger(getClass().getName());

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
                return new VideoSharingStateAndReasonCode(VideoSharing.State.FAILED,
                        ReasonCode.FAILED_INITIATION);
            case ContentSharingError.SESSION_INITIATION_CANCELLED:
            case ContentSharingError.SESSION_INITIATION_DECLINED:
                return new VideoSharingStateAndReasonCode(VideoSharing.State.REJECTED,
                        ReasonCode.REJECTED_BY_REMOTE);
            case ContentSharingError.MEDIA_TRANSFER_FAILED:
            case ContentSharingError.MEDIA_STREAMING_FAILED:
            case ContentSharingError.UNSUPPORTED_MEDIA_TYPE:
            case ContentSharingError.MEDIA_PLAYER_NOT_INITIALIZED:
                return new VideoSharingStateAndReasonCode(VideoSharing.State.FAILED,
                        ReasonCode.FAILED_SHARING);
            default:
                throw new IllegalArgumentException(
                        "Unknown errorCode=".concat(String.valueOf(code)));
        }
    }

    private void handleSessionRejected(ReasonCode reasonCode, ContactId contact) {
        if (mLogger.isActivated()) {
            mLogger.info("Session rejected; reasonCode=".concat(String.valueOf(reasonCode)));
        }
        synchronized (mLock) {
            mVideoSharingService.removeVideoSharing(mSharingId);

            mPersistentStorage.setStateReasonCodeAndDuration(VideoSharing.State.REJECTED,
                    reasonCode, getCurrentDuration());

            mBroadcaster.broadcastStateChanged(contact, mSharingId, VideoSharing.State.ABORTED,
                    reasonCode);
        }
    }

    private long getCurrentDuration() {
        try {
            return System.currentTimeMillis() - getTimestamp();

        } catch (RemoteException e) {
            return System.currentTimeMillis();
        }
    }

    private void setStateAndReasonCodeAndDuration(ContactId contact, long currentDuration,
            State state, ReasonCode reasonCode) {
        mPersistentStorage.setStateReasonCodeAndDuration(state, reasonCode, currentDuration);
        mBroadcaster.broadcastStateChanged(contact, mSharingId, state, reasonCode);
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
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
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
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
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
                return VideoSharing.State.STARTED.toInt();

            } else if (session.isInitiatedByRemote()) {
                if (session.isSessionAccepted()) {
                    return VideoSharing.State.ACCEPTING.toInt();
                }
                return VideoSharing.State.INVITED.toInt();
            }
            return VideoSharing.State.INITIATING.toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
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
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
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
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Accepts video sharing invitation
     * 
     * @param player Video player
     * @throws RemoteException
     */
    public void acceptInvitation(IVideoPlayer player) throws RemoteException {
        if (player == null) {
            throw new ServerApiIllegalArgumentException("player must not be null!");
        }
        if (mLogger.isActivated()) {
            mLogger.info("Accept session invitation");
        }
        try {
            final VideoStreamingSession session = mRichcallService
                    .getVideoSharingSession(mSharingId);
            if (session == null) {
                throw new ServerApiGenericException(
                        "No session with sharing ID:".concat(mSharingId));
            }
            session.setPlayer(player);
            new Thread() {
                public void run() {
                    session.acceptSession();
                }
            }.start();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Rejects video sharing invitation
     * 
     * @throws RemoteException
     */
    public void rejectInvitation() throws RemoteException {
        try {
            if (mLogger.isActivated()) {
                mLogger.info("Reject session invitation");
            }
            final VideoStreamingSession session = mRichcallService
                    .getVideoSharingSession(mSharingId);
            if (session == null) {
                throw new ServerApiGenericException(
                        "No session with sharing ID:".concat(mSharingId));
            }
            new Thread() {
                public void run() {
                    session.rejectSession(Response.DECLINE);
                }
            }.start();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
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
            if (mLogger.isActivated()) {
                mLogger.info("Cancel session");
            }
            final VideoStreamingSession session = mRichcallService
                    .getVideoSharingSession(mSharingId);
            if (session == null) {
                throw new ServerApiGenericException(
                        "No session with sharing ID:".concat(mSharingId));
            }
            new Thread() {
                public void run() {
                    session.terminateSession(TerminationReason.TERMINATION_BY_USER);
                }
            }.start();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
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
                throw new ServerApiGenericException(
                        "Cannot get video encoding for session with sharing ID:".concat(mSharingId));
            }
            VideoCodec codec = player.getCodec();
            if (codec == null) {
                throw new ServerApiGenericException(
                        "Cannot get video codec for session with sharing ID:".concat(mSharingId));
            }
            return codec.getEncoding();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
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
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
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
            return System.currentTimeMillis() - session.getTimestamp();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
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

            } else {
                VideoContent content = (VideoContent) session.getContent();
                return new VideoDescriptor(content.getWidth(), content.getHeight());
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
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
        long currentDuration = getCurrentDuration();
        synchronized (mLock) {
            setStateAndReasonCodeAndDuration(contact, currentDuration, VideoSharing.State.STARTED,
                    ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Session has been aborted
     * 
     * @param reason Termination reason
     */

    public void handleSessionAborted(ContactId contact, TerminationReason reason) {
        if (mLogger.isActivated()) {
            mLogger.info("Session aborted, reason=".concat(String.valueOf(reason)));
        }
        long currentDuration = getCurrentDuration();
        synchronized (mLock) {
            mVideoSharingService.removeVideoSharing(mSharingId);
            switch (reason) {
                case TERMINATION_BY_SYSTEM:
                case TERMINATION_BY_TIMEOUT:
                    setStateAndReasonCodeAndDuration(contact, currentDuration, State.ABORTED,
                            ReasonCode.ABORTED_BY_SYSTEM);
                    break;
                case TERMINATION_BY_CONNECTION_LOST:
                    setStateAndReasonCodeAndDuration(contact, currentDuration, State.FAILED,
                            ReasonCode.FAILED_SHARING);
                    break;
                case TERMINATION_BY_USER:
                    setStateAndReasonCodeAndDuration(contact, currentDuration, State.ABORTED,
                            ReasonCode.ABORTED_BY_USER);
                    break;
                case TERMINATION_BY_REMOTE:
                    setStateAndReasonCodeAndDuration(contact, currentDuration,
                            VideoSharing.State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unknown imsServiceSessionError=".concat(String.valueOf(reason)));
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
        if (mLogger.isActivated()) {
            mLogger.info("Sharing error ".concat(String.valueOf(error.getErrorCode())));
        }
        VideoSharingStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
        State state = stateAndReasonCode.getState();
        ReasonCode reasonCode = stateAndReasonCode.getReasonCode();
        long currentDuration = getCurrentDuration();
        synchronized (mLock) {
            mVideoSharingService.removeVideoSharing(mSharingId);
            setStateAndReasonCodeAndDuration(contact, currentDuration, state, reasonCode);
        }
    }

    @Override
    public void handleSessionAccepted(ContactId contact) {
        if (mLogger.isActivated()) {
            mLogger.info("Accepting sharing");
        }
        long currentDuration = getCurrentDuration();
        synchronized (mLock) {
            setStateAndReasonCodeAndDuration(contact, currentDuration,
                    VideoSharing.State.ACCEPTING, ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Video stream has been resized
     * 
     * @param width Video width
     * @param height Video height
     */
    public void handleVideoResized(int width, int height) {
        // Not used
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
    public void handleSessionInvited(ContactId contact, MmContent content, long timestamp) {
        if (mLogger.isActivated()) {
            mLogger.info("Invited to video sharing session");
        }
        synchronized (mLock) {
            mPersistentStorage.addVideoSharing(contact, Direction.INCOMING, (VideoContent) content,
                    VideoSharing.State.INVITED, ReasonCode.UNSPECIFIED, timestamp);
        }
        mBroadcaster.broadcastInvitation(mSharingId);
    }

    @Override
    public void handle180Ringing(ContactId contact) {
        long currentDuration = getCurrentDuration();
        synchronized (mLock) {
            setStateAndReasonCodeAndDuration(contact, currentDuration, VideoSharing.State.RINGING,
                    ReasonCode.UNSPECIFIED);
        }
    }
}
