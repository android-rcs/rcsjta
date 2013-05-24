/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.service.api.server.messaging;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.OneOneChatSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.messaging.IChatSession;
import com.orangelabs.rcs.service.api.client.messaging.IMessageDeliveryListener;
import com.orangelabs.rcs.service.api.client.messaging.IMessagingApi;
import com.orangelabs.rcs.service.api.client.messaging.MessagingApiIntents;
import com.orangelabs.rcs.service.api.server.ServerApiException;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Messaging API service
 * 
 * @author jexa7410
 */
public class MessagingApiService extends IMessagingApi.Stub {
	/**
	 * List of chat sessions
	 */
	private static Hashtable<String, IChatSession> chatSessions = new Hashtable<String, IChatSession>();  

	/**
	 * List of message delivery listeners
	 */
	private RemoteCallbackList<IMessageDeliveryListener> listeners = new RemoteCallbackList<IMessageDeliveryListener>();

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

	/**
	 * The logger
	 */
	private static Logger logger = Logger.getLogger(MessagingApiService.class.getName());

	/**
	 * Constructor
	 */
	public MessagingApiService() {
		if (logger.isActivated()) {
			logger.info("Messaging API service is loaded");
		}
	}
	
	/**
	 * Close API
	 */
	public void close() {
		// Clear lists of sessions
		chatSessions.clear();
	}

	/**
	 * Add a chat session in the list
	 * 
	 * @param session Chat session
	 */
	protected static void addChatSession(ImSession session) {
		if (logger.isActivated()) {
			logger.debug("Add a chat session in the list (size=" + chatSessions.size() + ")");
		}
		chatSessions.put(session.getSessionID(), session);
	}

