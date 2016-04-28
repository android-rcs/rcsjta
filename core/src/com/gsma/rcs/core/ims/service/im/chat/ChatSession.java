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

import static com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession.isFileCapacityAcceptable;
import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionActivityManager;
import com.gsma.rcs.core.ims.service.capability.Capabilities.CapabilitiesBuilder;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.core.ims.service.im.chat.iscomposing.IsComposingManager;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.DownloadFromInviteFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpThumbnail;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;

import android.net.Uri;

import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Chat session
 *
 * @author jexa7410
 */
public abstract class ChatSession extends ImsServiceSession implements MsrpEventListener {

    private String mSubject;
    private final MsrpManager mMsrpMgr;
    private final IsComposingManager mIsComposingMgr = new IsComposingManager(this);
    private final SessionActivityManager mActivityMgr;
    private String mContributionId;
    private List<String> mFeatureTags = new ArrayList<>();
    private List<String> mAcceptContactTags = new ArrayList<>();
    private String mAcceptTypes = "";
    private String mWrappedTypes = "";
    private static final Logger sLogger = Logger.getLogger(ChatSession.class.getSimpleName());

    protected final MessagingLog mMessagingLog;

    /**
     * First chat message
     */
    private final ChatMessage mFirstMsg;

    protected final InstantMessagingService mImService;

    protected final ImdnManager mImdnManager;

    /**
     * Receive chat message
     *
     * @param msg Chat message
     * @param msgSupportsImdnReport True if the message type supports imdn reports
     * @param imdnDisplayedRequested Indicates whether display report was requested
     * @param cpimMsgId Cpim message Id
     * @param timestamp Message receive time
     */
    protected void receive(ChatMessage msg, ContactId remoteId, boolean msgSupportsImdnReport,
            boolean imdnDisplayedRequested, String cpimMsgId, long timestamp) {
        if (mMessagingLog.isMessagePersisted(msg.getMessageId())) {
            // Message already received
            return;
        }
        // Is composing event is reset
        mIsComposingMgr.receiveIsComposingEvent(msg.getRemoteContact(), false);
        if (msgSupportsImdnReport && mImdnManager.isDeliveryDeliveredReportsEnabled()) {
            try {
                sendMsrpMessageDeliveryStatus(remoteId, cpimMsgId,
                        ImdnDocument.DeliveryStatus.DELIVERED, timestamp);
                for (ImsSessionListener listener : getListeners()) {
                    ((ChatSessionListener) listener).onMessageReceived(msg, imdnDisplayedRequested,
                            true);
                }
            } catch (NetworkException e) {
                for (ImsSessionListener listener : getListeners()) {
                    ((ChatSessionListener) listener).onMessageReceived(msg, imdnDisplayedRequested,
                            false);
                }
            }
        }
    }

    /**
     * Constructor
     *
     * @param imService InstantMessagingService
     * @param contact Remote contactId
     * @param remoteContact Remote Contact URI
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param firstMsg First message in session
     * @param timestamp Local timestamp for the session
     * @param contactManager The contact manager accessor
     */
    public ChatSession(InstantMessagingService imService, ContactId contact, Uri remoteContact,
            RcsSettings rcsSettings, MessagingLog messagingLog, ChatMessage firstMsg,
            long timestamp, ContactManager contactManager) {
        super(imService, contact, remoteContact, rcsSettings, timestamp, contactManager);

        mImService = imService;
        mImdnManager = imService.getImdnManager();
        mMessagingLog = messagingLog;
        mActivityMgr = new SessionActivityManager(this, mRcsSettings.getChatIdleDuration());

        // Create the MSRP manager
        int localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort(rcsSettings);
        String localIpAddress = mImService.getImsModule().getCurrentNetworkInterface()
                .getNetworkAccess().getIpAddress();
        mMsrpMgr = new MsrpManager(localIpAddress, localMsrpPort, imService, rcsSettings);
        if (imService.getImsModule().isConnectedToWifiAccess()) {
            mMsrpMgr.setSecured(rcsSettings.isSecureMsrpOverWifi());
        }
        mFirstMsg = firstMsg;
    }

