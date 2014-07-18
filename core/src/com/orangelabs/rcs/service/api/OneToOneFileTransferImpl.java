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

import static com.gsma.services.rcs.ft.FileTransfer.State.FAILED;
import static com.gsma.services.rcs.ft.FileTransfer.State.ABORTED;
import static com.gsma.services.rcs.ft.FileTransfer.State.STARTED;
import static com.gsma.services.rcs.ft.FileTransfer.State.TRANSFERRED;
import static com.gsma.services.rcs.ft.FileTransfer.State.PAUSED;

import android.net.Uri;
import android.os.RemoteCallbackList;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.IFileTransfer;
import com.gsma.services.rcs.ft.IFileTransferListener;
import com.gsma.services.rcs.ft.IGroupFileTransferListener;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.OriginatingFileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.HttpFileTransferSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.HttpTransferState;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.OriginatingHttpFileSharingSession;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.service.broadcaster.IOneToOneFileTransferBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File transfer implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class OneToOneFileTransferImpl extends IFileTransfer.Stub implements FileSharingSessionListener {
	
	/**
	 * Core session
	 */
	private FileSharingSession session;

	private final IOneToOneFileTransferBroadcaster mOneToOneFileTransferBroadcaster;

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
	 * @param broadcaster IOneToOneFileTransferBroadcaster
	 */
	public OneToOneFileTransferImpl(FileSharingSession session,
			IOneToOneFileTransferBroadcaster broadcaster) {
		this.session = session;
		mOneToOneFileTransferBroadcaster = broadcaster;
		session.addListener(this);
	}

	/**
	 * Returns the chat ID of the file transfer
	 *
	 * @return Transfer ID
	 */
	public String getChatId(){
		// For 1-1 file transfer, chat ID corresponds to the formatted contact number
		return getRemoteContact().toString();
	}

	/**
	 * Returns the file transfer ID of the file transfer
	 * 
	 * @return Transfer ID
	 */
	public String getTransferId() {
		return session.getFileTransferId();
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
	 * Returns the Uri of the file icon
	 *
	 * @return Uri
	 */
	public Uri getFileIcon() {
		MmContent fileIcon = session.getFileicon();
		return fileIcon != null ? fileIcon.getUri() : null;
	}

	/**
	 * Returns the state of the file transfer
	 * 
	 * @return State 
	 */
	public int getState() {
		int result = FileTransfer.State.INACTIVE;
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
        Thread t = new Thread() {
    		public void run() {
    			session.acceptSession();
    		}
    	};
    	t.start();
	}
	
	/**
	 * Rejects file transfer invitation
	 */
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}
		
		// Update rich messaging history
  		MessagingLog.getInstance().updateFileTransferStatus(session.getFileTransferId(), FileTransfer.State.ABORTED);

  		// Reject invitation
        Thread t = new Thread() {
    		public void run() {
    			session.rejectSession(603);
    		}
    	};
    	t.start();
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
        Thread t = new Thread() {
    		public void run() {
    			session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
    		}
    	};
    	t.start();
	}

    /**
     * Is HTTP transfer
     *
     * @return Boolean
     */
    public boolean isHttpTransfer() {
        return (session instanceof HttpFileTransferSession);
    }
    
	/**
	 * Pauses the file transfer (only for HTTP transfer)
	 */
	public void pauseTransfer() {
		if (logger.isActivated()) {
			logger.info("Pause session");
		}

		if (isHttpTransfer()) {
			((HttpFileTransferSession) session).pauseFileTransfer();
		} else {
			if (logger.isActivated()) {
				logger.info("Pause available only for HTTP transfer");
			}
		}
	}

	/**
	 * Pause the session (only for HTTP transfer)
	 */
	public boolean isSessionPaused() {
		if (isHttpTransfer()) {
			return ((HttpFileTransferSession) session).isFileTransferPaused();
		} else {
			if (logger.isActivated()) {
				logger.info("Pause available only for HTTP transfer");
			}
			return false;
		}
	}

	/**
	 * Resume the session (only for HTTP transfer)
	 */
	public void resumeTransfer() {
		if (logger.isActivated()) {
			logger.info("Resuming session paused=" + isSessionPaused() + " http=" + isHttpTransfer());
		}

		if (isHttpTransfer() && isSessionPaused()) {
			((HttpFileTransferSession) session).resumeFileTransfer();
		} else {
			if (logger.isActivated()) {
				logger.info("Resuming can only be used on a paused HTTP transfer");
			}
		}
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
			String transferId = session.getFileTransferId();
			// Update rich messaging history
			MessagingLog.getInstance().updateFileTransferStatus(transferId, STARTED);

			// Notify event listeners
			mOneToOneFileTransferBroadcaster.broadcastTransferStateChanged(getRemoteContact(),
					transferId, STARTED);
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
			// Update rich messaging history
			String transferId = getTransferId();
			MessagingLog.getInstance().updateFileTransferStatus(transferId, ABORTED);

			// Notify event listeners
			mOneToOneFileTransferBroadcaster.broadcastTransferStateChanged(getRemoteContact(), transferId,
					ABORTED);
	        
	        // Remove session from the list
	        FileTransferServiceImpl.removeFileTransferSession(transferId);
	    }
    }
    
    /**
     * Session has been terminated by remote
     */
	public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session terminated by remote");
		}
		synchronized (lock) {
			// Check if the file has been transferred or not
			String transferId = getTransferId();
			if (session.isFileTransfered()) {
				// Remove session from the list
				FileTransferServiceImpl.removeFileTransferSession(transferId);
			} else {
				// Update rich messaging history
				MessagingLog.getInstance().updateFileTransferStatus(transferId, ABORTED);

				// Notify event listeners
				mOneToOneFileTransferBroadcaster.broadcastTransferStateChanged(getRemoteContact(),
						transferId, ABORTED);

				// Remove session from the list
				FileTransferServiceImpl.removeFileTransferSession(transferId);
			}
		}
	}
    
    /**
     * File transfer error
     * 
     * @param error Error
     */
    public void handleTransferError(FileSharingError error) {
		if (logger.isActivated()) {
			logger.info("Sharing error " + error.getErrorCode());
		}
    	synchronized(lock) {
			if (error.getErrorCode() == FileSharingError.SESSION_INITIATION_CANCELLED) {
				// Do nothing here, this is an aborted event
				return;
			}
			// Update rich messaging history
			String transferId = session.getFileTransferId();
			MessagingLog.getInstance().updateFileTransferStatus(transferId, FAILED);

			// Notify event listeners
			switch (error.getErrorCode()) {
				case FileSharingError.SESSION_INITIATION_DECLINED:
					// TODO : Handle reason code in CR009
					mOneToOneFileTransferBroadcaster.broadcastTransferStateChanged(getRemoteContact(), transferId, FAILED /*, FileTransfer.Error.INVITATION_DECLINED*/);
					break;
				case FileSharingError.MEDIA_SAVING_FAILED:
					// TODO : Handle reason code in CR009
					mOneToOneFileTransferBroadcaster.broadcastTransferStateChanged(getRemoteContact(), transferId, FAILED /*, FileTransfer.Error.SAVING_FAILED*/);
					break;
				case FileSharingError.MEDIA_SIZE_TOO_BIG:
				case FileSharingError.MEDIA_TRANSFER_FAILED:
					// TODO : Handle reason code in CR009
					mOneToOneFileTransferBroadcaster.broadcastTransferStateChanged(getRemoteContact(), transferId, FAILED /*, FileTransfer.Error.TRANSFER_FAILED*/);
					break;
				default:
					// TODO : Handle reason code in CR009
					mOneToOneFileTransferBroadcaster.broadcastTransferStateChanged(getRemoteContact(), transferId, FAILED /*, FileTransfer.Error.TRANSFER_FAILED*/);
			}

	        // Remove session from the list
	        FileTransferServiceImpl.removeFileTransferSession(transferId);
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
			// Update rich messaging history
    		String transferId = getTransferId();
	  		MessagingLog.getInstance().updateFileTransferProgress(transferId, currentSize, totalSize);

			// Notify event listeners
			mOneToOneFileTransferBroadcaster.broadcastTransferprogress(getRemoteContact(),
					transferId, currentSize, totalSize);
		}
	}

    /**
     * File has been transfered
     *
     * @param content MmContent associated to the received file
     */
    public void handleFileTransfered(MmContent content) {
		if (logger.isActivated()) {
			logger.info("Content transferred");
		}
    	synchronized(lock) {
			// Update rich messaging history
			String transferId = getTransferId();
			MessagingLog.getInstance().updateFileTransferred(transferId, content);

			// Notify event listeners
			mOneToOneFileTransferBroadcaster.broadcastTransferStateChanged(getRemoteContact(), transferId,
					TRANSFERRED);
	    }	
    }
    
    /**
     * File transfer has been paused
     */
	public void handleFileTransferPaused() {
		if (logger.isActivated()) {
			logger.info("Transfer paused");
		}
		synchronized (lock) {
			// Update rich messaging history
			String transferId = getTransferId();
			MessagingLog.getInstance().updateFileTransferStatus(transferId, PAUSED);

			// Notify event listeners
			mOneToOneFileTransferBroadcaster.broadcastTransferStateChanged(getRemoteContact(), transferId,
					PAUSED);
		}
	}

	/**
	 * File transfer has been resumed
	 */
	public void handleFileTransferResumed() {
		if (logger.isActivated()) {
			logger.info("Transfer resumed");
		}
		synchronized (lock) {
			// Update rich messaging history
			String transferId = getTransferId();
			MessagingLog.getInstance().updateFileTransferStatus(transferId, STARTED);

			// Notify event listeners
			mOneToOneFileTransferBroadcaster.broadcastTransferStateChanged(getRemoteContact(), transferId,
					STARTED);
		}
	}

}
