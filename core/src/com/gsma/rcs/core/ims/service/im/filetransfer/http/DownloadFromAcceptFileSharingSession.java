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
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.fthttp.FtHttpResumeDownload;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

/**
 * Terminating file transfer HTTP session starting from user acceptance (after core was restarted).
 */
public class DownloadFromAcceptFileSharingSession extends TerminatingHttpFileSharingSession {

    private static final Logger sLogger = Logger
            .getLogger(DownloadFromAcceptFileSharingSession.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param content the content to be transferred
     * @param resume the Data Object to access FT HTTP table in DB
     * @param rcsSettings the RCS settings accessor
     * @param messagingLog the messaging log accessor
     * @param contactManager the contact manager
     */
    public DownloadFromAcceptFileSharingSession(InstantMessagingService imService,
            MmContent content, FtHttpResumeDownload resume, RcsSettings rcsSettings,
            MessagingLog messagingLog, ContactManager contactManager) {
        // @formatter:off
        super(imService,
                content,
                resume.getFileExpiration(),
                resume.getFileicon() != null ? FileTransferUtils.createIconContent(resume.getFileicon()) : null,
                resume.getFileicon() != null ? resume.getIconExpiration() : FileTransferData.UNKNOWN_EXPIRATION,
                resume.getContact(),
                resume.getChatId(),
                resume.getFileTransferId(),
                resume.isGroupTransfer(),
                resume.getServerAddress(),
                rcsSettings,
                messagingLog,
                resume.getTimestamp(),
                resume.getRemoteSipInstance(),
                contactManager);
        // @formatter:on
        setSessionAccepted();
    }

    @Override
    public void run() {
        if (sLogger.isActivated()) {
            sLogger.info("Accept HTTP file transfer session");
        }
        try {
            onHttpTransferStarted();

        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Download failed for a file sessionId : " + getSessionID()
                    + " with transferId : " + getFileTransferId(), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
            return;
        }
        super.run();
    }
}
