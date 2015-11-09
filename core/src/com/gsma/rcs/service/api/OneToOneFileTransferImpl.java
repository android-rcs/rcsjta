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

import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.service.ImsServiceSession.InvitationStatus;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.ImsFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.DownloadFromAcceptFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.DownloadFromResumeFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpFileTransferSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.ResumeUploadFileSharingSession;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.fthttp.FtHttpResume;
import com.gsma.rcs.provider.fthttp.FtHttpResumeDownload;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.rcs.provider.messaging.FileTransferPersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.FileTransferStateAndReasonCode;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.service.broadcaster.IOneToOneFileTransferBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;
import com.gsma.services.rcs.filetransfer.IFileTransfer;

import android.net.Uri;
import android.os.RemoteException;

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

    private final RcsSettings mRcsSettings;

    private final MessagingLog mMessagingLog;

    private final Object mLock = new Object();

    private static final Logger sLogger = Logger
            .getLogger(OneToOneFileTransferImpl.class.getName());

    private final ContactManager mContactManager;

    /**
     * Constructor
     * 
     * @param transferId Transfer ID
     * @param broadcaster IOneToOneFileTransferBroadcaster
     * @param imService InstantMessagingService
     * @param persistentStorage FileTransferPersistedStorageAccessor
     * @param fileTransferService FileTransferServiceImpl
     * @param rcsSettings The RCS settings accessor
     * @param messagingLog The messaging log accessor
     * @param contactManager The contact manager accessor
     */
    public OneToOneFileTransferImpl(InstantMessagingService imService, String transferId,
            IOneToOneFileTransferBroadcaster broadcaster,
            FileTransferPersistedStorageAccessor persistentStorage,
            FileTransferServiceImpl fileTransferService, RcsSettings rcsSettings,
            MessagingLog messagingLog, ContactManager contactManager) {
        mImService = imService;
        mFileTransferId = transferId;
        mBroadcaster = broadcaster;
        mPersistentStorage = persistentStorage;
        mFileTransferService = fileTransferService;
        mRcsSettings = rcsSettings;
        mMessagingLog = messagingLog;
        mContactManager = contactManager;
    }

    private State getRcsState(FileSharingSession session) {
        if (session instanceof HttpFileTransferSession) {
            HttpFileTransferSession.State state = ((HttpFileTransferSession) session)
                    .getSessionState();
            if (HttpFileTransferSession.State.ESTABLISHED == state) {
                if (isSessionPaused(session)) {
                    return State.PAUSED;
                }
                return State.STARTED;
            }
        } else if (session instanceof ImsFileSharingSession) {
            if (session.isFileTransferred()) {
                return State.TRANSFERRED;
            }
            SipDialogPath dialogPath = session.getDialogPath();
            if (dialogPath != null && dialogPath.isSessionEstablished()) {
                return State.STARTED;
            }
        } else {
            throw new IllegalArgumentException("Unsupported Filetransfer session type");
        }
        if (session.isInitiatedByRemote()) {
            if (session.isSessionAccepted()) {
                return State.ACCEPTING;
            }
            return State.INVITED;
        }
        return State.INITIATING;
    }

    private ReasonCode getRcsReasonCode(FileSharingSession session) {
        if (isSessionPaused(session)) {
            /*
             * If session is paused and still established it must have been paused by user
             */
            return ReasonCode.PAUSED_BY_USER;
        }
        return ReasonCode.UNSPECIFIED;
    }

    @Override
    public String getChatId() throws RemoteException {
        try {
            /* For 1-1 file transfer, chat ID corresponds to the formatted contact number */
            return getRemoteContact().toString();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public String getTransferId() throws RemoteException {
        return mFileTransferId;
    }

    @Override
    public ContactId getRemoteContact() throws RemoteException {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session == null) {
                return mPersistentStorage.getRemoteContact();
            }
            return session.getRemoteContact();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public String getFileName() throws RemoteException {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session == null) {
                return mPersistentStorage.getFileName();
            }
            return session.getContent().getName();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public Uri getFile() throws RemoteException {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session == null) {
                return mPersistentStorage.getFile();
            }
            return session.getContent().getUri();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public long getFileSize() throws RemoteException {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session == null) {
                return mPersistentStorage.getFileSize();
            }
            return session.getContent().getSize();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public String getMimeType() throws RemoteException {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session == null) {
                return mPersistentStorage.getMimeType();
            }
            return session.getContent().getEncoding();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public Uri getFileIcon() throws RemoteException {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session == null) {
                return mPersistentStorage.getFileIcon();
            }
            MmContent fileIcon = session.getContent();
            return fileIcon != null ? fileIcon.getUri() : null;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public String getFileIconMimeType() throws RemoteException {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session == null) {
                return mPersistentStorage.getFileIconMimeType();
            }
            MmContent fileIconMimeType = session.getContent();
            return fileIconMimeType != null ? fileIconMimeType.getEncoding() : null;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public int getState() throws RemoteException {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session == null) {
                return mPersistentStorage.getState().toInt();
            }
            return getRcsState(session).toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public int getReasonCode() throws RemoteException {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session == null) {
                return mPersistentStorage.getReasonCode().toInt();
            }
            return getRcsReasonCode(session).toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public int getDirection() throws RemoteException {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session == null) {
                return mPersistentStorage.getDirection().toInt();
            }
            if (session.isInitiatedByRemote()) {
                return Direction.INCOMING.toInt();
            }
            return Direction.OUTGOING.toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public long getTimestamp() throws RemoteException {
        try {
            return mPersistentStorage.getTimestamp();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public long getTimestampSent() throws RemoteException {
        try {
            return mPersistentStorage.getTimestampSent();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public long getTimestampDelivered() throws RemoteException {
        try {
            return mPersistentStorage.getTimestampDelivered();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public long getTimestampDisplayed() throws RemoteException {
        try {
            return mPersistentStorage.getTimestampDisplayed();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public void acceptInvitation() throws RemoteException {
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                try {
                    if (sLogger.isActivated()) {
                        sLogger.info("Accept session invitation");
                    }
                    final FileSharingSession ongoingSession = mImService
                            .getFileSharingSession(mFileTransferId);
                    if (ongoingSession != null) {
                        if (!ongoingSession.isInitiatedByRemote()) {
                            sLogger.error(new StringBuilder(
                                    "Cannot accept transfer with fileTransferId '")
                                    .append(mFileTransferId).append("': wrong direction")
                                    .toString());
                            return;
                        }
                        if (ongoingSession.isSessionAccepted()) {
                            sLogger.error(new StringBuilder(
                                    "Cannot accept transfer with fileTransferId '")
                                    .append(mFileTransferId).append("': already accepted")
                                    .toString());
                            return;
                        }
                        ongoingSession.acceptSession();
                        return;
                    }
                    /* No active session: restore session from provider */
                    FtHttpResume resume = mPersistentStorage.getFileTransferResumeInfo();
                    if (resume == null) {
                        sLogger.error("Cannot find session sith file transfer ID="
                                .concat(mFileTransferId));
                        return;
                    }
                    if (!(resume instanceof FtHttpResumeDownload)) {
                        sLogger.error(new StringBuilder(
                                "Cannot accept transfer with fileTransferId '")
                                .append(mFileTransferId).append("': wrong direction").toString());
                        return;
                    }
                    FtHttpResumeDownload download = (FtHttpResumeDownload) resume;
                    if (download.isAccepted()) {
                        sLogger.error(new StringBuilder(
                                "Cannot accept transfer with fileTransferId '")
                                .append(mFileTransferId).append("': already accepted").toString());
                        return;
                    }
                    if (download.getFileExpiration() < System.currentTimeMillis()) {
                        sLogger.error(new StringBuilder(
                                "Cannot accept transfer with fileTransferId '")
                                .append(mFileTransferId).append("': file has expired").toString());
                        return;
                    }
                    FileSharingSession session = new DownloadFromAcceptFileSharingSession(
                            mImService, ContentManager.createMmContent(resume.getFile(),
                                    resume.getSize(), resume.getFileName()), download,
                            mRcsSettings, mMessagingLog, mContactManager);
                    session.addListener(OneToOneFileTransferImpl.this);
                    session.startSession();

                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder(
                                    "Failed to accept file transfer with fileTransferId : ")
                                    .append(mFileTransferId).toString(), e);
                }
            }
        });
    }

    @Override
    public void rejectInvitation() throws RemoteException {
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                try {
                    if (sLogger.isActivated()) {
                        sLogger.info("Reject session invitation");
                    }
                    final FileSharingSession session = mImService
                            .getFileSharingSession(mFileTransferId);
                    if (session == null) {
                        if (Direction.INCOMING != mPersistentStorage.getDirection()) {
                            sLogger.error(new StringBuilder(
                                    "Cannot reject transfer with fileTransferId '")
                                    .append(mFileTransferId).append("': wrong direction")
                                    .toString());
                            return;
                        }
                        State state = mPersistentStorage.getState();
                        switch (state) {
                            case INVITED:
                                handleSessionRejected(ReasonCode.REJECTED_BY_USER,
                                        mPersistentStorage.getRemoteContact());
                                return;

                            case REJECTED:
                                if (sLogger.isActivated()) {
                                    sLogger.info("Transfer already rejected for ID="
                                            .concat(mFileTransferId));
                                }
                                return;

                            default:
                                sLogger.error("Illegal state to reject file transfer with ID="
                                        .concat(mFileTransferId));
                                return;
                        }
                    }
                    session.rejectSession(InvitationStatus.INVITATION_REJECTED_DECLINE);
                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder(
                                    "Failed to reject file transfer with fileTransferId : ")
                                    .append(mFileTransferId).toString(), e);
                }
            }
        });
    }

    @Override
    public void abortTransfer() throws RemoteException {
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                try {
                    if (sLogger.isActivated()) {
                        sLogger.info("Cancel session");
                    }
                    final FileSharingSession session = mImService
                            .getFileSharingSession(mFileTransferId);
                    if (session == null) {
                        /*
                         * File transfer can be aborted only if it is in state QUEUED/ PAUSED when
                         * there is no session.
                         */
                        State state = mPersistentStorage.getState();
                        switch (state) {
                            case QUEUED:
                            case PAUSED:
                                setStateAndReasonCode(getRemoteContact(), State.ABORTED,
                                        ReasonCode.ABORTED_BY_SYSTEM);
                                return;
                            default:
                                sLogger.error(new StringBuilder("Session with file transfer ID '")
                                        .append(mFileTransferId).append("' not available!")
                                        .toString());
                                return;
                        }
                    }
                    if (session.isFileTransferred()) {
                        /* File already transferred and session automatically closed after transfer */
                        sLogger.error("Cannot abort as file is already transferred!");
                        return;
                    }
                    session.terminateSession(TerminationReason.TERMINATION_BY_USER);

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }
                } catch (PayloadException | RemoteException e) {
                    sLogger.error(
                            new StringBuilder("Failed to terminate session with fileTransferId : ")
                                    .append(mFileTransferId).toString(), e);
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder("Failed to terminate session with fileTransferId : ")
                                    .append(mFileTransferId).toString(), e);
                }
            }
        });
    }

    private boolean isHttpTransfer() {
        FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
        if (session == null) {
            throw new ServerApiGenericException(new StringBuilder(
                    "Unable to check if it is HTTP transfer since session with file transfer ID '")
                    .append(mFileTransferId).append("' not available!").toString());
        }
        return session.isHttpTransfer();
    }

    @Override
    public boolean isAllowedToPauseTransfer() throws RemoteException {
        return isAllowedToPauseTransfer(false);
    }

    private boolean isAllowedToPauseTransfer(boolean internalRequest) {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session == null) {
                if (internalRequest && sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder("Cannot pause transfer with file transfer Id '")
                            .append(mFileTransferId)
                            .append("' as there is no ongoing session corresponding to the fileTransferId.")
                            .toString());
                }
                return false;
            }
            if (!session.isHttpTransfer()) {
                if (internalRequest && sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder("Cannot pause transfer with file transfer Id '")
                            .append(mFileTransferId).append("' as it is not a HTTP File transfer.")
                            .toString());
                }
                return false;
            }
            State state = getRcsState(session);
            if (State.STARTED != state) {
                if (internalRequest && sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder("Cannot pause transfer with file transfer Id '")
                            .append(mFileTransferId).append("' as it is in state ").append(state)
                            .toString());
                }
                return false;
            }
            if (mPersistentStorage.getFileTransferProgress() == mPersistentStorage.getFileSize()) {
                if (internalRequest && sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder("Cannot pause transfer with file transfer Id '")
                            .append(mFileTransferId).append("' as full content is transferred")
                            .toString());
                }
                return false;
            }
            return true;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public void pauseTransfer() throws RemoteException {
        if (!isAllowedToPauseTransfer(true)) {
            throw new ServerApiPermissionDeniedException("Not allowed to pause transfer.");
        }
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.debug("Pause session");
                }
                try {
                    HttpFileTransferSession session = (HttpFileTransferSession) mImService
                            .getFileSharingSession(mFileTransferId);
                    if (session == null) {
                        sLogger.error("Failed to pause file transfer with fileTransferId : "
                                + mFileTransferId + "since no such session exists anymore.");
                        return;
                    }
                    session.onPause();

                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder(
                                    "Failed to pause file transfer with fileTransferId : ").append(
                                    mFileTransferId).toString(), e);
                }
            }
        });
    }

    private boolean isSessionPaused(FileSharingSession session) {
        if (session == null) {
            throw new ServerApiGenericException(new StringBuilder(
                    "Unable to check if transfer is paused since session with file transfer ID '")
                    .append(mFileTransferId).append("' not available!").toString());
        }
        if (!isHttpTransfer()) {
            if (sLogger.isActivated()) {
                sLogger.info("Pause available only for HTTP transfer");
            }
            return false;
        }
        return ((HttpFileTransferSession) session).isFileTransferPaused();
    }

    @Override
    public boolean isAllowedToResumeTransfer() throws RemoteException {
        return isAllowedToResumeTransfer(false);
    }

    private boolean isAllowedToResumeTransfer(boolean internalRequest) throws RemoteException {
        try {
            ReasonCode reasonCode;
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session != null) {
                reasonCode = getRcsReasonCode(session);
            } else {
                reasonCode = mPersistentStorage.getReasonCode();
            }
            if (ReasonCode.PAUSED_BY_USER != reasonCode) {
                if (internalRequest && sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot resume transfer with file transfer Id '")
                            .append(mFileTransferId).append("' as it is ").append(reasonCode)
                            .toString());
                }
                return false;
            }
            if (!ServerApiUtils.isImsConnected()) {
                if (internalRequest && sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot resume transfer with file transfer Id '")
                            .append(mFileTransferId)
                            .append("' as it there is no IMS connection right now.").toString());
                }
                return false;
            }
            if (session == null) {
                if (!mImService.isFileTransferSessionAvailable()) {
                    if (internalRequest && sLogger.isActivated()) {
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
                        if (internalRequest && sLogger.isActivated()) {
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

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public void resumeTransfer() throws RemoteException {
        if (!isAllowedToResumeTransfer(true)) {
            throw new ServerApiPermissionDeniedException("Not allowed to resume transfer.");
        }
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.debug("Resume session");
                }
                try {
                    HttpFileTransferSession session = (HttpFileTransferSession) mImService
                            .getFileSharingSession(mFileTransferId);
                    if (session != null) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Resume file transfer from existing session: interrupted "
                                    + session.isInterrupted() + " state=" + session.getState());
                        }

                        session.onResume();
                        return;
                    }
                    FtHttpResume resume = mPersistentStorage.getFileTransferResumeInfo();
                    if (resume == null) {
                        sLogger.error("Unable to resume file with fileTransferId "
                                + mFileTransferId
                                + " since it no longer exists in persistent storage!");
                        return;
                    }
                    if (Direction.OUTGOING == resume.getDirection()) {
                        session = new ResumeUploadFileSharingSession(mImService, ContentManager
                                .createMmContent(resume.getFile(), resume.getSize(),
                                        resume.getFileName()), (FtHttpResumeUpload) resume,
                                mRcsSettings, mMessagingLog, mContactManager);
                    } else {
                        session = new DownloadFromResumeFileSharingSession(mImService,
                                ContentManager.createMmContent(resume.getFile(), resume.getSize(),
                                        resume.getFileName()), (FtHttpResumeDownload) resume,
                                mRcsSettings, mMessagingLog, mContactManager);
                    }
                    session.addListener(OneToOneFileTransferImpl.this);
                    if (sLogger.isActivated()) {
                        sLogger.debug("Resume file transfer from new session");
                    }
                    session.startSession();

                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder(
                                    "Failed to resume file transfer with fileTransferId : ")
                                    .append(mFileTransferId).toString(), e);
                }
            }
        });
    }

    @Override
    public boolean isAllowedToResendTransfer() throws RemoteException {
        return isAllowedToResendTransfer(false);
    }

    private boolean isAllowedToResendTransfer(boolean internalRequest) throws RemoteException {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session != null) {
                if (internalRequest && sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder("Cannot resend transfer with fileTransferId ")
                            .append(mFileTransferId)
                            .append(" as there is already an ongoing session corresponding to this fileTransferId")
                            .toString());
                }
                return false;
            }
            State rcsState = mPersistentStorage.getState();
            ReasonCode rcsReasonCode = mPersistentStorage.getReasonCode();
            /*
             * According to Blackbird PDD v3.0, "When a File Transfer is interrupted by sender
             * interaction (or fails), then resend button shall be offered to allow the user to
             * re-send the file without selecting a new receiver or selecting the file again."
             */
            switch (rcsState) {
                case FAILED:
                    return true;
                case REJECTED:
                    switch (rcsReasonCode) {
                        case REJECTED_BY_SYSTEM:
                            return true;
                        default:
                            if (internalRequest && sLogger.isActivated()) {
                                sLogger.debug(new StringBuilder(
                                        "Cannot resend transfer with fileTransferId ")
                                        .append(mFileTransferId).append(" as reasonCode=")
                                        .append(rcsReasonCode).toString());
                            }
                            return false;
                    }
                case ABORTED:
                    switch (rcsReasonCode) {
                        case ABORTED_BY_SYSTEM:
                        case ABORTED_BY_USER:
                            return true;
                        default:
                            if (internalRequest && sLogger.isActivated()) {
                                sLogger.debug(new StringBuilder(
                                        "Cannot resend transfer with fileTransferId ")
                                        .append(mFileTransferId).append(" as reasonCode=")
                                        .append(rcsReasonCode).toString());
                            }
                            return false;
                    }
                default:
                    if (internalRequest && sLogger.isActivated()) {
                        sLogger.debug(new StringBuilder(
                                "Cannot resend transfer with fileTransferId ")
                                .append(mFileTransferId).append(" as state=").append(rcsState)
                                .toString());
                    }
                    return false;
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public void resendTransfer() throws RemoteException {
        if (!isAllowedToResendTransfer(true)) {
            throw new ServerApiPermissionDeniedException(new StringBuilder(
                    "Unable to resend file with fileTransferId ").append(mFileTransferId)
                    .toString());
        }
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                try {
                    MmContent file = FileTransferUtils.createMmContent(getFile());
                    Uri fileIcon = getFileIcon();
                    MmContent fileIconContent = fileIcon != null ? FileTransferUtils
                            .createMmContent(fileIcon) : null;

                    mFileTransferService.resendOneToOneFile(getRemoteContact(), file,
                            fileIconContent, mFileTransferId);
                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder(
                                    "Failed to resume file transfer with fileTransferId : ")
                                    .append(mFileTransferId).toString(), e);
                } catch (RemoteException e) {
                    sLogger.error(
                            new StringBuilder(
                                    "Failed to resume file transfer with fileTransferId : ")
                                    .append(mFileTransferId).toString(), e);
                }
            }
        });
    }

    @Override
    public boolean isRead() throws RemoteException {
        try {
            return mPersistentStorage.isRead();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /*------------------------------- SESSION EVENTS ----------------------------------*/
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
                /* Intentional fall through */
            case FileSharingError.SESSION_INITIATION_FAILED:
                /* Intentional fall through */
            case FileSharingError.SEND_RESPONSE_FAILED:
                return new FileTransferStateAndReasonCode(State.FAILED,
                        ReasonCode.FAILED_INITIATION);
            case FileSharingError.NOT_ENOUGH_STORAGE_SPACE:
                return new FileTransferStateAndReasonCode(State.REJECTED,
                        ReasonCode.REJECTED_LOW_SPACE);
            default:
                throw new IllegalArgumentException(
                        new StringBuilder(
                                "Unknown reason in OneToOneFileTransferImpl.toStateAndReasonCode; fileSharingError=")
                                .append(fileSharingError).append("!").toString());
        }
    }

    private void setStateAndReasonCode(ContactId contact, State state, ReasonCode reasonCode) {
        if (mPersistentStorage.setStateAndReasonCode(state, reasonCode)) {
            mBroadcaster.broadcastStateChanged(contact, mFileTransferId, state, reasonCode);
        }
    }

    private void handleSessionRejected(ReasonCode reasonCode, ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Session rejected; reasonCode=" + reasonCode + ".");
        }
        synchronized (mLock) {
            mFileTransferService.removeOneToOneFileTransfer(mFileTransferId);
            setStateAndReasonCode(contact, State.REJECTED, reasonCode);
        }
        mImService.tryToDequeueFileTransfers();
    }

    @Override
    public void onSessionStarted(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Session started");
        }
        synchronized (mLock) {
            setStateAndReasonCode(contact, State.STARTED, ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Handle file info dequeued
     */
    public void onFileInfoDequeued(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("One-One file info with transferId ")
                    .append(mFileTransferId).append(" dequeued successfully.").toString());
        }
        synchronized (mLock) {
            mFileTransferService.removeOneToOneFileTransfer(mFileTransferId);
            long deliveryExpiration = 0;
            if (!mRcsSettings.isFtHttpCapAlwaysOn()) {
                long timeout = mRcsSettings.getMsgDeliveryTimeoutPeriod();
                if (timeout > 0) {
                    deliveryExpiration = System.currentTimeMillis() + timeout;
                    mImService.getDeliveryExpirationManager()
                            .scheduleOneToOneFileTransferDeliveryTimeoutAlarm(contact,
                                    mFileTransferId, deliveryExpiration);
                }
            }
            if (mPersistentStorage.setFileInfoDequeued(deliveryExpiration)) {
                mBroadcaster.broadcastStateChanged(contact, mFileTransferId, State.TRANSFERRED,
                        ReasonCode.UNSPECIFIED);
            }
        }
        mImService.tryToDequeueFileTransfers();
    }

    @Override
    public void onSessionAborted(ContactId contact, TerminationReason reason) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Session aborted (reason ").append(reason).append(")")
                    .toString());
        }
        synchronized (mLock) {
            mFileTransferService.removeOneToOneFileTransfer(mFileTransferId);
            switch (reason) {
                case TERMINATION_BY_TIMEOUT:
                case TERMINATION_BY_SYSTEM:
                    setStateAndReasonCode(contact, State.ABORTED, ReasonCode.ABORTED_BY_SYSTEM);
                    break;
                case TERMINATION_BY_CONNECTION_LOST:
                    setStateAndReasonCode(contact, State.FAILED, ReasonCode.FAILED_DATA_TRANSFER);
                    break;
                case TERMINATION_BY_USER:
                    setStateAndReasonCode(contact, State.ABORTED, ReasonCode.ABORTED_BY_USER);
                    break;
                case TERMINATION_BY_REMOTE:
                    /*
                     * TODO : Fix sending of SIP BYE by sender once transfer is completed and media
                     * session is closed. Then this check of state can be removed.
                     */
                    if (State.TRANSFERRED != mPersistentStorage.getState()) {
                        setStateAndReasonCode(contact, State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(
                            new StringBuilder(
                                    "Unknown reason in OneToOneFileTransferImpl.handleSessionAborted; terminationReason=")
                                    .append(reason).append("!").toString());
            }
        }
        mImService.tryToDequeueFileTransfers();
    }

    /**
     * Session has been terminated by remote
     * 
     * @param contact The remote contact
     */
    public void onSessionTerminatedByRemote(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Session terminated by remote");
        }
        synchronized (mLock) {
            mFileTransferService.removeOneToOneFileTransfer(mFileTransferId);
            /*
             * TODO : Fix sending of SIP BYE by sender once transfer is completed and media session
             * is closed. Then this check of state can be removed.
             */
            if (State.TRANSFERRED != mPersistentStorage.getState()) {
                setStateAndReasonCode(contact, State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
            }
        }
        mImService.tryToDequeueFileTransfers();
    }

    @Override
    public void onTransferError(FileSharingError error, ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Sharing error " + error.getErrorCode());
        }

        FileTransferStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
        State state = stateAndReasonCode.getState();
        ReasonCode reasonCode = stateAndReasonCode.getReasonCode();
        synchronized (mLock) {
            mFileTransferService.removeOneToOneFileTransfer(mFileTransferId);
            setStateAndReasonCode(contact, state, reasonCode);
        }
        mImService.tryToDequeueFileTransfers();
    }

    @Override
    public void onTransferProgress(ContactId contact, long currentSize, long totalSize) {
        synchronized (mLock) {
            if (mPersistentStorage.setProgress(currentSize)) {
                mBroadcaster.broadcastProgressUpdate(contact, mFileTransferId, currentSize,
                        totalSize);
            }
        }
    }

    @Override
    public void onTransferNotAllowedToSend(ContactId contact) {
        synchronized (mLock) {
            mFileTransferService.removeOneToOneFileTransfer(mFileTransferId);
            setStateAndReasonCode(contact, State.FAILED, ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
        }
        mImService.tryToDequeueFileTransfers();
    }

    @Override
    public void onFileTransferred(MmContent content, ContactId contact, long fileExpiration,
            long fileIconExpiration, FileTransferProtocol ftProtocol) {
        if (sLogger.isActivated()) {
            sLogger.info("Content transferred");
        }

        synchronized (mLock) {
            mFileTransferService.removeOneToOneFileTransfer(mFileTransferId);
            long deliveryExpiration = 0;
            if (FileTransferProtocol.HTTP == ftProtocol && !mRcsSettings.isFtHttpCapAlwaysOn()
                    && Direction.OUTGOING == mPersistentStorage.getDirection()) {
                long timeout = mRcsSettings.getMsgDeliveryTimeoutPeriod();
                if (timeout > 0) {
                    deliveryExpiration = System.currentTimeMillis() + timeout;
                    mImService.getDeliveryExpirationManager()
                            .scheduleOneToOneFileTransferDeliveryTimeoutAlarm(contact,
                                    mFileTransferId, deliveryExpiration);
                }
            }
            if (mPersistentStorage.setTransferred(content, fileExpiration, fileIconExpiration,
                    deliveryExpiration)) {
                mBroadcaster.broadcastStateChanged(contact, mFileTransferId, State.TRANSFERRED,
                        ReasonCode.UNSPECIFIED);
            }
        }
        mImService.tryToDequeueFileTransfers();
    }

    @Override
    public void onFileTransferPausedByUser(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Transfer paused by user");
        }
        synchronized (mLock) {
            setStateAndReasonCode(contact, State.PAUSED, ReasonCode.PAUSED_BY_USER);
        }
    }

    @Override
    public void onFileTransferPausedBySystem(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Transfer paused by system");
        }
        synchronized (mLock) {
            mFileTransferService.removeOneToOneFileTransfer(mFileTransferId);
            setStateAndReasonCode(contact, State.PAUSED, ReasonCode.PAUSED_BY_SYSTEM);
        }
    }

    @Override
    public void onFileTransferResumed(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Transfer resumed");
        }
        synchronized (mLock) {
            setStateAndReasonCode(contact, State.STARTED, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void onSessionAccepting(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Accepting transfer");
        }
        synchronized (mLock) {
            setStateAndReasonCode(contact, State.ACCEPTING, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void onSessionRejected(ContactId contact, TerminationReason reason) {
        switch (reason) {
            case TERMINATION_BY_USER:
                handleSessionRejected(ReasonCode.REJECTED_BY_USER, contact);
                break;
            case TERMINATION_BY_SYSTEM:
                /* Intentional fall through */
            case TERMINATION_BY_CONNECTION_LOST:
                handleSessionRejected(ReasonCode.REJECTED_BY_SYSTEM, contact);
                break;
            case TERMINATION_BY_TIMEOUT:
                handleSessionRejected(ReasonCode.REJECTED_BY_TIMEOUT, contact);
                break;
            case TERMINATION_BY_REMOTE:
                handleSessionRejected(ReasonCode.REJECTED_BY_REMOTE, contact);
                break;
            default:
                throw new IllegalArgumentException(new StringBuilder(
                        "Unknown reason RejectedReason=").append(reason).append("!").toString());
        }
    }

    @Override
    public void onSessionInvited(ContactId contact, MmContent file, MmContent fileIcon,
            long timestamp, long timestampSent, long fileExpiration, long fileIconExpiration) {
        if (sLogger.isActivated()) {
            sLogger.info("Invited to one-to-one file transfer session");
        }
        synchronized (mLock) {
            if (!mPersistentStorage.setStateAndTimestamp(State.INVITED, ReasonCode.UNSPECIFIED,
                    timestamp, timestampSent)) {
                mPersistentStorage.addOneToOneFileTransfer(contact, Direction.INCOMING, file,
                        fileIcon, State.INVITED, ReasonCode.UNSPECIFIED, timestamp, timestampSent,
                        fileExpiration, fileIconExpiration);
            }
        }

        mBroadcaster.broadcastInvitation(mFileTransferId);
    }

    @Override
    public void onSessionAutoAccepted(ContactId contact, MmContent file, MmContent fileIcon,
            long timestamp, long timestampSent, long fileExpiration, long fileIconExpiration) {
        if (sLogger.isActivated()) {
            sLogger.info("Session auto accepted");
        }
        synchronized (mLock) {
            if (!mPersistentStorage.setStateAndTimestamp(State.ACCEPTING, ReasonCode.UNSPECIFIED,
                    timestamp, timestampSent)) {
                mPersistentStorage.addOneToOneFileTransfer(contact, Direction.INCOMING, file,
                        fileIcon, State.ACCEPTING, ReasonCode.UNSPECIFIED, timestamp,
                        timestampSent, fileExpiration, fileIconExpiration);
            }
        }

        mBroadcaster.broadcastInvitation(mFileTransferId);
    }

    @Override
    public long getFileExpiration() throws RemoteException {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session == null) {
                return mPersistentStorage.getFileExpiration();
            }
            return session.getFileExpiration();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public long getFileIconExpiration() throws RemoteException {
        try {
            FileSharingSession session = mImService.getFileSharingSession(mFileTransferId);
            if (session == null) {
                return mPersistentStorage.getFileIconExpiration();
            }
            return session.getIconExpiration();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public boolean isExpiredDelivery() throws RemoteException {
        try {
            return mPersistentStorage.isExpiredDelivery();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public void onHttpDownloadInfoAvailable() {
        mImService.tryToDequeueFileTransfers();
    }
}
