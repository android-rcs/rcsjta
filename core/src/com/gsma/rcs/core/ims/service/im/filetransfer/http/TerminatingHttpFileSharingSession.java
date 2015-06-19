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
import com.gsma.rcs.core.ims.protocol.msrp.MsrpException;
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.IOException;

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
    private final Logger mLogger = Logger.getLogger(getClass().getSimpleName());

    /**
     * Is File Transfer initiated from a GC
     */
    protected final boolean mGroupFileTransfer;

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
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
    public TerminatingHttpFileSharingSession(InstantMessagingService imService, MmContent content,
            long fileExpiration, MmContent fileIcon, long iconExpiration, ContactId contact,
            String chatSessionId, String chatContributionId, String fileTransferId,
            boolean isGroup, Uri httpServerAddress, RcsSettings rcsSettings,
            MessagingLog messagingLog, long timestamp, String remoteInstanceId,
            ContactManager contactManager) {
        super(imService, content, contact, PhoneUtils.formatContactIdToUri(contact), fileIcon,
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
            /* Notify listeners */
            httpTransferStarted();

            Uri file = mDownloadManager.getDownloadedFileUri();
            /* Download file from the HTTP server */
            mDownloadManager.downloadFile();
            if (mLogger.isActivated()) {
                mLogger.debug("Download file with success");
            }

            /* Set filename */
            getContent().setUri(file);

            /* File transfered */
            handleFileTransfered();

            if (mImdnManager.isSendOneToOneDeliveryDisplayedReportsEnabled()) {
                sendDeliveryReport(ImdnDocument.DELIVERY_STATUS_DISPLAYED,
                        System.currentTimeMillis());
            }
        } catch (FileNotFoundException e) {
            /* Don't call handleError in case of Pause or Cancel */
            if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                return;
            }
            mLogger.error("Download of file has failed for mRemoteInstanceId : "
                    .concat(mRemoteInstanceId), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
        } catch (FileNotDownloadedException e) {
            /* Don't call handleError in case of Pause or Cancel */
            if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                return;
            }
            mLogger.error("Download of file has failed for mRemoteInstanceId : "
                    .concat(mRemoteInstanceId), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
        } catch (IOException e) {
            /* Don't call handleError in case of Pause or Cancel */
            if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                return;
            }
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
        } catch (SipPayloadException e) {
            mLogger.error("Download of file has failed for mRemoteInstanceId : "
                    .concat(mRemoteInstanceId), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
        } catch (SipNetworkException e) {
            if (mLogger.isActivated()) {
                mLogger.debug(e.getMessage());
            }
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            mLogger.error("Download of file has failed for mRemoteInstanceId : "
                    .concat(mRemoteInstanceId), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
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

    /**
     * Send delivery report
     * 
     * @param status Report status
     * @param timestamp Local timestamp
     * @throws MsrpException
     * @throws SipNetworkException
     * @throws SipPayloadException
     */
    protected void sendDeliveryReport(String status, long timestamp) throws MsrpException,
            SipPayloadException, SipNetworkException {
        String msgId = getFileTransferId();
        if (mLogger.isActivated()) {
            mLogger.debug("Send delivery report ".concat(status));
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
            mImdnManager.sendMessageDeliveryStatusImmediately(contact, msgId, status,
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
                try {
                    /* Download file from the HTTP server */
                    mDownloadManager.resumeDownload();
                    if (mLogger.isActivated()) {
                        mLogger.debug("Download file with success");
                    }

                    /* Set filename */
                    getContent().setUri(mDownloadManager.getDownloadedFileUri());

                    handleFileTransfered();

                    if (mImdnManager.isSendOneToOneDeliveryDisplayedReportsEnabled()) {
                        sendDeliveryReport(ImdnDocument.DELIVERY_STATUS_DISPLAYED,
                                System.currentTimeMillis());
                    }
                } catch (FileNotFoundException e) {
                    /* Don't call handleError in case of Pause or Cancel */
                    if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                        return;
                    }
                    mLogger.error("Download of file has failed for mRemoteInstanceId : "
                            .concat(mRemoteInstanceId), e);
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
                } catch (FileNotDownloadedException e) {
                    /* Don't call handleError in case of Pause or Cancel */
                    if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                        return;
                    }
                    mLogger.error("Download of file has failed for mRemoteInstanceId : "
                            .concat(mRemoteInstanceId), e);
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
                } catch (IOException e) {
                    /* Don't call handleError in case of Pause or Cancel */
                    if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                        return;
                    }
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
                } catch (SipPayloadException e) {
                    mLogger.error("Download of file has failed for mRemoteInstanceId : "
                            .concat(mRemoteInstanceId), e);
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
                } catch (SipNetworkException e) {
                    if (mLogger.isActivated()) {
                        mLogger.debug(e.getMessage());
                    }
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    mLogger.error("Download of file has failed for mRemoteInstanceId : "
                            .concat(mRemoteInstanceId), e);
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
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

    @Override
    public void terminateSession(TerminationReason reason) {
        if (mLogger.isActivated()) {
            mLogger.debug("terminateSession reason=".concat(reason.toString()));
        }
        closeHttpSession(reason);
        /*
         * If reason is TERMINATION_BY_SYSTEM or TERMINATION_BY_CONNECTION_LOST and session already
         * started, then it's a pause
         */
        boolean sessionAccepted = isSessionAccepted();
        ContactId contact = getRemoteContact();
        switch (reason) {
            case TERMINATION_BY_SYSTEM:
                /* Intentional fall through */
            case TERMINATION_BY_CONNECTION_LOST:
                if (isFileTransferPaused()) {
                    return;
                }
                /*
                 * File transfer invitation is valid until file transfer validity expires and hence
                 * the state of file transfer is not altered if the invitation was not yet answered.
                 */
                if (!sessionAccepted) {
                    return;
                }
                if (mLogger.isActivated()) {
                    mLogger.debug("Pause the session (session terminated, but can be resumed)");
                }
                for (ImsSessionListener listener : getListeners()) {
                    ((FileSharingSessionListener) listener)
                            .handleFileTransferPausedBySystem(contact);
                }
                return;
            default:
                break;
        }

        if (sessionAccepted) {
            for (ImsSessionListener listener : getListeners()) {
                listener.handleSessionAborted(contact, reason);
            }
            return;
        }
        for (ImsSessionListener listener : getListeners()) {
            listener.handleSessionRejected(contact, reason);
        }
    }
}
