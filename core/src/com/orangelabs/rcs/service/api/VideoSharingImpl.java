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

import static com.gsma.services.rcs.vsh.VideoSharing.State.FAILED;
import static com.gsma.services.rcs.vsh.VideoSharing.State.ABORTED;
import static com.gsma.services.rcs.vsh.VideoSharing.State.STARTED;
import static com.gsma.services.rcs.vsh.VideoSharing.State.TERMINATED;

import android.os.RemoteCallbackList;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.vsh.IVideoRenderer;
import com.gsma.services.rcs.vsh.IVideoSharing;
import com.gsma.services.rcs.vsh.IVideoSharingListener;
import com.gsma.services.rcs.vsh.VideoCodec;
import com.gsma.services.rcs.vsh.VideoSharing;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.video.OriginatingVideoStreamingSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSessionListener;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;
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
	private final Logger logger = Logger.getLogger(getClass().getName());

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
		int result = VideoSharing.State.INACTIVE;
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null) {
			if (dialogPath.isSessionCancelled()) {
				// Session canceled
				result = VideoSharing.State.ABORTED;
			} else
			if (dialogPath.isSessionEstablished()) {
				// Session started
				result = VideoSharing.State.STARTED;
			} else
			if (dialogPath.isSessionTerminated()) {
				// Session terminated
				result = VideoSharing.State.TERMINATED;
			} else {
				// Session pending
				if (session instanceof OriginatingVideoStreamingSession) {
					result = VideoSharing.State.INITIATED;
				} else {
					result = VideoSharing.State.INVITED;
				}
			}
		}
		return result;		
	}
	
	/**
	 * Returns the direction of the sharing (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see VideoSharing.Direction
	 */
	public int getDirection() {
		if (session instanceof OriginatingVideoStreamingSession) {
			return VideoSharing.Direction.OUTGOING;
		} else {
			return VideoSharing.Direction.INCOMING;
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
        Thread t = new Thread() {
    		public void run() {
    			session.acceptSession();
    		}
    	};
    	t.start();
	}
	
	/**
	 * Rejects video sharing invitation
	 */
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}
		
		// Update rich call history
		RichCallHistory.getInstance().setVideoSharingStatus(session.getSessionID(), VideoSharing.State.ABORTED);

		// Reject invitation
        Thread t = new Thread() {
    		public void run() {
    			session.rejectSession(603);
    		}
    	};
    	t.start();
	}

	/**
	 * Aborts the sharing
	 */
	public void abortSharing() {
		if (logger.isActivated()) {
			logger.info("Cancel session");
		}

		// Abort the session
        Thread t = new Thread() {
    		public void run() {
    			session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
    		}
    	};
    	t.start();	
	}

    /*------------------------------- SESSION EVENTS ----------------------------------*/

	/**
	 * Session is started
	 */
    public void handleSessionStarted() {
		if (logger.isActivated()) {
			logger.info("Session started");
		}
    	synchronized(lock) {
			// Update rich call history
			RichCallHistory.getInstance().setVideoSharingStatus(session.getSessionID(), STARTED);
			startedAt = System.currentTimeMillis();
			
			// Notify event listeners
			mVideoSharingEventBroadcaster.broadcastVideoSharingStateChanged(getRemoteContact(), getSharingId(), STARTED);
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
    	synchronized(lock) {
			// Update rich call history
			if (session.getDialogPath().isSessionCancelled()) {
				RichCallHistory.getInstance().setVideoSharingStatus(session.getSessionID(), ABORTED);
			} else {
				RichCallHistory.getInstance().setVideoSharingStatus(session.getSessionID(), TERMINATED);
				RichCallHistory.getInstance().setVideoSharingDuration(session.getSessionID(), (System.currentTimeMillis()-startedAt)/100);
			}
			
	  		// Notify event listeners
			mVideoSharingEventBroadcaster.broadcastVideoSharingStateChanged(getRemoteContact(), getSharingId(), ABORTED);
	        
	        // Remove session from the list
	        VideoSharingServiceImpl.removeVideoSharingSession(session.getSessionID());
	    }
    }
    
    /**
     * Session has been terminated by remote
     */
    public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session terminated by remote");
		}
    	synchronized(lock) {
			// Update rich call history
			RichCallHistory.getInstance().setVideoSharingStatus(session.getSessionID(), TERMINATED);
			RichCallHistory.getInstance().setVideoSharingDuration(session.getSessionID(), (System.currentTimeMillis()-startedAt)/100);

			// Notify event listeners
			mVideoSharingEventBroadcaster.broadcastVideoSharingStateChanged(getRemoteContact(), getSharingId(), TERMINATED);
	        
	        // Remove session from the list
	        VideoSharingServiceImpl.removeVideoSharingSession(session.getSessionID());
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
    	synchronized(lock) {
			if (error.getErrorCode() == ContentSharingError.SESSION_INITIATION_CANCELLED) {
				// Do nothing here, this is an aborted event
				return;
			}
			// Update rich call history
			RichCallHistory.getInstance().setVideoSharingStatus(session.getSessionID(), FAILED);
			
	  		// Notify event listeners
			switch (error.getErrorCode()) {
				case ContentSharingError.SESSION_INITIATION_DECLINED:
					// TODO : Handle reason code in CR009
					mVideoSharingEventBroadcaster.broadcastVideoSharingStateChanged(getRemoteContact(), getSharingId(), FAILED /*, VideoSharing.Error.INVITATION_DECLINED*/);
					break;
				default:
					// TODO : Handle reason code in CR009
					mVideoSharingEventBroadcaster.broadcastVideoSharingStateChanged(getRemoteContact(), getSharingId(), FAILED /*, VideoSharing.Error.SHARING_FAILED*/);
			}
	
	        // Remove session from the list
	        VideoSharingServiceImpl.removeVideoSharingSession(session.getSessionID());
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
}
