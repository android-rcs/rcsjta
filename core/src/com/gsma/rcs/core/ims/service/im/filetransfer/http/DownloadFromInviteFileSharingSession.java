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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.util.List;

import javax2.sip.header.ContactHeader;

/**
 * Terminating file transfer HTTP session starting from invitation
 * 
 * @author vfml3370
 */
public class DownloadFromInviteFileSharingSession extends TerminatingHttpFileSharingSession {

    private final Uri mIconRemoteUri;

    private static final Logger sLogger = Logger
            .getLogger(DownloadFromInviteFileSharingSession.class.getSimpleName());

    private final long mTimestampSent;

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param chatSession the chat session
     * @param fileTransferInfo the File transfer info document
     * @param fileTransferId the File transfer Id
     * @param contact the remote contact Id
     * @param displayName the display name of the remote contact
     * @param rcsSettings
     * @param messagingLog
     * @param timestamp
     * @param timestampSent
     * @param contactManager
     */
    public DownloadFromInviteFileSharingSession(InstantMessagingService imService,
            ChatSession chatSession, FileTransferHttpInfoDocument fileTransferInfo,
            String fileTransferId, ContactId contact, String displayName, RcsSettings rcsSettings,
            MessagingLog messagingLog, long timestamp, long timestampSent,
            ContactManager contactManager) {

        // @formatter:off
        super(imService,
                fileTransferInfo.getLocalMmContent(),
                fileTransferInfo.getExpiration(),
                fileTransferInfo.getFileThumbnail() == null ? null : fileTransferInfo.getFileThumbnail().getLocalMmContent(fileTransferId),
                getFileIconExpiration(fileTransferInfo.getFileThumbnail()),
                contact,
                chatSession.getContributionID(),
                fileTransferId,
                chatSession.isGroupChat(),
                fileTransferInfo.getUri(),
                rcsSettings,
                messagingLog,
                timestamp,
                getRemoteSipId(chatSession),
                contactManager);
        // @formatter:on

        mTimestampSent = timestampSent;

        setRemoteDisplayName(displayName);

        if (fileTransferInfo.getFileThumbnail() != null) {
            mIconRemoteUri = fileTransferInfo.getFileThumbnail().getUri();
        } else {
            mIconRemoteUri = null;
        }
        if (shouldBeAutoAccepted()) {
            setSessionAccepted();
        }
    }

    private static long getFileIconExpiration(FileTransferHttpThumbnail thumbnailInfo) {
        if (thumbnailInfo != null) {
            return thumbnailInfo.getExpiration();
        }
        return FileTransferData.UNKNOWN_EXPIRATION;
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
     * Download file icon from network server.<br>
     * Throws an exception if download fails.
     * 
     * @throws FileAccessException
     * @throws NetworkException
     */
    public void downloadFileIcon() throws NetworkException, FileAccessException {
        mDownloadManager.downloadThumbnail(mIconRemoteUri, getFileicon());
    }

    @Override
    public void run() {
        final boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Initiate a HTTP file transfer session as terminating");
        }
        List<ImsSessionListener> listeners = getListeners();
        ContactId contact = getRemoteContact();
        long fileExpiration = getFileExpiration();
        try {
            /* Check if session should be auto-accepted once */
            if (isSessionAccepted()) {
                if (logActivated) {
                    sLogger.debug("Received HTTP file transfer invitation marked for auto-accept");
                }

                for (ImsSessionListener listener : listeners) {
                    ((FileSharingSessionListener) listener).onSessionAutoAccepted(contact,
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
                    sLogger.debug("Received HTTP file transfer invitation marked for manual accept");
                }
                for (ImsSessionListener listener : listeners) {
                    ((FileSharingSessionListener) listener).onSessionInvited(contact, getContent(),
                            getFileicon(), getTimestamp(), mTimestampSent, fileExpiration,
                            getIconExpiration());
                }
                Uri downloadServerAddress = mDownloadManager.getHttpServerAddr();
                mMessagingLog.setFileDownloadAddress(getFileTransferId(), downloadServerAddress);
                if (mRemoteInstanceId != null) {
                    mMessagingLog.setRemoteSipId(getFileTransferId(), mRemoteInstanceId);
                }
                /* Compute the delay before file validity expiration */
                long delay = fileExpiration - System.currentTimeMillis();
                if (delay <= 0) {
                    if (logActivated) {
                        sLogger.debug("File no more available on server: transfer rejected on timeout");
                    }
                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.onSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_TIMEOUT);
                    }
                    return;

                }
                if (logActivated) {
                    sLogger.debug("Accept manually file transfer tiemout=".concat(Long
                            .toString(delay)));
                }
                /* Wait invitation answer */
                InvitationStatus answer = waitInvitationAnswer(delay);
                switch (answer) {
                    case INVITATION_REJECTED_DECLINE:
                        /* Intentional fall through */
                    case INVITATION_REJECTED_BUSY_HERE:
                        if (logActivated) {
                            sLogger.debug("Transfer has been rejected by user");
                        }
                        /*
                         * If session is rejected by user, session cannot be rejected at SIP level
                         * (already accepted200OK)
                         */
                        removeSession();
                        for (ImsSessionListener listener : listeners) {
                            listener.onSessionRejected(contact,
                                    TerminationReason.TERMINATION_BY_USER);
                        }
                        return;

                    case INVITATION_TIMEOUT:
                        if (logActivated) {
                            sLogger.debug("Transfer has been rejected on timeout");
                        }
                        removeSession();

                        for (ImsSessionListener listener : listeners) {
                            listener.onSessionRejected(contact,
                                    TerminationReason.TERMINATION_BY_TIMEOUT);
                        }
                        return;

                    case INVITATION_CANCELED:
                        if (logActivated) {
                            sLogger.debug("Http transfer has been rejected by remote.");
                        }
                        removeSession();

                        for (ImsSessionListener listener : listeners) {
                            listener.onSessionRejected(contact,
                                    TerminationReason.TERMINATION_BY_REMOTE);
                        }
                        return;

                    case INVITATION_ACCEPTED:
                        setSessionAccepted();
                        for (ImsSessionListener listener : listeners) {
                            ((FileSharingSessionListener) listener).onSessionAccepting(contact);
                        }
                        break;

                    case INVITATION_REJECTED_BY_SYSTEM:
                        if (logActivated) {
                            sLogger.debug("Http transfer has aborted by system");
                        }
                        removeSession();
                        return;

                    default:
                        throw new IllegalArgumentException(new StringBuilder(
                                "Unknown invitation answer in run; answer=").append(answer)
                                .toString());

                }
            }
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error(
                    new StringBuilder("Download failed for a file sessionId : ")
                            .append(getSessionID()).append(" with transferId : ")
                            .append(getFileTransferId()).toString(), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
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
