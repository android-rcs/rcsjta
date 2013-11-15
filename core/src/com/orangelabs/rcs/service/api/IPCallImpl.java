package com.orangelabs.rcs.service.api;

import org.gsma.joyn.ipcall.IIPCall;
import org.gsma.joyn.ipcall.IIPCallListener;
import org.gsma.joyn.ipcall.IIPCallPlayer;
import org.gsma.joyn.ipcall.IIPCallRenderer;
import org.gsma.joyn.ipcall.IPCall;

import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallError;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallSession;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallStreamingSessionListener;
import com.orangelabs.rcs.core.ims.service.ipcall.OriginatingIPCallSession;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionError;
import com.orangelabs.rcs.provider.ipcall.IPCallHistory;
import com.orangelabs.rcs.utils.PhoneUtils;
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

	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IIPCallListener> listeners = new RemoteCallbackList<IIPCallListener>();

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
	public IPCallImpl(IPCallSession session) {
		this.session = session;
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
	 * Get remote contact
	 * 
	 * @return Contact
	 */
	public String getRemoteContact() {
		return PhoneUtils.extractNumberFromUri(session.getRemoteContact());
	}

	/**
	 * Returns the state of the IP call
	 * 
	 * @return State 
	 */
	public int getState() {
		int result = IPCall.State.UNKNOWN;
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
				result = IPCall.State.ABORTED;
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
		session.acceptSession();
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
		session.rejectSession(603);
	}

	/**
	 * Aborts the call
	 */
	public void abortCall() {
		if (logger.isActivated()) {
			logger.info("Abort session");
		}

		// Abort the session
		session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
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
		session.addVideo();		
	}

	/**
	 * Remove video stream
	 */
	public void removeVideo() {
		if (logger.isActivated()) {
			logger.info("Remove video");
		}

		// Remove video from session
		session.removeVideo();		
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
		session.getUpdateSessionManager().acceptReInvite();
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
		session.getUpdateSessionManager().rejectReInvite(603);
	}

	/**
	 * Puts the call on hold
	 */
	public void holdCall() {
		if (logger.isActivated()) {
			logger.info("Hold call");
		}

		session.setOnHold(true);
	}

	/**
	 * Continues the call that hold's on
	 */
	public void continueCall() {
		if (logger.isActivated()) {
			logger.info("Continue call");
		}

		session.setOnHold(false);
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

	/**
	 * Adds a listener on IP call events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener(IIPCallListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an event listener");
		}

		synchronized (lock) {
			listeners.register(listener);
		}
	}

	/**
	 * Removes a listener from IP call events
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener(IIPCallListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove an event listener");
		}

		synchronized (lock) {
			listeners.unregister(listener);
		}
	}
	
    /*------------------------------- SESSION EVENTS ----------------------------------*/
	
	/**
	 * Session is started
	 */
    public void handleSessionStarted() {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Call started");
			}

			// Notify event listeners
			final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).onCallStarted();
				} catch (Exception e) {
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
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Call aborted (reason " + reason + ")");
			}

			// Update rich messaging history
			if (session.getDialogPath().isSessionCancelled()) {
				IPCallHistory.getInstance().setCallStatus(session.getSessionID(), IPCall.State.ABORTED); 
			} else {
				IPCallHistory.getInstance().setCallStatus(session.getSessionID(), IPCall.State.TERMINATED); 
			}

			// Notify event listeners
			final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).onCallAborted();
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't notify listener", e);
					}
				}
			}
			listeners.finishBroadcast();

			// Remove session from the list
			IPCallServiceImpl.removeIPCallSession(session.getSessionID());
		}
    }
    
    /**
     * Session has been terminated by remote
     */
    public void handleSessionTerminatedByRemote() {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Call terminated by remote");
			}

			// Update IP call history
			IPCallHistory.getInstance().setCallStatus(session.getSessionID(), IPCall.State.TERMINATED); 

			// Notify event listeners
			final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).onCallAborted();
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't notify listener", e);
					}
				}
			}
			listeners.finishBroadcast();

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
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Session error " + error.getErrorCode());
			}

			// Update IP call history
			IPCallHistory.getInstance().setCallStatus(session.getSessionID(), IPCall.State.FAILED); 

			// Notify event listeners
			final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
	            	int code;
	            	switch(error.getErrorCode()) {
            			case SipSessionError.SESSION_INITIATION_DECLINED:
	            			code = IPCall.Error.INVITATION_DECLINED;
	            			break;
	            		default:
	            			code = IPCall.Error.CALL_FAILED;
	            	}
					listeners.getBroadcastItem(i).onCallError(code);
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't notify listener", e);
					}
				}
			}
			listeners.finishBroadcast();

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
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Add video invitation");
			}

			// Notify event listeners
