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

import static com.gsma.services.rcs.chat.ChatLog.Message.Status.Content.DELIVERED;
import static com.gsma.services.rcs.chat.ChatLog.Message.Status.Content.DISPLAYED;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.chat.ChatIntent;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.chat.IChat;
import com.gsma.services.rcs.chat.IChatListener;
import com.gsma.services.rcs.chat.IChatService;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.OneOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.broadcaster.GroupChatEventBroadcaster;
import com.orangelabs.rcs.service.broadcaster.JoynServiceRegistrationEventBroadcaster;
import com.orangelabs.rcs.service.broadcaster.OneToOneChatEventBroadcaster;
import com.orangelabs.rcs.utils.IntentUtils;
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
	private static Hashtable<ContactId, ChatImpl> one2oneChatSessions = new Hashtable<ContactId, ChatImpl>();

	/**
	 * List of group chat sessions
	 */
	private static Hashtable<String, IGroupChat> groupChatSessions = new Hashtable<String, IGroupChat>();  

	private final static Executor mDisplayNotificationProcessor = Executors
			.newSingleThreadExecutor();

	private final OneToOneChatEventBroadcaster mOneToOneChatEventBroadcaster  = new OneToOneChatEventBroadcaster();

	private final GroupChatEventBroadcaster mGroupChatEventBroadcaster = new GroupChatEventBroadcaster();

	private final JoynServiceRegistrationEventBroadcaster mJoynServiceRegistrationEventBroadcaster = new JoynServiceRegistrationEventBroadcaster();

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
			ChatImpl chatImpl = one2oneChatSessions.get(contact);
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
	public void addServiceRegistrationListener(IJoynServiceRegistrationListener listener) {
		if (logger.isActivated()) {
			logger.info("Add a service listener");
		}
		synchronized (lock) {
			mJoynServiceRegistrationEventBroadcaster.addServiceRegistrationListener(listener);
		}
	}

	/**
	 * Unregisters a listener on service registration events
	 *
	 * @param listener Service registration listener
	 */
	public void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove a service listener");
		}
		synchronized (lock) {
			mJoynServiceRegistrationEventBroadcaster.removeServiceRegistrationListener(listener);
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
				mJoynServiceRegistrationEventBroadcaster.broadcastServiceRegistered();
			} else {
				mJoynServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered();
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
		ChatImpl sessionApi = new ChatImpl(contact, session, mOneToOneChatEventBroadcaster);
		ChatServiceImpl.addChatSession(contact, sessionApi);

		// Broadcast intent related to the received invitation
		Intent newOneToOneChatMessage = new Intent(ChatIntent.ACTION_NEW_ONE2ONE_CHAT_MESSAGE);
		IntentUtils.tryToSetExcludeStoppedPackagesFlag(newOneToOneChatMessage);
		IntentUtils.tryToSetReceiverForegroundFlag(newOneToOneChatMessage);
		newOneToOneChatMessage.putExtra(ChatIntent.EXTRA_MESSAGE_ID, session.getFirstMessage()
				.getMessageId());
		AndroidFactory.getApplicationContext().sendBroadcast(newOneToOneChatMessage);
    }
    
    /**
     * Open a single chat with a given contact and returns a Chat instance.
     * The parameter contact supports the following formats: MSISDN in national
     * or international format, SIP address, SIP-URI or Tel-URI.
     * 
     * @param contact Contact
     * @return Chat
	 * @throws ServerApiException
     */
    public IChat openSingleChat(ContactId contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Open a 1-1 chat session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();
		
		try {
			// Check if there is an existing chat or not
			ChatImpl sessionApi = (ChatImpl)ChatServiceImpl.getChatSession(contact);
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
				sessionApi = new ChatImpl(contact, mOneToOneChatEventBroadcaster);

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
	 * @param msgId Message ID
     * @param status Delivery status
     */
	public void receiveMessageDeliveryStatus(ContactId contact, String msgId, String status) {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("Receive message delivery status for message " + msgId + ", status "
						+ status);
			}
			MessagingLog.getInstance().updateOutgoingChatMessageDeliveryStatus(msgId, status);
			// Notify message delivery listeners
			if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
				mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, msgId,
						DELIVERED);
			} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
				mOneToOneChatEventBroadcaster.broadcastMessageStatusChanged(contact, msgId,
						DISPLAYED);
			}
		}
	}
    
	/**
	 * Add a chat session in the list
	 * 
	 * @param contact Contact ID
	 * @param session Chat session
	 */
	public static void addChatSession(ContactId contact, ChatImpl session) {
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
	private static IChat getChatSession(ContactId contact) {
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
     * Returns the list of single chats in progress
     * 
     * @return List of chats
     * @throws ServerApiException
     */
    public List<IBinder> getChats() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get chat sessions");
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(one2oneChatSessions.size());
			for (Enumeration<ChatImpl> e = one2oneChatSessions.elements() ; e.hasMoreElements() ;) {
				IChat sessionApi = e.nextElement() ;
				result.add(sessionApi.asBinder());
			}
			return result;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }
    
    /**
     * Returns a chat in progress from its unique ID
     * 
     * @param contact Contact ID
     * @return Chat or null if not found
     * @throws ServerApiException
     */
    public IChat getChat(ContactId contact) throws ServerApiException {
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

		// Update rich messaging history
		MessagingLog.getInstance().addGroupChat(session.getContributionID(),
				session.getSubject(), session.getParticipants(), GroupChat.State.INVITED, GroupChat.Direction.INCOMING);
		
		// Update displayName of remote contact
		ContactsManager.getInstance().setContactDisplayName(session.getRemoteContact(), session.getRemoteDisplayName());
		 
		// Add session in the list
		GroupChatImpl sessionApi = new GroupChatImpl(session, mGroupChatEventBroadcaster);
		ChatServiceImpl.addGroupChatSession(sessionApi);

		// Broadcast intent related to the received invitation
		Intent newInvitation = new Intent(GroupChatIntent.ACTION_NEW_INVITATION);
		IntentUtils.tryToSetExcludeStoppedPackagesFlag(newInvitation);
		IntentUtils.tryToSetReceiverForegroundFlag(newInvitation);
		newInvitation.putExtra(GroupChatIntent.EXTRA_CHAT_ID, sessionApi.getChatId());
		AndroidFactory.getApplicationContext().sendBroadcast(newInvitation);
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

			// Update rich messaging history
			MessagingLog.getInstance().addGroupChat(session.getContributionID(),
					session.getSubject(), session.getParticipants(),
					GroupChat.State.INITIATED, GroupChat.Direction.OUTGOING);

			// Start the session
	        new Thread() {
	    		public void run() {
					session.startSession();
	    		}
	    	}.start();
						
			// Add session in the list
			ChatServiceImpl.addGroupChatSession(sessionApi);
			return sessionApi;
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
			
			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	};
	    	t.start();

			// Add session in the list
			GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session, mGroupChatEventBroadcaster);
			ChatServiceImpl.addGroupChatSession(sessionApi);
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

			// Start the session
	       new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	}.start();
			
			// Add session in the list
			GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session, mGroupChatEventBroadcaster);
			ChatServiceImpl.addGroupChatSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }
    
    /**
     * Returns the list of group chats in progress
     * 
     * @return List of group chat
     * @throws ServerApiException
     */
    public List<IBinder> getGroupChats() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get group chat sessions");
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(groupChatSessions.size());
			for (Enumeration<IGroupChat> e = groupChatSessions.elements() ; e.hasMoreElements() ;) {
				IGroupChat sessionApi = (IGroupChat)e.nextElement() ;
				result.add(sessionApi.asBinder());
			}
			return result;
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
	 * Adds an event listener for OneToOne chat events
	 *
	 * @param listener Chat event listener
	 */
	public void addOneToOneChatEventListener(IChatListener listener) throws RemoteException {
		if (logger.isActivated()) {
			logger.info("Add an OneToOne chat event listener");
		}
		synchronized (lock) {
			mOneToOneChatEventBroadcaster.addOneToOneChatEventListener(listener);
		}
	}

	/**
	 * Removes an event listener for OneToOne chat events
	 *
	 * @param listener Chat event listener
	 */
	public void removeOneToOneChatEventListener(IChatListener listener) throws RemoteException {
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
	 * @param listener Chat invitation listener
	 * @throws ServerApiException
	 */
	public void addGroupChatEventListener(IGroupChatListener listener) throws ServerApiException {
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
	 * @param listener Chat invitation listener
	 * @throws ServerApiException
	 */
	public void removeGroupChatEventListener(IGroupChatListener listener) throws ServerApiException {
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
	 * @see JoynService.Build.VERSION_CODES
	 * @throws ServerApiException
	 */
	public int getServiceVersion() throws ServerApiException {
		return JoynService.Build.API_VERSION;
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
}