    /**
     * Get feature tags
     *
     * @return Feature tags
     */
    public String[] getFeatureTags() {
        return mFeatureTags.toArray(new String[mFeatureTags.size()]);
    }

    /**
     * Get Accept-Contact tags
     *
     * @return Feature tags
     */
    public String[] getAcceptContactTags() {
        return mAcceptContactTags.toArray(new String[mAcceptContactTags.size()]);
    }

    /**
     * Set feature tags
     *
     * @param tags Feature tags
     */
    public void setFeatureTags(List<String> tags) {
        mFeatureTags = tags;
    }

    /**
     * Set Accept-Contact tags
     *
     * @param tags Feature tags
     */
    public void setAcceptContactTags(List<String> tags) {
        mAcceptContactTags = tags;
    }

    /**
     * Get accept types
     *
     * @return Accept types
     */
    protected String getAcceptTypes() {
        return mAcceptTypes;
    }

    /**
     * Add types to accept types
     *
     * @param types Accept types
     */
    protected void addAcceptTypes(String types) {
        if (mAcceptTypes.isEmpty()) {
            mAcceptTypes = types;
        } else {
            mAcceptTypes += " " + types;
        }
    }

    /**
     * Get wrapped types
     *
     * @return Wrapped types
     */
    protected String getWrappedTypes() {
        return mWrappedTypes;
    }

    /**
     * Add types to wrapped types
     *
     * @param types Wrapped types
     */
    protected void addWrappedTypes(String types) {
        if (mWrappedTypes.isEmpty()) {
            mWrappedTypes = types;
        } else {
            mWrappedTypes += " " + types;
        }
    }

    /**
     * Returns the subject of the session
     *
     * @return String
     */
    public String getSubject() {
        return mSubject;
    }

    /**
     * Set the subject of the session
     *
     * @param subject Subject
     */
    public void setSubject(String subject) {
        mSubject = subject;
    }

    /**
     * Returns the session activity manager
     *
     * @return Activity manager
     */
    public SessionActivityManager getActivityManager() {
        return mActivityMgr;
    }

    /**
     * Return the contribution ID
     *
     * @return Contribution ID
     */
    public String getContributionID() {
        return mContributionId;
    }

    /**
     * Set the contribution ID
     *
     * @param id Contribution ID
     */
    public void setContributionID(String id) {
        mContributionId = id;
    }

    /**
     * Returns the IM session identity
     *
     * @return Identity (e.g. SIP-URI)
     */
    public String getImSessionIdentity() {
        if (getDialogPath() != null) {
            return getDialogPath().getTarget();
        }
        return null;
    }

    /**
     * Returns the MSRP manager
     *
     * @return MSRP manager
     */
    public MsrpManager getMsrpMgr() {
        return mMsrpMgr;
    }

    /**
     * Get first message.
     *
     * @return first message
     */
    public ChatMessage getFirstMessage() {
        return mFirstMsg;
    }

    /**
     * Close the MSRP session
     */
    public void closeMsrpSession() {
        if (getMsrpMgr() != null) {
            getMsrpMgr().closeSession();
            if (sLogger.isActivated()) {
                sLogger.debug("MSRP session has been closed");
            }
        }
    }

    /**
     * Handle 480 Temporarily Unavailable
     *
     * @param resp 480 response
     */
    public void handle480Unavailable(SipResponse resp) {
        handleError(new ChatError(ChatError.SESSION_INITIATION_DECLINED, resp.getReasonPhrase()));
    }

    /**
     * Handle 486 Busy
     *
     * @param resp 486 response
     */
    public void handle486Busy(SipResponse resp) {
        handleError(new ChatError(ChatError.SESSION_INITIATION_DECLINED, resp.getReasonPhrase()));
    }

    /**
     * Handle 603 Decline
     *
     * @param resp 603 response
     */
    public void handle603Declined(SipResponse resp) {
        handleDefaultError(resp);
    }

    /**
     * Data has been transferred
     *
     * @param msgId Message ID
     */
    public void msrpDataTransferred(String msgId) {
        // Update the activity manager
        mActivityMgr.updateActivity();
    }