/*			final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).handleAddVideoInvitation(
							videoEncoding, videoWidth, videoHeight);
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
	
	/**
	 * Remove video invitation
	 */
	public void handleRemoveVideo() {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Remove video invitation");
			}

			// Notify event listeners
			/*final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).handleRemoveVideo();
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

	/**
	 * Add video has been accepted by user 
	 */
	public void handleAddVideoAccepted() {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Add video accepted");
			}

			// Notify event listeners
			/*final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).handleAddVideoAccepted();
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

	/**
	 * Remove video has been accepted by user 
	 */
	public void handleRemoveVideoAccepted() {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Remove video accepted");
			}

			// Notify event listeners
			/*final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).handleRemoveVideoAccepted();
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
	
	/**
	 * Add video has been aborted
	 * 
	 * @param reason Termination reason
	 */
	public void handleAddVideoAborted(int reason) {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Add video aborted (reason " + reason + ")");
			}

	        // Notify event listeners
			/*final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).handleAddVideoAborted(reason);
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
	
	/**
	 * Remove video has been aborted
	 * 
	 * @param reason Termination reason
	 */
	public void handleRemoveVideoAborted(int reason) {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Remove video aborted (reason " + reason + ")");
			}

	        // Notify event listeners
			/*final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).handleRemoveVideoAborted(reason);
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
	
	/**
	 * Call Hold invitation
	 * 
	 */
	public void handleCallHold() {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Call hold");
			}

			// Update IP call history
			IPCallHistory.getInstance().setCallStatus(session.getSessionID(), IPCall.State.HOLD); 

			// Notify event listeners
			final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).onCallHeld();
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't notify listener", e);
					}
				}
			}
			listeners.finishBroadcast();
		}
	}

	/**
	 * Call Resume invitation
	 * 
	 */
	public void handleCallResume() {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Call Resume invitation");
			}

			// Update IP call history
			IPCallHistory.getInstance().setCallStatus(session.getSessionID(), IPCall.State.STARTED); 

			// Notify event listeners
			final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).onCallContinue();
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't notify listener", e);
					}
				}
			}
			listeners.finishBroadcast();
		}
	}

	/**
	 * Call Hold has been accepted
	 * 
	 */
	public void handleCallHoldAccepted() {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Call Hold accepted");
			}

			// Notify event listeners
			/*final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).handleCallHoldAccepted();
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

	/**
	 * Call Hold has been aborted
	 * 
	 * @param reason Termination reason
	 */
	public void handleCallHoldAborted(int errorCode) {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Call Hold aborted (reason " + errorCode + ")");
			}

	        // Notify event listeners
			/*final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).handleCallHoldAborted(errorCode);
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

	/**
	 * Call Resume has been accepted
	 * 
	 */
	public void handleCallResumeAccepted() {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Call Resume accepted");
			}

			// Notify event listeners
			/*final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).handleCallResumeAccepted();
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

	/**
	 * Call Resume has been aborted
	 * 
	 * @param reason Termination reason
	 */
	public void handleCallResumeAborted(int code) {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Call Resume aborted (reason " + code + ")");
			}

	        // Notify event listeners
			/*final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).handleCallResumeAborted(code);
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

	/**
	 * Called user is Busy
	 */
	public void handle486Busy() {
		// TODO
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
