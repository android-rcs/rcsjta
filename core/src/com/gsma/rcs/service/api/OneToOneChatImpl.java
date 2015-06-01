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

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpException;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatError;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSessionListener;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardOneToOneChatMessageSession;
import com.gsma.rcs.provider.contact.ContactManager;
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

    private final Core mCore;

    private final OneToOneUndeliveredImManager mUndeliveredImManager;

    /**
     * Lock used for synchronization
     */
    private final Object lock = new Object();

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
     * @param rcsSettings RcsSettings
     * @param chatService ChatServiceImpl
     * @param contactManager ContactManager
     * @param core Core
     * @param undeliveredImManager OneToOneUndeliveredImManager
     */
    public OneToOneChatImpl(ContactId contact, IOneToOneChatEventBroadcaster broadcaster,
            InstantMessagingService imService, MessagingLog messagingLog, RcsSettings rcsSettings,
            ChatServiceImpl chatService, ContactManager contactManager, Core core,
            OneToOneUndeliveredImManager undeliveredImManager) {
        mContact = contact;
        mBroadcaster = broadcaster;
        mImService = imService;
        mMessagingLog = messagingLog;
        mChatService = chatService;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
        mCore = core;
        mUndeliveredImManager = undeliveredImManager;
    }

    private void sendChatMessageInNewSession(ChatMessage msg) {
        final OneToOneChatSession newSession = mImService
                .initiateOneToOneChatSession(mContact, msg);
        newSession.addListener(this);
        mChatService.addOneToOneChat(mContact, this);
        newSession.startSession();
        handleMessageSent(msg.getMessageId(), msg.getMimeType());
    }

    private void sendChatMessageWithinSession(final OneToOneChatSession session, ChatMessage msg)
            throws MsrpException {
        session.sendChatMessage(msg);
    }

    private void acceptPendingSession(final OneToOneChatSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug("Accept one-to-one chat session with contact ".concat(mContact.toString()));
        }
        session.acceptSession();
    }

    /**
     * Sends a chat message
     * 
     * @param msg Message
     * @throws MsrpException
     */
    private void sendChatMessage(final ChatMessage msg) throws MsrpException {
        synchronized (lock) {
            boolean loggerActivated = sLogger.isActivated();
            if (loggerActivated) {
                sLogger.debug(new StringBuilder("Send chat message, msgId ")
                        .append(msg.getMessageId()).append(" and mimeType ")
                        .append(msg.getMimeType()).toString());
            }
            final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
            if (session == null) {
                if (!mImService.isChatSessionAvailable()) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Cannot start new session since to limit of sessions is reached; queue message.");
                    }
                    addOutgoingChatMessage(msg, Status.QUEUED);
                    return;
                }
                if (loggerActivated) {
                    sLogger.debug("Core session is not yet established: initiate a new session to send the message.");
                }
                addOutgoingChatMessage(msg, Status.SENDING);
                sendChatMessageInNewSession(msg);
                return;
            }
            if (session.isMediaEstablished()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Core session is established: use existing one to send the message");
                }
                addOutgoingChatMessage(msg, Status.SENDING);
                sendChatMessageWithinSession(session, msg);
                return;
            }
            /*
             * TODO : If session is originated by remote, then queue the message and accept the
             * pending session as part of this send operation
             */
            if (session.isInitiatedByRemote()) {
                addOutgoingChatMessage(msg, Status.QUEUED);
                acceptPendingSession(session);
            } else {
                if (!mImService.isChatSessionAvailable()) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Cannot start new session since to limit of sessions is reached; queue message.");
                    }
                    addOutgoingChatMessage(msg, Status.QUEUED);
                    return;
                }
                addOutgoingChatMessage(msg, Status.SENDING);
                sendChatMessageInNewSession(msg);
            }
        }
    }

    /**
     * Resends a chat message
     * 
     * @param msg Message
     * @throws MsrpException
     */
    private void resendChatMessage(final ChatMessage msg) throws MsrpException {
        synchronized (lock) {
            String msgId = msg.getMessageId();
            String mimeType = msg.getMimeType();
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Resend chat message, msgId ").append(msgId)
                        .append(" and mimeType ").append(mimeType).toString());
            }
            final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
            if (session == null) {
                if (!mImService.isChatSessionAvailable()) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Cannot start new session since to limit of sessions is reached. MessageId="
                                .concat(msgId));
                    }
                    mMessagingLog.requeueChatMessage(msg);
                    return;
                }
                if (sLogger.isActivated()) {
                    sLogger.debug("Core session is not yet established: initiate a new session to send the message");
                }
                mMessagingLog.resendChatMessage(msg);
                sendChatMessageInNewSession(msg);
                return;
            }
            if (session.isMediaEstablished()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Core session is established: use existing one to send the message");
                }
                mMessagingLog.resendChatMessage(msg);
                sendChatMessageWithinSession(session, msg);
                return;
            }
            /*
             * If session is originated by remote, then queue the message and accept the pending
             * session as part of this re-send operation
             */
            if (session.isInitiatedByRemote()) {
                mMessagingLog.requeueChatMessage(msg);
                acceptPendingSession(session);
            } else {
                if (!mImService.isChatSessionAvailable()) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Cannot start new session since to limit of sessions is reached. MessageId="
                                .concat(msgId));
                    }
                    mMessagingLog.requeueChatMessage(msg);
                    return;
                }
                mMessagingLog.resendChatMessage(msg);
                sendChatMessageInNewSession(msg);
            }
        }
    }

    /**
     * Returns the remote contact identifier
     * 
     * @return ContactId
     */
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
     */
    private void addOutgoingChatMessage(ChatMessage msg, Status status) {
        String msgId = msg.getMessageId();
        long timestampSent = msg.getTimestampSent();
        long deliveryExpiration = getDeliveryExpirationTime(timestampSent);
        mMessagingLog.addOutgoingOneToOneChatMessage(msg, status, ReasonCode.UNSPECIFIED,
                deliveryExpiration);
        if (deliveryExpiration != 0) {
            mUndeliveredImManager.scheduleOneToOneChatMessageDeliveryTimeoutAlarm(mContact, msgId,
                    deliveryExpiration);
        }
    }

    /**
     * Set chat message status
     * 
     * @param msgId
     * @param mimeType
     * @param state state of message
     */
    private void setChatMessageStatus(String msgId, String mimeType, Status status) {
        mMessagingLog.setChatMessageStatusAndReasonCode(msgId, status, ReasonCode.UNSPECIFIED);
        mBroadcaster.broadcastMessageStatusChanged(mContact, mimeType, msgId, status,
                ReasonCode.UNSPECIFIED);
    }

    /**
     * Update chat message timestamp
     * 
     * @param msgId
     * @param timestamp New local timestamp
     * @param timestampSent New timestamp sent in payload
     */
    private void updateChatMessageTimestamp(String msgId, long timestamp, long timestampSent) {
        mMessagingLog.setChatMessageTimestamp(msgId, timestamp, timestampSent);
    }

    /**
     * Sends a plain text message
     * 
     * @param message Text message
     * @return Chat message
     * @throws RemoteException
     */
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
            ChatMessage msg = ChatUtils.createTextMessage(mContact, message, timestamp, timestamp);
            ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msg.getMessageId(), msg.getRemoteContact(), message,
                    MimeType.TEXT_MESSAGE, mContact.toString(), Direction.OUTGOING);

            /* If the IMS is connected at this time then send this message. */
            if (ServerApiUtils.isImsConnected()) {
                sendChatMessage(msg);
            } else {
                /* If the IMS is NOT connected at this time then queue message. */
                addOutgoingChatMessage(msg, Status.QUEUED);
            }
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
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc
     * @return ChatMessage
     * @throws RemoteException
     */
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
            ChatMessage msg = ChatUtils.createGeolocMessage(mContact, geoloc, timestamp, timestamp);
            ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msg.getMessageId(), msg.getRemoteContact(), msg.toString(),
                    MimeType.GEOLOC_MESSAGE, mContact.toString(), Direction.OUTGOING);

            /* If the IMS is connected at this time then send this message. */
            if (ServerApiUtils.isImsConnected()) {
                sendChatMessage(msg);
            } else {
                /* If the IMS is NOT connected at this time then queue message. */
                addOutgoingChatMessage(msg, Status.QUEUED);
            }
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
     * @param message
     * @throws MsrpException
     */
    public void dequeueOneToOneChatMessage(ChatMessage message) throws MsrpException {
        String msgId = message.getMessageId();
        String mimeType = message.getMimeType();
        if (sLogger.isActivated()) {
            sLogger.debug("Dequeue chat message msgId=".concat(msgId));
        }
        mMessagingLog.dequeueChatMessage(message);
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(mimeType);
        mBroadcaster.broadcastMessageStatusChanged(mContact, apiMimeType, msgId, Status.SENDING,
                ReasonCode.UNSPECIFIED);
        OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
        if (session == null) {
            if (mImService.isChatSessionAvailable()) {
                sendChatMessageInNewSession(message);
            } else {
                throw new MsrpException(new StringBuilder(
                        "There is no available chat session for contact '").append(mContact)
                        .append("'!").toString());
            }
        } else if (session.isMediaEstablished()) {
            sendChatMessageWithinSession(session, message);
        } else if (session.isInitiatedByRemote()) {
            session.acceptSession();
        } else {
            if (mImService.isChatSessionAvailable()) {
                sendChatMessageInNewSession(message);
            } else {
                throw new MsrpException(new StringBuilder(
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
     * @throws MsrpException
     */
    private void sendFileInfoInNewSession(String fileTransferId, String fileInfo,
            OneToOneFileTransferImpl oneToOneFileTransfer) throws MsrpException {
        long timestamp = System.currentTimeMillis();
        /* For outgoing file transfer, timestampSent = timestamp */
        long timestampSent = timestamp;
        mMessagingLog.setFileTransferTimestamps(fileTransferId, timestamp, timestampSent);
        ChatMessage firstMsg = ChatUtils.createFileTransferMessage(getRemoteContact(), fileInfo,
                fileTransferId, timestamp, timestampSent);
        OneToOneChatSession chatSession = mImService.initiateOneToOneChatSession(
                getRemoteContact(), firstMsg);
        chatSession.startSession();
        mCore.getListener().handleOneOneChatSessionInitiation(chatSession);
        oneToOneFileTransfer.handleFileInfoDequeued(getRemoteContact());
    }

    /**
     * Dequeue one-one file info
     * 
     * @param fileTransferId
     * @param fileInfo
     * @param displayReportsEnabled
     * @param deliverReportsEnabled
     * @param oneToOneFileTransfer
     * @throws MsrpException
     */
    public void dequeueOneToOneFileInfo(String fileTransferId, String fileInfo,
            boolean displayReportsEnabled, boolean deliverReportsEnabled,
            OneToOneFileTransferImpl oneToOneFileTransfer) throws MsrpException {
        OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
        if (session == null) {
            if (mImService.isChatSessionAvailable()) {
                sendFileInfoInNewSession(fileTransferId, fileInfo, oneToOneFileTransfer);
            } else {
                throw new MsrpException(new StringBuilder(
                        "There is no available chat session for contact '").append(mContact)
                        .append("'!").toString());
            }
        } else if (session.isMediaEstablished()) {
            session.sendFileInfo(oneToOneFileTransfer, fileTransferId, fileInfo,
                    displayReportsEnabled, deliverReportsEnabled);
        } else if (session.isInitiatedByRemote()) {
            session.acceptSession();
        } else {
            if (mImService.isChatSessionAvailable()) {
                sendFileInfoInNewSession(fileTransferId, fileInfo, oneToOneFileTransfer);
            } else {
                throw new MsrpException(new StringBuilder(
                        "There is no available chat session for contact '").append(mContact)
                        .append("'!").toString());
            }
        }
    }

    /**
     * Sends a displayed delivery report for a given message ID
     * 
     * @param contact Contact ID
     * @param msgId Message ID
     * @param timestamp Timestamp sent in payload for IMDN datetime
     * @throws MsrpException
     */
    /* package private */void sendDisplayedDeliveryReport(final ContactId contact,
            final String msgId, final long timestamp) throws MsrpException {
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
            session.sendMsrpMessageDeliveryStatus(contact, msgId,
                    ImdnDocument.DELIVERY_STATUS_DISPLAYED, timestamp);
        } else {
            if (sLogger.isActivated()) {
                sLogger.info("No suitable session found to send the delivery status for " + msgId
                        + " : use SIP message");
            }
            mImService.getImdnManager().sendMessageDeliveryStatus(contact, msgId,
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
    public void setComposingStatus(final boolean status) throws RemoteException {
        try {
            mImService.removeOneToOneChatComposingStatus(mContact);
            final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
            if (session == null) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Unable to send composing event '" + status
                            + "' since oneToOne chat session found with contact '" + mContact
                            + "' does not exist for now.");
                }
                mImService.addOneToOneChatComposingStatus(mContact, status);
            } else if (session.getDialogPath().isSessionEstablished()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Sending composing event '" + status
                            + "' since oneToOne chat session found with contact '" + mContact
                            + "' is established.");
                }
                session.sendIsComposingStatus(status);
            } else if (!session.isInitiatedByRemote()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Unable to send composing event '" + status
                            + "' since oneToOne chat session found with contact '" + mContact
                            + "' is initiated locally.");
                }
                mImService.addOneToOneChatComposingStatus(mContact, status);
            } else {
                ImSessionStartMode imSessionStartMode = mRcsSettings.getImSessionStartMode();
                switch (imSessionStartMode) {
                    case ON_OPENING:
                    case ON_COMPOSING:
                        if (sLogger.isActivated()) {
                            sLogger.debug("OneToOne chat session found with contact '" + mContact
                                    + "' is not established and imSessionStartMode = "
                                    + imSessionStartMode
                                    + " so accepting it and sending composing event '" + status
                                    + "'");
                        }
                        session.acceptSession();
                        session.sendIsComposingStatus(status);
                        break;
                    default:
                        if (sLogger.isActivated()) {
                            sLogger.debug("OneToOne chat session found with contact '" + mContact
                                    + "' is not established and imSessionStartMode = "
                                    + imSessionStartMode
                                    + " so can't accept it and sending composing event '" + status
                                    + "' yet.");
                        }
                        mImService.addOneToOneChatComposingStatus(mContact, status);
                        break;
                }
            }
        } catch (MsrpException e) {
            mImService.addOneToOneChatComposingStatus(mContact, status);
        } catch (ServerApiBaseException e) {
            mImService.addOneToOneChatComposingStatus(mContact, status);
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;
        } catch (Exception e) {
            mImService.addOneToOneChatComposingStatus(mContact, status);
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * open the chat conversation. Note: if its an incoming pending chat session and the parameter
     * IM SESSION START is 0 then the session is accepted now.
     * 
     * @see ImSessionStartMode
     * @throws RemoteException
     */
    public void openChat() throws RemoteException {
        if (sLogger.isActivated()) {
            sLogger.info("Open a 1-1 chat session with " + mContact);
        }
        try {
            final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
            if (session == null) {
                /*
                 * If there is no session ongoing right now then we do not need to open anything
                 * right now so we just return here. A sending of a new message on this one-to-one
                 * chat will anyway result in creating a new session so we do not need to do
                 * anything more here for now.
                 */
                return;
            }
            if (!session.getDialogPath().isSessionEstablished()) {
                ImSessionStartMode imSessionStartMode = mRcsSettings.getImSessionStartMode();
                if (!session.isInitiatedByRemote()) {
                    /*
                     * This method needs to accept pending invitation if IM_SESSION_START_MODE is 0,
                     * which is not applicable if session is remote originated so we return here.
                     */
                    return;
                }
                if (ImSessionStartMode.ON_OPENING == imSessionStartMode) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Core chat session is pending: auto accept it, as IM_SESSION_START mode = 0");
                    }
                    session.acceptSession();
                }
            }
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
     * Resend a message which previously failed.
     * 
     * @param msgId
     * @throws RemoteException
     */
    public void resendMessage(String msgId) throws RemoteException {
        if (TextUtils.isEmpty(msgId)) {
            throw new ServerApiIllegalArgumentException(
                    "OnetoOneChat messageId must not be null or empty!");
        }
        try {
            String mimeType = mMessagingLog.getMessageMimeType(msgId);
            /* Set new timestamp for resend message */
            long timestamp = System.currentTimeMillis();
            /* For outgoing message, timestampSent = timestamp */
            ChatMessage msg = new ChatMessage(msgId, mContact,
                    mMessagingLog.getChatMessageContent(msgId), mimeType, timestamp, timestamp,
                    null);
            if (ServerApiUtils.isImsConnected()) {
                resendChatMessage(msg);
            } else {
                /* If the IMS is NOT connected at this time then re-queue message. */
                setChatMessageStatus(msgId, mimeType, Status.QUEUED);
            }
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

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    @Override
    public void handleSessionStarted(ContactId contact) {
        boolean loggerActivated = sLogger.isActivated();
        if (loggerActivated) {
            sLogger.info("Session started");
        }
        synchronized (lock) {
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
                } catch (MsrpException e) {
                    /*
                     * Nothing to be handled here as we are not able to send composing status for
                     * now, should try later and hence we don't remove it from the map.
                     */
                    if (loggerActivated) {
                        sLogger.debug(new StringBuilder(
                                "Failed to send isComposing command for contact : ")
                                .append(mContact.toString()).append(" for isComposing status : ")
                                .append(composingStatus).toString());
                    }
                }
            }
        }
        mCore.getListener().tryToDequeueOneToOneChatMessages(mContact, mCore);
    }

    @Override
    public void handleSessionAborted(ContactId contact, TerminationReason reason) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Session aborted (reason ").append(reason).append(")")
                    .toString());
        }
        synchronized (lock) {
            mChatService.removeOneToOneChat(mContact);
        }
        mCore.getListener().tryToDequeueAllOneToOneChatMessagesAndOneToOneFileTransfers(mCore);
    }

    @Override
    public void handleReceiveMessage(ChatMessage msg, boolean imdnDisplayedRequested) {
        String msgId = msg.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("New IM with messageId '").append(msgId)
                    .append("' received from ").append(mContact).append(".").toString());
        }
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(msg.getMimeType());
        synchronized (lock) {
            if (mContactManager.isBlockedForContact(mContact)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Contact "
                            + mContact
                            + " is blocked: automatically abort the chat session and store message to spam folder.");
                }
                OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
                if (session != null) {
                    session.terminateSession(TerminationReason.TERMINATION_BY_USER);
                }
                mMessagingLog.addOneToOneSpamMessage(msg);
                mBroadcaster.broadcastMessageReceived(apiMimeType, msgId);
                return;
            }
            mMessagingLog.addIncomingOneToOneChatMessage(msg, imdnDisplayedRequested);
            mBroadcaster.broadcastMessageReceived(apiMimeType, msgId);
        }
    }

    @Override
    public void handleImError(ChatError error, ChatMessage message) {
        int errorCode = error.getErrorCode();
        if (sLogger.isActivated()) {
            sLogger.info("IM error " + errorCode);
        }
        synchronized (lock) {
            mChatService.removeOneToOneChat(mContact);
            switch (errorCode) {
                case ChatError.SESSION_INITIATION_FAILED:
                case ChatError.SESSION_INITIATION_CANCELLED:
                    if (message != null) {
                        String msgId = message.getMessageId();
                        mUndeliveredImManager.cancelDeliveryTimeoutAlarm(msgId);
                        String mimeType = message.getMimeType();
                        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(mimeType);
                        mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.FAILED,
                                ReasonCode.FAILED_SEND);
                        mBroadcaster.broadcastMessageStatusChanged(mContact, apiMimeType, msgId,
                                Status.FAILED, ReasonCode.FAILED_SEND);
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
        mCore.getListener().tryToDequeueAllOneToOneChatMessagesAndOneToOneFileTransfers(mCore);
    }

    @Override
    public void handleIsComposingEvent(ContactId contact, boolean status) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("").append(contact)
                    .append(" is composing status set to ").append(status).toString());
        }
        synchronized (lock) {
            mBroadcaster.broadcastComposingEvent(contact, status);
        }
    }

    @Override
    public void handleMessageSent(String msgId, String mimeType) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Message sent; msgId=").append(msgId).append(".")
                    .toString());
        }
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(mimeType);
        synchronized (lock) {
            mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.SENT,
                    ReasonCode.UNSPECIFIED);
            mBroadcaster.broadcastMessageStatusChanged(mContact, apiMimeType, msgId, Status.SENT,
                    ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleMessageFailedSend(String msgId, String mimeType) {
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(mimeType);
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Message sent; msgId=").append(msgId).append(".")
                    .toString());
        }
        synchronized (lock) {
            mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.FAILED,
                    ReasonCode.FAILED_SEND);

            mBroadcaster.broadcastMessageStatusChanged(mContact, apiMimeType, msgId, Status.FAILED,
                    ReasonCode.FAILED_SEND);
        }
    }

    @Override
    public void handleMessageDeliveryStatus(ContactId contact, ImdnDocument imdn) {
        mChatService.receiveOneToOneMessageDeliveryStatus(contact, imdn);
    }

    @Override
    public void handleDeliveryStatus(String contributionId, ContactId contact, ImdnDocument imdn) {
        String msgId = imdn.getMsgId();

        if (mMessagingLog.isMessagePersisted(msgId)) {
            handleMessageDeliveryStatus(contact, imdn);
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
    public void handleSessionRejected(ContactId contact, TerminationReason reason) {
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
        synchronized (lock) {
            mChatService.removeOneToOneChat(mContact);
        }
        mCore.getListener().tryToDequeueAllOneToOneChatMessagesAndOneToOneFileTransfers(mCore);
    }

    @Override
    public void handleSessionInvited(ContactId contact) {
        /* Not used by one-to-one chat */
    }

    @Override
    public void handleSessionAccepted(ContactId contact) {
        /* Not used by one-to-one chat */
    }

    @Override
    public void handleSessionAutoAccepted(ContactId contact) {
        /* Not used by one-to-one chat */
    }
}
