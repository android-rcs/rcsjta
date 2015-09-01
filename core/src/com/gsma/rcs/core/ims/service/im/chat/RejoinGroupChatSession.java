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

import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.text.TextUtils;

import java.text.ParseException;

import javax2.sip.InvalidArgumentException;
import javax2.sip.header.SubjectHeader;

/**
 * Rejoin a group chat session
 * 
 * @author Jean-Marc AUFFRET
 */
public class RejoinGroupChatSession extends GroupChatSession {
    /**
     * The logger
     */
    private final Logger mLogger = Logger.getLogger(getClass().getSimpleName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param groupChatInfo Group Chat information
     * @param rcsSettings Rcs settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public RejoinGroupChatSession(InstantMessagingService imService, GroupChatInfo groupChatInfo,
            RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp,
            ContactManager contactManager) {
        super(imService, null, groupChatInfo.getRejoinId(), groupChatInfo.getParticipants(),
                rcsSettings, messagingLog, timestamp, contactManager);

        if (!TextUtils.isEmpty(groupChatInfo.getSubject())) {
            setSubject(groupChatInfo.getSubject());
        }

        createOriginatingDialogPath();

        setContributionID(groupChatInfo.getContributionId());
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (mLogger.isActivated()) {
                mLogger.info("Rejoin an existing group chat session");
            }

            String localSetup = createSetupOffer();
            if (mLogger.isActivated()) {
                mLogger.debug("Local setup attribute is ".concat(localSetup));
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

            getDialogPath().setLocalContent(sdp);

            if (mLogger.isActivated()) {
                mLogger.info("Send INVITE");
            }
            SipRequest invite = createInviteRequest(sdp);

            getAuthenticationAgent().setAuthorizationHeader(invite);

            getDialogPath().setInvite(invite);

            sendInvite(invite);
        } catch (InvalidArgumentException e) {
            mLogger.error("Unable to set authorization header for chat invite!", e);
            handleError(new ChatError(ChatError.SESSION_REJOIN_FAILED, e));
        } catch (ParseException e) {
            mLogger.error("Unable to set authorization header for chat invite!", e);
            handleError(new ChatError(ChatError.SESSION_REJOIN_FAILED, e));
        } catch (SipPayloadException e) {
            mLogger.error("Unable to send 200OK response!", e);
            handleError(new ChatError(ChatError.SESSION_REJOIN_FAILED, e));
        } catch (SipNetworkException e) {
            if (mLogger.isActivated()) {
                mLogger.debug(e.getMessage());
            }
            handleError(new ChatError(ChatError.SESSION_REJOIN_FAILED, e));
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            mLogger.error("Failed to rejoin a chat session!", e);
            handleError(new ChatError(ChatError.SESSION_REJOIN_FAILED, e));
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
            SipRequest invite = SipMessageFactory.createInvite(getDialogPath(), getFeatureTags(),
                    getAcceptContactTags(), content);
            final String subject = getSubject();
            if (subject != null) {
                invite.addHeader(SubjectHeader.NAME, subject);
            }
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

    /**
     * Handle 404 Session Not Found
     * 
     * @param resp 404 response
     */
    public void handle404SessionNotFound(SipResponse resp) {
        // Rejoin session has failed, we update the database with status terminated by remote

        // TODO Once after CR18 is implemented we will check if this callback is
        // really required and act accordingly

        // MessagingLog.getInstance().updateGroupChatStatus(getContributionID(),
        // GroupChat.State.TERMINATED, GroupChat.ReasonCode.NONE);

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
