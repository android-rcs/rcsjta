/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferPersistedStorageAccessor;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpFileTransferSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpTransferState;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.ResumeDownloadFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.ResumeUploadFileSharingSession;
import com.gsma.rcs.provider.fthttp.FtHttpResume;
import com.gsma.rcs.provider.fthttp.FtHttpResumeDownload;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.rcs.provider.messaging.FileTransferStateAndReasonCode;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.broadcaster.IGroupFileTransferBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;
import com.gsma.services.rcs.filetransfer.IFileTransfer;

import android.database.SQLException;
import android.net.Uri;

import javax2.sip.message.Response;

/**
 * File transfer implementation
 */
public class GroupFileTransferImpl extends IFileTransfer.Stub implements FileSharingSessionListener {

    private final String mFileTransferId;

    private final IGroupFileTransferBroadcaster mBroadcaster;

    private final InstantMessagingService mImService;

    private final FileTransferPersistedStorageAccessor mPersistentStorage;

    private final FileTransferServiceImpl mFileTransferService;

    private final RcsSettings mRcsSettings;

    private String mChatId;

    /**
     * Lock used for synchronization
     */
    private final Object lock = new Object();

    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(GroupFileTransferImpl.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param transferId Transfer ID
     * @param broadcaster IGroupFileTransferBroadcaster
     * @param imService InstantMessagingService
     * @param storageAccessor FileTransferPersistedStorageAccessor
     * @param fileTransferService FileTransferServiceImpl
     * @param rcsSettings RcsSettings
     */
    public GroupFileTransferImpl(String transferId, IGroupFileTransferBroadcaster broadcaster,
            InstantMessagingService imService,
            FileTransferPersistedStorageAccessor storageAccessor,
            FileTransferServiceImpl fileTransferService, RcsSettings rcsSettings) {
        mFileTransferId = transferId;
        mBroadcaster = broadcaster;
        mImService = imService;
        mPersistentStorage = storageAccessor;
        mFileTransferService = fileTransferService;
        mRcsSettings = rcsSettings;
    }

    /**
     * Constructor
     * 
     * @param transferId Transfer ID
     * @param chatId Chat Id
     * @param broadcaster IGroupFileTransferBroadcaster
     * @param imService InstantMessagingService
     * @param storageAccessor FileTransferPersistedStorageAccessor
     * @param fileTransferService FileTransferServiceImpl
     * @param rcsSettings RcsSettings
     */
    public GroupFileTransferImpl(String transferId, String chatId,
            IGroupFileTransferBroadcaster broadcaster, InstantMessagingService imService,
            FileTransferPersistedStorageAccessor storageAccessor,
            FileTransferServiceImpl fileTransferService, RcsSettings rcsSettings) {
        this(transferId, broadcaster, imService, storageAccessor, fileTransferService, rcsSettings);
        mChatId = chatId;
    }

    private State getRcsState(FileSharingSession session) {
        int state = ((HttpFileTransferSession) session).getSessionState();
        if (HttpTransferState.ESTABLISHED == state) {
            if (isSessionPaused()) {
                return State.PAUSED;
            }
            return State.STARTED;
        } else if (session.isInitiatedByRemote()) {
            if (session.isSessionAccepted()) {
                return State.ACCEPTING;
            }
            return State.INVITED;
        }
        return State.INITIATING;
    }

    private ReasonCode getRcsReasonCode(FileSharingSession session) {
        if (isSessionPaused()) {
            /*
             * If session is paused and still established it must have been paused by user
             */
            return ReasonCode.PAUSED_BY_USER;
        }
        return ReasonCode.UNSPECIFIED;
    }

    /**
     * Returns the chat ID of the group chat
     * 
     * @return Chat ID
     */
    public String getChatId() {
        if (mChatId != null) {
            return mChatId;
        }
        return mPersistentStorage.getChatId();
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
     * Returns the remote contact
     * 
     * @return Contact
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
     * @return Uri
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
     * Returns the state of the file transfer
     * 
     * @return State
     */
    public int getState() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getState().toInt();
        }
        return getRcsState(session).toInt();
    }

