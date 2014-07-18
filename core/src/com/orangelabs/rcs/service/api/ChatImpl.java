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

import static com.gsma.services.rcs.chat.ChatLog.Message.Status.Content.FAILED;
import static com.gsma.services.rcs.chat.ChatLog.Message.Status.Content.DELIVERED;
import static com.gsma.services.rcs.chat.ChatLog.Message.Status.Content.DISPLAYED;
import static com.gsma.services.rcs.chat. ChatLog.Message.Direction.INCOMING;

import android.content.Intent;
import android.os.Parcelable;
import android.os.RemoteCallbackList;
import android.util.Pair;

import com.gsma.services.rcs.chat.ChatIntent;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.IChat;
import com.gsma.services.rcs.chat.IChatListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.OneOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.service.broadcaster.IOneToOneChatEventBroadcaster;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Chat implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatImpl extends IChat.Stub implements ChatSessionListener {
	/**
	 * Remote contact
	 */
	private ContactId contact;
	
	/**
	 * Core session
	 */
	private OneOneChatSession session;

	private final IOneToOneChatEventBroadcaster mChatEventBroadcaster;

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
	 * @param session Session
	 * @param broadcaster IChatEventBroadcaster
	 */
	public ChatImpl(ContactId contact, OneOneChatSession session,
			IOneToOneChatEventBroadcaster broadcaster) {
		this.contact = contact;
		this.session = session;
		mChatEventBroadcaster = broadcaster;

		session.addListener(this);
	}

	/**
	 * Constructor
	 *
	 * @param contact Remote contact
	 * @param chatEventBroadcaster IChatEventBroadcaster
	 */
	public ChatImpl(ContactId contact, IOneToOneChatEventBroadcaster chatEventBroadcaster) {
		this(contact, null, chatEventBroadcaster);
	}

	/**
	 * Set core session
	 * 
	 * @param session Core session
	 */
	public void setCoreSession(OneOneChatSession session) {
		this.session = session;
		
		session.addListener(this);
	}	
	
	/**
	 * Reset core session
	 */
	public void resetCoreSession() {
		this.session = null;
	}	

	/**
	 * Get core session
	 * 
	 * @return Core session
	 */
	public OneOneChatSession getCoreSession() {
		return session;
	}
	
    /**
     * Returns the remote contact identifier
     * 
     * @return ContactId
     * @throws Exception 
     */
	public ContactId getRemoteContact() {
		return contact;
	}
	
	/**
     * Sends a plain text message
     * 
     * @param message Text message
     * @return Unique message ID or null in case of error
     */
    public String sendMessage(String message) {
		if (logger.isActivated()) {
			logger.debug("Send text message");
		}

		// Create a text message
    	InstantMessage msg = ChatUtils.createTextMessage(contact, message,
    			Core.getInstance().getImService().getImdnManager().isImdnActivated());

    	// Send message
	    return sendChatMessage(msg);
    }
    
	/**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc
     * @return Unique message ID or null in case of error
     */
    public String sendGeoloc(Geoloc geoloc) {
		if (logger.isActivated()) {
			logger.debug("Send geoloc message");
		}
			
		// Create a geoloc message
		GeolocPush geolocPush = new GeolocPush(geoloc.getLabel(),
				geoloc.getLatitude(), geoloc.getLongitude(),
				geoloc.getExpiration(), geoloc.getAccuracy());

		// Create a geoloc message
    	GeolocMessage msg = ChatUtils.createGeolocMessage(contact, geolocPush,
    			Core.getInstance().getImService().getImdnManager().isImdnActivated());

    	// Send message
	    return sendChatMessage(msg);
    }

	/**
     * Sends a chat message
     * 
     * @param msg Message
     * @return Unique message ID or null in case of error
     */
	private String sendChatMessage(final InstantMessage msg) {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.debug("Send chat message");
			}

			// Check if a session should be initiated or not
			if ((session == null) || !session.isMediaEstablished()) {

				try {
					if (logger.isActivated()) {
						logger.debug("Core session is not yet established: initiate a new session to send the message");
					}
					// Initiate a new session
					session = (OneOneChatSession) Core.getInstance().getImService().initiateOne2OneChatSession(contact, msg);
					// Update with new session
					setCoreSession(session);
					// Start the session
					new Thread() {
						public void run() {
							session.startSession();
						}
					}.start();
					return session.getFirstMessage().getMessageId();
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't send a new chat message", e);
					}
					return null;
				}
			} else {
				if (logger.isActivated()) {
					logger.debug("Core session is established: use existing one to send the message");
				}
				// Generate a message Id
				final String msgId = IdGenerator.generateMessageID();
				// Send message
				new Thread() {
					public void run() {
						if (msg instanceof GeolocMessage) {
							session.sendGeolocMessage(msgId, ((GeolocMessage) msg).getGeoloc());
						} else {
							session.sendTextMessage(msgId, msg.getTextMessage());
						}
					}
				}.start();
				return msgId;
			}
		}
	}
    
    /**
     * Sends a displayed delivery report for a given message ID
     * 
     * @param contact Contact ID
     * @param msgId Message ID
     */
    /*package private*/ void sendDisplayedDeliveryReport(final ContactId contact, final String msgId) {
		try {
			if (logger.isActivated()) {
				logger.debug("Set displayed delivery report for " + msgId);
			}

			// Send delivery status: check if media is established
			if ((session != null) && session.isMediaEstablished()) {
				if (logger.isActivated()) {
					logger.info("Use the original session to send the delivery status for " + msgId);
				}
				// Send via MSRP

		        new Thread() {
		    		public void run() {
						session.sendMsrpMessageDeliveryStatus(contact, msgId, ImdnDocument.DELIVERY_STATUS_DISPLAYED);
		    		}
		    	}.start();
			} else {
				if (logger.isActivated()) {
					logger.info("No suitable session found to send the delivery status for " + msgId + " : use SIP message");
				}
				// Send via SIP MESSAGE
				Core.getInstance().getImService().getImdnManager()
						.sendMessageDeliveryStatus(contact, msgId, ImdnDocument.DELIVERY_STATUS_DISPLAYED);
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Could not send MSRP delivery status", e);
			}
		}
	}
	
    /**
     * Sends an is-composing event. The status is set to true when
     * typing a message, else it is set to false.
     * 
     * @param status Is-composing status
     */
    public void sendIsComposingEvent(final boolean status) {
    	if (session != null) {
	        new Thread() {
	    		public void run() {
	        		session.sendIsComposingStatus(status);
	    		}
	    	}.start();
    	}
    }

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionStarted()
     */
    @Override
	public void handleSessionStarted() {
		if (logger.isActivated()) {
			logger.info("Session started");
		}
		// Update rich messaging history
		// Nothing done in database
	}
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionAborted(int)
     */
    @Override
	public void handleSessionAborted(int reason) {
		if (logger.isActivated()) {
			logger.info("Session aborted (reason " + reason + ")");
		}
		synchronized (lock) {
			// Update rich messaging history
			// Nothing done in database
			// Remove session from the list
			ChatServiceImpl.removeChatSession(session.getRemoteContact());
		}
	}
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionTerminatedByRemote()
     */
    @Override
	public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session terminated by remote");
		}
		synchronized (lock) {
			// Update rich messaging history
			// Nothing done in database
			// Remove session from the list
			ChatServiceImpl.removeChatSession(session.getRemoteContact());
		}
	}
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleReceiveMessage(com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage)
     */
    @Override
    public void handleReceiveMessage(InstantMessage message) {
		if (logger.isActivated()) {
			logger.info("New IM received "+message);
		}
    	synchronized(lock) {
			// Update rich messaging history
			MessagingLog.getInstance().addChatMessage(message, INCOMING);
			// Create a chat message
        	ChatMessage chatMsg = new ChatMessage(message.getMessageId(),
        			message.getRemote(),
        			message.getTextMessage(),
        			message.getServerDate());

        	// Broadcast intent related to the received invitation
	    	Intent intent = new Intent(ChatIntent.ACTION_NEW_CHAT);
	    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
	    	intent.putExtra(ChatIntent.EXTRA_CONTACT, (Parcelable)chatMsg.getContact());
	    	intent.putExtra(ChatIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
	    	intent.putExtra(ChatIntent.EXTRA_MESSAGE, chatMsg);
	    	AndroidFactory.getApplicationContext().sendBroadcast(intent);

	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleReceiveGeoloc(com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage)
     */
    @Override
    public void handleReceiveGeoloc(GeolocMessage geoloc) {
		if (logger.isActivated()) {
			logger.info("New geoloc received");
		}
    	synchronized(lock) {
			// Update rich messaging history
			MessagingLog.getInstance().addChatMessage(geoloc, INCOMING);
			// Create a geoloc message
        	Geoloc geolocApi = new Geoloc(geoloc.getGeoloc().getLabel(),
        			geoloc.getGeoloc().getLatitude(), geoloc.getGeoloc().getLongitude(),
        			geoloc.getGeoloc().getExpiration());
        	com.gsma.services.rcs.chat.GeolocMessage geolocMsg = new com.gsma.services.rcs.chat.GeolocMessage(geoloc.getMessageId(),
        			geoloc.getRemote(),
        			geolocApi, geoloc.getDate());

        	// Broadcast intent related to the received invitation
	    	Intent intent = new Intent(ChatIntent.ACTION_NEW_CHAT);
	    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
	    	intent.putExtra(ChatIntent.EXTRA_CONTACT, (Parcelable)geolocMsg.getContact());
	    	intent.putExtra(ChatIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
	    	intent.putExtra(ChatIntent.EXTRA_MESSAGE, geolocMsg);
	    	AndroidFactory.getApplicationContext().sendBroadcast(intent);

	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleImError(com.orangelabs.rcs.core.ims.service.im.chat.ChatError)
     */
    @Override
    public void handleImError(ChatError error) {
		if (logger.isActivated()) {
			logger.info("IM error " + error.getErrorCode());
		}
		synchronized (lock) {
			// Update rich messaging history
			switch (error.getErrorCode()) {
				case ChatError.SESSION_INITIATION_FAILED:
				case ChatError.SESSION_INITIATION_CANCELLED:
					String msgId = session.getFirstMessage().getMessageId();
					MessagingLog.getInstance().updateChatMessageStatus(msgId, FAILED);
					// notify listener
					mChatEventBroadcaster.broadcastMessageStatusChanged(getRemoteContact(), msgId, FAILED);
					break;
				default:
					break;
			}
			// Remove session from the list
			ChatServiceImpl.removeChatSession(session.getRemoteContact());
		}
	}
    
	@Override
	public void handleIsComposingEvent(ContactId contact, boolean status) {
		if (logger.isActivated()) {
			logger.info(contact + " is composing status set to " + status);
		}
    	synchronized(lock) {
	  		// Notify event listeners
			mChatEventBroadcaster.broadcastComposingEvent(contact, status);
		}
	}

    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleMessageDeliveryStatus(java.lang.String, java.lang.String)
     */
	@Override
	public void handleSendMessageFailure(String msgId) {
		if (logger.isActivated()) {
			logger.info("New message failure status for message " + msgId);
		}
		synchronized (lock) {
			// Update rich messaging history
			MessagingLog.getInstance().updateChatMessageStatus(msgId, FAILED);

			// Notify event listeners
			mChatEventBroadcaster.broadcastMessageStatusChanged(getRemoteContact(), msgId, FAILED);
		}
	}

	@Override
	public void handleMessageDeliveryStatus(String msgId, String status, ContactId contact) {
		if (logger.isActivated()) {
			logger.info("New message delivery status for message " + msgId + ", status " + status);
		}
    	synchronized(lock) {
			// Update rich messaging history
			MessagingLog.getInstance().updateOutgoingChatMessageDeliveryStatus(msgId, status);

			// Notify event listeners
			if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
				mChatEventBroadcaster.broadcastMessageStatusChanged(contact, msgId, DELIVERED);
			} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
				mChatEventBroadcaster.broadcastMessageStatusChanged(contact, msgId, DISPLAYED);
			} else if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
					|| ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
					|| ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
				mChatEventBroadcaster.broadcastMessageStatusChanged(contact, msgId, FAILED);
			}
	    }
    }
    
    @Override
    public void handleConferenceEvent(ContactId contact, String contactDisplayname, String state) {
    	// Not used here
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleAddParticipantSuccessful(com.gsma.services.rcs.contact.ContactId)
     */
    @Override
    public void handleAddParticipantSuccessful(ContactId contact) {
    	// Not used in single chat
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleAddParticipantFailed(com.gsma.services.rcs.contact.ContactId, java.lang.String)
     */
    @Override
    public void handleAddParticipantFailed(ContactId contact, String reason) {
    	// Not used in single chat
    }

	/* (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleParticipantStatusChanged(com.gsma.services.rcs.chat.ParticipantInfo)
	 */
	@Override
	public void handleParticipantStatusChanged(ParticipantInfo participantInfo) {
		// Not used in single chat
	}
}
