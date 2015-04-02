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

import com.gsma.rcs.ExceptionUtil;
import com.gsma.rcs.ServerApiBaseException;
import com.gsma.rcs.ServerApiGenericException;
import com.gsma.rcs.ServerApiIllegalArgumentException;
import com.gsma.rcs.ServerApiPermissionDeniedException;
import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatPersistedStorageAccessor;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.provider.eab.ContactsManager;
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
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.chat.IChatService;
import com.gsma.services.rcs.chat.IChatServiceConfiguration;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.IOneToOneChat;
import com.gsma.services.rcs.chat.IOneToOneChatListener;
import com.gsma.services.rcs.contact.ContactId;

import android.os.RemoteException;

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

    private final RcsSettings mRcsSettings;

    private final ContactsManager mContactManager;

    private final Core mCore;

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
     * @param rcsSettings RcsSettings
     * @param contactManager ContactsManager
     * @param core Core
     */
    public ChatServiceImpl(InstantMessagingService imService, MessagingLog messagingLog,
            RcsSettings rcsSettings, ContactsManager contactManager, Core core) {
        if (sLogger.isActivated()) {
            sLogger.info("Chat service API is loaded");
        }
        mImService = imService;
        mMessagingLog = messagingLog;
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

    /**
     * Tries to send a displayed delivery report
     * 
     * @param msgId Message ID
     * @param contact Contact ID
     * @param timestamp Timestamp sent in payload for IMDN datetime
     */
    public void tryToSendOne2OneDisplayedDeliveryReport(String msgId, ContactId contact,
            long timestamp) {
        try {
            OneToOneChatImpl chatImpl = mOneToOneChatCache.get(contact);
            if (chatImpl != null) {
                chatImpl.sendDisplayedDeliveryReport(contact, msgId, timestamp);
                return;
            }
            mImService.getImdnManager().sendMessageDeliveryStatus(contact, msgId,
                    ImdnDocument.DELIVERY_STATUS_DISPLAYED, timestamp);
        } catch (Exception ignore) {
            /*
             * Purposely ignoring exception since this method only makes an attempt to send report
             * and in case of failure the report will be sent later as postponed delivery report.
             */
        }
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
    public boolean isServiceRegistered() {
        return ServerApiUtils.isImsConnected();
    }

    /**
     * Return the reason code for IMS service registration
     * 
     * @return the reason code for IMS service registration
     */
    public int getServiceRegistrationReasonCode() {
        return ServerApiUtils.getServiceRegistrationReasonCode().toInt();
    }

    /**
     * Registers a listener on service registration events
     * 
     * @param listener Service registration listener
     */
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
        // Notify listeners
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
        // Notify listeners
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered(reasonCode);
        }
    }

    /**
     * Receive a new chat invitation
     * 
     * @param session Chat session
     */
    public void receiveOneOneChatInvitation(OneToOneChatSession session) {
        ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.info("Receive chat invitation from " + contact + " (display="
                    + session.getRemoteDisplayName() + ")");
        }
        // Update displayName of remote contact
        mContactManager.setContactDisplayName(contact, session.getRemoteDisplayName());

        // Add session in the list
        OneToOneChatImpl oneToOneChat = getOrCreateOneToOneChat(contact);
        session.addListener(oneToOneChat);
        addOneToOneChat(contact, oneToOneChat);

        ChatMessage firstMessage = session.getFirstMessage();
        if (firstMessage != null) {
            String mimeType = firstMessage.getMimeType();
            if (ChatUtils.isGeolocType(mimeType)) {
                mOneToOneChatEventBroadcaster.broadcastMessageReceived(MimeType.GEOLOC_MESSAGE,
                        firstMessage.getMessageId());
            } else if (ChatUtils.isTextPlainType(mimeType)) {
                mOneToOneChatEventBroadcaster.broadcastMessageReceived(MimeType.TEXT_MESSAGE,
                        firstMessage.getMessageId());
            } else {
                /*
                 * Only geolocation and text messages are valid parameters into this method. Thus it
                 * is certain at this point that it can only be a text message.
                 */
                throw new IllegalArgumentException(new StringBuilder("The mimetype '")
                        .append(mimeType)
                        .append("' is not supported by this chat service implementation!")
                        .toString());
            }
        }
    }

    /**
     * Receive message delivery status
     * 
     * @param contact Contact ID
     * @param imdn Imdn document
     */
    public void receiveMessageDeliveryStatus(ContactId contact, ImdnDocument imdn) {
        String status = imdn.getStatus();
        String msgId = imdn.getMsgId();
        String notificationType = imdn.getNotificationType();
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
                mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.FAILED, reasonCode);

                mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, mimeType,
                        msgId, Status.FAILED, reasonCode);
            }

        } else if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
            synchronized (mLock) {
                mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.DELIVERED,
                        ReasonCode.UNSPECIFIED);

                mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, mimeType,
                        msgId, Status.DELIVERED, ReasonCode.UNSPECIFIED);
            }

        } else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
            synchronized (mLock) {
                mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.DISPLAYED,
                        ReasonCode.UNSPECIFIED);

                mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, mimeType,
                        msgId, Status.DISPLAYED, ReasonCode.UNSPECIFIED);
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
    /* package private */void removeOneToOneChat(ContactId contact) {
        mOneToOneChatCache.remove(contact);
        if (sLogger.isActivated()) {
            sLogger.debug("Remove oneToOne chat from list (size=" + mOneToOneChatCache.size()
                    + ") for " + contact);
        }
    }

    public OneToOneChatImpl getOrCreateOneToOneChat(ContactId contact) {
        OneToOneChatImpl oneToOneChat = mOneToOneChatCache.get(contact);
        if (oneToOneChat != null) {
            return oneToOneChat;
        }
        return new OneToOneChatImpl(contact, mOneToOneChatEventBroadcaster, mImService,
                mMessagingLog, mRcsSettings, this, mContactManager, mCore);
    }

    /**
     * Returns a chat from its unique ID
     * 
     * @param contact Contact ID
     * @return IOneToOneChat
     * @throws ServerApiException
     */
    public IOneToOneChat getOneToOneChat(ContactId contact) throws ServerApiException {
        return getOrCreateOneToOneChat(contact);
    }

    /**
     * Receive a new group chat invitation
     * 
     * @param session Chat session
     */
    public void receiveGroupChatInvitation(GroupChatSession session) {
        if (sLogger.isActivated()) {
            sLogger.info("Receive group chat invitation from " + session.getRemoteContact()
                    + " (display=" + session.getRemoteDisplayName() + ")");
        }

        // Update displayName of remote contact
        mContactManager.setContactDisplayName(session.getRemoteContact(),
                session.getRemoteDisplayName());
        String chatId = session.getContributionID();
        GroupChatPersistedStorageAccessor storageAccessor = new GroupChatPersistedStorageAccessor(
                chatId, mMessagingLog, mRcsSettings);
        GroupChatImpl groupChat = new GroupChatImpl(chatId, mGroupChatEventBroadcaster, mImService,
                storageAccessor, mRcsSettings, mContactManager, this, mMessagingLog, mCore);
        session.addListener(groupChat);
        addGroupChat(groupChat);
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
    /* package private */void removeGroupChat(String chatId) {
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
    public IGroupChat initiateGroupChat(List<ContactId> contacts, String subject)
            throws RemoteException {
        if (contacts == null || contacts.isEmpty()) {
            throw new ServerApiIllegalArgumentException(
                    "GroupChat participants list must not be null or empty!");
        }
        if (!mRcsSettings.isGroupChatActivated()) {
            throw new ServerApiPermissionDeniedException(
                    "The GroupChat feature is not activated on the connected IMS server!");
        }
        // Test IMS connection
        ServerApiUtils.testIms();
        if (sLogger.isActivated()) {
            sLogger.info("Initiate an ad-hoc group chat session");
        }
        try {
            Set<ContactId> contactToInvite = new HashSet<ContactId>(contacts);
            long timestamp = System.currentTimeMillis();
            final GroupChatSession session = mImService.initiateAdhocGroupChatSession(
                    contactToInvite, subject, timestamp);
            String chatId = session.getContributionID();
            GroupChatPersistedStorageAccessor storageAccessor = new GroupChatPersistedStorageAccessor(
                    chatId, subject, Direction.OUTGOING, mMessagingLog, mRcsSettings, timestamp);
            GroupChatImpl groupChat = new GroupChatImpl(chatId, mGroupChatEventBroadcaster,
                    mImService, storageAccessor, mRcsSettings, mContactManager, this,
                    mMessagingLog, mCore);
            session.addListener(groupChat);

            mMessagingLog.addGroupChat(session.getContributionID(), session.getRemoteContact(),
                    session.getSubject(), session.getParticipants(), GroupChat.State.INITIATING,
                    GroupChat.ReasonCode.UNSPECIFIED, Direction.OUTGOING, timestamp);

            addGroupChat(groupChat);
            new Thread() {
                public void run() {
                    session.startSession();
                }
            }.start();
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
        if (groupChat != null) {
            return groupChat;
        }
        GroupChatPersistedStorageAccessor storageAccessor = new GroupChatPersistedStorageAccessor(
                chatId, mMessagingLog, mRcsSettings);
        return new GroupChatImpl(chatId, mGroupChatEventBroadcaster, mImService, storageAccessor,
                mRcsSettings, mContactManager, this, mMessagingLog, mCore);
    }

    /**
     * Returns a group chat from its unique ID. An exception is thrown if the chat ID does not exist
     * 
     * @param chatId Chat ID
     * @return IGroupChat
     * @throws ServerApiException
     */
    public IGroupChat getGroupChat(String chatId) throws ServerApiException {
        return getOrCreateGroupChat(chatId);
    }

    /**
     * Returns true if it is possible to initiate a group chat now, else returns false.
     * 
     * @return boolean
     */
    public boolean isAllowedToInitiateGroupChat() {
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
    }

    /**
     * Returns true if it's possible to initiate a new group chat with the specified contactId right
     * now, else returns false.
     * 
     * @param contact
     * @return true if it's possible to initiate a new group chat
     */
    public boolean isAllowedToInitiateGroupChat2(ContactId contact) {
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
                sLogger.debug(new StringBuilder("Cannot initiate group chat as the participant '")
                        .append(contact).append("' does not have IM capabilities.").toString());
            }
            return false;
        }
        if (mRcsSettings.isGroupChatInviteIfFullStoreForwardSupported()
                && !contactCapabilities.isGroupChatStoreForwardSupported()) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Cannot initiate group chat as the participant '")
                        .append(contact)
                        .append("' does not have store and forward feature supported.").toString());
            }
            return false;
        }
        return true;
    }

    /**
     * Deletes all one to one chat from history and abort/reject any associated ongoing session if
     * such exists.
     */
    public void deleteOneToOneChats() {
        throw new UnsupportedOperationException("This method has not been implemented yet!");
    }

    /**
     * Deletes all group chat from history and abort/reject any associated ongoing session if such
     * exists.
     */
    public void deleteGroupChats() {
        throw new UnsupportedOperationException("This method has not been implemented yet!");
    }

    /**
     * Deletes a one to one chat with a given contact from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param contact
     */
    public void deleteOneToOneChat(ContactId contact) {
        throw new UnsupportedOperationException("This method has not been implemented yet!");
    }

    /**
     * Delete a group chat by its chat id from history and abort/reject any associated ongoing
     * session if such exists.
     * 
     * @param chatId
     */
    public void deleteGroupChat(String chatId) {
        throw new UnsupportedOperationException("This method has not been implemented yet!");
    }

    /**
     * Delete a message from its message id from history.
     * 
     * @param msgId
     */
    public void deleteMessage(String msgId) {
        throw new UnsupportedOperationException("This method has not been implemented yet!");
    }

    /**
     * Marks undelivered chat messages to indicate that messages have been processed.
     * 
     * @param msgIds
     */
    public void markUndeliveredMessagesAsProcessed(List<String> msgIds) {
        throw new UnsupportedOperationException("This method has not been implemented yet!");
    }

    /**
     * Adds a listener on one-to-one chat events
     * 
     * @param listener One-to-One chat event listener
     * @throws RemoteException
     */
    public void addEventListener2(IOneToOneChatListener listener) throws RemoteException {
        if (sLogger.isActivated()) {
            sLogger.info("Add an OneToOne chat event listener");
        }
        synchronized (mLock) {
            mOneToOneChatEventBroadcaster.addOneToOneChatEventListener(listener);
        }
    }

    /**
     * Removes a listener on one-to-one chat events
     * 
     * @param listener One-to-One chat event listener
     * @throws RemoteException
     */
    public void removeEventListener2(IOneToOneChatListener listener) throws RemoteException {
        if (sLogger.isActivated()) {
            sLogger.info("Remove an OneToOne chat event listener");
        }
        synchronized (mLock) {
            mOneToOneChatEventBroadcaster.removeOneToOneChatEventListener(listener);
        }
    }

    /**
     * Adds a listener on group chat events
     * 
     * @param listener Group chat event listener
     * @throws ServerApiException
     */
    public void addEventListener3(IGroupChatListener listener) throws ServerApiException {
        if (sLogger.isActivated()) {
            sLogger.info("Add a Group chat event listener");
        }
        synchronized (mLock) {
            mGroupChatEventBroadcaster.addGroupChatEventListener(listener);
        }
    }

    /**
     * Removes a listener on group chat events
     * 
     * @param listener Group chat event listener
     * @throws ServerApiException
     */
    public void removeEventListener3(IGroupChatListener listener) throws ServerApiException {
        if (sLogger.isActivated()) {
            sLogger.info("Remove a group chat event listener");
        }
        synchronized (mLock) {
            mGroupChatEventBroadcaster.removeGroupChatEventListener(listener);
        }
    }

    /**
     * Returns the configuration of the chat service
     * 
     * @return Configuration
     */
    public IChatServiceConfiguration getConfiguration() {
        return new ChatServiceConfigurationImpl(mRcsSettings);
    }

    /**
     * Mark a received message as read (ie. displayed in the UI)
     * 
     * @param msgId Message ID
     * @throws ServerApiException
     */
    @Override
    public void markMessageAsRead(String msgId) throws ServerApiException {
        mMessagingLog.markMessageAsRead(msgId);
        if (mRcsSettings.isImReportsActivated() && mRcsSettings.isRespondToDisplayReports()) {
            if (sLogger.isActivated()) {
                sLogger.debug("tryToDispatchAllPendingDisplayNotifications for msgID "
                        .concat(msgId));
            }
            mCore.getListener().tryToDispatchAllPendingDisplayNotifications();
        }
    }

    /**
     * Returns service version
     * 
     * @return Version
     * @see VERSION_CODES
     * @throws ServerApiException
     */
    public int getServiceVersion() throws ServerApiException {
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
    public void addAndBroadcastGroupChatInvitationRejected(String chatId, ContactId contact,
            String subject, Map<ContactId, ParticipantStatus> participants,
            GroupChat.ReasonCode reasonCode, long timestamp) {

        mMessagingLog.addGroupChat(chatId, contact, subject, participants,
                GroupChat.State.REJECTED, reasonCode, Direction.INCOMING, timestamp);

        mGroupChatEventBroadcaster.broadcastInvitation(chatId);
    }

    /**
     * Handle one-to-one chat session initiation
     * 
     * @param session OneToOneChatSession
     */
    public void handleOneToOneChatSessionInitiation(OneToOneChatSession session) {
        ContactId contact = session.getRemoteContact();
        OneToOneChatImpl oneToOneChat = new OneToOneChatImpl(contact,
                mOneToOneChatEventBroadcaster, mImService, mMessagingLog, mRcsSettings, this,
                mContactManager, mCore);
        session.addListener(oneToOneChat);
        addOneToOneChat(contact, oneToOneChat);
    }

    /**
     * Returns a chat message from its unique ID
     * 
     * @param msgId
     * @return IChatMessage
     */
    public IChatMessage getChatMessage(String msgId) {
        ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
                mMessagingLog, msgId);
        return new ChatMessageImpl(persistentStorage);
    }

    /**
     * Handle rejoin group chat as part of send operation
     * 
     * @param chatId
     * @throws ServerApiException
     */
    public void handleRejoinGroupChatAsPartOfSendOperation(String chatId) throws ServerApiException {
        GroupChatImpl groupChat = getOrCreateGroupChat(chatId);
        groupChat.rejoinGroupChat();
    }

    /**
     * Handle rejoin group chat
     * 
     * @param chatId
     * @throws ServerApiException
     */
    public void handleRejoinGroupChat(String chatId) throws ServerApiException {
        GroupChatImpl groupChat = getOrCreateGroupChat(chatId);
        groupChat.rejoinGroupChat();
    }

    /**
     * Returns the common service configuration
     * 
     * @return the common service configuration
     */
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
        mMessagingLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode);
        mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, mimeType, msgId,
                status, reasonCode);
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
        mMessagingLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode);
        mGroupChatEventBroadcaster.broadcastMessageStatusChanged(chatId, mimeType, msgId, status,
                reasonCode);
    }
}
