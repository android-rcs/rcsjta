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

import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpConstants;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.ImsFileSharingSession;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransferLog;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

/**
 * Terminating file transfer session
 * 
 * @author jexa7410
 */
public class TerminatingMsrpFileSharingSession extends ImsFileSharingSession implements
        MsrpEventListener {
    /**
     * MSRP manager
     */
    private MsrpManager msrpMgr;

    /**
     * Since in MSRP communication we do not have a timestampSent to be extracted from the payload
     * then we need to fake that by using the local timestamp even if this is not the real proper
     * timestamp from the remote side in this case.
     */
    private long mTimestampSent;

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(TerminatingMsrpFileSharingSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param invite Initial INVITE request
     * @param remote contact
     * @param rcsSettings RCS settings
     * @param timestamp Local timestamp for the session
     * @param timestampSent the remote timestamp sent in payload for the file sharing
     * @param contactManager
     */
    public TerminatingMsrpFileSharingSession(ImsService parent, SipRequest invite,
            ContactId remote, RcsSettings rcsSettings, long timestamp, long timestampSent,
            ContactManager contactManager) {
        super(parent, ContentManager.createMmContentFromSdp(invite, rcsSettings), remote,
                FileTransferUtils.extractFileIcon(invite, rcsSettings), IdGenerator
                        .generateMessageID(), rcsSettings, timestamp, contactManager);
        mTimestampSent = timestampSent;

        // Create dialog path
        createTerminatingDialogPath(invite);

        // Set contribution ID
        String id = ChatUtils.getContributionId(invite);
        setContributionID(id);

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
        try {
            if (logger.isActivated()) {
                logger.info("Initiate a new file transfer session as terminating");
            }

            Collection<ImsSessionListener> listeners = getListeners();
            ContactId contact = getRemoteContact();
            MmContent file = getContent();
            MmContent fileIcon = getFileicon();
            long timestamp = getTimestamp();
            /* Check if session should be auto-accepted once */
            if (isSessionAccepted()) {
                if (logger.isActivated()) {
                    logger.debug("Auto accept file transfer invitation");
                }

                for (ImsSessionListener listener : listeners) {

                    ((FileSharingSessionListener) listener).handleSessionAutoAccepted(contact,
                            file, fileIcon, timestamp, mTimestampSent,
                            FileTransferLog.UNKNOWN_EXPIRATION, FileTransferLog.UNKNOWN_EXPIRATION);
                }

            } else {
                if (logger.isActivated()) {
                    logger.debug("Accept manually file transfer invitation");
                }

                for (ImsSessionListener listener : listeners) {
                    ((FileSharingSessionListener) listener).handleSessionInvited(contact, file,
                            fileIcon, timestamp, mTimestampSent,
                            FileTransferLog.UNKNOWN_EXPIRATION, FileTransferLog.UNKNOWN_EXPIRATION);
                }

                send180Ringing(getDialogPath().getInvite(), getDialogPath().getLocalTag());

                InvitationStatus answer = waitInvitationAnswer();
                switch (answer) {
                    case INVITATION_REJECTED:

                        if (logger.isActivated()) {
                            logger.debug("Session has been rejected by user");
                        }

                        removeSession();

                        for (ImsSessionListener listener : listeners) {
                            listener.handleSessionRejectedByUser(contact);
                        }
                        return;

                    case INVITATION_TIMEOUT:
                        if (logger.isActivated()) {
                            logger.debug("Session has been rejected on timeout");
                        }
                        // Ringing period timeout
                        send486Busy(getDialogPath().getInvite(), getDialogPath().getLocalTag());

                        removeSession();

                        for (ImsSessionListener listener : listeners) {
                            listener.handleSessionRejectedByTimeout(contact);
                        }
                        return;

                    case INVITATION_REJECTED_BY_SYSTEM:
                        if (logger.isActivated()) {
                            logger.debug("Session has been aborted by system");
                        }
                        removeSession();
                        return;

                    case INVITATION_CANCELED:
                        if (logger.isActivated()) {
                            logger.debug("Session has been rejected by remote");
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

                    case INVITATION_DELETED:
                        if (logger.isActivated()) {
                            logger.debug("Session has been deleted");
                        }
                        removeSession();
                        return;

                    default:
                        if (logger.isActivated()) {
                            logger.debug("Unknown invitation answer in run; answer=".concat(String
                                    .valueOf(answer)));
                        }
                        return;
                }
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
            if (logger.isActivated()) {
                logger.debug("Remote setup attribute is " + remoteSetup);
            }

            // Set setup mode
            String localSetup = createSetupAnswer(remoteSetup);
            if (logger.isActivated()) {
                logger.debug("Local setup attribute is " + localSetup);
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
            long maxSize = mRcsSettings.getMaxFileTransferSize();
            String sdp = SdpUtils.buildFileSDP(ipAddress, localMsrpPort,
                    msrpMgr.getLocalSocketProtocol(), getContent().getEncoding(), fileTransferId,
                    fileSelector, null, localSetup, msrpMgr.getLocalMsrpPath(),
                    SdpUtils.DIRECTION_RECVONLY, maxSize);

            // Set the local SDP part in the dialog path
            getDialogPath().setLocalContent(sdp);

            // Create the MSRP server session
            if (localSetup.equals("passive")) {
                // Passive mode: client wait a connection
                // Changed by Deutsche Telekom
                MsrpSession session = msrpMgr.createMsrpServerSession(remotePath, this);
                // Do not use right now the mapping to do not increase memory and cpu consumption
                session.setMapMsgIdFromTransationId(false);

                // Open the connection
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            // Open the MSRP session
                            msrpMgr.openMsrpSession(ImsFileSharingSession.DEFAULT_SO_TIMEOUT);

                            // Send an empty packet
                            sendEmptyDataChunk();
                        } catch (IOException e) {
                            if (logger.isActivated()) {
                                logger.error("Can't create the MSRP server session", e);
                            }
                        }
                    }
                };
                thread.start();
            }

            // Create a 200 OK response
            if (logger.isActivated()) {
                logger.info("Send 200 OK");
            }
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(getDialogPath(),
                    InstantMessagingService.FT_FEATURE_TAGS, sdp);

            // The signalisation is established
            getDialogPath().sigEstablished();

            // Send response
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                    .sendSipMessageAndWait(resp);

            // Analyze the received response
            if (ctx.isSipAck()) {
                // ACK received
                if (logger.isActivated()) {
                    logger.info("ACK request received");
                }

                // Create the MSRP client session
                if (localSetup.equals("active")) {
                    // Active mode: client should connect
                    // Changed by Deutsche Telekom
                    MsrpSession session = msrpMgr.createMsrpClientSession(remoteHost, remotePort,
                            remotePath, this, fingerprint);
                    session.setMapMsgIdFromTransationId(false);

                    // Open the connection
                    Thread thread = new Thread() {
                        public void run() {
                            try {
                                // Open the MSRP session
                                msrpMgr.openMsrpSession(ImsFileSharingSession.DEFAULT_SO_TIMEOUT);

                                // Send an empty packet
                                sendEmptyDataChunk();
                            } catch (IOException e) {
                                if (logger.isActivated()) {
                                    logger.error("Can't create the MSRP server session", e);
                                }
                            }
                        }
                    };
                    thread.start();
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
                if (logger.isActivated()) {
                    logger.debug("No ACK received for INVITE");
                }

                // No response received: timeout
                handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED));
            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }

        if (logger.isActivated()) {
            logger.debug("End of thread");
        }
    }

    /**
     * Send an empty data chunk
     */
    public void sendEmptyDataChunk() {
        try {
            msrpMgr.sendEmptyChunk();
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Problem while sending empty data chunk", e);
            }
        }
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
     * @param data Received data
     * @param mimeType Data mime-type
     */
    public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
        if (logger.isActivated()) {
            logger.info("Data received");
        }

        // File has been transfered
        fileTransfered();
        ContactId contact = getRemoteContact();
        MmContent file = getContent();
        Collection<ImsSessionListener> listeners = getListeners();
        try {
            // Close content with received data
            getContent().writeData2File(data);
            getContent().closeFile();

            // Notify listeners
            for (ImsSessionListener listener : listeners) {
                ((FileSharingSessionListener) listener).handleFileTransfered(file, contact,
                        FileTransferLog.UNKNOWN_EXPIRATION, FileTransferLog.UNKNOWN_EXPIRATION);
            }
        } catch (Exception e) {
            // Delete the temp file
            deleteFile();

            // Notify listeners
            for (ImsSessionListener listener : listeners) {
                ((FileSharingSessionListener) listener).handleTransferError(new FileSharingError(
                        FileSharingError.MEDIA_SAVING_FAILED), contact);
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
        if (isSessionInterrupted() || isInterrupted()) {
            return true;
        }
        ContactId contact = getRemoteContact();
        Collection<ImsSessionListener> listeners = getListeners();
        try {
            // Update content with received data
            getContent().writeData2File(data);

            // Notify listeners
            for (ImsSessionListener listener : listeners) {
                ((FileSharingSessionListener) listener).handleTransferProgress(contact,
                        currentSize, totalSize);
            }

        } catch (Exception e) {
            // Delete the temp file
            deleteFile();

            // Notify listeners
            for (ImsSessionListener listener : listeners) {
                ((FileSharingSessionListener) listener).handleTransferError(new FileSharingError(
                        FileSharingError.MEDIA_SAVING_FAILED, e.getMessage()), contact);
            }
        }
        return true;
    }

    /**
     * Data transfer has been aborted
     */
    public void msrpTransferAborted() {
        if (logger.isActivated()) {
            logger.info("Data transfer aborted");
        }

        if (!isFileTransfered()) {
            // Delete the temp file
            deleteFile();
        }
    }

    /**
     * Prepare media session
     * 
     * @throws Exception
     */
    public void prepareMediaSession() throws Exception {
        // Nothing to do in terminating side
    }

    /**
     * Start media session
     * 
     * @throws Exception
     */
    public void startMediaSession() throws Exception {
        // Nothing to do in terminating side
    }

    /**
     * Close media session
     */
    public void closeMediaSession() {
        // Close MSRP session
        if (msrpMgr != null) {
            msrpMgr.closeSession();
            if (logger.isActivated()) {
                logger.debug("MSRP session has been closed");
            }
        }
        if (!isFileTransfered()) {
            // Delete the temp file
            deleteFile();
        }
    }

    /**
     * Delete file
     */
    private void deleteFile() {
        if (logger.isActivated()) {
            logger.debug("Delete incomplete received file");
        }
        try {
            getContent().deleteFile();
        } catch (IOException e) {
            if (logger.isActivated()) {
                logger.error("Can't delete received file", e);
            }
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }
}
