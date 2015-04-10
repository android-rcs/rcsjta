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
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Collection;
import java.util.List;

/**
 * Abstract 1-1 chat session
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class OneToOneChatSession extends ChatSession {
    /**
     * Boundary tag
     */
    private final static String BOUNDARY_TAG = "boundary1";

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param contact Remote contact identifier
     * @param remoteUri Remote URI
     * @param firstMsg First chat message
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public OneToOneChatSession(ImsService parent, ContactId contact, String remoteUri,
            ChatMessage firstMsg, RcsSettings rcsSettings, MessagingLog messagingLog,
            long timestamp, ContactsManager contactManager) {
        super(parent, contact, remoteUri, rcsSettings, messagingLog, firstMsg, timestamp,
                contactManager);

        List<String> featureTags = ChatUtils.getSupportedFeatureTagsForChat(rcsSettings);
        setFeatureTags(featureTags);

        setAcceptContactTags(featureTags);

        String acceptTypes = new StringBuilder(CpimMessage.MIME_TYPE).append(" ")
                .append(IsComposingInfo.MIME_TYPE).toString();
        setAcceptTypes(acceptTypes);

        StringBuilder wrappedTypes = new StringBuilder(MimeType.TEXT_MESSAGE).append(" ").append(
                ImdnDocument.MIME_TYPE);
        if (mRcsSettings.isGeoLocationPushSupported()) {
            wrappedTypes.append(" ").append(GeolocInfoDocument.MIME_TYPE);
        }
        if (mRcsSettings.isFileTransferHttpSupported()) {
            wrappedTypes.append(" ").append(FileTransferHttpInfoDocument.MIME_TYPE);
        }
        setWrappedTypes(wrappedTypes.toString());
    }

    /**
     * Is group chat
     * 
     * @return Boolean
     */
    public boolean isGroupChat() {
        return false;
    }

    /**
     * Close media session
     */
    public void closeMediaSession() {
        // Stop the activity manager
        getActivityManager().stop();

        // Close MSRP session
        closeMsrpSession();
    }

    /**
     * Handle IncomingSessionInitiation error
     * 
     * @param error Error
     */
    public void handleIncomingSessionInitiationError(ImsServiceError error) {
        if (isSessionInterrupted()) {
            return;
        }
        closeMediaSession();
        removeSession();
        for (ImsSessionListener listener : getListeners()) {
            ((OneToOneChatSessionListener) listener)
                    .handleIncomingSessionInitiationError(new ChatError(error));
        }

    }

    /**
     * Send a text message
     * 
     * @param msg Chat message
     */
    @Override
    public void sendChatMessage(ChatMessage msg) {
        String from = ChatUtils.ANOMYNOUS_URI;
        String to = ChatUtils.ANOMYNOUS_URI;
        String msgId = msg.getMessageId();
        String networkContent;
        boolean useImdn = getImdnManager().isImdnActivated() && !mRcsSettings.isAlbatrosRelease();
        String mimeType = msg.getMimeType();
        if (useImdn) {
            networkContent = ChatUtils.buildCpimMessageWithImdn(from, to, msgId, msg.getContent(),
                    mimeType, msg.getTimestampSent());

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

    /**
     * Send is composing status
     * 
     * @param status Status
     */
    public void sendIsComposingStatus(boolean status) {
        String content = IsComposingInfo.buildIsComposingInfo(status);
        String msgId = IdGenerator.generateMessageID();
        sendDataChunks(msgId, content, IsComposingInfo.MIME_TYPE,
                MsrpSession.TypeMsrpChunk.IsComposing);
    }

    /**
     * Reject the session invitation
     */
    public void rejectSession() {
        rejectSession(486);
    }

    /**
     * Create INVITE request
     * 
     * @param content Content part
     * @return Request
     * @throws SipException
     */
    private SipRequest createMultipartInviteRequest(String content) throws SipException {
        SipRequest invite = SipMessageFactory.createMultipartInvite(getDialogPath(),
                getFeatureTags(), content, BOUNDARY_TAG);

        // Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());

        return invite;
    }

    /**
     * Create INVITE request
     * 
     * @param content Content part
     * @return Request
     * @throws SipException
     */
    private SipRequest createInviteRequest(String content) throws SipException {
        SipRequest invite = SipMessageFactory.createInvite(getDialogPath(),
                InstantMessagingService.CHAT_FEATURE_TAGS, content);

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
        // If there is a first message then builds a multipart content else
        // builds a SDP content
        String content = getDialogPath().getLocalContent();
        if (getFirstMessage() != null) {
            return createMultipartInviteRequest(content);
        }
        return createInviteRequest(content);
    }

    /**
     * Handle 200 0K response
     * 
     * @param resp 200 OK response
     */
    public void handle200OK(SipResponse resp) {
        super.handle200OK(resp);

        // Start the activity manager
        getActivityManager().start();
    }

    /**
     * Get SDP direction
     * 
     * @return Direction
     * @see com.gsma.rcs.core.ims.protocol.sdp.SdpUtils#DIRECTION_RECVONLY
     * @see com.gsma.rcs.core.ims.protocol.sdp.SdpUtils#DIRECTION_SENDONLY
     * @see com.gsma.rcs.core.ims.protocol.sdp.SdpUtils#DIRECTION_SENDRECV
     */
    public abstract String getSdpDirection();

    @Override
    public void msrpTransferError(String msgId, String error,
            MsrpSession.TypeMsrpChunk typeMsrpChunk) {
        super.msrpTransferError(msgId, error, typeMsrpChunk);

        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getRemoteContact());

    }

    @Override
    public void receiveBye(SipRequest bye) {
        super.receiveBye(bye);

        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getRemoteContact());
    }

    @Override
    public void receiveCancel(SipRequest cancel) {
        super.receiveCancel(cancel);

        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getRemoteContact());
    }
}
