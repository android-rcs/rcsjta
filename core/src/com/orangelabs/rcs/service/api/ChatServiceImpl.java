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

import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.os.RemoteException;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.ReasonCode;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.IChatService;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.IOneToOneChat;
import com.gsma.services.rcs.chat.IOneToOneChatListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ContributionIdGenerator;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.OneOneChatSession;
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
	/**
	 * List of one to one chat sessions
	 */
	// TODO : This change is only temporary. Will be changed with the implementation of CR018
	private static Hashtable<ContactId, OneToOneChatImpl> one2oneChatSessions = new Hashtable<ContactId, OneToOneChatImpl>();

	/**
	 * List of group chat sessions
	 */
	private static Hashtable<String, IGroupChat> groupChatSessions = new Hashtable<String, IGroupChat>();  

	private final static Executor mDisplayNotificationProcessor = Executors
			.newSingleThreadExecutor();

	private final OneToOneChatEventBroadcaster mOneToOneChatEventBroadcaster  = new OneToOneChatEventBroadcaster();

	private final GroupChatEventBroadcaster mGroupChatEventBroadcaster = new GroupChatEventBroadcaster();

	private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

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
	 */
	public ChatServiceImpl() {
		if (logger.isActivated()) {
			logger.info("Chat service API is loaded");
		}
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
			OneToOneChatImpl chatImpl = one2oneChatSessions.get(contact);
			if (chatImpl != null) {
				chatImpl.sendDisplayedDeliveryReport(contact, msgId);
				return;
			}
			Core.getInstance()
					.getImService()
					.getImdnManager()
					.sendMessageDeliveryStatus(contact,
							msgId, ImdnDocument.DELIVERY_STATUS_DISPLAYED);
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
		one2oneChatSessions.clear();
		groupChatSessions.clear();
		
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
    public void receiveOneOneChatInvitation(OneOneChatSession session) {
		ContactId contact = session.getRemoteContact();
		if (logger.isActivated()) {
			logger.info("Receive chat invitation from " + contact + " (display=" + session.getRemoteDisplayName() + ")");
		}
		// Update displayName of remote contact
		ContactsManager.getInstance().setContactDisplayName(contact, session.getRemoteDisplayName());
		 
		// Add session in the list
		OneToOneChatImpl chatApi = new OneToOneChatImpl(contact, session, mOneToOneChatEventBroadcaster);
		ChatServiceImpl.addChatSession(contact, chatApi);

		InstantMessage firstMessage = session.getFirstMessage();
		if (firstMessage != null) {
			mOneToOneChatEventBroadcaster.broadcastMessageReceived(firstMessage.getMessageId());
		}
    }
    
    /**
     * Open a single chat with a given contact and returns a Chat instance.
     * The parameter contact supports the following formats: MSISDN in national
     * or international format, SIP address, SIP-URI or Tel-URI.
     * 
     * @param contact Contact
     * @return One-to-One Chat
	 * @throws ServerApiException
     */
    public IOneToOneChat openSingleChat(ContactId contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Open a 1-1 chat session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();
		
		try {
			// Check if there is an existing chat or not
			OneToOneChatImpl sessionApi = (OneToOneChatImpl)ChatServiceImpl.getChatSession(contact);
			if (sessionApi != null) {
				if (logger.isActivated()) {
					logger.debug("Chat session already exists for " + contact);
				}

				// Check core session state
				final OneOneChatSession coreSession = sessionApi.getCoreSession();
				if (coreSession != null) {
					if (logger.isActivated()) {
						logger.debug("Core chat session already exists: " + coreSession.getSessionID());
					}

					if (coreSession.getDialogPath().isSessionTerminated() ||
							coreSession.getDialogPath().isSessionCancelled()) {
						if (logger.isActivated()) {
							logger.debug("Core chat session is terminated: reset it");
						}
						
						// Session has expired, remove it
						sessionApi.resetCoreSession();
					} else
					if (!coreSession.getDialogPath().isSessionEstablished()) {
						if (logger.isActivated()) {
							logger.debug("Core chat session is pending: auto accept it");
						}
						
						// Auto accept the pending session
				        Thread t = new Thread() {
				    		public void run() {
								coreSession.acceptSession();
				    		}
				    	};
				    	t.start();
					} else {
						if (logger.isActivated()) {
							logger.debug("Core chat session is already established");
						}
					}
				}
			} else {
				if (logger.isActivated()) {
					logger.debug("Create a new chat session with " + contact);
				}

				// Add session listener
				sessionApi = new OneToOneChatImpl(contact, mOneToOneChatEventBroadcaster);

				// Add session in the list
				ChatServiceImpl.addChatSession(contact, sessionApi);
			}
		
			return sessionApi;
		
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
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

		if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
				|| ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
				|| ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
			int reasonCode = imdnToFailedReasonCode(imdn);
			synchronized (lock) {
				MessagingLog.getInstance().updateChatMessageStatusAndReasonCode(msgId,
						Message.Status.Content.FAILED, reasonCode);

				mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, msgId,
						Message.Status.Content.FAILED, reasonCode);
			}

		} else if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
			synchronized (lock) {
				MessagingLog.getInstance().updateChatMessageStatusAndReasonCode(msgId,
						Message.Status.Content.DELIVERED, ReasonCode.UNSPECIFIED);

				mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, msgId,
						Message.Status.Content.DELIVERED, ReasonCode.UNSPECIFIED);
			}

		} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
			synchronized (lock) {
				MessagingLog.getInstance().updateChatMessageStatusAndReasonCode(msgId,
						Message.Status.Content.DISPLAYED, ReasonCode.UNSPECIFIED);

				mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, msgId,
						Message.Status.Content.DISPLAYED, ReasonCode.UNSPECIFIED);
			}
		}
	}
    
	/**
	 * Add a chat session in the list
	 * 
	 * @param contact Contact ID
	 * @param session Chat session
	 */
	public static void addChatSession(ContactId contact, OneToOneChatImpl session) {
		int sizeBefore = one2oneChatSessions.size();
		one2oneChatSessions.put(contact, session);
		int sizeAfter = one2oneChatSessions.size();
		if (logger.isActivated()) {
			logger.debug("Add " + (sizeAfter - sizeBefore) + " chat session to list (size=" + sizeAfter + ") for " + contact);
		}
	}

	/**
	 * Get a chat session from the list for a given contact
	 * 
	 * @param contact Contact ID
	 * @param GroupChat session
	 */
	private static IOneToOneChat getChatSession(ContactId contact) {
		if (logger.isActivated()) {
			logger.debug("Get a chat session for " + contact);
		}
		
		return one2oneChatSessions.get(contact);
	}

	/**
	 * Remove a chat session from the list
	 * 
	 * @param contact Contact ID
	 */
	/* package private */ static void removeChatSession(ContactId contact) {
		int sizeBefore = one2oneChatSessions.size();
		one2oneChatSessions.remove(contact);
		int sizeAfter = one2oneChatSessions.size();
		if (logger.isActivated()) {
			logger.debug("Remove " + (sizeBefore - sizeAfter) + " chat session from list (size=" + sizeAfter + ") for " + contact);
		}
	}

    
    /**
     * Returns a chat in progress from its unique ID
     * 
     * @param contact Contact ID
     * @return One-to-One Chat or null if not found
     * @throws ServerApiException
     */
    public IOneToOneChat getChat(ContactId contact) throws ServerApiException {
		// Return a session instance
		return one2oneChatSessions.get(contact);
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
		ContactsManager.getInstance().setContactDisplayName(session.getRemoteContact(), session.getRemoteDisplayName());
		 
		// Add session in the list
		GroupChatImpl sessionApi = new GroupChatImpl(session, mGroupChatEventBroadcaster);
		ChatServiceImpl.addGroupChatSession(sessionApi);
    }
	
	/**
	 * Add a group chat session in the list
	 * 
	 * @param session Chat session
	 */
	private static void addGroupChatSession(GroupChatImpl session) {
		int sizeBefore = groupChatSessions.size();
		groupChatSessions.put(session.getChatId(), session);
		int sizeAfter = groupChatSessions.size();
		if (logger.isActivated()) {
			logger.debug("Add " + (sizeAfter - sizeBefore) + " GC session to list (size=" + sizeAfter + ") for chatId "+session.getChatId() );
		}
	}

	/**
	 * Remove a group chat session from the list
	 * 
	 * @param chatId Chat ID
	 */
	/* package private */ static void removeGroupChatSession(String chatId) {
		int sizeBefore = groupChatSessions.size();
		groupChatSessions.remove(chatId);
		int sizeAfter = groupChatSessions.size();
		if (logger.isActivated()) {
			logger.debug("Remove " + (sizeBefore - sizeAfter) + " GC session to list (size=" + sizeAfter + ") for chatId "+chatId );
		}
	}

	/**
	 * Initiates a group chat with a group of contact and returns a GroupChat instance. The subject is optional and may be null.
	 * 
	 * @param contact
	 *            List of contact IDs
	 * @param subject
	 *            Subject
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
			// Initiate the session
			final ChatSession session = Core.getInstance().getImService().initiateAdhocGroupChatSession(contacts, subject);

			// Add session listener
			GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session, mGroupChatEventBroadcaster);

			MessagingLog.getInstance()
					.addGroupChat(session.getContributionID(), session.getRemoteContact(),
							session.getSubject(), session.getParticipants(),
							GroupChat.State.INITIATED, GroupChat.ReasonCode.UNSPECIFIED,
							Direction.OUTGOING);

			ChatServiceImpl.addGroupChatSession(sessionApi);

			// Start the session
	        new Thread() {
	    		public void run() {
					session.startSession();
	    		}
	    	}.start();
			return sessionApi;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Core exception", e);
			}

			Set<ParticipantInfo> participants = ParticipantInfoUtils
					.getParticipantInfos(contacts);

			String callId = Core.getInstance().getImsModule().getSipManager().getSipStack().generateCallId();
			MessagingLog.getInstance().addGroupChat(
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
    
    /**
     * Rejoins an existing group chat from its unique chat ID
     * 
     * @param chatId Chat ID
     * @return Group chat
     * @throws ServerApiException
     */
    public IGroupChat rejoinGroupChat(String chatId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Rejoin group chat session related to the conversation " + chatId);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			final ChatSession session = Core.getInstance().getImService().rejoinGroupChatSession(chatId);

			// Add session in the list
			GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session, mGroupChatEventBroadcaster);

			ChatServiceImpl.addGroupChatSession(sessionApi);

			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	};
	    	t.start();
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }
    
    /**
     * Restarts a previous group chat from its unique chat ID
     * 
     * @param chatId Chat ID
     * @return Group chat
     * @throws ServerApiException
     */
    public IGroupChat restartGroupChat(String chatId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Restart group chat session related to the conversation " + chatId);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			final ChatSession session = Core.getInstance().getImService().restartGroupChatSession(chatId);

			GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session, mGroupChatEventBroadcaster);
			ChatServiceImpl.addGroupChatSession(sessionApi);

			// Start the session
	       new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	}.start();
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }

    /**
     * Returns a group chat in progress from its unique ID
     * 
     * @param chatId Chat ID
     * @return Group chat or null if not found
     * @throws ServerApiException
     */
    public IGroupChat getGroupChat(String chatId) throws ServerApiException {
		// Return a session instance
		return groupChatSessions.get(chatId);
	}

	/**
	 * Adds a listener on one-to-one chat events
	 *
	 * @param listener One-to-One chat event listener
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
    public ChatServiceConfiguration getConfiguration() {
    	RcsSettings settings = RcsSettings.getInstance();
    	return new ChatServiceConfiguration(
    			settings.isImAlwaysOn(),
    			settings.isStoreForwardWarningActivated(),
    			settings.getChatIdleDuration(),
    			settings.getIsComposingTimeout(),
    			settings.getMaxChatParticipants(),
    			settings.getMinGroupChatParticipants(),
    			settings.getMaxChatMessageLength(),
    			settings.getMaxGroupChatMessageLength(),
    			settings.getGroupChatSubjectMaxLength(),
    			settings.isSmsFallbackServiceActivated(),
    			settings.isRespondToDisplayReports(),
    			settings.getMaxGeolocLabelLength(),
    			settings.getGeolocExpirationTime());
	}    

	/**
	 * Mark a received message as read (ie. displayed in the UI)
	 * 
	 * @param msgId Message ID
	 * @throws ServerApiException
	 */
	@Override
	public void markMessageAsRead(String msgId) throws ServerApiException {
		MessagingLog.getInstance().markMessageAsRead(msgId);
		if (RcsSettings.getInstance().isImReportsActivated() && RcsSettings.getInstance().isRespondToDisplayReports()) {
			if (logger.isActivated()) {
				logger.debug("tryToDispatchAllPendingDisplayNotifications for msgID "+msgId);
			}
			tryToDispatchAllPendingDisplayNotifications();
		}
	}

	/**
	 * Returns service version
	 * 
	 * @return Version
	 * @see RcsService.Build.VERSION_CODES
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
		RcsSettings.getInstance().setRespondToDisplayReports(enable);
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

		MessagingLog.getInstance().addGroupChat(chatId, contact, subject, participants,
				GroupChat.State.REJECTED, reasonCode, Direction.INCOMING);

		mGroupChatEventBroadcaster.broadcastInvitation(chatId);
	}
}
