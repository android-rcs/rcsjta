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
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransferLog;

import android.net.Uri;

/**
 * Abstract IMS file transfer session
 * 
 * @author jexa7410
 */
public abstract class ImsFileSharingSession extends FileSharingSession {
    /**
     * Boundary tag
     */
    private final static String BOUNDARY_TAG = "boundary1";

    /**
     * Default SO_TIMEOUT value (in seconds)
     */
    public final static int DEFAULT_SO_TIMEOUT = 30;

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(ImsFileSharingSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param content Content of file to be shared
     * @param contact Remote contact identifier
     * @param fileIcon Content of file icon
     * @param filetransferId File transfer Id
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public ImsFileSharingSession(ImsService parent, MmContent content, ContactId contact,
            MmContent fileIcon, String filetransferId, RcsSettings rcsSettings, long timestamp,
            ContactsManager contactManager) {
        super(parent, content, contact, PhoneUtils.formatContactIdToUri(contact), fileIcon,
                filetransferId, rcsSettings, timestamp, contactManager);
    }

    @Override
    public boolean isHttpTransfer() {
        return false;
    }

    /**
     * Returns the "file-transfer-id" attribute
     * 
     * @return String
     */
    public String getFileTransferIdAttribute() {
        return Long.toString(System.currentTimeMillis());
    }

    /**
     * Returns the "file-selector" attribute
     * 
     * @return String
     */
    public String getFileSelectorAttribute() {
        return "name:\"" + getContent().getName() + "\"" + " type:" + getContent().getEncoding()
                + " size:" + getContent().getSize();
    }

    /**
     * Returns the "file-location" attribute
     * 
     * @return Uri
     */
    public Uri getFileLocationAttribute() {
        Uri file = getContent().getUri();
        if ((file != null) && file.getScheme().startsWith("http")) {
            return file;
        } else {
            return null;
        }
    }

    /**
     * Create an INVITE request
     * 
     * @return the INVITE request
     * @throws SipException
     */
    public SipRequest createInvite() throws SipException {
        SipRequest invite;
        if (getFileicon() != null) {
            invite = SipMessageFactory.createMultipartInvite(getDialogPath(),
                    InstantMessagingService.FT_FEATURE_TAGS, getDialogPath().getLocalContent(),
                    BOUNDARY_TAG);
        } else {
            invite = SipMessageFactory.createInvite(getDialogPath(),
                    InstantMessagingService.FT_FEATURE_TAGS, getDialogPath().getLocalContent());
        }

        // Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());

        return invite;
    }

    /**
     * Handle error
     * 
     * @param error Error
     */
    public void handleError(ImsServiceError error) {
        if (isSessionInterrupted()) {
            return;
        }

        // Error
        if (logger.isActivated()) {
            logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
        }

        // Close media session
        closeMediaSession();

        // Remove the current session
        removeSession();

        ContactId contact = getRemoteContact();
        for (int j = 0; j < getListeners().size(); j++) {
            ((FileSharingSessionListener) getListeners().get(j)).handleTransferError(
                    new FileSharingError(error), contact);
        }
    }

    /**
     * Data transfer error
     * 
     * @param msgId Message ID
     * @param error Error code
     * @param typeMsrpChunk
     */
    public void msrpTransferError(String msgId, String error,
            MsrpSession.TypeMsrpChunk typeMsrpChunk) {
        if (isSessionInterrupted() || getDialogPath().isSessionTerminated()) {
            return;
        }
        boolean logActivated = logger.isActivated();

        if (logActivated) {
            logger.info("Data transfer error " + error);
        }

        try {
            // Terminate session
            terminateSession(ImsServiceSession.TerminationReason.TERMINATION_BY_SYSTEM);

            // Close the media session
            closeMediaSession();
        } catch (Exception e) {
            if (logActivated) {
                logger.error("Can't close correctly the file transfer session", e);
            }
        }

        // Request capabilities
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getRemoteContact());

        // Remove the current session
        removeSession();

        // Notify listeners
        if (!isSessionInterrupted() && !isSessionTerminatedByRemote()) {
            ContactId contact = getRemoteContact();
            for (ImsSessionListener listener : getListeners()) {
                ((FileSharingSessionListener) listener).handleTransferError(new FileSharingError(
                        FileSharingError.MEDIA_TRANSFER_FAILED, error), contact);
            }
        }
    }

    @Override
    public long getFileExpiration() {
        return FileTransferLog.UNKNOWN_EXPIRATION;
    }

    @Override
    public long getIconExpiration() {
        return FileTransferLog.UNKNOWN_EXPIRATION;
    }

}
