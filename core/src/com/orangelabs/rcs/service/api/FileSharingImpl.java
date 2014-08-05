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

import android.net.Uri;
import android.os.RemoteCallbackList;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.fsh.FileSharing;
import com.gsma.services.rcs.fsh.IFileSharing;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;
import com.orangelabs.rcs.service.broadcaster.IFileSharingEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File sharing implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileSharingImpl extends IFileSharing.Stub implements FileSharingSessionListener {
	
	/**
	 * Core session
	 */
	private FileSharingSession session;

	private final IFileSharingEventBroadcaster mFileSharingEventBroadcaster;

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
	 * @param broadcaster Event broadcaster
	 */
	public FileSharingImpl(FileSharingSession session, IFileSharingEventBroadcaster broadcaster) {
		this.session = session;
		this.mFileSharingEventBroadcaster = broadcaster;

		session.addListener(this);
	}

	/**
	 * Returns the sharing ID of the file sharing
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
	 * Returns the URI of the shared file
	 * 
	 * @return Uri
	 */
	public Uri getFile() {
		return session.getContent().getUri();
	}

	/**
	 * Returns the state of the file sharing
	 * 
	 * @return State 
	 */
	public int getState() {
		int result = FileSharing.State.INACTIVE;
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null) {
			if (dialogPath.isSessionCancelled()) {
				// Session canceled
				result = FileSharing.State.ABORTED;
			} else
			if (dialogPath.isSessionEstablished()) {
				// Session started
				result = FileSharing.State.STARTED;
			} else
			if (dialogPath.isSessionTerminated()) {
				// Session terminated
				if (session.isFileTransfered()) {
					result = FileSharing.State.TRANSFERRED;
				} else {
					result = FileSharing.State.ABORTED;
				}
			} else {
				// Session pending
				if (session instanceof OriginatingFileSharingSession) {
					result = FileSharing.State.INITIATED;
				} else {
					result = FileSharing.State.INVITED;
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns the direction of the sharing (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see FileSharing.Direction
	 */
	public int getDirection() {
		if (session instanceof OriginatingFileSharingSession) {
			return FileSharing.Direction.OUTGOING;
		} else {
			return FileSharing.Direction.INCOMING;
		}
	}		
		
	/**
	 * Accepts file sharing invitation
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
	 * Rejects file sharing invitation
	 */
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}

		// Update rich call history
		RichCallHistory.getInstance().setFileSharingStatus(session.getSessionID(), FileSharing.State.ABORTED);

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

		if (session.isFileTransfered()) {
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
			RichCallHistory.getInstance().setFileSharingStatus(session.getSessionID(), FileSharing.State.STARTED);

			// Notify event listeners
			mFileSharingEventBroadcaster.broadcastFileSharingStateChanged(getRemoteContact(),
					getSharingId(), FileSharing.State.STARTED);
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
			RichCallHistory.getInstance().setFileSharingStatus(session.getSessionID(), FileSharing.State.ABORTED);
			
			// Notify event listeners
			mFileSharingEventBroadcaster.broadcastFileSharingStateChanged(getRemoteContact(),
					getSharingId(), FileSharing.State.ABORTED);
	        
	        // Remove session from the list
			FileSharingServiceImpl.removeFileSharingSession(session.getSessionID());
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
	  		if (session.isFileTransfered()) {
		        // Remove session from the list
	  			FileSharingServiceImpl.removeFileSharingSession(session.getSessionID());
	  		} else {
				// Update rich call history
				RichCallHistory.getInstance().setFileSharingStatus(session.getSessionID(), FileSharing.State.ABORTED);

				// Notify event listeners
				mFileSharingEventBroadcaster.broadcastFileSharingStateChanged(getRemoteContact(),
						getSharingId(), FileSharing.State.ABORTED);

		        // Remove session from the list
				FileSharinggServiceImpl.removeFileSharingSession(session.getSessionID());
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
			RichCallHistory.getInstance().setFileSharingStatus(session.getSessionID(), FileSharing.State.FAILED);
	
			// Notify event listeners
			switch (error.getErrorCode()) {
				case ContentSharingError.SESSION_INITIATION_DECLINED:
					// TODO : Handle reason code in CR009
					mFileSharingEventBroadcaster.broadcastFileSharingStateChanged(getRemoteContact(), getSharingId(), FileSharing.State.FAILED /*, FileSharing.Error.INVITATION_DECLINED*/);
					break;
				case ContentSharingError.MEDIA_SAVING_FAILED:
					// TODO : Handle reason code in CR009
					mFileSharingEventBroadcaster.broadcastFileSharingStateChanged(getRemoteContact(), getSharingId(), FileSharing.State.FAILED /*, FileSharing.Error.SHARING_FAILED*/);
					break;
				case ContentSharingError.MEDIA_TRANSFER_FAILED:
					// TODO : Handle reason code in CR009
					mFileSharingEventBroadcaster.broadcastFileSharingStateChanged(getRemoteContact(), getSharingId(), FileSharing.State.FAILED /*, FileSharing.Error.SHARING_FAILED*/);
					break;
				default:
					// TODO : Handle reason code in CR009
					mFileSharingEventBroadcaster.broadcastFileSharingStateChanged(getRemoteContact(), getSharingId(), FileSharing.State.FAILED /*, FileSharing.Error.SHARING_FAILED*/);
			}
	
	        // Remove session from the list
			FileSharingServiceImpl.removeFileSharingSession(session.getSessionID());
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
			RichCallHistory.getInstance().setFileSharingProgress(session.getSessionID(), currentSize, totalSize);

			// Notify event listeners
			mFileSharingEventBroadcaster.broadcastFileSharingProgress(getRemoteContact(),
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
			logger.info("File transferred");
		}
    	synchronized(lock) {
			// Update rich call history
			RichCallHistory.getInstance().setFileSharingStatus(session.getSessionID(), FileSharing.State.TRANSFERRED);

			// Notify event listeners
			mFileSharingEventBroadcaster.broadcastFileSharingStateChanged(getRemoteContact(),
					getSharingId(), FileSharing.State.TRANSFERRED);
	    }
    }
}
