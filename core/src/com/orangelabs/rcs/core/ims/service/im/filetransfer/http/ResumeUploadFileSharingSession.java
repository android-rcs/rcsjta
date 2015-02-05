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

package com.orangelabs.rcs.core.ims.service.im.filetransfer.http;

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeUpload;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Resuming session of OriginatingHttpFileSharingSession
 * 
 * @author Benoit JOGUET
 */
public class ResumeUploadFileSharingSession extends OriginatingHttpFileSharingSession {

    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(ResumeUploadFileSharingSession.class
            .getSimpleName());

    /**
     * Constructor create instance of session object to resume download
     * 
     * @param parent IMS service
     * @param content the content (url, mime-type and size)
     * @param resumeUpload the data object in DB
     */
    public ResumeUploadFileSharingSession(ImsService parent, MmContent content,
            FtHttpResumeUpload resumeUpload) {
        super(resumeUpload.getFileTransferId(), parent, content, resumeUpload.getContact(),
                resumeUpload.getFileicon() != null ? FileTransferUtils.createMmContent(resumeUpload
                        .getFileicon()) : null, resumeUpload.getTId(), Core.getInstance(),
                MessagingLog.getInstance());
        this.resumeFT = resumeUpload;
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (logger.isActivated()) {
                logger.info("Resume a HTTP file transfer session as originating");
            }
            ContactId contact = getRemoteContact();
            for (int j = 0; j < getListeners().size(); j++) {
                getListeners().get(j).handleSessionStarted(contact);
            }

            // Resume the file upload to the HTTP server
            byte[] result = uploadManager.resumeUpload();
            sendResultToContact(result);
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Transfer has failed", e);
            }
            // Unexpected error
            handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

    @Override
    public void uploadStarted() {
        // Upload entry already created in fthttp table
    }

}
