/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.io.IOException;

/**
 * Originating file transfer HTTP session
 * 
 * @author vfml3370
 */
public class OriginatingHttpGroupFileSharingSession extends HttpFileTransferSession implements
        HttpUploadTransferEventListener {

    protected HttpUploadManager mUploadManager;

    private static final Logger sLogger = Logger
            .getLogger(OriginatingHttpGroupFileSharingSession.class.getName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param fileTransferId File transfer Id
     * @param content The file content to share
     * @param fileIcon Content of file icon
     * @param conferenceId Conference ID
     * @param chatContributionId Chat contribution Id
     * @param tId TID of the upload
     * @param rcsSettings The RCS settings accessor
     * @param messagingLog The messaging log accessor
     * @param timestamp Local timestamp for the session
     * @param contactManager The contact manager accessor
     */
    public OriginatingHttpGroupFileSharingSession(InstantMessagingService imService,
            String fileTransferId, MmContent content, MmContent fileIcon, Uri conferenceId,
            String chatContributionId, String tId, RcsSettings rcsSettings,
            MessagingLog messagingLog, long timestamp, ContactManager contactManager) {
        // @formatter:off
        super(imService, 
                content, 
                null, 
                conferenceId, 
                fileIcon, 
                chatContributionId,
                fileTransferId, 
                rcsSettings, 
                messagingLog, 
                timestamp,
                FileTransferData.UNKNOWN_EXPIRATION, 
                FileTransferData.UNKNOWN_EXPIRATION,
                contactManager);
        // @formatter:on
        mUploadManager = new HttpUploadManager(getContent(), fileIcon, this, tId, rcsSettings);
    }

    @Override
    public void run() {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a new HTTP group file transfer session as originating");
        }
        try {
            processHttpUploadResponse(mUploadManager.uploadFile());

        } catch (NetworkException e) {
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (IOException e) {
            /* Don't call handleError in case of Pause or Cancel */
            if (mUploadManager.isCancelled() || mUploadManager.isPaused()) {
                return;
            }
            sLogger.error("Failed to initiate file transfer session for sessionId : "
                    + getSessionID() + " with fileTransferId : " + getFileTransferId(), e);
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (PayloadException | RuntimeException e) {
            sLogger.error("Failed to initiate file transfer session for sessionId : "
                    + getSessionID() + " with fileTransferId : " + getFileTransferId(), e);
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        mUploadManager.interrupt();
    }

    /**
     * Process the HTTP upload response
     * 
     * @param response byte[] which contains the result of the 200 OK from the content server
     * @throws PayloadException
     * @throws NetworkException
     */
    protected void processHttpUploadResponse(byte[] response) throws PayloadException,
            NetworkException {
        if (mUploadManager.isCancelled()) {
            return;
        }
        FileTransferHttpInfoDocument infoDocument;
        if (response == null
                || (infoDocument = FileTransferUtils.parseFileTransferHttpDocument(response,
                        mRcsSettings)) == null) {
            handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED));
            return;
        }
        mMessagingLog.setFileTransferDownloadInfo(getFileTransferId(), infoDocument);
        removeSession();
        handleHttpDownloadInfoAvailable();
    }

    @Override
    public void onPause() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    setFileTransferPaused();
                    interruptSession();
                    mUploadManager.pauseTransferByUser();

                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error("Failed to pause upload for sessionId : " + getSessionID()
                            + " with fileTransferId : " + getFileTransferId(), e);
                    handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED, e));
                }
            }
        }).start();
    }

    @Override
    public void onResume() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    setFileTransferResumed();
                    FtHttpResumeUpload upload = mMessagingLog
                            .retrieveFtHttpResumeUpload(mUploadManager.getTId());
                    if (upload != null) {
                        processHttpUploadResponse(mUploadManager.resumeUpload());
                    } else {
                        processHttpUploadResponse(null);
                    }
                } catch (NetworkException e) {
                    handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED, e));

                } catch (IOException | PayloadException | RuntimeException e) {
                    sLogger.error("Failed to resume upload for sessionId : " + getSessionID()
                            + " with fileTransferId : " + getFileTransferId(), e);
                    handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED, e));
                }
            }
        }).start();
    }

    @Override
    public void uploadStarted() {
        mMessagingLog.setFileUploadTId(getFileTransferId(), mUploadManager.getTId());
    }

    @Override
    public boolean isInitiatedByRemote() {
        return false;
    }

    /**
     * Sets the timestamp when file on the content server is no longer valid to download.
     * 
     * @param timestamp The timestamp
     */
    public void setFileExpiration(long timestamp) {
        mFileExpiration = timestamp;
    }

    @Override
    public void terminateSession(TerminationReason reason) throws PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("terminateSession reason=".concat(reason.toString()));
        }
        closeHttpSession(reason);
        /*
         * If reason is TERMINATION_BY_SYSTEM or TERMINATION_BY_CONNECTION_LOST and session already
         * started, then it's a pause
         */
        ContactId contact = getRemoteContact();
        State state = getSessionState();
        switch (reason) {
            case TERMINATION_BY_SYSTEM:
                /* Intentional fall through */
            case TERMINATION_BY_CONNECTION_LOST:
                if (isFileTransferPaused()) {
                    return;
                }
                /*
                 * TId id needed for resuming the file transfer. Hence pausing the file transfer
                 * only if TId is present.
                 */
                if (State.ESTABLISHED == state && mUploadManager.getTId() != null) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Pause the session (session terminated, but can be resumed)");
                    }
                    for (ImsSessionListener listener : getListeners()) {
                        ((FileSharingSessionListener) listener)
                                .onFileTransferPausedBySystem(contact);
                    }
                    return;
                }
                /* Intentional fall through */
                //$FALL-THROUGH$
            default:
                if (State.ESTABLISHED == state) {
                    for (ImsSessionListener listener : getListeners()) {
                        listener.onSessionAborted(contact, reason);
                    }
                } else {
                    for (ImsSessionListener listener : getListeners()) {
                        listener.onSessionRejected(contact, reason);
                    }
                }
                break;
        }
    }
}
