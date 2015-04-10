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

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.filetransfer.FileTransferLog;

/**
 * Originating file transfer HTTP session
 * 
 * @author vfml3370
 */
public class OriginatingHttpGroupFileSharingSession extends HttpFileTransferSession implements
        HttpUploadTransferEventListener {

    private final Core mCore;

    /**
     * HTTP upload manager
     */
    private HttpUploadManager mUploadManager;

    /**
     * File information to send via chat
     */
    private String mFileInfo;

    /**
     * Chat session used to send file info
     */
    private ChatSession mChatSession;

    /**
     * The timestamp to be sent in payload when the file sharing was initiated for outgoing group
     * file sharing
     */
    private long mTimestampSent;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger
            .getLogger(OriginatingHttpGroupFileSharingSession.class.getName());

    /**
     * Constructor
     * 
     * @param fileTransferId File transfer Id
     * @param parent IMS service
     * @param content The file content to share
     * @param fileIcon Content of fileicon
     * @param conferenceId Conference ID
     * @param chatSessionId Chat session ID
     * @param chatContributionId Chat contribution Id
     * @param tId TID of the upload
     * @param core Core
     * @param rcsSettings
     * @param messagingLog
     * @param timestamp Local timestamp for the session
     * @param timestampSent the timestamp sent in payload for the group file sharing
     * @param contactManager
     */
    public OriginatingHttpGroupFileSharingSession(String fileTransferId, ImsService parent,
            MmContent content, MmContent fileIcon, String conferenceId, String chatSessionId,
            String chatContributionId, String tId, Core core, RcsSettings rcsSettings,
            MessagingLog messagingLog, long timestamp, long timestampSent,
            ContactsManager contactManager) {
        super(parent, content, null, conferenceId, fileIcon, chatSessionId, chatContributionId,
                fileTransferId, rcsSettings, messagingLog, timestamp,
                FileTransferLog.UNKNOWN_EXPIRATION, FileTransferLog.UNKNOWN_EXPIRATION,
                contactManager);
        mCore = core;
        mTimestampSent = timestampSent;

        // Instantiate the upload manager
        mUploadManager = new HttpUploadManager(getContent(), fileIcon, this, tId, rcsSettings);
    }

    /**
     * Background processing
     */
    public void run() {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a new HTTP group file transfer session as originating");
        }
        try {
            // Upload the file to the HTTP server
            byte[] result = mUploadManager.uploadFile();
            sendResultToContact(result);
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("File transfer has failed", e);
            }

            // Unexpected error
            handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

    @Override
    public void handleError(ImsServiceError error) {
        super.handleError(error);
    }

    @Override
    public void handleFileTransfered() {
        super.handleFileTransfered();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        mUploadManager.interrupt();
    }

    /**
     * Send the file transfer information
     */
    private void sendFileTransferInfo() {
        String mime = CpimMessage.MIME_TYPE;
        String from = ImsModule.IMS_USER_PROFILE.getPublicAddress();
        String to = ChatUtils.ANOMYNOUS_URI;
        // Note: FileTransferId is always generated to equal the associated msgId of a FileTransfer
        // invitation message.
        String msgId = getFileTransferId();

        String content = ChatUtils.buildCpimMessageWithImdn(from, to, msgId, mFileInfo,
                FileTransferHttpInfoDocument.MIME_TYPE, mTimestampSent);

        mChatSession.sendDataChunks(IdGenerator.generateMessageID(), content, mime,
                TypeMsrpChunk.FileSharing);
    }

    /**
     * Prepare to send the info to terminating side
     * 
     * @param result byte[] which contains the result of the 200 OK from the content server
     */
    private void sendResultToContact(byte[] result) {
        if (mUploadManager.isCancelled()) {
            return;
        }
        FileTransferHttpInfoDocument infoDocument;
        boolean logActivated = sLogger.isActivated();
        if (result == null
                || (infoDocument = FileTransferUtils.parseFileTransferHttpDocument(result,
                        mRcsSettings)) == null) {
            if (logActivated) {
                sLogger.debug("Upload has failed");
            }
            handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED));
            return;

        }
        mFileInfo = new String(result, UTF8);
        if (logActivated) {
            sLogger.debug("Upload done with success: ".concat(mFileInfo.toString()));
        }
        setFileExpiration(infoDocument.getExpiration());
        FileTransferHttpThumbnail thumbnail = infoDocument.getFileThumbnail();
        if (thumbnail != null) {
            setIconExpiration(thumbnail.getExpiration());
        } else {
            setIconExpiration(FileTransferLog.UNKNOWN_EXPIRATION);
        }

        mChatSession = mCore.getImService().getGroupChatSession(getContributionID());
        if (mChatSession != null) {
            if (logActivated) {
                sLogger.debug("Send file transfer info via an existing chat session");
            }
            sendFileTransferInfo();
            handleFileTransfered();

        } else {
            handleError(new FileSharingError(FileSharingError.NO_CHAT_SESSION));
        }
    }

    /**
     * Pausing the transfer
     */
    @Override
    public void pauseFileTransfer() {
        fileTransferPaused();
        interruptSession();
        mUploadManager.pauseTransferByUser();
    }

    /**
     * Resuming the transfer
     */
    @Override
    public void resumeFileTransfer() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    FtHttpResumeUpload upload = mMessagingLog
                            .retrieveFtHttpResumeUpload(mUploadManager.getTId());
                    if (upload != null) {
                        sendResultToContact(mUploadManager.resumeUpload());
                    } else {
                        sendResultToContact(null);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void uploadStarted() {
    }

    @Override
    public boolean isInitiatedByRemote() {
        return false;
    }

    /**
     * Sets the timestamp when file icon on the content server is no longer valid to download.
     * 
     * @param timestamp
     */
    public void setIconExpiration(long timestamp) {
        mIconExpiration = timestamp;
    }

    /**
     * Sets the timestamp when file on the content server is no longer valid to download.
     * 
     * @param timestamp
     */
    public void setFileExpiration(long timestamp) {
        mFileExpiration = timestamp;
    }
}
