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

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
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
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax2.sip.header.ExtensionHeader;

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
    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(GroupChatSession.class.getSimpleName());

    /**
     * Constructor for originating side
     * 
     * @param parent IMS service
     * @param contact remote contact identifier
     * @param conferenceId Conference id
     * @param participants Initial set of participants
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     */
    public GroupChatSession(ImsService parent, ContactId contact, String conferenceId,
            Map<ContactId, ParticipantStatus> participants, RcsSettings rcsSettings,
            MessagingLog messagingLog, long timestamp) {
        super(parent, contact, conferenceId, rcsSettings, messagingLog, null, timestamp);

        mMaxParticipants = rcsSettings.getMaxChatParticipants();

        mParticipants = participants;

        mConferenceSubscriber = new ConferenceEventSubscribeManager(this, rcsSettings, messagingLog);

        mImsModule = parent.getImsModule();

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
     * Returns participants with ParticipantStatus status.
     * 
     * @param status of participants to be returned.
     * @return Set of participants with status participantStatus.
     */
    public Map<ContactId, ParticipantStatus> getParticipants(ParticipantStatus status) {
        synchronized (mParticipants) {
            Map<ContactId, ParticipantStatus> matcingParticipants = new HashMap<ContactId, ParticipantStatus>();

            for (Map.Entry<ContactId, ParticipantStatus> participant : mParticipants.entrySet()) {
                if (participant.getValue() == status) {
                    matcingParticipants.put(participant.getKey(), participant.getValue());
                }
            }

            return matcingParticipants;
        }
    }

    /**
     * Apply updates or additions to participants of the group chat.
     * 
     * @param participantUpdates Participant updates
     * @return participants for which status has changed
     */
    public Map<ContactId, ParticipantStatus> updateParticipants(
            Map<ContactId, ParticipantStatus> participantUpdates) {
        synchronized (mParticipants) {
            Map<ContactId, ParticipantStatus> updatedParticipants = new HashMap<ContactId, ParticipantStatus>();

            for (Map.Entry<ContactId, ParticipantStatus> participantUpdate : participantUpdates
                    .entrySet()) {
                ContactId contact = participantUpdate.getKey();
                ParticipantStatus status = participantUpdate.getValue();
                if (!status.equals(mParticipants.get(contact))) {
                    mParticipants.put(contact, status);
                    updatedParticipants.put(contact, status);
                }
            }

            if (!updatedParticipants.isEmpty()) {
                for (ImsSessionListener listener : getListeners()) {
                    ((GroupChatSessionListener) listener).handleParticipantUpdates(
                            updatedParticipants, mParticipants);
                }
            }
            return updatedParticipants;
        }
    }

    /**
     * Set a status for group chat participants.
     * 
     * @param contacts The contacts that should have their status set.
     * @param status The status to set.
     */
    public void updateParticipants(final Set<ContactId> contacts, ParticipantStatus status) {
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

    /**
     * Close media session
     */
    public void closeMediaSession() {
        // Close MSRP session
        closeMsrpSession();
    }

    /**
     * Terminate session
     * 
     * @param reason Reason
     */
    public void terminateSession(TerminationReason reason) {
        // Stop conference subscription
        mConferenceSubscriber.terminate();

        // Terminate session
        super.terminateSession(reason);
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
            mImsModule.getCapabilityService().requestContactCapabilities(remote);
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("Failed to request capabilities: invalid contact '" + contact + "'");
            }
        }
    }

    /**
     * Receive BYE request
     * 
     * @param bye BYE request
     */
    public void receiveBye(SipRequest bye) {
        // Stop conference subscription
        mConferenceSubscriber.terminate();

        // Receive BYE request
        super.receiveBye(bye);

        // Request capabilities if remote contact is valid
        requestContactCapabilities(getDialogPath().getRemoteParty());
    }

    /**
     * Receive CANCEL request
     * 
     * @param cancel CANCEL request
     */
    public void receiveCancel(SipRequest cancel) {
        // Stop conference subscription
        mConferenceSubscriber.terminate();

        super.receiveCancel(cancel);

        // Request capabilities if remote contact is valid
        requestContactCapabilities(getDialogPath().getRemoteParty());
    }

    /**
     * Send a text message
     * 
     * @param msg Chat message
     */
    @Override
    public void sendChatMessage(ChatMessage msg) {
        String from = ImsModule.IMS_USER_PROFILE.getPublicAddress();
        String to = ChatUtils.ANOMYNOUS_URI;
        String msgId = msg.getMessageId();
        String networkContent;
        String mimeType = msg.getMimeType();
        boolean useImdn = getImdnManager().isImdnActivated() && !mRcsSettings.isAlbatrosRelease();
        if (useImdn) {
            networkContent = ChatUtils.buildCpimMessageWithDeliveredImdn(from, to, msgId,
                    msg.getContent(), mimeType, msg.getTimestampSent());

        } else {
            networkContent = ChatUtils.buildCpimMessage(from, to, msg.getContent(), mimeType,
                    msg.getTimestampSent());
        }

        Collection<ImsSessionListener> listeners = getListeners();
        for (ImsSessionListener listener : listeners) {
            ((ChatSessionListener) listener).handleMessageSending(msg);
        }

        boolean sendOperationSucceeded = false;
        if (ChatUtils.isGeolocType(mimeType)) {
            sendOperationSucceeded = sendDataChunks(IdGenerator.generateMessageID(),
                    networkContent, CpimMessage.MIME_TYPE, TypeMsrpChunk.GeoLocation);
        } else {
            sendOperationSucceeded = sendDataChunks(IdGenerator.generateMessageID(),
                    networkContent, CpimMessage.MIME_TYPE, TypeMsrpChunk.TextMessage);
        }

        /* TODO:This will be redone with CR037 */
        if (sendOperationSucceeded) {
            for (ImsSessionListener listener : getListeners()) {
                ((ChatSessionListener) listener).handleMessageSent(msgId,
                        ChatUtils.networkMimeTypeToApiMimeType(mimeType));
            }
        } else {
            for (ImsSessionListener listener : getListeners()) {
                ((ChatSessionListener) listener).handleMessageFailedSend(msgId,
                        ChatUtils.networkMimeTypeToApiMimeType(mimeType));
            }
        }
    }

    @Override
    public void sendIsComposingStatus(boolean status) {
        String from = ImsModule.IMS_USER_PROFILE.getPublicUri();
        String to = ChatUtils.ANOMYNOUS_URI;
        String msgId = IdGenerator.generateMessageID();
        String content = ChatUtils.buildCpimMessage(from, to,
                IsComposingInfo.buildIsComposingInfo(status), IsComposingInfo.MIME_TYPE,
                System.currentTimeMillis());
        sendDataChunks(msgId, content, CpimMessage.MIME_TYPE, TypeMsrpChunk.IsComposing);
    }

    @Override
    public void sendMsrpMessageDeliveryStatus(ContactId remote, String msgId, String status,
            long timestamp) {
        // Send status in CPIM + IMDN headers
        String to = (remote != null) ? remote.toString() : ChatUtils.ANOMYNOUS_URI;
        sendMsrpMessageDeliveryStatus(null, to, msgId, status, timestamp);
    }

    @Override
    public void sendMsrpMessageDeliveryStatus(String fromUri, String toUri, String msgId,
            String status, long timestamp) {
        // Do not perform Message Delivery Status in Albatros for group chat
        // Only perform delivery status delivered in GC
        if (mRcsSettings.isAlbatrosRelease()
                || !status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Send delivery status delivered for message " + msgId);
        }
        // Send status in CPIM + IMDN headers
        /* Timestamp for IMDN datetime */
        String imdn = ChatUtils.buildImdnDeliveryReport(msgId, status, timestamp);
        /* Timestamp for CPIM DateTime */
        String content = ChatUtils.buildCpimDeliveryReport(
                ImsModule.IMS_USER_PROFILE.getPublicUri(), toUri, imdn, System.currentTimeMillis());

        // Send data
        sendDataChunks(IdGenerator.generateMessageID(), content, CpimMessage.MIME_TYPE,
                TypeMsrpChunk.MessageDeliveredReport);
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
     */
    public void inviteContact(ContactId contact) {
        Set<ContactId> contacts = new HashSet<ContactId>();
        contacts.add(contact);
        inviteParticipants(contacts);
    }

    /**
     * Add a set of participants to the session
     * 
     * @param contacts set of participants
     */
    public void inviteParticipants(Set<ContactId> contacts) {
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
                String singleContactUri = PhoneUtils.formatContactIdToUri(contacts.iterator()
                        .next());
                refer = SipMessageFactory.createRefer(getDialogPath(), singleContactUri,
                        getSubject(), getContributionID());
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
                    String singleContactUri = PhoneUtils.formatContactIdToUri(contacts.iterator()
                            .next());
                    refer = SipMessageFactory.createRefer(getDialogPath(), singleContactUri,
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
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("REFER request has failed", e);
            }

            updateParticipants(contacts, ParticipantStatus.FAILED);
        }
    }

    /**
     * Reject the session invitation
     */
    public void rejectSession() {
        rejectSession(603);
    }

    /**
     * Create an INVITE request
     * 
     * @return the INVITE request
     * @throws SipException
     */
    public SipRequest createInvite() throws SipException {
        // Nothing to do in terminating side
        return null;
    }

    /**
     * Handle 200 0K response
     * 
     * @param resp 200 OK response
     */
    public void handle200OK(SipResponse resp) {
        super.handle200OK(resp);

        mConferenceSubscriber.subscribe();
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.core.ims.service.im.chat.ChatSession#msrpDataReceived(java.lang.String,
     * byte[], java.lang.String)
     */
    @Override
    public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Data received (type " + mimeType + ")");
        }

        // Update the activity manager
        getActivityManager().updateActivity();

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
            boolean imdnDisplayedRequested = false;
            receive(msg, imdnDisplayedRequested);
            return;

        } else if (!ChatUtils.isMessageCpimType(mimeType)) {
            // Not supported content
            if (logActivated) {
                sLogger.debug("Not supported content " + mimeType + " in chat session");
            }
            return;
        }

        // Receive a CPIM message
        CpimParser cpimParser = null;
        try {
            cpimParser = new CpimParser(data);
        } catch (Exception e) {
            if (logActivated) {
                sLogger.error("Can't parse the CPIM message", e);
            }
            return;
        }
        CpimMessage cpimMsg = cpimParser.getCpimMessage();
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

        // Check if the message needs a delivery report
        String dispositionNotification = cpimMsg.getHeader(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);

        boolean isFToHTTP = FileTransferUtils.isFileTransferHttpType(contentType);
        /**
         * Set message's timestamp to the System.currentTimeMillis, not the session's itself
         * timestamp
         */
        long timestamp = System.currentTimeMillis();
        long timestampSent = cpimMsg.getTimestampSent();

        // Analyze received message thanks to the MIME type
        if (isFToHTTP) {
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
            // Process delivery request
            sendMsrpMessageDeliveryStatus(remoteId, cpimMsgId,
                    ImdnDocument.DELIVERY_STATUS_DELIVERED, timestamp);
        } else {
            if (ChatUtils.isTextPlainType(contentType)) {
                ChatMessage msg = new ChatMessage(cpimMsgId, remoteId, cpimMsg.getMessageContent(),
                        MimeType.TEXT_MESSAGE, timestamp, timestampSent, pseudo);
                boolean imdnDisplayedRequested = false;
                receive(msg, imdnDisplayedRequested);
            } else {
                if (ChatUtils.isApplicationIsComposingType(contentType)) {
                    // Is composing event
                    receiveIsComposing(remoteId, cpimMsg.getMessageContent().getBytes(UTF8));
                } else {
                    if (ChatUtils.isMessageImdnType(contentType)) {
                        // Delivery report
                        String publicUri = ImsModule.IMS_USER_PROFILE.getPublicUri();
                        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(publicUri);
                        if (number == null) {
                            if (logActivated) {
                                sLogger.error("Cannot parse user contact " + publicUri);
                            }
                        } else {
                            ContactId me = ContactUtil.createContactIdFromValidatedData(number);
                            // Only consider delivery report if sent to me
                            if (localId != null && localId.equals(me)) {
                                receiveMessageDeliveryStatus(remoteId, cpimMsg.getMessageContent());
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
                            boolean imdnDisplayedRequested = false;
                            receive(msg, imdnDisplayedRequested);
                        }
                    }
                }
            }
            // Process delivery request
            if (dispositionNotification != null) {
                if (dispositionNotification.contains(ImdnDocument.POSITIVE_DELIVERY)) {
                    // Positive delivery requested, send MSRP message with status "delivered"
                    sendMsrpMessageDeliveryStatus(remoteId, cpimMsgId,
                            ImdnDocument.DELIVERY_STATUS_DELIVERED, timestamp);
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
    public void abortSession(TerminationReason reason) {
        /*
         * If there is an ongoing group chat session with same chatId, this session has to be
         * silently aborted so after aborting the session we make sure to not call the rest of this
         * method that would otherwise abort the "current" session also and the GroupChat as a whole
         * which is of course not the intention here
         */
        if (!isPendingForRemoval()) {
            super.abortSession(reason);
            return;
        }

        interruptSession();

        terminateSession(reason);

        closeMediaSession();
    }

    @Override
    public void removeSession() {
        mImsModule.getInstantMessagingService().removeSession(this);
    }

}
