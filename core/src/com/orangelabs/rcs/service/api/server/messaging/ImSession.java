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

import java.util.List;

import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.messaging.GeolocMessage;
import com.orangelabs.rcs.service.api.client.messaging.GeolocPush;
import com.orangelabs.rcs.service.api.client.messaging.IChatEventListener;
import com.orangelabs.rcs.service.api.client.messaging.IChatSession;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * IM session
 * 
 * @author jexa7410
 */
public class ImSession extends IChatSession.Stub implements ChatSessionListener {
	
	/**
	 * Core session
	 */
	private ChatSession session;

	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IChatEventListener> listeners = new RemoteCallbackList<IChatEventListener>();

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
	 * @param session Session
	 */
	public ImSession(ChatSession session) {
		this.session = session;
		session.addListener(this);
	}

	/**
	 * Get session ID
	 * 
	 * @return Session ID
	 */
	public String getSessionID() {
		return session.getSessionID();
	}
	
	/**
	 * Get chat ID
	 * 
	 * @return Chat ID
	 */
	public String getChatID() {
		return session.getContributionID();
	}
	
	/**
	 * Get remote contact
	 * 
	 * @return Contact
	 */
	public String getRemoteContact() {
		return session.getRemoteContact();
	}
	
	/**
	 * Get session state
	 * 
	 * @return State (see class SessionState) 
	 */
	public int getSessionState() {
		return ServerApiUtils.getSessionState(session);
	}
	
	/**
	 * Is group chat
	 * 
	 * @return Boolean
	 */
	public boolean isGroupChat() {
		return session.isGroupChat();
	}
	
	/**
	 * Is Store & Forward
	 * 
	 * @return Boolean
	 */
	public boolean isStoreAndForward() {
		return session.isStoreAndForward();
	}
	
	/**
	 * Get first message exchanged during the session
	 * 
	 * @return First message
	 */
	public InstantMessage getFirstMessage() {
		return session.getFirstMessage();
	}
	
	/**
	 * Get subject associated to the session
	 * 
	 * @return String
	 */
	public String getSubject() {
		return session.getSubject();
	}

	/**
	 * Accept the session invitation
	 */
	public void acceptSession() {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}
				
