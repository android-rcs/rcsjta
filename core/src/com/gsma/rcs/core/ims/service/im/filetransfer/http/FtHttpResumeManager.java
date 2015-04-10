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
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.fthttp.FtHttpResume;
import com.gsma.rcs.provider.fthttp.FtHttpResumeDownload;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * File Transfer HTTP resume manager
 */
public class FtHttpResumeManager implements Runnable {
    /**
     * IMS service
     */
    private InstantMessagingService mImsService;

    /**
     * List of pending sessions to resume
     */
    private LinkedList<FtHttpResume> mListOfFtHttpResume;

    /**
     * FT HTTP session being resumed
     */
    private FtHttpResume mFtHttpResume;

    private static final Logger sLogger = Logger.getLogger(FtHttpResumeManager.class
            .getSimpleName());

    private final RcsSettings mRcsSettings;

    private final MessagingLog mMessagingLog;

    private final ContactsManager mContactManager;

    /**
     * Constructor
     * 
     * @param instantMessagingService IMS service
     * @param rcsSettings
     * @param messagingLog
     * @param contactManager
     */
    public FtHttpResumeManager(InstantMessagingService instantMessagingService,
            RcsSettings rcsSettings, MessagingLog messagingLog, ContactsManager contactManager) {
        mRcsSettings = rcsSettings;
        mImsService = instantMessagingService;
        mMessagingLog = messagingLog;
        mContactManager = contactManager;
    }

    @Override
    public void run() {
        try {
            // Retrieve all resumable sessions
            List<FtHttpResume> transfersToResume = mMessagingLog
                    .retrieveFileTransfersPausedBySystem();
            if (!transfersToResume.isEmpty()) {
                mListOfFtHttpResume = new LinkedList<FtHttpResume>(transfersToResume);
                processNext();
            }
        } catch (Exception e) {
            // handle exception
            if (sLogger.isActivated()) {
                sLogger.error("Exception occurred", e);
            }
        }
    }

    /**
     * resume next pending session
     */
    private void processNext() {
        if (mListOfFtHttpResume.isEmpty())
            return;
        // Remove the oldest session from the list
        mFtHttpResume = mListOfFtHttpResume.poll();
        if (sLogger.isActivated()) {
            sLogger.debug("Resume FT HTTP ".concat(mFtHttpResume.toString()));
        }
        switch (mFtHttpResume.getDirection()) {
            case INCOMING:
                FtHttpResumeDownload downloadInfo = (FtHttpResumeDownload) mFtHttpResume;
                MmContent downloadContent = ContentManager.createMmContent(mFtHttpResume.getFile(),
                        downloadInfo.getSize(), downloadInfo.getFileName());

                // Creates the Resume Download session object
                final DownloadFromResumeFileSharingSession resumeDownload = new DownloadFromResumeFileSharingSession(
                        mImsService, downloadContent, downloadInfo, mRcsSettings, mMessagingLog,
                        mContactManager);
                resumeDownload.addListener(getFileSharingSessionListener());
                // Start the download HTTP FT session object
                new Thread() {
                    public void run() {
                        resumeDownload.startSession();
                    }
                }.start();
                // Notify the UI and update rich messaging
                mImsService
                        .getImsModule()
                        .getCore()
                        .getListener()
                        .handleIncomingFileTransferResuming(resumeDownload,
                                resumeDownload.isGroupFileTransfer(),
                                resumeDownload.getChatSessionID(),
                                resumeDownload.getContributionID());
                break;

            case OUTGOING:
                // TODO : only managed for 1-1 FToHTTP
                FtHttpResumeUpload uploadInfo = (FtHttpResumeUpload) mFtHttpResume;
                if (!mFtHttpResume.isGroupTransfer()) {
                    // Get upload content
                    MmContent uploadContent = ContentManager.createMmContent(uploadInfo.getFile(),
                            uploadInfo.getSize(), uploadInfo.getFileName());

                    // Create Resume Upload session
                    final ResumeUploadFileSharingSession resumeUpload = new ResumeUploadFileSharingSession(
                            mImsService, uploadContent, uploadInfo, mRcsSettings, mMessagingLog,
                            mContactManager);
                    resumeUpload.addListener(getFileSharingSessionListener());

                    // Start Resume Upload session
                    new Thread() {
                        public void run() {
                            resumeUpload.startSession();
                        }
                    }.start();

                    // Notify the UI and update rich messaging
                    mImsService.getImsModule().getCore().getListener()
                            .handleOutgoingFileTransferResuming(resumeUpload, false);
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
            public void handleSessionStarted(ContactId contact) {
            }

            @Override
            public void handleSessionAborted(ContactId contact, TerminationReason reason) {
                if (fired.compareAndSet(false, true)) {
                    processNext();
                }
            }

            @Override
            public void handleTransferProgress(ContactId contact, long currentSize, long totalSize) {
            }

            @Override
            public void handleTransferNotAllowedToSend(ContactId contact) {
            }

            @Override
            public void handleTransferError(FileSharingError error, ContactId contact) {
                if (fired.compareAndSet(false, true)) {
                    processNext();
                }
            }

            @Override
            public void handleFileTransfered(MmContent content, ContactId contact,
                    long fileExpiration, long fileIconExpiration) {
                if (fired.compareAndSet(false, true)) {
                    processNext();
                }
            }

            @Override
            public void handleFileTransferResumed(ContactId contact) {
            }

            @Override
            public void handleSessionAccepted(ContactId contact) {
            }

            @Override
            public void handleFileTransferPausedByUser(ContactId contact) {
            }

            @Override
            public void handleFileTransferPausedBySystem(ContactId contact) {
            }

            @Override
            public void handleSessionRejectedByUser(ContactId contact) {
            }

            @Override
            public void handleSessionRejectedByTimeout(ContactId contact) {
            }

            @Override
            public void handleSessionRejectedByRemote(ContactId contact) {
            }

            @Override
            public void handleSessionInvited(ContactId contact, MmContent file, MmContent fileIcon,
                    long timestamp, long timestampSent, long fileExpiration, long fileIconExpiration) {
            }

            @Override
            public void handleSessionAutoAccepted(ContactId contact, MmContent file,
                    MmContent fileIcon, long timestamp, long timestampSent, long fileExpiration,
                    long fileIconExpiration) {
            }
        };
    }

    /**
     * Terminates
     */
    public void terminate() {
    }
}
