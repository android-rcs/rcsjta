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
import com.gsma.rcs.core.ims.service.richcall.video.VideoSharingPersistedStorageAccessor;
import com.gsma.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.gsma.rcs.core.ims.service.richcall.video.VideoStreamingSessionListener;
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
    private final Logger logger = Logger.getLogger(getClass().getName());

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
        if (logger.isActivated()) {
            logger.info("Session rejected; reasonCode=".concat(String.valueOf(reasonCode)));
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
        return System.currentTimeMillis() - getTimestamp();
    }

    private void setStateAndReasonCodeAndDurationAndBroadcast(ContactId contact,
            long currentDuration, State state, ReasonCode reasonCode) {
        mPersistentStorage.setStateReasonCodeAndDuration(state, reasonCode, currentDuration);
        mBroadcaster.broadcastStateChanged(contact, mSharingId, state, reasonCode);
    }

    /**
     * Returns the sharing ID of the video sharing
     * 
     * @return Sharing ID
     */
    public String getSharingId() {
        return mSharingId;
    }

    /**
     * Returns the remote contact ID
     * 
     * @return ContactId
     */
    public ContactId getRemoteContact() {
        VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
        if (session == null) {
            return mPersistentStorage.getRemoteContact();
        }
        return session.getRemoteContact();
    }

    /**
     * Returns the state of the sharing
     * 
     * @return State
     * @see State
     */
    public int getState() {
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
    }

    /**
     * Returns the reason code of the state of the video sharing
     * 
     * @return ReasonCode
     * @see ReasonCode
     */
    public int getReasonCode() {
        VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
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
        VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
        if (session == null) {
            return mPersistentStorage.getDirection().toInt();
        }
        if (session.isInitiatedByRemote()) {
            return Direction.INCOMING.toInt();
        }
        return Direction.OUTGOING.toInt();
    }

    /**
     * Accepts video sharing invitation
     * 
     * @param player Video player
     */
    public void acceptInvitation(IVideoPlayer player) {
        if (logger.isActivated()) {
            logger.info("Accept session invitation");
        }
        final VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException("No session with sharing ID:".concat(mSharingId));
        }
        // Set the video player
        session.setPlayer(player);

        // Accept invitation
        new Thread() {
            public void run() {
                session.acceptSession();
            }
        }.start();
    }

    /**
     * Rejects video sharing invitation
     */
    public void rejectInvitation() {
        if (logger.isActivated()) {
            logger.info("Reject session invitation");
        }
        final VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException("No session with sharing ID:".concat(mSharingId));
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
        final VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException("No session with sharing ID:".concat(mSharingId));
        }
        // Abort the session
        new Thread() {
            public void run() {
                session.abortSession(TerminationReason.TERMINATION_BY_USER);
            }
        }.start();
    }

    /**
     * Return the video encoding (eg. H.264)
     * 
     * @return Encoding
     */
    public String getVideoEncoding() {
        final VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
        if (session == null) {
            return mPersistentStorage.getVideoEncoding();
        }
        try {
            IVideoPlayer player = session.getPlayer();
            if (player != null) {
                VideoCodec codec = player.getCodec();
                if (codec != null) {
                    return codec.getEncoding();
                }
            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Exception occurred", e);
            }
            // TODO Should we not rethrow exception ? TODO CR037
        }
        if (logger.isActivated()) {
            logger.warn("Cannot get video encoding");
        }
        return null;
    }

    /**
     * Returns the local timestamp of when the video sharing was initiated for outgoing video
     * sharing or the local timestamp of when the video sharing invitation was received for incoming
     * video sharings.
     * 
     * @return Timestamp in milliseconds
     */
    public long getTimestamp() {
        final VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
        if (session == null) {
            return mPersistentStorage.getTimestamp();
        }
        return session.getTimestamp();
    }

    /**
     * Returns the duration of the video sharing
     * 
     * @return Duration in milliseconds
     */
    public long getDuration() {
        final VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
        if (session == null) {
            return mPersistentStorage.getDuration();
        }
        return System.currentTimeMillis() - session.getTimestamp();
    }

    /**
     * Returns the video descriptor
     * 
     * @return Video descriptor
     * @see VideoDescriptor
     */
    public VideoDescriptor getVideoDescriptor() {
        final VideoStreamingSession session = mRichcallService.getVideoSharingSession(mSharingId);
        if (session == null) {
            return mPersistentStorage.getVideoDescriptor();
        }
        try {
            IVideoPlayer player = session.getPlayer();
            if (player != null) {
                VideoCodec codec = player.getCodec();
                return new VideoDescriptor(codec.getWidth(), codec.getHeight());

            } else {
                VideoContent content = (VideoContent) session.getContent();
                return new VideoDescriptor(content.getWidth(), content.getHeight());
            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Exception occurred", e);
            }
            // TODO should we not rethrow exception here ?
            return null;
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
        long currentDuration = getCurrentDuration();
        synchronized (mLock) {
            setStateAndReasonCodeAndDurationAndBroadcast(contact, currentDuration,
                    VideoSharing.State.STARTED, ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Session has been aborted
     * 
     * @param reason Termination reason
     */

    public void handleSessionAborted(ContactId contact, TerminationReason reason) {
        if (logger.isActivated()) {
            logger.info("Session aborted, reason=".concat(String.valueOf(reason)));
        }
        long currentDuration = getCurrentDuration();
        synchronized (mLock) {
            mVideoSharingService.removeVideoSharing(mSharingId);
            switch (reason) {
                case TERMINATION_BY_SYSTEM:
                case TERMINATION_BY_TIMEOUT:
                    setStateAndReasonCodeAndDurationAndBroadcast(contact, currentDuration,
                            State.ABORTED, ReasonCode.ABORTED_BY_SYSTEM);
                    break;
                case TERMINATION_BY_CONNECTION_LOST:
                    setStateAndReasonCodeAndDurationAndBroadcast(contact, currentDuration,
                            State.FAILED, ReasonCode.FAILED_SHARING);
                    break;
                case TERMINATION_BY_USER:
                    setStateAndReasonCodeAndDurationAndBroadcast(contact, currentDuration,
                            State.ABORTED, ReasonCode.ABORTED_BY_USER);
                    break;
                case TERMINATION_BY_REMOTE:
                    setStateAndReasonCodeAndDurationAndBroadcast(contact, currentDuration,
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
        if (logger.isActivated()) {
            logger.info("Sharing error ".concat(String.valueOf(error.getErrorCode())));
        }
        VideoSharingStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
        State state = stateAndReasonCode.getState();
        ReasonCode reasonCode = stateAndReasonCode.getReasonCode();
        long currentDuration = getCurrentDuration();
        synchronized (mLock) {
            mVideoSharingService.removeVideoSharing(mSharingId);
            setStateAndReasonCodeAndDurationAndBroadcast(contact, currentDuration, state,
                    reasonCode);
        }
    }

    @Override
    public void handleSessionAccepted(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Accepting sharing");
        }
        long currentDuration = getCurrentDuration();
        synchronized (mLock) {
            setStateAndReasonCodeAndDurationAndBroadcast(contact, currentDuration,
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
    public void handleSessionRejectedByUser(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_USER, contact);
    }

    /*
     * TODO: Fix reasoncode mapping between rejected_by_inactivity and rejected_by_timout.
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
    public void handleSessionInvited(ContactId contact, MmContent content, long timestamp) {
        if (logger.isActivated()) {
            logger.info("Invited to video sharing session");
        }
        synchronized (mLock) {
            mPersistentStorage.addVideoSharing(getRemoteContact(), Direction.INCOMING,
                    (VideoContent) content, VideoSharing.State.INVITED, ReasonCode.UNSPECIFIED,
                    timestamp);
        }
        mBroadcaster.broadcastInvitation(mSharingId);
    }

    @Override
    public void handle180Ringing(ContactId contact) {
        long currentDuration = getCurrentDuration();
        synchronized (mLock) {
            setStateAndReasonCodeAndDurationAndBroadcast(contact, currentDuration,
                    VideoSharing.State.RINGING, ReasonCode.UNSPECIFIED);
        }
    }
}
