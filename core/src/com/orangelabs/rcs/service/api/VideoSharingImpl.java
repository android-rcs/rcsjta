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

import org.gsma.joyn.JoynService;
import org.gsma.joyn.vsh.IVideoRenderer;
import org.gsma.joyn.vsh.IVideoSharing;
import org.gsma.joyn.vsh.IVideoSharingListener;
import org.gsma.joyn.vsh.VideoSharing;

import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.video.OriginatingVideoStreamingSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSessionListener;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;
import com.orangelabs.rcs.utils.PhoneUtils;
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
	 * Started at
	 */
	private long startedAt;
	
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
		return PhoneUtils.extractNumberFromUri(session.getRemoteContact());
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
		int result = VideoSharing.State.UNKNOWN;
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
				result = VideoSharing.State.ABORTED;
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
		RichCallHistory.getInstance().setVideoSharingStatus(session.getSessionID(), VideoSharing.State.ABORTED);

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
	 * Removes a listener from video sharing events
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
	
			// Update rich call history
			RichCallHistory.getInstance().setVideoSharingStatus(session.getSessionID(), VideoSharing.State.STARTED);
			startedAt = System.currentTimeMillis();
			
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
			if (session.getDialogPath().isSessionCancelled()) {
				RichCallHistory.getInstance().setVideoSharingStatus(session.getSessionID(), VideoSharing.State.ABORTED);
			} else {
				RichCallHistory.getInstance().setVideoSharingStatus(session.getSessionID(), VideoSharing.State.TERMINATED);
				RichCallHistory.getInstance().setVideoSharingDuration(session.getSessionID(), (System.currentTimeMillis()-startedAt)/100);
			}
			
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
			RichCallHistory.getInstance().setVideoSharingStatus(session.getSessionID(), VideoSharing.State.TERMINATED);
			RichCallHistory.getInstance().setVideoSharingDuration(session.getSessionID(), (System.currentTimeMillis()-startedAt)/100);

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
			if (error.getErrorCode() == ContentSharingError.SESSION_INITIATION_CANCELLED) {
				// Do nothing here, this is an aborted event
				return;
			}

			if (logger.isActivated()) {
				logger.info("Sharing error " + error.getErrorCode());
			}
	
			// Update rich call history
			RichCallHistory.getInstance().setVideoSharingStatus(session.getSessionID(), VideoSharing.State.FAILED);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	int code;
	            	switch(error.getErrorCode()) {
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
     * Video stream has been resized
     *
     * @param width Video width
     * @param height Video height
     */
    public void handleVideoResized(int width, int height) {
    	// TODO
    }

    /**
	 * Returns service version.
	 */
	@Override
	public int getServiceVersion() throws RemoteException {
		if (logger.isActivated()) {
			logger.info("Service Version:" + JoynService.Build.GSMA_VERSION);
		}
		return JoynService.Build.GSMA_VERSION;
	}
}
