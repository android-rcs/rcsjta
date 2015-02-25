/*******************************************************************************
w * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.provider.fthttp.FtHttpResumeDownload;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

/**
 * @author yplo6403
 */
public class ResumeDownloadFileSharingSession extends TerminatingHttpFileSharingSession {

    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(ResumeDownloadFileSharingSession.class
            .getSimpleName());

    /**
     * Constructor create instance of session object to resume download
     * 
     * @param parent IMS service
     * @param content the content (url, mime-type and size)
     * @param resumeDownload the data object in DB
     * @param rcsSettings
     */
    public ResumeDownloadFileSharingSession(ImsService parent, MmContent content,
            FtHttpResumeDownload resumeDownload, RcsSettings rcsSettings) {
        super(parent, content, resumeDownload, rcsSettings);
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (logger.isActivated()) {
                logger.info("Resume a HTTP file transfer session as terminating");
            }
            ContactId contact = getRemoteContact();
            for (int j = 0; j < getListeners().size(); j++) {
                getListeners().get(j).handleSessionStarted(contact);
            }

            // Resume download file from the HTTP server
            if (downloadManager.mStreamForFile != null && downloadManager.resumeDownload()) {
                if (logger.isActivated()) {
                    logger.debug("Resume download success for " + mResumeFT);
                }
                // Set file URL
                getContent().setUri(downloadManager.getDownloadedFileUri());

                // File transfered
                handleFileTransfered();
                // Send delivery report "displayed"
                sendDeliveryReport(ImdnDocument.DELIVERY_STATUS_DISPLAYED);
            } else {
                // Don't call handleError in case of Pause or Cancel
                if (downloadManager.isCancelled() || downloadManager.isPaused()) {
                    return;
                }

                // Upload error
                if (logger.isActivated()) {
                    logger.info("Resume Download file has failed");
                }
                handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED));
            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Transfer has failed", e);
            }
            // Unexpected error
            handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

}
