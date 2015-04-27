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

import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpConstants;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingError;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

/**
 * Terminating content sharing session (transfer)
 * 
 * @author jexa7410
 */
public class TerminatingImageTransferSession extends ImageTransferSession implements
        MsrpEventListener {
    /**
     * MSRP manager
     */
    private MsrpManager msrpMgr;

    /**
     * The logger
     */
    private final Logger mLogger = Logger.getLogger(getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param invite Initial INVITE request
     * @param contact Contact ID
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public TerminatingImageTransferSession(ImsService parent, SipRequest invite, ContactId contact,
            RcsSettings rcsSettings, long timestamp, ContactManager contactManager) {
        super(parent, ContentManager.createMmContentFromSdp(invite, rcsSettings), contact,
                FileTransferUtils.extractFileIcon(invite, rcsSettings), rcsSettings, timestamp,
                contactManager);

        // Create dialog path
        createTerminatingDialogPath(invite);
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (mLogger.isActivated()) {
                mLogger.info("Initiate a new sharing session as terminating");
            }

            send180Ringing(getDialogPath().getInvite(), getDialogPath().getLocalTag());

            // Check if the MIME type is supported
            if (getContent() == null) {
                if (mLogger.isActivated()) {
                    mLogger.debug("MIME type is not supported");
                }

                // Send a 415 Unsupported media type response
                send415Error(getDialogPath().getInvite());

                // Unsupported media type
                handleError(new ContentSharingError(ContentSharingError.UNSUPPORTED_MEDIA_TYPE));
                return;
            }

            Collection<ImsSessionListener> listeners = getListeners();
            ContactId contact = getRemoteContact();
            MmContent content = getContent();
            long timestamp = getTimestamp();
            for (ImsSessionListener listener : listeners) {
                ((ImageTransferSessionListener) listener).handleSessionInvited(contact, content,
                        timestamp);
            }

            InvitationStatus answer = waitInvitationAnswer();
            switch (answer) {
                case INVITATION_REJECTED:
                    if (mLogger.isActivated()) {
                        mLogger.debug("Session has been rejected by user");
                    }

                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_USER);
                    }
                    return;

                case INVITATION_TIMEOUT:
                    if (mLogger.isActivated()) {
                        mLogger.debug("Session has been rejected on timeout");
                    }

                    // Ringing period timeout
                    send486Busy(getDialogPath().getInvite(), getDialogPath().getLocalTag());

                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_TIMEOUT);
                    }
                    return;

                case INVITATION_REJECTED_BY_SYSTEM:
                    if (mLogger.isActivated()) {
                        mLogger.debug("Session has been aborted by system");
                    }
                    removeSession();
                    return;

                case INVITATION_CANCELED:
                    if (mLogger.isActivated()) {
                        mLogger.debug("Session has been rejected by remote");
                    }

                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_REMOTE);
                    }
                    return;

                case INVITATION_ACCEPTED:
                    setSessionAccepted();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionAccepted(contact);
                    }
                    break;

                case INVITATION_DELETED:
                    if (mLogger.isActivated()) {
                        mLogger.debug("Session has been deleted");
                    }
                    removeSession();
                    return;

                default:
                    if (mLogger.isActivated()) {
                        mLogger.debug("Unknown invitation answer in run; answer=".concat(String
                                .valueOf(answer)));
                    }
                    return;
            }

            // Parse the remote SDP part
            String remoteSdp = getDialogPath().getInvite().getSdpContent();
            SdpParser parser = new SdpParser(remoteSdp.getBytes(UTF8));
            Vector<MediaDescription> media = parser.getMediaDescriptions();
            MediaDescription mediaDesc = media.elementAt(0);
            String protocol = mediaDesc.protocol;
            boolean isSecured = false;
            if (protocol != null) {
                isSecured = protocol.equalsIgnoreCase(MsrpConstants.SOCKET_MSRP_SECURED_PROTOCOL);
            }
            // Changed by Deutsche Telekom
            String fileSelector = mediaDesc.getMediaAttribute("file-selector").getValue();
            // Changed by Deutsche Telekom
            String fileTransferId = mediaDesc.getMediaAttribute("file-transfer-id").getValue();
            MediaAttribute attr3 = mediaDesc.getMediaAttribute("path");
            String remotePath = attr3.getValue();
            String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
            int remotePort = mediaDesc.port;

            // Changed by Deutsche Telekom
            String fingerprint = SdpUtils.extractFingerprint(parser, mediaDesc);

            // Extract the "setup" parameter
            String remoteSetup = "passive";
            MediaAttribute attr4 = mediaDesc.getMediaAttribute("setup");
            if (attr4 != null) {
                remoteSetup = attr4.getValue();
            }
            if (mLogger.isActivated()) {
                mLogger.debug("Remote setup attribute is " + remoteSetup);
            }

            // Set setup mode
            String localSetup = createSetupAnswer(remoteSetup);
            if (mLogger.isActivated()) {
                mLogger.debug("Local setup attribute is " + localSetup);
            }

            // Set local port
            int localMsrpPort;
            if (localSetup.equals("active")) {
                localMsrpPort = 9; // See RFC4145, Page 4
            } else {
                localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort(mRcsSettings);
            }

            // Create the MSRP manager
            String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface()
                    .getNetworkAccess().getIpAddress();
            msrpMgr = new MsrpManager(localIpAddress, localMsrpPort, getImsService(), mRcsSettings);
            msrpMgr.setSecured(isSecured);

            // Build SDP part
            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            long maxSize = ImageTransferSession.getMaxImageSharingSize(mRcsSettings);
            String sdp = SdpUtils.buildFileSDP(ipAddress, localMsrpPort,
                    msrpMgr.getLocalSocketProtocol(), getContent().getEncoding(), fileTransferId,
                    fileSelector, null, localSetup, msrpMgr.getLocalMsrpPath(),
                    SdpUtils.DIRECTION_RECVONLY, maxSize);

            // Set the local SDP part in the dialog path
            getDialogPath().setLocalContent(sdp);

            // Test if the session should be interrupted
            if (isInterrupted()) {
                if (mLogger.isActivated()) {
                    mLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            // Create the MSRP server session
            if (localSetup.equals("passive")) {
                // Passive mode: client wait a connection
                // Changed by Deutsche Telekom
                MsrpSession session = msrpMgr.createMsrpServerSession(remotePath, this);
                // Do not use right now the mapping to do not increase memory and cpu consumption
                session.setMapMsgIdFromTransationId(false);

                /* Open the MSRP session */
                msrpMgr.openMsrpSession(ImageTransferSession.DEFAULT_SO_TIMEOUT);

                /* Send an empty packet */
                sendEmptyDataChunk();
            }

            // Create a 200 OK response
            if (mLogger.isActivated()) {
                mLogger.info("Send 200 OK");
            }
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(getDialogPath(),
                    RichcallService.FEATURE_TAGS_IMAGE_SHARE, sdp);

            // The signalisation is established
            getDialogPath().sigEstablished();

            // Send response
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                    .sendSipMessageAndWait(resp);

            // Analyze the received response
            if (ctx.isSipAck()) {
                // ACK received
                if (mLogger.isActivated()) {
                    mLogger.info("ACK request received");
                }

                // Create the MSRP client session
                if (localSetup.equals("active")) {
                    // Active mode: client should connect
                    // Changed by Deutsche Telekom
                    MsrpSession session = msrpMgr.createMsrpClientSession(remoteHost, remotePort,
                            remotePath, this, fingerprint);
                    session.setMapMsgIdFromTransationId(false);
                    /* Open the MSRP session */
                    msrpMgr.openMsrpSession(ImageTransferSession.DEFAULT_SO_TIMEOUT);

                    /* Send an empty packet */
                    sendEmptyDataChunk();
                }

                // The session is established
                getDialogPath().sessionEstablished();

                for (ImsSessionListener listener : listeners) {
                    listener.handleSessionStarted(contact);
                }

                // Start session timer
                if (getSessionTimerManager().isSessionTimerActivated(resp)) {
                    getSessionTimerManager().start(SessionTimerManager.UAS_ROLE,
                            getDialogPath().getSessionExpireTime());
                }
            } else {
                if (mLogger.isActivated()) {
                    mLogger.debug("No ACK received for INVITE");
                }

                // No response received: timeout
                handleError(new ContentSharingError(ContentSharingError.SEND_RESPONSE_FAILED));
            }
        } catch (MsrpException e) {
            handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED, e));
        } catch (SipException e) {
            mLogger.error("Failed to send 200OK response!", e);
            handleError(new ContentSharingError(ContentSharingError.SEND_RESPONSE_FAILED, e));
        } catch (IOException e) {
            handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED, e));
        } catch (RuntimeException e) {
            mLogger.error("Failed to initiate a new sharing session as terminating!", e);
            handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED, e));
        }

        if (mLogger.isActivated()) {
            mLogger.debug("End of thread");
        }
    }

    /**
     * Send an empty data chunk
     * 
     * @throws MsrpException
     */
    public void sendEmptyDataChunk() throws MsrpException {
        msrpMgr.sendEmptyChunk();
    }

    /**
     * Data has been transfered
     * 
     * @param msgId Message ID
     */
    public void msrpDataTransfered(String msgId) {
        // Not used in terminating side
    }

    /**
     * Data transfer has been received
     * 
     * @param msgId Message ID
     * @param data Last received data chunk
     * @param mimeType Data mime-type
     */
    public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
        if (mLogger.isActivated()) {
            mLogger.info("Data received");
        }

        // Image has been transfered
        imageTransfered();

        ContactId contact = getRemoteContact();
        try {
            // Close content with received data
            getContent().writeData2File(data);
            getContent().closeFile();

            Uri image = getContent().getUri();
            for (int j = 0; j < getListeners().size(); j++) {
                ((ImageTransferSessionListener) getListeners().get(j)).handleContentTransfered(
                        contact, image);
            }
        } catch (IOException e) {
            // Delete the temp file
            deleteFile();

            for (int j = 0; j < getListeners().size(); j++) {
                ((ImageTransferSessionListener) getListeners().get(j)).handleSharingError(contact,
                        new ContentSharingError(ContentSharingError.MEDIA_SAVING_FAILED));
            }
        }
    }

    /**
     * Data transfer in progress
     * 
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     */
    public void msrpTransferProgress(long currentSize, long totalSize) {
        // Not used
    }

    /**
     * Data transfer in progress
     * 
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     * @param data received data chunk
     * @return always true TODO
     */
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        ContactId contact = getRemoteContact();
        try {
            // Update content with received data
            getContent().writeData2File(data);

            // Notify listeners
            for (int j = 0; j < getListeners().size(); j++) {
                ((ImageTransferSessionListener) getListeners().get(j)).handleSharingProgress(
                        contact, currentSize, totalSize);
            }
        } catch (IOException e) {
            // Delete the temp file
            deleteFile();

            // Notify listeners
            for (int j = 0; j < getListeners().size(); j++) {
                ((ImageTransferSessionListener) getListeners().get(j)).handleSharingError(contact,
                        new ContentSharingError(ContentSharingError.MEDIA_TRANSFER_FAILED));
            }
        }
        return true;
    }

    /**
     * Data transfer has been aborted
     */
    public void msrpTransferAborted() {
        if (mLogger.isActivated()) {
            mLogger.info("Data transfer aborted");
        }

        if (!isImageTransfered()) {
            // Delete the temp file
            deleteFile();
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

        if (isImageTransfered()) {
            return;
        }
        // Notify listeners
        if (!isSessionInterrupted() && !isSessionTerminatedByRemote()) {
            for (ImsSessionListener listener : getListeners()) {
                ((ImageTransferSessionListener) listener).handleSharingError(contact,
                        new ContentSharingError(ContentSharingError.MEDIA_TRANSFER_FAILED));
            }
        }
    }

    /**
     * Prepare media session
     */
    public void prepareMediaSession() {
        // Nothing to do in terminating side
    }

    /**
     * Start media session
     */
    public void startMediaSession() {
        // Nothing to do in terminating side
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
        if (!isImageTransfered()) {
            // Delete the temp file
            deleteFile();
        }
    }

    /**
     * Delete file
     */
    private void deleteFile() {
        if (mLogger.isActivated()) {
            mLogger.debug("Delete incomplete received image");
        }
        try {
            getContent().deleteFile();
        } catch (IOException e) {
            if (mLogger.isActivated()) {
                mLogger.error("Can't delete received image", e);
            }
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }
}
