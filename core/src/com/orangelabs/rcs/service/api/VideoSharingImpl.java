/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.service.api;

import org.gsma.joyn.vsh.IVideoRenderer;
import org.gsma.joyn.vsh.IVideoSharing;
import org.gsma.joyn.vsh.IVideoSharingListener;
import org.gsma.joyn.vsh.VideoSharing;

import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSessionListener;
import com.orangelabs.rcs.provider.sharing.RichCall;
import com.orangelabs.rcs.provider.sharing.RichCallData;
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
	
	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IVideoSharingListener> listeners = new RemoteCallbackList<IVideoSharingListener>();

	/**
	 * Lock used for synchronisation
	 */
	private Object lock = new Object();

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param session Session
	 */
	public VideoSharingImpl(VideoStreamingSession session) {
		this.session = session;
		
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
	 * Returns the remote contact
	 * 
	 * @return Contact
	 */
	public String getRemoteContact() {
		return session.getRemoteContact();
	}
	
	/**
	 * Returns the video encoding
	 * 
	 * @return Encoding name (e.g. H264)
	 */
	public String getVideoEncoding() {
		// TODO
		return null;
	}
	
	/**
	 * Returns the video format
	 * 
	 * @return Format (e.g. QCIF)
	 */
	public String getVideoFormat() {
		// TODO
		return null;
	}
	
	/**
	 * Returns the state of the sharing
	 * 
	 * @return State
	 * @see VideoSharing.State
	 */
	public int getState() {
		// TODO
		int state = ServerApiUtils.getSessionState(session);
		switch(state) {
			case SessionState.PENDING:
				return VideoSharing.State.INITIATED;
			
			case SessionState.ESTABLISHED:
				return VideoSharing.State.STARTED;
			
			case SessionState.CANCELLED:
				return VideoSharing.State.INITIATED;
			
			case SessionState.TERMINATED:
				return VideoSharing.State.ABORTED;

			default:
				return VideoSharing.State.UNKNOWN;
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
		session.setMediaRenderer(renderer);
		
		// Accept invitation
		session.acceptSession();
	}
	
	/**
	 * Rejects video sharing invitation
	 */
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}
		
		// Update rich call history
		RichCall.getInstance().setStatus(session.getSessionID(), RichCallData.STATUS_CANCELED);

		// Reject invitation
		session.rejectSession(603);
	}

	/**
	 * Aborts the sharing
	 */
	public void abortSharing() {
		if (logger.isActivated()) {
			logger.info("Cancel session");
		}

		// Abort the session
		session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
	}

	/**
	 * Adds a listener on video sharing events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener(IVideoSharingListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an event listener");
		}

    	synchronized(lock) {
    		listeners.register(listener);
    	}
	}

	/**
	 * Removes a listener from video sharing
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener(IVideoSharingListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove an event listener");
		}

    	synchronized(lock) {
    		listeners.unregister(listener);
    	}
	}

    /*------------------------------- SESSION EVENTS ----------------------------------*/

	/**
	 * Session is started
	 */
    public void handleSessionStarted() {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session started");
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onSharingStarted();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();		
	    }
    }
    
    /**
     * Session has been aborted
     * 
	 * @param reason Termination reason
	 */
    public void handleSessionAborted(int reason) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session aborted (reason " + reason + ")");
			}
	
			// Update rich call history
			RichCall.getInstance().setStatus(session.getSessionID(), RichCallData.STATUS_CANCELED);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onSharingAborted();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        VideoSharingServiceImpl.removeVideoSharingSession(session.getSessionID());
	    }
    }
    
    /**
     * Session has been terminated by remote
     */
    public void handleSessionTerminatedByRemote() {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session terminated by remote");
			}
	
			// Update rich call history
			RichCall.getInstance().setStatus(session.getSessionID(), RichCallData.STATUS_TERMINATED);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onSharingAborted();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();		
	        
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
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Sharing error " + error.getErrorCode());
			}
	
			// Update rich call history
			RichCall.getInstance().setStatus(session.getSessionID(), RichCallData.STATUS_FAILED);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	int code;
	            	switch(error.getErrorCode()) {
            			case ContentSharingError.SESSION_INITIATION_CANCELLED:
	            			code = VideoSharing.Error.SHARING_CANCELLED;
	            			break;
            			case ContentSharingError.SESSION_INITIATION_DECLINED:
	            			code = VideoSharing.Error.INVITATION_DECLINED;
	            			break;
	            		default:
	            			code = VideoSharing.Error.SHARING_FAILED;
	            	}
	            	listeners.getBroadcastItem(i).onSharingError(code);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        VideoSharingServiceImpl.removeVideoSharingSession(session.getSessionID());
	    }
    }
    
    /**
     * The size of media has changed
     *
     * @param width Video width
     * @param height Video height
     */
    public void handleMediaResized(int width, int height) {
    	// TODO
    }
}