		// Accept invitation
		session.acceptSession();
	}
	
	/**
	 * Reject the session invitation
	 */ 
	public void rejectSession() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}

		// Update rich messaging history
		RichMessaging.getInstance().addChatSessionTermination(session);
		
        // Reject invitation
		session.rejectSession();
	}

	/**
	 * Cancel the session
	 */
	public void cancelSession() {
		if (logger.isActivated()) {
			logger.info("Cancel session");
		}
		
		// Abort the session
		session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
	}
	
	/**
	 * Get list of participants connected to the session
	 * 
	 * @return List
	 */
	public List<String> getParticipants() {
		if (logger.isActivated()) {
			logger.info("Get list of connected participants in the session");
		}
		return session.getConnectedParticipants().getList();
	}

    /**
     * Get max number of participants in the session
     *
     * @return max
     */
    public int getMaxParticipants() {
        if (logger.isActivated()) {
            logger.info("Get max number of participants in the session");
        }
        return session.getMaxParticipants();
    }

	/**
	 * Get max number of participants which can be added to the conference
	 * 
	 * @return Number
	 */
	public int getMaxParticipantsToBeAdded() {
		int max = session.getMaxParticipants()-1;
		int connected = session.getConnectedParticipants().getList().size(); 
		return max-connected;
	}	

	/**
	 * Add a participant to the session
	 * 
	 * @param participant Participant
	 */
	public void addParticipant(String participant) {
		if (logger.isActivated()) {
			logger.info("Add participant " + participant + " to the session");
		}

		int max = session.getMaxParticipants()-1;
		int connected = session.getConnectedParticipants().getList().size(); 
		if (connected < max) {
            // Add a participant to the session
            session.addParticipant(participant);
        } else {
        	// Max participants achieved
            handleAddParticipantFailed("Maximum number of participants reached");
        }
	}

	/**
	 * Add a list of participants to the session
	 * 
	 * @param participants List of participants
	 */
	public void addParticipants(List<String> participants) {
		if (logger.isActivated()) {
			logger.info("Add " + participants.size() + " participants to the session");
		}

		int max = session.getMaxParticipants()-1;
		int connected = session.getConnectedParticipants().getList().size(); 
        if (connected < max) {
            // Add a list of participants to the session
            session.addParticipants(participants);
        } else {
        	// Max participants achieved
            handleAddParticipantFailed("Maximum number of participants reached");
        }
	}
	
	/**
	 * Send a text message
	 * 
	 * @param text Text message
	 * @return Message ID
	 */
	public String sendMessage(String text) {
		// Generate a message Id
		String msgId = ChatUtils.generateMessageId();

		// Send text message
		session.sendTextMessage(msgId, text);

		return msgId;
	}

	/**
	 * Is geoloc supported
	 * 
	 * @return Boolean
	 */
	public boolean isGeolocSupported() {
		return RcsSettings.getInstance().isGeoLocationPushSupported(); // TODO && session.isGeolocSupportedByRemote();
	}
	
    /**
     * Send a geoloc message
     * 
     * @param geoloc Geolocation
     * @return Message ID
     */
    public String sendGeoloc(GeolocPush geoloc) {
		// Generate a message Id
		String msgId = ChatUtils.generateMessageId();

		// Send text message
		session.sendGeolocMessage(msgId, geoloc);

		return msgId;
    }

	/**
	 * Is file transfer over HTTP supported
	 * 
	 * @return Boolean
	 */
	public boolean isFileTransferHttpSupported() {
		return RcsSettings.getInstance().isFileTransferHttpSupported(); // TODO && session.isFileTransferHttpSupportedByRemote();
	}

    /**
     * Send file over HTTP
     * 
     * @param filename Filename
     */
    public void sendFile(String filename) {
		// TODO
    }

    /**
	 * Set is composing status
	 * 
	 * @param status Status
	 */
	public void setIsComposingStatus(boolean status) {
		session.sendIsComposingStatus(status);
	}

	/**
	 * Set message delivery status
	 * 
	 * @param msgId Message ID
	 * @param status Delivery status
	 */
	public void setMessageDeliveryStatus(String msgId, String status) {
		try {
			if (logger.isActivated()) {
				logger.debug("Set message delivery status " + status + " for " + msgId);
			}
			
			// Send MSRP delivery status
			session.sendMsrpMessageDeliveryStatus(session.getRemoteContact(), msgId, status);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Could not send MSRP delivery status",e);
			}
		}
	}
	
	/**
	 * Add session listener
	 * 
	 * @param listener Listener
	 */
	public void addSessionListener(IChatEventListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an event listener");
		}

    	synchronized(lock) {
    		listeners.register(listener);
    	}
	}
	
	/**
	 * Remove session listener
	 * 
	 * @param listener Listener
	 */
	public void removeSessionListener(IChatEventListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove an event listener");
		}

    	synchronized(lock) {
    		listeners.unregister(listener);
    	}
	}
	
	/**
	 * Session is started
	 */
    public void handleSessionStarted() {
    	synchronized(lock) {
	    	if (logger.isActivated()) {
				logger.info("Session started");
			}

			// Update rich messaging history
			RichMessaging.getInstance().markChatSessionStarted(session);
	    	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleSessionStarted();
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
			if (reason == ImsServiceSession.TERMINATION_BY_USER) {
				RichMessaging.getInstance().addChatSessionTerminationByUser(session);
			} else {
				RichMessaging.getInstance().addChatSessionTermination(session);
			}
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleSessionAborted(reason);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        MessagingApiService.removeChatSession(session.getSessionID());
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
			RichMessaging.getInstance().addChatSessionTerminationByRemote(session);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleSessionTerminatedByRemote();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        MessagingApiService.removeChatSession(session.getSessionID());
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
			RichMessaging.getInstance().addIncomingChatMessage(message, session);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleReceiveMessage(message);
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
	    		case ChatError.SESSION_NOT_FOUND:
	    		case ChatError.SESSION_RESTART_FAILED:
	    			// These errors are not logged
	    			break;
		    	case ChatError.SESSION_INITIATION_DECLINED:
					RichMessaging.getInstance().addChatSessionTermination(session);
		    		break;
		    	case ChatError.SESSION_INITIATION_FAILED:
		    	case ChatError.SESSION_INITIATION_CANCELLED:
					RichMessaging.getInstance().addChatSessionTermination(session);
					RichMessaging.getInstance().markFirstMessageFailed(session.getSessionID());
		    		break;
		    	default:
					RichMessaging.getInstance().addChatSessionError(session);
		    		break;
	    	}
	    	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleImError(error.getErrorCode());
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        MessagingApiService.removeChatSession(session.getSessionID());
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
	            	listeners.getBroadcastItem(i).handleIsComposingEvent(contact, status);
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
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New conference event " + state + " for " + contact);
			}
			
			// Update rich messaging history
			RichMessaging.getInstance().addConferenceEvent(session, contact, state);
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleConferenceEvent(contact, contactDisplayname, state);
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
			RichMessaging.getInstance().setChatMessageDeliveryStatus(msgId, status);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleMessageDeliveryStatus(msgId, status);
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
     * Request to add participant is successful
     */
    public void handleAddParticipantSuccessful() {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Add participant request is successful");
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleAddParticipantSuccessful();
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
     * Request to add participant has failed
     * 
     * @param reason Error reason
     */
    public void handleAddParticipantFailed(String reason) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Add participant request has failed " + reason);
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleAddParticipantFailed(reason);
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
     * New geolocation info received
     * 
     * @param geoloc Geoloc message
     */
    public void handleReceiveGeoloc(GeolocMessage geoloc) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New geoloc message received");
			}

			// Update rich messaging history
			RichMessaging.getInstance().addIncomingGeoloc(geoloc, session);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleReceiveGeoloc(geoloc);
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
