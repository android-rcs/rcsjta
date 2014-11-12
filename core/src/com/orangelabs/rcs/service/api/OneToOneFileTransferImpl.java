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

import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransfer.ReasonCode;
import com.gsma.services.rcs.ft.IFileTransfer;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.HttpFileTransferSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.HttpTransferState;
import com.orangelabs.rcs.provider.messaging.FileTransferStateAndReasonCode;
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
	 * Lock used for synchronization
	 */
	private final Object lock = new Object();

	/**
	 * The logger
	 */
	private final static Logger logger = Logger.getLogger(OneToOneFileTransferImpl.class.getSimpleName());

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
    public String getMimeType() {
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
		if (session instanceof HttpFileTransferSession) {
			int state = ((HttpFileTransferSession)session).getSessionState();
			if (state == HttpTransferState.ESTABLISHED) {
				if (isSessionPaused()) {
					return FileTransfer.State.PAUSED;
				}

				return FileTransfer.State.STARTED;
			}

		} else {
			// MSRP transfer
			SipDialogPath dialogPath = session.getDialogPath();
			if (dialogPath != null && dialogPath.isSessionEstablished()) {
				return FileTransfer.State.STARTED;
			}
		}

		if (session.isInitiatedByRemote()) {
			if (session.isSessionAccepted()) {
				return FileTransfer.State.ACCEPTING;
			}

			return FileTransfer.State.INVITED;
		}

		return FileTransfer.State.INITIATED;
	}

	/**
	 * Returns the reason code of the state of the file transfer
	 *
	 * @return ReasonCode
	 */
	public int getReasonCode() {
		if (isSessionPaused()) {
			/*
			 * If session is paused and still established it must have been
			 * paused by user
			 */
			return ReasonCode.PAUSED_BY_USER;
		}

		return ReasonCode.UNSPECIFIED;
	}

	/**
	 * Returns the direction of the transfer (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see Direction
	 */
	public int getDirection() {
		if (session.isInitiatedByRemote()) {
			return Direction.INCOMING;
		} else {
			return Direction.OUTGOING;
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

	private int sessionAbortedReasonToReasonCode(int sessionAbortedReason) {
		switch (sessionAbortedReason) {
			case ImsServiceSession.TERMINATION_BY_TIMEOUT:
			case ImsServiceSession.TERMINATION_BY_SYSTEM:
				return ReasonCode.ABORTED_BY_SYSTEM;
			case ImsServiceSession.TERMINATION_BY_USER:
				return ReasonCode.ABORTED_BY_USER;
			default:
				throw new IllegalArgumentException(
						"Unknown reason in OneToOneFileTransferImpl.sessionAbortedReasonToReasonCode; sessionAbortedReason="
								+ sessionAbortedReason + "!");
		}
	}

	private FileTransferStateAndReasonCode toStateAndReasonCode(FileSharingError error) {
		switch (error.getErrorCode()) {
			case FileSharingError.SESSION_INITIATION_DECLINED:
			case FileSharingError.SESSION_INITIATION_CANCELLED:
				return new FileTransferStateAndReasonCode(FileTransfer.State.REJECTED,
						ReasonCode.REJECTED_BY_REMOTE);
			case FileSharingError.MEDIA_SAVING_FAILED:
				return new FileTransferStateAndReasonCode(FileTransfer.State.FAILED,
						ReasonCode.FAILED_SAVING);
			case FileSharingError.MEDIA_SIZE_TOO_BIG:
				return new FileTransferStateAndReasonCode(FileTransfer.State.REJECTED,
						ReasonCode.REJECTED_MAX_SIZE);
			case FileSharingError.MEDIA_TRANSFER_FAILED:
			case FileSharingError.MEDIA_UPLOAD_FAILED:
			case FileSharingError.MEDIA_DOWNLOAD_FAILED:
				return new FileTransferStateAndReasonCode(FileTransfer.State.FAILED,
						ReasonCode.FAILED_DATA_TRANSFER);
			case FileSharingError.NO_CHAT_SESSION:
			case FileSharingError.SESSION_INITIATION_FAILED:
				return new FileTransferStateAndReasonCode(FileTransfer.State.FAILED,
						ReasonCode.FAILED_INITIATION);
			case FileSharingError.NOT_ENOUGH_STORAGE_SPACE:
				return new FileTransferStateAndReasonCode(FileTransfer.State.REJECTED,
						ReasonCode.REJECTED_LOW_SPACE);
			default:
				throw new IllegalArgumentException(
						"Unknown reason in OneToOneFileTransferImpl.toStateAndReasonCode; error="
								+ error + "!");
		}
	}

	private void handleSessionRejected(int reasonCode) {
		if (logger.isActivated()) {
			logger.info("Session rejected; reasonCode=" + reasonCode + ".");
		}
		String fileTransferId = getTransferId();
		synchronized (lock) {
			FileTransferServiceImpl.removeFileTransferSession(fileTransferId);

			MessagingLog.getInstance().updateFileTransferStateAndReasonCode(fileTransferId,
					FileTransfer.State.REJECTED, reasonCode);

			mOneToOneFileTransferBroadcaster.broadcastStateChanged(getRemoteContact(),
					fileTransferId, FileTransfer.State.REJECTED, reasonCode);
		}
	}

	/**
	 * Session is started
	 */
	public void handleSessionStarted() {
		if (logger.isActivated()) {
			logger.info("Session started");
		}
		String fileTransferId = getTransferId();
		synchronized (lock) {
			MessagingLog.getInstance().updateFileTransferStateAndReasonCode(fileTransferId,
					FileTransfer.State.STARTED, ReasonCode.UNSPECIFIED);

			mOneToOneFileTransferBroadcaster.broadcastStateChanged(getRemoteContact(),
					fileTransferId, FileTransfer.State.STARTED, ReasonCode.UNSPECIFIED);
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
		int reasonCode = sessionAbortedReasonToReasonCode(reason);
		String fileTransferId = getTransferId();
		synchronized (lock) {
			FileTransferServiceImpl.removeFileTransferSession(fileTransferId);

			MessagingLog.getInstance().updateFileTransferStateAndReasonCode(fileTransferId,
					FileTransfer.State.ABORTED, reasonCode);

			mOneToOneFileTransferBroadcaster.broadcastStateChanged(getRemoteContact(),
					fileTransferId, FileTransfer.State.ABORTED, reasonCode);
		}
	}

	/**
	 * Session has been terminated by remote
	 */
	public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session terminated by remote");
		}
		String fileTransferId = getTransferId();
		synchronized (lock) {
			FileTransferServiceImpl.removeFileTransferSession(fileTransferId);

			if (!session.isFileTransfered()) {
				MessagingLog.getInstance().updateFileTransferStateAndReasonCode(fileTransferId,
						FileTransfer.State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);

				mOneToOneFileTransferBroadcaster.broadcastStateChanged(getRemoteContact(),
						fileTransferId, FileTransfer.State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
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

		String fileTransferId = getTransferId();
		FileTransferStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
		int state = stateAndReasonCode.getState();
		int reasonCode = stateAndReasonCode.getReasonCode();
		synchronized (lock) {
			FileTransferServiceImpl.removeFileTransferSession(fileTransferId);

			MessagingLog.getInstance().updateFileTransferStateAndReasonCode(fileTransferId, state,
					reasonCode);

			mOneToOneFileTransferBroadcaster.broadcastStateChanged(getRemoteContact(),
					fileTransferId, state, reasonCode);
		}
	}

	/**
	 * File transfer progress
	 * 
	 * @param currentSize Data size transferred 
	 * @param totalSize Total size to be transferred
	 */
	public void handleTransferProgress(long currentSize, long totalSize) {
		String fileTransferId = getTransferId();
		synchronized (lock) {
			MessagingLog.getInstance().updateFileTransferProgress(fileTransferId, currentSize);

			// Notify event listeners
			mOneToOneFileTransferBroadcaster.broadcastProgressUpdate(getRemoteContact(),
					fileTransferId, currentSize, totalSize);
		}
	}

	/**
	 * File transfer not allowed to send
	 */
	@Override
	public void handleTransferNotAllowedToSend() {
		String fileTransferId = getTransferId();
		synchronized (lock) {
			FileTransferServiceImpl.removeFileTransferSession(fileTransferId);

			MessagingLog.getInstance().updateFileTransferStateAndReasonCode(fileTransferId,
					FileTransfer.State.FAILED, ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);

			mOneToOneFileTransferBroadcaster.broadcastStateChanged(getRemoteContact(),
					fileTransferId, FileTransfer.State.FAILED,
					ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
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

		String fileTransferId = getTransferId();
		synchronized (lock) {
			FileTransferServiceImpl.removeFileTransferSession(fileTransferId);

			MessagingLog.getInstance().updateFileTransferred(fileTransferId, content);

			mOneToOneFileTransferBroadcaster.broadcastStateChanged(getRemoteContact(),
					fileTransferId, FileTransfer.State.TRANSFERRED, ReasonCode.UNSPECIFIED);
		}
	}

	/**
	 * File transfer has been paused by user
	 */
	public void handleFileTransferPausedByUser() {
		if (logger.isActivated()) {
			logger.info("Transfer paused by user");
		}
		String fileTransferId = getTransferId();
		synchronized (lock) {
			MessagingLog.getInstance().updateFileTransferStateAndReasonCode(fileTransferId,
					FileTransfer.State.PAUSED, ReasonCode.PAUSED_BY_USER);

			mOneToOneFileTransferBroadcaster.broadcastStateChanged(getRemoteContact(),
					fileTransferId, FileTransfer.State.PAUSED, ReasonCode.PAUSED_BY_USER);
		}
	}

	/**
	 * File transfer has been paused by system
	 */
	public void handleFileTransferPausedBySystem() {
		if (logger.isActivated()) {
			logger.info("Transfer paused by system");
		}
		String fileTransferId = getTransferId();
		synchronized (lock) {
			FileTransferServiceImpl.removeFileTransferSession(fileTransferId);

			MessagingLog.getInstance().updateFileTransferStateAndReasonCode(fileTransferId,
					FileTransfer.State.PAUSED, ReasonCode.PAUSED_BY_SYSTEM);

			mOneToOneFileTransferBroadcaster.broadcastStateChanged(getRemoteContact(),
					fileTransferId, FileTransfer.State.PAUSED, ReasonCode.PAUSED_BY_SYSTEM);
		}
	}

	/**
	 * File transfer has been resumed
	 */
	public void handleFileTransferResumed() {
		if (logger.isActivated()) {
			logger.info("Transfer resumed");
		}
		String fileTransferId = getTransferId();
		synchronized (lock) {
			MessagingLog.getInstance().updateFileTransferStateAndReasonCode(fileTransferId,
					FileTransfer.State.STARTED, ReasonCode.UNSPECIFIED);

			mOneToOneFileTransferBroadcaster.broadcastStateChanged(getRemoteContact(),
					fileTransferId, FileTransfer.State.STARTED, ReasonCode.UNSPECIFIED);
		}
	}

	@Override
	public void handleSessionAccepted() {
		if (logger.isActivated()) {
			logger.info("Accepting transfer");
		}
		String fileTransferId = getTransferId();
		synchronized (lock) {
			MessagingLog.getInstance().updateFileTransferStateAndReasonCode(fileTransferId,
					FileTransfer.State.ACCEPTING, ReasonCode.UNSPECIFIED);

			mOneToOneFileTransferBroadcaster.broadcastStateChanged(getRemoteContact(),
					fileTransferId, FileTransfer.State.ACCEPTING, ReasonCode.UNSPECIFIED);
		}
	}

	@Override
	public void handleSessionRejectedByUser() {
		handleSessionRejected(ReasonCode.REJECTED_BY_USER);
	}

	@Override
	public void handleSessionRejectedByTimeout() {
		handleSessionRejected(ReasonCode.REJECTED_TIME_OUT);
	}

	@Override
	public void handleSessionRejectedByRemote() {
		handleSessionRejected(ReasonCode.REJECTED_BY_REMOTE);
	}

	@Override
	public void handleSessionInvited() {
		if (logger.isActivated()) {
			logger.info("Invited to one-to-one file transfer session");
		}
		String fileTransferId = session.getFileTransferId();
		synchronized (lock) {
			MessagingLog.getInstance().addFileTransfer(getRemoteContact(), fileTransferId,
					Direction.INCOMING, session.getContent(), session.getFileicon(),
					FileTransfer.State.INVITED, ReasonCode.UNSPECIFIED);
		}

		mOneToOneFileTransferBroadcaster.broadcastInvitation(fileTransferId);
	}

	@Override
	public void handleSessionAutoAccepted() {
		if (logger.isActivated()) {
			logger.info("Session auto accepted");
		}
		String fileTransferId = session.getFileTransferId();
		synchronized (lock) {
			MessagingLog.getInstance().addFileTransfer(getRemoteContact(), fileTransferId,
					Direction.INCOMING, session.getContent(), session.getFileicon(),
					FileTransfer.State.ACCEPTING, ReasonCode.UNSPECIFIED);
		}

		mOneToOneFileTransferBroadcaster.broadcastInvitation(fileTransferId);
	}
}
