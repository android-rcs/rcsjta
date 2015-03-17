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
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransferLog;

import android.net.Uri;

import java.util.Vector;

import javax2.sip.header.ContactHeader;

/**
 * Terminating file transfer HTTP session starting from invitation
 * 
 * @author vfml3370
 */
public class DownloadFromInviteFileSharingSession extends TerminatingHttpFileSharingSession {
    /**
     * The minimum delay in milliseconds to wait for user acceptance
     */
    private static long MINIMUM_DELAY_FOR_USER_ACCEPTANCE = 30000;

    /**
     * The logger
     */
    private final static Logger LOGGER = Logger
            .getLogger(DownloadFromInviteFileSharingSession.class.getSimpleName());

    private final long mTimestampSent;

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param chatSession the chat session
     * @param fileTransferInfo the File transfer info document
     * @param fileTransferId the File transfer Id
     * @param contact the remote contact Id
     * @param displayName the display name of the remote contact
     * @param rcsSettings
     * @param messagingLog
     * @param timestamp
     * @param timestampSent
     */
    public DownloadFromInviteFileSharingSession(ImsService parent, ChatSession chatSession,
            FileTransferHttpInfoDocument fileTransferInfo, String fileTransferId,
            ContactId contact, String displayName, RcsSettings rcsSettings,
            MessagingLog messagingLog, long timestamp, long timestampSent) {

        // @formatter:off
        super(parent,
                ContentManager.createMmContent(ContentManager.generateUriForReceivedContent(
                fileTransferInfo.getFilename(), fileTransferInfo.getFileType(), rcsSettings),
                fileTransferInfo.getFileSize(), fileTransferInfo.getFilename()),
                fileTransferInfo.getTransferValidity(),
                null,
                FileTransferData.UNKNOWN_EXPIRATION,
                contact,
                chatSession.getSessionID(),
                chatSession.getContributionID(),
                fileTransferId,
                chatSession.isGroupChat(),
                fileTransferInfo.getFileUri(),
                rcsSettings,
                messagingLog,
                timestamp,
                getRemoteSipId(chatSession));
        // @formatter:on

        mTimestampSent = timestampSent;

        setRemoteDisplayName(displayName);
        // Build a new dialogPath with this of chatSession and an empty CallId
        setDialogPath(new SipDialogPath(chatSession.getDialogPath()));
        getDialogPath().setCallId("");

        // Download thumbnail
        FileTransferHttpThumbnail thumbnailInfo = fileTransferInfo.getFileThumbnail();
        if (thumbnailInfo != null) {
            String iconName = FileTransferUtils.buildFileiconUrl(fileTransferId,
                    thumbnailInfo.getType());
            setFileicon(mDownloadManager.downloadThumbnail(thumbnailInfo, iconName));
            setIconExpiration(thumbnailInfo.getValidity());
        } else {
            setIconExpiration(FileTransferLog.NOT_APPLICABLE_EXPIRATION);
        }

        if (shouldBeAutoAccepted()) {
            setSessionAccepted();
        }
    }

    /**
     * Check if session should be auto accepted depending on settings and roaming conditions This
     * method should only be called once per session
     * 
     * @return true if file transfer should be auto accepted
     */
    private boolean shouldBeAutoAccepted() {
        long ftWarnSize = mRcsSettings.getWarningMaxFileTransferSize();

        if (ftWarnSize > 0 && getContent().getSize() > ftWarnSize) {
            /*
             * User should be warned about the potential charges associated to the transfer of a
             * large file. Hence do not auto accept if file size is above the warning limit.
             */
            return false;
        }

        if (getImsService().getImsModule().isInRoaming()) {
            return mRcsSettings.isFileTransferAutoAcceptedInRoaming();
        }

        return mRcsSettings.isFileTransferAutoAccepted();
    }