	/**
	 * Remove a chat session from the list
	 * 
	 * @param sessionId Session ID
	 */
	protected static void removeChatSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a chat session from the list (size=" + chatSessions.size() + ")");
		}
		chatSessions.remove(sessionId);
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

		// Extract number from contact 
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Update rich messaging history
		RichMessaging.getInstance().addIncomingChatSession(session);

		// Add session in the list
		ImSession sessionApi = new ImSession(session);
		MessagingApiService.addChatSession(sessionApi);

		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(MessagingApiIntents.CHAT_INVITATION);
    	intent.putExtra("contact", number);
    	intent.putExtra("contactDisplayname", session.getRemoteDisplayName());
    	intent.putExtra("sessionId", session.getSessionID());
    	intent.putExtra("isGroupChat", false);
    	intent.putExtra("isStoreAndForward", session.isStoreAndForward());
    	intent.putExtra("firstMessage", session.getFirstMessage());
        intent.putExtra("autoAccept", RcsSettings.getInstance().isChatAutoAccepted());
    	AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }
    
	/**
	 * Extend a 1-1 chat session
	 * 
     * @param groupSession Group chat session
     * @param oneoneSession 1-1 chat session
	 */
    public void extendOneOneChatSession(GroupChatSession groupSession, OneOneChatSession oneoneSession) {
		if (logger.isActivated()) {
			logger.info("Extend a 1-1 chat session");
		}

		// Add session in the list
		ImSession sessionApi = new ImSession(groupSession);
		MessagingApiService.addChatSession(sessionApi);
		
		// Broadcast intent related to the received invitation
		Intent intent = new Intent(MessagingApiIntents.CHAT_SESSION_REPLACED);
		intent.putExtra("sessionId", groupSession.getSessionID());
		intent.putExtra("replacedSessionId", oneoneSession.getSessionID());
		AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }
	
    /**
	 * Initiate a one-to-one chat session
	 * 
     * @param contact Remote contact
     * @param firstMsg First message exchanged during the session
	 * @return Chat session
     * @throws ServerApiException
	 */
	public IChatSession initiateOne2OneChatSession(String contact, String firstMsg) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a 1-1 chat session with " + contact);
		}

    	// Check permission
		ServerApiUtils.testPermission();

		// Test IMS connection
		ServerApiUtils.testIms();
		
		try {
			// Initiate the session
			ChatSession session = Core.getInstance().getImService().initiateOne2OneChatSession(contact, firstMsg);
			
			// Update rich messaging history
			RichMessaging.getInstance().addOutgoingChatSession(session);
			
			// Add session in the list
			ImSession sessionApi = new ImSession(session);
			MessagingApiService.addChatSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

	/**
	 * Receive a new group chat invitation
	 * 
	 * @param session Chat session
	 */
    public void receiveGroupChatInvitation(GroupChatSession session) {
		if (logger.isActivated()) {
			logger.info("Receive chat invitation from " + session.getRemoteContact());
		}

		// Extract number from contact 
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Update rich messaging history
		RichMessaging.getInstance().addIncomingChatSession(session);

		// Add session in the list
		ImSession sessionApi = new ImSession(session);
		MessagingApiService.addChatSession(sessionApi);

		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(MessagingApiIntents.CHAT_INVITATION);
    	intent.putExtra("contact", number);
    	intent.putExtra("contactDisplayname", session.getRemoteDisplayName());
    	intent.putExtra("sessionId", session.getSessionID());
    	intent.putExtra("isGroupChat", true);
    	intent.putExtra("replacedSessionId", session.getReplacedSessionId());
    	intent.putExtra("subject", session.getSubject());
    	intent.putExtra("autoAccept", RcsSettings.getInstance().isGroupChatAutoAccepted());
    	AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }

	/**
	 * Initiate an ad-hoc group chat session
	 * 
     * @param participants List of participants
     * @param subject Subject associated to the session	 * @return Chat session
     * @throws ServerApiException
	 */
	public IChatSession initiateAdhocGroupChatSession(List<String> participants, String subject) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate an ad-hoc group chat session");
		}
		
    	// Check permission
		ServerApiUtils.testPermission();

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			ChatSession session = Core.getInstance().getImService().initiateAdhocGroupChatSession(participants, subject);

			// Update rich messaging history
			RichMessaging.getInstance().addOutgoingChatSession(session);
			
			// Add session in the list
			ImSession sessionApi = new ImSession(session);
			MessagingApiService.addChatSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}
	
	/**
	 * Rejoin a group chat session
	 * 
	 * @param chatId Chat ID
	 * @return Chat session
     * @throws ServerApiException
	 */
	public IChatSession rejoinGroupChatSession(String chatId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Rejoin group chat session related to the conversation " + chatId);
		}
		
    	// Check permission
		ServerApiUtils.testPermission();

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			ChatSession session = Core.getInstance().getImService().rejoinGroupChatSession(chatId);

			// Add session in the list
			ImSession sessionApi = new ImSession(session);
			MessagingApiService.addChatSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}	
	
	/**
	 * Restart a group chat session
	 * 
	 * @param chatId Chat ID
	 * @return Chat session
     * @throws ServerApiException
	 */
	public IChatSession restartGroupChatSession(String chatId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Restart group chat session related to the conversation " + chatId);
		}
		
    	// Check permission
		ServerApiUtils.testPermission();

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			ChatSession session = Core.getInstance().getImService().restartGroupChatSession(chatId);

			// Add session in the list
			ImSession sessionApi = new ImSession(session);
			MessagingApiService.addChatSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}	

	/**
	 * Get current chat session from its session id
	 * 
	 * @param id Session ID
	 * @return Session
	 * @throws ServerApiException
	 */
	public IChatSession getChatSession(String id) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get chat session " + id);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		// Return a session instance
		return chatSessions.get(id);
	}
	
	/**
	 * Get list of current chat sessions with a contact
	 * 
	 * @param contact Contact
	 * @return Session
	 * @throws ServerApiException
	 */
	public List<IBinder> getChatSessionsWith(String contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get chat sessions with " + contact);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();
		
		try {
			Vector<ChatSession> list = Core.getInstance().getImService().getImSessionsWith(contact);
			ArrayList<IBinder> result = new ArrayList<IBinder>(list.size());
			for(int i=0; i < list.size(); i++) {
				ChatSession session = list.elementAt(i);
				IChatSession sessionApi = chatSessions.get(session.getSessionID());
				if (sessionApi != null) {
					result.add(sessionApi.asBinder());
				}
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
	 * Get list of current chat sessions
	 * 
	 * @return List of sessions
	 * @throws ServerApiException
	 */
	public List<IBinder> getChatSessions() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get chat sessions");
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();
		
		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(chatSessions.size());
			for (Enumeration<IChatSession> e = chatSessions.elements() ; e.hasMoreElements() ;) {
				IChatSession sessionApi = (IChatSession)e.nextElement() ;
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
	 * Get list of current group chat sessions
	 * 
	 * @return List of sessions
	 * @throws ServerApiException
	 */
	public List<IBinder> getGroupChatSessions() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get group chat sessions");
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();
		
		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(chatSessions.size());
			for (Enumeration<IChatSession> e = chatSessions.elements() ; e.hasMoreElements() ;) {
				IChatSession sessionApi = (IChatSession)e.nextElement() ;
				if (sessionApi.isGroupChat()) {
					result.add(sessionApi.asBinder());
				}
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
	 * Get list of current group chat sessions for a given conversation
	 * 
	 * @return List of sessions
	 * @throws ServerApiException
	 */
	public List<IBinder> getGroupChatSessionsWith(String chatId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get group chat sessions");
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();
		
		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(chatSessions.size());
			for (Enumeration<IChatSession> e = chatSessions.elements() ; e.hasMoreElements() ;) {
				IChatSession sessionApi = (IChatSession)e.nextElement() ;
				String id = sessionApi.getChatID(); 
				if (sessionApi.isGroupChat() && id.equals(chatId)) {
					result.add(sessionApi.asBinder());
				}
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
	 * Set message delivery status outside of a chat session
	 * 
	 * @param contact Contact requesting a delivery status
	 * @param msgId Message ID
	 * @param status Delivery status
	 * @throws ServerApiException
	 */
	public void setMessageDeliveryStatus(String contact, String msgId, String status) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Set message delivery status " + status + " for message " + msgId);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testIms();
		
		try {
			// Send a delivery status
			Core.getInstance().getImService().getImdnManager().sendMessageDeliveryStatus(PhoneUtils.formatNumberToSipUri(contact),
					msgId, status);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}	

	/**
	 * Add message delivery listener
	 * 
	 * @param listener Listener
	 */
	public void addMessageDeliveryListener(IMessageDeliveryListener listener) {
		if (logger.isActivated()) {
			logger.info("Add a message delivery listener");
		}

		listeners.register(listener);
	}
	
	/**
	 * Remove message delivery listener
	 * 
	 * @param listener Listener
	 */
	public void removeMessageDeliveryListener(IMessageDeliveryListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove a message delivery listener");
		}

		listeners.unregister(listener);
	}

    /**
     * New message delivery status
     * 
	 * @param contact Contact
	 * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(String contact, String msgId, String status) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New message delivery status for message " + msgId + ", status " + status);
			}
	
			// Update rich messaging history
			RichMessaging.getInstance().setChatMessageDeliveryStatus(msgId, status);
			
	  		// Notify message delivery listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleMessageDeliveryStatus(contact, msgId, status);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
    	}
    }
}
