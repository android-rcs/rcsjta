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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.api.OneToOneFileTransferImpl;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.text.ParseException;
import java.util.List;

import javax2.sip.message.Response;

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

    private static final Logger sLogger = Logger.getLogger(OneToOneChatSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param contact Remote contact identifier
     * @param remoteContact Remote Contact URI
     * @param firstMsg First chat message
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager Contact manager accessor
     */
    public OneToOneChatSession(InstantMessagingService imService, ContactId contact,
            Uri remoteContact, ChatMessage firstMsg, RcsSettings rcsSettings,
            MessagingLog messagingLog, long timestamp, ContactManager contactManager) {
        super(imService, contact, remoteContact, rcsSettings, messagingLog, firstMsg, timestamp,
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
     * Send a text message
     * 
     * @param msg Chat message
     * @throws NetworkException
     */
    @Override
    public void sendChatMessage(ChatMessage msg) throws NetworkException {
        String from = ChatUtils.ANONYMOUS_URI;
        String to = ChatUtils.ANONYMOUS_URI;
        String msgId = msg.getMessageId();
        String mimeType = msg.getMimeType();
        String networkMimeType = ChatUtils.apiMimeTypeToNetworkMimeType(mimeType);
        long timestampSent = msg.getTimestampSent();
        String networkContent = msg.getContent();
        String data;
        if (MimeType.GEOLOC_MESSAGE.equals(mimeType)) {
            networkContent = ChatUtils.persistedGeolocContentToNetworkGeolocContent(networkContent,
                    msgId, timestampSent);
        }
        if (mImdnManager.isRequestOneToOneDeliveryDisplayedReportsEnabled()) {
            data = ChatUtils.buildCpimMessageWithImdn(from, to, msgId, networkContent,
                    networkMimeType, timestampSent);
        } else if (mImdnManager.isDeliveryDeliveredReportsEnabled()) {
            data = ChatUtils.buildCpimMessageWithoutDisplayedImdn(from, to, msgId, networkContent,
                    networkMimeType, timestampSent);
        } else {
            data = ChatUtils.buildCpimMessage(from, to, networkContent, networkMimeType,
                    timestampSent);
        }

        if (ChatUtils.isGeolocType(networkMimeType)) {
            sendDataChunks(IdGenerator.generateMessageID(), data, CpimMessage.MIME_TYPE,
                    TypeMsrpChunk.GeoLocation);
        } else {
            sendDataChunks(IdGenerator.generateMessageID(), data, CpimMessage.MIME_TYPE,
                    TypeMsrpChunk.TextMessage);
        }
        for (ImsSessionListener listener : getListeners()) {
            ((ChatSessionListener) listener).onMessageSent(msgId, mimeType);
        }
    }

    /**
     * Send file info within a 1-1 chat session
     * 
     * @param fileTransfer
     * @param fileTransferId
     * @param fileInfo
     * @param displayedReportEnabled
     * @param deliveredReportEnabled
     * @throws NetworkException
     */
    public void sendFileInfo(OneToOneFileTransferImpl fileTransfer, String fileTransferId,
            String fileInfo, boolean displayedReportEnabled, boolean deliveredReportEnabled)
            throws NetworkException {
        String networkContent;
        long timestamp = System.currentTimeMillis();
        /* For outgoing file transfer, timestampSent = timestamp */
        long timestampSent = timestamp;
        mMessagingLog.setFileTransferTimestamps(fileTransferId, timestamp, timestampSent);

        if (displayedReportEnabled) {
            networkContent = ChatUtils.buildCpimMessageWithImdn(ChatUtils.ANONYMOUS_URI,
                    ChatUtils.ANONYMOUS_URI, fileTransferId, fileInfo,
                    FileTransferHttpInfoDocument.MIME_TYPE, timestampSent);
        } else if (deliveredReportEnabled) {
            networkContent = ChatUtils.buildCpimMessageWithoutDisplayedImdn(
                    ChatUtils.ANONYMOUS_URI, ChatUtils.ANONYMOUS_URI, fileTransferId, fileInfo,
                    FileTransferHttpInfoDocument.MIME_TYPE, timestampSent);
        } else {
            networkContent = ChatUtils.buildCpimMessage(ChatUtils.ANONYMOUS_URI,
                    ChatUtils.ANONYMOUS_URI, fileInfo, FileTransferHttpInfoDocument.MIME_TYPE,
                    timestampSent);
        }
        sendDataChunks(IdGenerator.generateMessageID(), networkContent, CpimMessage.MIME_TYPE,
                MsrpSession.TypeMsrpChunk.HttpFileSharing);
        fileTransfer.onFileInfoDequeued(getRemoteContact());
    }

    /**
     * Send is composing status
     * 
     * @param status Status
     * @throws NetworkException
     */
    public void sendIsComposingStatus(boolean status) throws NetworkException {
        String content = IsComposingInfo.buildIsComposingInfo(status);
        String msgId = IdGenerator.generateMessageID();
        sendDataChunks(msgId, content, IsComposingInfo.MIME_TYPE,
                MsrpSession.TypeMsrpChunk.IsComposing);
    }

    /**
     * Reject the session invitation
     */
    public void rejectSession() {
        rejectSession(InvitationStatus.INVITATION_REJECTED_BUSY_HERE);
    }

    /**
     * Create INVITE request
     * 
     * @param content Content part
     * @return Request
     * @throws PayloadException
     */
    private SipRequest createMultipartInviteRequest(String content) throws PayloadException {
        try {
            SipRequest invite = SipMessageFactory.createMultipartInvite(getDialogPath(),
                    getFeatureTags(), content, BOUNDARY_TAG);
            invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
            return invite;

        } catch (ParseException e) {
            throw new PayloadException("Failed to create multipart invite request!", e);
        }
    }

    /**
     * Create INVITE request
     * 
     * @param content Content part
     * @return Request
     * @throws PayloadException
     */
    private SipRequest createInviteRequest(String content) throws PayloadException {
        try {
            SipRequest invite = SipMessageFactory.createInvite(getDialogPath(),
                    InstantMessagingService.CHAT_FEATURE_TAGS, content);
            invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
            return invite;

        } catch (ParseException e) {
            throw new PayloadException("Failed to create invite request!", e);
        }
    }

    /**
     * Create an INVITE request
     * 
     * @return the INVITE request
     * @throws PayloadException
     */
    public SipRequest createInvite() throws PayloadException {
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
     * @throws PayloadException
     * @throws NetworkException
     * @throws FileAccessException
     */
    public void handle200OK(SipResponse resp) throws PayloadException, NetworkException,
            FileAccessException {
        super.handle200OK(resp);
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
        if (isSessionInterrupted()) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Data transfer error ").append(error)
                    .append(" for message ").append(msgId).append(" (MSRP chunk type: ")
                    .append(typeMsrpChunk).append(")").toString());
        }

        ContactId remote = getRemoteContact();
        if (TypeMsrpChunk.MessageDeliveredReport.equals(typeMsrpChunk)) {
            for (ImsSessionListener listener : getListeners()) {
                ((OneToOneChatSessionListener) listener).onDeliveryReportSendViaMsrpFailure(msgId,
                        remote, typeMsrpChunk);
            }
        } else if (TypeMsrpChunk.MessageDisplayedReport.equals(typeMsrpChunk)) {
            for (ImsSessionListener listener : getListeners()) {
                ((OneToOneChatSessionListener) listener).onDeliveryReportSendViaMsrpFailure(msgId,
                        remote, typeMsrpChunk);
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
             * Session should not be torn down immediately as there may be more errors to come but
             * as errors occurred we shouldn't use it for sending any longer. RFC 4975 408: An
             * endpoint MUST treat a 408 response in the same manner as it would treat a local
             * timeout. 413: If a message sender receives a 413 in a response, or in a REPORT
             * request, it MUST NOT send any further chunks in the message, that is, any further
             * chunks with the same Message-ID value. If the sender receives the 413 while in the
             * process of sending a chunk, and the chunk is interruptible, the sender MUST interrupt
             * it.
             */

            errorCode = ChatError.MEDIA_SESSION_BROKEN;
        } else {
            /*
             * Default error; used e.g. for 481 or any other error RFC 4975 481: A 481 response
             * indicates that the indicated session does not exist. The sender should terminate the
             * session.
             */

            errorCode = ChatError.MEDIA_SESSION_FAILED;
        }

        handleError(getFirstMessage(), new ChatError(errorCode, error));

        /* Request capabilities to the remote */
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getRemoteContact());

    }

    @Override
    public void receiveBye(SipRequest bye) throws PayloadException, NetworkException {
        super.receiveBye(bye);
        ContactId remote = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            listener.onSessionAborted(remote, TerminationReason.TERMINATION_BY_REMOTE);
        }
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(remote);
    }

    @Override
    public void receiveCancel(SipRequest cancel) throws NetworkException, PayloadException {
        super.receiveCancel(cancel);
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getRemoteContact());
    }

    /**
     * Handle error
     * 
     * @param error Error
     */
    @Override
    public void handleError(ImsServiceError error) {
        if (isSessionInterrupted()) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Session error: ").append(error.getErrorCode())
                    .append(", reason=").append(error.getMessage()).toString());
        }
        closeMediaSession();
        removeSession();

        handleError(getFirstMessage(), new ChatError(error));
    }

    /**
     * Handle error
     * 
     * @param msg ChatMessage that errored
     * @param error Error
     */
    private void handleError(ChatMessage msg, ChatError error) {
        String msgId = msg.getMessageId();
        for (ImsSessionListener listener : getListeners()) {
            ((OneToOneChatSessionListener) listener).onImError(error, msgId, msg.getMimeType());
        }
    }
}
