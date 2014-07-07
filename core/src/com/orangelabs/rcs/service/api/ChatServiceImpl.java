/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/
package com.orangelabs.rcs.service.api;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.chat.ChatIntent;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.chat.IChat;
import com.gsma.services.rcs.chat.IChatListener;
import com.gsma.services.rcs.chat.IChatService;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.INewChatListener;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.OneOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Chat service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatServiceImpl extends IChatService.Stub {
	/**
	 * List of service event listeners
	 */
	private RemoteCallbackList<IJoynServiceRegistrationListener> serviceListeners = new RemoteCallbackList<IJoynServiceRegistrationListener>();

	/**
	 * List of chat sessions
	 */
	// TODO : This change is only temporary. Will be changed with the implementation of CR018
	private static Hashtable<String, ChatImpl> chatSessions = new Hashtable<String, ChatImpl>();

	/**
	 * List of group chat sessions
	 */
	private static Hashtable<String, IGroupChat> groupChatSessions = new Hashtable<String, IGroupChat>();  

	private final static Executor mDisplayNotificationProcessor = Executors
			.newSingleThreadExecutor();

	/**
	 * List of file chat invitation listeners
	 */
	private RemoteCallbackList<INewChatListener> listeners = new RemoteCallbackList<INewChatListener>();

	/**
	 * The logger
	 */
	private static Logger logger = Logger.getLogger(ChatServiceImpl.class.getName());

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

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
	 * @param contactId Contact ID
	 */
	public void tryToSendOne2OneDisplayedDeliveryReport(String msgId, ContactId contactId) {
		try {
			ChatImpl chatImpl = chatSessions.get(contactId);
			if (chatImpl != null) {
				chatImpl.sendDisplayedDeliveryReport(contactId, msgId);
				return;
			}
			Core.getInstance()
					.getImService()
					.getImdnManager()
//					.sendMessageDeliveryStatus(PhoneUtils.formatNumberToSipUri(contactNumber),
					.sendMessageDeliveryStatus(contactId,
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
		chatSessions.clear();
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
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Add a service listener");
			}

			serviceListeners.register(listener);
		}
	}
	
	/**
	 * Unregisters a listener on service registration events
	 * 
	 * @param listener Service registration listener
	 */
	public void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Remove a service listener");
			}
			
			serviceListeners.unregister(listener);
    	}	
	}    
    
    /**
     * Receive registration event
     * 
     * @param state Registration state
     */
    public void notifyRegistrationEvent(boolean state) {
    	// Notify listeners
    	synchronized(lock) {
			final int N = serviceListeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	if (state) {
	            		serviceListeners.getBroadcastItem(i).onServiceRegistered();
	            	} else {
	            		serviceListeners.getBroadcastItem(i).onServiceUnregistered();
	            	}
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        serviceListeners.finishBroadcast();
	    }    	    	
    }
    
    /**
	 * Receive a new chat invitation
	 * 
	 * @param session Chat session
	 */
    public void receiveOneOneChatInvitation(OneOneChatSession session) {
		if (logger.isActivated()) {
			logger.info("Receive chat invitation from " + session.getRemoteContact());
		}

		// Update rich messaging history
		// Nothing done in database

		// Add session in the list
		ChatImpl sessionApi = new ChatImpl(session.getRemoteContact(), session);
		ChatServiceImpl.addChatSession(session.getRemoteContact(), sessionApi);

		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(ChatIntent.ACTION_NEW_CHAT);
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
    	intent.putExtra(ChatIntent.EXTRA_CONTACT, session.getRemoteContact().toString());
    	intent.putExtra(ChatIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
    	InstantMessage msg = session.getFirstMessage();
    	ChatMessage msgApi;
    	if (msg instanceof GeolocMessage) {
    		GeolocMessage geoloc = (GeolocMessage)msg;
        	Geoloc geolocApi = new Geoloc(geoloc.getGeoloc().getLabel(),
        			geoloc.getGeoloc().getLatitude(), geoloc.getGeoloc().getLongitude(),
        			geoloc.getGeoloc().getExpiration());
        	msgApi = new com.gsma.services.rcs.chat.GeolocMessage(geoloc.getMessageId(),
        			geoloc.getRemote(),
        			geolocApi, geoloc.getDate());
	    	intent.putExtra(ChatIntent.EXTRA_MESSAGE, msgApi);
    	} else {
        	msgApi = new ChatMessage(msg.getMessageId(),
        			msg.getRemote(),
        			msg.getTextMessage(), msg.getServerDate());
        	intent.putExtra(ChatIntent.EXTRA_MESSAGE, msgApi);    		
    	}
    	AndroidFactory.getApplicationContext().sendBroadcast(intent);
    	
    	// Notify chat invitation listeners
    	synchronized(lock) {
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onNewSingleChat(session.getRemoteContact(), msgApi);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }    	    	
    }
    
    /**
     * Open a single chat with a given contact and returns a Chat instance.
     * The parameter contact supports the following formats: MSISDN in national
     * or international format, SIP address, SIP-URI or Tel-URI.
     * 
     * @param contact Contact
     * @param listener Chat event listener
     * @return Chat
	 * @throws ServerApiException
     */
    public IChat openSingleChat(ContactId contactId, IChatListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Open a 1-1 chat session with " + contactId);
		}

		// Test IMS connection
		ServerApiUtils.testIms();
		
		try {
			// Check if there is an existing chat or not
			ChatImpl sessionApi = (ChatImpl)ChatServiceImpl.getChatSession(contactId);
			if (sessionApi != null) {
				if (logger.isActivated()) {
					logger.debug("Chat session already exists for " + contactId);
				}
				
				// Add session listener
				sessionApi.addEventListener(listener);

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
					logger.debug("Create a new chat session with " + contactId);
				}

				// Add session listener
				sessionApi = new ChatImpl(contactId);
				sessionApi.addEventListener(listener);
	
				// Add session in the list
				ChatServiceImpl.addChatSession(contactId, sessionApi);
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
	 * @param contactId Contact ID
	 * @param msgId Message ID
     * @param status Delivery status
     */
    public void receiveMessageDeliveryStatus(ContactId contactId, String msgId, String status) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Receive message delivery status for message " + msgId + ", status " + status);
			}
	
	  		// Notify message delivery listeners
			ChatImpl chat = (ChatImpl)ChatServiceImpl.getChatSession(contactId);
			if (chat != null) {
				// TODO FUSION check if correct ?
            	chat.handleMessageDeliveryStatus(msgId, status, contactId);
			} else {
				// Update rich messaging history
				MessagingLog.getInstance().updateOutgoingChatMessageDeliveryStatus(msgId,
						status);

				// TODO : Callbacks for delivery notifications received outside
				// session will be implemented as part of CR011.
			}
    	}
    }
    
	/**
	 * Add a chat session in the list
	 * 
	 * @param contactId Contact ID
	 * @param session Chat session
	 */
	public static void addChatSession(ContactId contactId, ChatImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add a chat session in the list (size=" + chatSessions.size() + ") for " + contactId);
		}
		
		chatSessions.put(contactId.toString(), session);
	}

	/**
	 * Get a chat session from the list for a given contact
	 * 
	 * @param contactId Contact ID
	 * @param GroupChat session
	 */
	protected static IChat getChatSession(ContactId contactId) {
		if (logger.isActivated()) {
			logger.debug("Get a chat session for " + contactId);
		}
		
		return chatSessions.get(contactId.toString());
	}

	/**
	 * Remove a chat session from the list
	 * 
	 * @param contactId Contact ID
	 */
	protected static void removeChatSession(String contactId) {
		if (logger.isActivated()) {
			logger.debug("Remove a chat session from the list (size=" + chatSessions.size() + ") for " + contactId);
		}
		
		if ((chatSessions != null) && (contactId != null)) {
			chatSessions.remove(contactId);
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
			ArrayList<IBinder> result = new ArrayList<IBinder>(chatSessions.size());
			for (Enumeration<ChatImpl> e = chatSessions.elements() ; e.hasMoreElements() ;) {
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
     * @param contactId Contact ID
     * @return Chat or null if not found
     * @throws ServerApiException
     */
    public IChat getChat(String contactId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get chat session with " + contactId);
		}

		// Return a session instance
		return chatSessions.get(contactId);
    }
    
    /**
	 * Receive a new group chat invitation
	 * 
	 * @param session Chat session
	 */
    public void receiveGroupChatInvitation(GroupChatSession session) {
		if (logger.isActivated()) {
			logger.info("Receive group chat invitation from " + session.getRemoteContact());
		}

		// Update rich messaging history
		MessagingLog.getInstance().addGroupChat(session.getContributionID(),
				session.getSubject(), session.getParticipants(), GroupChat.State.INVITED, GroupChat.Direction.INCOMING);
		
		// Add session in the list
		GroupChatImpl sessionApi = new GroupChatImpl(session);
		ChatServiceImpl.addGroupChatSession(sessionApi);

		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(GroupChatIntent.ACTION_NEW_INVITATION);
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
    	if (session.getRemoteContact() != null) {
    		intent.putExtra(GroupChatIntent.EXTRA_CONTACT, session.getRemoteContact().toString());
    	}
    	intent.putExtra(GroupChatIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
    	intent.putExtra(GroupChatIntent.EXTRA_CHAT_ID, sessionApi.getChatId());
    	intent.putExtra(GroupChatIntent.EXTRA_SUBJECT, sessionApi.getSubject());
    	AndroidFactory.getApplicationContext().sendBroadcast(intent);
    	
    	// Notify chat invitation listeners
    	synchronized(lock) {
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onNewGroupChat(sessionApi.getChatId());
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }    	
    }
	
	/**
	 * Add a group chat session in the list
	 * 
	 * @param session Chat session
	 */
	protected static void addGroupChatSession(GroupChatImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add a group chat session in the list (size=" + groupChatSessions.size() + ")");
		}
		
		groupChatSessions.put(session.getChatId(), session);
	}

	/**
	 * Remove a group chat session from the list
	 * 
	 * @param chatId Chat ID
	 */
	protected static void removeGroupChatSession(String chatId) {
		if (logger.isActivated()) {
			logger.debug("Remove a group chat session from the list (size=" + groupChatSessions.size() + ")");
		}
		
		groupChatSessions.remove(chatId);
	}

	/**
	 * Initiates a group chat with a group of contact and returns a GroupChat instance. The subject is optional and may be null.
	 * 
	 * @param contact
	 *            List of contact IDs
	 * @param subject
	 *            Subject
	 * @param listener
	 *            Chat event listener
	 * @throws ServerApiException
	 *             Note: List is used instead of Set because AIDL does only support List
	 */
    public IGroupChat initiateGroupChat(List<ContactId> contacts, String subject, IGroupChatListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate an ad-hoc group chat session");
		}
		
		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			final ChatSession session = Core.getInstance().getImService().initiateAdhocGroupChatSession(contacts, subject);

			// Add session listener
			GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session);
			sessionApi.addEventListener(listener);

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
			GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session);
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
			GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session);
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
		if (logger.isActivated()) {
			logger.info("Get group chat session " + chatId);
		}

		// Return a session instance
		return groupChatSessions.get(chatId);
	}
    
    /**
     * Adds a listener on new chat invitation events
     * 
     * @param listener Chat invitation listener
     * @throws ServerApiException
     */
    public void addEventListener(INewChatListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add a chat invitation listener");
		}
		
		listeners.register(listener);
    }
    
    /**
     * Removes a listener on new chat invitation events
     * 
     * @param listener Chat invitation listener
     * @throws ServerApiException
     */
    public void removeEventListener(INewChatListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove a chat invitation listener");
		}
		
		listeners.unregister(listener);
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
	 * Registers a new chat invitation listener
	 * 
	 * @param listener New file transfer listener
	 * @throws ServerApiException
	 */
	public void addNewChatListener(INewChatListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add a new chat invitation listener");
		}
		
		listeners.register(listener);
	}

	/**
	 * Unregisters a chat invitation listener
	 * 
	 * @param listener New file transfer listener
	 * @throws ServerApiException
	 */
	public void removeNewChatListener(INewChatListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove a chat invitation listener");
		}
		
		listeners.unregister(listener);
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
