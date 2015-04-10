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

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

/**
 * Terminating file transfer HTTP session
 * 
 * @author vfml3370
 */
public abstract class TerminatingHttpFileSharingSession extends HttpFileTransferSession implements
        HttpTransferEventListener {

    /**
     * HTTP download manager
     */
    protected final HttpDownloadManager mDownloadManager;

    /**
     * Remote instance Id
     */
    protected final String mRemoteInstanceId;

    /**
     * The logger
     */
    private final static Logger sLogger = Logger.getLogger(TerminatingHttpFileSharingSession.class
            .getSimpleName());

    /**
     * Is File Transfer initiated from a GC
     */
    protected final boolean mGroupFileTransfer;

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param fileTransferId the File transfer Id
     * @param contact the remote contact Id
     * @param content
     * @param fileExpiration
     * @param fileIcon
     * @param iconExpiration
     * @param chatSessionId
     * @param chatContributionId
     * @param isGroup
     * @param httpServerAddress
     * @param rcsSettings
     * @param messagingLog
     * @param timestamp Local timestamp for the session
     * @param remoteInstanceId
     * @param contactManager
     */
    public TerminatingHttpFileSharingSession(ImsService parent, MmContent content,
            long fileExpiration, MmContent fileIcon, long iconExpiration, ContactId contact,
            String chatSessionId, String chatContributionId, String fileTransferId,
            boolean isGroup, Uri httpServerAddress, RcsSettings rcsSettings,
            MessagingLog messagingLog, long timestamp, String remoteInstanceId,
            ContactsManager contactManager) {
        super(parent, content, contact, PhoneUtils.formatContactIdToUri(contact), fileIcon,
                chatSessionId, chatContributionId, fileTransferId, rcsSettings, messagingLog,
                timestamp, fileExpiration, iconExpiration, contactManager);
        mGroupFileTransfer = isGroup;
        mRemoteInstanceId = remoteInstanceId;
        // Instantiate the download manager
        mDownloadManager = new HttpDownloadManager(content, this, httpServerAddress, rcsSettings);
    }

    protected boolean isGroupFileTransfer() {
        return mGroupFileTransfer;
    }

    /**
     * Posts an interrupt request to this Thread
     */
    @Override
    public void interrupt() {
        super.interrupt();

        // Interrupt the download
        mDownloadManager.interrupt();
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            // Notify listeners
            httpTransferStarted();

            Uri file = mDownloadManager.getDownloadedFileUri();
            // Download file from the HTTP server
            if (mDownloadManager.downloadFile()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Download file with success");
                }

                // Set filename
                getContent().setUri(file);

                // File transfered
                handleFileTransfered();

                // Send delivery report "displayed"
                // According to BB PDD section 6.1.4 there should be no display for GC messages.
                if (!mGroupFileTransfer) {
                    sendDeliveryReport(ImdnDocument.DELIVERY_STATUS_DISPLAYED,
                            System.currentTimeMillis());
                }
            } else {
                // Don't call handleError in case of Pause or Cancel
                if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                    return;
                }

                // Upload error
                if (sLogger.isActivated()) {
                    sLogger.info("Download file has failed");
                }
                handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED));
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Transfer has failed", e);
            }

            // Unexpected error
            handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

    // If session is rejected by user, session cannot be rejected at SIP level (already accepted
    // 200OK).
    @Override
    public void rejectSession(int code) {
        if (sLogger.isActivated()) {
            sLogger.debug("Session invitation has been rejected");
        }
        mInvitationStatus = InvitationStatus.INVITATION_REJECTED;

        // Unblock semaphore
        synchronized (mWaitUserAnswer) {
            mWaitUserAnswer.notifyAll();
        }

        // Remove the session in the session manager
        removeSession();
    }

    @Override
    public void handleError(ImsServiceError error) {
        super.handleError(error);
    }

    @Override
    public void handleFileTransfered() {
        super.handleFileTransfered();
    }

    /**
     * Send delivery report
     * 
     * @param status Report status
     * @param timestamp Local timestamp
     */
    protected void sendDeliveryReport(String status, long timestamp) {
        String msgId = getFileTransferId();
        if (sLogger.isActivated()) {
            sLogger.debug("Send delivery report ".concat(status));
        }
        ChatSession chatSession;
        ContactId contact = getRemoteContact();
        InstantMessagingService imService = Core.getInstance().getImService();
        if (mGroupFileTransfer) {
            chatSession = imService.getGroupChatSession(getContributionID());
        } else {
            chatSession = imService.getOneToOneChatSession(contact);
        }
        if (chatSession != null && chatSession.isMediaEstablished()) {
            // Send message delivery status via a MSRP
            chatSession.sendMsrpMessageDeliveryStatus(contact, msgId, status, timestamp);
        } else {
            // Send message delivery status via a SIP MESSAGE
            imService.getImdnManager().sendMessageDeliveryStatusImmediately(contact, msgId, status,
                    mRemoteInstanceId, timestamp);
        }
    }

    /**
     * Resume File Transfer
     */
    @Override
    public void resumeFileTransfer() {
        fileTransferResumed();
        mDownloadManager.getListener().httpTransferResumed();

        new Thread(new Runnable() {
            public void run() {
                // Download file from the HTTP server
                if (mDownloadManager.resumeDownload()) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Download file with success");
                    }

                    // Set filename
                    getContent().setUri(mDownloadManager.getDownloadedFileUri());

                    // File transfered
                    handleFileTransfered();

                    // Send delivery report "displayed"
                    sendDeliveryReport(ImdnDocument.DELIVERY_STATUS_DISPLAYED,
                            System.currentTimeMillis());
                } else {
                    // Don't call handleError in case of Pause or Cancel
                    if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                        return;
                    }

                    // Upload error
                    if (sLogger.isActivated()) {
                        sLogger.info("Download file has failed");
                    }
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED));
                }
            }
        }).start();
    }

    /**
     * Pause File Transfer
     */
    @Override
    public void pauseFileTransfer() {
        fileTransferPaused();
        interruptSession();
        mDownloadManager.pauseTransferByUser();
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }

}
