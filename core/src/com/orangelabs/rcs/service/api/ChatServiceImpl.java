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
package com.orangelabs.rcs.service.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.os.RemoteException;

import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.ChatLog.Message.ReasonCode;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.chat.IChatService;
import com.gsma.services.rcs.chat.IChatServiceConfiguration;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.IOneToOneChat;
import com.gsma.services.rcs.chat.IOneToOneChatListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.ContributionIdGenerator;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatPersistedStorageAccessor;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ParticipantInfoUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.broadcaster.GroupChatEventBroadcaster;
import com.orangelabs.rcs.service.broadcaster.OneToOneChatEventBroadcaster;
import com.orangelabs.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Chat service implementation
 *
 * @author Jean-Marc AUFFRET
 */
public class ChatServiceImpl extends IChatService.Stub {

	private final static Executor mDisplayNotificationProcessor = Executors
			.newSingleThreadExecutor();

	private final OneToOneChatEventBroadcaster mOneToOneChatEventBroadcaster = new OneToOneChatEventBroadcaster();

	private final GroupChatEventBroadcaster mGroupChatEventBroadcaster = new GroupChatEventBroadcaster();

	private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

	private final InstantMessagingService mImService;

	private final MessagingLog mMessagingLog;

	private final RcsSettings mRcsSettings;

	private final ContactsManager mContactsManager;

	private final Core mCore;

	private final Map<ContactId, OneToOneChatImpl> mOneToOneChatCache = new HashMap<ContactId, OneToOneChatImpl>();

	private final Map<String, GroupChatImpl> mGroupChatCache = new HashMap<String, GroupChatImpl>();

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(ChatServiceImpl.class.getSimpleName());

	/**
	 * Lock used for synchronization
	 */
	private final Object lock = new Object();

	/**
	 * Constructor
	 *
	 * @param imService InstantMessagingService
	 * @param messagingLog MessagingLog
	 * @param rcsSettings RcsSettings
	 * @param contactsManager ContactsManager
	 * @param core Core
	 */
	public ChatServiceImpl(InstantMessagingService imService, MessagingLog messagingLog,
			RcsSettings rcsSettings, ContactsManager contactsManager, Core core) {
		if (logger.isActivated()) {
			logger.info("Chat service API is loaded");
		}
		mImService = imService;
		mMessagingLog = messagingLog;
		mRcsSettings = rcsSettings;
		mContactsManager = contactsManager;
		mCore = core;
	}

