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
import com.gsma.rcs.provider.eab.ContactsManager;
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

    private final ContactsManager mContactManager;

    private final Core mCore;

    /**
     * Lock used for synchronization
     */
    private final Object lock = new Object();

    /**
     * The logger
     */
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Constructor
     * 
     * @param contact Remote contact ID
     * @param broadcaster IChatEventBroadcaster
     * @param imService InstantMessagingService
     * @param messagingLog MessagingLog
     * @param rcsSettings RcsSettings
     * @param chatService ChatServiceImpl
     * @param contactManager ContactsManager
     * @param core Core
     */
    public OneToOneChatImpl(ContactId contact, IOneToOneChatEventBroadcaster broadcaster,
            InstantMessagingService imService, MessagingLog messagingLog, RcsSettings rcsSettings,
            ChatServiceImpl chatService, ContactsManager contactManager, Core core) {
        mContact = contact;
        mBroadcaster = broadcaster;
        mImService = imService;
        mMessagingLog = messagingLog;
        mChatService = chatService;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
        mCore = core;
    }

    private ReasonCode imdnToFailedReasonCode(ImdnDocument imdn) {
        String notificationType = imdn.getNotificationType();
        if (ImdnDocument.DELIVERY_NOTIFICATION.equals(notificationType)) {
            return ReasonCode.FAILED_DELIVERY;

        } else if (ImdnDocument.DISPLAY_NOTIFICATION.equals(notificationType)) {
            return ReasonCode.FAILED_DISPLAY;
        }

        throw new IllegalArgumentException(new StringBuilder(
                "Received invalid imdn notification type:'").append(notificationType).append("'")
                .toString());
    }

    private void sendChatMessageInNewSession(ChatMessage msg) {
        try {
            final OneToOneChatSession newSession = mImService.initiateOneToOneChatSession(mContact,
                    msg);
            newSession.addListener(this);
            mChatService.addOneToOneChat(mContact, this);
            newSession.startSession();
            handleMessageSent(msg.getMessageId(), msg.getMimeType());

        } catch (Exception e) {
            /* TODO : Exception handling will be implemented better in CR037 */
            if (logger.isActivated()) {
                logger.error("Can't send a new chat message", e);
            }
            handleMessageFailedSend(msg.getMessageId(), msg.getMimeType());
        }
    }

    private void sendChatMessageWithinSession(final OneToOneChatSession session, ChatMessage msg) {
        session.sendChatMessage(msg);
    }

    private void acceptPendingSession(final OneToOneChatSession session) {
        if (logger.isActivated()) {
            logger.debug("Accept one-to-one chat session with contact ".concat(mContact.toString()));
        }
        session.acceptSession();
    }

    /**
     * Sends a chat message
     * 
     * @param msg Message
     */
    private void sendChatMessage(final ChatMessage msg) {
        synchronized (lock) {
            boolean loggerActivated = logger.isActivated();
            if (loggerActivated) {
                logger.debug(new StringBuilder("Send chat message, msgId ")
                        .append(msg.getMessageId()).append(" and mimeType ")
                        .append(msg.getMimeType()).toString());
            }
            final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
            if (session == null) {
                if (loggerActivated) {
                    logger.debug("Core session is not yet established: initiate a new session to send the message.");
                }
                addOutgoingChatMessage(msg, Status.SENDING);
                sendChatMessageInNewSession(msg);
                return;
            }
            if (session.isMediaEstablished()) {
                if (logger.isActivated()) {
                    logger.debug("Core session is established: use existing one to send the message");
                }
                addOutgoingChatMessage(msg, Status.SENDING);
                sendChatMessageWithinSession(session, msg);
                return;
            }
            /*
             * TODO : If session is originated by remote, then queue the message and accept the
             * pending session as part of this send operation
             */
            addOutgoingChatMessage(msg, Status.QUEUED);
            if (session.isInitiatedByRemote()) {
                acceptPendingSession(session);
            }
        }
    }

    /**
     * Resends a chat message
     * 
     * @param msg Message
     */
    private void resendChatMessage(final ChatMessage msg) {
        synchronized (lock) {
            String msgId = msg.getMessageId();
            String mimeType = msg.getMimeType();
            if (logger.isActivated()) {
                logger.debug(new StringBuilder("Resend chat message, msgId ").append(msgId)
                        .append(" and mimeType ").append(mimeType).toString());
            }
            final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
            if (session == null) {
                if (!mImService.isChatSessionAvailable()) {
                    if (logger.isActivated()) {
                        logger.debug("Session does not exist. Cannot start new session since to limit of sessions is reached. MessageId="
                                .concat(msgId));
                    }
                    setChatMessageStatus(msgId, mimeType, Status.QUEUED);
                    return;
                }

                if (logger.isActivated()) {
                    logger.debug("Core session is not yet established: initiate a new session to send the message");
                }

                setChatMessageStatus(msgId, mimeType, Status.SENDING);
                sendChatMessageInNewSession(msg);
                return;
            }
            if (session.isMediaEstablished()) {
                if (logger.isActivated()) {
                    logger.debug("Core session is established: use existing one to send the message");
                }
                setChatMessageStatus(msgId, mimeType, Status.SENDING);
                sendChatMessageWithinSession(session, msg);
                return;
            }
            /*
             * TODO : If session is originated by remote, then queue the message and accept the
             * pending session as part of this re-send operation
             */
            setChatMessageStatus(msgId, mimeType, Status.QUEUED);
            if (session.isInitiatedByRemote()) {
                acceptPendingSession(session);
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
     */
    public boolean isAllowedToSendMessage() {
        if (!mRcsSettings.getMyCapabilities().isImSessionSupported()) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder(
                        "Cannot send message on one-to-one chat with contact '").append(mContact)
                        .append("' as IM capabilities are not supported for self.").toString());
            }
            return false;
        }
        Capabilities remoteCapabilities = mContactManager.getContactCapabilities(mContact);
        if (remoteCapabilities == null) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder(
                        "Cannot send message on one-to-one chat with contact '").append(mContact)
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
                    if (logger.isActivated()) {
                        logger.debug(new StringBuilder(
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
            if (logger.isActivated()) {
                logger.debug(new StringBuilder(
                        "Cannot send message on one-to-one chat with contact '").append(mContact)
                        .append("' as IM capabilities are not supported for that contact.")
                        .toString());
            }
            return false;
        }
        return true;
    }

    /**
     * Add chat message to Db
     * 
     * @param msg InstantMessage
     * @param state state of message
     */
    private void addOutgoingChatMessage(ChatMessage msg, Status status) {
        mMessagingLog.addOutgoingOneToOneChatMessage(msg, status, ReasonCode.UNSPECIFIED);
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(msg.getMimeType());
        mBroadcaster.broadcastMessageStatusChanged(mContact, apiMimeType, msg.getMessageId(),
                status, ReasonCode.UNSPECIFIED);
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
     * Sends a plain text message
     * 
     * @param message Text message
     * @return Chat message
     */
    public IChatMessage sendMessage(String message) {
        if (logger.isActivated()) {
            logger.debug("Send text message.");
        }
        ChatMessage msg = ChatUtils.createTextMessage(mContact, message);
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
    }

    /**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc
     * @return ChatMessage
     */
    public IChatMessage sendMessage2(Geoloc geoloc) {
        if (logger.isActivated()) {
            logger.debug("Send geolocation message.");
        }
        ChatMessage msg = ChatUtils.createGeolocMessage(mContact, geoloc);
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
    }

    public void dequeueChatMessageWithinSession(ChatMessage message, OneToOneChatSession session) {
        String msgId = message.getMessageId();
        String mimeType = message.getMimeType();
        if (logger.isActivated()) {
            logger.debug("Dequeue chat message msgId=".concat(msgId));
        }
        mMessagingLog.dequeueChatMessage(message);
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(mimeType);
        mBroadcaster.broadcastMessageStatusChanged(mContact, apiMimeType, msgId, Status.SENDING,
                ReasonCode.UNSPECIFIED);
        synchronized (lock) {
            sendChatMessageWithinSession(session, message);
        }
    }

    public void dequeueChatMessageInNewSession(ChatMessage message) {
        String msgId = message.getMessageId();
        String mimeType = message.getMimeType();
        if (logger.isActivated()) {
            logger.debug("Dequeue chat message msgId=".concat(msgId));
        }
        mMessagingLog.dequeueChatMessage(message);
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(mimeType);
        mBroadcaster.broadcastMessageStatusChanged(mContact, apiMimeType, msgId, Status.SENDING,
                ReasonCode.UNSPECIFIED);
        synchronized (lock) {
            sendChatMessageInNewSession(message);
        }
    }

    /**
     * Sends a displayed delivery report for a given message ID
     * 
     * @param contact Contact ID
     * @param msgId Message ID
     */
    /* package private */void sendDisplayedDeliveryReport(final ContactId contact,
            final String msgId) {
        try {
            if (logger.isActivated()) {
                logger.debug("Set displayed delivery report for " + msgId);
            }
            TerminatingStoreAndForwardOneToOneChatMessageSession storeAndForwardSession = mImService
                    .getStoreAndForwardMsgSession(mContact);
            final OneToOneChatSession session = storeAndForwardSession != null ? storeAndForwardSession
                    : mImService.getOneToOneChatSession(mContact);
            if (session != null && session.isMediaEstablished()) {
                if (logger.isActivated()) {
                    logger.info("Use the original session to send the delivery status for " + msgId);
                }

                new Thread() {
                    public void run() {
                        session.sendMsrpMessageDeliveryStatus(contact, msgId,
                                ImdnDocument.DELIVERY_STATUS_DISPLAYED);
                    }
                }.start();
            } else {
                if (logger.isActivated()) {
                    logger.info("No suitable session found to send the delivery status for "
                            + msgId + " : use SIP message");
                }
                mImService.getImdnManager().sendMessageDeliveryStatus(contact, msgId,
                        ImdnDocument.DELIVERY_STATUS_DISPLAYED);
            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Could not send MSRP delivery status", e);
            }
        }
    }

    /**
     * Sends an is-composing event. The status is set to true when typing a message, else it is set
     * to false.
     * 
     * @param status Is-composing status
     * @see ImSessionStartMode
     */
    public void sendIsComposingEvent(final boolean status) {
        final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
        if (session == null) {
            if (logger.isActivated()) {
                logger.debug("Unable to send composing event '" + status
                        + "' since oneToOne chat session found with contact '" + mContact
                        + "' does not exist for now");
            }
            return;
        }
        if (session.getDialogPath().isSessionEstablished()) {
            session.sendIsComposingStatus(status);
            return;
        }
        if (!session.isInitiatedByRemote()) {
            return;
        }
        ImSessionStartMode imSessionStartMode = mRcsSettings.getImSessionStartMode();
        switch (imSessionStartMode) {
            case ON_OPENING:
            case ON_COMPOSING:
                if (logger.isActivated()) {
                    logger.debug("Core chat session is pending: auto accept it.");
                }
                session.acceptSession();
                break;
            default:
                break;
        }
    }

    /**
     * open the chat conversation. Note: if its an incoming pending chat session and the parameter
     * IM SESSION START is 0 then the session is accepted now.
     * 
     * @see ImSessionStartMode
     */
    public void openChat() {
        if (logger.isActivated()) {
            logger.info("Open a 1-1 chat session with " + mContact);
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
                    if (logger.isActivated()) {
                        logger.debug("Core chat session is pending: auto accept it, as IM_SESSION_START mode = 0");
                    }
                    session.acceptSession();
                }
            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Unexpected error", e);
            }
            // TODO: Exception handling in CR037
        }
    }

    /**
     * Resend a message which previously failed.
     * 
     * @param msgId
     */
    public void resendMessage(String msgId) {
        String mimeType = mMessagingLog.getMessageMimeType(msgId);
        ChatMessage msg = new ChatMessage(msgId, mContact,
                mMessagingLog.getChatMessageContent(msgId), mimeType, null, null);
        if (ServerApiUtils.isImsConnected()) {
            resendChatMessage(msg);
        } else {
            /* If the IMS is NOT connected at this time then re-queue message. */
            setChatMessageStatus(msgId, mimeType, Status.QUEUED);
        }
    }

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.core.ims.service.ImsSessionListener#handleSessionStarted ()
     */
    @Override
    public void handleSessionStarted(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session started");
        }
        mCore.getListener().tryToDequeueOneToOneChatMessages(mContact, mImService);
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionAborted
     * (TerminationReason)
     */
    @Override
    public void handleSessionAborted(ContactId contact, TerminationReason reason) {
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Session aborted (reason ").append(reason).append(")")
                    .toString());
        }
        synchronized (lock) {
            mChatService.removeOneToOneChat(mContact);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.core.ims.service.ImsSessionListener# handleSessionTerminatedByRemote()
     */
    @Override
    public void handleSessionTerminatedByRemote(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session terminated by remote");
        }
        synchronized (lock) {
            mChatService.removeOneToOneChat(contact);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.core.ims.service.im.chat.ChatSessionListener# handleReceiveMessage
     * (com.gsma.rcs.core.ims.service.im.chat.ChatMessage, boolean)
     */
    @Override
    public void handleReceiveMessage(ChatMessage msg, boolean imdnDisplayedRequested) {
        String msgId = msg.getMessageId();
        if (logger.isActivated()) {
            logger.info(new StringBuilder("New IM with messageId '").append(msgId)
                    .append("' received from ").append(mContact).append(".").toString());
        }
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(msg.getMimeType());
        synchronized (lock) {
            mMessagingLog.addIncomingOneToOneChatMessage(msg, imdnDisplayedRequested);
            mBroadcaster.broadcastMessageReceived(apiMimeType, msgId);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.core.ims.service.im.chat.ChatSessionListener#
     * handleOneToOneIncomingSessionInitiationError
     * (com.gsma.rcs.core.ims.service.im.chat.ChatError)
     */
    @Override
    public void handleIncomingSessionInitiationError(ChatError error) {
        int errorCode = error.getErrorCode();
        if (logger.isActivated()) {
            logger.info("IncomingSessionInitiation error " + errorCode);
        }
        synchronized (lock) {
            mChatService.removeOneToOneChat(mContact);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.core.ims.service.im.chat.ChatSessionListener#handleImError
     * (com.gsma.rcs.core.ims.service.im.chat.ChatError)
     */
    @Override
    public void handleImError(ChatError error, ChatMessage message) {
        int errorCode = error.getErrorCode();
        if (logger.isActivated()) {
            logger.info("IM error " + errorCode);
        }
        synchronized (lock) {
            mChatService.removeOneToOneChat(mContact);
            switch (errorCode) {
                case ChatError.SESSION_INITIATION_FAILED:
                case ChatError.SESSION_INITIATION_CANCELLED:
                    if (message != null) {
                        String msgId = message.getMessageId();
                        String mimeType = message.getMimeType();
                        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(mimeType);
                        mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.FAILED,
                                ReasonCode.FAILED_SEND);
                        mBroadcaster.broadcastMessageStatusChanged(mContact, apiMimeType, msgId,
                                Status.FAILED, ReasonCode.FAILED_SEND);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void handleIsComposingEvent(ContactId contact, boolean status) {
        if (logger.isActivated()) {
            logger.info(new StringBuilder("").append(contact)
                    .append(" is composing status set to ").append(status).toString());
        }
        synchronized (lock) {
            mBroadcaster.broadcastComposingEvent(contact, status);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.core.ims.service.im.chat.ChatSessionListener# handleMessageSending(
     * com.gsma.rcs.core.ims.service.im.chat.ChatMessage)
     */
    @Override
    public void handleMessageSending(ChatMessage msg) {
        String msgId = msg.getMessageId();
        String networkMimeType = msg.getMimeType();
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Message is being sent; msgId=").append(msgId)
                    .append("; mimeType").append(networkMimeType).append(".").toString());
        }
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(networkMimeType);
        synchronized (lock) {
            mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.SENDING,
                    ReasonCode.UNSPECIFIED);
            mBroadcaster.broadcastMessageStatusChanged(mContact, apiMimeType, msgId,
                    Status.SENDING, ReasonCode.UNSPECIFIED);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.core.ims.service.im.chat.ChatSessionListener# handleMessageSent(
     * com.gsma.rcs.core.ims.service.im.chat.ChatMessage)
     */
    @Override
    public void handleMessageSent(String msgId, String mimeType) {
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Message sent; msgId=").append(msgId).append(".")
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

    /*
     * (non-Javadoc)
     * @see com.gsma.rcs.core.ims.service.im.chat.ChatSessionListener# handleMessageFailedSend(
     * com.gsma.rcs.core.ims.service.im.chat.ChatMessage)
     */
    @Override
    public void handleMessageFailedSend(String msgId, String mimeType) {
        String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(mimeType);
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Message sent; msgId=").append(msgId).append(".")
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
        String msgId = imdn.getMsgId();
        String status = imdn.getStatus();
        if (logger.isActivated()) {
            logger.info(new StringBuilder("New message delivery status for message ").append(msgId)
                    .append(", status ").append(status).append(".").toString());
        }
        String mimeType = mMessagingLog.getMessageMimeType(msgId);
        if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
            ReasonCode reasonCode = imdnToFailedReasonCode(imdn);
            synchronized (lock) {
                mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.FAILED, reasonCode);

                mBroadcaster.broadcastMessageStatusChanged(contact, mimeType, msgId, Status.FAILED,
                        reasonCode);
            }

        } else if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
            synchronized (lock) {
                mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.DELIVERED,
                        ReasonCode.UNSPECIFIED);

                mBroadcaster.broadcastMessageStatusChanged(contact, mimeType, msgId,
                        Status.DELIVERED, ReasonCode.UNSPECIFIED);
            }

        } else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
            synchronized (lock) {
                mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.DISPLAYED,
                        ReasonCode.UNSPECIFIED);

                mBroadcaster.broadcastMessageStatusChanged(contact, mimeType, msgId,
                        Status.DISPLAYED, ReasonCode.UNSPECIFIED);
            }
        }
    }

    @Override
    public void handleSessionRejectedByUser(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session rejected by user.");
        }
        synchronized (lock) {
            mChatService.removeOneToOneChat(mContact);
        }
    }

    @Override
    public void handleSessionRejectedByTimeout(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session rejected by time-out.");
        }
        synchronized (lock) {
            mChatService.removeOneToOneChat(mContact);
        }
    }

    @Override
    public void handleSessionRejectedByRemote(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Session rejected by remote.");
        }
        synchronized (lock) {
            mChatService.removeOneToOneChat(mContact);
        }
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
