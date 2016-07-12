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
 * http://www.apache.org/licenses/LICENSE-2.0
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

package com.gsma.rcs.core.ims.service.im.filetransfer;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.StorageUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract file sharing session
 *
 * @author jexa7410
 */
public abstract class FileSharingSession extends ImsServiceSession {
    /**
     * File render disposition which means the file is player automatically
     */
    public final static String FILE_DISPOSITION_RENDER = "render";

    /**
     * File attachment disposition which means the file should be opened manually
     */
    public final static String FILE_DISPOSITION_ATTACH = "attachment";

    private String mContributionId;

    /**
     * Content to be shared
     */
    private MmContent mContent;

    private boolean mFileTransferred = false;

    protected final Map<ContactId, ParticipantStatus> mParticipants;

    private final MmContent mFileIcon;

    private boolean mFileTransferPaused = false;

    private final String mFileTransferId;

    protected final ImdnManager mImdnManager;

    private static final Logger sLogger = Logger.getLogger(FileSharingSession.class.getName());

    /**
     * Constructor
     *
     * @param imService InstantMessagingService
     * @param content Content to be shared
     * @param contact Remote contactId
     * @param remoteContact the remote contact URI
     * @param fileIcon File icon
     * @param fileTransferId File transfer identifier
     * @param rcsSettings RCS settings accessor
     * @param timestamp Local timestamp for the session
     * @param contactManager Contact manager accessor
     */
    public FileSharingSession(InstantMessagingService imService, MmContent content,
            ContactId contact, Uri remoteContact, MmContent fileIcon, String fileTransferId,
            RcsSettings rcsSettings, long timestamp, ContactManager contactManager) {
        super(imService, contact, remoteContact, rcsSettings, timestamp, contactManager);
        mContent = content;
        mFileIcon = fileIcon;
        mFileTransferId = fileTransferId;
        mImdnManager = imService.getImdnManager();
        mParticipants = new HashMap<>();
    }

    /**
     * Check if the file sharing session is a HTTP transfer
     *
     * @return {@code true} if HTTP transfer, otherwise {@code false}
     */
    public abstract boolean isHttpTransfer();

    /**
     * Return the contribution ID
     *
     * @return Contribution ID
     */
    public String getContributionID() {
        return mContributionId;
    }

    /**
     * Set the contribution ID
     *
     * @param id Contribution ID
     */
    public void setContributionID(String id) {
        mContributionId = id;
    }

    /**
     * Returns the content
     *
     * @return Content
     */
    public MmContent getContent() {
        return mContent;
    }

    /**
     * Returns the list of participants involved in the transfer
     *
     * @return List of participants
     */
    public Map<ContactId, ParticipantStatus> getParticipants() {
        return mParticipants;
    }

    /**
     * Set the content
     *
     * @param content Content
     */
    public void setContent(MmContent content) {
        mContent = content;
    }

    /**
     * Returns the unique id for file transfer
     *
     * @return filetransferId String
     */
    public String getFileTransferId() {
        return mFileTransferId;
    }

    /**
     * Set file as being transferred
     */
    public void setFileTransferred() {
        mFileTransferred = true;
    }

    /**
     * Is file transferred
     *
     * @return Boolean
     */
    public boolean isFileTransferred() {
        return mFileTransferred;
    }

    /**
     * File has been paused
     */
    public void setFileTransferPaused() {
        mFileTransferPaused = true;
    }

    /**
     * File is resuming
     */
    public void setFileTransferResumed() {
        mFileTransferPaused = false;
    }

    /**
     * Is file transfer paused
     *
     * @return fileTransferPaused
     */
    public boolean isFileTransferPaused() {
        return mFileTransferPaused;
    }

    /**
     * Returns the fileIcon content
     *
     * @return Fileicon
     */
    public MmContent getFileicon() {
        return mFileIcon;
    }

    /**
     * Check if file capacity is acceptable
     *
     * @param fileSize File size in bytes
     * @param rcsSettings RCS settings accessor
     * @return Error or null if file capacity is acceptable
     */
    public static FileSharingError isFileCapacityAcceptable(long fileSize, RcsSettings rcsSettings) {
        long maxFileSharingSize = rcsSettings.getMaxFileTransferSize();
        boolean fileIsToBig = (maxFileSharingSize > 0) && fileSize > maxFileSharingSize;
        boolean storageIsTooSmall = (StorageUtils.getExternalStorageFreeSpace() > 0)
                && fileSize > StorageUtils.getExternalStorageFreeSpace();
        if (fileIsToBig) {
            if (sLogger.isActivated()) {
                sLogger.warn("File is too big, reject the file transfer");
            }
            return new FileSharingError(FileSharingError.MEDIA_SIZE_TOO_BIG);
        }
        if (storageIsTooSmall) {
            if (sLogger.isActivated()) {
                sLogger.warn("Not enough storage capacity, reject the file transfer");
            }
            return new FileSharingError(FileSharingError.NOT_ENOUGH_STORAGE_SPACE);
        }
        return null;
    }

    /**
     * Session inactivity event
     */
    public void handleInactivityEvent() {
        /* Not need in this class */
    }

    @Override
    public void receiveCancel(SipRequest cancel) throws NetworkException, PayloadException {
        super.receiveCancel(cancel);
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getRemoteContact());
    }

    @Override
    public void startSession() {
        getImsService().getImsModule().getInstantMessagingService().addSession(this);
        start();
    }

    @Override
    public void removeSession() {
        getImsService().getImsModule().getInstantMessagingService().removeSession(this);
    }

    /**
     * Returns the time when the file on the content server is no longer valid to download.
     *
     * @return time
     */
    public abstract long getFileExpiration();

    /**
     * Returns the time when the file icon on the content server is no longer valid to download.
     *
     * @return time
     */
    public abstract long getIconExpiration();

    /**
     * Returns the file-disposition
     *
     * @return string for payload insertion.
     */
    public String getFileDisposition() {
        return getContent().isPlayable() ? FileSharingSession.FILE_DISPOSITION_RENDER
                : FileSharingSession.FILE_DISPOSITION_ATTACH;
    }

    /**
     * Convert Disposition to string for payload insertion.
     *
     * @param disposition the disposition
     * @return the string for payload insertion.
     */
    /* package private */static String DispositionToString(FileTransfer.Disposition disposition) {
        return FileTransfer.Disposition.ATTACH == disposition ? FILE_DISPOSITION_ATTACH
                : FILE_DISPOSITION_RENDER;
    }
}
