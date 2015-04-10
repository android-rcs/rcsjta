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

package com.gsma.rcs.core.ims.service.richcall.image;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingError;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax2.sip.header.ContentDispositionHeader;
import javax2.sip.header.ContentLengthHeader;
import javax2.sip.header.ContentTypeHeader;

/**
 * Originating content sharing session (transfer)
 * 
 * @author jexa7410
 */
public class OriginatingImageTransferSession extends ImageTransferSession implements
        MsrpEventListener {
    /**
     * Boundary tag
     */
    private final static String BOUNDARY_TAG = "boundary1";

    /**
     * MSRP manager
     */
    private MsrpManager msrpMgr;

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(OriginatingImageTransferSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param content Content to be shared
     * @param contact Remote contact Id
     * @param thumbnail Thumbnail content option
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public OriginatingImageTransferSession(ImsService parent, MmContent content, ContactId contact,
            MmContent thumbnail, RcsSettings rcsSettings, long timestamp,
            ContactsManager contactManager) {
        super(parent, content, contact, thumbnail, rcsSettings, timestamp, contactManager);

        // Create dialog path
        createOriginatingDialogPath();
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (logger.isActivated()) {
                logger.info("Initiate a new sharing session as originating");
            }

            // Set setup mode
            String localSetup = createMobileToMobileSetupOffer();
            if (logger.isActivated()) {
                logger.debug("Local setup attribute is " + localSetup);
            }

            // Set local port
            int localMsrpPort;
            if ("active".equals(localSetup)) {
                localMsrpPort = 9; // See RFC4145, Page 4
            } else {
                localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort(mRcsSettings);
            }

            // Create the MSRP manager
            String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface()
                    .getNetworkAccess().getIpAddress();
            msrpMgr = new MsrpManager(localIpAddress, localMsrpPort, getImsService(), mRcsSettings);
            if (getImsService().getImsModule().isConnectedToWifiAccess()) {
                msrpMgr.setSecured(mRcsSettings.isSecureMsrpOverWifi());
            }

            // Build SDP part
            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String encoding = getContent().getEncoding();
            long maxSize = ImageTransferSession.getMaxImageSharingSize(mRcsSettings);
            // Set File-selector attribute
            String selector = getFileSelectorAttribute();
            String sdp = SdpUtils.buildFileSDP(ipAddress, localMsrpPort,
                    msrpMgr.getLocalSocketProtocol(), encoding, getFileTransferId(), selector,
                    "render", localSetup, msrpMgr.getLocalMsrpPath(), SdpUtils.DIRECTION_SENDONLY,
                    maxSize);

            // Set File-location attribute
            Uri location = getFileLocationAttribute();
            if (location != null) {
                sdp += "a=file-location:" + location.toString() + SipUtils.CRLF;
            }

            if (getThumbnail() != null) {
                sdp += "a=file-icon:cid:image@joyn.com" + SipUtils.CRLF;

                // Encode the thumbnail file
                String imageEncoded = Base64.encodeBase64ToString(getThumbnail().getData());

                String multipart = new StringBuilder(Multipart.BOUNDARY_DELIMITER)
                        .append(BOUNDARY_TAG).append(SipUtils.CRLF).append(ContentTypeHeader.NAME)
                        .append(": application/sdp").append(SipUtils.CRLF)
                        .append(ContentLengthHeader.NAME).append(": ")
                        .append(sdp.getBytes(UTF8).length).append(SipUtils.CRLF)
                        .append(SipUtils.CRLF).append(sdp).append(SipUtils.CRLF)
                        .append(Multipart.BOUNDARY_DELIMITER).append(BOUNDARY_TAG)
                        .append(SipUtils.CRLF).append(ContentTypeHeader.NAME).append(": ")
                        .append(getContent().getEncoding()).append(SipUtils.CRLF)
                        .append(SipUtils.HEADER_CONTENT_TRANSFER_ENCODING).append(": base64")
                        .append(SipUtils.CRLF).append(SipUtils.HEADER_CONTENT_ID)
                        .append(": <image@joyn.com>").append(SipUtils.CRLF)
                        .append(ContentLengthHeader.NAME).append(": ")
                        .append(imageEncoded.length()).append(SipUtils.CRLF)
                        .append(ContentDispositionHeader.NAME).append(": icon")
                        .append(SipUtils.CRLF).append(SipUtils.CRLF).append(imageEncoded)
                        .append(SipUtils.CRLF).append(Multipart.BOUNDARY_DELIMITER)
                        .append(BOUNDARY_TAG).append(Multipart.BOUNDARY_DELIMITER).toString();

                // Set the local SDP part in the dialog path
                getDialogPath().setLocalContent(multipart);
            } else {
                // Set the local SDP part in the dialog path
                getDialogPath().setLocalContent(sdp);
            }

            // Create an INVITE request
            if (logger.isActivated()) {
                logger.info("Send INVITE");
            }
            SipRequest invite = createInvite();

            // Set the Authorization header
            getAuthenticationAgent().setAuthorizationHeader(invite);

            // Set initial request in the dialog path
            getDialogPath().setInvite(invite);

            // Send INVITE request
            sendInvite(invite);
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new ContentSharingError(ContentSharingError.UNEXPECTED_EXCEPTION,
                    e.getMessage()));
        }

        if (logger.isActivated()) {
            logger.debug("End of thread");
        }
    }

    /**
     * Prepare media session
     * 
     * @throws Exception
     */
    public void prepareMediaSession() throws Exception {
        // Changed by Deutsche Telekom
        // Get the remote SDP part
        byte[] sdp = getDialogPath().getRemoteContent().getBytes(UTF8);

        // Changed by Deutsche Telekom
        // Create the MSRP session
        MsrpSession session = msrpMgr.createMsrpSession(sdp, this);

        session.setFailureReportOption(true);
        session.setSuccessReportOption(false);
        // Changed by Deutsche Telekom
        // Do not use right now the mapping to do not increase memory and cpu consumption
        session.setMapMsgIdFromTransationId(false);
    }

    /**
     * Start media session
     * 
     * @throws Exception
     */
    public void startMediaSession() throws Exception {
        // Open the MSRP session
        msrpMgr.openMsrpSession();

        new Thread() {
            public void run() {
                try {
                    // Start sending data chunks
                    byte[] data = getContent().getData();
                    InputStream stream;
                    if (data == null) {
                        // Load data from Uri
                        stream = AndroidFactory.getApplicationContext().getContentResolver()
                                .openInputStream(getContent().getUri());
                    } else {
                        // Load data from memory
                        stream = new ByteArrayInputStream(data);
                    }

                    msrpMgr.sendChunks(stream, getFileTransferId(), getContent().getEncoding(),
                            getContent().getSize(), TypeMsrpChunk.FileSharing);
                } catch (Exception e) {
                    // Unexpected error
                    if (logger.isActivated()) {
                        logger.error("Session initiation has failed", e);
                    }
                    handleError(new ImsServiceError(ImsServiceError.UNEXPECTED_EXCEPTION,
                            e.getMessage()));
                }
            }
        }.start();
    }

    /**
     * Close media session
     */
    public void closeMediaSession() {
        // Close the MSRP session
        if (msrpMgr != null) {
            msrpMgr.closeSession();
        }
        if (logger.isActivated()) {
            logger.debug("MSRP session has been closed");
        }
    }

    /**
     * Data has been transfered
     * 
     * @param msgId Message ID
     */
    public void msrpDataTransfered(String msgId) {
        if (logger.isActivated()) {
            logger.info("Data transfered");
        }

        // Image has been transfered
        imageTransfered();

        // Close the media session
        closeMediaSession();

        // Terminate session
        terminateSession(TerminationReason.TERMINATION_BY_USER);

        // Remove the current session
        removeSession();

        ContactId contact = getRemoteContact();
        Uri image = getContent().getUri();
        for (ImsSessionListener listener : getListeners()) {
            ((ImageTransferSessionListener) listener).handleContentTransfered(contact, image);
        }
    }

    /**
     * Data transfer has been received
     * 
     * @param msgId Message ID
     * @param data Received data
     * @param mimeType Data mime-type
     */
    public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
        // Not used in originating side
    }

    /**
     * Data transfer in progress
     * 
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     */
    public void msrpTransferProgress(long currentSize, long totalSize) {
        ContactId contact = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            ((ImageTransferSessionListener) listener).handleSharingProgress(contact, currentSize,
                    totalSize);
        }
    }

    /**
     * Data transfer in progress
     * 
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     * @param data received data chunk
     * @return always false TODO
     */
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        // Not used in originating side
        return false;
    }

    /**
     * Data transfer has been aborted
     */
    public void msrpTransferAborted() {
        if (logger.isActivated()) {
            logger.info("Data transfer aborted");
        }
    }

    /**
     * Data transfer error
     * 
     * @param msgId Message ID
     * @param error Error code
     * @param typeMsrpChunk Type of MSRP chunk
     */
    public void msrpTransferError(String msgId, String error, TypeMsrpChunk typeMsrpChunk) {
        if (isSessionInterrupted() || isInterrupted() || getDialogPath().isSessionTerminated()) {
            return;
        }

        if (logger.isActivated()) {
            logger.info("Data transfer error " + error);
        }

        // Terminate session
        terminateSession(TerminationReason.TERMINATION_BY_SYSTEM);

        // Close the media session
        closeMediaSession();

        ContactId contact = getRemoteContact();
        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(contact);

        // Remove the current session
        removeSession();

        // Notify listeners
        if (!isSessionInterrupted() && !isSessionTerminatedByRemote()) {
            for (ImsSessionListener listener : getListeners()) {
                ((ImageTransferSessionListener) listener).handleSharingError(contact,
                        new ContentSharingError(ContentSharingError.MEDIA_TRANSFER_FAILED, error));
            }
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return false;
    }

    @Override
    public void handle180Ringing(SipResponse response) {
        if (logger.isActivated()) {
            logger.debug("handle180Ringing");
        }
        ContactId contact = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            ((ImageTransferSessionListener) listener).handle180Ringing(contact);
        }
    }
}
