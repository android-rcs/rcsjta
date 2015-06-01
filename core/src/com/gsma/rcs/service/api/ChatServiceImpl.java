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
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.ChatMessagePersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.GroupChatDeleteTask;
import com.gsma.rcs.provider.messaging.GroupChatMessageDeleteTask;
import com.gsma.rcs.provider.messaging.GroupChatPersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.GroupFileTransferDeleteTask;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.messaging.OneToOneChatMessageDeleteTask;
import com.gsma.rcs.provider.messaging.OneToOneFileTransferDeleteTask;
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
import android.text.TextUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

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

    private final ContactManager mContactManager;

    private final Core mCore;

    private final LocalContentResolver mLocalContentResolver;

    private final ExecutorService mImOperationExecutor;

    private final OneToOneUndeliveredImManager mOneToOneUndeliveredImManager;

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

    private final Object mImsLock;

    private final FileTransferServiceImpl mFileTransferService;

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param messagingLog MessagingLog
     * @param rcsSettings RcsSettings
     * @param contactManager ContactManager
     * @param core Core
     * @param localContentResolver LocalContentResolver
     * @param imOperationExecutor im operation ExecutorService
     * @param fileTransferService FileTransferServiceImpl
     * @param imsLock ims operations lock
     * @param oneToOneUndeliveredImManager OneToOneUndeliveredImManager
     */
    public ChatServiceImpl(InstantMessagingService imService, MessagingLog messagingLog,
            RcsSettings rcsSettings, ContactManager contactManager, Core core,
            LocalContentResolver localContentResolver, ExecutorService imOperationExecutor,
            Object imsLock, FileTransferServiceImpl fileTransferService,
            OneToOneUndeliveredImManager oneToOneUndeliveredImManager) {
        if (sLogger.isActivated()) {
            sLogger.info("Chat service API is loaded");
        }
        mImService = imService;
        mMessagingLog = messagingLog;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
        mCore = core;
        mLocalContentResolver = localContentResolver;
        mImOperationExecutor = imOperationExecutor;
        mImsLock = imsLock;
        mFileTransferService = fileTransferService;
        mOneToOneUndeliveredImManager = oneToOneUndeliveredImManager;
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
     * @param contact Contact ID
     * @param timestamp Timestamp sent in payload for IMDN datetime
     * @throws MsrpException
     */
    public void tryToSendOne2OneDisplayedDeliveryReport(String msgId, ContactId contact,
            long timestamp) throws MsrpException {

        OneToOneChatImpl chatImpl = mOneToOneChatCache.get(contact);
        if (chatImpl != null) {
            chatImpl.sendDisplayedDeliveryReport(contact, msgId, timestamp);
            return;
        }
        mImService.getImdnManager().sendMessageDeliveryStatus(contact, msgId,
                ImdnDocument.DELIVERY_STATUS_DISPLAYED, timestamp);
    }

    /**
     * Tries to send a displayed delivery report for a group chat
     * 
     * @param msgId Message ID
     * @param contact Contact ID
     * @param timestamp Timestamp sent in payload for IMDN datetime
     * @throws MsrpException
     */
    public void tryToSendGroupChatDisplayedDeliveryReport(final String msgId,
            final ContactId contact, final long timestamp, String chatId) throws MsrpException {
        final GroupChatSession session = mImService.getGroupChatSession(chatId);

        if (session == null || !session.isMediaEstablished()) {
            if (sLogger.isActivated()) {
                sLogger.info("No suitable session found to send the delivery status for " + msgId
                        + " : use SIP message");
            }
            mImService.getImdnManager().sendMessageDeliveryStatus(contact, msgId,
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
        String displayName = session.getRemoteDisplayName();
        if (sLogger.isActivated()) {
            sLogger.info("Chat invitation from " + contact + " (display=" + displayName + ")");
        }
        mContactManager.setContactDisplayName(contact, displayName);
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
    public void receiveOneToOneMessageDeliveryStatus(ContactId contact, ImdnDocument imdn) {
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
                // TODO: Potential race condition, the message may have been removed at this
                // point which means the database won't be updated, but we'll still do the
                // broadcast. A local lock like mLock isn't much help since mMessagingLog is
                // accessed from many other places.
                mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.FAILED, reasonCode);

                mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, mimeType,
                        msgId, Status.FAILED, reasonCode);
            }

        } else if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
            mOneToOneUndeliveredImManager.cancelDeliveryTimeoutAlarm(msgId);
            synchronized (mLock) {
                // TODO: Potential race condition, the message may have been removed at this
                // point which means the database won't be updated, but we'll still do the
                // broadcast. A local lock like mLock isn't much help since mMessagingLog is
                // accessed from many other places.
                mMessagingLog.setChatMessageStatusDelivered(msgId, timestamp);

                mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, mimeType,
                        msgId, Status.DELIVERED, ReasonCode.UNSPECIFIED);
            }

        } else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
            mOneToOneUndeliveredImManager.cancelDeliveryTimeoutAlarm(msgId);
            synchronized (mLock) {
                // TODO: Potential race condition, the message may have been removed at this
                // point which means the database won't be updated, but we'll still do the
                // broadcast. A local lock like mLock isn't much help since mMessagingLog is
                // accessed from many other places.
                mMessagingLog.setChatMessageStatusDisplayed(msgId, timestamp);

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
            oneToOneChat = new OneToOneChatImpl(contact, mOneToOneChatEventBroadcaster, mImService,
                    mMessagingLog, mRcsSettings, this, mContactManager, mCore,
                    mOneToOneUndeliveredImManager);
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
        String displayName = session.getRemoteDisplayName();
        ContactId remote = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.info("Group chat invitation from " + remote + " (display=" + displayName + ")");
        }
        if (remote != null) {
            mContactManager.setContactDisplayName(remote, displayName);
        }
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
            session.startSession();
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
            GroupChatPersistedStorageAccessor storageAccessor = new GroupChatPersistedStorageAccessor(
                    chatId, mMessagingLog, mRcsSettings);
            groupChat = new GroupChatImpl(chatId, mGroupChatEventBroadcaster, mImService,
                    storageAccessor, mRcsSettings, mContactManager, this, mMessagingLog, mCore);
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
    public void deleteOneToOneChats() {
        mImOperationExecutor.execute(new OneToOneFileTransferDeleteTask(mFileTransferService,
                mImService, mLocalContentResolver, mImsLock));
        mImOperationExecutor.execute(new OneToOneChatMessageDeleteTask(this, mImService,
                mLocalContentResolver, mImsLock));
    }

    /**
     * Deletes all group chat from history and abort/reject any associated ongoing session if such
     * exists.
     */
    public void deleteGroupChats() {
        mImOperationExecutor.execute(new GroupFileTransferDeleteTask(mFileTransferService,
                mImService, mLocalContentResolver, mImsLock));
        mImOperationExecutor.execute(new GroupChatMessageDeleteTask(this, mImService,
                mLocalContentResolver, mImsLock));
        mImOperationExecutor.execute(new GroupChatDeleteTask(this, mImService,
                mLocalContentResolver, mImsLock));
    }

    /**
     * Deletes a one to one chat with a given contact from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param contact
     */
    public void deleteOneToOneChat(ContactId contact) {
        mImOperationExecutor.execute(new OneToOneFileTransferDeleteTask(mFileTransferService,
                mImService, mLocalContentResolver, mImsLock, contact));
        mImOperationExecutor.execute(new OneToOneChatMessageDeleteTask(this, mImService,
                mLocalContentResolver, mImsLock, contact));
    }

    /**
     * Delete a group chat by its chat id from history and abort/reject any associated ongoing
     * session if such exists.
     * 
     * @param chatId
     */
    public void deleteGroupChat(String chatId) {
        mImOperationExecutor.execute(new GroupChatMessageDeleteTask(this, mImService,
                mLocalContentResolver, mImsLock, chatId));
        mImOperationExecutor.execute(new GroupFileTransferDeleteTask(mFileTransferService,
                mImService, mLocalContentResolver, mImsLock, chatId));
        mImOperationExecutor.execute(new GroupChatDeleteTask(this, mImService,
                mLocalContentResolver, mImsLock, chatId));
    }

    /**
     * Delete a message from its message id from history. Will resolve if the message is one to one
     * or from a group chat.
     * 
     * @param msgId
     */
    public void deleteMessage(String msgId) {
        if (mMessagingLog.isOneToOneChatMessage(msgId)) {
            mImOperationExecutor.execute(new OneToOneChatMessageDeleteTask(this, mImService,
                    mLocalContentResolver, mImsLock, msgId));
        } else {
            mImOperationExecutor.execute(new GroupChatMessageDeleteTask(this, mImService,
                    mLocalContentResolver, mImsLock, null, msgId));
        }
    }

    /**
     * Disables and clears any delivery expiration for a set of chat messages regardless if the
     * delivery of them has expired already or not.
     * 
     * @param msgIds
     * @throws RemoteException
     */
    public void clearMessageDeliveryExpiration(List<String> msgIds) throws RemoteException {
        if (msgIds == null || msgIds.isEmpty()) {
            throw new ServerApiIllegalArgumentException(
                    "Undelivered chat messageId list must not be null and empty!");
        }
        for (String msgId : msgIds) {
            mOneToOneUndeliveredImManager.cancelDeliveryTimeoutAlarm(msgId);
        }
        mMessagingLog.clearMessageDeliveryExpiration(msgIds);
    }

    /**
     * Adds a listener on one-to-one chat events
     * 
     * @param listener One-to-One chat event listener
     * @throws RemoteException
     */
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
    public void markMessageAsRead(String msgId) throws RemoteException {
        if (TextUtils.isEmpty(msgId)) {
            throw new ServerApiIllegalArgumentException("msgId must not be null or empty!");
        }
        try {
            mMessagingLog.markMessageAsRead(msgId);
            if (mRcsSettings.isImReportsActivated() && mRcsSettings.isRespondToDisplayReports()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("tryToDispatchAllPendingDisplayNotifications for msgID "
                            .concat(msgId));
                }

                ImdnManager imdnManager = mImService.getImdnManager();
                if (imdnManager.isSendOneToOneDeliveryDisplayedReportsEnabled()
                        || imdnManager.isSendGroupDeliveryDisplayedReportsEnabled()) {
                    mCore.getListener().tryToDispatchAllPendingDisplayNotifications();
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
    public void handleOneToOneChatSessionInitiation(OneToOneChatSession session) {
        ContactId contact = session.getRemoteContact();
        OneToOneChatImpl oneToOneChat = new OneToOneChatImpl(contact,
                mOneToOneChatEventBroadcaster, mImService, mMessagingLog, mRcsSettings, this,
                mContactManager, mCore, mOneToOneUndeliveredImManager);
        session.addListener(oneToOneChat);
        addOneToOneChat(contact, oneToOneChat);
    }

    /**
     * Returns a chat message from its unique ID
     * 
     * @param msgId
     * @return IChatMessage
     * @throws RemoteException
     */
    public IChatMessage getChatMessage(String msgId) throws RemoteException {
        if (TextUtils.isEmpty(msgId)) {
            throw new ServerApiIllegalArgumentException("msgId must not be null or empty!");
        }
        try {
            ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msgId);
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
     * Handle rejoin group chat as part of send operation
     * 
     * @param chatId
     */
    public void handleRejoinGroupChatAsPartOfSendOperation(String chatId) {
        GroupChatImpl groupChat = getOrCreateGroupChat(chatId);
        groupChat.setRejoinedAsPartOfSendOperation(true);
        groupChat.rejoinGroupChat();
    }

    /**
     * Handle rejoin group chat
     * 
     * @param chatId
     */
    public void handleRejoinGroupChat(String chatId) {
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
     * @param chatId
     * @param status
     * @param reasonCode
     */
    public void setGroupChatMessageStatusAndReasonCode(String msgId, String chatId, Status status,
            ReasonCode reasonCode) {
        mMessagingLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode);
        String mimeType = mMessagingLog.getMessageMimeType(msgId);
        mGroupChatEventBroadcaster.broadcastMessageStatusChanged(chatId, mimeType, msgId, status,
                reasonCode);
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

}
