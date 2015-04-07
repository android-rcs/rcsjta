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
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import android.text.TextUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax2.sip.header.RequireHeader;
import javax2.sip.header.SubjectHeader;
import javax2.sip.header.WarningHeader;

/**
 * Restart group chat session
 * 
 * @author jexa7410
 */
public class RestartGroupChatSession extends GroupChatSession {
    /**
     * Boundary tag
     */
    private final static String BOUNDARY_TAG = "boundary1";

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(RestartGroupChatSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param conferenceId Conference ID
     * @param subject Subject associated to the session
     * @param storedParticipants List of invited participants
     * @param contributionId Contribution ID
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     */
    public RestartGroupChatSession(ImsService parent, String conferenceId, String subject,
            String contributionId, Map<ContactId, ParticipantStatus> storedParticipants,
            RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp) {
        super(parent, null, conferenceId, storedParticipants, rcsSettings, messagingLog, timestamp);

        if (!TextUtils.isEmpty(subject)) {
            setSubject(subject);
        }

        createOriginatingDialogPath();

        setContributionID(contributionId);
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (logger.isActivated()) {
                logger.info("Restart a group chat session");
            }

            String localSetup = createSetupOffer();
            if (logger.isActivated()) {
                logger.debug("Local setup attribute is " + localSetup);
            }

            int localMsrpPort;
            if ("active".equals(localSetup)) {
                localMsrpPort = 9; /* See RFC4145, Page 4 */
            } else {
                localMsrpPort = getMsrpMgr().getLocalMsrpPort();
            }

            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String sdp = SdpUtils.buildGroupChatSDP(ipAddress, localMsrpPort, getMsrpMgr()
                    .getLocalSocketProtocol(), getAcceptTypes(), getWrappedTypes(), localSetup,
                    getMsrpMgr().getLocalMsrpPath(), SdpUtils.DIRECTION_SENDRECV);

            Set<ContactId> invitees = new HashSet<ContactId>();
            Map<ContactId,ParticipantStatus> participants = getParticipants();
            for (Map.Entry<ContactId, ParticipantStatus> participant : participants.entrySet()) {
                switch (participants.get(participant.getValue())) {
                    case INVITE_QUEUED:
                    case INVITING:
                    case INVITED:
                    case CONNECTED:
                    case DISCONNECTED:
                        invitees.add(participant.getKey());
                        break;

                    default:
                        break;
                }
            }
            
            String resourceList = ChatUtils.generateChatResourceList(invitees);

            String multipart = new StringBuilder(Multipart.BOUNDARY_DELIMITER).append(BOUNDARY_TAG)
                    .append(SipUtils.CRLF).append("Content-Type: application/sdp")
                    .append(SipUtils.CRLF).append("Content-Length: ")
                    .append(sdp.getBytes(UTF8).length).append(SipUtils.CRLF).append(SipUtils.CRLF)
                    .append(sdp).append(SipUtils.CRLF).append(Multipart.BOUNDARY_DELIMITER)
                    .append(BOUNDARY_TAG).append(SipUtils.CRLF)
                    .append("Content-Type: application/resource-lists+xml").append(SipUtils.CRLF)
                    .append("Content-Length: ").append(resourceList.getBytes(UTF8).length)
                    .append(SipUtils.CRLF).append("Content-Disposition: recipient-list")
                    .append(SipUtils.CRLF).append(SipUtils.CRLF).append(resourceList)
                    .append(SipUtils.CRLF).append(Multipart.BOUNDARY_DELIMITER)
                    .append(BOUNDARY_TAG).append(Multipart.BOUNDARY_DELIMITER).toString();

            getDialogPath().setLocalContent(multipart);

            if (logger.isActivated()) {
                logger.info("Send INVITE");
            }
            SipRequest invite = createInviteRequest(multipart);

            getAuthenticationAgent().setAuthorizationHeader(invite);

            getDialogPath().setInvite(invite);

            sendInvite(invite);
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

    /**
     * Create INVITE request
     * 
     * @param content Content part
     * @return Request
     * @throws SipException
     */
    private SipRequest createInviteRequest(String content) throws SipException {
        SipRequest invite = SipMessageFactory.createMultipartInvite(getDialogPath(),
                getFeatureTags(), getAcceptContactTags(), content, BOUNDARY_TAG);

        // Test if there is a subject
        if (getSubject() != null) {
            // Add a subject header
            invite.addHeader(SubjectHeader.NAME, getSubject());
        }

        // Add a require header
        invite.addHeader(RequireHeader.NAME, "recipient-list-invite");

        // Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());

        return invite;
    }

    /**
     * Create an INVITE request
     * 
     * @return the INVITE request
     * @throws SipException
     */
    public SipRequest createInvite() throws SipException {
        return createInviteRequest(getDialogPath().getLocalContent());
    }

    @Override
    public void handle403Forbidden(SipResponse resp) {
        WarningHeader warn = (WarningHeader) resp.getHeader(WarningHeader.NAME);
        if ((warn != null) && (warn.getText() != null)
                && (warn.getText().contains("127 Service not authorised"))) {
            handleError(new ChatError(ChatError.SESSION_RESTART_FAILED, resp.getReasonPhrase()));
        } else {
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, resp.getStatusCode()
                    + " " + resp.getReasonPhrase()));
        }
    }

    /**
     * Handle 404 Session Not Found
     * 
     * @param resp 404 response
     */
    public void handle404SessionNotFound(SipResponse resp) {
        handleError(new ChatError(ChatError.SESSION_NOT_FOUND, resp.getReasonPhrase()));
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
}
