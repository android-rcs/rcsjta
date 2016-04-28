/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimParser;
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
        addAcceptTypes(CpimMessage.MIME_TYPE);
        addAcceptTypes(IsComposingInfo.MIME_TYPE);
        addWrappedTypes(MimeType.TEXT_MESSAGE);
        addWrappedTypes(ImdnDocument.MIME_TYPE);
        if (mRcsSettings.isGeoLocationPushSupported()) {
            addWrappedTypes(GeolocInfoDocument.MIME_TYPE);
        }
        if (mRcsSettings.isFileTransferHttpSupported()) {
            addWrappedTypes(FileTransferHttpInfoDocument.MIME_TYPE);
        }
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
        getActivityManager().stop();
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
     * @param fileTransfer the file transfer API implementation
     * @param fileTransferId the file transfer ID
     * @param fileInfo the file transfer information
     * @param displayedReportEnabled is displayed report enabled
     * @param deliveredReportEnabled is delivered report enabled
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

    @Override
    public void sendMsrpMessageDeliveryStatus(ContactId remote, String msgId,
            ImdnDocument.DeliveryStatus status, long timestamp) throws NetworkException {
        String fromUri = ChatUtils.ANONYMOUS_URI;
        String toUri = ChatUtils.ANONYMOUS_URI;
        if (sLogger.isActivated()) {
            sLogger.debug("Send delivery status " + status + " for message " + msgId);
        }
        // Send status in CPIM + IMDN headers
        /* Timestamp fo IMDN datetime */
        String imdn = ChatUtils.buildImdnDeliveryReport(msgId, status, timestamp);
        /* Timestamp for CPIM DateTime */
        String content = ChatUtils.buildCpimDeliveryReport(fromUri, toUri, imdn,
                System.currentTimeMillis());

        TypeMsrpChunk typeMsrpChunk = TypeMsrpChunk.OtherMessageDeliveredReportStatus;
        if (ImdnDocument.DeliveryStatus.DISPLAYED == status) {
            typeMsrpChunk = TypeMsrpChunk.MessageDisplayedReport;
        } else {
            if (ImdnDocument.DeliveryStatus.DELIVERED == status) {
                typeMsrpChunk = TypeMsrpChunk.MessageDeliveredReport;
            }
        }
        sendDataChunks(IdGenerator.generateMessageID(), content, CpimMessage.MIME_TYPE,
                typeMsrpChunk);
        if (ImdnDocument.DeliveryStatus.DISPLAYED == status) {
            if (mMessagingLog.isMessagePersisted(msgId)) {
                for (ImsSessionListener listener : getListeners()) {
                    ((ChatSessionListener) listener).onChatMessageDisplayReportSent(msgId);
                }
            }
        }
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
            sLogger.info("Data transfer error " + error + " for message " + msgId
                    + " (MSRP chunk type: " + typeMsrpChunk + ")");
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
        } else if (msgId != null && TypeMsrpChunk.TextMessage.equals(typeMsrpChunk)) {
            for (ImsSessionListener listener : getListeners()) {
                ImdnDocument imdn = new ImdnDocument(msgId, ImdnDocument.DELIVERY_NOTIFICATION,
                        ImdnDocument.DeliveryStatus.FAILED, ImdnDocument.IMDN_DATETIME_NOT_SET);
                ((ChatSessionListener) listener).onMessageDeliveryStatusReceived(null, imdn);
            }
        } else {
            // do nothing
            sLogger.error("MSRP transfer error not handled for message '" + msgId
                    + "' and chunk type : '" + typeMsrpChunk + "'!");
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
    public void receiveMsrpData(String msgId, byte[] data, String mimeType)
            throws PayloadException, NetworkException, ContactManagerException {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("MSRP data received (type " + mimeType + ")");
        }
        getActivityManager().updateActivity();
        if (data == null || data.length == 0) {
            if (logActivated) {
                sLogger.debug("By-pass received empty data");
            }
            return;
        }
        if (ChatUtils.isApplicationIsComposingType(mimeType)) {
            receiveIsComposing(getRemoteContact(), data);
            return;
        }
        if (ChatUtils.isTextPlainType(mimeType)) {
            /**
             * Set message's timestamp to the System.currentTimeMillis, not the session's itself
             * timestamp
             */
            long timestamp = System.currentTimeMillis();
            /**
             * Since legacy server can send non CPIM data (like plain text without timestamp) in the
             * payload, we need to fake timesampSent by using the local timestamp even if this is
             * not the real proper timestamp from the remote side in this case.
             */
            ChatMessage msg = new ChatMessage(msgId, getRemoteContact(), new String(data, UTF8),
                    MimeType.TEXT_MESSAGE, timestamp, timestamp, null);
            boolean imdnDisplayedRequested = false;
            boolean msgSupportsImdnReport = false;
            receive(msg, null, msgSupportsImdnReport, imdnDisplayedRequested, null, timestamp);
            return;
        }
        if (ChatUtils.isMessageCpimType(mimeType)) {
            CpimParser cpimParser = new CpimParser(data);
            CpimMessage cpimMsg = cpimParser.getCpimMessage();
            if (cpimMsg != null) {
                String cpimMsgId = cpimMsg.getHeader(ImdnUtils.HEADER_IMDN_MSG_ID);
                if (cpimMsgId == null) {
                    cpimMsgId = msgId;
                }
                String contentType = cpimMsg.getContentType();
                ContactId contact = getRemoteContact();
                /*
                 * In One to One chat, the MSRP 'from' header is '<sip:anonymous@anonymous.invalid>'
                 */
                boolean imdnDisplayedRequested = false;
                boolean msgSupportsImdnReport = true;
                String dispositionNotification = cpimMsg
                        .getHeader(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);
                if (dispositionNotification != null
                        && dispositionNotification.contains(ImdnDocument.DISPLAY)
                        && mImdnManager.isSendOneToOneDeliveryDisplayedReportsEnabled()) {
                    imdnDisplayedRequested = true;
                }
                boolean isFToHTTP = FileTransferUtils.isFileTransferHttpType(contentType);
                /**
                 * Set message's timestamp to the System.currentTimeMillis, not the session's itself
                 * timestamp
                 */
                long timestamp = System.currentTimeMillis();
                long timestampSent = cpimMsg.getTimestampSent();
                if (isFToHTTP) {
                    FileTransferHttpInfoDocument fileInfo = FileTransferUtils
                            .parseFileTransferHttpDocument(
                                    cpimMsg.getMessageContent().getBytes(UTF8), mRcsSettings);
                    if (fileInfo != null) {
                        receiveHttpFileTransfer(contact, getRemoteDisplayName(), fileInfo,
                                cpimMsgId, timestamp, timestampSent);
                        if (imdnDisplayedRequested) {
                            // TODO set File Transfer status to DISPLAY_REPORT_REQUESTED ??
                        }
                    } else {
                        // TODO : else return error to Originating side
                    }
                } else {
                    if (ChatUtils.isTextPlainType(contentType)) {
                        ChatMessage msg = new ChatMessage(cpimMsgId, contact,
                                cpimMsg.getMessageContent(), MimeType.TEXT_MESSAGE, timestamp,
                                timestampSent, null);
                        receive(msg, null, msgSupportsImdnReport, imdnDisplayedRequested,
                                cpimMsgId, timestamp);

                    } else if (ChatUtils.isApplicationIsComposingType(contentType)) {
                        receiveIsComposing(contact, cpimMsg.getMessageContent().getBytes(UTF8));

                    } else if (ChatUtils.isMessageImdnType(contentType)) {
                        onDeliveryStatusReceived(contact, cpimMsg.getMessageContent());

                    } else if (ChatUtils.isGeolocType(contentType)) {
                        ChatMessage msg = new ChatMessage(cpimMsgId, contact,
                                ChatUtils.networkGeolocContentToPersistedGeolocContent(cpimMsg
                                        .getMessageContent()), MimeType.GEOLOC_MESSAGE, timestamp,
                                timestampSent, null);
                        receive(msg, null, msgSupportsImdnReport, imdnDisplayedRequested,
                                cpimMsgId, timestamp);
                    }
                }
            }
        } else {
            if (logActivated) {
                sLogger.debug("Not supported content " + mimeType + " in chat session");
            }
        }
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
            sLogger.info("Session error: " + error.getErrorCode() + ", reason="
                    + error.getMessage());
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
