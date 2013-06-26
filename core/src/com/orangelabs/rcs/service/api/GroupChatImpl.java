package com.orangelabs.rcs.service.api;

import java.util.List;

import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.GroupChat;
import org.gsma.joyn.chat.IGroupChat;
import org.gsma.joyn.chat.IGroupChatListener;

import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.event.User;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Group chat implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class GroupChatImpl extends IGroupChat.Stub implements ChatSessionListener {
	
	/**
	 * Core session
	 */
	private GroupChatSession session;
	
	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IGroupChatListener> listeners = new RemoteCallbackList<IGroupChatListener>();

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
	public GroupChatImpl(GroupChatSession session) {
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
	public String getChatId() {
		return session.getContributionID();
	}
	
	/**
	 * Get remote contact
	 * 
	 * @return Contact
	 */
	public String getRemoteContact() {
		return PhoneUtils.extractNumberFromUri(session.getRemoteContact());
	}
	
	/**
	 * Returns the state of the file transfer
	 * 
	 * @return State 
	 */
	public int getState() {
		// TODO
		int state = ServerApiUtils.getSessionState(session);
		switch(state) {
			case SessionState.PENDING:
				return GroupChat.State.INITIATED;
			
			case SessionState.ESTABLISHED:
				return GroupChat.State.STARTED;
			
			case SessionState.CANCELLED:
				return GroupChat.State.INITIATED;
			
			case SessionState.TERMINATED:
				return GroupChat.State.TERMINATED;

			default:
				return GroupChat.State.UNKNOWN;
		}
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
	 * Get subject associated to the session
	 * 
	 * @return String
	 */
	public String getSubject() {
		return session.getSubject();
	}

	/**
	 * Accepts chat invitation
	 */
	public void acceptInvitation() {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}
				
		// Accept invitation
		session.acceptSession();
	}
	
	/**
	 * Rejects chat invitation
	 */ 
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}

		// Update rich messaging history
		RichMessaging.getInstance().addChatSessionTermination(session);
		
        // Reject invitation
		session.rejectSession();
	}

	/**
	 * Quits a group chat conversation. The conversation will continue between
	 * other participants if there are enough participants.
	 */
	public void quitConversation() {
		if (logger.isActivated()) {
			logger.info("Cancel session");
		}
		
		// Abort the session
		session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
	}
	
	/**
	 * Returns the list of connected participants. A participant is identified
	 * by its MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
	 * 
	 * @return List of participants
	 */
	public List<String> getParticipants() {
		if (logger.isActivated()) {
			logger.info("Get list of connected participants in the session");
		}
		return session.getConnectedParticipants().getList();
	}

	/**
	 * Returns the max number of participants for a group chat from the group
	 * chat info subscription (this value overrides the provisioning parameter)
	 * 
	 * @return Number
	 */
	public int getMaxParticipants() {
        if (logger.isActivated()) {
            logger.info("Get max number of participants in the session");
        }
        return session.getMaxParticipants();
    }

	/**
	 * Adds participants to a group chat
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
	 * Sends a text message to the group
	 * 
	 * @param text Message
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
	 * Sends a “is-composing” event. The status is set to true when typing
	 * a message, else it is set to false.
	 * 
	 * @param status Is-composing status
	 */
	public void sendIsComposingEvent(boolean status) {
		session.sendIsComposingStatus(status);
	}
	
	/**
	 * Adds a listener on chat events
	 * 
	 * @param listener Group chat event listener 
	 */
	public void addEventListener(IGroupChatListener listener) {
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
	 * @param listener Group chat event listener 
	 */
	public void removeEventListener(IGroupChatListener listener) {
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
			RichMessaging.getInstance().markChatSessionStarted(session);
	    	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onSessionStarted();
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
	            	listeners.getBroadcastItem(i).onSessionTerminated(reason); // TODO: reason error
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(session.getSessionID());
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
	            	listeners.getBroadcastItem(i).onSessionTerminated(-1); // TODO
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(session.getSessionID());
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
	            	ChatMessage msg = new ChatMessage(message.getMessageId(),
	            			PhoneUtils.extractNumberFromUri(message.getRemote()),
	            			message.getTextMessage(),
	            			message.getServerDate(), message.isImdnDisplayedRequested());
	            	listeners.getBroadcastItem(i).onNewMessage(msg);
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
	            	listeners.getBroadcastItem(i).onSessionError(error.getErrorCode()); // TODO: error code
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(session.getSessionID());
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
        	contact = PhoneUtils.extractNumberFromUri(contact);

        	if (logger.isActivated()) {
				logger.info(contact + " is composing status set to " + status);
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onComposingEvent(contact, status);
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
        	contact = PhoneUtils.extractNumberFromUri(contact);

        	if (logger.isActivated()) {
				logger.info("New conference event " + state + " for " + contact);
			}
			
			// Update rich messaging history
			RichMessaging.getInstance().addConferenceEvent(session, contact, state);
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	if (state.equals(User.STATE_CONNECTED)) {
	            		listeners.getBroadcastItem(i).onParticipantJoined(contact, contactDisplayname);
	            	} else
	            	if (state.equals(User.STATE_DISCONNECTED)) {
	            		listeners.getBroadcastItem(i).onParticipantDisconnected(contact);
	            	} else
	            	if (state.equals(User.STATE_DEPARTED)) {
	            		listeners.getBroadcastItem(i).onParticipantLeft(contact);
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
     * Request to add participant is successful
     */
    public void handleAddParticipantSuccessful() {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Add participant request is successful");
			}
	
	  		// Notify event listeners
/*			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleAddParticipantSuccessful();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();*/ // TODO
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
/*			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).handleAddParticipantFailed(reason);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();*/ // TODO
	    }  
    }

    /**
     * New geoloc message received
     * 
     * @param geoloc Geoloc message
     */
    public void handleReceiveGeoloc(GeolocMessage geoloc) {
    	// Not used here
    }
}