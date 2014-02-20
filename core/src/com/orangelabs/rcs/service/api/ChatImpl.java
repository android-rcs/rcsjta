package com.orangelabs.rcs.service.api;

import android.content.Intent;
import android.os.RemoteCallbackList;

import com.gsma.services.rcs.chat.ChatIntent;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.IChat;
import com.gsma.services.rcs.chat.IChatListener;
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
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.utils.PhoneUtils;
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
	private String contact;
	
	/**
	 * Core session
	 */
	private OneOneChatSession session;
	
	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IChatListener> listeners = new RemoteCallbackList<IChatListener>();

	/**
	 * Lock used for synchronisation
	 */
	private Object lock = new Object();

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param contact Remote contact
	 */
	public ChatImpl(String contact) {
		this.contact = contact;
		this.session = null;
	}
	
	/**
	 * Constructor
	 * 
	 * @param contact Remote contact
	 * @param session Session
	 */
	public ChatImpl(String contact, OneOneChatSession session) {
		this.contact = contact;
		this.session = session;
		
		session.addListener(this);
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
     * Returns the remote contact
     * 
     * @return Contact
     */
    public String getRemoteContact() {
		return PhoneUtils.extractNumberFromUri(contact);
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
				geoloc.getLatitude(), geoloc.getLongitude(), geoloc.getAltitude(),
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
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.debug("Send chat message");
			}
			
			// Check if a session should be initiated or not
	    	if ((session == null) || session.getDialogPath().isSessionTerminated()) {
	    		try {
	    			if (logger.isActivated()) {
	    				logger.debug("Core session is not yet established: initiate a new session to send the message");
	    			}
	
	    	    	// Initiate a new session
					session = (OneOneChatSession)Core.getInstance().getImService().initiateOne2OneChatSession(contact, msg);
					
					// Update with new session
					setCoreSession(session);
			
					// Update rich messaging history
					RichMessagingHistory.getInstance().addChatMessage(msg, ChatLog.Message.Direction.OUTGOING);
	
					// Start the session
			        Thread t = new Thread() {
			    		public void run() {
							session.startSession();
			    		}
			    	};
			    	t.start();
					return session.getFirstMessage().getMessageId();
				} catch(Exception e) {
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
				final String msgId = ChatUtils.generateMessageId();
		
				// Send message
		        Thread t = new Thread() {
		    		public void run() {
						if (msg instanceof GeolocMessage) {
							session.sendGeolocMessage(msgId, ((GeolocMessage)msg).getGeoloc());
						} else {
							session.sendTextMessage(msgId, msg.getTextMessage());					
						}
		    		}
		    	};
		    	t.start();
				return msgId;
	    	}
		}    	
    }
    
    /**
     * Sends a displayed delivery report for a given message ID
     * 
     * @param msgId Message ID
     */
    public void sendDisplayedDeliveryReport(final String msgId) {
		try {
			if (logger.isActivated()) {
				logger.debug("Set displayed delivery report for " + msgId);
			}

			// Send delivery status
			if ((session != null) &&
					(session.getDialogPath() != null) &&
						(session.getDialogPath().isSessionEstablished())) { 
				// Send via MSRP
		        Thread t = new Thread() {
		    		public void run() {
						session.sendMsrpMessageDeliveryStatus(session.getRemoteContact(), msgId, ImdnDocument.DELIVERY_STATUS_DISPLAYED);
		    		}
		    	};
		    	t.start();
			} else {
				// Send via SIP MESSAGE
				Core.getInstance().getImService().getImdnManager().sendMessageDeliveryStatus(
						session.getRemoteContact(), msgId, ImdnDocument.DELIVERY_STATUS_DISPLAYED);
			}
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Could not send MSRP delivery status",e);
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
	        Thread t = new Thread() {
	    		public void run() {
	        		session.sendIsComposingStatus(status);
	    		}
	    	};
	    	t.start();
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

    /**
	 * Session is started
	 */
    public void handleSessionStarted() {
    	synchronized(lock) {
	    	if (logger.isActivated()) {
				logger.info("Session started");
			}

			// Update rich messaging history
	    	// Nothing done in database
	    }
    }
    
    /**
     * Session has been aborted
     * 
	 * @param reason Termination reason
	 */
    public void handleSessionAborted(int reason) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session aborted (reason " + reason + ")");
			}
	
			// Update rich messaging history
	    	// Nothing done in database
	        
	        // Remove session from the list
	        ChatServiceImpl.removeChatSession(session.getContributionID());
	    }
    }
    
    /**
     * Session has been terminated by remote
     */
    public void handleSessionTerminatedByRemote() {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session terminated by remote");
			}
	
			// Update rich messaging history
	    	// Nothing done in database
			
	        // Remove session from the list
			ChatServiceImpl.removeChatSession(session.getContributionID());
	    }
    }
    
	/**
	 * New text message received
	 * 
	 * @param text Text message
	 */
    public void handleReceiveMessage(InstantMessage message) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New IM received");
			}
			
			// Update rich messaging history
			RichMessagingHistory.getInstance().addChatMessage(message, ChatLog.Message.Direction.INCOMING);
			
			// Create a chat message
        	ChatMessage msgApi = new ChatMessage(message.getMessageId(),
        			PhoneUtils.extractNumberFromUri(message.getRemote()),
        			message.getTextMessage(),
        			message.getServerDate(), message.isImdnDisplayedRequested());

        	// Broadcast intent related to the received invitation
	    	Intent intent = new Intent(ChatIntent.ACTION_NEW_CHAT);
	    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
	    	intent.putExtra(ChatIntent.EXTRA_CONTACT, msgApi.getContact());
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
    
    /**
     * New geoloc message received
     * 
     * @param geoloc Geoloc message
     */
    public void handleReceiveGeoloc(GeolocMessage geoloc) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New geoloc received");
			}
			
			// Update rich messaging history
			RichMessagingHistory.getInstance().addChatMessage(geoloc, ChatLog.Message.Direction.INCOMING);
			
			// Create a geoloc message
        	Geoloc geolocApi = new Geoloc(geoloc.getGeoloc().getLabel(),
        			geoloc.getGeoloc().getLatitude(), geoloc.getGeoloc().getLongitude(),
        			geoloc.getGeoloc().getAltitude(), geoloc.getGeoloc().getExpiration());
        	com.gsma.services.rcs.chat.GeolocMessage msgApi = new com.gsma.services.rcs.chat.GeolocMessage(geoloc.getMessageId(),
        			PhoneUtils.extractNumberFromUri(geoloc.getRemote()),
        			geolocApi, geoloc.getDate(), geoloc.isImdnDisplayedRequested());

        	// Broadcast intent related to the received invitation
	    	Intent intent = new Intent(ChatIntent.ACTION_NEW_CHAT);
	    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
	    	intent.putExtra(ChatIntent.EXTRA_CONTACT, msgApi.getContact());
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
    
    /**
     * IM session error
     * 
     * @param error Error
     */
    public void handleImError(ChatError error) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("IM error " + error.getErrorCode());
			}
			
			// Update rich messaging history
	    	switch(error.getErrorCode()){
		    	case ChatError.SESSION_INITIATION_FAILED:
		    	case ChatError.SESSION_INITIATION_CANCELLED:
					RichMessagingHistory.getInstance().updateChatMessageStatus(session.getFirstMessage().getMessageId(),
							ChatLog.Message.Status.Content.FAILED);
					// TODO: notify listener
		    		break;
		    	default:
		    		break;
	    	}
	    	
	        // Remove session from the list
	        ChatServiceImpl.removeChatSession(session.getContributionID());
	    }
    }
    
    /**
	 * Is composing event
	 * 
	 * @param contact Contact
	 * @param status Status
	 */
	public void handleIsComposingEvent(String contact, boolean status) {
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
    
    /**
     * New message delivery status
     * 
	 * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(String msgId, String status) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New message delivery status for message " + msgId + ", status " + status);
			}
	
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageDeliveryStatus(msgId, status);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
	            		listeners.getBroadcastItem(i).onReportMessageDelivered(msgId);
	            	} else
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
	            		listeners.getBroadcastItem(i).onReportMessageDisplayed(msgId);
	            	} else
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_ERROR)) {
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
    
    /**
     * Conference event
     * 
	 * @param contact Contact
	 * @param contactDisplayname Contact display name
     * @param state State associated to the contact
     */
    public void handleConferenceEvent(String contact, String contactDisplayname, String state) {
    	// Not used here
    }
    
    /**
     * Request to add participant is successful
     */
    public void handleAddParticipantSuccessful() {
    	// Not used in single chat
    }
    
    /**
     * Request to add participant has failed
     * 
     * @param reason Error reason
     */
    public void handleAddParticipantFailed(String reason) {
    	// Not used in single chat
    }
}