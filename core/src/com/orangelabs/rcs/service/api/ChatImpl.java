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

import android.content.Intent;
import android.os.Parcelable;
import android.os.RemoteCallbackList;

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
	
	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IChatListener> listeners = new RemoteCallbackList<IChatListener>();

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

	/**
	 * The logger
	 */
	private final static Logger logger = Logger.getLogger(ChatImpl.class.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param contact Remote contact
	 */
	public ChatImpl(ContactId contact) {
		this(contact,null);
	}
	
	/**
	 * Constructor
	 * 
	 * @param contact Remote contact ID
	 * @param session Session
	 */
	public ChatImpl(ContactId contact, OneOneChatSession session) {
		this.contact = contact;
		this.session = session;
		if (session != null) {
			session.addListener(this);
		}
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
     * Sends an “is-composing” event. The status is set to true when
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
	
    /**
     * Adds a listener on chat events
     *  
     * @param listener Chat event listener
     */
    public void addEventListener(IChatListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an event listener");
		}

    	synchronized(lock) {
    		listeners.register(listener);
    	}
    }
	
    /**
     * Removes a listener on chat events
     * 
     * @param listener Chat event listener
     */
    public void removeEventListener(IChatListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove an event listener");
		}

    	synchronized(lock) {
    		listeners.unregister(listener);
    	}
    }
    
    /*------------------------------- SESSION EVENTS ----------------------------------*/

    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionStarted()
     */
    @Override
    public void handleSessionStarted() {
    	synchronized(lock) {
	    	if (logger.isActivated()) {
				logger.info("Session started");
			}

			// Update rich messaging history
	    	// Nothing done in database
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionAborted(int)
     */
    @Override
    public void handleSessionAborted(int reason) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session aborted (reason " + reason + ")");
			}
	
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
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session terminated by remote");
			}
	
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
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New IM received "+message);
			}
			
			// Update rich messaging history
			MessagingLog.getInstance().addChatMessage(message, ChatLog.Message.Direction.INCOMING);
			
			// Create a chat message
        	ChatMessage msgApi = new ChatMessage(message.getMessageId(),
        			message.getRemote(),
        			message.getTextMessage(),
        			message.getServerDate());

        	// Broadcast intent related to the received invitation
	    	Intent intent = new Intent(ChatIntent.ACTION_NEW_CHAT);
	    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
	    	intent.putExtra(ChatIntent.EXTRA_CONTACT, (Parcelable)msgApi.getContact());
	    	intent.putExtra(ChatIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
	    	intent.putExtra(ChatIntent.EXTRA_MESSAGE, msgApi);
	    	AndroidFactory.getApplicationContext().sendBroadcast(intent);

	    	// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onNewMessage(msgApi);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();		
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleReceiveGeoloc(com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage)
     */
    @Override
    public void handleReceiveGeoloc(GeolocMessage geoloc) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New geoloc received");
			}
			
			// Update rich messaging history
			MessagingLog.getInstance().addChatMessage(geoloc, ChatLog.Message.Direction.INCOMING);
			
			// Create a geoloc message
        	Geoloc geolocApi = new Geoloc(geoloc.getGeoloc().getLabel(),
        			geoloc.getGeoloc().getLatitude(), geoloc.getGeoloc().getLongitude(),
        			geoloc.getGeoloc().getExpiration());
        	com.gsma.services.rcs.chat.GeolocMessage msgApi = new com.gsma.services.rcs.chat.GeolocMessage(geoloc.getMessageId(),
        			geoloc.getRemote(),
        			geolocApi, geoloc.getDate());

        	// Broadcast intent related to the received invitation
	    	Intent intent = new Intent(ChatIntent.ACTION_NEW_CHAT);
	    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
	    	intent.putExtra(ChatIntent.EXTRA_CONTACT, (Parcelable)msgApi.getContact());
	    	intent.putExtra(ChatIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
	    	intent.putExtra(ChatIntent.EXTRA_MESSAGE, msgApi);
	    	AndroidFactory.getApplicationContext().sendBroadcast(intent);

	    	// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onNewGeoloc(msgApi);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();		
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleImError(com.orangelabs.rcs.core.ims.service.im.chat.ChatError)
     */
    @Override
    public void handleImError(ChatError error) {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("IM error " + error.getErrorCode());
			}

			// Update rich messaging history
			switch (error.getErrorCode()) {
			case ChatError.SESSION_INITIATION_FAILED:
			case ChatError.SESSION_INITIATION_CANCELLED:
				MessagingLog.getInstance().updateChatMessageStatus(session.getFirstMessage().getMessageId(),
						ChatLog.Message.Status.Content.FAILED);
				// notify listener
				final int N = listeners.beginBroadcast();
				for (int i = 0; i < N; i++) {
					try {
						listeners.getBroadcastItem(i).onReportMessageFailed(session.getFirstMessage().getMessageId());
					} catch (Exception e) {
						if (logger.isActivated()) {
							logger.error("Can't notify listener", e);
						}
					}
				}
				listeners.finishBroadcast();
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
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info(contact + " is composing status set to " + status);
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onComposingEvent(status);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
		}
	}

    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleMessageDeliveryStatus(java.lang.String, java.lang.String)
     */
	public void handleSendMessageFailure(String msgId) {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.info("New message failure status for message " + msgId);
			}

			// Update rich messaging history
			MessagingLog.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);

			// Notify event listeners
			final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
				try {
					listeners.getBroadcastItem(i).onReportMessageFailed(msgId);
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't notify listener", e);
					}
				}
			}
			listeners.finishBroadcast();
		}
	}

	@Override
	public void handleMessageDeliveryStatus(String msgId, String status, ContactId contact) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New message delivery status for message " + msgId + ", status " + status);
			}
	
			// Update rich messaging history
			MessagingLog.getInstance().updateOutgoingChatMessageDeliveryStatus(msgId, status);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
				try {
					if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
						listeners.getBroadcastItem(i).onReportMessageDelivered(msgId);
					} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
						listeners.getBroadcastItem(i).onReportMessageDisplayed(msgId);
					} else if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
							|| ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
							|| ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
						listeners.getBroadcastItem(i).onReportMessageFailed(msgId);
					}
				} catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }
    }
    
    @Override
    public void handleConferenceEvent(ContactId contact, String contactDisplayname, String state) {
    	// Not used here
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleAddParticipantSuccessful()
     */
    @Override
    public void handleAddParticipantSuccessful() {
    	// Not used in single chat
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleAddParticipantFailed(java.lang.String)
     */
    @Override
    public void handleAddParticipantFailed(String reason) {
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
