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
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
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
public abstract class TerminatingHttpFileSharingSession extends HttpFileTransferSession {

    protected final HttpDownloadManager mDownloadManager;

    protected final String mRemoteInstanceId;

    private static final Logger sLogger = Logger.getLogger(TerminatingHttpFileSharingSession.class
            .getSimpleName());

    /**
     * Is File Transfer initiated from a GC
     */
    protected final boolean mGroupFileTransfer;

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param fileTransferId the File transfer Id
     * @param remote the remote contact Id
     * @param content
     * @param fileExpiration
     * @param fileIcon
     * @param iconExpiration
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
            long fileExpiration, MmContent fileIcon, long iconExpiration, ContactId remote,
            String chatContributionId, String fileTransferId, boolean isGroup,
            Uri httpServerAddress, RcsSettings rcsSettings, MessagingLog messagingLog,
            long timestamp, String remoteInstanceId, ContactManager contactManager) {
        // @formatter:off
        super(imService,
                content,
                remote,
                PhoneUtils.formatContactIdToUri(remote),
                fileIcon,
                chatContributionId,
                fileTransferId,
                rcsSettings,
                messagingLog,
                timestamp,
                fileExpiration,
                iconExpiration,
                contactManager);
        // @formatter:on
        mGroupFileTransfer = isGroup;
        mRemoteInstanceId = remoteInstanceId;
        mDownloadManager = new HttpDownloadManager(content, this, httpServerAddress, rcsSettings);
    }

    protected boolean isGroupFileTransfer() {
        return mGroupFileTransfer;
    }

    @Override
    public void interrupt() {
        super.interrupt();
        mDownloadManager.interrupt();
    }

    @Override
    public void run() {
        try {
            onHttpTransferStarted();
            Uri file = mDownloadManager.getDownloadedFileUri();
            /* Download file from the HTTP server */
            mDownloadManager.downloadFile();
            if (sLogger.isActivated()) {
                sLogger.debug("Download file with success");
            }
            getContent().setUri(file);
            handleFileTransferred();
            if (mImdnManager.isSendOneToOneDeliveryDisplayedReportsEnabled()) {
                sendDeliveryReport(ImdnDocument.DELIVERY_STATUS_DISPLAYED,
                        System.currentTimeMillis());
            }

        } catch (NetworkException e) {
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

        } catch (FileNotFoundException e) {
            /* Don't call handleError in case of Pause or Cancel */
            if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                return;
            }
            sLogger.error(new StringBuilder("Download of file has failed for mRemoteInstanceId : ")
                    .append(mRemoteInstanceId).toString(), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

        } catch (FileNotDownloadedException e) {
            /* Don't call handleError in case of Pause or Cancel */
            if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                return;
            }
            sLogger.error(new StringBuilder("Download of file has failed for mRemoteInstanceId : ")
                    .append(mRemoteInstanceId).toString(), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

        } catch (IOException e) {
            /* Don't call handleError in case of Pause or Cancel */
            if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                return;
            }
            sLogger.error(new StringBuilder("Download of file has failed for mRemoteInstanceId : ")
                    .append(mRemoteInstanceId).toString(), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

        } catch (PayloadException e) {
            sLogger.error(new StringBuilder("Download of file has failed for mRemoteInstanceId : ")
                    .append(mRemoteInstanceId).toString(), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error(new StringBuilder("Download of file has failed for mRemoteInstanceId : ")
                    .append(mRemoteInstanceId).toString(), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
        }
    }

    /**
     * Send delivery report
     * 
     * @param status Report status
     * @param timestamp Local timestamp
     * @throws NetworkException
     * @throws PayloadException
     */
    protected void sendDeliveryReport(String status, long timestamp) throws PayloadException,
            NetworkException {
        String msgId = getFileTransferId();
        if (sLogger.isActivated()) {
            sLogger.debug("Send delivery report ".concat(status));
        }
        ChatSession chatSession;
        ContactId remote = getRemoteContact();
        InstantMessagingService imService = Core.getInstance().getImService();
        if (mGroupFileTransfer) {
            chatSession = imService.getGroupChatSession(getContributionID());
        } else {
            chatSession = imService.getOneToOneChatSession(remote);
        }
        if (chatSession != null && chatSession.isMediaEstablished()) {
            chatSession.sendMsrpMessageDeliveryStatus(remote, msgId, status, timestamp);
        } else {
            String chatId = mGroupFileTransfer ? getContributionID() : remote.toString();
            mImdnManager.sendMessageDeliveryStatusImmediately(chatId, remote, msgId, status,
                    mRemoteInstanceId, timestamp);
        }
    }

    /**
     * Pause File Transfer
     */
    @Override
    public void onPause() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    fileTransferPaused();
                    interruptSession();
                    mDownloadManager.pauseTransferByUser();

                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error(new StringBuilder(
                            "Pause of download of file has failed for mRemoteInstanceId : ")
                            .append(mRemoteInstanceId).toString(), e);
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
                }
            }
        }).start();
    }

    /**
     * Resume File Transfer
     */
    @Override
    public void onResume() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    fileTransferResumed();
                    mDownloadManager.getListener().onHttpTransferResumed();
                    /* Download file from the HTTP server */
                    mDownloadManager.resumeDownload();
                    if (sLogger.isActivated()) {
                        sLogger.debug("Download file with success");
                    }
                    getContent().setUri(mDownloadManager.getDownloadedFileUri());
                    handleFileTransferred();
                    if (mImdnManager.isSendOneToOneDeliveryDisplayedReportsEnabled()) {
                        sendDeliveryReport(ImdnDocument.DELIVERY_STATUS_DISPLAYED,
                                System.currentTimeMillis());
                    }

                } catch (NetworkException e) {
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

                } catch (FileNotFoundException e) {
                    /* Don't call handleError in case of Pause or Cancel */
                    if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                        return;
                    }
                    sLogger.error(
                            new StringBuilder(
                                    "Download of file has failed for mRemoteInstanceId : ").append(
                                    mRemoteInstanceId).toString(), e);
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

                } catch (FileNotDownloadedException e) {
                    /* Don't call handleError in case of Pause or Cancel */
                    if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                        return;
                    }
                    sLogger.error(
                            new StringBuilder(
                                    "Download of file has failed for mRemoteInstanceId : ").append(
                                    mRemoteInstanceId).toString(), e);
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

                } catch (IOException e) {
                    /* Don't call handleError in case of Pause or Cancel */
                    if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                        return;
                    }
                    sLogger.error(
                            new StringBuilder(
                                    "Download of file has failed for mRemoteInstanceId : ").append(
                                    mRemoteInstanceId).toString(), e);
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

                } catch (PayloadException e) {
                    sLogger.error(
                            new StringBuilder(
                                    "Download of file has failed for mRemoteInstanceId : ").append(
                                    mRemoteInstanceId).toString(), e);
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder(
                                    "Download of file has failed for mRemoteInstanceId : ").append(
                                    mRemoteInstanceId).toString(), e);
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
                }
            }
        }).start();
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }

    @Override
    public void terminateSession(TerminationReason reason) throws PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("terminateSession reason=".concat(reason.toString()));
        }
        closeHttpSession(reason);
        /*
         * If reason is TERMINATION_BY_SYSTEM or TERMINATION_BY_CONNECTION_LOST and session already
         * started, then it's a pause
         */
        boolean sessionAccepted = isSessionAccepted();
        ContactId remote = getRemoteContact();
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
                if (sLogger.isActivated()) {
                    sLogger.debug("Pause the session (session terminated, but can be resumed)");
                }
                for (ImsSessionListener listener : getListeners()) {
                    ((FileSharingSessionListener) listener).onFileTransferPausedBySystem(remote);
                }
                return;
            default:
                break;
        }

        if (sessionAccepted) {
            for (ImsSessionListener listener : getListeners()) {
                listener.onSessionAborted(remote, reason);
            }
            return;
        }
        for (ImsSessionListener listener : getListeners()) {
            listener.onSessionRejected(remote, reason);
        }
    }
}
