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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.os.RemoteCallbackList;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.chat.ParticipantInfo.Status;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.OriginatingAdhocGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.RejoinGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.RestartGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.event.User;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.utils.IdGenerator;
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
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(GroupChatImpl.class.getSimpleName());

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
	 * Get chat ID
	 * 
	 * @return Chat ID
	 */
	public String getChatId() {
		return session.getContributionID();
	}
	
	/**
	 * Get remote contact identifier
	 * 
	 * @return ContactId
	 */
	public ContactId getRemoteContact() {
		return session.getRemoteContact();
	}
	
	/**
	 * Returns the direction of the group chat (incoming or outgoing)
	 * 
	 * @return Direction
	 */
	public int getDirection() {
		if ((session instanceof OriginatingAdhocGroupChatSession) ||
				(session instanceof RejoinGroupChatSession) ||
					(session instanceof RestartGroupChatSession)) {
			return GroupChat.Direction.OUTGOING;
		} else {
			return GroupChat.Direction.INCOMING;
		}
	}		
	
	/**
	 * Returns the state of the group chat
	 * 
	 * @return State 
	 */
	public int getState() {
		int result = GroupChat.State.INACTIVE;
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null) {
			if (dialogPath.isSessionCancelled()) {
				// Session canceled
				result = GroupChat.State.ABORTED;
			} else
			if (dialogPath.isSessionEstablished()) {
				// Session started
				result = GroupChat.State.STARTED;
			} else
			if (dialogPath.isSessionTerminated()) {
				// Session terminated
				result = GroupChat.State.TERMINATED;
			} else {
				// Session pending
				if ((session instanceof OriginatingAdhocGroupChatSession) ||
						(session instanceof RestartGroupChatSession) ||
						(session instanceof RejoinGroupChatSession)) {
					result = GroupChat.State.INITIATED;
				} else {
					result = GroupChat.State.INVITED;
				}
			}
		}
		return result;			
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
        new Thread() {
    		public void run() {
    			session.acceptSession();
    		}
    	}.start();
	}
	
	/**
	 * Rejects chat invitation
	 */ 
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}

		// Update rich messaging history
		MessagingLog.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.ABORTED);
		
        // Reject invitation
        new Thread() {
    		public void run() {
    			session.rejectSession(603);
    		}
    	}.start();
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
        new Thread() {
    		public void run() {
    			session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
    		}
    	}.start();
	}
	
	/**
	 * Returns the list of participants. A participant is identified
	 * by its MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
	 * 
	 * @return List of participants
	 */
	public List<ParticipantInfo> getParticipants() {
		List<ParticipantInfo> result = new ArrayList<ParticipantInfo>();
		if (session.getConnectedParticipants() == null || session.getConnectedParticipants().size()==0)
			return result;
		return new ArrayList<ParticipantInfo>(session.getConnectedParticipants());
	}
	
	/**
	 * Returns the max number of participants for a group chat from the group
	 * chat info subscription (this value overrides the provisioning parameter)
	 * 
	 * @return Number
	 */
	public int getMaxParticipants() {
        return session.getMaxParticipants();
    }

	/**
	 * Calculate the number of participants who did not decline or left the Group chat.
	 * 
	 * @param setOfParticipant
	 *            the set of participant information
	 * @return the number of participants who did not decline or left the Group chat.
	 */
	private static int getNumberOfParticipants(final Set<ParticipantInfo> participants) {
		int result = 0;
		for (ParticipantInfo participant : participants) {
			switch (participant.getStatus()) {
			case Status.DEPARTED:
			case Status.DECLINED:
				break;
			default:
				result++;
			}
		}
		return result;
	}
	
	/**
	 * Adds participants to a group chat
	 * 
	 * @param participants Set of participants
	 */
	public void addParticipants(final List<ContactId> participants) {
		if (logger.isActivated()) {
			logger.info("Add " + Arrays.toString(participants.toArray()) + " participants to the session");
		}

		int max = session.getMaxParticipants() - 1;
		// PDD 6.3.5.9 Adding participants to a Group Chat (Clarification)
		// For the maximum user count, the joyn client shall take into account both the active and inactive users,
		// but not those that have explicitly left or declined the Chat.
		int connected = getNumberOfParticipants(session.getConnectedParticipants());
		if (connected < max) {
			// Add a list of participants to the session
			new Thread() {
				public void run() {
					session.addParticipants(new HashSet<ContactId>(participants));
				}
			}.start();
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
	public String sendMessage(final String text) {
		// Generate a message Id
		final String msgId = IdGenerator.generateMessageID();

		// Send text message
        new Thread() {
    		public void run() {
    			session.sendTextMessage(msgId, text);
    		}
    	}.start();
		return msgId;
	}
	
	/**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc
     * @return Unique message ID or null in case of error
     */
    public String sendGeoloc(Geoloc geoloc) {
		// Generate a message Id
		final String msgId = IdGenerator.generateMessageID();

		// Send geoloc message
		final GeolocPush geolocPush = new GeolocPush(geoloc.getLabel(),
				geoloc.getLatitude(), geoloc.getLongitude(),
				geoloc.getExpiration(), geoloc.getAccuracy());
        new Thread() {
    		public void run() {
    			session.sendGeolocMessage(msgId, geolocPush);
    		}
    	}.start();
		return msgId;
    }	

    /**
	 * Sends a is-composing event. The status is set to true when typing
	 * a message, else it is set to false.
	 * 
	 * @param status Is-composing status
	 */
	public void sendIsComposingEvent(final boolean status) {
        new Thread() {
    		public void run() {
    			session.sendIsComposingStatus(status);
    		}
    	}.start();
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
	
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionStarted()
     */
    public void handleSessionStarted() {
    	synchronized(lock) {
	    	if (logger.isActivated()) {
				logger.info("Session started");
			}

			// Update rich messaging history
	    	MessagingLog.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.STARTED);
	    	MessagingLog.getInstance().updateGroupChatRejoinId(getChatId(), session.getImSessionIdentity());
			
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
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionAborted(int)
     */
    public void handleSessionAborted(int reason) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session aborted (reason " + reason + ")");
			}
	
			// Update rich messaging history
			if (reason == ImsServiceSession.TERMINATION_BY_USER) {
				MessagingLog.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.CLOSED_BY_USER);
			} else {
				if (session.getDialogPath().isSessionCancelled()) {
					MessagingLog.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.ABORTED);
				} else {
					MessagingLog.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.TERMINATED);
				}
			}
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onSessionAborted();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(getChatId());
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionTerminatedByRemote()
     */
    public void handleSessionTerminatedByRemote() {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session terminated by remote");
			}
	
			// Update rich messaging history
			if (session.getDialogPath().isSessionCancelled()) {
				MessagingLog.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.ABORTED);
			} else {
				MessagingLog.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.TERMINATED);
			}
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onSessionAborted();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(getChatId());
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleReceiveMessage(com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage)
     */
    public void handleReceiveMessage(InstantMessage message) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New IM received: "+message);
			}
			
			// Update rich messaging history
			MessagingLog.getInstance().addGroupChatMessage(session.getContributionID(),
					message, ChatLog.Message.Direction.INCOMING);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	ChatMessage msgApi = new ChatMessage(message.getMessageId(),
	            			message.getRemote(),
	            			message.getTextMessage(),
	            			message.getServerDate());
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
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleImError(com.orangelabs.rcs.core.ims.service.im.chat.ChatError)
     */
    public void handleImError(ChatError error) {
    	synchronized(lock) {
			if (error.getErrorCode() == ChatError.SESSION_INITIATION_CANCELLED) {
				// Do nothing here, this is an aborted event
				return;
			}
    		
			if (logger.isActivated()) {
				logger.info("IM error " + error.getErrorCode());
			}
			
			// Update rich messaging history
			switch(error.getErrorCode()){
	    		case ChatError.SESSION_NOT_FOUND:
	    		case ChatError.SESSION_RESTART_FAILED:
	    			// These errors are not logged
	    			break;
		    	default:
		    		MessagingLog.getInstance().updateGroupChatStatus(session.getContributionID(), GroupChat.State.FAILED);
		    		break;
	    	}
	    	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	int code;
	            	switch(error.getErrorCode()) {
            			case ChatError.SESSION_INITIATION_DECLINED:
	            			code = GroupChat.Error.INVITATION_DECLINED;
	            			break;
            			case ChatError.SESSION_NOT_FOUND:
	            			code = GroupChat.Error.CHAT_NOT_FOUND;
	            			break;
	            		default:
	            			code = GroupChat.Error.CHAT_FAILED;
	            	}
	            	listeners.getBroadcastItem(i).onSessionError(code);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    	
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(getChatId());
	    }
    }
    
    @Override
	public void handleIsComposingEvent(ContactId contactId, boolean status) {
    	synchronized(lock) {
        	if (logger.isActivated()) {
				logger.info(contactId + " is composing status set to " + status);
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onComposingEvent(contactId, status);
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
    public void handleConferenceEvent(ContactId contactId, String contactDisplayname, String state) {
    	synchronized(lock) {
        	if (logger.isActivated()) {
				logger.info("New conference event " + state + " for " + contactId);
			}
			
	  		// Update history and notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	if (state.equals(User.STATE_CONNECTED)) {
	        			// Update rich messaging history
	            		MessagingLog.getInstance().addGroupChatSystemMessage(session.getContributionID(), contactId, ChatLog.Message.Status.System.JOINED);

	        	  		// Notify event listener
	        			listeners.getBroadcastItem(i).onParticipantJoined(contactId, contactDisplayname);
	            	} else
	            	if (state.equals(User.STATE_DISCONNECTED)) {
	        			// Update rich messaging history
	            		MessagingLog.getInstance().addGroupChatSystemMessage(session.getContributionID(), contactId, ChatLog.Message.Status.System.DISCONNECTED);

	        	  		// Notify event listener
	        			listeners.getBroadcastItem(i).onParticipantDisconnected(contactId);
	            	} else
	            	if (state.equals(User.STATE_DEPARTED)) {
	        			// Update rich messaging history
	            		MessagingLog.getInstance().addGroupChatSystemMessage(session.getContributionID(), contactId, ChatLog.Message.Status.System.GONE);

	        	  		// Notify event listener
	        			listeners.getBroadcastItem(i).onParticipantLeft(contactId);
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
    public void handleMessageDeliveryStatus(String msgId, String status, ContactId contactId) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New message delivery status for message " + msgId + ", status " + status);
			}
	
			// Update rich messaging history
			MessagingLog messagingLog = MessagingLog.getInstance();
			messagingLog.updateGroupChatDeliveryInfoStatus(msgId, status, contactId);
			// TODO : Listeners to notify group file delivery status for
			// individual contacts will be implemented as part of CR011. For now,
			// the same callback is used for sending both per contact group delivery
			// status and for the whole group message delivery status.
			// Notify event listeners
			final int N = listeners.beginBroadcast();
			for (int i = 0; i < N; i++) {
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
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't notify listener", e);
					}
				}
			}
			listeners.finishBroadcast();
			if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)
					&& messagingLog.isDeliveredToAllRecipients(msgId)) {
				messagingLog.updateOutgoingChatMessageDeliveryStatus(msgId, status);
				final int P = listeners.beginBroadcast();
				for (int i = 0; i < P; i++) {
					try {
						listeners.getBroadcastItem(i).onReportMessageDelivered(msgId);
					} catch (Exception e) {
						if (logger.isActivated()) {
							logger.error("Can't notify listener", e);
						}
					}
				}
				listeners.finishBroadcast();

			} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)
					&& messagingLog.isDisplayedByAllRecipients(msgId)) {
				messagingLog.updateOutgoingChatMessageDeliveryStatus(msgId, status);
				final int Q = listeners.beginBroadcast();
				for (int i = 0; i < Q; i++) {
					try {
						listeners.getBroadcastItem(i).onReportMessageDisplayed(msgId);
					} catch (Exception e) {
						if (logger.isActivated()) {
							logger.error("Can't notify listener", e);
						}
					}
				}
				listeners.finishBroadcast();
			}

		}
	}
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleAddParticipantSuccessful()
     */
    public void handleAddParticipantSuccessful() {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Add participant request is successful");
			}
	
			// TODO: nothing send over API?
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
	
			// TODO: nothing send over API?
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
			MessagingLog.getInstance().addGroupChatMessage(session.getContributionID(),
					geoloc, ChatLog.Message.Direction.INCOMING);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	Geoloc geolocApi = new Geoloc(geoloc.getGeoloc().getLabel(),
	            			geoloc.getGeoloc().getLatitude(), geoloc.getGeoloc().getLongitude(),
	            			geoloc.getGeoloc().getExpiration());
	            	com.gsma.services.rcs.chat.GeolocMessage msgApi = new com.gsma.services.rcs.chat.GeolocMessage(geoloc.getMessageId(),
	            			geoloc.getRemote(),
	            			geolocApi, geoloc.getDate());
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
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleParticipantStatusChanged(com.gsma.services.rcs.chat.ParticipantInfo)
     */
    public void handleParticipantStatusChanged(ParticipantInfo participantInfo)
    {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("handleParticipantStatusChanged "+participantInfo);
			}
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onParticipantStatusChanged(participantInfo);
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
