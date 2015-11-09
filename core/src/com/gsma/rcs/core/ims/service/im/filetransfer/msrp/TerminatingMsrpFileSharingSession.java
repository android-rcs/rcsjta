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
import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpConstants;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.ImsFileSharingSession;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

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

    private MsrpManager msrpMgr;

    /**
     * Since in MSRP communication we do not have a timestampSent to be extracted from the payload
     * then we need to fake that by using the local timestamp even if this is not the real proper
     * timestamp from the remote side in this case.
     */
    private long mTimestampSent;

    private static final Logger sLogger = Logger.getLogger(TerminatingMsrpFileSharingSession.class
            .getName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param invite Initial INVITE request
     * @param remote contact
     * @param rcsSettings RCS settings
     * @param timestamp Local timestamp for the session
     * @param timestampSent the remote timestamp sent in payload for the file sharing
     * @param contactManager The contact manager accessor
     * @throws PayloadException
     * @throws FileAccessException
     */
    public TerminatingMsrpFileSharingSession(InstantMessagingService imService, SipRequest invite,
            ContactId remote, RcsSettings rcsSettings, long timestamp, long timestampSent,
            ContactManager contactManager) throws PayloadException, FileAccessException {
        super(imService, ContentManager.createMmContentFromSdp(invite, rcsSettings), remote,
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

    @Override
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Initiate a new file transfer session as terminating");
            }

            Collection<ImsSessionListener> listeners = getListeners();
            ContactId contact = getRemoteContact();
            MmContent file = getContent();
            MmContent fileIcon = getFileicon();
            long timestamp = getTimestamp();
            SipDialogPath dialogPath = getDialogPath();
            /* Check if session should be auto-accepted once */
            if (isSessionAccepted()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Auto accept file transfer invitation");
                }

                for (ImsSessionListener listener : listeners) {

                    ((FileSharingSessionListener) listener).onSessionAutoAccepted(contact, file,
                            fileIcon, timestamp, mTimestampSent,
                            FileTransferData.UNKNOWN_EXPIRATION,
                            FileTransferData.UNKNOWN_EXPIRATION);
                }

            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("Accept manually file transfer invitation");
                }

                for (ImsSessionListener listener : listeners) {
                    ((FileSharingSessionListener) listener).onSessionInvited(contact, file,
                            fileIcon, timestamp, mTimestampSent,
                            FileTransferData.UNKNOWN_EXPIRATION,
                            FileTransferData.UNKNOWN_EXPIRATION);
                }

                send180Ringing(dialogPath.getInvite(), dialogPath.getLocalTag());

                InvitationStatus answer = waitInvitationAnswer();
                switch (answer) {
                    case INVITATION_REJECTED_DECLINE:
                        /* Intentional fall through */
                    case INVITATION_REJECTED_BUSY_HERE:
                        if (sLogger.isActivated()) {
                            sLogger.debug("Session has been rejected by user");
                        }
                        sendErrorResponse(dialogPath.getInvite(), dialogPath.getLocalTag(), answer);
                        removeSession();

                        for (ImsSessionListener listener : listeners) {
                            listener.onSessionRejected(contact,
                                    TerminationReason.TERMINATION_BY_USER);
                        }
                        return;

                    case INVITATION_TIMEOUT:
                        if (sLogger.isActivated()) {
                            sLogger.debug("Session has been rejected on timeout");
                        }
                        /* Ringing period timeout */
                        send486Busy(dialogPath.getInvite(), dialogPath.getLocalTag());

                        removeSession();

                        for (ImsSessionListener listener : listeners) {
                            listener.onSessionRejected(contact,
                                    TerminationReason.TERMINATION_BY_TIMEOUT);
                        }
                        return;

                    case INVITATION_REJECTED_BY_SYSTEM:
                        if (sLogger.isActivated()) {
                            sLogger.debug("Session has been aborted by system");
                        }
                        removeSession();
                        return;

                    case INVITATION_CANCELED:
                        if (sLogger.isActivated()) {
                            sLogger.debug("Session has been rejected by remote");
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
                             listener.onSessionAccepting(contact);
                        }
                        break;

                    case INVITATION_DELETED:
                        if (sLogger.isActivated()) {
                            sLogger.debug("Session has been deleted");
                        }
                        removeSession();
                        return;

                    default:
                        throw new IllegalArgumentException(new StringBuilder(
                                "Unknown invitation answer in run; answer=").append(answer)
                                .toString());
                }
            }

            /* Parse the remote SDP part */
            final SipRequest invite = dialogPath.getInvite();
            String remoteSdp = invite.getSdpContent();
            SipUtils.assertContentIsNotNull(remoteSdp, invite);
            SdpParser parser = new SdpParser(remoteSdp.getBytes(UTF8));
            Vector<MediaDescription> media = parser.getMediaDescriptions();
            MediaDescription mediaDesc = media.elementAt(0);
            String protocol = mediaDesc.mProtocol;
            boolean isSecured = false;
            if (protocol != null) {
                isSecured = protocol.equalsIgnoreCase(MsrpConstants.SOCKET_MSRP_SECURED_PROTOCOL);
            }
            /* Changed by Deutsche Telekom */
            String fileSelector = mediaDesc.getMediaAttribute("file-selector").getValue();
            String fileTransferId = mediaDesc.getMediaAttribute("file-transfer-id").getValue();
            MediaAttribute attr3 = mediaDesc.getMediaAttribute("path");
            String remotePath = attr3.getValue();
            String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
            int remotePort = mediaDesc.mPort;

            /* Changed by Deutsche Telekom */
            String fingerprint = SdpUtils.extractFingerprint(parser, mediaDesc);

            /* Extract the "setup" parameter */
            String remoteSetup = "passive";
            MediaAttribute attr4 = mediaDesc.getMediaAttribute("setup");
            if (attr4 != null) {
                remoteSetup = attr4.getValue();
            }
            if (sLogger.isActivated()) {
                sLogger.debug("Remote setup attribute is " + remoteSetup);
            }

            /* Set setup mode */
            String localSetup = createSetupAnswer(remoteSetup);
            if (sLogger.isActivated()) {
                sLogger.debug("Local setup attribute is " + localSetup);
            }

            /* Set local port */
            int localMsrpPort;
            if (localSetup.equals("active")) {
                localMsrpPort = 9; /* See RFC4145, Page 4 */
            } else {
                localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort(mRcsSettings);
            }

            /* Create the MSRP manager */
            String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface()
                    .getNetworkAccess().getIpAddress();
            msrpMgr = new MsrpManager(localIpAddress, localMsrpPort, getImsService(), mRcsSettings);
            msrpMgr.setSecured(isSecured);

            /* Build SDP part */
            String ipAddress = dialogPath.getSipStack().getLocalIpAddress();
            long maxSize = mRcsSettings.getMaxFileTransferSize();
            String sdp = SdpUtils.buildFileSDP(ipAddress, localMsrpPort,
                    msrpMgr.getLocalSocketProtocol(), getContent().getEncoding(), fileTransferId,
                    fileSelector, null, localSetup, msrpMgr.getLocalMsrpPath(),
                    SdpUtils.DIRECTION_RECVONLY, maxSize);

            /* Set the local SDP part in the dialog path */
            dialogPath.setLocalContent(sdp);

            // Test if the session should be interrupted
            if (isInterrupted()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            /* Create a 200 OK response */
            if (sLogger.isActivated()) {
                sLogger.info("Send 200 OK");
            }
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(dialogPath,
                    InstantMessagingService.FT_FEATURE_TAGS, sdp);

            /* The signalisation is established */
            dialogPath.setSigEstablished();

            /* Send response */
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                    .sendSipMessage(resp);

            /* Create the MSRP server session */
            if (localSetup.equals("passive")) {
                /* Passive mode: client wait a connection */
                /* Changed by Deutsche Telekom */
                MsrpSession session = msrpMgr.createMsrpServerSession(remotePath, this);
                /* Do not use right now the mapping to do not increase memory and cpu consumption */
                session.setMapMsgIdFromTransationId(false);

                msrpMgr.openMsrpSession(ImsFileSharingSession.DEFAULT_SO_TIMEOUT);
                msrpMgr.sendEmptyChunk();
            }

            /* wait a response */
            getImsService().getImsModule().getSipManager().waitResponse(ctx);

            // Test if the session should be interrupted
            if (isInterrupted()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            /* Analyze the received response */
            if (ctx.isSipAck()) {
                if (sLogger.isActivated()) {
                    sLogger.info("ACK request received");
                }

                /* / Create the MSRP client session */
                if (localSetup.equals("active")) {
                    /* Active mode: client should connect */
                    /* Changed by Deutsche Telekom */
                    MsrpSession session = msrpMgr.createMsrpClientSession(remoteHost, remotePort,
                            remotePath, this, fingerprint);
                    session.setMapMsgIdFromTransationId(false);
                    msrpMgr.openMsrpSession(ImsFileSharingSession.DEFAULT_SO_TIMEOUT);
                    msrpMgr.sendEmptyChunk();
                }

                /* The session is established */
                dialogPath.setSessionEstablished();

                for (ImsSessionListener listener : listeners) {
                    listener.onSessionStarted(contact);
                }

                /* Start session timer */
                SessionTimerManager sessionTimerManager = getSessionTimerManager();
                if (sessionTimerManager.isSessionTimerActivated(resp)) {
                    sessionTimerManager.start(SessionTimerManager.UAS_ROLE,
                            dialogPath.getSessionExpireTime());
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("No ACK received for INVITE");
                }

                /* No response received: timeout */
                handleError(new FileSharingError(FileSharingError.SEND_RESPONSE_FAILED));
            }

        } catch (PayloadException e) {
            sLogger.error("Unable to send 200OK response!", e);
            handleError(new FileSharingError(FileSharingError.SEND_RESPONSE_FAILED, e));

        } catch (NetworkException e) {
            handleError(new FileSharingError(FileSharingError.SEND_RESPONSE_FAILED, e));

        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to initiate chat session as terminating!", e);
            handleError(new FileSharingError(FileSharingError.SEND_RESPONSE_FAILED, e));
        }
    }

    @Override
    public void msrpDataTransferred(String msgId) {
        // Not used in terminating side
    }

    @Override
    public void receiveMsrpData(String msgId, byte[] data, String mimeType) {
        if (sLogger.isActivated()) {
            sLogger.info("Data received");
        }
        fileTransfered();
        ContactId contact = getRemoteContact();
        MmContent file = getContent();
        Collection<ImsSessionListener> listeners = getListeners();
        try {
            file.writeData2File(data);
            file.closeFile();
            for (ImsSessionListener listener : listeners) {
                ((FileSharingSessionListener) listener).onFileTransferred(file, contact,
                        FileTransferData.UNKNOWN_EXPIRATION, FileTransferData.UNKNOWN_EXPIRATION,
                        FileTransferProtocol.MSRP);
            }
        } catch (FileAccessException e) {
            deleteFile();
            for (ImsSessionListener listener : listeners) {
                ((FileSharingSessionListener) listener).onTransferError(new FileSharingError(
                        FileSharingError.MEDIA_SAVING_FAILED), contact);
            }
        }
    }

    @Override
    public void msrpTransferProgress(long currentSize, long totalSize) {
        // Not used
    }

    @Override
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        if (isSessionInterrupted() || isInterrupted()) {
            return true;
        }
        ContactId contact = getRemoteContact();
        Collection<ImsSessionListener> listeners = getListeners();
        try {
            getContent().writeData2File(data);
            for (ImsSessionListener listener : listeners) {
                ((FileSharingSessionListener) listener).onTransferProgress(contact, currentSize,
                        totalSize);
            }
        } catch (FileAccessException e) {
            deleteFile();
            for (ImsSessionListener listener : listeners) {
                ((FileSharingSessionListener) listener).onTransferError(new FileSharingError(
                        FileSharingError.MEDIA_SAVING_FAILED, e.getMessage()), contact);
            }
        }
        return true;
    }

    @Override
    public void msrpTransferAborted() {
        if (sLogger.isActivated()) {
            sLogger.info("Data transfer aborted");
        }
        if (!isFileTransferred()) {
            deleteFile();
        }
    }

    @Override
    public void prepareMediaSession() {
        /* Nothing to do in terminating side */
    }

    @Override
    public void openMediaSession() {
        /* Nothing to do in terminating side */
    }

    @Override
    public void startMediaTransfer() {
        /* Nothing to do in terminating side */
    }

    @Override
    public void closeMediaSession() {
        if (msrpMgr != null) {
            msrpMgr.closeSession();
            if (sLogger.isActivated()) {
                sLogger.debug("MSRP session has been closed");
            }
        }
        if (!isFileTransferred()) {
            deleteFile();
        }
    }

    /**
     * Delete file
     */
    private void deleteFile() {
        if (sLogger.isActivated()) {
            sLogger.debug("Delete incomplete received file");
        }
        try {
            getContent().deleteFile();
        } catch (IOException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't delete received file", e);
            }
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }
}
