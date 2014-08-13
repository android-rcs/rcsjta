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

import static com.gsma.services.rcs.ish.ImageSharing.State.ABORTED;
import static com.gsma.services.rcs.ish.ImageSharing.State.FAILED;
import static com.gsma.services.rcs.ish.ImageSharing.State.STARTED;
import static com.gsma.services.rcs.ish.ImageSharing.State.TRANSFERRED;
import android.net.Uri;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ish.IImageSharing;
import com.gsma.services.rcs.ish.ImageSharing;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSessionListener;
import com.orangelabs.rcs.core.ims.service.richcall.image.OriginatingImageTransferSession;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;
import com.orangelabs.rcs.service.broadcaster.IImageSharingEventBroadcaster;
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

	private final IImageSharingEventBroadcaster mImageSharingEventBroadcaster;

	/**
	 * Lock used for synchronization
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
	 * @param mImageSharingEventBroadcaster IImageSharingEventBroadcaster
	 */
	public ImageSharingImpl(ImageTransferSession session,
			IImageSharingEventBroadcaster broadcaster) {
		this.session = session;
		mImageSharingEventBroadcaster = broadcaster;

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
	 * Returns the remote contact identifier
	 * 
	 * @return ContactId
	 */
	public ContactId getRemoteContact() {
		return session.getRemoteContact();
	}
	
	/**
     * Returns the complete filename including the path of the file to be transferred
     *
     * @return Filename
     */
	public String getFileName() {
		return session.getContent().getName();
	}

	/**
	 * Returns the Uri of the file to be transferred
	 *
	 * @return Filename
	 */
	public Uri getFile() {
		return session.getContent().getUri();
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
		int result = ImageSharing.State.INACTIVE;
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null) {
			if (dialogPath.isSessionCancelled()) {
				// Session canceled
				result = ImageSharing.State.ABORTED;
			} else
			if (dialogPath.isSessionEstablished()) {
				// Session started
				result = ImageSharing.State.STARTED;
			} else
			if (dialogPath.isSessionTerminated()) {
				// Session terminated
				if (session.isImageTransfered()) {
					result = ImageSharing.State.TRANSFERRED;
				} else {
					result = ImageSharing.State.ABORTED;
				}
			} else {
				// Session pending
				if (session instanceof OriginatingImageTransferSession) {
					result = ImageSharing.State.INITIATED;
				} else {
					result = ImageSharing.State.INVITED;
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns the direction of the sharing (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see ImageSharing.Direction
	 */
	public int getDirection() {
		if (session.isInitiatedByRemote()) {
			return ImageSharing.Direction.INCOMING;
		} else {
			return ImageSharing.Direction.OUTGOING;
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
        Thread t = new Thread() {
    		public void run() {
    			session.acceptSession();
    		}
    	};
    	t.start();
	}
	
	/**
	 * Rejects image sharing invitation
	 */
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}

		// Update rich call history
		RichCallHistory.getInstance().setImageSharingStatus(session.getSessionID(), ImageSharing.State.ABORTED);

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

		if (session.isImageTransfered()) {
			// Automatically closed after transfer
			return;
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
			RichCallHistory.getInstance().setImageSharingStatus(session.getSessionID(), STARTED);

			// Notify event listeners
			mImageSharingEventBroadcaster.broadcastImageSharingStateChanged(getRemoteContact(),
					getSharingId(), STARTED);
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
			RichCallHistory.getInstance().setImageSharingStatus(session.getSessionID(), ABORTED);
			
			// Notify event listeners
			mImageSharingEventBroadcaster.broadcastImageSharingStateChanged(getRemoteContact(),
					getSharingId(), ABORTED);
	        
	        // Remove session from the list
	        ImageSharingServiceImpl.removeImageSharingSession(session.getSessionID());
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
			// Check if the file has been transferred or not
	  		if (session.isImageTransfered()) {
		        // Remove session from the list
	  			ImageSharingServiceImpl.removeImageSharingSession(session.getSessionID());
	  		} else {
				// Update rich call history
				RichCallHistory.getInstance().setImageSharingStatus(session.getSessionID(), ABORTED);

				// Notify event listeners
				mImageSharingEventBroadcaster.broadcastImageSharingStateChanged(getRemoteContact(),
						getSharingId(), ABORTED);

		        // Remove session from the list
				ImageSharingServiceImpl.removeImageSharingSession(session.getSessionID());
	  		}
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
			RichCallHistory.getInstance().setImageSharingStatus(session.getSessionID(), FAILED);
	
			// Notify event listeners
			switch (error.getErrorCode()) {
				case ContentSharingError.SESSION_INITIATION_DECLINED:
					// TODO : Handle reason code in CR009
					mImageSharingEventBroadcaster.broadcastImageSharingStateChanged(getRemoteContact(), getSharingId(), FAILED /*, ImageSharing.Error.INVITATION_DECLINED*/);
					break;
				case ContentSharingError.MEDIA_SAVING_FAILED:
					// TODO : Handle reason code in CR009
					mImageSharingEventBroadcaster.broadcastImageSharingStateChanged(getRemoteContact(), getSharingId(), FAILED /*, ImageSharing.Error.SHARING_FAILED*/);
					break;
				case ContentSharingError.MEDIA_TRANSFER_FAILED:
					// TODO : Handle reason code in CR009
					mImageSharingEventBroadcaster.broadcastImageSharingStateChanged(getRemoteContact(), getSharingId(), FAILED /*, ImageSharing.Error.SHARING_FAILED*/);
					break;
				default:
					// TODO : Handle reason code in CR009
					mImageSharingEventBroadcaster.broadcastImageSharingStateChanged(getRemoteContact(), getSharingId(), FAILED /*, ImageSharing.Error.SHARING_FAILED*/);
			}
	
	        // Remove session from the list
	        ImageSharingServiceImpl.removeImageSharingSession(session.getSessionID());
	    }
    }
    
    /**
     * Content sharing progress
     *
     * @param currentSize Data size transferred
     * @param totalSize Total size to be transferred
     */
    public void handleSharingProgress(long currentSize, long totalSize) {
    	synchronized(lock) {
			// Update rich call history
			RichCallHistory.getInstance().setImageSharingProgress(session.getSessionID(), currentSize, totalSize);

			// Notify event listeners
			mImageSharingEventBroadcaster.broadcastImageSharingProgress(getRemoteContact(),
					getSharingId(), currentSize, totalSize);
	     }
    }
    
    /**
     * Content has been transferred
     *
     * @param filename Filename associated to the received content
     */
    public void handleContentTransfered(Uri file) {
		if (logger.isActivated()) {
			logger.info("Image transferred");
		}
    	synchronized(lock) {
			// Update rich call history
			RichCallHistory.getInstance().setImageSharingStatus(session.getSessionID(), TRANSFERRED);

			// Notify event listeners
			mImageSharingEventBroadcaster.broadcastImageSharingStateChanged(getRemoteContact(),
					getSharingId(), TRANSFERRED);
	    }
    }
}
