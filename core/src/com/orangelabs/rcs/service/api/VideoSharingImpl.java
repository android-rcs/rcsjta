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

package com.orangelabs.rcs.service.api;

import javax2.sip.message.Response;

import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.vsh.IVideoRenderer;
import com.gsma.services.rcs.vsh.IVideoSharing;
import com.gsma.services.rcs.vsh.VideoCodec;
import com.gsma.services.rcs.vsh.VideoSharing;
import com.gsma.services.rcs.vsh.VideoSharing.ReasonCode;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSessionListener;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;
import com.orangelabs.rcs.provider.sharing.VideoSharingStateAndReasonCode;
import com.orangelabs.rcs.service.broadcaster.IVideoSharingEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Video sharing session
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingImpl extends IVideoSharing.Stub implements VideoStreamingSessionListener {
	
	/**
	 * Core session
	 */
	private VideoStreamingSession session;

	private final IVideoSharingEventBroadcaster mVideoSharingEventBroadcaster;

	/**
	 * Lock used for synchronization
	 */
	private final Object lock = new Object();

	/**
	 * Started at
	 */
	private long startedAt;
	
	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(VideoSharingImpl.class.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param session Session
	 * @param broadcaster IVideoSharingEventBroadcaster
	 */
	public VideoSharingImpl(VideoStreamingSession session,
			IVideoSharingEventBroadcaster broadcaster) {
		this.session = session;
		mVideoSharingEventBroadcaster = broadcaster;

		session.addListener(this);
	}

	private VideoSharingStateAndReasonCode toStateAndReasonCode(ContentSharingError error) {
		switch (error.getErrorCode()) {
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
			case ContentSharingError.MEDIA_RENDERER_NOT_INITIALIZED:
				return new VideoSharingStateAndReasonCode(VideoSharing.State.FAILED,
						ReasonCode.FAILED_SHARING);
			default:
				throw new IllegalArgumentException(
						"Unknown reason in VideoSharingImpl.toStateAndReasonCode; error="
								+ error + "!");
		}
	}

	private int imsServiceSessionErrorToReasonCode(int imsServiceSessionErrorCodeAsReasonCode) {
		switch (imsServiceSessionErrorCodeAsReasonCode) {
			case ImsServiceSession.TERMINATION_BY_SYSTEM:
			case ImsServiceSession.TERMINATION_BY_TIMEOUT:
				return ReasonCode.ABORTED_BY_SYSTEM;
			case ImsServiceSession.TERMINATION_BY_USER:
				return ReasonCode.ABORTED_BY_USER;
			default:
				throw new IllegalArgumentException(
						"Unknown reason in ImageSharingImpl.imsServiceSessionErrorToReasonCode; imsServiceSessionErrorCodeAsReasonCode="
								+ imsServiceSessionErrorCodeAsReasonCode + "!");
		}
	}

	private void handleSessionRejected(int reasonCode) {
		if (logger.isActivated()) {
			logger.info("Session rejected; reasonCode=" + reasonCode + ".");
		}
		String sharingId = getSharingId();
		synchronized (lock) {
			VideoSharingServiceImpl.removeVideoSharingSession(sharingId);

			RichCallHistory.getInstance().setVideoSharingState(sharingId,
					VideoSharing.State.ABORTED, reasonCode);

			mVideoSharingEventBroadcaster.broadcastStateChanged(getRemoteContact(),
					sharingId, VideoSharing.State.ABORTED, reasonCode);
		}
	}

    /**
	 * Returns the sharing ID of the video sharing
	 * 
	 * @return Sharing ID
	 */
	public String getSharingId() {
		return session.getSessionID();
	}
	
	/**
	 * Returns the remote contact ID
	 * 
	 * @return ContactId
	 */
	public ContactId getRemoteContact() {
		return session.getRemoteContact();
	}
	
	/**
	 * Returns the video codec
	 * 
	 * @return Video codec
	 * @see VideoCodec
	 */
	public VideoCodec getVideoCodec() {
		VideoCodec codec = null;
		try {
			if (session.getVideoPlayer() != null) {
				codec = session.getVideoPlayer().getCodec();
			} else
			if (session.getVideoRenderer() != null) {
				codec = session.getVideoRenderer().getCodec();
			}
		} catch(Exception e) {}
		return codec;
	}
	
	/**
	 * Returns the state of the sharing
	 * 
	 * @return State
	 * @see VideoSharing.State
	 */
	public int getState() {
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null && dialogPath.isSessionEstablished()) {
			return VideoSharing.State.STARTED;

		} else if (session.isInitiatedByRemote()) {
			if (session.isSessionAccepted()) {
				return VideoSharing.State.ACCEPTING;
			}

			return VideoSharing.State.INVITED;
		}

		return VideoSharing.State.INITIATED;
	}

	/**
	 * Returns the reason code of the state of the video sharing
	 *
	 * @return ReasonCode
	 */
	public int getReasonCode() {
		return ReasonCode.UNSPECIFIED;
	}
	
	/**
	 * Returns the direction of the sharing (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see Direction
	 */
	public int getDirection() {
		if (session.isInitiatedByRemote()) {
			return Direction.INCOMING;
		} else {
			return Direction.OUTGOING;
		}
	}	
	
	/**
	 * Accepts video sharing invitation
	 * 
	 * @param renderer Video renderer
	 */
	public void acceptInvitation(IVideoRenderer renderer) {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}

		// Set the video renderer
		session.setVideoRenderer(renderer);
		
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

		// Abort the session
        new Thread() {
    		public void run() {
    			session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
    		}
    	}.start();	
	}

    /*------------------------------- SESSION EVENTS ----------------------------------*/

	/**
	 * Session is started
	 */
	public void handleSessionStarted() {
		if (logger.isActivated()) {
			logger.info("Session started");
		}
		String sharingId = getSharingId();
		synchronized (lock) {
			RichCallHistory.getInstance().setVideoSharingState(sharingId,
					VideoSharing.State.STARTED, ReasonCode.UNSPECIFIED);
			startedAt = System.currentTimeMillis();

			mVideoSharingEventBroadcaster.broadcastStateChanged(getRemoteContact(),
					sharingId, VideoSharing.State.STARTED, ReasonCode.UNSPECIFIED);
		}
	}

	/**
	 * Session has been aborted
	 *
	 * @param reason Termination reason
	 */
	public void handleSessionAborted(int reason) {
		if (logger.isActivated()) {
			logger.info("Session aborted (reason " + reason + ")");
		}
		String sharingId = getSharingId();
		synchronized (lock) {
			VideoSharingServiceImpl.removeVideoSharingSession(sharingId);

			if (session.getDialogPath().isSessionCancelled()) {
				RichCallHistory.getInstance().setVideoSharingState(sharingId,
						VideoSharing.State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);

				mVideoSharingEventBroadcaster.broadcastStateChanged(getRemoteContact(),
						sharingId, VideoSharing.State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
			} else {
				int reasonCode = imsServiceSessionErrorToReasonCode(reason);
				RichCallHistory.getInstance().setVideoSharingState(sharingId,
						VideoSharing.State.ABORTED, reasonCode);

				mVideoSharingEventBroadcaster.broadcastStateChanged(getRemoteContact(),
						sharingId, VideoSharing.State.ABORTED, reasonCode);
				RichCallHistory.getInstance().setVideoSharingDuration(sharingId,
						(System.currentTimeMillis() - startedAt) / 100);
			}
		}
	}

	/**
	 * Session has been terminated by remote
	 */
	public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session terminated by remote");
		}
		String sharingId = getSharingId();
		synchronized (lock) {
			VideoSharingServiceImpl.removeVideoSharingSession(sharingId);

			RichCallHistory.getInstance().setVideoSharingState(sharingId,
					VideoSharing.State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
			RichCallHistory.getInstance().setVideoSharingDuration(sharingId,
					(System.currentTimeMillis() - startedAt) / 100);

			mVideoSharingEventBroadcaster.broadcastStateChanged(getRemoteContact(),
					getSharingId(), VideoSharing.State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
		}
	}

	/**
	 * Content sharing error
	 *
	 * @param error Error
	 */
	public void handleSharingError(ContentSharingError error) {
		if (logger.isActivated()) {
			logger.info("Sharing error " + error.getErrorCode());
		}
		String sharingId = getSharingId();
		VideoSharingStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
		int state = stateAndReasonCode.getState();
		int reasonCode = stateAndReasonCode.getReasonCode();
		synchronized (lock) {
			VideoSharingServiceImpl.removeVideoSharingSession(sharingId);

			RichCallHistory.getInstance().setVideoSharingState(sharingId, state, reasonCode);

			mVideoSharingEventBroadcaster.broadcastStateChanged(getRemoteContact(),
					sharingId, state, reasonCode);
		}
	}

	@Override
	public void handleSessionAccepted() {
		if (logger.isActivated()) {
			logger.info("Accepting sharing");
		}
		String sharingId = getSharingId();
		synchronized (lock) {
			RichCallHistory.getInstance().setVideoSharingState(sharingId,
					VideoSharing.State.ACCEPTING, ReasonCode.UNSPECIFIED);
			mVideoSharingEventBroadcaster.broadcastStateChanged(getRemoteContact(),
					sharingId, VideoSharing.State.ACCEPTING, ReasonCode.UNSPECIFIED);
		}
	}
    
    /**
     * Video stream has been resized
     *
     * @param width Video width
     * @param height Video height
     */
	public void handleVideoResized(int width, int height) {
		// TODO : Check if new callback needed
	}

	@Override
	public void handleSessionRejectedByUser() {
		handleSessionRejected(ReasonCode.REJECTED_BY_USER);
	}

	@Override
	public void handleSessionRejectedByTimeout() {
		handleSessionRejected(ReasonCode.REJECTED_TIME_OUT);
	}

	@Override
	public void handleSessionRejectedByRemote() {
		handleSessionRejected(ReasonCode.REJECTED_BY_REMOTE);
	}

	@Override
	public void handleSessionInvited() {
		if (logger.isActivated()) {
			logger.info("Invited to video sharing session");
		}
		String sharingId = getSharingId();
		VideoContent content = (VideoContent)session.getContent();
		synchronized (lock) {
			RichCallHistory.getInstance()
					.addVideoSharing(getRemoteContact(), sharingId, Direction.INCOMING, content,
							VideoSharing.State.INVITED, ReasonCode.UNSPECIFIED);
		}
		mVideoSharingEventBroadcaster.broadcastInvitation(sharingId);
	}

	@Override
	public void handle180Ringing() {
		String sharingId = getSharingId();
		synchronized (lock) {
			RichCallHistory.getInstance().setVideoSharingState(sharingId,
					VideoSharing.State.RINGING, ReasonCode.UNSPECIFIED);
			mVideoSharingEventBroadcaster.broadcastStateChanged(getRemoteContact(),
					sharingId, VideoSharing.State.RINGING, ReasonCode.UNSPECIFIED);
		}
	}
}
