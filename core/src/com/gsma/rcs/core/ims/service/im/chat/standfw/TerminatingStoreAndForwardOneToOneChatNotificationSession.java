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

package com.gsma.rcs.core.ims.service.im.chat.standfw;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatError;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimParser;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.io.IOException;
import java.util.Vector;

/**
 * Terminating Store & Forward session for one-one push notifications
 * 
 * @author jexa7410
 */
public class TerminatingStoreAndForwardOneToOneChatNotificationSession extends OneToOneChatSession {

    private MsrpManager mMsrpMgr;

    private static final Logger sLogger = Logger
            .getLogger(TerminatingStoreAndForwardOneToOneChatNotificationSession.class.getName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param invite Initial INVITE request
     * @param contact the remote ContactId
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public TerminatingStoreAndForwardOneToOneChatNotificationSession(
            InstantMessagingService imService, SipRequest invite, ContactId contact,
            RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp,
            ContactManager contactManager) {
        super(imService, contact, PhoneUtils.formatContactIdToUri(contact), null, rcsSettings,
                messagingLog, timestamp, contactManager);

        // Create the MSRP manager
        int localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort(rcsSettings);
        String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface()
                .getNetworkAccess().getIpAddress();
        mMsrpMgr = new MsrpManager(localIpAddress, localMsrpPort, rcsSettings);
        if (imService.getImsModule().isConnectedToWifiAccess()) {
            mMsrpMgr.setSecured(rcsSettings.isSecureMsrpOverWifi());
        }

        // Create dialog path
        createTerminatingDialogPath(invite);
    }

    /**
     * Background processing
     */
    public void run() {
        final boolean logActivated = sLogger.isActivated();
        try {
            if (logActivated) {
                sLogger.info("Initiate a new store & forward session for notifications");
            }

            /* Parse the remote SDP part */
            SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes(UTF8));
            Vector<MediaDescription> media = parser.getMediaDescriptions();
            MediaDescription mediaDesc = media.elementAt(0);
            MediaAttribute attr1 = mediaDesc.getMediaAttribute("path");
            String remotePath = attr1.getValue();
            String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
            int remotePort = mediaDesc.mPort;

            /* Changed by Deutsche Telekom */
            String fingerprint = SdpUtils.extractFingerprint(parser, mediaDesc);

            /* Extract the "setup" parameter */
            String remoteSetup = "passive";
            MediaAttribute attr2 = mediaDesc.getMediaAttribute("setup");
            if (attr2 != null) {
                remoteSetup = attr2.getValue();
            }
            if (logActivated) {
                sLogger.debug("Remote setup attribute is ".concat(remoteSetup));
            }

            /* Set setup mode */
            String localSetup = createSetupAnswer(remoteSetup);
            if (logActivated) {
                sLogger.debug("Local setup attribute is ".concat(localSetup));
            }

            /* Set local port */
            int localMsrpPort;
            if (localSetup.equals("active")) {
                localMsrpPort = getMsrpMgr().getLocalMsrpPort();
            } else {
                localMsrpPort = 9; /* See RFC4145, Page 4 */
            }

            /* Build SDP part */
            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String sdp = SdpUtils.buildChatSDP(ipAddress, localMsrpPort, getMsrpMgr()
                    .getLocalSocketProtocol(), getAcceptTypes(), getWrappedTypes(), localSetup,
                    getMsrpMgr().getLocalMsrpPath(), getSdpDirection());

            /* Set the local SDP part in the dialog path */
            getDialogPath().setLocalContent(sdp);

            /* Test if the session should be interrupted */
            if (isInterrupted()) {
                if (logActivated) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            /* Create a 200 OK response */
            if (logActivated) {
                sLogger.info("Send 200 OK");
            }
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(getDialogPath(),
                    InstantMessagingService.CHAT_FEATURE_TAGS, sdp);

            getDialogPath().setSigEstablished();

            /* Send response */
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                    .sendSipMessage(resp);

            // Create the MSRP server session
            if (localSetup.equals("passive")) {
                // Passive mode: client wait a connection
                MsrpSession session = getMsrpMgr().createMsrpServerSession(remotePath, this);
                session.setFailureReportOption(false);
                session.setSuccessReportOption(false);
                getMsrpMgr().openMsrpSession();
                /*
                 * Even if local setup is passive, an empty chunk must be sent to open the NAT and
                 * so enable the active endpoint to initiate a MSRP connection.
                 */
                sendEmptyDataChunk();
            }

            /* wait a response */
            getImsService().getImsModule().getSipManager().waitResponse(ctx);

            // Test if the session should be interrupted
            if (isInterrupted()) {
                if (logActivated) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            /* Analyze the received response */
            if (ctx.isSipAck()) {
                if (logActivated) {
                    sLogger.info("ACK request received");
                }
                getDialogPath().setSessionEstablished();

                /* Create the MSRP client session */
                if (localSetup.equals("active")) {
                    /* Active mode: client should connect */
                    MsrpSession session = getMsrpMgr().createMsrpClientSession(remoteHost,
                            remotePort, remotePath, this, fingerprint);
                    session.setFailureReportOption(false);
                    session.setSuccessReportOption(false);
                    getMsrpMgr().openMsrpSession();
                    sendEmptyDataChunk();
                }
                getActivityManager().start();

            } else {
                if (logActivated) {
                    sLogger.debug("No ACK received for INVITE");
                }

                /* No response received: timeout */
                handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED));
            }
        } catch (MsrpException e) {
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
        } catch (SipPayloadException e) {
            sLogger.error("Unable to send 200OK response!", e);
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
        } catch (SipNetworkException e) {
            if (logActivated) {
                sLogger.debug(e.getMessage());
            }
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
        } catch (IOException e) {
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed initiating a store & forward session for notifications!", e);
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
        }
    }

