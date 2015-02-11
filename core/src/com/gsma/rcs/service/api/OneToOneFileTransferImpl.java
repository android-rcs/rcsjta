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

package com.gsma.rcs.service.api;

import javax2.sip.message.Response;
import android.net.Uri;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferPersistedStorageAccessor;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpFileTransferSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpTransferState;
import com.gsma.rcs.provider.messaging.FileTransferStateAndReasonCode;
import com.gsma.rcs.service.broadcaster.IOneToOneFileTransferBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;
import com.gsma.services.rcs.filetransfer.IFileTransfer;

/**
 * File transfer implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class OneToOneFileTransferImpl extends IFileTransfer.Stub implements
        FileSharingSessionListener {

    private final String mFileTransferId;

    private final IOneToOneFileTransferBroadcaster mBroadcaster;

    private final InstantMessagingService mImService;

    private final FileTransferPersistedStorageAccessor mPersistentStorage;

    private final FileTransferServiceImpl mFileTransferService;

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
     * @param transferId Transfer ID
     * @param broadcaster IOneToOneFileTransferBroadcaster
     * @param imService InstantMessagingService
     * @param persistentStorage FileTransferPersistedStorageAccessor
     * @param fileTransferService FileTransferServiceImpl
     */
    public OneToOneFileTransferImpl(String transferId,
            IOneToOneFileTransferBroadcaster broadcaster, InstantMessagingService imService,
            FileTransferPersistedStorageAccessor persistentStorage,
            FileTransferServiceImpl fileTransferService) {
        mFileTransferId = transferId;
        mBroadcaster = broadcaster;
        mImService = imService;
        mPersistentStorage = persistentStorage;
        mFileTransferService = fileTransferService;
    }

    /**
     * Returns the chat ID of the file transfer
     * 
     * @return Transfer ID
     */
    public String getChatId() {
        // For 1-1 file transfer, chat ID corresponds to the formatted contact number
        return getRemoteContact().toString();
    }

    /**
     * Returns the file transfer ID of the file transfer
     * 
     * @return Transfer ID
     */
    public String getTransferId() {
        return mFileTransferId;
    }

    /**
     * Returns the remote contact identifier
     * 
     * @return ContactId
     */
    public ContactId getRemoteContact() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getRemoteContact();
        }
        return session.getRemoteContact();
    }

    /**
     * Returns the complete filename including the path of the file to be transferred
     * 
     * @return Filename
     */
    public String getFileName() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getFileName();
        }
        return session.getContent().getName();
    }

    /**
     * Returns the Uri of the file to be transferred
     * 
     * @return Filename
     */
    public Uri getFile() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getFile();
        }
        return session.getContent().getUri();
    }

    /**
     * Returns the size of the file to be transferred
     * 
     * @return Size in bytes
     */
    public long getFileSize() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getFileSize();
        }
        return session.getContent().getSize();
    }

    /**
     * Returns the MIME type of the file to be transferred
     * 
     * @return Type
     */
    public String getMimeType() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getMimeType();
        }
        return session.getContent().getEncoding();
    }

    /**
     * Returns the Uri of the file icon
     * 
     * @return Uri
     */
    public Uri getFileIcon() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getFileIcon();
        }
        MmContent fileIcon = session.getContent();
        return fileIcon != null ? fileIcon.getUri() : null;
    }

    /**
     * Returns the Mime type of file icon
     * 
     * @return Mime type
     */
    public String getFileIconMimeType() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getFileIconMimeType();
        }
        MmContent fileIconMimeType = session.getContent();
        return fileIconMimeType != null ? fileIconMimeType.getEncoding() : null;
    }

    /**
     * Returns the state of the file transfer
     * 
     * @return State
     */
    public int getState() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getState();
        }
        if (session instanceof HttpFileTransferSession) {
            int state = ((HttpFileTransferSession) session).getSessionState();
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
        return FileTransfer.State.INITIATING;
    }

    /**
     * Returns the reason code of the state of the file transfer
     * 
     * @return ReasonCode
     */
    public int getReasonCode() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getReasonCode();
        }
        if (isSessionPaused()) {
            /*
             * If session is paused and still established it must have been paused by user
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
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getDirection().toInt();
        }
        if (session.isInitiatedByRemote()) {
            return Direction.INCOMING.toInt();
        }
        return Direction.OUTGOING.toInt();
    }

    public long getTimestamp() {
        return mPersistentStorage.getTimestamp();
    }

    public long getTimestampSent() {
        return mPersistentStorage.getTimestampSent();
    }

    public long getTimestampDelivered() {
        return mPersistentStorage.getTimestampDelivered();
    }

    public long getTimestampDisplayed() {
        return mPersistentStorage.getTimestampDisplayed();
    }

    /**
     * Accepts file transfer invitation
     */
    public void acceptInvitation() {
        if (logger.isActivated()) {
            logger.info("Accept session invitation");
        }
        final FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException("Session with file transfer ID '" + mFileTransferId
                    + "' not available.");
        }

        // Accept invitation
        new Thread() {
            public void run() {
                session.acceptSession();
            }
        }.start();
    }

    /**
     * Rejects file transfer invitation
     */
    public void rejectInvitation() {
        if (logger.isActivated()) {
            logger.info("Reject session invitation");
        }
        final FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException("Session with file transfer ID '" + mFileTransferId
                    + "' not available.");
        }

        // Reject invitation
        new Thread() {
            public void run() {
                session.rejectSession(Response.DECLINE);
            }
        }.start();
    }

    /**
     * Aborts the file transfer
     */
    public void abortTransfer() {
        if (logger.isActivated()) {
            logger.info("Cancel session");
        }
        final FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException("Session with file transfer ID '" + mFileTransferId
                    + "' not available.");
        }
        if (session.isFileTransfered()) {
            // File already transferred and session automatically closed after transfer
            return;
        }
        // Abort the session
        new Thread() {
            public void run() {
                session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
            }
        }.start();
    }

    /**
     * Is HTTP transfer
     * 
     * @return Boolean
     */
    public boolean isHttpTransfer() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException(
                    "Unable to check if it is HTTP transfer since session with file transfer ID '"
                            + mFileTransferId + "' not available.");
        }

        return (session instanceof HttpFileTransferSession);
    }

    /**
     * Returns true if it is possible to pause this file transfer right now, else returns false. If
     * this filetransfer corresponds to a file transfer that is no longer present in the persistent
     * storage false will be returned (this is no error)
     * 
     * @return boolean
     */
    public boolean canPauseTransfer() {
        throw new UnsupportedOperationException("This method has not been implemented yet!");
    }

    /**
     * Pauses the file transfer (only for HTTP transfer)
     */
    public void pauseTransfer() {
        if (logger.isActivated()) {
            logger.info("Pause session");
        }
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException(
                    "Unable to pause transfer since session with file transfer ID '"
                            + mFileTransferId + "' not available.");
        }
        if (!isHttpTransfer()) {
            if (logger.isActivated()) {
                logger.info("Pause available only for HTTP transfer");
            }
            return;
        }

        ((HttpFileTransferSession) session).pauseFileTransfer();
    }

    /**
     * Checks if transfer is paused (only for HTTP transfer)
     * 
     * @return True if transfer is paused
     */
    public boolean isSessionPaused() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException(
                    "Unable to check if transfer is paused since session with file transfer ID '"
                            + mFileTransferId + "' not available.");
        }
        if (!isHttpTransfer()) {
            if (logger.isActivated()) {
                logger.info("Pause available only for HTTP transfer");
            }
            return false;
        }
        return ((HttpFileTransferSession) session).isFileTransferPaused();
    }

    /**
     * Returns true if it is possible to resume this file transfer right now, else return false. If
     * this filetransfer corresponds to a file transfer that is no longer present in the persistent
     * storage false will be returned.
     * 
     * @return boolean
     */
    public boolean canResumeTransfer() {
        throw new UnsupportedOperationException("This method has not been implemented yet!");
    }

    /**
     * Resume the session (only for HTTP transfer)
     */
    public void resumeTransfer() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException(
                    "Unable to resume transfer since session with file transfer ID '"
                            + mFileTransferId + "' not available.");
        }
        boolean fileSharingSessionPaused = isSessionPaused();
        boolean fileTransferOverHttp = isHttpTransfer();
        if (logger.isActivated()) {
            logger.info("Resuming session paused=" + fileSharingSessionPaused + " http="
                    + fileTransferOverHttp);
        }

        if (!(fileTransferOverHttp && fileSharingSessionPaused)) {
            if (logger.isActivated()) {
                logger.info("Resuming can only be used on a paused HTTP transfer");
            }
            return;
        }

        ((HttpFileTransferSession) session).resumeFileTransfer();
    }

    /**
     * Returns whether you can resend the transfer.
     * 
     * @return boolean
     */
    public boolean canResendTransfer() {
        int state = getState();
        int reasonCode = getReasonCode();
        /*
         * According to Blackbird PDD v3.0, "When a File Transfer is interrupted by sender
         * interaction (or fails), then    resend button    shall be offered to allow the user to
         * re-send the file without selecting a new receiver or selecting the file again."
         */
        switch (state) {
            case State.FAILED:
                return true;
            case State.ABORTED:
                if (ReasonCode.ABORTED_BY_SYSTEM == reasonCode
                        || ReasonCode.ABORTED_BY_USER == reasonCode) {
                    return true;
                }
                if (logger.isActivated()) {
                    logger.debug("Cannot resend transfer as it has been ABORTED_BY_REMOTE, fileTransferId "
                            .concat(mFileTransferId));
                }
                return false;
            default:
                if (logger.isActivated()) {
                    logger.debug(new StringBuilder("Cannot resend transfer with fileTransferId ")
                            .append(mFileTransferId).append(" as state=").append(state)
                            .append(" reasonCode=").append(reasonCode).toString());
                }
                return false;
        }
    }

    /**
     * Resend a file transfer which was previously failed. This only for 1-1 file transfer, an
     * exception is thrown in case of a file transfer to group.
     */
    public void resendTransfer() {
        if (!canResendTransfer()) {
            // TODO Temporarily illegal access exception
            throw new IllegalStateException(new StringBuilder(
                    "Unable to resend file with fileTransferId ").append(mFileTransferId)
                    .toString());
        }
        MmContent file = FileTransferUtils.createMmContent(getFile());
        Uri fileIcon = getFileIcon();
        MmContent fileIconContent = fileIcon != null ? FileTransferUtils.createMmContent(fileIcon)
                : null;

        mFileTransferService.resendOneToOneFile(getRemoteContact(), file, fileIconContent,
                mFileTransferId);
    }

    /**
     * Returns true if file transfer has been marked as read
     * 
     * @return boolean
     */
    public boolean isRead() {
        return mPersistentStorage.isRead();
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
        int fileSharingError = error.getErrorCode();
        switch (fileSharingError) {
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
                        new StringBuilder(
                                "Unknown reason in OneToOneFileTransferImpl.toStateAndReasonCode; fileSharingError=")
                                .append(fileSharingError).append("!").toString());
        }
    }

    private void handleSessionRejected(int reasonCode, ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session rejected; reasonCode=" + reasonCode + ".");
        }
        synchronized (lock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);

            mPersistentStorage.setStateAndReasonCode(FileTransfer.State.REJECTED, reasonCode);

            mBroadcaster.broadcastStateChanged(contact, mFileTransferId,
                    FileTransfer.State.REJECTED, reasonCode);
        }
    }

    /**
     * Session is started
     */
    public void handleSessionStarted(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session started");
        }
        synchronized (lock) {
            mPersistentStorage.setStateAndReasonCode(FileTransfer.State.STARTED,
                    ReasonCode.UNSPECIFIED);

            mBroadcaster.broadcastStateChanged(contact, mFileTransferId,
                    FileTransfer.State.STARTED, ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Session has been aborted
     * 
     * @param reason Termination reason
     */
    public void handleSessionAborted(ContactId contact, int reason) {
        if (logger.isActivated()) {
            logger.info("Session aborted (reason " + reason + ")");
        }
        int reasonCode = sessionAbortedReasonToReasonCode(reason);
        synchronized (lock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);

            mPersistentStorage.setStateAndReasonCode(FileTransfer.State.ABORTED, reasonCode);

            mBroadcaster.broadcastStateChanged(contact, mFileTransferId,
                    FileTransfer.State.ABORTED, reasonCode);
        }
    }

    /**
     * Session has been terminated by remote
     */
    public void handleSessionTerminatedByRemote(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session terminated by remote");
        }
        synchronized (lock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);
            /*
             * TODO : Fix sending of SIP BYE by sender once transfer is completed and media session
             * is closed. Then this check of state can be removed.
             */
            if (State.TRANSFERRED != getState()) {
                mPersistentStorage.setStateAndReasonCode(FileTransfer.State.ABORTED,
                        ReasonCode.ABORTED_BY_REMOTE);
                mBroadcaster.broadcastStateChanged(contact, mFileTransferId,
                        FileTransfer.State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
            }
        }
    }

    /**
     * File transfer error
     * 
     * @param error Error
     */
    public void handleTransferError(FileSharingError error, ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Sharing error " + error.getErrorCode());
        }

        FileTransferStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
        int state = stateAndReasonCode.getState();
        int reasonCode = stateAndReasonCode.getReasonCode();
        synchronized (lock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);

            mPersistentStorage.setStateAndReasonCode(state, reasonCode);

            mBroadcaster.broadcastStateChanged(getRemoteContact(), mFileTransferId, state,
                    reasonCode);
        }
    }

    /**
     * File transfer progress
     * 
     * @param currentSize Data size transferred
     * @param totalSize Total size to be transferred
     */
    public void handleTransferProgress(ContactId contact, long currentSize, long totalSize) {
        synchronized (lock) {
            mPersistentStorage.setProgress(currentSize);

            mBroadcaster.broadcastProgressUpdate(contact, mFileTransferId, currentSize, totalSize);
        }
    }

    /**
     * File transfer not allowed to send
     */
    @Override
    public void handleTransferNotAllowedToSend(ContactId contact) {
        synchronized (lock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);

            mPersistentStorage.setStateAndReasonCode(FileTransfer.State.FAILED,
                    ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);

            mBroadcaster.broadcastStateChanged(contact, mFileTransferId, FileTransfer.State.FAILED,
                    ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
        }
    }

    /**
     * File has been transfered
     * 
     * @param content MmContent associated to the received file
     */
    public void handleFileTransfered(MmContent content, ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Content transferred");
        }

        synchronized (lock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);

            mPersistentStorage.setTransferred(content);

            mBroadcaster.broadcastStateChanged(contact, mFileTransferId,
                    FileTransfer.State.TRANSFERRED, ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * File transfer has been paused by user
     */
    public void handleFileTransferPausedByUser(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Transfer paused by user");
        }
        synchronized (lock) {
            mPersistentStorage.setStateAndReasonCode(FileTransfer.State.PAUSED,
                    ReasonCode.PAUSED_BY_USER);

            mBroadcaster.broadcastStateChanged(contact, mFileTransferId, FileTransfer.State.PAUSED,
                    ReasonCode.PAUSED_BY_USER);
        }
    }

    /**
     * File transfer has been paused by system
     */
    public void handleFileTransferPausedBySystem(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Transfer paused by system");
        }
        synchronized (lock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);

            mPersistentStorage.setStateAndReasonCode(FileTransfer.State.PAUSED,
                    ReasonCode.PAUSED_BY_SYSTEM);

            mBroadcaster.broadcastStateChanged(contact, mFileTransferId, FileTransfer.State.PAUSED,
                    ReasonCode.PAUSED_BY_SYSTEM);
        }
    }

    /**
     * File transfer has been resumed
     */
    public void handleFileTransferResumed(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Transfer resumed");
        }
        synchronized (lock) {
            mPersistentStorage.setStateAndReasonCode(FileTransfer.State.STARTED,
                    ReasonCode.UNSPECIFIED);

            mBroadcaster.broadcastStateChanged(contact, mFileTransferId,
                    FileTransfer.State.STARTED, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleSessionAccepted(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Accepting transfer");
        }
        synchronized (lock) {
            mPersistentStorage.setStateAndReasonCode(FileTransfer.State.ACCEPTING,
                    ReasonCode.UNSPECIFIED);

            mBroadcaster.broadcastStateChanged(contact, mFileTransferId,
                    FileTransfer.State.ACCEPTING, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleSessionRejectedByUser(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_USER, contact);
    }

    @Override
    public void handleSessionRejectedByTimeout(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_TIME_OUT, contact);
    }

    @Override
    public void handleSessionRejectedByRemote(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_REMOTE, contact);
    }

    @Override
    public void handleSessionInvited(ContactId contact, MmContent file, MmContent fileIcon) {
        if (logger.isActivated()) {
            logger.info("Invited to one-to-one file transfer session");
        }
        synchronized (lock) {
            mPersistentStorage.addFileTransfer(contact, Direction.INCOMING, file, fileIcon,
                    FileTransfer.State.INVITED, ReasonCode.UNSPECIFIED);
        }

        mBroadcaster.broadcastInvitation(mFileTransferId);
    }

    @Override
    public void handleSessionAutoAccepted(ContactId contact, MmContent file, MmContent fileIcon) {
        if (logger.isActivated()) {
            logger.info("Session auto accepted");
        }
        synchronized (lock) {
            mPersistentStorage.addFileTransfer(contact, Direction.INCOMING, file, fileIcon,
                    FileTransfer.State.ACCEPTING, ReasonCode.UNSPECIFIED);
        }

        mBroadcaster.broadcastInvitation(mFileTransferId);
    }
}
