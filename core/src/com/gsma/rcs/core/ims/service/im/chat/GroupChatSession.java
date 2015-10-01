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
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimIdentity;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimParser;
import com.gsma.rcs.core.ims.service.im.chat.event.ConferenceEventSubscribeManager;
import com.gsma.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnUtils;
import com.gsma.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.api.GroupFileTransferImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import gov2.nist.javax2.sip.header.Reason;

import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax2.sip.InvalidArgumentException;
import javax2.sip.header.ExtensionHeader;
import javax2.sip.message.Response;

/**
 * Abstract Group chat session
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public abstract class GroupChatSession extends ChatSession {

    private final ConferenceEventSubscribeManager mConferenceSubscriber;

    /**
     * List of participants as reported by the network via conference events or invited by us. These
     * are persisted in the database. mParticipants should be in sync with the provider at all
     * times.
     */
    private final Map<ContactId, ParticipantStatus> mParticipants;

    /**
     * Boolean variable indicating that the session is no longer marked as the active one for
     * outgoing operations and pending to be removed when it times out.
     */
    private boolean mPendingRemoval = false;

    private final ImsModule mImsModule;

    /**
     * Max number of participants allowed in the group chat including the current user.
     */
    private int mMaxParticipants;

    private static final Logger sLogger = Logger.getLogger(GroupChatSession.class.getSimpleName());

    /**
     * Constructor for originating side
     * 
     * @param imService InstantMessagingService
     * @param contact remote contact identifier
     * @param conferenceId Conference id
     * @param participants Initial set of participants
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public GroupChatSession(InstantMessagingService imService, ContactId contact, Uri conferenceId,
            Map<ContactId, ParticipantStatus> participants, RcsSettings rcsSettings,
            MessagingLog messagingLog, long timestamp, ContactManager contactManager) {
        super(imService, contact, conferenceId, rcsSettings, messagingLog, null, timestamp,
                contactManager);

        mMaxParticipants = rcsSettings.getMaxChatParticipants();

        mParticipants = participants;

        mConferenceSubscriber = new ConferenceEventSubscribeManager(this, rcsSettings, messagingLog);

        mImsModule = imService.getImsModule();

        setFeatureTags(ChatUtils.getSupportedFeatureTagsForGroupChat(rcsSettings));

        setAcceptContactTags(ChatUtils.getAcceptContactTagsForGroupChat());

        String acceptTypes = CpimMessage.MIME_TYPE;
        setAcceptTypes(acceptTypes);

        StringBuilder wrappedTypes = new StringBuilder(MimeType.TEXT_MESSAGE).append(" ").append(
                IsComposingInfo.MIME_TYPE);
        if (rcsSettings.isGeoLocationPushSupported()) {
            wrappedTypes.append(" ").append(GeolocInfoDocument.MIME_TYPE);
        }
        if (rcsSettings.isFileTransferHttpSupported()) {
            wrappedTypes.append(" ").append(FileTransferHttpInfoDocument.MIME_TYPE);
        }
        setWrappedTypes(wrappedTypes.toString());
    }

    @Override
    public boolean isGroupChat() {
        return true;
    }

    /**
     * Get max number of participants in the session including the initiator
     * 
     * @return Maximum number of participants
     */
    public int getMaxParticipants() {
        return mMaxParticipants;
    }

    /**
     * Set max number of participants in the session including the initiator
     * 
     * @param maxParticipants Max number
     */
    public void setMaxParticipants(int maxParticipants) {
        mMaxParticipants = maxParticipants;
    }

    /**
     * Returns all participants associated with the session.
     * 
     * @return Set of participants associated with the session.
     */
    public Map<ContactId, ParticipantStatus> getParticipants() {
        synchronized (mParticipants) {
            return new HashMap<ContactId, ParticipantStatus>(mParticipants);
        }
    }

    /**
     * Returns participants with specified status.
     * 
     * @param status of participants to be returned.
     * @return Set of participants with status participantStatus.
     */
    public Map<ContactId, ParticipantStatus> getParticipants(ParticipantStatus status) {
        synchronized (mParticipants) {
            Map<ContactId, ParticipantStatus> matchingParticipants = new HashMap<ContactId, ParticipantStatus>();
            for (Map.Entry<ContactId, ParticipantStatus> participant : mParticipants.entrySet()) {
                ParticipantStatus participantStatus = participant.getValue();
                if (participantStatus == status) {
                    matchingParticipants.put(participant.getKey(), participantStatus);
                }
            }
            return matchingParticipants;
        }
    }

    /**
     * Returns participants that matches any of the specified statues.
     * 
     * @param status of participants to be returned.
     * @return Set of participants which has any one the statuses specified.
     */
    public Map<ContactId, ParticipantStatus> getParticipants(Set<ParticipantStatus> statuses) {
        synchronized (mParticipants) {
            Map<ContactId, ParticipantStatus> matchingParticipants = new HashMap<ContactId, ParticipantStatus>();
            for (Map.Entry<ContactId, ParticipantStatus> participant : mParticipants.entrySet()) {
                if (statuses.contains(participant.getValue())) {
                    matchingParticipants.put(participant.getKey(), participant.getValue());
                }
            }
            return matchingParticipants;
        }
    }

    /**
     * Get participants for which status has changed and require update
     * 
     * @param participants Participants
     * @return participants for which status has changed
     */
    public Map<ContactId, ParticipantStatus> getParticipantsToUpdate(
            Map<ContactId, ParticipantStatus> participants) {
        synchronized (mParticipants) {
            Map<ContactId, ParticipantStatus> participantsToUpdate = new HashMap<ContactId, ParticipantStatus>();
            for (Map.Entry<ContactId, ParticipantStatus> participantUpdate : participants
                    .entrySet()) {
                ContactId contact = participantUpdate.getKey();
                ParticipantStatus status = participantUpdate.getValue();
                if (status != mParticipants.get(contact)) {
                    participantsToUpdate.put(contact, status);
                }
            }
            return participantsToUpdate;
        }
    }

    /**
     * Apply updates or additions to participants of the group chat.
     * 
     * @param participants Participants
     */
    public void updateParticipants(Map<ContactId, ParticipantStatus> participants) {
        synchronized (mParticipants) {
            Map<ContactId, ParticipantStatus> participantsToUpdate = getParticipantsToUpdate(participants);
            if (participantsToUpdate.isEmpty()) {
                return;
            }
            for (Map.Entry<ContactId, ParticipantStatus> participant : participantsToUpdate
                    .entrySet()) {
                mParticipants.put(participant.getKey(), participant.getValue());
            }
            for (ImsSessionListener listener : getListeners()) {
                ((GroupChatSessionListener) listener).onParticipantsUpdated(participantsToUpdate,
                        mParticipants);
            }
        }
    }

    /**
     * Set a status for group chat participants.
     * 
     * @param contacts The contacts that should have their status set.
     * @param status The status to set.
     */
    private void updateParticipants(final Set<ContactId> contacts, ParticipantStatus status) {
        Map<ContactId, ParticipantStatus> participants = new HashMap<ContactId, ParticipantStatus>();

        for (ContactId contact : contacts) {
            participants.put(contact, status);
        }

        updateParticipants(participants);
    }

    /**
     * Get replaced session ID
     * 
     * @return Session ID
     */
    public String getReplacedSessionId() {
        String result = null;
        ExtensionHeader sessionReplace = (ExtensionHeader) getDialogPath().getInvite().getHeader(
                SipUtils.HEADER_SESSION_REPLACES);
        if (sessionReplace != null) {
            result = sessionReplace.getValue();
        } else {
            String content = getDialogPath().getRemoteContent();
            if (content != null) {
                int index1 = content.indexOf("Session-Replaces=");
                if (index1 != -1) {
                    int index2 = content.indexOf("\"", index1);
                    result = content.substring(index1 + 17, index2);
                }
            }
        }
        return result;
    }

    /**
     * Returns the conference event subscriber
     * 
     * @return Subscribe manager
     */
    public ConferenceEventSubscribeManager getConferenceEventSubscriber() {
        return mConferenceSubscriber;
    }

    @Override
    public void closeMediaSession() {
        // Close MSRP session
        closeMsrpSession();
    }

    @Override
    public void closeSession(TerminationReason reason) throws PayloadException, NetworkException {
        mConferenceSubscriber.terminate();
        super.closeSession(reason);
    }

    @Override
    public void receiveBye(SipRequest bye) throws PayloadException, NetworkException {
        mConferenceSubscriber.terminate();
        super.receiveBye(bye);
        /*
         * When group chat reaches the minimum number of active participants, the Controlling
         * Function indicates this by including a Reason header field with the protocol set to SIP
         * and the protocol-cause set to 410 (e.g. SIP;cause=410;text="Gone") in the SIP BYE request
         * that it sends to the remaining participants.
         */
        TerminationReason reason = TerminationReason.TERMINATION_BY_INACTIVITY;
        Reason sipByeReason = bye.getReason();
        if (sipByeReason != null && Response.GONE == sipByeReason.getCause()) {
            reason = TerminationReason.TERMINATION_BY_REMOTE;
        }
        for (ImsSessionListener listener : getListeners()) {
            listener.onSessionAborted(getRemoteContact(), reason);
        }
    }

    @Override
    public void receiveCancel(SipRequest cancel) throws NetworkException, PayloadException {
        mConferenceSubscriber.terminate();
        super.receiveCancel(cancel);
    }

    /**
     * Send a text message
     * 
     * @param msg Chat message
     * @throws NetworkException
     */
    @Override
    public void sendChatMessage(ChatMessage msg) throws NetworkException {
        String from = ImsModule.getImsUserProfile().getPublicAddress();
        String to = ChatUtils.ANONYMOUS_URI;
        String msgId = msg.getMessageId();
        String networkContent;
        String mimeType = msg.getMimeType();

        if (mImdnManager.isRequestGroupDeliveryDisplayedReportsEnabled()) {
            networkContent = ChatUtils.buildCpimMessageWithImdn(from, to, msgId, msg.getContent(),
                    mimeType, msg.getTimestampSent());
        } else if (mImdnManager.isDeliveryDeliveredReportsEnabled()) {
            networkContent = ChatUtils.buildCpimMessageWithoutDisplayedImdn(from, to, msgId,
                    msg.getContent(), mimeType, msg.getTimestampSent());
        } else {
            networkContent = ChatUtils.buildCpimMessage(from, to, msg.getContent(), mimeType,
                    msg.getTimestampSent());
        }

        if (ChatUtils.isGeolocType(mimeType)) {
            sendDataChunks(IdGenerator.generateMessageID(), networkContent, CpimMessage.MIME_TYPE,
                    TypeMsrpChunk.GeoLocation);
        } else {
            sendDataChunks(IdGenerator.generateMessageID(), networkContent, CpimMessage.MIME_TYPE,
                    TypeMsrpChunk.TextMessage);
        }
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(msg);
        for (ImsSessionListener listener : getListeners()) {
            ((ChatSessionListener) listener).onMessageSent(msgId, apiMimeType);
        }
    }

    @Override
    public void sendIsComposingStatus(boolean status) throws NetworkException {
        String from = ImsModule.getImsUserProfile().getPublicUri();
        String to = ChatUtils.ANONYMOUS_URI;
        String msgId = IdGenerator.generateMessageID();
        String content = ChatUtils.buildCpimMessage(from, to,
                IsComposingInfo.buildIsComposingInfo(status), IsComposingInfo.MIME_TYPE,
                System.currentTimeMillis());
        sendDataChunks(msgId, content, CpimMessage.MIME_TYPE, TypeMsrpChunk.IsComposing);
    }

    @Override
    public void sendMsrpMessageDeliveryStatus(ContactId remote, String msgId, String status,
            long timestamp) throws NetworkException {
        sendMsrpMessageDeliveryStatus(ImsModule.getImsUserProfile().getPublicUri(),
                remote.toString(), msgId, status, timestamp);
    }

    @Override
    public void sendMsrpMessageDeliveryStatus(String fromUri, String toUri, String msgId,
            String status, long timestamp) throws NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Send delivery status ").append(status)
                    .append(" for message ").append(msgId).toString());
        }
        // Send status in CPIM + IMDN headers
        /* Timestamp for IMDN datetime */
        String imdn = ChatUtils.buildImdnDeliveryReport(msgId, status, timestamp);
        /* Timestamp for CPIM DateTime */
        String content = ChatUtils.buildCpimDeliveryReport(fromUri, toUri, imdn,
                System.currentTimeMillis());

        // Send data
        TypeMsrpChunk typeMsrpChunk = TypeMsrpChunk.OtherMessageDeliveredReportStatus;
        if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equalsIgnoreCase(status)) {
            typeMsrpChunk = TypeMsrpChunk.MessageDisplayedReport;
        } else if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equalsIgnoreCase(status)) {
            typeMsrpChunk = TypeMsrpChunk.MessageDeliveredReport;
        }
        sendDataChunks(IdGenerator.generateMessageID(), content, CpimMessage.MIME_TYPE,
                typeMsrpChunk);
        if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
            if (mMessagingLog.getMessageChatId(msgId) != null) {
                for (ImsSessionListener listener : getListeners()) {
                    ((ChatSessionListener) listener).onChatMessageDisplayReportSent(msgId);
                }
            }
        }
    }

    /**
     * Send file info on group chat session
     * 
     * @param fileTransfer
     * @param fileTransferId
     * @param fileInfo
     * @param displayedReportEnabled
     * @param deliveredReportEnabled
     * @throws NetworkException
     */
    public void sendFileInfo(GroupFileTransferImpl fileTransfer, String fileTransferId,
            String fileInfo, boolean displayedReportEnabled, boolean deliveredReportEnabled)
            throws NetworkException {
        String from = ImsModule.getImsUserProfile().getPublicAddress();
        String networkContent;
        long timestamp = System.currentTimeMillis();
        /* For outgoing file transfer, timestampSent = timestamp */
        long timestampSent = timestamp;
        mMessagingLog.setFileTransferTimestamps(fileTransferId, timestamp, timestampSent);
        if (displayedReportEnabled) {
            networkContent = ChatUtils
                    .buildCpimMessageWithImdn(from, ChatUtils.ANONYMOUS_URI, fileTransferId,
                            fileInfo, FileTransferHttpInfoDocument.MIME_TYPE, timestampSent);
        } else if (deliveredReportEnabled) {
            networkContent = ChatUtils.buildCpimMessageWithoutDisplayedImdn(from,
                    ChatUtils.ANONYMOUS_URI, fileTransferId, fileInfo,
                    FileTransferHttpInfoDocument.MIME_TYPE, timestampSent);
        } else {
            networkContent = ChatUtils.buildCpimMessage(from, ChatUtils.ANONYMOUS_URI, fileInfo,
                    FileTransferHttpInfoDocument.MIME_TYPE, timestampSent);
        }
        sendDataChunks(IdGenerator.generateMessageID(), networkContent, CpimMessage.MIME_TYPE,
                TypeMsrpChunk.HttpFileSharing);
        fileTransfer.onFileInfoDequeued();
    }

    /**
     * Get the number of participants that can be added to the group chat without exceeding its max
     * number of participants.
     * 
     * @return the max number of participants that can be added.
     */
    public int getMaxNumberOfAdditionalParticipants() {
        synchronized (mParticipants) {

            int currentParticipants = 0;

            for (ParticipantStatus status : mParticipants.values()) {
                switch (status) {
                    case INVITE_QUEUED:
                    case INVITING:
                    case INVITED:
                    case CONNECTED:
                        currentParticipants++;
                        break;
                    default:
                        break;
                }
            }

            return mMaxParticipants - currentParticipants - 1;
        }
    }

    /**
     * Invite a contact to the session
     * 
     * @param contact Contact to invite
     * @throws NetworkException
     * @throws PayloadException
     */
    public void inviteContact(ContactId contact) throws PayloadException, NetworkException {
        Set<ContactId> contacts = new HashSet<ContactId>();
        contacts.add(contact);
        inviteParticipants(contacts);
    }

    /**
     * Add a set of participants to the session
     * 
     * @param contacts set of participants
     * @throws PayloadException
     * @throws NetworkException
     */
    public void inviteParticipants(Set<ContactId> contacts) throws PayloadException,
            NetworkException {
        try {
            int nbrOfContacts = contacts.size();

            if (sLogger.isActivated()) {
                sLogger.debug("Add " + nbrOfContacts + " participants to the session");
            }

            updateParticipants(contacts, ParticipantStatus.INVITING);

            SessionAuthenticationAgent authenticationAgent = getAuthenticationAgent();

            getDialogPath().incrementCseq();

            if (sLogger.isActivated()) {
                sLogger.debug("Send REFER");
            }

            SipRequest refer;

            if (nbrOfContacts == 1) {
                Uri singleContact = PhoneUtils.formatContactIdToUri(contacts.iterator().next());
                refer = SipMessageFactory.createRefer(getDialogPath(), singleContact, getSubject(),
                        getContributionID());
            } else {
                refer = SipMessageFactory.createRefer(getDialogPath(), contacts, getSubject(),
                        getContributionID());
            }
            SipTransactionContext ctx = mImsModule.getSipManager().sendSubsequentRequest(
                    getDialogPath(), refer);

            int statusCode = ctx.getStatusCode();

            if (statusCode == 407) {
                if (sLogger.isActivated()) {
                    sLogger.debug("407 response received");
                }

                authenticationAgent.readProxyAuthenticateHeader(ctx.getSipResponse());

                getDialogPath().incrementCseq();

                if (sLogger.isActivated()) {
                    sLogger.info("Send second REFER");
                }

                if (nbrOfContacts == 1) {
                    Uri singleContact = PhoneUtils.formatContactIdToUri(contacts.iterator().next());
                    refer = SipMessageFactory.createRefer(getDialogPath(), singleContact,
                            getSubject(), getContributionID());
                } else {
                    refer = SipMessageFactory.createRefer(getDialogPath(), contacts, getSubject(),
                            getContributionID());
                }

                authenticationAgent.setProxyAuthorizationHeader(refer);

                ctx = mImsModule.getSipManager().sendSubsequentRequest(getDialogPath(), refer);

                statusCode = ctx.getStatusCode();

                if ((statusCode >= 200) && (statusCode < 300)) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("20x OK response received");
                    }

                    updateParticipants(contacts, ParticipantStatus.INVITED);
                } else {
                    if (sLogger.isActivated()) {
                        sLogger.debug("REFER has failed (" + statusCode + ")");
                    }

                    updateParticipants(contacts, ParticipantStatus.FAILED);
                }
            } else if ((statusCode >= 200) && (statusCode < 300)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("20x OK response received");
                }

                updateParticipants(contacts, ParticipantStatus.INVITED);
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("No response received");
                }

                updateParticipants(contacts, ParticipantStatus.FAILED);
            }
        } catch (InvalidArgumentException e) {
            throw new PayloadException("REFER request has failed for contacts : " + contacts, e);

        } catch (ParseException e) {
            throw new PayloadException("REFER request has failed for contacts : " + contacts, e);
        }
    }

    @Override
    public void rejectSession() {
        rejectSession(InvitationStatus.INVITATION_REJECTED_DECLINE);
    }

    @Override
    public SipRequest createInvite() throws PayloadException {
        // Nothing to do in terminating side
        return null;
    }

    @Override
    public void handle200OK(SipResponse resp) throws PayloadException, NetworkException,
            FileAccessException {
        super.handle200OK(resp);

        mConferenceSubscriber.subscribe();
    }

    private boolean shouldSendDisplayReport(String dispositionNotification) {
        return mImdnManager.isSendGroupDeliveryDisplayedReportsEnabled()
                && (dispositionNotification != null && dispositionNotification
                        .contains(ImdnDocument.DISPLAY));
    }

    @Override
    public void receiveMsrpData(String msgId, byte[] data, String mimeType)
            throws PayloadException, NetworkException, ContactManagerException {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Data received (type " + mimeType + ")");
        }

        if (data == null || data.length == 0) {
            // By-pass empty data
            if (logActivated) {
                sLogger.debug("By-pass received empty data");
            }
            return;
        }

        if (ChatUtils.isApplicationIsComposingType(mimeType)) {
            // Is composing event
            receiveIsComposing(getRemoteContact(), data);
            return;

        } else if (ChatUtils.isTextPlainType(mimeType)) {
            long timestamp = getTimestamp();
            /**
             * Since legacy server can send non CPIM data (like plain text without timestamp) in the
             * payload, we need to fake timesampSent by using the local timestamp even if this is
             * not the real proper timestamp from the remote side in this case.
             */
            ChatMessage msg = new ChatMessage(msgId, getRemoteContact(), new String(data, UTF8),
                    MimeType.TEXT_MESSAGE, timestamp, timestamp, null);
            receive(msg, false);
            return;

        } else if (!ChatUtils.isMessageCpimType(mimeType)) {
            // Not supported content
            if (logActivated) {
                sLogger.debug("Not supported content " + mimeType + " in chat session");
            }
            return;
        }
        CpimMessage cpimMsg = new CpimParser(data).getCpimMessage();
        if (cpimMsg == null) {
            return;
        }
        String cpimMsgId = cpimMsg.getHeader(ImdnUtils.HEADER_IMDN_MSG_ID);
        if (cpimMsgId == null) {
            cpimMsgId = msgId;
        }
        String contentType = cpimMsg.getContentType();
        ContactId remoteId = getRemoteContact();
        String pseudo = null;
        // In GC, the MSRP 'FROM' header of the SEND message is set to the remote URI
        // Extract URI and optional display name to get pseudo and remoteId
        CpimIdentity cpimIdentity = new CpimIdentity(cpimMsg.getHeader(CpimMessage.HEADER_FROM));
        pseudo = cpimIdentity.getDisplayName();
        PhoneNumber remoteNumber = ContactUtil.getValidPhoneNumberFromUri(cpimIdentity.getUri());
        if (remoteNumber == null) {
            if (logActivated) {
                sLogger.warn("Cannot parse FROM Cpim Identity: ".concat(cpimIdentity.toString()));
            }
        } else {
            remoteId = ContactUtil.createContactIdFromValidatedData(remoteNumber);
            if (logActivated) {
                sLogger.info("Cpim FROM Identity: ".concat(cpimIdentity.toString()));
            }
        }

        // Extract local contactId from "TO" header
        ContactId localId = null;
        cpimIdentity = new CpimIdentity(cpimMsg.getHeader(CpimMessage.HEADER_TO));
        PhoneNumber localNumber = ContactUtil.getValidPhoneNumberFromUri(cpimIdentity.getUri());
        if (localNumber == null) {
            /* purposely left blank */
        } else {
            localId = ContactUtil.createContactIdFromValidatedData(localNumber);
            if (logActivated) {
                sLogger.info("Cpim TO Identity: ".concat(cpimIdentity.toString()));
            }
        }

        String dispositionNotification = cpimMsg.getHeader(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);

        /**
         * Set message's timestamp to the System.currentTimeMillis, not the session's itself
         * timestamp
         */
        long timestamp = System.currentTimeMillis();
        long timestampSent = cpimMsg.getTimestampSent();

        // Analyze received message thanks to the MIME type
        if (FileTransferUtils.isFileTransferHttpType(contentType)) {
            // File transfer over HTTP message
            // Parse HTTP document
            FileTransferHttpInfoDocument fileInfo = FileTransferUtils
                    .parseFileTransferHttpDocument(cpimMsg.getMessageContent().getBytes(UTF8),
                            mRcsSettings);
            if (fileInfo != null) {
                receiveHttpFileTransfer(remoteId, pseudo, fileInfo, cpimMsgId, timestamp,
                        timestampSent);
            } else {
                // TODO : else return error to Originating side
            }

            if (mImdnManager.isDeliveryDeliveredReportsEnabled()) {
                sendMsrpMessageDeliveryStatus(remoteId, cpimMsgId,
                        ImdnDocument.DELIVERY_STATUS_DELIVERED, timestamp);
            }
        } else {
            if (ChatUtils.isTextPlainType(contentType)) {
                ChatMessage msg = new ChatMessage(cpimMsgId, remoteId, cpimMsg.getMessageContent(),
                        MimeType.TEXT_MESSAGE, timestamp, timestampSent, pseudo);
                receive(msg, shouldSendDisplayReport(dispositionNotification));
                if (mImdnManager.isDeliveryDeliveredReportsEnabled()) {
                    sendMsrpMessageDeliveryStatus(remoteId, cpimMsgId,
                            ImdnDocument.DELIVERY_STATUS_DELIVERED, timestamp);
                }
            } else {
                if (ChatUtils.isApplicationIsComposingType(contentType)) {
                    // Is composing event
                    receiveIsComposing(remoteId, cpimMsg.getMessageContent().getBytes(UTF8));
                } else {
                    if (ChatUtils.isMessageImdnType(contentType)) {
                        // Delivery report
                        String publicUri = ImsModule.getImsUserProfile().getPublicUri();
                        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(publicUri);
                        if (number == null) {
                            if (logActivated) {
                                sLogger.error("Cannot parse user contact " + publicUri);
                            }
                        } else {
                            ContactId me = ContactUtil.createContactIdFromValidatedData(number);
                            // Only consider delivery report if sent to me
                            if (localId != null && localId.equals(me)) {
                                onDeliveryStatusReceived(remoteId, cpimMsg.getMessageContent());
                            } else {
                                if (logActivated) {
                                    sLogger.debug("Discard delivery report send to " + localId);
                                }
                            }
                        }
                    } else {
                        if (ChatUtils.isGeolocType(contentType)) {
                            ChatMessage msg = new ChatMessage(cpimMsgId, remoteId,
                                    cpimMsg.getMessageContent(), GeolocInfoDocument.MIME_TYPE,
                                    timestamp, timestampSent, pseudo);
                            receive(msg, shouldSendDisplayReport(dispositionNotification));
                            if (mImdnManager.isDeliveryDeliveredReportsEnabled()) {
                                sendMsrpMessageDeliveryStatus(remoteId, cpimMsgId,
                                        ImdnDocument.DELIVERY_STATUS_DELIVERED, timestamp);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Since the session is no longer used it is marked to be removed when it times out.
     */
    public void markForPendingRemoval() {
        mPendingRemoval = true;
    }

    /**
     * Returns true if this session is no longer used and it is pending to be removed upon timeout.
     * 
     * @return true if this session is no longer used and it is pending to be removed upon timeout.
     */
    public boolean isPendingForRemoval() {
        return mPendingRemoval;
    }

    @Override
    public void terminateSession(TerminationReason reason) throws PayloadException,
            NetworkException {
        /*
         * If there is an ongoing group chat session with same chatId, this session has to be
         * silently aborted so after aborting the session we make sure to not call the rest of this
         * method that would otherwise abort the "current" session also and the GroupChat as a whole
         * which is of course not the intention here
         */
        if (!isPendingForRemoval()) {
            super.terminateSession(reason);
            return;
        }

        interruptSession();

        closeSession(reason);

        closeMediaSession();
    }

    @Override
    public void removeSession() {
        mImsModule.getInstantMessagingService().removeSession(this);
    }

    @Override
    public void msrpTransferError(String msgId, String error, TypeMsrpChunk typeMsrpChunk) {
        try {
            if (isSessionInterrupted()) {
                return;
            }
            if (sLogger.isActivated()) {
                sLogger.info(new StringBuilder("Data transfer error ").append(error)
                        .append(" for message ").append(msgId).append(" (MSRP chunk type: ")
                        .append(typeMsrpChunk).append(")").toString());
            }

            String chatId = getContributionID();
            if (TypeMsrpChunk.MessageDeliveredReport.equals(typeMsrpChunk)) {
                for (ImsSessionListener listener : getListeners()) {
                    ((GroupChatSessionListener) listener).onDeliveryReportSendViaMsrpFailure(msgId,
                            chatId, typeMsrpChunk);
                }
            } else if (TypeMsrpChunk.MessageDisplayedReport.equals(typeMsrpChunk)) {
                for (ImsSessionListener listener : getListeners()) {
                    ((GroupChatSessionListener) listener).onDeliveryReportSendViaMsrpFailure(msgId,
                            chatId, typeMsrpChunk);
                }
            } else if ((msgId != null) && TypeMsrpChunk.TextMessage.equals(typeMsrpChunk)) {
                for (ImsSessionListener listener : getListeners()) {
                    ImdnDocument imdn = new ImdnDocument(msgId, ImdnDocument.DELIVERY_NOTIFICATION,
                            ImdnDocument.DELIVERY_STATUS_FAILED, ImdnDocument.IMDN_DATETIME_NOT_SET);
                    ContactId contact = null;
                    ((ChatSessionListener) listener).onMessageDeliveryStatusReceived(contact, imdn);
                }
            } else {
                // do nothing
                sLogger.error(new StringBuilder("MSRP transfer error not handled for message '")
                        .append(msgId).append("' and chunk type : '").append(typeMsrpChunk)
                        .append("'!").toString());
            }

            int errorCode;

            if ((error != null)
                    && (error.contains(String.valueOf(Response.REQUEST_ENTITY_TOO_LARGE)) || error
                            .contains(String.valueOf(Response.REQUEST_TIMEOUT)))) {
                /*
                 * Session should not be torn down immediately as there may be more errors to come
                 * but as errors occurred we shouldn't use it for sending any longer RFC 4975 408:
                 * An endpoint MUST treat a 408 response in the same manner as it would treat a
                 * local timeout. 413: If a message sender receives a 413 in a response, or in a
                 * REPORT request, it MUST NOT send any further chunks in the message, that is, any
                 * further chunks with the same Message-ID value. If the sender receives the 413
                 * while in the process of sending a chunk, and the chunk is interruptible, the
                 * sender MUST interrupt it.
                 */

                errorCode = ChatError.MEDIA_SESSION_BROKEN;
            } else {
                /*
                 * Default error; used e.g. for 481 or any other error RFC 4975 481: A 481 response
                 * indicates that the indicated session does not exist. The sender should terminate
                 * the session.
                 */

                errorCode = ChatError.MEDIA_SESSION_FAILED;
            }

            ChatError chatError = new ChatError(errorCode, error);
            for (ImsSessionListener listener : getListeners()) {
                ((GroupChatSessionListener) listener).onImError(chatError);
            }

        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error(
                    new StringBuilder("Failed to handle msrp error").append(error)
                            .append(" for message ").append(msgId).toString(), e);
        }
    }

    @Override
    public void handleError(ImsServiceError error) {
        try {
            if (isSessionInterrupted()) {
                return;
            }
            if (sLogger.isActivated()) {
                sLogger.info(new StringBuilder("Session error: ").append(error.getErrorCode())
                        .append(", reason=").append(error.getMessage()).toString());
            }
            closeMediaSession();
            removeSession();
            ChatError chatError = new ChatError(error);
            for (ImsSessionListener listener : getListeners()) {
                ((GroupChatSessionListener) listener).onImError(chatError);
            }

        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error(new StringBuilder("Failed to handle error").append(error).append("!")
                    .toString(), e);
        }
    }
}
