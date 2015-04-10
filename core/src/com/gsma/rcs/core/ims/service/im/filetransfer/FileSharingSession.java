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

package com.gsma.rcs.core.ims.service.im.filetransfer;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.StorageUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract file sharing session
 * 
 * @author jexa7410
 */
public abstract class FileSharingSession extends ImsServiceSession {

    /**
     * Contribution ID
     */
    private String mContributionId;

    /**
     * Content to be shared
     */
    private MmContent mContent;

    /**
     * File transfered
     */
    private boolean mFileTransfered = false;

    /**
     * Participants
     */
    protected Map<ContactId, ParticipantStatus> mParticipants = new HashMap<ContactId, ParticipantStatus>();

    /**
     * Fileicon
     */
    private MmContent mFileIcon;

    /**
     * File transfer paused
     */
    private boolean mFileTransferPaused = false;

    /**
     * File transfer Id
     */
    private String mFiletransferId;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger
            .getLogger(FileSharingSession.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param content Content to be shared
     * @param contact Remote contactId
     * @param remoteUri the remote URI
     * @param fileIcon File icon
     * @param filetransferId
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public FileSharingSession(ImsService parent, MmContent content, ContactId contact,
            String remoteUri, MmContent fileIcon, String filetransferId, RcsSettings rcsSettings,
            long timestamp, ContactsManager contactManager) {
        super(parent, contact, remoteUri, rcsSettings, timestamp, contactManager);

        mContent = content;
        mFileIcon = fileIcon;
        mFiletransferId = filetransferId;
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
        return mFiletransferId;
    }

    /**
     * File has been transfered
     */
    public void fileTransfered() {
        mFileTransfered = true;

    }

    /**
     * Is file transfered
     * 
     * @return Boolean
     */
    public boolean isFileTransfered() {
        return mFileTransfered;
    }

    /**
     * File has been paused
     */
    public void fileTransferPaused() {
        mFileTransferPaused = true;
    }

    /**
     * File is resuming
     */
    public void fileTransferResumed() {
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
     * @param rcsSettings
     * @return Error or null if file capacity is acceptable
     */
    public static FileSharingError isFileCapacityAcceptable(long fileSize, RcsSettings rcsSettings) {
        long maxFileSharingSize = rcsSettings.getMaxFileTransferSize();
        boolean fileIsToBig = (maxFileSharingSize > 0) ? fileSize > maxFileSharingSize : false;
        boolean storageIsTooSmall = (StorageUtils.getExternalStorageFreeSpace() > 0) ? fileSize > StorageUtils
                .getExternalStorageFreeSpace() : false;
        if (fileIsToBig) {
            if (sLogger.isActivated()) {
                sLogger.warn("File is too big, reject the file transfer");
            }
            return new FileSharingError(FileSharingError.MEDIA_SIZE_TOO_BIG);
        } else {
            if (storageIsTooSmall) {
                if (sLogger.isActivated()) {
                    sLogger.warn("Not enough storage capacity, reject the file transfer");
                }
                return new FileSharingError(FileSharingError.NOT_ENOUGH_STORAGE_SPACE);
            }
        }
        return null;
    }

    @Override
    public void receiveBye(SipRequest bye) {
        super.receiveBye(bye);

        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getRemoteContact());
    }

    @Override
    public void receiveCancel(SipRequest cancel) {
        super.receiveCancel(cancel);

        // Request capabilities to the remote
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

}
