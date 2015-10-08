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

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.history.HistoryLog;
import com.gsma.rcs.provider.messaging.ChatMessagePersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.GroupChatPersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.broadcaster.GroupChatEventBroadcaster;
import com.gsma.rcs.service.broadcaster.OneToOneChatEventBroadcaster;
import com.gsma.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.chat.IChatService;
import com.gsma.services.rcs.chat.IChatServiceConfiguration;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.IOneToOneChat;
import com.gsma.services.rcs.chat.IOneToOneChatListener;
import com.gsma.services.rcs.contact.ContactId;

import android.os.RemoteException;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chat service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatServiceImpl extends IChatService.Stub {

    private final OneToOneChatEventBroadcaster mOneToOneChatEventBroadcaster = new OneToOneChatEventBroadcaster();

    private final GroupChatEventBroadcaster mGroupChatEventBroadcaster = new GroupChatEventBroadcaster();

    private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

    private final InstantMessagingService mImService;

    private final MessagingLog mMessagingLog;

    private final HistoryLog mHistoryLog;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private final Map<ContactId, OneToOneChatImpl> mOneToOneChatCache = new HashMap<ContactId, OneToOneChatImpl>();

    private final Map<String, GroupChatImpl> mGroupChatCache = new HashMap<String, GroupChatImpl>();

    /**
     * The sLogger
     */
    private static final Logger sLogger = Logger.getLogger(ChatServiceImpl.class.getSimpleName());

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param messagingLog MessagingLog
     * @param historyLog HistoryLog
     * @param rcsSettings RcsSettings
     * @param contactManager ContactManager
     * @param core Core
     * @param imOperationExecutor im operation ExecutorService
     * @param imsLock ims operations lock
     * @param fileTransferService FileTransferServiceImpl
     * @param oneToOneUndeliveredImManager OneToOneUndeliveredImManager
     */
    public ChatServiceImpl(InstantMessagingService imService, MessagingLog messagingLog,
            HistoryLog historyLog, RcsSettings rcsSettings, ContactManager contactManager) {
        if (sLogger.isActivated()) {
            sLogger.info("Chat service API is loaded");
        }
        mImService = imService;
        mImService.register(this);
        mMessagingLog = messagingLog;
        mHistoryLog = historyLog;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
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

    /**
     * Tries to send a displayed delivery report for a one to one chat
     * 
     * @param msgId Message ID
     * @param remote Remote contact
     * @param timestamp Timestamp sent in payload for IMDN datetime
     * @throws NetworkException
     */
    public void sendOne2OneDisplayedDeliveryReport(String msgId, ContactId remote, long timestamp)
            throws NetworkException {
        getOrCreateOneToOneChat(remote).sendDisplayedDeliveryReport(remote, msgId, timestamp);
    }

    /**
     * Tries to send a displayed delivery report for a group chat
     * 
     * @param msgId Message ID
     * @param contact Contact ID
     * @param timestamp Timestamp sent in payload for IMDN datetime
     * @throws NetworkException
     */
    public void sendGroupChatDisplayedDeliveryReport(final String msgId, final ContactId contact,
            final long timestamp, String chatId) throws NetworkException {
        final GroupChatSession session = mImService.getGroupChatSession(chatId);
        if (session == null || !session.isMediaEstablished()) {
            if (sLogger.isActivated()) {
                sLogger.info("No suitable session found to send the delivery status for " + msgId
                        + " : use SIP message");
            }
            mImService.getImdnManager().sendMessageDeliveryStatus(chatId, contact, msgId,
                    ImdnDocument.DELIVERY_STATUS_DISPLAYED, timestamp);
            return;
        }

        if (sLogger.isActivated()) {
            sLogger.info("Using the available session to send displayed for " + msgId);
        }
        session.sendMsrpMessageDeliveryStatus(contact, msgId,
                ImdnDocument.DELIVERY_STATUS_DISPLAYED, timestamp);
    }

    /**
     * Close API
     */
    public void close() {
        // Clear list of sessions
        mOneToOneChatCache.clear();
        mGroupChatCache.clear();

        if (sLogger.isActivated()) {
            sLogger.info("Chat service API is closed");
        }
    }

    /**
     * Returns true if the service is registered to the platform, else returns false
     * 
     * @return Returns true if registered else returns false
     */
    @Override
    public boolean isServiceRegistered() {
        return ServerApiUtils.isImsConnected();
    }

    /**
     * Return the reason code for IMS service registration
     * 
     * @return the reason code for IMS service registration
     */
    @Override
    public int getServiceRegistrationReasonCode() {
        return ServerApiUtils.getServiceRegistrationReasonCode().toInt();
    }

    /**
     * Registers a listener on service registration events
     * 
     * @param listener Service registration listener
     */
    @Override
    public void addEventListener(IRcsServiceRegistrationListener listener) {
        if (sLogger.isActivated()) {
            sLogger.info("Add a service listener");
        }
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.addEventListener(listener);
        }
    }

    /**
     * Unregisters a listener on service registration events
     * 
     * @param listener Service registration listener
     */
    @Override
    public void removeEventListener(IRcsServiceRegistrationListener listener) {
        if (sLogger.isActivated()) {
            sLogger.info("Remove a service listener");
        }
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.removeEventListener(listener);
        }
    }

    /**
     * Notifies registration event
     */
    public void notifyRegistration() {
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.broadcastServiceRegistered();
        }
    }

    /**
     * Notifies unregistration event
     * 
     * @param reasonCode for unregistration
     */
    public void notifyUnRegistration(RcsServiceRegistration.ReasonCode reasonCode) {
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered(reasonCode);
        }
    }

    /**
     * Receive a new chat invitation
     * 
     * @param session Chat session
     */
    public void receiveOneToOneChatInvitation(OneToOneChatSession session) {
        ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Chat invitation from ").append(contact)
                    .append(" (display=").append(session.getRemoteDisplayName()).append(")")
                    .toString());
        }
        OneToOneChatImpl oneToOneChat = getOrCreateOneToOneChat(contact);
        session.addListener(oneToOneChat);

        ChatMessage firstMessage = session.getFirstMessage();
        if (firstMessage != null) {
            mOneToOneChatEventBroadcaster.broadcastMessageReceived(firstMessage.getMimeType(),
                    firstMessage.getMessageId());
        }
    }

    /**
     * Receive message delivery status
     * 
     * @param contact Contact ID
     * @param imdn Imdn document
     */
    public void onOneToOneMessageDeliveryStatusReceived(ContactId contact, ImdnDocument imdn) {
        String status = imdn.getStatus();
        String msgId = imdn.getMsgId();
        String notificationType = imdn.getNotificationType();
        long timestamp = imdn.getDateTime();

        if (sLogger.isActivated()) {
            sLogger.info("Receive message delivery status for message " + msgId + ", status "
                    + status + "notificationType=" + notificationType);
        }

        String mimeType = mMessagingLog.getMessageMimeType(msgId);
        if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
            ReasonCode reasonCode = imdnToFailedReasonCode(imdn);
            synchronized (mLock) {
                if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.FAILED,
                        reasonCode)) {
                    mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, mimeType,
                            msgId, Status.FAILED, reasonCode);
                }
            }

        } else if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
            mImService.getDeliveryExpirationManager().cancelDeliveryTimeoutAlarm(msgId);
            synchronized (mLock) {
                if (mMessagingLog.setChatMessageStatusDelivered(msgId, timestamp)) {
                    mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, mimeType,
                            msgId, Status.DELIVERED, ReasonCode.UNSPECIFIED);
                }
            }

        } else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
            mImService.getDeliveryExpirationManager().cancelDeliveryTimeoutAlarm(msgId);
            synchronized (mLock) {
                if (mMessagingLog.setChatMessageStatusDisplayed(msgId, timestamp)) {
                    mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, mimeType,
                            msgId, Status.DISPLAYED, ReasonCode.UNSPECIFIED);
                }
            }
        }
    }

    /**
     * Add a oneToOne chat in the list
     * 
     * @param contact Contact ID
     * @param oneToOneChat OneToOne Chat
     */
    public void addOneToOneChat(ContactId contact, OneToOneChatImpl oneToOneChat) {
        mOneToOneChatCache.put(contact, oneToOneChat);
        if (sLogger.isActivated()) {
            sLogger.debug("Add oneToOne chat to list (size=" + mOneToOneChatCache.size() + ") for "
                    + contact);
        }
    }

    /**
     * Remove a oneToOne chat from the list
     * 
     * @param contact Contact ID
     */
    public void removeOneToOneChat(ContactId contact) {
        mOneToOneChatCache.remove(contact);
        if (sLogger.isActivated()) {
            sLogger.debug("Remove oneToOne chat from list (size=" + mOneToOneChatCache.size()
                    + ") for " + contact);
        }
    }

    public OneToOneChatImpl getOrCreateOneToOneChat(ContactId contact) {
        OneToOneChatImpl oneToOneChat = mOneToOneChatCache.get(contact);
        if (oneToOneChat == null) {
            oneToOneChat = new OneToOneChatImpl(mImService, contact, mOneToOneChatEventBroadcaster,
                    mMessagingLog, mHistoryLog, mRcsSettings, this, mContactManager);
            mOneToOneChatCache.put(contact, oneToOneChat);
        }
        return oneToOneChat;
    }

    /**
     * Returns a chat from its unique ID
     * 
     * @param contact Contact ID
     * @return IOneToOneChat
     * @throws RemoteException
     */
    @Override
    public IOneToOneChat getOneToOneChat(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        try {
            return getOrCreateOneToOneChat(contact);

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
     * Receive a new group chat invitation
     * 
     * @param session Chat session
     */
    public void receiveGroupChatInvitation(GroupChatSession session) {
        ContactId remote = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Group chat invitation from ").append(remote)
                    .append(" (display=").append(session.getRemoteDisplayName()).append(")")
                    .toString());
        }
        String chatId = session.getContributionID();
        GroupChatImpl groupChat = getOrCreateGroupChat(chatId);
        session.addListener(groupChat);
    }

    /**
     * Add a group chat in the list
     * 
     * @param groupChat Group chat
     */
    /* package private */void addGroupChat(GroupChatImpl groupChat) {
        String chatId = groupChat.getChatId();
        mGroupChatCache.put(chatId, groupChat);
        if (sLogger.isActivated()) {
            sLogger.debug("Add Group Chat to list (size=" + mGroupChatCache.size()
                    + ") for chatId " + chatId);
        }
    }

    /**
     * Remove a group chat from the list
     * 
     * @param chatId Chat ID
     */
    public void removeGroupChat(String chatId) {
        mGroupChatCache.remove(chatId);
        if (sLogger.isActivated()) {
            sLogger.debug("Remove Group Chat to list (size=" + mGroupChatCache.size()
                    + ") for chatId " + chatId);
        }
    }

    /**
     * Initiates a group chat with a group of contact and returns a GroupChat instance. The subject
     * is optional and may be null.
     * 
     * @param contacts List of contact IDs
     * @param subject Subject
     * @return instance of IGroupChat
     * @throws RemoteException <p>
     *             Note: List is used instead of Set because AIDL does only support List
     *             </p>
     */
    @Override
    public IGroupChat initiateGroupChat(List<ContactId> contacts, String subject)
            throws RemoteException {
        if (contacts == null || contacts.isEmpty()) {
            throw new ServerApiIllegalArgumentException(
                    "GroupChat participants list must not be null or empty!");
        }
        if (contacts.size() > mRcsSettings.getMaxChatParticipants() - 1) {
            throw new ServerApiIllegalArgumentException(
                    "Number of contacts exeeds maximum number that can be added to a group chat!");
        }
        if (!mRcsSettings.isGroupChatActivated()) {
            throw new ServerApiPermissionDeniedException(
                    "The GroupChat feature is not activated on the connected IMS server!");
        }
        ServerApiUtils.testIms();
        if (sLogger.isActivated()) {
            sLogger.info("Initiate an ad-hoc group chat session");
        }
        try {
            long timestamp = System.currentTimeMillis();
            final GroupChatSession session = mImService.createOriginatingAdHocGroupChatSession(
                    new HashSet<ContactId>(contacts), subject, timestamp);
            final String chatId = session.getContributionID();
            final GroupChatImpl groupChat = getOrCreateGroupChat(chatId);

            mMessagingLog.addGroupChat(session.getContributionID(), session.getRemoteContact(),
                    session.getSubject(), session.getParticipants(), GroupChat.State.INITIATING,
                    GroupChat.ReasonCode.UNSPECIFIED, Direction.OUTGOING, timestamp);

            mImService.scheduleImOperation(new Runnable() {
                public void run() {
                    try {
                        if (!isServiceRegistered() || !mImService.isChatSessionAvailable()) {
                            sLogger.error(new StringBuilder(
                                    "Failed to initiate group chat with chatId '").append(chatId)
                                    .append("'!").toString());
                            setGroupChatStateAndReasonCode(chatId, GroupChat.State.FAILED,
                                    GroupChat.ReasonCode.FAILED_INITIATION);
                            return;
                        }
                        session.addListener(groupChat);
                        session.startSession();

                    } catch (PayloadException e) {
                        sLogger.error(new StringBuilder(
                                "Failed to initiate group chat with chatId '").append(chatId)
                                .append("'!").toString(), e);
                        setGroupChatStateAndReasonCode(chatId, GroupChat.State.FAILED,
                                GroupChat.ReasonCode.FAILED_INITIATION);

                    } catch (NetworkException e) {
                        if (sLogger.isActivated()) {
                            sLogger.debug(new StringBuilder(
                                    "Failed to initiate group chat with chatId '").append(chatId)
                                    .append("'! (").append(e.getMessage()).append(")").toString());
                        }
                        setGroupChatStateAndReasonCode(chatId, GroupChat.State.FAILED,
                                GroupChat.ReasonCode.FAILED_INITIATION);

                    } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                        sLogger.error(new StringBuilder(
                                "Failed to initiate group chat with chatId '").append(chatId)
                                .append("'!").toString(), e);
                        setGroupChatStateAndReasonCode(chatId, GroupChat.State.FAILED,
                                GroupChat.ReasonCode.FAILED_INITIATION);
                    }
                }
            });
            return groupChat;

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

    public GroupChatImpl getOrCreateGroupChat(String chatId) {
        GroupChatImpl groupChat = mGroupChatCache.get(chatId);
        if (groupChat == null) {
            GroupChatPersistedStorageAccessor persistedStorage = new GroupChatPersistedStorageAccessor(
                    chatId, mMessagingLog, mRcsSettings);
            groupChat = new GroupChatImpl(mImService, chatId, mGroupChatEventBroadcaster,
                    persistedStorage, mRcsSettings, this, mContactManager, mMessagingLog,
                    mHistoryLog);
            mGroupChatCache.put(chatId, groupChat);
        }
        return groupChat;
    }

    /**
     * Returns a group chat from its unique ID. An exception is thrown if the chat ID does not exist
     * 
     * @param chatId Chat ID
     * @return IGroupChat
     * @throws RemoteException
     */
    @Override
    public IGroupChat getGroupChat(String chatId) throws RemoteException {
        if (TextUtils.isEmpty(chatId)) {
            throw new ServerApiIllegalArgumentException("chatId must not be null or empty!");
        }
        try {
            return getOrCreateGroupChat(chatId);

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
     * Returns true if it is possible to initiate a group chat now, else returns false.
     * 
     * @return boolean
     * @throws RemoteException
     */
    @Override
    public boolean isAllowedToInitiateGroupChat() throws RemoteException {
        try {
            if (!mRcsSettings.isGroupChatActivated()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot initiate group chat as group chat feature is not supported.");
                }
                return false;
            }
            if (!mRcsSettings.getMyCapabilities().isImSessionSupported()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot initiate group chat as IM capabilities are not supported for self.");
                }
                return false;
            }
            if (!ServerApiUtils.isImsConnected()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot initiate group chat as IMS is not connected.");
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

    /**
     * Returns true if it's possible to initiate a new group chat with the specified contactId right
     * now, else returns false.
     * 
     * @param contact
     * @return true if it's possible to initiate a new group chat
     * @throws RemoteException
     */
    @Override
    public boolean isAllowedToInitiateGroupChat2(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        try {
            if (!isAllowedToInitiateGroupChat()) {
                return false;
            }
            Capabilities contactCapabilities = mContactManager.getContactCapabilities(contact);
            if (contactCapabilities == null) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot initiate group chat as the capabilities of the participant '")
                            .append(contact).append("' are not known.").toString());
                }
                return false;
            }
            if (!contactCapabilities.isImSessionSupported()) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot initiate group chat as the participant '").append(contact)
                            .append("' does not have IM capabilities.").toString());
                }
                return false;
            }
            if (mRcsSettings.isGroupChatInviteIfFullStoreForwardSupported()
                    && !contactCapabilities.isGroupChatStoreForwardSupported()) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot initiate group chat as the participant '").append(contact)
                            .append("' does not have store and forward feature supported.")
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

    /**
     * Deletes all one to one chat from history and abort/reject any associated ongoing session if
     * such exists.
     */
    @Override
    public void deleteOneToOneChats() {
        mImService.tryToDeleteOneToOneChats();
    }

    /**
     * Deletes all group chat from history and abort/reject any associated ongoing session if such
     * exists.
     */
    @Override
    public void deleteGroupChats() {
        mImService.tryToDeleteGroupChats();
    }

    /**
     * Deletes a one to one chat with a given contact from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param contact
     */
    @Override
    public void deleteOneToOneChat(ContactId contact) {
        mImService.tryToDeleteOneToOneChat(contact);
    }

    /**
     * Delete a group chat by its chat id from history and abort/reject any associated ongoing
     * session if such exists.
     * 
     * @param chatId
     */
    @Override
    public void deleteGroupChat(String chatId) {
        mImService.tryToDeleteGroupChat(chatId);
    }

    /**
     * Delete a message from its message id from history. Will resolve if the message is one to one
     * or from a group chat.
     * 
     * @param msgId
     */
    @Override
    public void deleteMessage(String msgId) {
        mImService.tryToDeleteChatMessage(msgId);
    }

    /**
     * Disables and clears any delivery expiration for a set of chat messages regardless if the
     * delivery of them has expired already or not.
     * 
     * @param msgIds
     * @throws RemoteException
     */
    @Override
    public void clearMessageDeliveryExpiration(final List<String> msgIds) throws RemoteException {
        if (msgIds == null || msgIds.isEmpty()) {
            throw new ServerApiIllegalArgumentException(
                    "Undelivered chat messageId list must not be null and empty!");
        }
        mImService.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    for (String msgId : msgIds) {
                        mImService.getDeliveryExpirationManager().cancelDeliveryTimeoutAlarm(msgId);
                    }
                    mMessagingLog.clearMessageDeliveryExpiration(msgIds);
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to mark message as read!", e);
                }
            }
        });
    }

    /**
     * Adds a listener on one-to-one chat events
     * 
     * @param listener One-to-One chat event listener
     * @throws RemoteException
     */
    @Override
    public void addEventListener2(IOneToOneChatListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add an OneToOne chat event listener");
        }
        try {
            synchronized (mLock) {
                mOneToOneChatEventBroadcaster.addOneToOneChatEventListener(listener);
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
     * Removes a listener on one-to-one chat events
     * 
     * @param listener One-to-One chat event listener
     * @throws RemoteException
     */
    @Override
    public void removeEventListener2(IOneToOneChatListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove an OneToOne chat event listener");
        }
        try {
            synchronized (mLock) {
                mOneToOneChatEventBroadcaster.removeOneToOneChatEventListener(listener);
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
     * Adds a listener on group chat events
     * 
     * @param listener Group chat event listener
     * @throws RemoteException
     */
    @Override
    public void addEventListener3(IGroupChatListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add a Group chat event listener");
        }
        try {
            synchronized (mLock) {
                mGroupChatEventBroadcaster.addGroupChatEventListener(listener);
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
     * Removes a listener on group chat events
     * 
     * @param listener Group chat event listener
     * @throws RemoteException
     */
    @Override
    public void removeEventListener3(IGroupChatListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove a group chat event listener");
        }
        try {
            synchronized (mLock) {
                mGroupChatEventBroadcaster.removeGroupChatEventListener(listener);
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
     * Returns the configuration of the chat service
     * 
     * @return Configuration
     */
    @Override
    public IChatServiceConfiguration getConfiguration() {
        return new ChatServiceConfigurationImpl(mRcsSettings);
    }

    /**
     * Mark a received message as read (ie. displayed in the UI)
     * 
     * @param msgId Message ID
     * @throws RemoteException
     */
    @Override
    public void markMessageAsRead(final String msgId) throws RemoteException {
        if (TextUtils.isEmpty(msgId)) {
            throw new ServerApiIllegalArgumentException("msgId must not be null or empty!");
        }
        mImService.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    mMessagingLog.markMessageAsRead(msgId);
                    if (mRcsSettings.isImReportsActivated()
                            && mRcsSettings.isRespondToDisplayReports()) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("tryToDispatchAllPendingDisplayNotifications for msgID "
                                    .concat(msgId));
                        }
                        ImdnManager imdnManager = mImService.getImdnManager();
                        if (imdnManager.isSendOneToOneDeliveryDisplayedReportsEnabled()
                                || imdnManager.isSendGroupDeliveryDisplayedReportsEnabled()) {
                            mImService.tryToDispatchAllPendingDisplayNotifications();
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
                    sLogger.error("Failed to mark message as read!", e);
                }
            }
        });
    }

    /**
     * Returns service version
     * 
     * @return Version
     * @see VERSION_CODES
     */
    public int getServiceVersion() {
        return RcsService.Build.API_VERSION;
    }

    /**
     * Add and broadcast group chat invitation rejections.
     * 
     * @param chatId Chat ID
     * @param contact Contact ID
     * @param subject Subject
     * @param participants Participants
     * @param reasonCode Reason code
     * @param timestamp Local timestamp when got invitation
     */
    public void addGroupChatInvitationRejected(String chatId, ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, GroupChat.ReasonCode reasonCode,
            long timestamp) {

        mMessagingLog.addGroupChat(chatId, contact, subject, participants,
                GroupChat.State.REJECTED, reasonCode, Direction.INCOMING, timestamp);

        mGroupChatEventBroadcaster.broadcastInvitation(chatId);
    }

    /**
     * Handle one-to-one chat session initiation
     * 
     * @param session OneToOneChatSession
     */
    public void receiveOneToOneChatSessionInitiation(OneToOneChatSession session) {
        ContactId contact = session.getRemoteContact();
        OneToOneChatImpl oneToOneChat = getOrCreateOneToOneChat(contact);
        session.addListener(oneToOneChat);
    }

    /**
     * Returns a chat message from its unique ID
     * 
     * @param msgId
     * @return IChatMessage
     * @throws RemoteException
     */
    @Override
    public IChatMessage getChatMessage(String msgId) throws RemoteException {
        if (TextUtils.isEmpty(msgId)) {
            throw new ServerApiIllegalArgumentException("msgId must not be null or empty!");
        }
        try {
            ChatMessagePersistedStorageAccessor persistedStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msgId);
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
     * Handle rejoin group chat as part of send operation
     * 
     * @param chatId
     */
    public void rejoinGroupChatAsPartOfSendOperation(String chatId) throws PayloadException,
            NetworkException {
        GroupChatImpl groupChat = getOrCreateGroupChat(chatId);
        groupChat.setRejoinedAsPartOfSendOperation(true);
        groupChat.rejoinGroupChat();
    }

    /**
     * Handle rejoin group chat
     * 
     * @param chatId
     * @throws NetworkException
     * @throws PayloadException
     */
    public void rejoinGroupChat(String chatId) throws PayloadException, NetworkException {
        GroupChatImpl groupChat = getOrCreateGroupChat(chatId);
        groupChat.rejoinGroupChat();
    }

    /**
     * Returns the common service configuration
     * 
     * @return the common service configuration
     */
    @Override
    public ICommonServiceConfiguration getCommonConfiguration() {
        return new CommonServiceConfigurationImpl(mRcsSettings);
    }

    /**
     * Set one-one chat message status and reason code
     * 
     * @param msgId
     * @param mimeType
     * @param contact
     * @param status
     * @param reasonCode
     */
    public void setOneToOneChatMessageStatusAndReasonCode(String msgId, String mimeType,
            ContactId contact, Status status, ReasonCode reasonCode) {
        if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode)) {
            mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, mimeType, msgId,
                    status, reasonCode);
        }
    }

    /**
     * Set group chat message status and reason code
     * 
     * @param msgId
     * @param mimeType
     * @param chatId
     * @param status
     * @param reasonCode
     */
    public void setGroupChatMessageStatusAndReasonCode(String msgId, String mimeType,
            String chatId, Status status, ReasonCode reasonCode) {
        if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode)) {
            mGroupChatEventBroadcaster.broadcastMessageStatusChanged(chatId, mimeType, msgId,
                    status, reasonCode);
        }
    }

    public void broadcastGroupChatMessagesDeleted(String chatId, Set<String> msgIds) {
        mGroupChatEventBroadcaster.broadcastMessagesDeleted(chatId, msgIds);
    }

    public void broadcastGroupChatsDeleted(Set<String> chatIds) {
        mGroupChatEventBroadcaster.broadcastGroupChatsDeleted(chatIds);
    }

    public void broadcastOneToOneMessagesDeleted(ContactId contact, Set<String> msgIds) {
        mOneToOneChatEventBroadcaster.broadcastMessagesDeleted(contact, msgIds);
    }

    /**
     * Set group chat state and reason code
     * 
     * @param chatId
     * @param state
     * @param reasonCode
     */
    public void setGroupChatStateAndReasonCode(String chatId, State state,
            GroupChat.ReasonCode reasonCode) {
        if (mMessagingLog.setGroupChatStateAndReasonCode(chatId, state, reasonCode)) {
            mGroupChatEventBroadcaster.broadcastStateChanged(chatId, state, reasonCode);
        }
    }

    /**
     * Checks if the group chat with specific chatId is active
     * 
     * @param chatId
     * @return boolean
     */
    public boolean isGroupChatActive(String chatId) {
        return getOrCreateGroupChat(chatId).isGroupChatActive();
    }

    /**
     * Checks if the group chat is abandoned and can never be used to send or receive messages.
     * 
     * @param chatId
     * @return boolean
     */
    public boolean isGroupChatAbandoned(String chatId) {
        return getOrCreateGroupChat(chatId).isGroupChatAbandoned();
    }

    public void onDisplayReportSent(String chatId, ContactId remote, String msgId) {
        if (remote != null && chatId.equals(remote.toString())) {
            getOrCreateOneToOneChat(remote).onChatMessageDisplayReportSent(msgId);
        } else {
            getOrCreateGroupChat(chatId).onChatMessageDisplayReportSent(msgId);
        }
    }
}
