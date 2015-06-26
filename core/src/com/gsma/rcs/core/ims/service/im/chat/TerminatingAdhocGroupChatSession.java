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

import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * Terminating ad-hoc group chat session
 * 
 * @author jexa7410
 */
public class TerminatingAdhocGroupChatSession extends GroupChatSession implements MsrpEventListener {

    private final MessagingLog mMessagingLog;

    private final Logger mLogger = Logger.getLogger(getClass().getSimpleName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param invite Initial INVITE request
     * @param contact remote contact
     * @param participantsFromInvite Map of participants
     * @param remoteUri the remote Uri
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager Contact manager accessor
     * @throws SipPayloadException Thrown if constructor fails to get information from payload
     */
    public TerminatingAdhocGroupChatSession(InstantMessagingService imService, SipRequest invite,
            ContactId contact, Map<ContactId, ParticipantStatus> participantsFromInvite,
            String remoteUri, RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp,
            ContactManager contactManager) throws SipPayloadException {
        super(imService, contact, remoteUri, participantsFromInvite, rcsSettings, messagingLog,
                timestamp, contactManager);

        mMessagingLog = messagingLog;

        String subject = ChatUtils.getSubject(invite);
        setSubject(subject);

        createTerminatingDialogPath(invite);

        if (contact != null) {
            String fromUri = invite.getFrom();
            if (fromUri != null) {
                setRemoteDisplayName(SipUtils.getDisplayNameFromUri(fromUri));
            }
        }

        String chatId = ChatUtils.getContributionId(invite);
        setContributionID(chatId);

        if (shouldBeAutoAccepted()) {
            setSessionAccepted();
        }
    }

    /**
     * Check is session should be auto accepted. This method should only be called once per session
     * 
     * @return true if group chat session should be auto accepted
     * @throws SipPayloadException
     */
    private boolean shouldBeAutoAccepted() throws SipPayloadException {
        /*
         * In case the invite contains a http file transfer info the chat session should be
         * auto-accepted so that the file transfer session can be started.
         */
        if (FileTransferUtils.getHttpFTInfo(getDialogPath().getInvite(), mRcsSettings) != null) {
            return true;
        }

        return mRcsSettings.isGroupChatAutoAccepted();
    }

    private Set<ContactId> getContactToBeInvited(Map<ContactId, ParticipantStatus> participants) {
        String chatId = getContributionID();
        Set<ContactId> storedContacts = new HashSet<ContactId>();
        for (Map.Entry<ContactId, ParticipantStatus> participant : mMessagingLog.getParticipants(
                chatId).entrySet()) {
            switch (participant.getValue()) {
                case INVITING:
                case INVITED:
                case CONNECTED:
                case DISCONNECTED:
                    storedContacts.add(participant.getKey());
                    break;
                default:
                    break;
            }
        }

        if (storedContacts.isEmpty()) {
            if (mLogger.isActivated()) {
                mLogger.info("No initial Group Chat");
            }
            return storedContacts;
        }
        Set<ContactId> contactsToBeInvited = storedContacts;
        /*
         * Only contacts we have stored since before but didn't receive in the group chat invitation
         * are missing and should be re-invited.
         */
        contactsToBeInvited.removeAll(participants.keySet());
        if (!contactsToBeInvited.isEmpty()) {
            /* Early exit if set of contacts to be invited is empty */
            return contactsToBeInvited;
        }
        StringBuilder stringBuilder = new StringBuilder("Invite to restart with missing contacts: ");
        for (ContactId contactToBeInvited : contactsToBeInvited) {
            stringBuilder.append(contactToBeInvited.toString()).append(" ");
        }
        mLogger.info(stringBuilder.toString());
        return contactsToBeInvited;
    }

    /**
     * Background processing
     */
    public void run() {
        final boolean logActivated = mLogger.isActivated();
        try {
            if (logActivated) {
                mLogger.info("Initiate a new ad-hoc group chat session as terminating");
            }

            Collection<ImsSessionListener> listeners = getListeners();
            ContactId contact = getRemoteContact();
            String subject = getSubject();
            Map<ContactId, ParticipantStatus> participants = getParticipants();

            /* Check if session should be auto-accepted once */
            long timestamp = getTimestamp();
            SipDialogPath dialogPath = getDialogPath();
            if (isSessionAccepted()) {
                if (logActivated) {
                    mLogger.debug("Received group chat invitation marked for auto-accept");
                }
                for (ImsSessionListener listener : listeners) {
                    ((GroupChatSessionListener) listener).handleSessionAutoAccepted(contact,
                            subject, participants, timestamp);
                }
            } else {
                if (logActivated) {
                    mLogger.debug("Received group chat invitation marked for manual accept");
                }

                for (ImsSessionListener listener : listeners) {
                    ((GroupChatSessionListener) listener).handleSessionInvited(contact, subject,
                            participants, timestamp);
                }

                send180Ringing(dialogPath.getInvite(), dialogPath.getLocalTag());

                InvitationStatus answer = waitInvitationAnswer();
                switch (answer) {
                    case INVITATION_REJECTED_DECLINE:
                        /* Intentional fall through */
                    case INVITATION_REJECTED_BUSY_HERE:
                        if (logActivated) {
                            mLogger.debug("Session has been rejected by user");
                        }
                        sendErrorResponse(dialogPath.getInvite(), dialogPath.getLocalTag(), answer);
                        removeSession();

                        for (ImsSessionListener listener : listeners) {
                            listener.handleSessionRejected(contact,
                                    TerminationReason.TERMINATION_BY_USER);
                        }
                        return;

                    case INVITATION_TIMEOUT:
                        if (logActivated) {
                            mLogger.debug("Session has been rejected on timeout");
                        }

                        /* Ringing period timeout */
                        send486Busy(dialogPath.getInvite(), dialogPath.getLocalTag());

                        removeSession();

                        for (ImsSessionListener listener : listeners) {
                            listener.handleSessionRejected(contact,
                                    TerminationReason.TERMINATION_BY_TIMEOUT);
                        }
                        return;

                    case INVITATION_REJECTED_BY_SYSTEM:
                        if (logActivated) {
                            mLogger.debug("Session has been aborted by system");
                        }
                        removeSession();
                        return;

                    case INVITATION_CANCELED:
                        if (logActivated) {
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
                            ((ChatSessionListener) listener).handleSessionAccepted(contact);
                        }
                        break;

                    case INVITATION_DELETED:
                        if (mLogger.isActivated()) {
                            mLogger.debug("Session has been deleted");
                        }
                        removeSession();
                        return;

                    default:
                        throw new IllegalArgumentException(
                                "Unknown invitation answer in run; answer=".concat(String
                                        .valueOf(answer)));
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
            int remotePort = mediaDesc.port;

            /* Changed by Deutsche Telekom */
            String fingerprint = SdpUtils.extractFingerprint(parser, mediaDesc);

            /* Extract the "setup" parameter */
            String remoteSetup = "passive";
            MediaAttribute attr2 = mediaDesc.getMediaAttribute("setup");
            if (attr2 != null) {
                remoteSetup = attr2.getValue();
            }
            if (logActivated) {
                mLogger.debug("Remote setup attribute is ".concat(remoteSetup));
            }

            /* Set setup mode */
            String localSetup = createSetupAnswer(remoteSetup);
            if (logActivated) {
                mLogger.debug("Local setup attribute is ".concat(localSetup));
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
            String sdp = SdpUtils.buildGroupChatSDP(ipAddress, localMsrpPort, getMsrpMgr()
                    .getLocalSocketProtocol(), getAcceptTypes(), getWrappedTypes(), localSetup,
                    getMsrpMgr().getLocalMsrpPath(), SdpUtils.DIRECTION_SENDRECV);

            /* Set the local SDP part in the dialog path */
            dialogPath.setLocalContent(sdp);

            /* Test if the session should be interrupted */
            if (isInterrupted()) {
                if (logActivated) {
                    mLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            /* Create a 200 OK response */
            if (logActivated) {
                mLogger.info("Send 200 OK");
            }
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(dialogPath,
                    getFeatureTags(), getAcceptContactTags(), sdp);

            dialogPath.setSigEstablished();
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
                 * Even if local setup is passive, an empty packet must be sent to open the NAT and
                 * so enable the active endpoint to initiate a MSRP connection.
                 */
                sendEmptyDataChunk();
            }

            /* wait a response */
            getImsService().getImsModule().getSipManager().waitResponse(ctx);

            /* Test if the session should be interrupted */
            if (isInterrupted()) {
                if (logActivated) {
                    mLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            /* Analyze the received response */
            if (ctx.isSipAck()) {
                if (logActivated) {
                    mLogger.info("ACK request received");
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
                    listener.handleSessionStarted(contact);
                }
                Set<ContactId> contactsToBeInvited = getContactToBeInvited(participants);
                if (!contactsToBeInvited.isEmpty()) {
                    inviteMissingParticipants(contactsToBeInvited);
                }

                getConferenceEventSubscriber().subscribe();

                SessionTimerManager sessionTimerManager = getSessionTimerManager();
                if (sessionTimerManager.isSessionTimerActivated(resp)) {
                    sessionTimerManager.start(SessionTimerManager.UAS_ROLE,
                            dialogPath.getSessionExpireTime());
                }
            } else {
                if (logActivated) {
                    mLogger.debug("No ACK received for INVITE");
                }

                /* No response received: timeout */
                handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED));
            }
        } catch (MsrpException e) {
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
        } catch (SipException e) {
            mLogger.error("Unable to send 200OK response!", e);
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
        } catch (IOException e) {
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            mLogger.error("Failed to initiate chat session as terminating!", e);
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
        }
    }

    /**
     * Invite missing participants.
     * 
     * @param participants Set of missing participant identifiers
     */
    private void inviteMissingParticipants(final Set<ContactId> participants) {
        final boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.info("Invite missing participants: ".concat(participants.toString()));
        }
        inviteParticipants(participants);
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }

    @Override
    public void startSession() {
        final boolean logActivated = mLogger.isActivated();
        String chatId = getContributionID();
        if (logActivated) {
            mLogger.debug("Start GroupChatSession with chatID '" + chatId + "'");
        }
        InstantMessagingService imService = getImsService().getImsModule()
                .getInstantMessagingService();
        GroupChatSession currentSession = imService.getGroupChatSession(chatId);
        if (currentSession != null) {
            /*
             * If there is already a groupchat session with same chatId existing, we should not
             * reject the new session but update cache with this groupchat session and mark the old
             * groupchat session pending for removal which will timeout eventually
             */
            if (logActivated) {
                mLogger.debug("Ongoing GrooupChat session detected for chatId '" + chatId
                        + "' marking that session pending for removal");
            }
            currentSession.markForPendingRemoval();
            /*
             * Since the current session was already established and we are now replacing that
             * session with a new session then we make sure to auto-accept that new replacement
             * session also so to leave the client in the same situation for the replacement session
             * as for the original "current" session regardless if the the provisioning setting for
             * chat is set to non-auto-accept or not.
             */
            if (currentSession.getDialogPath().isSessionEstablished()) {
                setSessionAccepted();
            }
        }
        imService.addSession(this);
        start();
    }

    /**
     * Request capabilities to contact
     * 
     * @param contact
     */
    private void requestContactCapabilities(String contact) {
        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(contact);
        if (number != null) {
            ContactId remote = ContactUtil.createContactIdFromValidatedData(number);
            getImsService().getImsModule().getCapabilityService()
                    .requestContactCapabilities(remote);
        } else {
            if (mLogger.isActivated()) {
                mLogger.debug("Failed to request capabilities: invalid contact '" + contact + "'");
            }
        }
    }

    @Override
    public void receiveBye(SipRequest bye) {
        super.receiveBye(bye);

        requestContactCapabilities(getDialogPath().getRemoteParty());
    }

    /**
     * Receive CANCEL request
     * 
     * @param cancel CANCEL request
     */
    public void receiveCancel(SipRequest cancel) {
        super.receiveCancel(cancel);

        requestContactCapabilities(getDialogPath().getRemoteParty());
    }
}