    /**
     * Returns the reason code of the state of the file transfer
     * 
     * @return ReasonCode
     */
    public int getReasonCode() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getReasonCode().toInt();
        }
        return getRcsReasonCode(session).toInt();
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
            // File already transferred and session automatically closed after
            // transfer
            return;
        }
        // Abort the session
        new Thread() {
            public void run() {
                session.abortSession(TerminationReason.TERMINATION_BY_USER);
            }
        }.start();
    }

    /**
     * Is HTTP transfer
     * 
     * @return Boolean
     */
    public boolean isHttpTransfer() {
        /* Group file transfer is always a HTTP file transfer */
        return true;
    }

    /**
     * Returns true if it is possible to pause this file transfer right now, else returns false. If
     * this filetransfer corresponds to a file transfer that is no longer present in the persistent
     * storage false will be returned (this is no error)
     * 
     * @return boolean
     */
    public boolean isAllowedToPauseTransfer() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder("Cannot pause transfer with file transfer Id '")
                        .append(mFileTransferId)
                        .append("' as there is no ongoing session corresponding to the fileTransferId.")
                        .toString());
            }
            return false;
        }
        if (!session.isHttpTransfer()) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder("Cannot pause transfer with file transfer Id '")
                        .append(mFileTransferId).append("' as it is not a HTTP File transfer.")
                        .toString());
            }
            return false;
        }
        State state = getRcsState(session);
        if (State.STARTED != state) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder("Cannot pause transfer with file transfer Id '")
                        .append(mFileTransferId).append("' as it is in state ").append(state)
                        .toString());
            }
            return false;
        }
        return true;
    }

    /**
     * Pauses the file transfer (only for HTTP transfer)
     */
    public void pauseTransfer() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException(
                    "Unable to pause transfer since session with file transfer ID '"
                            + mFileTransferId + "' not available.");
        }
        State state = getRcsState(session);
        if (State.STARTED != state) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder("Cannot pause transfer with file transfer Id '")
                        .append(mFileTransferId).append("' as it is in state ").append(state)
                        .toString());
            }
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException("Session not in STARTED state.");
        }
        if (logger.isActivated()) {
            logger.info("Pause session");
        }
        ((HttpFileTransferSession) session).pauseFileTransfer();
    }

    /**
     * Is session paused (only for HTTP transfer)
     */
    private boolean isSessionPaused() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException(
                    "Unable to check if transfer is paused since session with file transfer ID '"
                            + mFileTransferId + "' not available.");
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
    public boolean isAllowedToResumeTransfer() {
        ReasonCode reasonCode;
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session != null) {
            reasonCode = getRcsReasonCode(session);
        } else {
            try {
                reasonCode = mPersistentStorage.getReasonCode();
            } catch (SQLException e) {
                if (logger.isActivated()) {
                    logger.debug(new StringBuilder("Cannot resume transfer with file transfer Id '")
                            .append(mFileTransferId).append("' as it does not exist in DB.")
                            .toString());
                }
                return false;
            }
        }
        if (ReasonCode.PAUSED_BY_USER != reasonCode) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder("Cannot resume transfer with file transfer Id '")
                        .append(mFileTransferId).append("' as it is ").append(reasonCode)
                        .toString());
            }
            return false;
        }
        if (!ServerApiUtils.isImsConnected()) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder("Cannot resume transfer with file transfer Id '")
                        .append(mFileTransferId)
                        .append("' as it there is no IMS connection right now.").toString());
            }
            return false;
        }
        if (session == null) {
            if (!mImService.isFileTransferSessionAvailable()) {
                if (logger.isActivated()) {
                    logger.debug(new StringBuilder("Cannot resume transfer with file transfer Id '")
                            .append(mFileTransferId)
                            .append("' as the limit of available file transfer session is reached.")
                            .toString());
                }
                return false;
            }
            if (Direction.OUTGOING == mPersistentStorage.getDirection()) {
                if (mImService.isMaxConcurrentOutgoingFileTransfersReached()) {
                    if (logger.isActivated()) {
                        logger.debug(new StringBuilder(
                                "Cannot resume transfer with file transfer Id '")
                                .append(mFileTransferId)
                                .append("' as the limit of maximum concurrent outgoing file transfer is reached.")
                                .toString());
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Resume the session (only for HTTP transfer)
     */
    public void resumeTransfer() {
        if (!isAllowedToResumeTransfer()) {
            throw new IllegalStateException("Not allowed to resume transfer.");
        }
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            FtHttpResume resume = mPersistentStorage.getFileTransferResumeInfo();
            if (Direction.OUTGOING == mPersistentStorage.getDirection()) {
                session = new ResumeUploadFileSharingSession(mImService,
                        FileTransferUtils.createMmContent(resume.getFile()),
                        (FtHttpResumeUpload) resume, mRcsSettings);
            } else {
                session = new ResumeDownloadFileSharingSession(mImService,
                        FileTransferUtils.createMmContent(resume.getFile()),
                        (FtHttpResumeDownload) resume, mRcsSettings);
            }
            session.addListener(this);
            session.startSession();
            return;
        }
        boolean fileSharingSessionPaused = isSessionPaused();
        if (logger.isActivated()) {
            logger.info("Resuming session paused=" + fileSharingSessionPaused);
        }

        if (!fileSharingSessionPaused) {
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
    public boolean isAllowedToResendTransfer() {
        /* Resend file transfer is supported only for one-to-one transfers */
        return false;
    }

    /**
     * Returns true if file transfer has been marked as read
     * 
     * @return boolean
     */
    public boolean isRead() {
        return mPersistentStorage.isRead();
    }

    /**
     * Resend a file transfer which was previously failed. This only for 1-1 file transfer, an
     * exception is thrown in case of a file transfer to group.
     */
    public void resendTransfer() {
        /*
         * TODO: Throw correct exception as part of CR037 implementation
         */
        throw new IllegalStateException(
                "Resend operation not supported for group file transfer with file transfer ID "
                        .concat(mFileTransferId));
    }

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    /**
     * Session is started
     */
    public void handleSessionStarted(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session started");
        }
        synchronized (lock) {
            mPersistentStorage.setStateAndReasonCode(State.STARTED, ReasonCode.UNSPECIFIED);

            mBroadcaster.broadcastStateChanged(mChatId, mFileTransferId, State.STARTED,
                    ReasonCode.UNSPECIFIED);
        }
    }

    /*
     * TODO : Fix reasoncode mapping in the switch.
     */
    private FileTransferStateAndReasonCode toStateAndReasonCode(FileSharingError error) {
        int fileSharingError = error.getErrorCode();
        switch (fileSharingError) {
            case FileSharingError.SESSION_INITIATION_DECLINED:
            case FileSharingError.SESSION_INITIATION_CANCELLED:
                return new FileTransferStateAndReasonCode(State.REJECTED,
                        ReasonCode.REJECTED_BY_REMOTE);
            case FileSharingError.MEDIA_SAVING_FAILED:
                return new FileTransferStateAndReasonCode(State.FAILED, ReasonCode.FAILED_SAVING);
            case FileSharingError.MEDIA_SIZE_TOO_BIG:
                return new FileTransferStateAndReasonCode(State.REJECTED,
                        ReasonCode.REJECTED_MAX_SIZE);
            case FileSharingError.MEDIA_TRANSFER_FAILED:
            case FileSharingError.MEDIA_UPLOAD_FAILED:
            case FileSharingError.MEDIA_DOWNLOAD_FAILED:
                return new FileTransferStateAndReasonCode(State.FAILED,
                        ReasonCode.FAILED_DATA_TRANSFER);
            case FileSharingError.NO_CHAT_SESSION:
            case FileSharingError.SESSION_INITIATION_FAILED:
                return new FileTransferStateAndReasonCode(State.FAILED,
                        ReasonCode.FAILED_INITIATION);
            case FileSharingError.NOT_ENOUGH_STORAGE_SPACE:
                return new FileTransferStateAndReasonCode(State.REJECTED,
                        ReasonCode.REJECTED_LOW_SPACE);
            default:
                throw new IllegalArgumentException(
                        new StringBuilder(
                                "Unknown reason in GroupFileTransferImpl.toStateAndReasonCode; fileSharingError=")
                                .append(fileSharingError).append("!").toString());
        }
    }

    private void handleSessionRejected(ReasonCode reasonCode, ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session rejected; reasonCode=" + reasonCode + ".");
        }
        synchronized (lock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);

            mPersistentStorage.setStateAndReasonCode(State.REJECTED, reasonCode);

            mBroadcaster
                    .broadcastStateChanged(mChatId, mFileTransferId, State.REJECTED, reasonCode);
        }
    }

    /**
     * Session has been aborted
     * 
     * @param reason Termination reason
     */
    public void handleSessionAborted(ContactId contact, TerminationReason reason) {
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Session aborted (reason ").append(reason).append(")")
                    .toString());
        }
        /*
         * TODO : Fix reasoncode mapping in the switch.
         */
        ReasonCode reasonCode;
        switch (reason) {
            case TERMINATION_BY_TIMEOUT:
            case TERMINATION_BY_SYSTEM:
                reasonCode = ReasonCode.ABORTED_BY_SYSTEM;
                break;
            case TERMINATION_BY_USER:
                reasonCode = ReasonCode.ABORTED_BY_USER;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown reason in GroupFileTransferImpl.handleSessionAborted; terminationReason="
                                + reason + "!");
        }
        synchronized (lock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);

            mPersistentStorage.setStateAndReasonCode(State.ABORTED, reasonCode);

            mBroadcaster.broadcastStateChanged(mChatId, mFileTransferId, State.ABORTED, reasonCode);
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
             * is closed. Then this check of state can be removed. Also need to check if it is
             * storing and broadcasting right state and reasoncode.
             */
            if (State.TRANSFERRED != mPersistentStorage.getState()) {
                mPersistentStorage.setStateAndReasonCode(State.ABORTED,
                        ReasonCode.ABORTED_BY_REMOTE);
                mBroadcaster.broadcastStateChanged(mChatId, mFileTransferId, State.ABORTED,
                        ReasonCode.ABORTED_BY_REMOTE);
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
        State state = stateAndReasonCode.getState();
        ReasonCode reasonCode = stateAndReasonCode.getReasonCode();
        synchronized (lock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);

            mPersistentStorage.setStateAndReasonCode(state, reasonCode);

            mBroadcaster.broadcastStateChanged(mChatId, mFileTransferId, state, reasonCode);
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

            mBroadcaster.broadcastProgressUpdate(mChatId, mFileTransferId, currentSize, totalSize);
        }
    }

    /**
     * File transfer not allowed to send
     */
    @Override
    public void handleTransferNotAllowedToSend(ContactId contact) {
        synchronized (lock) {
            mPersistentStorage.setStateAndReasonCode(State.FAILED,
                    ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);

            mBroadcaster.broadcastStateChanged(mChatId, mFileTransferId, State.FAILED,
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

            mBroadcaster.broadcastStateChanged(mChatId, mFileTransferId, State.TRANSFERRED,
                    ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * File transfer has been paused by user
     */
    @Override
    public void handleFileTransferPausedByUser(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Transfer paused by user");
        }
        synchronized (lock) {
            mPersistentStorage.setStateAndReasonCode(State.PAUSED, ReasonCode.PAUSED_BY_USER);

            mBroadcaster.broadcastStateChanged(mChatId, mFileTransferId, State.PAUSED,
                    ReasonCode.PAUSED_BY_USER);
        }
    }

    /**
     * File transfer has been paused by system
     */
    @Override
    public void handleFileTransferPausedBySystem(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Transfer paused by system");
        }
        synchronized (lock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);

            mPersistentStorage.setStateAndReasonCode(State.PAUSED, ReasonCode.PAUSED_BY_SYSTEM);

            mBroadcaster.broadcastStateChanged(mChatId, mFileTransferId, State.PAUSED,
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
            mPersistentStorage.setStateAndReasonCode(State.STARTED, ReasonCode.UNSPECIFIED);

            mBroadcaster.broadcastStateChanged(mChatId, mFileTransferId, State.STARTED,
                    ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleSessionAccepted(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Accepting transfer");
        }
        synchronized (lock) {
            mPersistentStorage.setStateAndReasonCode(State.ACCEPTING, ReasonCode.UNSPECIFIED);

            mBroadcaster.broadcastStateChanged(mChatId, mFileTransferId, State.ACCEPTING,
                    ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleSessionRejectedByUser(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_USER, contact);
    }

    /*
     * TODO: Fix reason code mapping between rejected_by_timeout and rejected_by_inactivity.
     */
    @Override
    public void handleSessionRejectedByTimeout(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_INACTIVITY, contact);
    }

    @Override
    public void handleSessionRejectedByRemote(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_REMOTE, contact);
    }

    @Override
    public void handleSessionInvited(ContactId contact, MmContent file, MmContent fileIcon) {
        if (logger.isActivated()) {
            logger.info("Invited to group file transfer session");
        }
        synchronized (lock) {
            mPersistentStorage.addIncomingGroupFileTransfer(mChatId, contact, file, fileIcon,
                    State.INVITED, ReasonCode.UNSPECIFIED);
        }

        mBroadcaster.broadcastInvitation(mFileTransferId);
    }

    @Override
    public void handleSessionAutoAccepted(ContactId contact, MmContent file, MmContent fileIcon) {
        if (logger.isActivated()) {
            logger.info("Session auto accepted");
        }
        synchronized (lock) {
            mPersistentStorage.addIncomingGroupFileTransfer(mChatId, contact, file, fileIcon,
                    State.ACCEPTING, ReasonCode.UNSPECIFIED);
        }

        mBroadcaster.broadcastInvitation(mFileTransferId);
    }
}
