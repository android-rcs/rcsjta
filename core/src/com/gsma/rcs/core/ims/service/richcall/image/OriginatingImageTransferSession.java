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
import com.gsma.rcs.core.ims.protocol.msrp.MsrpException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingError;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.api.ServerApiUtils;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax2.sip.InvalidArgumentException;
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
    private final Logger mLogger = Logger.getLogger(getClass().getSimpleName());

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
     * @param serverApiUtils
     */
    public OriginatingImageTransferSession(ImsService parent, MmContent content, ContactId contact,
            MmContent thumbnail, RcsSettings rcsSettings, long timestamp,
            ContactManager contactManager, ServerApiUtils serverApiUtils) {
        super(parent, content, contact, thumbnail, rcsSettings, timestamp, contactManager,
                serverApiUtils);

        // Create dialog path
        createOriginatingDialogPath();
    }

    private byte[] getFileData(Uri file, int size) throws IOException {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = (FileInputStream) AndroidFactory.getApplicationContext()
                    .getContentResolver().openInputStream(file);
            byte[] data = new byte[size];
            if (size != fileInputStream.read(data, 0, size)) {
                throw new IOException("Unable to retrive data from ".concat(file.toString()));
            }
            return data;
        } finally {
            CloseableUtils.close(fileInputStream);
        }
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (mLogger.isActivated()) {
                mLogger.info("Initiate a new sharing session as originating");
            }

            // Set setup mode
            String localSetup = createMobileToMobileSetupOffer();
            if (mLogger.isActivated()) {
                mLogger.debug("Local setup attribute is " + localSetup);
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
            StringBuilder sdp = new StringBuilder(SdpUtils.buildFileSDP(ipAddress, localMsrpPort,
                    msrpMgr.getLocalSocketProtocol(), encoding, getFileTransferId(), selector,
                    "render", localSetup, msrpMgr.getLocalMsrpPath(), SdpUtils.DIRECTION_SENDONLY,
                    maxSize));

            // Set File-location attribute
            Uri location = getFileLocationAttribute();
            if (location != null) {
                sdp.append("a=file-location:").append(location.toString()).append(SipUtils.CRLF);
            }

            MmContent fileIcon = getThumbnail();
            if (fileIcon == null) {
                /* Set the local SDP part in the dialog path */
                getDialogPath().setLocalContent(sdp.toString());
            } else {
                Capabilities remoteCapabilities = mContactManager
                        .getContactCapabilities(getRemoteContact());
                boolean fileIconSupported = remoteCapabilities != null
                        && remoteCapabilities.isFileTransferThumbnailSupported();
                if (fileIconSupported) {
                    sdp.append("a=file-icon:cid:image@joyn.com").append(SipUtils.CRLF);

                    // Encode the thumbnail file
                    String imageEncoded = Base64.encodeBase64ToString(getFileData(
                            fileIcon.getUri(), (int) fileIcon.getSize()));
                    String sdpContent = sdp.toString();
                    String multipart = new StringBuilder(Multipart.BOUNDARY_DELIMITER)
                            .append(BOUNDARY_TAG).append(SipUtils.CRLF)
                            .append(ContentTypeHeader.NAME).append(": application/sdp")
                            .append(SipUtils.CRLF).append(ContentLengthHeader.NAME).append(": ")
                            .append(sdpContent.getBytes(UTF8).length).append(SipUtils.CRLF)
                            .append(SipUtils.CRLF).append(sdpContent).append(SipUtils.CRLF)
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
                    getDialogPath().setLocalContent(sdp.toString());
                }
            }
            // Create an INVITE request
            if (mLogger.isActivated()) {
                mLogger.info("Send INVITE");
            }
            SipRequest invite = createInvite();

            // Set the Authorization header
            getAuthenticationAgent().setAuthorizationHeader(invite);

            // Set initial request in the dialog path
            getDialogPath().setInvite(invite);

            // Send INVITE request
            sendInvite(invite);
        } catch (SipException e) {
            mLogger.error("Failed to send invite!", e);
            handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED, e));
        } catch (InvalidArgumentException e) {
            mLogger.error("Failed to send invite!", e);
            handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED, e));
        } catch (IOException e) {
            if (mLogger.isActivated()) {
                mLogger.debug("Failed to initiate a new image transfer session with sharingId "
                        .concat(getFileTransferId()));
            }
            handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED, e));
        } catch (RuntimeException e) {
            /**
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            mLogger.error("Failed to initiate a new sharing session as originating!", e);
            handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED, e));
        }

        if (mLogger.isActivated()) {
            mLogger.debug("End of thread");
        }
    }

    /**
     * Prepare media session
     * 
     * @throws MsrpException
     */
    public void prepareMediaSession() throws MsrpException {
        // Changed by Deutsche Telekom
        /* Get the remote SDP part */
        byte[] sdp = getDialogPath().getRemoteContent().getBytes(UTF8);

        // Changed by Deutsche Telekom
        MsrpSession session = msrpMgr.createMsrpSession(sdp, this);

        session.setFailureReportOption(true);
        session.setSuccessReportOption(false);
        // Changed by Deutsche Telekom
        /* Do not use right now the mapping to do not increase memory and cpu consumption */
        session.setMapMsgIdFromTransationId(false);
    }

    /**
     * Open media session
     * 
     * @throws IOException
     */
    public void openMediaSession() throws IOException {
        msrpMgr.openMsrpSession();
    }

    /**
     * Start media transfer
     * 
     * @throws IOException
     */
    public void startMediaTransfer() throws IOException {
        /* Start sending data chunks */
        InputStream stream = AndroidFactory.getApplicationContext().getContentResolver()
                .openInputStream(getContent().getUri());
        msrpMgr.sendChunks(stream, getFileTransferId(), getContent().getEncoding(), getContent()
                .getSize(), TypeMsrpChunk.FileSharing);
    }

    /**
     * Close media session
     */
    public void closeMediaSession() {
        // Close the MSRP session
        if (msrpMgr != null) {
            msrpMgr.closeSession();
        }
        if (mLogger.isActivated()) {
            mLogger.debug("MSRP session has been closed");
        }
    }

    /**
     * Data has been transfered
     * 
     * @param msgId Message ID
     */
    public void msrpDataTransfered(String msgId) {
        if (mLogger.isActivated()) {
            mLogger.info("Data transfered");
        }

        // Image has been transfered
        imageTransfered();

        // Close the media session
        closeMediaSession();

        closeSession(TerminationReason.TERMINATION_BY_USER);

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
        if (mLogger.isActivated()) {
            mLogger.info("Data transfer aborted");
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

        if (mLogger.isActivated()) {
            mLogger.info("Data transfer error " + error);
        }

        closeSession(TerminationReason.TERMINATION_BY_SYSTEM);

        // Close the media session
        closeMediaSession();

        ContactId contact = getRemoteContact();
        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(contact);

        // Remove the current session
        removeSession();

        for (ImsSessionListener listener : getListeners()) {
            ((ImageTransferSessionListener) listener).handleSharingError(contact,
                    new ContentSharingError(ContentSharingError.MEDIA_TRANSFER_FAILED));
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return false;
    }

    @Override
    public void handle180Ringing(SipResponse response) {
        if (mLogger.isActivated()) {
            mLogger.debug("handle180Ringing");
        }
        ContactId contact = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            ((ImageTransferSessionListener) listener).handle180Ringing(contact);
        }
    }
}
