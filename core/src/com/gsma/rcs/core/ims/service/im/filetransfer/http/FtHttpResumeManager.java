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

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.fthttp.FtHttpResume;
import com.gsma.rcs.provider.fthttp.FtHttpResumeDownload;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * File Transfer HTTP resume manager
 */
public class FtHttpResumeManager implements Runnable {
    private InstantMessagingService mImsService;

    /**
     * List of pending sessions to resume
     */
    private LinkedList<FtHttpResume> mListOfFtHttpResume;

    private final Logger mLogger = Logger.getLogger(getClass().getSimpleName());

    private final RcsSettings mRcsSettings;

    private final MessagingLog mMessagingLog;

    private final ContactManager mContactManager;

    /**
     * Constructor
     *
     * @param instantMessagingService IMS service
     * @param rcsSettings the RCS setting accessor
     * @param messagingLog the messaging log accessor
     * @param contactManager the contact manager accessor
     */
    public FtHttpResumeManager(InstantMessagingService instantMessagingService,
            RcsSettings rcsSettings, MessagingLog messagingLog, ContactManager contactManager) {
        mRcsSettings = rcsSettings;
        mImsService = instantMessagingService;
        mMessagingLog = messagingLog;
        mContactManager = contactManager;
    }

    @Override
    public void run() {
        try {
            /* Retrieve all resumable sessions */
            List<FtHttpResume> transfersToResume = mMessagingLog
                    .retrieveFileTransfersPausedBySystem();
            if (!transfersToResume.isEmpty()) {
                mListOfFtHttpResume = new LinkedList<>(transfersToResume);
                processNext();
            }

        } catch (ServerApiPersistentStorageException e) {
            /*
             * No state change in case we havn't able to find any resumable file transfer sessions ,
             * as may be they are not any such ones. In case there has been a error while trying to
             * fetch such sessions then we will retry later to fetch them.
             */
            mLogger.error("Error retrieving resumable sessions!", e);

        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            mLogger.error("Error retrieving resumable sessions!", e);
        }
    }

    /**
     * resume next pending session
     */
    private void processNext() {
        if (mListOfFtHttpResume.isEmpty())
            return;
        // Remove the oldest session from the list
        /*
         * FT HTTP session being resumed
         */
        FtHttpResume ftHttpResume = mListOfFtHttpResume.poll();
        if (mLogger.isActivated()) {
            mLogger.debug("Resume FT HTTP ".concat(ftHttpResume.toString()));
        }
        switch (ftHttpResume.getDirection()) {
            case INCOMING:
                FtHttpResumeDownload downloadInfo = (FtHttpResumeDownload) ftHttpResume;
                MmContent downloadContent = ContentManager.createMmContent(ftHttpResume.getFile(),
                        downloadInfo.getSize(), downloadInfo.getFileName());
                final DownloadFromResumeFileSharingSession resumeDownload = new DownloadFromResumeFileSharingSession(
                        mImsService, downloadContent, downloadInfo, mRcsSettings, mMessagingLog,
                        mContactManager);
                resumeDownload.addListener(getFileSharingSessionListener());
                mImsService.resumeIncomingFileTransfer(resumeDownload,
                        resumeDownload.isGroupFileTransfer());
                resumeDownload.startSession();
                break;

            case OUTGOING:
                FtHttpResumeUpload uploadInfo = (FtHttpResumeUpload) ftHttpResume;
                MmContent uploadContent = ContentManager.createMmContent(uploadInfo.getFile(),
                        uploadInfo.getSize(), uploadInfo.getFileName());
                if (!ftHttpResume.isGroupTransfer()) {
                    final ResumeUploadFileSharingSession resumeUpload = new ResumeUploadFileSharingSession(
                            mImsService, uploadContent, uploadInfo, mRcsSettings, mMessagingLog,
                            mContactManager);
                    resumeUpload.addListener(getFileSharingSessionListener());
                    mImsService.resumeOutgoingFileTransfer(resumeUpload, false);
                    resumeUpload.startSession();
                } else {
                    final ResumeUploadGroupFileSharingSession resumeUpload = new ResumeUploadGroupFileSharingSession(
                            mImsService, uploadContent, uploadInfo, mRcsSettings, mMessagingLog,
                            mContactManager);
                    resumeUpload.addListener(getFileSharingSessionListener());
                    mImsService.resumeOutgoingFileTransfer(resumeUpload, true);
                    resumeUpload.startSession();
                }
                break;

            default:
                break;
        }

    }

    /**
     * Create an event listener to handle end of session
     * 
     * @return the File sharing event listener
     */
    private FileSharingSessionListener getFileSharingSessionListener() {
        return new FileSharingSessionListener() {
            AtomicBoolean fired = new AtomicBoolean(false);

            @Override
            public void onSessionStarted(ContactId contact) {
            }

            @Override
            public void onSessionAborted(ContactId contact, TerminationReason reason) {
                if (fired.compareAndSet(false, true)) {
                    processNext();
                }
            }

            @Override
            public void onTransferProgress(ContactId contact, long currentSize, long totalSize) {
            }

            @Override
            public void onTransferNotAllowedToSend(ContactId contact) {
            }

            @Override
            public void onTransferError(FileSharingError error, ContactId contact) {
                if (fired.compareAndSet(false, true)) {
                    processNext();
                }
            }

            @Override
            public void onFileTransferred(MmContent content, ContactId contact, long fileExpiration,
                    long fileIconExpiration, FileTransferProtocol ftProtocol) {
                if (fired.compareAndSet(false, true)) {
                    processNext();
                }
            }

            @Override
            public void onFileTransferResumed(ContactId contact) {
            }

            @Override
            public void onSessionAccepting(ContactId contact) {
            }

            @Override
            public void onFileTransferPausedByUser(ContactId contact) {
            }

            @Override
            public void onFileTransferPausedBySystem(ContactId contact) {
            }

            @Override
            public void onSessionRejected(ContactId contact, TerminationReason reason) {
            }

            @Override
            public void onSessionInvited(ContactId contact, MmContent file, MmContent fileIcon,
                    long timestamp, long timestampSent, long fileExpiration, long fileIconExpiration) {
            }

            @Override
            public void onSessionAutoAccepted(ContactId contact, MmContent file,
                    MmContent fileIcon, long timestamp, long timestampSent, long fileExpiration,
                    long fileIconExpiration) {
            }

            @Override
            public void onHttpDownloadInfoAvailable() {

            }
        };
    }
}
