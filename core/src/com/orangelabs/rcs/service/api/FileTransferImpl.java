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

import org.gsma.joyn.ft.FileTransfer;
import org.gsma.joyn.ft.IFileTransfer;
import org.gsma.joyn.ft.IFileTransferListener;

import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.OriginatingFileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.HttpFileTransferSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.HttpTransferState;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.OriginatingHttpFileSharingSession;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File transfer implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferImpl extends IFileTransfer.Stub implements FileSharingSessionListener {
	
	/**
	 * Core session
	 */
	private FileSharingSession session;
	
	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IFileTransferListener> listeners = new RemoteCallbackList<IFileTransferListener>();

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
	public FileTransferImpl(FileSharingSession session) {
		this.session = session;
		
		session.addListener(this);
	}

	/**
	 * Returns the file transfer ID of the file transfer
	 * 
	 * @return Transfer ID
	 */
	public String getTransferId() {
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
     * Returns the complete filename including the path of the file to be transferred
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
     * Returns the complete filename including the path of the file icon
     *
     * @return Filename
     */
	public String getFileIconName() {
		// TODO
		return null;
	}

	/**
	 * Returns the state of the file transfer
	 * 
	 * @return State 
	 */
	public int getState() {
		int result = FileTransfer.State.UNKNOWN;
		if (session instanceof HttpFileTransferSession) {
			// HTTP transfer
			int state = ((HttpFileTransferSession)session).getSessionState(); 
			if (state == HttpTransferState.CANCELLED) {
				// Session canceled
				result = FileTransfer.State.ABORTED;
			} else
			if (state == HttpTransferState.ESTABLISHED) {
				// Session started
				result = FileTransfer.State.STARTED;
			} else
			if (state == HttpTransferState.TERMINATED) {
				// Session terminated
				if (session.isFileTransfered()) {
					result = FileTransfer.State.TRANSFERRED;
				} else {
					result = FileTransfer.State.ABORTED;
				}
			} else
			if (state == HttpTransferState.PENDING) {
				// Session pending
				if (session instanceof OriginatingHttpFileSharingSession) {
					result = FileTransfer.State.INITIATED;
				} else {
					result = FileTransfer.State.INVITED;
				}
			}
		} else {
			// MSRP transfer
			SipDialogPath dialogPath = session.getDialogPath();
			if (dialogPath != null) {
				if (dialogPath.isSessionCancelled()) {
					// Session canceled
					result = FileTransfer.State.ABORTED;
				} else
				if (dialogPath.isSessionEstablished()) {
					// Session started
					result = FileTransfer.State.STARTED;
				} else
				if (dialogPath.isSessionTerminated()) {
					// Session terminated
					if (session.isFileTransfered()) {
						result = FileTransfer.State.TRANSFERRED;
					} else {
						result = FileTransfer.State.ABORTED;
					}
				} else {
					// Session pending
					if (session instanceof OriginatingFileSharingSession) {
						result = FileTransfer.State.INITIATED;
					} else {
						result = FileTransfer.State.INVITED;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns the direction of the transfer (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see FileTransfer.Direction
	 */
	public int getDirection() {
		if (session instanceof OriginatingFileSharingSession) {
			return FileTransfer.Direction.OUTGOING;
		} else {
			return FileTransfer.Direction.INCOMING;
		}
	}	
		
	/**
	 * Accepts file transfer invitation
	 */
	public void acceptInvitation() {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}
		
		// Accept invitation
		session.acceptSession();
	}
	
	/**
	 * Rejects file transfer invitation
	 */
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}
		
		// Update rich messaging history
  		RichMessagingHistory.getInstance().updateFileTransferStatus(session.getSessionID(), FileTransfer.State.ABORTED);

  		// Reject invitation
		session.rejectSession(603);
	}

	/**
	 * Aborts the file transfer
	 */
	public void abortTransfer() {
		if (logger.isActivated()) {
			logger.info("Cancel session");
		}
		
		if (session.isFileTransfered()) {
			// File already transferred and session automatically closed after transfer
			return;
		}

		// Abort the session
		session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
	}

	/**
	 * Adds a listener on file transfer events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener(IFileTransferListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an event listener");
		}

    	synchronized(lock) {
    		listeners.register(listener);
    	}
	}
	
	/**
	 * Removes a listener from file transfer events
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener(IFileTransferListener listener) {
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
	
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateFileTransferStatus(session.getSessionID(), FileTransfer.State.STARTED);

			// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onTransferStarted();
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
			RichMessagingHistory.getInstance().updateFileTransferStatus(session.getSessionID(), FileTransfer.State.ABORTED);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onTransferAborted();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        FileTransferServiceImpl.removeFileTransferSession(session.getSessionID());
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
	
			// Check if the file has been transferred or not
	  		if (session.isFileTransfered()) {
		        // Remove session from the list
	  			FileTransferServiceImpl.removeFileTransferSession(session.getSessionID());
	  		} else {
				// Update rich messaging history
		  		RichMessagingHistory.getInstance().updateFileTransferStatus(session.getSessionID(), FileTransfer.State.ABORTED);
		
		  		// Notify event listeners
				final int N = listeners.beginBroadcast();
		        for (int i=0; i < N; i++) {
		            try {
		            	listeners.getBroadcastItem(i).onTransferAborted();
		            } catch(Exception e) {
		            	if (logger.isActivated()) {
		            		logger.error("Can't notify listener", e);
		            	}
		            }
		        }
		        listeners.finishBroadcast();

		        // Remove session from the list
		        FileTransferServiceImpl.removeFileTransferSession(session.getSessionID());
	  		}
	    }
    }
    
    /**
     * File transfer error
     * 
     * @param error Error
     */
    public void handleTransferError(FileSharingError error) {
    	synchronized(lock) {
			if (error.getErrorCode() == FileSharingError.SESSION_INITIATION_CANCELLED) {
				// Do nothing here, this is an aborted event
				return;
			}

			if (logger.isActivated()) {
				logger.info("Sharing error " + error.getErrorCode());
			}

			// Update rich messaging history
	  		RichMessagingHistory.getInstance().updateFileTransferStatus(session.getSessionID(), FileTransfer.State.FAILED);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	int code;
	            	switch(error.getErrorCode()) {
            			case FileSharingError.SESSION_INITIATION_DECLINED:
	            			code = FileTransfer.Error.INVITATION_DECLINED;
	            			break;
	            		case FileSharingError.MEDIA_SAVING_FAILED:
	            			code = FileTransfer.Error.SAVING_FAILED;
	            			break;
	            		case FileSharingError.MEDIA_SIZE_TOO_BIG:
	            		case FileSharingError.MEDIA_TRANSFER_FAILED:
	            			code = FileTransfer.Error.TRANSFER_FAILED;
	            			break;
	            		default:
	            			code = FileTransfer.Error.TRANSFER_FAILED;
	            	}
	            	listeners.getBroadcastItem(i).onTransferError(code);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        FileTransferServiceImpl.removeFileTransferSession(session.getSessionID());
	    }
    }
    
    /**
	 * File transfer progress
	 * 
	 * @param currentSize Data size transferred 
	 * @param totalSize Total size to be transferred
	 */
    public void handleTransferProgress(long currentSize, long totalSize) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.debug("Sharing progress");
			}
			
			// Update rich messaging history
	  		RichMessagingHistory.getInstance().updateFileTransferProgress(session.getSessionID(), currentSize, totalSize);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onTransferProgress(currentSize, totalSize);
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
				logger.info("Content transferred");
			}
	
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateFileTransferUrl(session.getSessionID(), filename);
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onFileTransferred(filename);
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
     * File transfer has been paused
     */
    public void handleFileUploadPaused() {
    	// TODO
    }
}
