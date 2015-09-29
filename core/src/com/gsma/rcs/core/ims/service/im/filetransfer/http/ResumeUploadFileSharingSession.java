/*******************************************************************************
w * Software Name : RCS IMS Stack
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

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import java.io.IOException;

/**
 * Resuming session of OriginatingHttpFileSharingSession
 * 
 * @author Benoit JOGUET
 */
public class ResumeUploadFileSharingSession extends OriginatingHttpFileSharingSession {

    private final static Logger sLogger = Logger.getLogger(ResumeUploadFileSharingSession.class
            .getSimpleName());

    /**
     * Constructor create instance of session object to resume download
     * 
     * @param imService InstantMessagingService
     * @param content the content (url, mime-type and size)
     * @param resumeUpload the data object in DB
     * @param rcsSettings
     * @param messagingLog
     * @param contactManager
     */
    public ResumeUploadFileSharingSession(InstantMessagingService imService, MmContent content,
            FtHttpResumeUpload resumeUpload, RcsSettings rcsSettings, MessagingLog messagingLog,
            ContactManager contactManager) {
        super(imService, resumeUpload.getFileTransferId(), content, resumeUpload.getContact(),
                resumeUpload.getFileicon() != null ? FileTransferUtils.createMmContent(resumeUpload
                        .getFileicon()) : null, resumeUpload.getTId(), messagingLog, rcsSettings,
                resumeUpload.getTimestamp(), resumeUpload.getTimestampSent(), contactManager);
    }

    @Override
    public void run() {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Resume a HTTP file transfer session as originating");
        }
        try {
            httpTransferStarted();
            /* Resume the file upload to the HTTP server */
            byte[] result = mUploadManager.resumeUpload();
            sendResultToContact(result);

        } catch (IOException e) {
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (PayloadException e) {
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (NetworkException e) {
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));
        }
    }

    @Override
    public void uploadStarted() {
        // Upload entry already created in fthttp table
    }

}
