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

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatError;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSessionListener;
import com.gsma.rcs.core.ims.service.im.chat.SessionUnavailableException;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardOneToOneChatMessageSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.history.HistoryLog;
import com.gsma.rcs.provider.messaging.ChatMessagePersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.ImSessionStartMode;
import com.gsma.rcs.service.broadcaster.IOneToOneChatEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMode;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.chat.IOneToOneChat;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import android.os.RemoteException;
import android.text.TextUtils;

/**
 * One-to-One Chat implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class OneToOneChatImpl extends IOneToOneChat.Stub implements OneToOneChatSessionListener {

    private final ContactId mContact;

    private final IOneToOneChatEventBroadcaster mBroadcaster;

    private final InstantMessagingService mImService;

    private final MessagingLog mMessagingLog;

    private final ChatServiceImpl mChatService;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(OneToOneChatImpl.class.getName());

    /**
     * Constructor
     * 
     * @param contact Remote contact ID
     * @param broadcaster IChatEventBroadcaster
     * @param imService InstantMessagingService
     * @param messagingLog MessagingLog
     * @param historyLog HistoryLog
     * @param rcsSettings RcsSettings
     * @param chatService ChatServiceImpl
     * @param contactManager ContactManager
     * @param core Core
     * @param undeliveredImManager OneToOneUndeliveredImManager
     */
    public OneToOneChatImpl(InstantMessagingService imService, ContactId contact,
            IOneToOneChatEventBroadcaster broadcaster, MessagingLog messagingLog,
            HistoryLog historyLog, RcsSettings rcsSettings, ChatServiceImpl chatService,
            ContactManager contactManager) {
        mImService = imService;
        mContact = contact;
        mBroadcaster = broadcaster;
        mMessagingLog = messagingLog;
        mChatService = chatService;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
    }

    private void sendChatMessageInNewSession(final ChatMessage msg) throws PayloadException,
            NetworkException {
        final OneToOneChatSession newSession = mImService.createOneToOneChatSession(mContact, msg);
        newSession.addListener(OneToOneChatImpl.this);
        newSession.startSession();
        onMessageSent(msg.getMessageId(), msg.getMimeType());
    }

    private void sendChatMessageWithinSession(final OneToOneChatSession session,
            final ChatMessage msg) throws NetworkException {
        session.sendChatMessage(msg);
    }

    /**
     * Resends a chat message
     * 
     * @param msg Message
     * @throws PayloadException
     * @throws NetworkException
     * @throws FileAccessException
     */
    private void resendChatMessage(final ChatMessage msg) throws PayloadException,
            NetworkException, FileAccessException {
        synchronized (mLock) {
            String msgId = msg.getMessageId();
            String mimeType = msg.getMimeType();
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Resend chat message, msgId ").append(msgId)
                        .append(" and mimeType ").append(mimeType).toString());
            }
            mImService.acceptStoreAndForwardMessageSessionIfSuchExists(mContact);
            final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
            if (session == null) {
                if (!mImService.isChatSessionAvailable()) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Cannot start new session since to limit of sessions is reached. MessageId="
                                .concat(msgId));
                    }
                    setChatMessageStatusAndTimestamp(msg, Status.QUEUED);
                    return;
                }
                if (sLogger.isActivated()) {
                    sLogger.debug("Core session is not yet established: initiate a new session to send the message");
                }
                setChatMessageStatusAndTimestamp(msg, Status.SENDING);
                sendChatMessageInNewSession(msg);
                return;
            }
            if (session.isMediaEstablished()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Core session is established: use existing one to send the message");
                }
                setChatMessageStatusAndTimestamp(msg, Status.SENDING);
                sendChatMessageWithinSession(session, msg);
                return;
            }
            /*
             * If session is originated by remote, then queue the message and accept the pending
             * session as part of this re-send operation
             */
            if (session.isInitiatedByRemote()) {
                setChatMessageStatusAndTimestamp(msg, Status.QUEUED);
                if (sLogger.isActivated()) {
                    sLogger.debug("Accept one-to-one chat session with contact ".concat(mContact
                            .toString()));
                }
                session.acceptSession();
            } else {
                if (!mImService.isChatSessionAvailable()) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Cannot start new session since to limit of sessions is reached. MessageId="
                                .concat(msgId));
                    }
                    setChatMessageStatusAndTimestamp(msg, Status.QUEUED);
                    return;
                }
                setChatMessageStatusAndTimestamp(msg, Status.SENDING);
                sendChatMessageInNewSession(msg);
            }
        }
    }

    /**
     * Returns the remote contact identifier
     * 
     * @return ContactId
     */
    @Override
    public ContactId getRemoteContact() {
        return mContact;
    }

    /**
     * Returns true if it is possible to send messages in this one to one chat right now, else
     * return false.
     * 
     * @return boolean
     * @throws RemoteException
     */
    @Override
    public boolean isAllowedToSendMessage() throws RemoteException {
        try {
            if (!mRcsSettings.getMyCapabilities().isImSessionSupported()) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot send message on one-to-one chat with contact '")
                            .append(mContact)
                            .append("' as IM capabilities are not supported for self.").toString());
                }
                return false;
            }
            Capabilities remoteCapabilities = mContactManager.getContactCapabilities(mContact);
            if (remoteCapabilities == null) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot send message on one-to-one chat with contact '")
                            .append(mContact)
                            .append("' as the contact's capabilities are not known.").toString());
                }
                return false;
            }
            MessagingMode mode = mRcsSettings.getMessagingMode();
            switch (mode) {
                case INTEGRATED:
                case SEAMLESS:
                    if (!mRcsSettings.isImAlwaysOn()
                            && !mImService.isCapabilitiesValid(remoteCapabilities)) {
                        if (sLogger.isActivated()) {
                            sLogger.debug(new StringBuilder(
                                    "Cannot send message on one-to-one chat with contact '")
                                    .append(mContact)
                                    .append("' as the contact's cached capabilities are not valid anymore for one-to-one communication.")
                                    .toString());
                        }
                        return false;
                    }
                    break;
                default:
                    break;
            }
            if (!remoteCapabilities.isImSessionSupported()) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot send message on one-to-one chat with contact '")
                            .append(mContact)
                            .append("' as IM capabilities are not supported for that contact.")
                            .toString());
                }
                return false;
            }
            return true;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    private long getDeliveryExpirationTime(long timestampSent) {
        if (mRcsSettings.isImAlwaysOn()) {
            return 0;
        }
        final long timeout = mRcsSettings.getMsgDeliveryTimeoutPeriod();
        if (timeout == 0L) {
            return 0;
        }
        return timestampSent + timeout;
    }

    /**
     * Add chat message to Db
     * 
     * @param msg InstantMessage
     * @param state state of message
     * @throws PayloadException
     * @throws NetworkException
     */
    private void addOutgoingChatMessage(ChatMessage msg, Status status) throws PayloadException,
            NetworkException {
        String msgId = msg.getMessageId();
        long timestampSent = msg.getTimestampSent();
        long deliveryExpiration = getDeliveryExpirationTime(timestampSent);
        mMessagingLog.addOutgoingOneToOneChatMessage(msg, status, ReasonCode.UNSPECIFIED,
                deliveryExpiration);
        if (deliveryExpiration != 0) {
            mImService.getDeliveryExpirationManager()
                    .scheduleOneToOneChatMessageDeliveryTimeoutAlarm(mContact, msgId,
                            deliveryExpiration);
        }
    }

    /**
     * Set chat message status
     * 
     * @param msgId
     * @param mimeType
     * @param state state of message
     * @param reasonCode
     */
    private void setChatMessageStatusAndReasonCode(String msgId, String mimeType, Status status,
            ReasonCode reasonCode) {
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode)) {
                mBroadcaster.broadcastMessageStatusChanged(mContact, mimeType, msgId, status,
                        reasonCode);
            }
        }
    }

    /**
     * Set chat message status and timestamp
     * 
     * @param ChatMessage
     * @param state state of message
     */
    private void setChatMessageStatusAndTimestamp(ChatMessage msg, Status status) {
        String msgId = msg.getMessageId();
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndTimestamp(msgId, status,
                    ReasonCode.UNSPECIFIED, msg.getTimestamp(), msg.getTimestampSent())) {
                mBroadcaster.broadcastMessageStatusChanged(mContact, msg.getMimeType(), msgId,
                        status, ReasonCode.UNSPECIFIED);
            }
        }
    }

    /**
     * Sends a plain text message
     * 
     * @param message Text message
     * @return Chat message
     * @throws RemoteException
     */
    @Override
    public IChatMessage sendMessage(String message) throws RemoteException {
        if (TextUtils.isEmpty(message)) {
            throw new ServerApiIllegalArgumentException("message must not be null or empty!");
        }
        int messageLength = message.length();
        int maxMessageLength = mRcsSettings.getMaxChatMessageLength();
        if (messageLength > maxMessageLength) {
            throw new ServerApiIllegalArgumentException(new StringBuilder()
                    .append("chat message length: ").append(messageLength)
                    .append(" exeeds max chat message length: ").append(maxMessageLength)
                    .append("!").toString());
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Send text message.");
        }
        try {
            mImService.removeOneToOneChatComposingStatus(mContact); /* clear cache */
            long timestamp = System.currentTimeMillis();
            /* For outgoing message, timestampSent = timestamp */
            final ChatMessage msg = ChatUtils.createTextMessage(mContact, message, timestamp,
                    timestamp);
            ChatMessagePersistedStorageAccessor persistedStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msg.getMessageId(), msg.getRemoteContact(), message,
                    msg.getMimeType(), mContact.toString(), Direction.OUTGOING);
            /* Always insert message with status QUEUED */
            addOutgoingChatMessage(msg, Status.QUEUED);

            mImService.tryToDequeueOneToOneChatMessages(mContact);
            return new ChatMessageImpl(persistedStorage);

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc
     * @return ChatMessage
     * @throws RemoteException
     */
    @Override
    public IChatMessage sendMessage2(Geoloc geoloc) throws RemoteException {
        if (geoloc == null) {
            throw new ServerApiIllegalArgumentException("geoloc must not be null!");
        }
        String label = geoloc.getLabel();
        if (label != null) {
            int labelLength = label.length();
            int labelMaxLength = mRcsSettings.getMaxGeolocLabelLength();
            if (labelLength > labelMaxLength) {
                throw new ServerApiIllegalArgumentException(new StringBuilder()
                        .append("geoloc message label length: ").append(labelLength)
                        .append(" exeeds max length: ").append(labelMaxLength).append("!")
                        .toString());
            }
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Send geolocation message.");
        }
        try {
            long timestamp = System.currentTimeMillis();
            /** For outgoing message, timestampSent = timestamp */
            final ChatMessage geolocMsg = ChatUtils.createGeolocMessage(mContact, geoloc, timestamp,
                    timestamp);
            ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, geolocMsg.getMessageId(), geolocMsg.getRemoteContact(),
                    geolocMsg.getContent(), geolocMsg.getMimeType(), mContact.toString(),
                    Direction.OUTGOING);
            /* Always insert message with status QUEUED */
            addOutgoingChatMessage(geolocMsg, Status.QUEUED);

            mImService.tryToDequeueOneToOneChatMessages(mContact);
            return new ChatMessageImpl(persistentStorage);

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Dequeue one-one chat message
     * 
     * @param msg
     * @throws SessionUnavailableException
     * @throws PayloadException
     * @throws NetworkException
     */
    public void dequeueOneToOneChatMessage(ChatMessage msg) throws SessionUnavailableException,
            PayloadException, NetworkException {
        String msgId = msg.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.debug("Dequeue chat message msgId=".concat(msgId));
        }
        mImService.acceptStoreAndForwardMessageSessionIfSuchExists(mContact);
        OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
        if (session == null) {
            if (mImService.isChatSessionAvailable()) {
                setChatMessageStatusAndTimestamp(msg, Status.SENDING);
                sendChatMessageInNewSession(msg);
            } else {
                throw new SessionUnavailableException(new StringBuilder(
                        "There is no available chat session for contact '").append(mContact)
                        .append("'!").toString());
            }
        } else if (session.isMediaEstablished()) {
            setChatMessageStatusAndTimestamp(msg, Status.SENDING);
            sendChatMessageWithinSession(session, msg);
        } else if (session.isInitiatedByRemote()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Accept one-to-one chat session with contact ".concat(mContact
                        .toString()));
            }
            session.acceptSession();
        } else {
            if (mImService.isChatSessionAvailable()) {
                setChatMessageStatusAndTimestamp(msg, Status.SENDING);
                sendChatMessageInNewSession(msg);
            } else {
                throw new SessionUnavailableException(new StringBuilder(
                        "There is no available chat session for contact '").append(mContact)
                        .append("'!").toString());
            }
        }
    }

    /**
     * Send file info in a new one-one chat session
     * 
     * @param fileTransferId
     * @param fileInfo
     * @param oneToOneFileTransfer
     * @throws PayloadException
     * @throws NetworkException
     */
    private void sendFileInfoInNewSession(String fileTransferId, String fileInfo,
            OneToOneFileTransferImpl oneToOneFileTransfer) throws PayloadException,
            NetworkException {
        long timestamp = System.currentTimeMillis();
        /* For outgoing file transfer, timestampSent = timestamp */
        long timestampSent = timestamp;
        mMessagingLog.setFileTransferTimestamps(fileTransferId, timestamp, timestampSent);
        ChatMessage firstMsg = ChatUtils.createFileTransferMessage(getRemoteContact(), fileInfo,
                fileTransferId, timestamp, timestampSent);
        OneToOneChatSession chatSession = mImService.createOneToOneChatSession(getRemoteContact(),
                firstMsg);
        chatSession.startSession();
        mImService.receiveOneOneChatSessionInitiation(chatSession);
        oneToOneFileTransfer.onFileInfoDequeued(getRemoteContact());
    }

    /**
     * Dequeue one-one file info
     * 
     * @param fileTransferId
     * @param fileInfo
     * @param displayReportsEnabled
     * @param deliverReportsEnabled
     * @param oneToOneFileTransfer
     * @throws PayloadException
     * @throws NetworkException
     * @throws SessionUnavailableException
     */
    public void dequeueOneToOneFileInfo(String fileTransferId, String fileInfo,
            boolean displayReportsEnabled, boolean deliverReportsEnabled,
            OneToOneFileTransferImpl oneToOneFileTransfer) throws PayloadException,
            NetworkException, SessionUnavailableException {
        mImService.acceptStoreAndForwardMessageSessionIfSuchExists(mContact);
        OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
        if (session == null) {
            if (mImService.isChatSessionAvailable()) {
                sendFileInfoInNewSession(fileTransferId, fileInfo, oneToOneFileTransfer);
            } else {
                throw new SessionUnavailableException(new StringBuilder(
                        "There is no available chat session for contact '").append(mContact)
                        .append("'!").toString());
            }
        } else if (session.isMediaEstablished()) {
            session.sendFileInfo(oneToOneFileTransfer, fileTransferId, fileInfo,
                    displayReportsEnabled, deliverReportsEnabled);
        } else if (session.isInitiatedByRemote()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Accept one-to-one chat session with contact ".concat(mContact
                        .toString()));
            }
            session.acceptSession();
        } else {
            if (mImService.isChatSessionAvailable()) {
                sendFileInfoInNewSession(fileTransferId, fileInfo, oneToOneFileTransfer);
            } else {
                throw new SessionUnavailableException(new StringBuilder(
                        "There is no available chat session for contact '").append(mContact)
                        .append("'!").toString());
            }
        }
    }

    /**
     * Sends a displayed delivery report for a given message ID
     * 
     * @param remote Remote contact
     * @param msgId Message ID
     * @param timestamp Timestamp sent in payload for IMDN datetime
     * @throws NetworkException
     */
    /* package private */void sendDisplayedDeliveryReport(final ContactId remote,
            final String msgId, final long timestamp) throws NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("Set displayed delivery report for " + msgId);
        }
        TerminatingStoreAndForwardOneToOneChatMessageSession storeAndForwardSession = mImService
                .getStoreAndForwardMsgSession(mContact);
        final OneToOneChatSession session = storeAndForwardSession != null ? storeAndForwardSession
                : mImService.getOneToOneChatSession(mContact);
        if (session != null && session.isMediaEstablished()) {
            if (sLogger.isActivated()) {
                sLogger.info("Use the original session to send the delivery status for " + msgId);
            }
            session.sendMsrpMessageDeliveryStatus(remote, msgId,
                    ImdnDocument.DELIVERY_STATUS_DISPLAYED, timestamp);
        } else {
            if (sLogger.isActivated()) {
                sLogger.info("No suitable session found to send the delivery status for " + msgId
                        + " : use SIP message");
            }
            mImService.getImdnManager().sendMessageDeliveryStatus(remote.toString(), remote, msgId,
                    ImdnDocument.DELIVERY_STATUS_DISPLAYED, timestamp);
        }
    }

    /**
     * Sends an is-composing event. The status is set to true when typing a message, else it is set
     * to false.
     * 
     * @param status
     * @throws RemoteException
     */
    @Override
    public void setComposingStatus(final boolean status) throws RemoteException {
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                try {
                    mImService.removeOneToOneChatComposingStatus(mContact);
                    ImSessionStartMode imSessionStartMode = mRcsSettings.getImSessionStartMode();
                    switch (imSessionStartMode) {
                        case ON_OPENING:
                        case ON_COMPOSING:
                            mImService.acceptStoreAndForwardMessageSessionIfSuchExists(mContact);
                            break;
                        default:
                            break;
                    }
                    final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
                    if (session == null) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Unable to send composing event '" + status
                                    + "' since oneToOne chat session found with contact '"
                                    + mContact + "' does not exist for now.");
                        }
                        mImService.addOneToOneChatComposingStatus(mContact, status);
                    } else if (session.getDialogPath().isSessionEstablished()) {
                        session.sendIsComposingStatus(status);
                    } else if (!session.isInitiatedByRemote()) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Unable to send composing event '" + status
                                    + "' since oneToOne chat session found with contact '"
                                    + mContact + "' is initiated locally.");
                        }
                        mImService.addOneToOneChatComposingStatus(mContact, status);
                    } else {
                        switch (imSessionStartMode) {
                            case ON_OPENING:
                            case ON_COMPOSING:
                                if (sLogger.isActivated()) {
                                    sLogger.debug("OneToOne chat session found with contact '"
                                            + mContact
                                            + "' is not established and imSessionStartMode = "
                                            + imSessionStartMode
                                            + " so accepting it and sending composing event '"
                                            + status + "'");
                                }
                                session.acceptSession();
                                session.sendIsComposingStatus(status);
                                break;
                            default:
                                if (sLogger.isActivated()) {
                                    sLogger.debug("OneToOne chat session found with contact '"
                                            + mContact
                                            + "' is not established and imSessionStartMode = "
                                            + imSessionStartMode
                                            + " so can't accept it and sending composing event '"
                                            + status + "' yet.");
                                }
                                mImService.addOneToOneChatComposingStatus(mContact, status);
                                break;
                        }
                    }
                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(new StringBuilder("Failed to send composing status to contact '")
                            .append(mContact).append("'").toString(), e);
                }
            }
        });
    }

    /**
     * open the chat conversation. Note: if its an incoming pending chat session and the parameter
     * IM SESSION START is 0 then the session is accepted now.
     * 
     * @see ImSessionStartMode
     * @throws RemoteException
     */
    @Override
    public void openChat() throws RemoteException {
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.info("Open a 1-1 chat session with " + mContact);
                }
                try {
                    ImSessionStartMode imSessionStartMode = mRcsSettings.getImSessionStartMode();
                    if (ImSessionStartMode.ON_OPENING == imSessionStartMode) {
                        mImService.acceptStoreAndForwardMessageSessionIfSuchExists(mContact);
                    }
                    final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
                    if (session == null) {
                        /*
                         * If there is no session ongoing right now then we do not need to open
                         * anything right now so we just return here. A sending of a new message on
                         * this one-to-one chat will anyway result in creating a new session so we
                         * do not need to do anything more here for now.
                         */
                        return;
                    }
                    if (!session.getDialogPath().isSessionEstablished()) {
                        if (!session.isInitiatedByRemote()) {
                            /*
                             * This method needs to accept pending invitation if
                             * IM_SESSION_START_MODE is 0, which is not applicable if session is
                             * remote originated so we return here.
                             */
                            return;
                        }
                        if (ImSessionStartMode.ON_OPENING == imSessionStartMode) {
                            if (sLogger.isActivated()) {
                                sLogger.debug("Accept one-to-one chat session with contact "
                                        .concat(mContact.toString()));
                            }
                            session.acceptSession();
                        }
                    }
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(new StringBuilder("Failed to open one-one chat with contact : '")
                            .append(mContact).append("'").toString(), e);
                }
            }
        });
    }

    /**
     * Resend a message which previously failed.
     * 
     * @param msgId
     * @throws RemoteException
     */
    @Override
    public void resendMessage(final String msgId) throws RemoteException {
        if (TextUtils.isEmpty(msgId)) {
            throw new ServerApiIllegalArgumentException(
                    "OnetoOneChat messageId must not be null or empty!");
        }
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
                        mMessagingLog, msgId);
                final String mimeType = persistentStorage.getMimeType();
                /* Set new timestamp for resend message */
                long timestamp = System.currentTimeMillis();
                /* For outgoing message, timestampSent = timestamp */
                final ChatMessage msg = new ChatMessage(msgId, mContact, persistentStorage
                        .getContent(), mimeType, timestamp, timestamp, null);
                try {
                    if (ServerApiUtils.isImsConnected()) {
                        resendChatMessage(msg);
                    } else {
                        /* If the IMS is NOT connected at this time then re-queue message. */
                        setChatMessageStatusAndReasonCode(msgId, mimeType, Status.QUEUED,
                                ReasonCode.UNSPECIFIED);
                    }
                } catch (FileAccessException e) {
                    sLogger.error(new StringBuilder("Failed to send chat message with msgId '")
                            .append(msgId).append("'").toString(), e);
                    setChatMessageStatusAndReasonCode(msgId, mimeType, Status.FAILED,
                            ReasonCode.FAILED_SEND);
                } catch (PayloadException e) {
                    sLogger.error(new StringBuilder("Failed to send chat message with msgId '")
                            .append(msgId).append("'").toString(), e);
                    setChatMessageStatusAndReasonCode(msgId, mimeType, Status.FAILED,
                            ReasonCode.FAILED_SEND);
                } catch (NetworkException e) {
                    sLogger.error(new StringBuilder("Failed to send chat message with msgId '")
                            .append(msg.getMessageId()).append("'").toString(), e);
                    setChatMessageStatusAndReasonCode(msgId, mimeType, Status.FAILED,
                            ReasonCode.FAILED_SEND);
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(new StringBuilder("Failed to send chat message with msgId '")
                            .append(msg.getMessageId()).append("'").toString(), e);
                }
            }
        });
    }

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    @Override
    public void onSessionStarted(ContactId contact) {
        boolean loggerActivated = sLogger.isActivated();
        if (loggerActivated) {
            sLogger.info("Session started");
        }
        synchronized (mLock) {
            Boolean composingStatus = mImService.getOneToOneChatComposingStatus(mContact);
            if (composingStatus != null) {
                if (loggerActivated) {
                    sLogger.debug("Sending isComposing command with status :"
                            .concat(composingStatus.toString()));
                }
                OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
                try {
                    session.sendIsComposingStatus(composingStatus);
                    mImService.removeOneToOneChatComposingStatus(mContact);
                } catch (NetworkException e) {
                    /*
                     * Nothing to be handled here as we are not able to send composing status for
                     * now, should try later and hence we don't remove it from the map.
                     */
                    if (loggerActivated) {
                        sLogger.debug(e.getMessage());
                    }
                }
            }
        }
        mImService.tryToDequeueOneToOneChatMessages(contact);
    }

    @Override
    public void onSessionAborted(ContactId contact, TerminationReason reason) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Session aborted (reason ").append(reason).append(")")
                    .toString());
        }
        synchronized (mLock) {
            mChatService.removeOneToOneChat(mContact);
        }
        mImService.tryToDequeueAllOneToOneChatMessagesAndOneToOneFileTransfers();
    }

    @Override
    public void onMessageReceived(final ChatMessage msg, final boolean imdnDisplayedRequested) {
        mImService.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    String msgId = msg.getMessageId();
                    if (sLogger.isActivated()) {
                        sLogger.info(new StringBuilder("New IM with messageId '").append(msgId)
                                .append("' received from ").append(mContact).append(".").toString());
                    }
                    synchronized (mLock) {
                        if (mContactManager.isBlockedForContact(mContact)) {
                            if (sLogger.isActivated()) {
                                sLogger.debug("Contact "
                                        + mContact
                                        + " is blocked: automatically abort the chat session and store message to spam folder.");
                            }
                            OneToOneChatSession session = mImService
                                    .getOneToOneChatSession(mContact);
                            if (session != null) {
                                session.terminateSession(TerminationReason.TERMINATION_BY_USER);
                            }
                            mMessagingLog.addOneToOneSpamMessage(msg);
                            mBroadcaster.broadcastMessageReceived(msg.getMimeType(), msgId);
                            return;
                        }
                        mMessagingLog.addIncomingOneToOneChatMessage(msg, imdnDisplayedRequested);
                        mBroadcaster.broadcastMessageReceived(msg.getMimeType(), msgId);
                    }
                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive chat message! (" + e.getMessage() + ")");
                    }
                } catch (PayloadException e) {
                    sLogger.error("Failed to receive chat message!", e);
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to receive chat message!", e);
                }
            }
        });
    }

    @Override
    public void onImError(ChatError error, String msgId, String mimeType) {
        int errorCode = error.getErrorCode();
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("IM error ").append(errorCode)
                    .append(" ; First message '").append(msgId).append("'").toString());
        }
        synchronized (mLock) {
            mChatService.removeOneToOneChat(mContact);
            switch (errorCode) {
                case ChatError.SESSION_INITIATION_FAILED:
                case ChatError.SESSION_INITIATION_CANCELLED:
                    mImService.getDeliveryExpirationManager().cancelDeliveryTimeoutAlarm(msgId);
                    if (FileTransferUtils.isFileTransferHttpType(mimeType)) {
                        mImService.setOneToOneFileTransferFailureReasonCode(msgId, mContact,
                                FileTransfer.ReasonCode.FAILED_DATA_TRANSFER);
                    } else if (ChatUtils.isTextPlainType(mimeType)
                            || MimeType.GEOLOC_MESSAGE.equals(mimeType)) {
                        setChatMessageStatusAndReasonCode(msgId, mimeType, Status.FAILED,
                                ReasonCode.FAILED_SEND);
                    }
                    break;
                /*
                 * For cases where send response failed due to no ACK/200 OK response, we should not
                 * change Chat state.
                 */
                /* Intentional fall through */
                case ChatError.SEND_RESPONSE_FAILED:
                    break;
                default:
                    break;
            }
        }
        mImService.tryToDequeueAllOneToOneChatMessagesAndOneToOneFileTransfers();
    }

    @Override
    public void onIsComposingEventReceived(ContactId contact, boolean status) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("").append(contact)
                    .append(" is composing status set to ").append(status).toString());
        }
        synchronized (mLock) {
            mBroadcaster.broadcastComposingEvent(contact, status);
        }
    }

    @Override
    public void onMessageSent(String msgId, String mimeType) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Message sent; msgId=").append(msgId).append(".")
                    .toString());
        }
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.SENT,
                    ReasonCode.UNSPECIFIED)) {
                mBroadcaster.broadcastMessageStatusChanged(mContact, mimeType, msgId, Status.SENT,
                        ReasonCode.UNSPECIFIED);
            }
        }
    }

    @Override
    public void onMessageFailedSend(String msgId, String mimeType) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Message sent; msgId=").append(msgId).append(".")
                    .toString());
        }
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.FAILED,
                    ReasonCode.FAILED_SEND)) {
                mBroadcaster.broadcastMessageStatusChanged(mContact, mimeType, msgId,
                        Status.FAILED, ReasonCode.FAILED_SEND);
            }
        }
    }

    @Override
    public void onMessageDeliveryStatusReceived(ContactId contact, ImdnDocument imdn) {
        mChatService.onOneToOneMessageDeliveryStatusReceived(contact, imdn);
    }

    @Override
    public void onDeliveryStatusReceived(String contributionId, ContactId contact, ImdnDocument imdn) {
        String msgId = imdn.getMsgId();

        if (mMessagingLog.isMessagePersisted(msgId)) {
            onMessageDeliveryStatusReceived(contact, imdn);
            return;
        }

        if (mMessagingLog.isFileTransfer(msgId)) {
            mImService.receiveOneToOneFileDeliveryStatus(contact, imdn);
            return;
        }

        sLogger.error(new StringBuilder(
                "Imdn delivery report received referencing an entry that was ")
                .append("not found in our database. Message id ").append(msgId)
                .append(", ignoring.").toString());
    }

    @Override
    public void onSessionRejected(ContactId contact, TerminationReason reason) {
        if (sLogger.isActivated()) {
            switch (reason) {
                case TERMINATION_BY_USER:
                    sLogger.info("Session rejected by user.");
                    break;
                case TERMINATION_BY_CONNECTION_LOST:
                    /* Intentional fall through */
                case TERMINATION_BY_SYSTEM:
                    sLogger.info("Session rejected by system.");
                    break;
                case TERMINATION_BY_TIMEOUT:
                    sLogger.info("Session rejected by timeout.");
                    break;
                case TERMINATION_BY_INACTIVITY:
                    sLogger.info("Session rejected by inactivity.");
                    break;
                case TERMINATION_BY_REMOTE:
                    sLogger.info("Session rejected by remote.");
                    break;
                default:
                    throw new IllegalArgumentException(new StringBuilder(
                            "Unknown reason RejectedReason=").append(reason).append("!").toString());
            }
        }
        synchronized (mLock) {
            mChatService.removeOneToOneChat(mContact);
        }
        mImService.tryToDequeueAllOneToOneChatMessagesAndOneToOneFileTransfers();
    }

    @Override
    public void onSessionInvited(ContactId contact) {
        /* Not used by one-to-one chat */
    }

    @Override
    public void onSessionAccepting(ContactId contact) {
        /* Not used by one-to-one chat */
    }

    @Override
    public void onSessionAutoAccepted(ContactId contact) {
        /* Not used by one-to-one chat */
    }

    @Override
    public void onChatMessageDisplayReportSent(String msgId) {
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.RECEIVED,
                    ReasonCode.UNSPECIFIED)) {
                String apiMimeType = mMessagingLog.getMessageMimeType(msgId);
                mBroadcaster.broadcastMessageStatusChanged(mContact, apiMimeType, msgId,
                        Status.RECEIVED, ReasonCode.UNSPECIFIED);
            }
        }
    }

    @Override
    public void onDeliveryReportSendViaMsrpFailure(String msgId, ContactId contact,
            TypeMsrpChunk typeMsrpChunk) {
        if (TypeMsrpChunk.MessageDeliveredReport.equals(typeMsrpChunk)) {
            if (sLogger.isActivated()) {
                sLogger.info(new StringBuilder(
                        "Failed to send delivered message via MSRP, so try to send via SIP message to ")
                        .append(contact).append("(msgId = ").append(msgId).toString());
            }
            /* Send the delivered notification by SIP */
            ContactId remote = getRemoteContact();
            mImService.getImdnManager().sendMessageDeliveryStatus(remote.toString(), remote, msgId,
                    ImdnDocument.DELIVERY_STATUS_DELIVERED, System.currentTimeMillis());
        } else if (TypeMsrpChunk.MessageDisplayedReport.equals(typeMsrpChunk)) {
            if (sLogger.isActivated()) {
                sLogger.info(new StringBuilder(
                        "Failed to send displayed message via MSRP, so try to send via SIP message to ")
                        .append(contact).append("(msgId = ").append(msgId).toString());
            }
            /* Send the displayed notification by SIP */
            ContactId remote = getRemoteContact();
            mImService.getImdnManager().sendMessageDeliveryStatus(remote.toString(), remote, msgId,
                    ImdnDocument.DELIVERY_STATUS_DISPLAYED, System.currentTimeMillis());
        }
    }
}
