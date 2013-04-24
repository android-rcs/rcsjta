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

package com.orangelabs.rcs.service.api.server.messaging;

import java.util.List;

import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.service.api.client.SessionState;
import com.orangelabs.rcs.service.api.client.eventslog.EventsLogApi;
import com.orangelabs.rcs.service.api.client.messaging.IFileTransferEventListener;
import com.orangelabs.rcs.service.api.client.messaging.IFileTransferSession;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File transfer session
 * 
 * @author jexa7410
 */
public class FileTransferSession extends IFileTransferSession.Stub implements FileSharingSessionListener {
	
	/**
	 * Core session
	 */
	private FileSharingSession session;
	
	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IFileTransferEventListener> listeners = new RemoteCallbackList<IFileTransferEventListener>();

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
	public FileTransferSession(FileSharingSession session) {
		this.session = session;
		session.addListener(this);
	}

	/**
	 * Get session ID
	 * 
	 * @return Session ID
	 */
	public String getSessionID() {
		return session.getSessionID();
	}
	
	/**
	 * Get remote contact
	 * 
	 * @return Contact
	 */
	public String getRemoteContact() {
		return session.getRemoteContact();
	}
	
	/**
	 * Get session state
	 * 
	 * @return State (see class SessionState) 
	 * @see SessionState
	 */
	public int getSessionState() {
		return ServerApiUtils.getSessionState(session);
	}
	
	/**
     * Get filename
     *
     * @return Filename
     */
	public String getFilename() {
		return session.getContent().getName();
	}

	/**
     * Get file size
     *
     * @return Size in bytes
     */
	public long getFilesize() {
		return session.getContent().getSize();
	}	

    /**
     * Get file thumbnail
     * 
     * @return Thumbnail
     */
    public byte[] getFileThumbnail() {
        return session.getThumbnail();
    }

    /**
     * Is group transfer
     *
     * @return Boolean
     */
    public boolean isGroupTransfer() {
        // TODO: NOT YET IMPLEMENTED
        return false;
    }

    /**
     * Is HTTP transfer
     *
     * @return Boolean
     */
    public boolean isHttpTransfer() {
        // TODO: NOT YET IMPLEMENTED
        return false;
    }

    /**
     * Get list of contacts (only for group transfer)
     *
     * @return List of contacts
     */
    public List<String> getContacts() {
        // TODO: NOT YET IMPLEMENTED
        return null;
    }

	/**
	 * Accept the session invitation
	 */
	public void acceptSession() {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}
		
		// Accept invitation
		session.acceptSession();

	}
	
	/**
	 * Reject the session invitation
	 */
	public void rejectSession() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}
		
		// Update rich messaging history
  		RichMessaging.getInstance().updateFileTransferStatus(session.getSessionID(), EventsLogApi.STATUS_CANCELED);

  		// Reject invitation
		session.rejectSession(603);
	}

	/**
	 * Cancel the session
	 */
	public void cancelSession() {
		if (logger.isActivated()) {
			logger.info("Cancel session");
		}
		
		if (session.isFileTransfered()) {
			// File already transfered and session automatically closed after transfer
			return;
		}

		// Abort the session
		session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
	}

    /**
     * Resume the session (only for HTTP transfer)
     */
    public void resumeSession() {
        // TODO: NOT YET IMPLEMENTED
    }

	/**
	 * Add session listener
	 * 
	 * @param listener Listener
	 */
	public void addSessionListener(IFileTransferEventListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an event listener");
		}

    	synchronized(lock) {
    		listeners.register(listener);
    	}
	}
	
	/**
	 * Remove session listener
	 * 
	 * @param listener Listener
	 */
	public void removeSessionListener(IFileTransferEventListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove an event listener");
		}

    	synchronized(lock) {
    		listeners.unregister(listener);
    	}
	}
	
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
	            	listeners.getBroadcastItem(i).handleSessionStarted();
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
	
			// Update rich messaging history
			RichMessaging.getInstance().updateFileTransferStatus(session.getSessionID(), EventsLogApi.STATUS_CANCELED);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleSessionAborted(reason);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        MessagingApiService.removeFileTransferSession(session.getSessionID());
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
	
	  		if (session.isFileTransfered()) {
				// The file has been received, so only remove session from the list
				MessagingApiService.removeFileTransferSession(session.getSessionID());
	  			return;
	  		}
	  		
			// Update rich messaging history
	  		RichMessaging.getInstance().updateFileTransferStatus(session.getSessionID(), EventsLogApi.STATUS_FAILED);
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleSessionTerminatedByRemote();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	
	        // Remove session from the list
	        MessagingApiService.removeFileTransferSession(session.getSessionID());
	    }
    }
    
    /**
     * File transfer error
     * 
     * @param error Error
     */
    public void handleTransferError(FileSharingError error) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Sharing error " + error.getErrorCode());
			}
	
			// Update rich messaging history
	  		RichMessaging.getInstance().updateFileTransferStatus(session.getSessionID(), EventsLogApi.STATUS_FAILED);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleTransferError(error.getErrorCode());
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        MessagingApiService.removeFileTransferSession(session.getSessionID());
	    }
    }
    
    /**
	 * File transfer progress
	 * 
	 * @param currentSize Data size transfered 
	 * @param totalSize Total size to be transfered
	 */
    public void handleTransferProgress(long currentSize, long totalSize) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.debug("Sharing progress");
			}
			
			// Update rich messaging history
	  		RichMessaging.getInstance().updateFileTransferProgress(session.getSessionID(), currentSize, totalSize);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleTransferProgress(currentSize, totalSize);
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
     * File has been transfered
     * 
     * @param filename Filename associated to the received content
     */
    public void handleFileTransfered(String filename) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Content transfered");
			}
	
			// Update rich messaging history
			RichMessaging.getInstance().updateFileTransferUrl(session.getSessionID(), filename);
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleFileTransfered(filename);
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
