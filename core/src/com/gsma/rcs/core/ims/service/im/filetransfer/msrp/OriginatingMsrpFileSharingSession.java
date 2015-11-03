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

package com.gsma.rcs.core.ims.service.im.filetransfer.msrp;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ContributionIdGenerator;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.core.ims.service.im.filetransfer.ImsFileSharingSession;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import javax2.sip.InvalidArgumentException;
import javax2.sip.header.ContentDispositionHeader;
import javax2.sip.header.ContentLengthHeader;
import javax2.sip.header.ContentTypeHeader;

/**
 * Originating file transfer session
 * 
 * @author jexa7410
 */
public class OriginatingMsrpFileSharingSession extends ImsFileSharingSession implements
        MsrpEventListener {
    /**
     * Boundary tag
     */
    private final static String BOUNDARY_TAG = "boundary1";

    private MsrpManager msrpMgr;

    private final InstantMessagingService mImService;

    private static final Logger sLogger = Logger.getLogger(OriginatingMsrpFileSharingSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param fileTransferId File transfer Id
     * @param imService InstantMessagingService
     * @param content Content to be shared
     * @param contact Remote contact identifier
     * @param fileIcon Content of file icon
     * @param rcsSettings The RCS settings accessor
     * @param timestamp Local timestamp for the session
     * @param contactManager The contact manager accessor
     */
    public OriginatingMsrpFileSharingSession(InstantMessagingService imService,
            String fileTransferId, MmContent content, ContactId contact, MmContent fileIcon,
            RcsSettings rcsSettings, long timestamp, ContactManager contactManager) {
        super(imService, content, contact, fileIcon, fileTransferId, rcsSettings, timestamp,
                contactManager);

        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("OriginatingFileSharingSession contact=")
                    .append(contact).append(" filename=").append(content.getName()).toString());
        }
        mImService = imService;
        // Create dialog path
        createOriginatingDialogPath();

        // Set contribution ID
        String id = ContributionIdGenerator.getContributionId(getDialogPath().getCallId());
        setContributionID(id);
    }

    private byte[] getFileData(Uri file, int size) throws NetworkException {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = (FileInputStream) AndroidFactory.getApplicationContext()
                    .getContentResolver().openInputStream(file);
            byte[] data = new byte[size];
            if (size != fileInputStream.read(data, 0, size)) {
                throw new NetworkException(new StringBuilder("Unable to retrive data from ")
                        .append(file).toString());
            }
            return data;

        } catch (IOException e) {
            throw new NetworkException(new StringBuilder("Failed to get file data for uri : ")
                    .append(file).toString(), e);

        } finally {
            CloseableUtils.tryToClose(fileInputStream);
        }
    }

    @Override
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Initiate a file transfer session as originating");
            }

            /* Set setup mode */
            String localSetup = createSetupOffer();
            if (sLogger.isActivated()) {
                sLogger.debug("Local setup attribute is ".concat(localSetup));
            }

            /* Set local port */
            int localMsrpPort;
            if ("active".equals(localSetup)) {
                localMsrpPort = 9; /* See RFC4145, Page 4 */
            } else {
                localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort(mRcsSettings);
            }

            /* Create the MSRP manager */
            String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface()
                    .getNetworkAccess().getIpAddress();
            msrpMgr = new MsrpManager(localIpAddress, localMsrpPort, getImsService(), mRcsSettings);
            if (getImsService().getImsModule().isConnectedToWifiAccess()) {
                msrpMgr.setSecured(mRcsSettings.isSecureMsrpOverWifi());
            }

            /* Build SDP part */
            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String encoding = getContent().getEncoding();
            long maxSize = mRcsSettings.getMaxFileTransferSize();
            /* Set File-selector attribute */
            String selector = getFileSelectorAttribute();
            StringBuilder sdp = new StringBuilder(SdpUtils.buildFileSDP(ipAddress, localMsrpPort,
                    msrpMgr.getLocalSocketProtocol(), encoding, getFileTransferIdAttribute(),
                    selector, "attachment", localSetup, msrpMgr.getLocalMsrpPath(),
                    SdpUtils.DIRECTION_SENDONLY, maxSize));

            /* Set File-location attribute */
            Uri location = getFileLocationAttribute();
            if (location != null) {
                sdp.append("a=file-location:").append(location.toString()).append(SipUtils.CRLF);
            }

            MmContent fileIcon = getFileicon();
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

                    /* Encode the file icon file */
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
                            .append(fileIcon.getEncoding()).append(SipUtils.CRLF)
                            .append(SipUtils.HEADER_CONTENT_TRANSFER_ENCODING).append(": base64")
                            .append(SipUtils.CRLF).append(SipUtils.HEADER_CONTENT_ID)
                            .append(": <image@joyn.com>").append(SipUtils.CRLF)
                            .append(ContentLengthHeader.NAME).append(": ")
                            .append(imageEncoded.length()).append(SipUtils.CRLF)
                            .append(ContentDispositionHeader.NAME).append(": icon")
                            .append(SipUtils.CRLF).append(SipUtils.CRLF).append(imageEncoded)
                            .append(SipUtils.CRLF).append(Multipart.BOUNDARY_DELIMITER)
                            .append(BOUNDARY_TAG).append(Multipart.BOUNDARY_DELIMITER).toString();

                    /* Set the local SDP part in the dialog path */
                    getDialogPath().setLocalContent(multipart);
                } else {
                    /* Set the local SDP part in the dialog path */
                    getDialogPath().setLocalContent(sdp.toString());
                }
            }

            /* Create an INVITE request */
            if (sLogger.isActivated()) {
                sLogger.info("Send INVITE");
            }
            SipRequest invite = createInvite();

            /* Set the Authorization header */
            getAuthenticationAgent().setAuthorizationHeader(invite);

            /* Set initial request in the dialog path */
            getDialogPath().setInvite(invite);

            /* Send INVITE request */
            sendInvite(invite);

        } catch (InvalidArgumentException | ParseException e) {
            sLogger.error("Unable to set authorization header!", e);
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (FileAccessException | PayloadException e) {
            sLogger.error("Unable to set and send initial invite!", e);
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (NetworkException e) {
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to initiate a file transfer session!", e);
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));
        }
    }

    @Override
    public void prepareMediaSession() {
        // Get the remote SDP part
        byte[] sdp = getDialogPath().getRemoteContent().getBytes(UTF8);

        // Create the MSRP session
        MsrpSession session = msrpMgr.createMsrpSession(sdp, this);

        session.setFailureReportOption(true);
        session.setSuccessReportOption(false);
        // Do not use right now the mapping to do not increase memory and cpu consumption
        session.setMapMsgIdFromTransationId(false);
    }

    @Override
    public void openMediaSession() throws PayloadException, NetworkException {
        msrpMgr.openMsrpSession();
    }

    @Override
    public void startMediaTransfer() throws NetworkException, FileAccessException {
        try {
            /* Start sending data chunks */
            InputStream stream = AndroidFactory.getApplicationContext().getContentResolver()
                    .openInputStream(getContent().getUri());
            msrpMgr.sendChunks(stream, IdGenerator.generateMessageID(), getContent().getEncoding(),
                    getContent().getSize(), TypeMsrpChunk.FileSharing);

        } catch (FileNotFoundException e) {
            throw new FileAccessException("Failed to initiate media transfer!", e);

        } catch (SecurityException e) {
            sLogger.error("Session initiation has failed due to that the file is not accessible!",
                    e);
            ContactId contact = getRemoteContact();
            for (ImsSessionListener listener : getListeners()) {
                ((FileSharingSessionListener) listener).onTransferNotAllowedToSend(contact);
            }
        }
    }

    @Override
    public void msrpDataTransferred(String msgId) {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Data transferred");
            }
            long timestamp = System.currentTimeMillis();
            fileTransfered();
            closeMediaSession();
            closeSession(TerminationReason.TERMINATION_BY_USER);
            removeSession();
            ContactId contact = getRemoteContact();
            MmContent content = getContent();
            for (ImsSessionListener listener : getListeners()) {
                ((FileSharingSessionListener) listener).onFileTransferred(content, contact,
                        FileTransferData.UNKNOWN_EXPIRATION, FileTransferData.UNKNOWN_EXPIRATION,
                        FileTransferProtocol.MSRP);
            }
            mImService.receiveOneToOneFileDeliveryStatus(contact, new ImdnDocument(
                    getFileTransferId(), ImdnDocument.DISPLAY,
                    ImdnDocument.DELIVERY_STATUS_DISPLAYED, timestamp));

        } catch (PayloadException e) {
            sLogger.error(new StringBuilder("Failed to notify MSRP data transferred for msgId : ")
                    .append(msgId).toString(), e);

        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }

        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error(new StringBuilder("Failed to notify msrp data transfered for msgId : ")
                    .append(msgId).toString(), e);
        }
    }

    @Override
    public void receiveMsrpData(String msgId, byte[] data, String mimeType) {
        // Not used in originating side
    }

    @Override
    public void msrpTransferProgress(long currentSize, long totalSize) {
        ContactId contact = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            ((FileSharingSessionListener) listener).onTransferProgress(contact, currentSize,
                    totalSize);
        }
    }

    @Override
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        // Not used in originating side
        return false;
    }

    @Override
    public void msrpTransferAborted() {
        if (sLogger.isActivated()) {
            sLogger.info("Data transfer aborted");
        }
    }

    @Override
    public void closeMediaSession() {
        // Close MSRP session
        if (msrpMgr != null) {
            msrpMgr.closeSession();
        }
        if (sLogger.isActivated()) {
            sLogger.debug("MSRP session has been closed");
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return false;
    }

    @Override
    public void handle200OK(SipResponse resp) throws PayloadException, NetworkException,
            FileAccessException {
        long timestamp = System.currentTimeMillis();
        mImService.receiveOneToOneFileDeliveryStatus(getRemoteContact(), new ImdnDocument(
                getFileTransferId(), ImdnDocument.POSITIVE_DELIVERY,
                ImdnDocument.DELIVERY_STATUS_DELIVERED, timestamp));
        super.handle200OK(resp);
    }
}
