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
import com.gsma.rcs.provider.fthttp.FtHttpResume;
import com.gsma.rcs.provider.fthttp.FtHttpResumeDaoImpl;
import com.gsma.rcs.provider.fthttp.FtHttpResumeDownload;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * File Transfer HTTP resume manager
 */
public class FtHttpResumeManager {
    /**
     * Interface to get access to the FtHttp table
     */
    private FtHttpResumeDaoImpl mDao = FtHttpResumeDaoImpl.getInstance();

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

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(FtHttpResumeManager.class
            .getSimpleName());

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param instantMessagingService IMS service
     * @param rcsSettings
     */
    public FtHttpResumeManager(InstantMessagingService instantMessagingService,
            RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
        if (mDao == null) {
            if (sLogger.isActivated()) {
                sLogger.error("Cannot resume FT");
            }
            return;
        }
        mImsService = instantMessagingService;

        try {
            // Retrieve all resumable sessions
            List<FtHttpResume> listFile2resume = mDao.queryAll();
            if (listFile2resume.isEmpty() == false) {
                // Rich Messaging - set all "in progress" File transfer to "paused".
                // This is necessary in case of the application can't update the
                // state before device switch off.
                for (FtHttpResume ftHttpResume : listFile2resume) {
                    MessagingLog.getInstance().setFileTransferStateAndReasonCode(
                            ftHttpResume.getFileTransferId(), FileTransfer.State.PAUSED,
                            FileTransfer.ReasonCode.PAUSED_BY_SYSTEM);
                }
                mListOfFtHttpResume = new LinkedList<FtHttpResume>(listFile2resume);
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
            sLogger.debug("Resume FT HTTP " + mFtHttpResume);
        }
        switch (mFtHttpResume.getDirection()) {
            case INCOMING:
                FtHttpResumeDownload downloadInfo = (FtHttpResumeDownload) mFtHttpResume;
                MmContent downloadContent = ContentManager.createMmContent(mFtHttpResume.getFile(),
                        downloadInfo.getSize(), downloadInfo.getFileName());
                // Creates the Resume Download session object
                final ResumeDownloadFileSharingSession resumeDownload = new ResumeDownloadFileSharingSession(
                        mImsService, downloadContent, downloadInfo, mRcsSettings);
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
                    MmContent uploadContent = ContentManager.createMmContentFromMime(
                            uploadInfo.getFile(), uploadInfo.getMimetype(), uploadInfo.getSize(),
                            uploadInfo.getFileName());

                    // Create Resume Upload session
                    final ResumeUploadFileSharingSession resumeUpload = new ResumeUploadFileSharingSession(
                            mImsService, uploadContent, uploadInfo, mRcsSettings);
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
            public void handleSessionTerminatedByRemote(ContactId contact) {
                if (fired.compareAndSet(false, true)) {
                    processNext();
                }
            }

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
            public void handleFileTransfered(MmContent content, ContactId contact) {
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
            public void handleSessionInvited(ContactId contact, MmContent file, MmContent fileIcon) {
            }

            @Override
            public void handleSessionAutoAccepted(ContactId contact, MmContent file,
                    MmContent fileIcon) {
            }
        };
    }

    /**
     * Terminates
     */
    public void terminate() {
    }
}
