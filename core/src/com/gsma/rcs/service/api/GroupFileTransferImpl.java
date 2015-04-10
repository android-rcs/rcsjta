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

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferPersistedStorageAccessor;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.DownloadFromAcceptFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.DownloadFromResumeFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpFileTransferSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.ResumeUploadFileSharingSession;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.fthttp.FtHttpResume;
import com.gsma.rcs.provider.fthttp.FtHttpResumeDownload;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.rcs.provider.messaging.FileTransferStateAndReasonCode;
import com.gsma.rcs.provider.messaging.MessagingLog;
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
import android.os.RemoteException;

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

    private final Core mCore;

    private final MessagingLog mMessagingLog;

    private String mChatId;

    private final Object mLock = new Object();

    private final ContactsManager mContactManager;

    private final static Logger sLogger = Logger.getLogger(GroupFileTransferImpl.class
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
     * @param core Core
     * @param messagingLog
     * @param contactManager
     */
    public GroupFileTransferImpl(String transferId, IGroupFileTransferBroadcaster broadcaster,
            InstantMessagingService imService,
            FileTransferPersistedStorageAccessor storageAccessor,
            FileTransferServiceImpl fileTransferService, RcsSettings rcsSettings, Core core,
            MessagingLog messagingLog, ContactsManager contactManager) {
        mFileTransferId = transferId;
        mBroadcaster = broadcaster;
        mImService = imService;
        mPersistentStorage = storageAccessor;
        mFileTransferService = fileTransferService;
        mRcsSettings = rcsSettings;
        mCore = core;
        mMessagingLog = messagingLog;
        mContactManager = contactManager;
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
     * @param core Core
     * @param messagingLog
     * @param contactManager
     */
    public GroupFileTransferImpl(String transferId, String chatId,
            IGroupFileTransferBroadcaster broadcaster, InstantMessagingService imService,
            FileTransferPersistedStorageAccessor storageAccessor,
            FileTransferServiceImpl fileTransferService, RcsSettings rcsSettings, Core core,
            MessagingLog messagingLog, ContactsManager contactManager) {
        this(transferId, broadcaster, imService, storageAccessor, fileTransferService, rcsSettings,
                core, messagingLog, contactManager);
        mChatId = chatId;
    }

    private State getRcsState(FileSharingSession session) {
        HttpFileTransferSession.State state = ((HttpFileTransferSession) session).getSessionState();
        if (HttpFileTransferSession.State.ESTABLISHED == state) {
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
        if (sLogger.isActivated()) {
            sLogger.info("Accept session invitation");
        }
        final FileSharingSession ongoingSession = mImService.getFileSharingSession(mFileTransferId);
        if (ongoingSession != null) {
            if (!ongoingSession.isInitiatedByRemote()) {
                // TODO Temporarily illegal access exception
                throw new IllegalStateException(new StringBuilder(
                        "Cannot accept transfer with fileTransferId '").append(mFileTransferId)
                        .append("': wrong direction").toString());

            }
            if (ongoingSession.isSessionAccepted()) {
                // TODO Temporarily illegal access exception
                throw new IllegalStateException(new StringBuilder(
                        "Cannot accept transfer with fileTransferId '").append(mFileTransferId)
                        .append("': already accepted").toString());

            }
            /* Accept invitation */
            new Thread() {
                public void run() {
                    ongoingSession.acceptSession();
                }
            }.start();
            return;

        }
        /* No active session: restore session from provider */
        FtHttpResume resume = mPersistentStorage.getFileTransferResumeInfo();
        if (resume != null) {
            if (!(resume instanceof FtHttpResumeDownload)) {
                // TODO Temporarily illegal access exception
                throw new IllegalStateException(new StringBuilder(
                        "Cannot accept transfer with fileTransferId '").append(mFileTransferId)
                        .append("': wrong direction").toString());

            }
            FtHttpResumeDownload download = (FtHttpResumeDownload) resume;
            if (download.isAccepted()) {
                // TODO Temporarily illegal access exception
                throw new IllegalStateException(new StringBuilder(
                        "Cannot accept transfer with fileTransferId '").append(mFileTransferId)
                        .append("': already accepted").toString());

            }
            if (download.getFileExpiration() > System.currentTimeMillis()) {
                FileSharingSession session = new DownloadFromAcceptFileSharingSession(mImService,
                        ContentManager.createMmContent(resume.getFile(), resume.getSize(),
                                resume.getFileName()), download, mRcsSettings, mMessagingLog,
                        mContactManager);
                session.addListener(this);
                session.startSession();
                return;

            }
            // TODO Temporarily illegal access exception
            throw new IllegalStateException(new StringBuilder(
                    "Cannot accept transfer with fileTransferId '").append(mFileTransferId)
                    .append("': file has expired").toString());
        }
        /*
         * TODO: Throw correct exception as part of CR037 implementation
         */
        throw new IllegalStateException(
                "Cannot find session with file transfer ID=".concat(mFileTransferId));
    }

    /**
     * Rejects file transfer invitation
     */
    public void rejectInvitation() {
        if (sLogger.isActivated()) {
            sLogger.info("Reject session invitation");
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
        if (sLogger.isActivated()) {
            sLogger.info("Cancel session");
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
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Cannot pause transfer with file transfer Id '")
                        .append(mFileTransferId)
                        .append("' as there is no ongoing session corresponding to the fileTransferId.")
                        .toString());
            }
            return false;
        }
        if (!session.isHttpTransfer()) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Cannot pause transfer with file transfer Id '")
                        .append(mFileTransferId).append("' as it is not a HTTP File transfer.")
                        .toString());
            }
            return false;
        }
        State state = getRcsState(session);
        if (State.STARTED != state) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Cannot pause transfer with file transfer Id '")
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
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Cannot pause transfer with file transfer Id '")
                        .append(mFileTransferId).append("' as it is in state ").append(state)
                        .toString());
            }
            /*
             * TODO: Throw correct exception as part of CR037 implementation
             */
            throw new IllegalStateException("Session not in STARTED state.");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Pause session");
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
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot resume transfer with file transfer Id '")
                            .append(mFileTransferId).append("' as it does not exist in DB.")
                            .toString());
                }
                return false;
            }
        }
        if (ReasonCode.PAUSED_BY_USER != reasonCode) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Cannot resume transfer with file transfer Id '")
                        .append(mFileTransferId).append("' as it is ").append(reasonCode)
                        .toString());
            }
            return false;
        }
        if (!ServerApiUtils.isImsConnected()) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Cannot resume transfer with file transfer Id '")
                        .append(mFileTransferId)
                        .append("' as it there is no IMS connection right now.").toString());
            }
            return false;
        }
        if (session == null) {
            if (!mImService.isFileTransferSessionAvailable()) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot resume transfer with file transfer Id '")
                            .append(mFileTransferId)
                            .append("' as the limit of available file transfer session is reached.")
                            .toString());
                }
                return false;
            }
            if (Direction.OUTGOING == mPersistentStorage.getDirection()) {
                if (mImService.isMaxConcurrentOutgoingFileTransfersReached()) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(new StringBuilder(
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
            if (resume == null) {
                // TODO Temporarily illegal access exception
                throw new IllegalStateException(new StringBuilder(
                        "Unable to resume file with fileTransferId ").append(mFileTransferId)
                        .toString());
            }
            if (Direction.OUTGOING == mPersistentStorage.getDirection()) {
                session = new ResumeUploadFileSharingSession(mImService,
                        ContentManager.createMmContent(resume.getFile(), resume.getSize(),
                                resume.getFileName()), (FtHttpResumeUpload) resume, mRcsSettings,
                        mMessagingLog, mContactManager);
            } else {
                session = new DownloadFromResumeFileSharingSession(mImService,
                        ContentManager.createMmContent(resume.getFile(), resume.getSize(),
                                resume.getFileName()), (FtHttpResumeDownload) resume, mRcsSettings,
                        mMessagingLog, mContactManager);
            }
            session.addListener(this);
            session.startSession();
            return;
        }
        boolean fileSharingSessionPaused = isSessionPaused();
        if (sLogger.isActivated()) {
            sLogger.info("Resuming session paused=" + fileSharingSessionPaused);
        }

        if (!fileSharingSessionPaused) {
            if (sLogger.isActivated()) {
                sLogger.info("Resuming can only be used on a paused HTTP transfer");
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
     * 
     * @param contact
     */
    public void handleSessionStarted(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Session started");
        }
        synchronized (mLock) {
            setStateAndReasonCodeAndBroadcast(State.STARTED, ReasonCode.UNSPECIFIED);
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
        if (sLogger.isActivated()) {
            sLogger.info("Session rejected; reasonCode=" + reasonCode + ".");
        }
        synchronized (mLock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);

            setStateAndReasonCodeAndBroadcast(State.REJECTED, reasonCode);
        }
        mCore.getListener().tryToDequeueFileTransfers(mImService);
    }

    private void setStateAndReasonCodeAndBroadcast(State state, ReasonCode reasonCode) {
        mPersistentStorage.setStateAndReasonCode(state, reasonCode);
        mBroadcaster.broadcastStateChanged(mChatId, mFileTransferId, state, reasonCode);
    }

    /**
     * Session has been aborted
     * 
     * @param contact
     * @param reason Termination reason
     */
    public void handleSessionAborted(ContactId contact, TerminationReason reason) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Session aborted (reason ").append(reason).append(")")
                    .toString());
        }
        synchronized (mLock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);
            switch (reason) {
                case TERMINATION_BY_TIMEOUT:
                case TERMINATION_BY_SYSTEM:
                    setStateAndReasonCodeAndBroadcast(State.ABORTED, ReasonCode.ABORTED_BY_SYSTEM);
                    break;
                case TERMINATION_BY_CONNECTION_LOST:
                    setStateAndReasonCodeAndBroadcast(State.FAILED, ReasonCode.FAILED_DATA_TRANSFER);
                    break;
                case TERMINATION_BY_USER:
                    setStateAndReasonCodeAndBroadcast(State.ABORTED, ReasonCode.ABORTED_BY_USER);
                    break;
                case TERMINATION_BY_REMOTE:
                    /*
                     * TODO : Fix sending of SIP BYE by sender once transfer is completed and media
                     * session is closed. Then this check of state can be removed. Also need to
                     * check if it is storing and broadcasting right state and reasoncode.
                     */
                    if (State.TRANSFERRED != mPersistentStorage.getState()) {
                        setStateAndReasonCodeAndBroadcast(State.ABORTED,
                                ReasonCode.ABORTED_BY_REMOTE);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(
                            new StringBuilder(
                                    "Unknown reason in GroupFileTransferImpl.handleSessionAborted; terminationReason=")
                                    .append(reason).append("!").toString());
            }
        }
        mCore.getListener().tryToDequeueFileTransfers(mImService);
    }

    /**
     * Session has been terminated by remote
     * 
     * @param contact
     */
    public void handleSessionTerminatedByRemote(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Session terminated by remote");
        }
        synchronized (mLock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);
            /*
             * TODO : Fix sending of SIP BYE by sender once transfer is completed and media session
             * is closed. Then this check of state can be removed. Also need to check if it is
             * storing and broadcasting right state and reasoncode.
             */
            if (State.TRANSFERRED != mPersistentStorage.getState()) {
                setStateAndReasonCodeAndBroadcast(State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
            }
        }
        mCore.getListener().tryToDequeueFileTransfers(mImService);
    }

    /**
     * File transfer error
     * 
     * @param error Error
     * @param contact
     */
    public void handleTransferError(FileSharingError error, ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Sharing error " + error.getErrorCode());
        }
        FileTransferStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
        State state = stateAndReasonCode.getState();
        ReasonCode reasonCode = stateAndReasonCode.getReasonCode();
        synchronized (mLock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);
            setStateAndReasonCodeAndBroadcast(state, reasonCode);
        }
        mCore.getListener().tryToDequeueFileTransfers(mImService);
    }

    /**
     * File transfer progress
     * 
     * @param contact
     * @param currentSize Data size transferred
     * @param totalSize Total size to be transferred
     */
    public void handleTransferProgress(ContactId contact, long currentSize, long totalSize) {
        synchronized (mLock) {
            mPersistentStorage.setProgress(currentSize);

            mBroadcaster.broadcastProgressUpdate(mChatId, mFileTransferId, currentSize, totalSize);
        }
    }

    /**
     * File transfer not allowed to send
     */
    @Override
    public void handleTransferNotAllowedToSend(ContactId contact) {
        synchronized (mLock) {
            setStateAndReasonCodeAndBroadcast(State.FAILED, ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
        }
        mCore.getListener().tryToDequeueFileTransfers(mImService);
    }

    /**
     * File has been transfered
     * 
     * @param content MmContent associated to the received file
     * @param contact
     * @param fileExpiration the time when the file on the content server is no longer valid to
     *            download.
     * @param fileIconExpiration the time when the file icon on the content server is no longer
     *            valid to download.
     */
    public void handleFileTransfered(MmContent content, ContactId contact, long fileExpiration,
            long fileIconExpiration) {
        if (sLogger.isActivated()) {
            sLogger.info("Content transferred");
        }
        synchronized (mLock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);
            mPersistentStorage.setTransferred(content, fileExpiration, fileIconExpiration);
            mBroadcaster.broadcastStateChanged(mChatId, mFileTransferId, State.TRANSFERRED,
                    ReasonCode.UNSPECIFIED);
        }
        mCore.getListener().tryToDequeueFileTransfers(mImService);
    }

    /**
     * File transfer has been paused by user
     */
    @Override
    public void handleFileTransferPausedByUser(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Transfer paused by user");
        }
        synchronized (mLock) {
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
        if (sLogger.isActivated()) {
            sLogger.info("Transfer paused by system");
        }
        synchronized (mLock) {
            mFileTransferService.removeFileTransfer(mFileTransferId);
            setStateAndReasonCodeAndBroadcast(State.PAUSED, ReasonCode.PAUSED_BY_SYSTEM);
        }
    }

    /**
     * File transfer has been resumed
     * 
     * @param contact
     */
    public void handleFileTransferResumed(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Transfer resumed");
        }
        synchronized (mLock) {
            setStateAndReasonCodeAndBroadcast(State.STARTED, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleSessionAccepted(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Accepting transfer");
        }
        synchronized (mLock) {
            setStateAndReasonCodeAndBroadcast(State.ACCEPTING, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleSessionRejectedByUser(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_USER, contact);
    }

    @Override
    public void handleSessionRejectedByTimeout(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_TIMEOUT, contact);
    }

    @Override
    public void handleSessionRejectedByRemote(ContactId contact) {
        handleSessionRejected(ReasonCode.REJECTED_BY_REMOTE, contact);
    }

    @Override
    public void handleSessionInvited(ContactId contact, MmContent file, MmContent fileIcon,
            long timestamp, long timestampSent, long fileExpiration, long fileIconExpiration) {
        if (sLogger.isActivated()) {
            sLogger.info("Invited to group file transfer session");
        }
        synchronized (mLock) {
            mPersistentStorage.addIncomingGroupFileTransfer(mChatId, contact, file, fileIcon,
                    State.INVITED, ReasonCode.UNSPECIFIED, timestamp, timestampSent,
                    fileExpiration, fileIconExpiration);
        }

        mBroadcaster.broadcastInvitation(mFileTransferId);
    }

    @Override
    public void handleSessionAutoAccepted(ContactId contact, MmContent file, MmContent fileIcon,
            long timestamp, long timestampSent, long fileExpiration, long fileIconExpiration) {
        if (sLogger.isActivated()) {
            sLogger.info("Session auto accepted");
        }
        synchronized (mLock) {
            mPersistentStorage.addIncomingGroupFileTransfer(mChatId, contact, file, fileIcon,
                    State.ACCEPTING, ReasonCode.UNSPECIFIED, timestamp, timestampSent,
                    fileExpiration, fileIconExpiration);
        }

        mBroadcaster.broadcastInvitation(mFileTransferId);
    }

    @Override
    public long getFileExpiration() throws RemoteException {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getFileExpiration();
        }
        return session.getFileExpiration();
    }

    @Override
    public long getFileIconExpiration() throws RemoteException {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            return mPersistentStorage.getFileIconExpiration();
        }
        return session.getIconExpiration();
    }
}