	private int imdnToFailedReasonCode(ImdnDocument imdn) {
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
	 * Tries to Flush pending display notifications
	 */
	public void tryToDispatchAllPendingDisplayNotifications() {
		mDisplayNotificationProcessor.execute(new DelayedDisplayNotificationDispatcher(
				AndroidFactory.getApplicationContext().getContentResolver(), this));
	}

	/**
	 * Tries to send a displayed delivery report
	 *
	 * @param msgId Message ID
	 * @param contact Contact ID
	 */
	public void tryToSendOne2OneDisplayedDeliveryReport(String msgId, ContactId contact) {
		try {
			OneToOneChatImpl chatImpl = mOneToOneChatCache.get(contact);
			if (chatImpl != null) {
				chatImpl.sendDisplayedDeliveryReport(contact, msgId);
				return;
			}
			mImService.getImdnManager().sendMessageDeliveryStatus(contact, msgId,
					ImdnDocument.DELIVERY_STATUS_DISPLAYED);
		} catch (Exception ignore) {
			/*
			 * Purposely ignoring exception since this method only makes an
			 * attempt to send report and in case of failure the report will be
			 * sent later as postponed delivery report.
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

		if (logger.isActivated()) {
			logger.info("Chat service API is closed");
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
	 * Registers a listener on service registration events
	 *
	 * @param listener Service registration listener
	 */
	public void addEventListener(IRcsServiceRegistrationListener listener) {
		if (logger.isActivated()) {
			logger.info("Add a service listener");
		}
		synchronized (lock) {
			mRcsServiceRegistrationEventBroadcaster.addEventListener(listener);
		}
	}

	/**
	 * Unregisters a listener on service registration events
	 *
	 * @param listener Service registration listener
	 */
	public void removeEventListener(IRcsServiceRegistrationListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove a service listener");
		}
		synchronized (lock) {
			mRcsServiceRegistrationEventBroadcaster.removeEventListener(listener);
		}
	}

	/**
	 * Receive registration event
	 *
	 * @param state Registration state
	 */
	public void notifyRegistrationEvent(boolean state) {
		// Notify listeners
		synchronized (lock) {
			if (state) {
				mRcsServiceRegistrationEventBroadcaster.broadcastServiceRegistered();
			} else {
				mRcsServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered();
			}
		}
	}

    /**
	 * Receive a new chat invitation
	 *
	 * @param session Chat session
	 */
    public void receiveOneOneChatInvitation(OneToOneChatSession session) {
		ContactId contact = session.getRemoteContact();
		if (logger.isActivated()) {
			logger.info("Receive chat invitation from " + contact + " (display=" + session.getRemoteDisplayName() + ")");
		}
		// Update displayName of remote contact
		mContactsManager.setContactDisplayName(contact, session.getRemoteDisplayName());

		// Add session in the list
		OneToOneChatImpl oneToOneChat = new OneToOneChatImpl(contact,
				mOneToOneChatEventBroadcaster, mImService, mMessagingLog, mRcsSettings, this);
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
                 * Only geolocation and text messages are valid parameters into
                 * this method. Thus it is certain at this point that it can
                 * only be a text message.
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
		if (logger.isActivated()) {
			logger.info("Receive message delivery status for message " + msgId + ", status "
					+ status + "notificationType=" + notificationType);
		}

		String mimeType = mMessagingLog.getMessageMimeType(msgId);
		if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
				|| ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
				|| ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
			int reasonCode = imdnToFailedReasonCode(imdn);
			synchronized (lock) {
				mMessagingLog.setChatMessageStatusAndReasonCode(msgId,
						Message.Status.Content.FAILED, reasonCode);

				mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, mimeType,
						msgId, Message.Status.Content.FAILED, reasonCode);
			}

		} else if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
			synchronized (lock) {
				mMessagingLog.setChatMessageStatusAndReasonCode(msgId,
						Message.Status.Content.DELIVERED, ReasonCode.UNSPECIFIED);

				mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, mimeType,
						msgId, Message.Status.Content.DELIVERED, ReasonCode.UNSPECIFIED);
			}

		} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
			synchronized (lock) {
				mMessagingLog.setChatMessageStatusAndReasonCode(msgId,
						Message.Status.Content.DISPLAYED, ReasonCode.UNSPECIFIED);

				mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, mimeType,
						msgId, Message.Status.Content.DISPLAYED, ReasonCode.UNSPECIFIED);
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
		if (logger.isActivated()) {
			logger.debug("Add oneToOne chat to list (size=" + mOneToOneChatCache.size() + ") for " + contact);
		}
	}

	/**
	 * Remove a oneToOne chat from the list
	 *
	 * @param contact Contact ID
	 */
	/* package private */ void removeOneToOneChat(ContactId contact) {
		mOneToOneChatCache.remove(contact);
		if (logger.isActivated()) {
			logger.debug("Remove oneToOne chat from list (size=" + mOneToOneChatCache.size() + ") for "
					+ contact);
		}
	}

    /**
     * Returns a chat from its unique ID
     *
     * @param contact Contact ID
     * @return IOneToOneChat
     * @throws ServerApiException
     */
	public IOneToOneChat getOneToOneChat(ContactId contact) throws ServerApiException {
		IOneToOneChat oneToOneChat = mOneToOneChatCache.get(contact);
		if (oneToOneChat != null) {
			return oneToOneChat;
		}
		return new OneToOneChatImpl(contact, mOneToOneChatEventBroadcaster,
				mImService, mMessagingLog, mRcsSettings, this);
	}

    /**
	 * Receive a new group chat invitation
	 *
	 * @param session Chat session
	 */
    public void receiveGroupChatInvitation(GroupChatSession session) {
		if (logger.isActivated()) {
			logger.info("Receive group chat invitation from " + session.getRemoteContact() + " (display="
					+ session.getRemoteDisplayName()+")");
		}

		// Update displayName of remote contact
		mContactsManager.setContactDisplayName(session.getRemoteContact(), session.getRemoteDisplayName());
		String chatId = session.getContributionID();
		GroupChatPersistedStorageAccessor storageAccessor = new GroupChatPersistedStorageAccessor(
				chatId, mMessagingLog);
		GroupChatImpl groupChat = new GroupChatImpl(chatId, mGroupChatEventBroadcaster,
				mImService, storageAccessor, mRcsSettings, mContactsManager, this, mMessagingLog);
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
		if (logger.isActivated()) {
			logger.debug("Add Group Chat to list (size=" + mGroupChatCache.size() + ") for chatId "
					+ chatId);
		}
	}

	/**
	 * Remove a group chat from the list
	 *
	 * @param chatId Chat ID
	 */
	/* package private */ void removeGroupChat(String chatId) {
		mGroupChatCache.remove(chatId);
		if (logger.isActivated()) {
			logger.debug("Remove Group Chat to list (size=" + mGroupChatCache.size() + ") for chatId "
					+ chatId);
		}
	}

	/**
	 * Initiates a group chat with a group of contact and returns a GroupChat instance. The subject is optional and may be null.
	 *
	 * @param contacts
	 *            List of contact IDs
	 * @param subject
	 *            Subject
	 * @return instance of IGroupChat
	 * @throws ServerApiException
	 *             Note: List is used instead of Set because AIDL does only support List
	 */
    public IGroupChat initiateGroupChat(List<ContactId> contacts, String subject) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate an ad-hoc group chat session");
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			final ChatSession session = mImService.initiateAdhocGroupChatSession(contacts, subject);

			String chatId = session.getContributionID();
			GroupChatPersistedStorageAccessor storageAccessor = new GroupChatPersistedStorageAccessor(
					chatId, subject, Direction.OUTGOING, mMessagingLog);
			GroupChatImpl groupChat = new GroupChatImpl(chatId, mGroupChatEventBroadcaster,
					mImService, storageAccessor, mRcsSettings, mContactsManager, this, mMessagingLog);
			session.addListener(groupChat);

			mMessagingLog.addGroupChat(session.getContributionID(), session.getRemoteContact(),
					session.getSubject(), session.getParticipants(), GroupChat.State.INITIATING,
					GroupChat.ReasonCode.UNSPECIFIED, Direction.OUTGOING);

			addGroupChat(groupChat);
	        new Thread() {
	    		public void run() {
					session.startSession();
	    		}
	    	}.start();
	    	return groupChat;

		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Core exception", e);
			}

			Set<ParticipantInfo> participants = ParticipantInfoUtils
					.getParticipantInfos(contacts);

			String callId = mCore.getImsModule().getSipManager().getSipStack().generateCallId();
			mMessagingLog.addGroupChat(
					ContributionIdGenerator.getContributionId(callId), null, subject, participants,
					GroupChat.State.REJECTED, GroupChat.ReasonCode.REJECTED_MAX_CHATS,
					Direction.OUTGOING);
			throw new ServerApiException(e.getMessage());
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }

	private GroupChatImpl getOrCreateGroupChat(String chatId) {
		GroupChatImpl groupChat = mGroupChatCache.get(chatId);
		if (groupChat != null) {
			return groupChat;
		}
		GroupChatPersistedStorageAccessor storageAccessor = new GroupChatPersistedStorageAccessor(
				chatId, mMessagingLog);
		return new GroupChatImpl(chatId, mGroupChatEventBroadcaster, mImService, storageAccessor,
				mRcsSettings, mContactsManager, this, mMessagingLog);
	}

	/**
	 * Returns a group chat from its unique ID. An exception is thrown if the
	 * chat ID does not exist
	 *
	 * @param chatId Chat ID
	 * @return IGroupChat
	 * @throws ServerApiException
	 */
	public IGroupChat getGroupChat(String chatId) throws ServerApiException {
		return getOrCreateGroupChat(chatId);
	}

	/**
	 * Adds a listener on one-to-one chat events
	 *
	 * @param listener One-to-One chat event listener
	 * @throws RemoteException 
	 */
	public void addEventListener2(IOneToOneChatListener listener) throws RemoteException {
		if (logger.isActivated()) {
			logger.info("Add an OneToOne chat event listener");
		}
		synchronized (lock) {
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
		if (logger.isActivated()) {
			logger.info("Remove an OneToOne chat event listener");
		}
		synchronized (lock) {
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
		if (logger.isActivated()) {
			logger.info("Add a Group chat event listener");
		}
		synchronized (lock) {
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
		if (logger.isActivated()) {
			logger.info("Remove a group chat event listener");
		}
		synchronized (lock) {
			mGroupChatEventBroadcaster.removeGroupChatEventListener(listener);
		}
	}

    /**
     * Returns the configuration of the chat service
     *
     * @return Configuration
     */
    public IChatServiceConfiguration getConfiguration() {
    	return new ChatServiceConfigurationImpl();
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
			if (logger.isActivated()) {
				logger.debug("tryToDispatchAllPendingDisplayNotifications for msgID ".concat(msgId));
			}
			tryToDispatchAllPendingDisplayNotifications();
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
	 * Set the parameter in order to respond or not to display reports when requested by the remote part.
	 *
	 * @param enable true if respond to display reports
	 * @throws ServerApiException
	 */
	@Override
	public void setRespondToDisplayReports(boolean enable) throws RemoteException {
		mRcsSettings.setRespondToDisplayReports(enable);
	}

	/**
	 * Add and broadcast group chat invitation rejections.
	 *
	 * @param chatId Chat ID
	 * @param contact Contact ID
	 * @param subject Subject
	 * @param participants Participants
	 * @param reasonCode Reason code
	 */
	public void addAndBroadcastGroupChatInvitationRejected(String chatId, ContactId contact,
			String subject, Set<ParticipantInfo> participants, int reasonCode) {

		mMessagingLog.addGroupChat(chatId, contact, subject, participants,
				GroupChat.State.REJECTED, reasonCode, Direction.INCOMING);

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
				mOneToOneChatEventBroadcaster, mImService, mMessagingLog, mRcsSettings, this);
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
		addGroupChat(groupChat);
	}

	/**
	 * Handle auto rejoin group chat
	 *
	 * @param chatId
	 * @throws ServerApiException
	 */
	public void handleAutoRejoinGroupChat(String chatId) throws ServerApiException {
		GroupChatImpl groupChat = getOrCreateGroupChat(chatId);
		groupChat.rejoinGroupChat();
		addGroupChat(groupChat);
	}
	
	/**
	 * Returns the common service configuration
	 * 
	 * @return the common service configuration
	 */
	public ICommonServiceConfiguration getCommonConfiguration() {
		return new CommonServiceConfigurationImpl();
	}
	
}