    /**
     * Background processing
     */
    public void run() {
        boolean logActivated = LOGGER.isActivated();
        if (logActivated) {
            LOGGER.info("Initiate a HTTP file transfer session as terminating");
        }
        Vector<ImsSessionListener> listeners = getListeners();
        ContactId contact = getRemoteContact();
        long fileExpiration = getFileExpiration();
        try {
            /* Check if session should be auto-accepted once */
            if (isSessionAccepted()) {
                if (logActivated) {
                    LOGGER.debug("Received HTTP file transfer invitation marked for auto-accept");
                }

                for (ImsSessionListener listener : listeners) {
                    ((FileSharingSessionListener) listener).handleSessionAutoAccepted(contact,
                            getContent(), getFileicon(), getTimestamp(), mTimestampSent,
                            fileExpiration, getIconExpiration());
                }
                Uri downloadServerAddress = mDownloadManager.getHttpServerAddr();
                mMessagingLog.setFileDownloadAddress(getFileTransferId(), downloadServerAddress);
                if (mRemoteInstanceId != null) {
                    mMessagingLog.setRemoteSipId(getFileTransferId(), mRemoteInstanceId);
                }
            } else {
                if (logActivated) {
                    LOGGER.debug("Received HTTP file transfer invitation marked for manual accept");
                }
                for (ImsSessionListener listener : listeners) {
                    ((FileSharingSessionListener) listener).handleSessionInvited(contact,
                            getContent(), getFileicon(), getTimestamp(), mTimestampSent,
                            fileExpiration, getIconExpiration());
                }
                Uri downloadServerAddress = mDownloadManager.getHttpServerAddr();
                mMessagingLog.setFileDownloadAddress(getFileTransferId(), downloadServerAddress);
                if (mRemoteInstanceId != null) {
                    mMessagingLog.setRemoteSipId(getFileTransferId(), mRemoteInstanceId);
                }
                // Compute the delay before file validity expiration
                long delay = fileExpiration - System.currentTimeMillis();
                if (delay <= 0) {
                    if (logActivated) {
                        LOGGER.debug("File no more available on server: transfer rejected on timeout");
                    }
                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejectedByTimeout(contact);
                    }
                    return;

                } else {
                    if (delay < MINIMUM_DELAY_FOR_USER_ACCEPTANCE) {
                        delay = MINIMUM_DELAY_FOR_USER_ACCEPTANCE;
                    }
                }
                if (logActivated) {
                    LOGGER.debug("Accept manually file transfer tiemout=".concat(Long
                            .toString(delay)));
                }
                // Wait invitation answer
                InvitationStatus answer = waitInvitationAnswer(delay);
                switch (answer) {
                    case INVITATION_REJECTED:
                        if (logActivated) {
                            LOGGER.debug("Transfer has been rejected by user");
                        }
                        removeSession();
                        for (ImsSessionListener listener : listeners) {
                            listener.handleSessionRejectedByUser(contact);
                        }
                        return;

                    case INVITATION_NOT_ANSWERED:
                        if (!isFileTransferPaused()) {
                            if (logActivated) {
                                LOGGER.debug("Transfer has been rejected on timeout");
                            }
                            removeSession();
                            for (ImsSessionListener listener : listeners) {
                                listener.handleSessionRejectedByTimeout(contact);
                            }
                        }
                        return;

                    case INVITATION_CANCELED:
                        if (logActivated) {
                            LOGGER.debug("Http transfer has been rejected by remote.");
                        }
                        removeSession();
                        for (ImsSessionListener listener : listeners) {
                            listener.handleSessionRejectedByRemote(contact);
                        }
                        return;

                    case INVITATION_ACCEPTED:
                        setSessionAccepted();
                        for (ImsSessionListener listener : listeners) {
                            ((FileSharingSessionListener) listener).handleSessionAccepted(contact);
                        }
                        break;

                    default:
                        if (logActivated) {
                            LOGGER.debug("Unknown invitation answer in run; answer=".concat(answer
                                    .toString()));
                        }
                        return;

                }
            }
        } catch (Exception e) {
            if (LOGGER.isActivated()) {
                LOGGER.error("Transfer has failed", e);
            }
            // Unexpected error
            handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
            return;

        }
        super.run();
    }

    private static String getRemoteSipId(ChatSession session) {
        ContactHeader inviteContactHeader = (ContactHeader) session.getDialogPath().getInvite()
                .getHeader(ContactHeader.NAME);
        if (inviteContactHeader == null) {
            return null;
        }
        return inviteContactHeader.getParameter(SipUtils.SIP_INSTANCE_PARAM);
    }
}
