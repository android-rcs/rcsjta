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

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
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
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Collection;
import java.util.Vector;

/**
 * Terminating one-to-one chat session
 * 
 * @author jexa7410
 */
public class TerminatingOneToOneChatSession extends OneToOneChatSession {

    private static final Logger sLogger = Logger.getLogger(TerminatingOneToOneChatSession.class
            .getName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param invite Initial INVITE request
     * @param contact the remote contactId
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager
     * @throws PayloadException
     */
    public TerminatingOneToOneChatSession(InstantMessagingService imService, SipRequest invite,
            ContactId contact, RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp,
            ContactManager contactManager) throws PayloadException {
        super(imService, contact, PhoneUtils.formatContactIdToUri(contact), ChatUtils
                .getFirstMessage(invite, timestamp), rcsSettings, messagingLog, timestamp,
                contactManager);

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
     * Check is session should be auto accepted. This method should only be called once per session
     * 
     * @return true if one-to-one chat session should be auto accepted
     * @throws PayloadException
     */
    private boolean shouldBeAutoAccepted() throws PayloadException {
        /*
         * In case the invite contains a http file transfer info the chat session should be
         * auto-accepted so that the file transfer session can be started.
         */
        if (FileTransferUtils.getHttpFTInfo(getDialogPath().getInvite(), mRcsSettings) != null) {
            return true;
        }
        return mRcsSettings.isChatAutoAccepted();
    }

    /**
     * Background processing
     */
    public void run() {
        final boolean logActivated = sLogger.isActivated();
        try {
            if (logActivated) {
                sLogger.info("Initiate a new 1-1 chat session as terminating");
            }
            ContactId remote = getRemoteContact();
            SipDialogPath dialogPath = getDialogPath();
            if (mImdnManager.isDeliveryDeliveredReportsEnabled()) {
                /* Check notification disposition */
                String msgId = ChatUtils.getMessageId(dialogPath.getInvite());
                if (msgId != null) {
                    /* Send message delivery status via a SIP MESSAGE */
                    mImdnManager.sendMessageDeliveryStatusImmediately(remote.toString(), remote,
                            msgId, ImdnDocument.DELIVERY_STATUS_DELIVERED,
                            SipUtils.getRemoteInstanceID(dialogPath.getInvite()), getTimestamp());
                }
            }

            Collection<ImsSessionListener> listeners = getListeners();

            /* Check if session should be auto-accepted once */
            if (isSessionAccepted()) {
                if (logActivated) {
                    sLogger.debug("Received one-to-one chat invitation marked for auto-accept");
                }

                for (ImsSessionListener listener : listeners) {
                    ((OneToOneChatSessionListener) listener).onSessionAutoAccepted(remote);
                }
            } else {
                if (logActivated) {
                    sLogger.debug("Received one-to-one chat invitation marked for manual accept");
                }

                for (ImsSessionListener listener : listeners) {
                    ((OneToOneChatSessionListener) listener).onSessionInvited(remote);
                }

                send180Ringing(dialogPath.getInvite(), dialogPath.getLocalTag());

                InvitationStatus answer = waitInvitationAnswer();
                switch (answer) {
                    case INVITATION_REJECTED_DECLINE:
                        /* Intentional fall through */
                    case INVITATION_REJECTED_BUSY_HERE:
                        if (logActivated) {
                            sLogger.debug("Session has been rejected by user");
                        }
                        sendErrorResponse(dialogPath.getInvite(), dialogPath.getLocalTag(), answer);
                        removeSession();

                        for (ImsSessionListener listener : listeners) {
                            listener.onSessionRejected(remote,
                                    TerminationReason.TERMINATION_BY_USER);
                        }
                        return;

                    case INVITATION_TIMEOUT:
                        if (logActivated) {
                            sLogger.debug("Session has been rejected on timeout");
                        }

                        /* Ringing period timeout */
                        send486Busy(dialogPath.getInvite(), dialogPath.getLocalTag());

                        removeSession();

                        for (ImsSessionListener listener : listeners) {
                            listener.onSessionRejected(remote,
                                    TerminationReason.TERMINATION_BY_TIMEOUT);
                        }
                        return;

                    case INVITATION_REJECTED_BY_SYSTEM:
                        if (logActivated) {
                            sLogger.debug("Session has been aborted by system");
                        }
                        removeSession();
                        return;

                    case INVITATION_CANCELED:
                        if (logActivated) {
                            sLogger.debug("Session has been rejected by remote");
                        }

                        removeSession();

                        for (ImsSessionListener listener : listeners) {
                            listener.onSessionRejected(remote,
                                    TerminationReason.TERMINATION_BY_REMOTE);
                        }
                        return;

                    case INVITATION_ACCEPTED:
                        setSessionAccepted();

                        for (ImsSessionListener listener : listeners) {
                            listener.onSessionAccepting(remote);
                        }
                        break;

                    case INVITATION_DELETED:
                        if (sLogger.isActivated()) {
                            sLogger.debug("Session has been deleted");
                        }
                        removeSession();
                        return;

                    default:
                        if (logActivated) {
                            sLogger.debug("Unknown invitation answer in run; answer=".concat(String
                                    .valueOf(answer)));
                        }
                        break;
                }
            }

            /* Parse the remote SDP part */
            final SipRequest invite = dialogPath.getInvite();
            String remoteSdp = invite.getSdpContent();
            SipUtils.assertContentIsNotNull(remoteSdp, invite);
            SdpParser parser = new SdpParser(remoteSdp.getBytes(UTF8));
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
                localMsrpPort = 9; /* See RFC4145, Page 4 */
            } else {
                localMsrpPort = getMsrpMgr().getLocalMsrpPort();
            }

            /* Build SDP part */
            String ipAddress = dialogPath.getSipStack().getLocalIpAddress();
            String sdp = SdpUtils.buildChatSDP(ipAddress, localMsrpPort, getMsrpMgr()
                    .getLocalSocketProtocol(), getAcceptTypes(), getWrappedTypes(), localSetup,
                    getMsrpMgr().getLocalMsrpPath(), getSdpDirection());

            /* Set the local SDP part in the dialog path */
            dialogPath.setLocalContent(sdp);

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
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(dialogPath,
                    getFeatureTags(), sdp);

            dialogPath.setSigEstablished();

            /* Send response */
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                    .sendSipMessage(resp);

            /* Create the MSRP server session */
            if (localSetup.equals("passive")) {
                /* Passive mode: client wait a connection */
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

            /* Test if the session should be interrupted */
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
                dialogPath.setSessionEstablished();

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
                for (ImsSessionListener listener : listeners) {
                    listener.onSessionStarted(remote);
                }
                SessionTimerManager sessionTimerManager = getSessionTimerManager();
                if (sessionTimerManager.isSessionTimerActivated(resp)) {
                    sessionTimerManager.start(SessionTimerManager.UAS_ROLE,
                            dialogPath.getSessionExpireTime());
                }
                getActivityManager().start();

            } else {
                if (logActivated) {
                    sLogger.debug("No ACK received for INVITE");
                }

                /* No response received: timeout */
                handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED));
            }
        } catch (PayloadException e) {
            sLogger.error("Unable to send 200OK response!", e);
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));

        } catch (NetworkException e) {
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));

        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to initiate chat session as terminating!", e);
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
        }
    }

    // Changed by Deutsche Telekom
    @Override
    public String getSdpDirection() {
        return SdpUtils.DIRECTION_SENDRECV;
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }

    @Override
    public void startSession() throws PayloadException, NetworkException {
        final boolean logActivated = sLogger.isActivated();
        ContactId remote = getRemoteContact();
        if (logActivated) {
            sLogger.debug(new StringBuilder("Start OneToOneChatSession with '").append(remote)
                    .append("'").toString());
        }
        InstantMessagingService imService = getImsService().getImsModule()
                .getInstantMessagingService();
        OneToOneChatSession currentSession = imService.getOneToOneChatSession(remote);
        if (currentSession != null) {
            boolean currentSessionInitiatedByRemote = currentSession.isInitiatedByRemote();
            boolean currentSessionEstablished = currentSession.getDialogPath()
                    .isSessionEstablished();
            if (!currentSessionEstablished && !currentSessionInitiatedByRemote) {
                /*
                 * Rejecting the NEW invitation since there is already a PENDING OneToOneChatSession
                 * that was locally originated with the same contact.
                 */
                if (logActivated) {
                    sLogger.warn(new StringBuilder("Rejecting OneToOneChatSession (session id '")
                            .append(getSessionID()).append("') with '").append(remote).append("'")
                            .toString());
                }
                rejectSession();
                return;
            }
            /*
             * If this oneToOne session does NOT already contain another oneToOne chat session which
             * in state PENDING and also LOCALLY originating we should leave (reject or abort) the
             * CURRENT rcs chat session if there is one and replace it with the new one.
             */
            if (logActivated) {
                sLogger.warn(new StringBuilder(
                        "Rejecting/Aborting existing OneToOneChatSession (session id '")
                        .append(getSessionID()).append("') with '").append(remote).append("'")
                        .toString());
            }
            if (currentSessionInitiatedByRemote) {
                if (currentSessionEstablished) {
                    currentSession.terminateSession(TerminationReason.TERMINATION_BY_SYSTEM);
                } else {
                    currentSession.rejectSession();
                }
            } else {
                currentSession.terminateSession(TerminationReason.TERMINATION_BY_SYSTEM);
            }
            /*
             * Since the current session was already established and we are now replacing that
             * session with a new session then we make sure to auto-accept that new replacement
             * session also so to leave the client in the same situation for the replacement session
             * as for the original "current" session regardless if the the provisioning setting for
             * chat is set to non-auto-accept or not.
             */
            if (currentSessionEstablished) {
                setSessionAccepted();
            }
        }
        imService.addSession(this);
        start();
    }

    @Override
    public void removeSession() {
        getImsService().getImsModule().getInstantMessagingService().removeSession(this);
    }
}