    /**
     * Returns the MSRP manager
     * 
     * @return MSRP manager
     */
    public MsrpManager getMsrpMgr() {
        return mMsrpMgr;
    }

    /**
     * Close the MSRP session
     */
    public void closeMsrpSession() {
        if (getMsrpMgr() != null) {
            getMsrpMgr().closeSession();
            if (sLogger.isActivated()) {
                sLogger.debug("MSRP session has been closed");
            }
        }
    }

    /**
     * Handle error
     * 
     * @param error Error
     */
    public void handleError(ImsServiceError error) {
        // Error
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Session error: ").append(error.getErrorCode())
                    .append(", reason=").append(error.getMessage()).toString());
        }

        // Close media session
        closeMediaSession();

        // Remove the current session
        removeSession();
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
     * @throws SipNetworkException
     * @throws SipPayloadException
     */
    public void msrpDataReceived(String msgId, byte[] data, String mimeType)
            throws SipPayloadException, SipNetworkException {
        final boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug(new StringBuilder("Data received (type ").append(mimeType).append(")")
                    .toString());
        }
        getActivityManager().updateActivity();
        if ((data == null) || (data.length == 0)) {
            if (logActivated) {
                sLogger.debug("By-pass received empty data");
            }
            return;
        }
        if (ChatUtils.isMessageCpimType(mimeType)) {
            CpimParser cpimParser = new CpimParser(data);
            CpimMessage cpimMsg = cpimParser.getCpimMessage();
            if (cpimMsg == null) {
                return;
            }
            String contentType = cpimMsg.getContentType();
            if (!ChatUtils.isMessageImdnType(contentType)) {
                return;
            }
            String from = cpimMsg.getHeader(CpimMessage.HEADER_FROM);
            PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(from);
            if (number != null) {
                ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
                onDeliveryStatusReceived(contact, cpimMsg.getMessageContent());
            } else {
                onDeliveryStatusReceived(getRemoteContact(), cpimMsg.getMessageContent());
            }
        } else {
            if (logActivated) {
                sLogger.debug(new StringBuilder("Not supported content ").append(mimeType)
                        .append(" in chat session").toString());
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
        // Not used by S&F
    }

    /**
     * Data transfer has been aborted
     */
    public void msrpTransferAborted() {
        // Not used by S&F
    }

    /**
     * Data transfer error
     * 
     * @param msgId Message ID
     * @param error Error code
     */
    public void msrpTransferError(String msgId, String error) {
        if (sLogger.isActivated()) {
            sLogger.info("Data transfer error ".concat(error));
        }
    }

    /**
     * Send an empty data chunk
     * 
     * @throws MsrpException
     */
    public void sendEmptyDataChunk() throws MsrpException {
        mMsrpMgr.sendEmptyChunk();
    }

    // Changed by Deutsche Telekom
    @Override
    public String getSdpDirection() {
        return SdpUtils.DIRECTION_RECVONLY;
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
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
}
