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

import static com.gsma.services.rcs.ipcall.IPCall.State.FAILED;
import static com.gsma.services.rcs.ipcall.IPCall.State.ABORTED;
import static com.gsma.services.rcs.ipcall.IPCall.State.STARTED;
import static com.gsma.services.rcs.ipcall.IPCall.State.TERMINATED;
import static com.gsma.services.rcs.ipcall.IPCall.State.HOLD;
import static com.gsma.services.rcs.ipcall.IPCall.State.INVITED;

import android.os.RemoteCallbackList;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ipcall.IIPCall;
import com.gsma.services.rcs.ipcall.IIPCallListener;
import com.gsma.services.rcs.ipcall.IIPCallPlayer;
import com.gsma.services.rcs.ipcall.IIPCallRenderer;
import com.gsma.services.rcs.ipcall.IPCall;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallError;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallSession;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallStreamingSessionListener;
import com.orangelabs.rcs.core.ims.service.ipcall.OriginatingIPCallSession;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionError;
import com.orangelabs.rcs.provider.ipcall.IPCallHistory;
import com.orangelabs.rcs.service.broadcaster.IIPCallEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * IP call implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCallImpl extends IIPCall.Stub implements IPCallStreamingSessionListener { 

	/**
	 * Core session
	 */
	private IPCallSession session;

	private final IIPCallEventBroadcaster mIPCallEventBroadcaster;
	/**
	 * Lock used for synchronisation
	 */
	private final Object lock = new Object();

	/**
	 * The logger
	 */
	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param session Session
	 * @param broadcaster IIPCallEventBroadcaster
	 */
	public IPCallImpl(IPCallSession session, IIPCallEventBroadcaster broadcaster) {
		this.session = session;
		mIPCallEventBroadcaster = broadcaster;
		session.addListener(this);
	}

    /**
	 * Returns the call ID of call
	 * 
	 * @return Call ID
	 */
	public String getCallId() {
		return session.getSessionID();
	}

	/**
	 * Get remote contact identifier
	 * 
	 * @return ContactId
	 */
	public ContactId getRemoteContact() {
		return session.getRemoteContact();
	}

	/**
	 * Returns the state of the IP call
	 * 
	 * @return State 
	 */
	public int getState() {
		int result = IPCall.State.INACTIVE;
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null) {
			if (dialogPath.isSessionCancelled()) {
				// Session canceled
				result = IPCall.State.ABORTED;
			} else
			if (dialogPath.isSessionEstablished()) {
				// Session started
				result = IPCall.State.STARTED;
			} else
			if (dialogPath.isSessionTerminated()) {
				// Session terminated
				result = IPCall.State.TERMINATED;
			} else {
				// Session pending
				if (session instanceof OriginatingIPCallSession) {
					result = IPCall.State.INITIATED;
				} else {
					result = IPCall.State.INVITED;
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns the direction of the call (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see IPCall.Direction
	 */
	public int getDirection() {
		if (session instanceof OriginatingIPCallSession) {
			return IPCall.Direction.OUTGOING;
		} else {
			return IPCall.Direction.INCOMING;
		}
	}
	
	/**
	 * Accepts call invitation
	 * 
	 * @param player IP call player
	 * @param renderer IP call renderer
	 */
	public void acceptInvitation(IIPCallPlayer player, IIPCallRenderer renderer) {
		if (logger.isActivated()) {
			logger.info("Accept call invitation");
		}

		// Set player and renderer
		session.setPlayer(player);
		session.setRenderer(renderer);
		
		// Accept invitation
        Thread t = new Thread() {
    		public void run() {
    			session.acceptSession();
    		}
    	};
    	t.start();
	}

	/**
	 * Rejects call invitation
	 */
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}

		// Update IP call history
		IPCallHistory.getInstance().setCallStatus(session.getSessionID(), IPCall.State.ABORTED); 

		// Reject invitation
        Thread t = new Thread() {
    		public void run() {
    			session.rejectSession(603);
    		}
    	};
    	t.start();
	}

	/**
	 * Aborts the call
	 */
	public void abortCall() {
		if (logger.isActivated()) {
			logger.info("Abort session");
		}

		// Abort the session
        Thread t = new Thread() {
    		public void run() {
    			session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
    		}
    	};
    	t.start();			
	}

	/**
	 * Is video activated
	 * 
	 * @return Boolean
	 */
	public boolean isVideo() {
		return session.isVideoActivated();
	}

	/**
	 * Add video stream
	 */
	public void addVideo() {
		if (logger.isActivated()) {
			logger.info("Add video");
		}

		// Add video to session
        Thread t = new Thread() {
    		public void run() {
    			session.addVideo();		
    		}
    	};
    	t.start();			
	}

	/**
	 * Remove video stream
	 */
	public void removeVideo() {
		if (logger.isActivated()) {
			logger.info("Remove video");
		}

		// Remove video from session
        Thread t = new Thread() {
    		public void run() {
    			session.removeVideo();		
    		}
    	};
    	t.start();		
	}

	/**
	 * Accept invitation to add video
	 */
	// TODO
	public void acceptAddVideo() {
		if (logger.isActivated()) {
			logger.info("Accept invitation to add video");
			
		}
		
		// Accept to add video
        Thread t = new Thread() {
    		public void run() {
    			session.getUpdateSessionManager().acceptReInvite();
    		}
    	};
    	t.start();		
	}

	/**
	 * Reject invitation to add video
	 */
	// TODO
	public void rejectAddVideo() {
		if (logger.isActivated()) {
			logger.info("Reject invitation to add video");
		}
		
		//set video content to null
		session.setVideoContent(null);
		
		// Reject add video
        Thread t = new Thread() {
    		public void run() {
    			session.getUpdateSessionManager().rejectReInvite(603);
    		}
    	};
    	t.start();		
	}

	/**
	 * Puts the call on hold
	 */
	public void holdCall() {
		if (logger.isActivated()) {
			logger.info("Hold call");
		}

        Thread t = new Thread() {
    		public void run() {
    			session.setOnHold(true);
    		}
    	};
    	t.start();		
	}

	/**
	 * Continues the call that hold's on
	 */
	public void continueCall() {
		if (logger.isActivated()) {
			logger.info("Continue call");
		}

        Thread t = new Thread() {
    		public void run() {
    			session.setOnHold(false);
    		}
    	};
    	t.start();		
	}

	/**
	 * Is call on hold
	 * 
	 * @return Boolean
	 */
	public boolean isOnHold() {
		// TODO
		return false;
	}

    /*------------------------------- SESSION EVENTS ----------------------------------*/

	/**
	 * Session is started
	 */
    public void handleSessionStarted() {
		if (logger.isActivated()) {
			logger.info("Call started");
		}
		synchronized (lock) {
			// Notify event listeners
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), STARTED);
		}
    }
    
    /**
     * Session has been aborted
     * 
	 * @param reason Termination reason
	 */
    public void handleSessionAborted(int reason) {
		if (logger.isActivated()) {
			logger.info("Call aborted (reason " + reason + ")");
		}
		synchronized (lock) {
			// Update rich messaging history
			if (session.getDialogPath().isSessionCancelled()) {
				IPCallHistory.getInstance().setCallStatus(session.getSessionID(), ABORTED);
			} else {
				IPCallHistory.getInstance().setCallStatus(session.getSessionID(), TERMINATED);
			}

			// Notify event listeners
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), ABORTED);

			// Remove session from the list
			IPCallServiceImpl.removeIPCallSession(session.getSessionID());
		}
    }
    
    /**
     * Session has been terminated by remote
     */
    public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Call terminated by remote");
		}
		synchronized (lock) {
			// Update IP call history
			IPCallHistory.getInstance().setCallStatus(session.getSessionID(), TERMINATED);

			// Notify event listeners
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), ABORTED);

			// Remove session from the list
			IPCallServiceImpl.removeIPCallSession(session.getSessionID());
		}
    }
    
	/**
	 * IP Call error
	 * 
	 * @param error Error
	 */
	public void handleCallError(IPCallError error) {
		if (logger.isActivated()) {
			logger.info("Session error " + error.getErrorCode());
		}
		synchronized (lock) {
			// Update IP call history
			IPCallHistory.getInstance().setCallStatus(session.getSessionID(), FAILED);
			// Notify event listeners
			switch (error.getErrorCode()) {
				case SipSessionError.SESSION_INITIATION_DECLINED:
					// TODO : Handle reason code in CR009
					mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), FAILED /*, IPCall.Error.INVITATION_DECLINED*/);
					break;
				default:
					// TODO : Handle reason code in CR009
					mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), FAILED /*, IPCall.Error.CALL_FAILED*/);
			}

			// Remove session from the list
			IPCallServiceImpl.removeIPCallSession(session.getSessionID());
		}
	}
	
	/**
	 * Add video invitation
	 * 
	 * @param videoEncoding Video encoding
     * @param width Video width
     * @param height Video height
	 */
	public void handleAddVideoInvitation(String videoEncoding, int videoWidth, int videoHeight) {
		if (logger.isActivated()) {
			logger.info("Add video invitation");
		}
		synchronized (lock) {
			// Notify event listeners
			// TODO : Verify if the status change callback listener used is the right one!
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), INVITED);
		}
	}
	
	/**
	 * Remove video invitation
	 */
	public void handleRemoveVideo() {
		if (logger.isActivated()) {
			logger.info("Remove video invitation");
		}
		synchronized (lock) {
			// Notify event listeners
			// TODO : Verify if the status change callback listener used is the right one!
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), ABORTED);
		}
	}

	/**
	 * Add video has been accepted by user 
	 */
	public void handleAddVideoAccepted() {
		if (logger.isActivated()) {
			logger.info("Add video accepted");
		}
		synchronized (lock) {
			// Notify event listeners
			// TODO : Verify if the status change callback listener used is the right one!
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), STARTED);
		}
	}

	/**
	 * Remove video has been accepted by user 
	 */
	public void handleRemoveVideoAccepted() {
		if (logger.isActivated()) {
			logger.info("Remove video accepted");
		}
		synchronized (lock) {
			// Notify event listeners
			// TODO : Verify if the status change callback listener used is the right one!
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), ABORTED);
		}
	}
	
	/**
	 * Add video has been aborted
	 * 
	 * @param reason Termination reason
	 */
	public void handleAddVideoAborted(int reason) {
		if (logger.isActivated()) {
			logger.info("Add video aborted (reason " + reason + ")");
		}
		synchronized (lock) {
	        // Notify event listeners
			// TODO : Verify if the status change callback listener used is the right one!
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), ABORTED);
		}
	}
	
	/**
	 * Remove video has been aborted
	 * 
	 * @param reason Termination reason
	 */
	public void handleRemoveVideoAborted(int reason) {
		if (logger.isActivated()) {
			logger.info("Remove video aborted (reason " + reason + ")");
		}
		synchronized (lock) {
	        // Notify event listeners
			// TODO : Verify if the status change callback listener used is the right one!
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), ABORTED);
		}		
	}
	
	/**
	 * Call Hold invitation
	 * 
	 */
	public void handleCallHold() {
		if (logger.isActivated()) {
			logger.info("Call hold");
		}
		synchronized (lock) {
			// Update IP call history
			IPCallHistory.getInstance().setCallStatus(session.getSessionID(), HOLD);

			// Notify event listeners
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), HOLD);
		}
	}

	/**
	 * Call Resume invitation
	 * 
	 */
	public void handleCallResume() {
		if (logger.isActivated()) {
			logger.info("Call Resume invitation");
		}
		synchronized (lock) {
			// Update IP call history
			IPCallHistory.getInstance().setCallStatus(session.getSessionID(), STARTED);

			// Notify event listeners
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), STARTED);
		}
	}

	/**
	 * Call Hold has been accepted
	 * 
	 */
	public void handleCallHoldAccepted() {
		if (logger.isActivated()) {
			logger.info("Call Hold accepted");
		}
		synchronized (lock) {
			// Notify event listeners
			// TODO : Verify if the status change callback listener used is the right one!
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), HOLD);
		}
	}

	/**
	 * Call Hold has been aborted
	 * 
	 * @param reason Termination reason
	 */
	public void handleCallHoldAborted(int errorCode) {
		if (logger.isActivated()) {
			logger.info("Call Hold aborted (reason " + errorCode + ")");
		}
		synchronized (lock) {
	        // Notify event listeners
			// TODO : Verify if the status change callback listener used is the right one!
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), ABORTED);
		}		
	}

	/**
	 * Call Resume has been accepted
	 * 
	 */
	public void handleCallResumeAccepted() {
		if (logger.isActivated()) {
			logger.info("Call Resume accepted");
		}
		synchronized (lock) {
			// Notify event listeners
			// TODO : Verify if the status change callback listener used is the right one!
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), STARTED);
		}
	}

	/**
	 * Call Resume has been aborted
	 * 
	 * @param reason Termination reason
	 */
	public void handleCallResumeAborted(int code) {
		if (logger.isActivated()) {
			logger.info("Call Resume aborted (reason " + code + ")");
		}
		synchronized (lock) {
			// TODO : Verify if the status change callback listener used is the right one!
			mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), ABORTED);
		}		
	}

	/**
	 * Called user is Busy
	 */
	public void handle486Busy() {
		// Notify event listeners
		// TODO : Verify if the status change callback listener used is the right one!
		mIPCallEventBroadcaster.broadcastIPCallStateChanged(getRemoteContact(), getCallId(), FAILED);
	}

    /**
     * Video stream has been resized
     *
     * @param width Video width
     * @param height Video height
     */
	public void handleVideoResized(int width, int height) {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Video resized to " + width + "x" + height);
			}

			// Notify event listeners
			/*final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).handleVideoResized(width,
							height);
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't notify listener", e);
					}
				}
			}
			listeners.finishBroadcast();*/
			// TODO
		}
	}
}