    /**
     * Session inactivity event
     *
     * @throws NetworkException
     * @throws PayloadException
     */
    @Override
    public void handleInactivityEvent() throws PayloadException, NetworkException {
        terminateSession(TerminationReason.TERMINATION_BY_INACTIVITY);
    }

    /**
     * Data transfer has been received
     *
     * @param msgId Message ID
     * @param data Received data
     * @param mimeType Data mime-type
     * @throws NetworkException
     * @throws PayloadException
     * @throws ContactManagerException
     */
    public abstract void receiveMsrpData(String msgId, byte[] data, String mimeType)
            throws PayloadException, NetworkException, ContactManagerException;

    /**
     * Data transfer in progress
     *
     * @param currentSize Current transferred size in bytes
     * @param totalSize Total size in bytes
     */
    public void msrpTransferProgress(long currentSize, long totalSize) {
        // Not used by chat
    }

    /**
     * Data transfer in progress
     *
     * @param currentSize Current transferred size in bytes
     * @param totalSize Total size in bytes
     * @param data received data chunk
     * @return always false TODO
     */
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        // Not used by chat
        return false;
    }

    /**
     * Data transfer has been aborted
     */
    public void msrpTransferAborted() {
        // Not used by chat
    }

    /**
     * Receive is composing event
     *
     * @param contact Contact
     * @param event Event
     * @throws PayloadException
     */
    protected void receiveIsComposing(ContactId contact, byte[] event) throws PayloadException {
        mIsComposingMgr.receiveIsComposingEvent(contact, event);
    }

    /**
     * Send an empty data chunk
     *
     * @throws NetworkException
     */
    public void sendEmptyDataChunk() throws NetworkException {
        mMsrpMgr.sendEmptyChunk();
    }

    private void handleFileTransferInvitationRejected(ContactId contact, MmContent content,
            MmContent fileIcon, FileTransfer.ReasonCode reasonCode, long timestamp,
            long timestampSent) {
        mImService.addFileTransferInvitationRejected(contact, content, fileIcon, reasonCode,
                timestamp, timestampSent);
    }

    private void handleResendFileTransferInvitationRejected(String fileTransferId,
            FileTransfer.ReasonCode reasonCode, long timestamp, long timestampSent) {
        mImService.setResendFileTransferInvitationRejected(fileTransferId, reasonCode, timestamp,
                timestampSent);
    }

    /**
     * Receive HTTP file transfer event
     *
     * @param contact the remote contact
     * @param displayName Display Name
     * @param fileTransferInfo Information of on file to transfer over HTTP
     * @param msgId Message ID
     * @param timestamp The local timestamp of the message for receiving a HttpFileTransfer
     * @param timestampSent Remote timestamp sent in payload for receiving a HttpFileTransfer
     * @throws PayloadException
     * @throws ContactManagerException
     * @throws NetworkException
     */
    protected void receiveHttpFileTransfer(ContactId contact, String displayName,
            FileTransferHttpInfoDocument fileTransferInfo, String msgId, long timestamp,
            long timestampSent) throws PayloadException, ContactManagerException, NetworkException {
        try {
            /*
             * Update the remote contact's capabilities to include at least HTTP FT and IM session
             * capabilities as we have just received a HTTP file transfer invitation from this
             * contact so he/she must at least have this capability. We do not need any capability
             * exchange response to determine that.
             */
            mContactManager.mergeContactCapabilities(contact,
                    new CapabilitiesBuilder().setImSession(true).setFileTransferHttp(true)
                            .setTimestampOfLastResponse(timestamp).build(), RcsStatus.RCS_CAPABLE,
                    RegistrationState.ONLINE, displayName);

            FileTransferHttpThumbnail fileTransferHttpThumbnail = fileTransferInfo
                    .getFileThumbnail();
            if (mImService.isFileTransferAlreadyOngoing(msgId)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("File transfer with fileTransferId '" + msgId
                            + "' already ongoing, so ignoring this one.");
                }
                return;
            }
            boolean fileResent = mImService.isFileTransferResentAndNotAlreadyOngoing(msgId);
            if (mContactManager.isBlockedForContact(contact)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Contact " + contact
                            + " is blocked, reject the HTTP File transfer");
                }
                if (fileResent) {
                    handleResendFileTransferInvitationRejected(msgId, ReasonCode.REJECTED_SPAM,
                            timestamp, timestampSent);
                    return;
                }
                MmContent fileIconContent = (fileTransferHttpThumbnail == null) ? null
                        : fileTransferHttpThumbnail.getLocalMmContent(msgId);
                handleFileTransferInvitationRejected(contact, fileTransferInfo.getLocalMmContent(),
                        fileIconContent, ReasonCode.REJECTED_SPAM, timestamp, timestampSent);
                return;
            }

            /* Auto reject if file too big or size exceeds device storage capacity. */
            FileSharingError error = isFileCapacityAcceptable(fileTransferInfo.getSize(),
                    mRcsSettings);
            if (error != null) {
                if (sLogger.isActivated()) {
                    sLogger.debug("File is too big or exceeds storage capacity, "
                            .concat("reject the HTTP File transfer."));
                }
                MmContent fileIconContent = (fileTransferHttpThumbnail == null) ? null
                        : fileTransferHttpThumbnail.getLocalMmContent(msgId);

                int errorCode = error.getErrorCode();
                switch (errorCode) {
                    case FileSharingError.MEDIA_SIZE_TOO_BIG:
                        if (fileResent) {
                            handleResendFileTransferInvitationRejected(msgId,
                                    ReasonCode.REJECTED_MAX_SIZE, timestamp, timestampSent);
                            break;
                        }
                        handleFileTransferInvitationRejected(contact,
                                fileTransferInfo.getLocalMmContent(), fileIconContent,
                                ReasonCode.REJECTED_MAX_SIZE, timestamp, timestampSent);
                        break;
                    case FileSharingError.NOT_ENOUGH_STORAGE_SPACE:
                        if (fileResent) {
                            handleResendFileTransferInvitationRejected(msgId,
                                    ReasonCode.REJECTED_LOW_SPACE, timestamp, timestampSent);
                            break;
                        }
                        handleFileTransferInvitationRejected(contact,
                                fileTransferInfo.getLocalMmContent(), fileIconContent,
                                ReasonCode.REJECTED_LOW_SPACE, timestamp, timestampSent);
                        break;
                    default:
                        if (sLogger.isActivated()) {
                            sLogger.error("Unexpected error while receiving HTTP file transfer."
                                    .concat(Integer.toString(errorCode)));
                        }
                }
                return;
            }
            if (!mImService.getImsModule().getInstantMessagingService()
                    .isFileTransferSessionAvailable()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Max number of File Transfer reached, reject the HTTP File transfer");
                }
                MmContent fileIconContent = (fileTransferHttpThumbnail == null) ? null
                        : fileTransferHttpThumbnail.getLocalMmContent(msgId);
                if (fileResent) {
                    handleResendFileTransferInvitationRejected(msgId,
                            ReasonCode.REJECTED_MAX_FILE_TRANSFERS, timestamp, timestampSent);
                    return;
                }
                handleFileTransferInvitationRejected(contact, fileTransferInfo.getLocalMmContent(),
                        fileIconContent, ReasonCode.REJECTED_MAX_FILE_TRANSFERS, timestamp,
                        timestampSent);
                return;
            }
            DownloadFromInviteFileSharingSession fileSession = new DownloadFromInviteFileSharingSession(
                    mImService, this, fileTransferInfo, msgId, contact, displayName, mRcsSettings,
                    mMessagingLog, timestamp, timestampSent, mContactManager);
            if (fileTransferHttpThumbnail != null) {
                try {
                    fileSession.downloadFileIcon();
                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to download file icon! (" + e.getMessage() + ")");
                    }
                    MmContent fileIconContent = fileTransferHttpThumbnail.getLocalMmContent(msgId);
                    if (fileResent) {
                        handleResendFileTransferInvitationRejected(msgId,
                                ReasonCode.REJECTED_MAX_FILE_TRANSFERS, timestamp, timestampSent);
                        return;
                    }
                    handleFileTransferInvitationRejected(contact,
                            fileTransferInfo.getLocalMmContent(), fileIconContent,
                            ReasonCode.REJECTED_MEDIA_FAILED, timestamp, timestampSent);
                    return;
                }
            }
            if (fileResent) {
                mImService.receiveResendFileTransferInvitation(fileSession, contact, displayName);
            } else {
                mImService.receiveFileTransferInvitation(fileSession, isGroupChat(), contact,
                        displayName);
            }

            if (mImdnManager.isDeliveryDeliveredReportsEnabled()) {
                sendMsrpMessageDeliveryStatus(contact, msgId,
                        ImdnDocument.DeliveryStatus.DELIVERED, timestamp);
            }
            fileSession.startSession();

        } catch (FileAccessException e) {
            throw new PayloadException("Failed to receive file transfer with fileTransferId : "
                    + msgId + "for contact : " + contact, e);
        }
    }

    /**
     * Send data chunk with a specified MIME type
     *
     * @param msgId Message ID
     * @param data Data
     * @param mime MIME type
     * @param typeMsrpChunk Type of MSRP chunk
     * @throws NetworkException
     */
    public void sendDataChunks(String msgId, String data, String mime, TypeMsrpChunk typeMsrpChunk)
            throws NetworkException {
        byte[] bytes = data.getBytes(UTF8);
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        mMsrpMgr.sendChunks(stream, msgId, mime, bytes.length, typeMsrpChunk);
    }

    /**
     * Is group chat
     *
     * @return Boolean
     */
    public abstract boolean isGroupChat();

    /**
     * Send a chat message
     *
     * @param msg Chat message
     * @throws NetworkException
     */
    public abstract void sendChatMessage(ChatMessage msg) throws NetworkException;

    /**
     * Send is composing status
     *
     * @param status Status
     * @throws NetworkException
     */
    public abstract void sendIsComposingStatus(boolean status) throws NetworkException;

    /**
     * Send message delivery status via MSRP
     *
     * @param remote Contact that requested the delivery status
     * @param msgId Message ID
     * @param status Status
     * @param timestamp Timestamp sent in payload for IMDN datetime
     * @throws NetworkException
     */
    public abstract void sendMsrpMessageDeliveryStatus(ContactId remote, String msgId,
            ImdnDocument.DeliveryStatus status, long timestamp) throws NetworkException;

    /**
     * Receive a message delivery status from an XML document
     *
     * @param contact Contact identifier
     * @param xml XML document
     * @throws PayloadException
     */
    public void onDeliveryStatusReceived(ContactId contact, String xml) throws PayloadException {
        try {
            ImdnDocument imdn = ChatUtils.parseDeliveryReport(xml);
            for (ImsSessionListener listener : getListeners()) {
                ((ChatSessionListener) listener).onDeliveryStatusReceived(mContributionId, contact,
                        imdn);
            }
        } catch (SAXException | ParserConfigurationException | ParseFailureException e) {
            throw new PayloadException("Failed to parse IMDN document for contact : " + contact, e);
        }
    }

    /**
     * Prepare media session
     */
    public void prepareMediaSession() {
        // Get the remote SDP part
        byte[] sdp = getDialogPath().getRemoteContent().getBytes(UTF8);
        // Create the MSRP session
        MsrpSession session = getMsrpMgr().createMsrpSession(sdp, this);
        session.setFailureReportOption(false);
        session.setSuccessReportOption(false);
    }

    /**
     * Open media session
     *
     * @throws PayloadException
     * @throws NetworkException
     */
    public void openMediaSession() throws PayloadException, NetworkException {
        getMsrpMgr().openMsrpSession();
        sendEmptyDataChunk();
    }

    /**
     * Start media transfer
     */
    public void startMediaTransfer() {
        /* Not used here */
    }

    /**
     * Reject the session invitation
     */
    public abstract void rejectSession();

    /**
     * Is media session established
     *
     * @return true If the empty packet was sent successfully
     */
    public boolean isMediaEstablished() {
        return getMsrpMgr().isEstablished() && !getDialogPath().isSessionTerminated();
    }
}
