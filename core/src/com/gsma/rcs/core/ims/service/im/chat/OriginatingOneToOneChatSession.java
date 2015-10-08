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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import java.text.ParseException;

import javax2.sip.InvalidArgumentException;

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
    private final Logger mLogger = Logger.getLogger(getClass().getName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param contact Remote contact identifier
     * @param msg First message of the session
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public OriginatingOneToOneChatSession(InstantMessagingService imService, ContactId contact,
            ChatMessage msg, RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp,
            ContactManager contactManager) {
        super(imService, contact, PhoneUtils.formatContactIdToUri(contact), msg, rcsSettings,
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
            if (mLogger.isActivated()) {
                mLogger.info("Initiate a new 1-1 chat session as originating");
            }

            // Set setup mode
            String localSetup = createSetupOffer();
            if (mLogger.isActivated()) {
                mLogger.debug("Local setup attribute is " + localSetup);
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
                String from = ChatUtils.ANONYMOUS_URI;
                String to = ChatUtils.ANONYMOUS_URI;

                String cpim;
                String mimeType = chatMessage.getMimeType();
                String networkMimeType = ChatUtils.apiMimeTypeToNetworkMimeType(mimeType);
                String networkContent = chatMessage.getContent();
                String msgId = chatMessage.getMessageId();
                long timestampSent = chatMessage.getTimestampSent();
                if (MimeType.GEOLOC_MESSAGE.equals(mimeType)) {
                    networkContent = ChatUtils.persistedGeolocContentToNetworkGeolocContent(
                            networkContent, msgId, timestampSent);
                }
                if (mImdnManager.isRequestOneToOneDeliveryDisplayedReportsEnabled()) {
                    cpim = ChatUtils.buildCpimMessageWithImdn(from, to, msgId, networkContent,
                            networkMimeType, timestampSent);
                } else if (mImdnManager.isDeliveryDeliveredReportsEnabled()) {
                    cpim = ChatUtils.buildCpimMessageWithoutDisplayedImdn(from, to, msgId,
                            networkContent, networkMimeType, timestampSent);
                } else {
                    cpim = ChatUtils.buildCpimMessage(from, to, networkContent, networkMimeType,
                            timestampSent);
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
        } catch (InvalidArgumentException e) {
            mLogger.error("Unable to set authorization header for chat invite!", e);
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));
        } catch (ParseException e) {
            mLogger.error("Unable to set authorization header for chat invite!", e);
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));
        } catch (FileAccessException e) {
            mLogger.error("Unable to send 200OK response!", e);
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));
        } catch (PayloadException e) {
            mLogger.error("Unable to send 200OK response!", e);
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));
        } catch (NetworkException e) {
            if (mLogger.isActivated()) {
                mLogger.debug(e.getMessage());
            }
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            mLogger.error("Failed initiating chat session!", e);
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));
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
