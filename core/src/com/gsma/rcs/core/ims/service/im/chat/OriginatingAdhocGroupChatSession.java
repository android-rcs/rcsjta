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
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;
import android.text.TextUtils;

import java.text.ParseException;
import java.util.Map;

import javax2.sip.InvalidArgumentException;
import javax2.sip.header.RequireHeader;
import javax2.sip.header.SubjectHeader;

/**
 * Originating ad-hoc group chat session
 * 
 * @author jexa7410
 */
public class OriginatingAdhocGroupChatSession extends GroupChatSession {
    /**
     * Boundary tag
     */
    private final static String BOUNDARY_TAG = "boundary1";

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(OriginatingAdhocGroupChatSession.class
            .getName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param conferenceId Conference ID
     * @param subject Subject associated to the session
     * @param participantsToInvite Map of participants to invite
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public OriginatingAdhocGroupChatSession(InstantMessagingService imService, Uri conferenceId,
            String subject, Map<ContactId, ParticipantStatus> participantsToInvite,
            RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp,
            ContactManager contactManager) {
        super(imService, null, conferenceId, participantsToInvite, rcsSettings, messagingLog,
                timestamp, contactManager);

        if (!TextUtils.isEmpty(subject)) {
            setSubject(subject);
        }

        createOriginatingDialogPath();

        String id = ContributionIdGenerator.getContributionId(getDialogPath().getCallId());
        setContributionID(id);
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Initiate a new ad-hoc group chat session as originating");
            }

            String localSetup = createSetupOffer();
            if (sLogger.isActivated()) {
                sLogger.debug("Local setup attribute is ".concat(localSetup));
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

            String resourceList = ChatUtils.generateChatResourceList(getParticipants().keySet());

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

            if (sLogger.isActivated()) {
                sLogger.info("Send INVITE");
            }
            SipRequest invite = createInviteRequest(multipart);

            getAuthenticationAgent().setAuthorizationHeader(invite);

            getDialogPath().setInvite(invite);

            sendInvite(invite);
        } catch (InvalidArgumentException e) {
            sLogger.error("Unable to set authorization header for chat invite!", e);
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));
        } catch (ParseException e) {
            sLogger.error("Unable to set authorization header for chat invite!", e);
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));
        } catch (SipPayloadException e) {
            sLogger.error("Unable to send 200OK response!", e);
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));
        } catch (SipNetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed initiating chat session!", e);
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));
        }
    }

    /**
     * Create INVITE request
     * 
     * @param content Content part
     * @return Request
     * @throws SipPayloadException
     */
    private SipRequest createInviteRequest(String content) throws SipPayloadException {
        try {
            SipRequest invite = SipMessageFactory.createMultipartInvite(getDialogPath(),
                    getFeatureTags(), getAcceptContactTags(), content, BOUNDARY_TAG);
            if (getSubject() != null) {
                invite.addHeader(SubjectHeader.NAME, getSubject());
            }
            invite.addHeader(RequireHeader.NAME, "recipient-list-invite");
            invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
            return invite;

        } catch (ParseException e) {
            throw new SipPayloadException("Failed to create invite request!", e);
        }
    }

    /**
     * Create an INVITE request
     * 
     * @return the INVITE request
     * @throws SipPayloadException
     */
    public SipRequest createInvite() throws SipPayloadException {
        return createInviteRequest(getDialogPath().getLocalContent());
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
