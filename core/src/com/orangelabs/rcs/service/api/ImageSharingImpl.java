package com.orangelabs.rcs.service.api;

import org.gsma.joyn.ish.IImageSharingListener;
import org.gsma.joyn.ish.ImageSharing;
import org.gsma.joyn.ish.IImageSharing;

import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSessionListener;
import com.orangelabs.rcs.provider.sharing.RichCall;
import com.orangelabs.rcs.provider.sharing.RichCallData;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Image sharing implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingImpl extends IImageSharing.Stub implements ImageTransferSessionListener {
	
	/**
	 * Core session
	 */
	private ImageTransferSession session;
	
	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IImageSharingListener> listeners = new RemoteCallbackList<IImageSharingListener>();

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
	public ImageSharingImpl(ImageTransferSession session) {
		this.session = session;
		
		session.addListener(this);
	}

	/**
	 * Returns the sharing ID of the image sharing
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
     * Returns the complete filename including the path of the file to be transfered
     *
     * @return Filename
     */
	public String getFileName() {
		return session.getContent().getName();
	}

	/**
     * Returns the size of the file to be transferred
     *
     * @return Size in bytes
     */
	public long getFileSize() {
		return session.getContent().getSize();
	}	

    /**
     * Returns the MIME type of the file to be transferred
     * 
     * @return Type
     */
    public String getFileType() {
        return session.getContent().getEncoding();
    }

	/**
	 * Returns the state of the image sharing
	 * 
	 * @return State 
	 */
	public int getState() {
		// TODO
		int state = ServerApiUtils.getSessionState(session);
		switch(state) {
			case SessionState.PENDING:
				return ImageSharing.State.INITIATED;
			
			case SessionState.ESTABLISHED:
				return ImageSharing.State.STARTED;
			
			case SessionState.CANCELLED:
				return ImageSharing.State.INITIATED;
			
			case SessionState.TERMINATED:
				return ImageSharing.State.TRANSFERED;

			default:
				return ImageSharing.State.UNKNOWN;
		}
	}		
		
	/**
	 * Accepts image sharing invitation
	 */
	public void acceptInvitation() {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}

		// Accept invitation
		session.acceptSession();
	}
	
	/**
	 * Rejects image sharing invitation
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

		if (session.isImageTransfered()) {
			// Automatically closed after transfer
			return;
		}
		
		// Abort the session
		session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
	}

	/**
	 * Adds a listener on image sharing events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener(IImageSharingListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an event listener");
		}

    	synchronized(lock) {
    		listeners.register(listener);
    	}
	}
	
	/**
	 * Removes a listener on image sharing
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener(IImageSharingListener listener) {
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
	        ImageSharingServiceImpl.removeImageSharingSession(session.getSessionID());
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
			
	  		if (session.isImageTransfered()) {
				// The image has been received, so only remove session from the list
	  			ImageSharingServiceImpl.removeImageSharingSession(session.getSessionID());
	  			return;
	  		}
			
			// Update rich call history
			RichCall.getInstance().setStatus(session.getSessionID(), RichCallData.STATUS_FAILED);
	
	        // Remove session from the list
			ImageSharingServiceImpl.removeImageSharingSession(session.getSessionID());
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
	            			code = ImageSharing.Error.SHARING_CANCELLED;
	            			break;
            			case ContentSharingError.SESSION_INITIATION_DECLINED:
	            			code = ImageSharing.Error.INVITATION_DECLINED;
	            			break;
	            		case ContentSharingError.MEDIA_SAVING_FAILED:
	            			code = ImageSharing.Error.SAVING_FAILED;
	            			break;
	            		case ContentSharingError.MEDIA_SIZE_TOO_BIG:
	            			code = ImageSharing.Error.SIZE_TOO_BIG;
	            			break;
	            		case ContentSharingError.MEDIA_TRANSFER_FAILED:
	            			code = ImageSharing.Error.SHARING_FAILED;
	            			break;
	            		case ContentSharingError.UNSUPPORTED_MEDIA_TYPE:
	            			code = ImageSharing.Error.UNSUPPORTED_TYPE;
	            			break;
	            		default:
	            			code = ImageSharing.Error.SHARING_FAILED;
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
	        ImageSharingServiceImpl.removeImageSharingSession(session.getSessionID());
	    }
    }
    
    /**
     * Content sharing progress
     *
     * @param currentSize Data size transfered
     * @param totalSize Total size to be transfered
     */
    public void handleSharingProgress(long currentSize, long totalSize) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.debug("Sharing progress");
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onSharingProgress(currentSize, totalSize);
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
     * Content has been transfered
     *
     * @param filename Filename associated to the received content
     */
    public void handleContentTransfered(String filename) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Image transfered");
			}
	
			// Update rich call history
			RichCall.getInstance().setStatus(session.getSessionID(), RichCallData.STATUS_TERMINATED);
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onImageShared(filename);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }
    }
}
