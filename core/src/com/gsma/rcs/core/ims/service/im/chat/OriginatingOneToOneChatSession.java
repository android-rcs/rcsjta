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

package com.gsma.rcs.core.ims.service.im.chat;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Originating one-to-one chat session
 * 
 * @author jexa7410
 */
public class OriginatingOneToOneChatSession extends OneToOneChatSession {
    /**
     * Boundary tag
     */
    private final static String BOUNDARY_TAG = "boundary1";

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param contact Remote contact identifier
     * @param msg First message of the session
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public OriginatingOneToOneChatSession(ImsService parent, ContactId contact, ChatMessage msg,
            RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp,
            ContactsManager contactManager) {
        super(parent, contact, PhoneUtils.formatContactIdToUri(contact), msg, rcsSettings,
                messagingLog, timestamp, contactManager);
        // Create dialog path
        createOriginatingDialogPath();
        // Set contribution ID
        String id = ContributionIdGenerator.getContributionId(getDialogPath().getCallId());
        setContributionID(id);
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (logger.isActivated()) {
                logger.info("Initiate a new 1-1 chat session as originating");
            }

            // Set setup mode
            String localSetup = createSetupOffer();
            if (logger.isActivated()) {
                logger.debug("Local setup attribute is " + localSetup);
            }

            // Set local port
            int localMsrpPort;
            if ("active".equals(localSetup)) {
                localMsrpPort = 9; // See RFC4145, Page 4
            } else {
                localMsrpPort = getMsrpMgr().getLocalMsrpPort();
            }

            // Build SDP part
            // String ntpTime =
            // SipUtils.constructNTPtime(System.currentTimeMillis());
            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String sdp = SdpUtils.buildChatSDP(ipAddress, localMsrpPort, getMsrpMgr()
                    .getLocalSocketProtocol(), getAcceptTypes(), getWrappedTypes(), localSetup,
                    getMsrpMgr().getLocalMsrpPath(), getSdpDirection());

            // If there is a first message then builds a multipart content else
            // builds a SDP content
            ChatMessage chatMessage = getFirstMessage();
            if (chatMessage != null) {
                // Build CPIM part
                String from = ChatUtils.ANOMYNOUS_URI;
                String to = ChatUtils.ANOMYNOUS_URI;

                boolean useImdn = getImdnManager().isImdnActivated();
                String cpim;
                if (useImdn) {
                    // Send message in CPIM + IMDN
                    cpim = ChatUtils.buildCpimMessageWithImdn(from, to, chatMessage.getMessageId(),
                            chatMessage.getContent(), chatMessage.getMimeType(),
                            chatMessage.getTimestampSent());
                } else {
                    // Send message in CPIM
                    cpim = ChatUtils.buildCpimMessage(from, to, chatMessage.getContent(),
                            chatMessage.getMimeType(), chatMessage.getTimestampSent());
                }

                String multipart = new StringBuilder(Multipart.BOUNDARY_DELIMITER)
                        .append(BOUNDARY_TAG).append(SipUtils.CRLF)
                        .append("Content-Type: application/sdp").append(SipUtils.CRLF)
                        .append("Content-Length: ").append(sdp.getBytes(UTF8).length)
                        .append(SipUtils.CRLF).append(SipUtils.CRLF).append(sdp)
                        .append(SipUtils.CRLF).append(Multipart.BOUNDARY_DELIMITER)
                        .append(BOUNDARY_TAG).append(SipUtils.CRLF).append("Content-Type: ")
                        .append(CpimMessage.MIME_TYPE).append(SipUtils.CRLF)
                        .append("Content-Length: ").append(cpim.getBytes(UTF8).length)
                        .append(SipUtils.CRLF).append(SipUtils.CRLF).append(cpim)
                        .append(SipUtils.CRLF).append(Multipart.BOUNDARY_DELIMITER)
                        .append(BOUNDARY_TAG).append(Multipart.BOUNDARY_DELIMITER).toString();

                // Set the local SDP part in the dialog path
                getDialogPath().setLocalContent(multipart);
            } else {
                // Set the local SDP part in the dialog path
                getDialogPath().setLocalContent(sdp);
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
            handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

    // Changed by Deutsche Telekom
    @Override
    public String getSdpDirection() {
        return SdpUtils.DIRECTION_SENDRECV;
    }

    @Override
    public boolean isInitiatedByRemote() {
        return false;
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
