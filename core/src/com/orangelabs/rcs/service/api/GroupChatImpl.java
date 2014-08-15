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

import static com.gsma.services.rcs.chat.ChatLog.GroupChatDeliveryInfo.DeliveryStatus.DELIVERED;
import static com.gsma.services.rcs.chat.ChatLog.GroupChatDeliveryInfo.DeliveryStatus.DISPLAYED;
import static com.gsma.services.rcs.chat.ChatLog.GroupChatDeliveryInfo.ReasonCode.DELIVERY_ERROR;
import static com.gsma.services.rcs.chat.ChatLog.GroupChatDeliveryInfo.ReasonCode.DISPLAY_ERROR;
import static com.gsma.services.rcs.chat.ChatLog.GroupChatDeliveryInfo.ReasonCode.NONE;
import static com.gsma.services.rcs.chat.ChatLog.Message.Status.Content.FAILED;
import static com.gsma.services.rcs.chat.ChatLog.Message.Status.System.DISCONNECTED;
import static com.gsma.services.rcs.chat.ChatLog.Message.Status.System.GONE;
import static com.gsma.services.rcs.chat.ChatLog.Message.Status.System.JOINED;
import static com.gsma.services.rcs.chat.GroupChat.State.ABORTED;
import static com.gsma.services.rcs.chat.GroupChat.State.CLOSED_BY_USER;
import static com.gsma.services.rcs.chat.GroupChat.State.INITIATED;
import static com.gsma.services.rcs.chat.GroupChat.State.INVITED;
import static com.gsma.services.rcs.chat.GroupChat.State.STARTED;
import static com.gsma.services.rcs.chat.GroupChat.State.TERMINATED;
import static com.gsma.services.rcs.chat.ParticipantInfo.Status.CONNECTED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Intent;
import android.util.Pair;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.chat.IGroupChat;
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
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.service.broadcaster.IGroupChatEventBroadcaster;
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

	private final IGroupChatEventBroadcaster mGroupChatEventBroadcaster;

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
	 * @param session Session
	 * @param broadcaster IGroupChatEventBroadcaster
	 */
	public GroupChatImpl(GroupChatSession session,
			IGroupChatEventBroadcaster broadcaster) {
		this.session = session;
		mGroupChatEventBroadcaster = broadcaster;
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
		if (session.isInitiatedByRemote()) {
			return GroupChat.Direction.INCOMING;
		} else {
			return GroupChat.Direction.OUTGOING;
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
				result = ABORTED;
			} else
			if (dialogPath.isSessionEstablished()) {
				// Session started
				result = STARTED;
			} else
			if (dialogPath.isSessionTerminated()) {
				// Session terminated
				result = TERMINATED;
			} else {
				// Session pending
				if ((session instanceof OriginatingAdhocGroupChatSession) ||
						(session instanceof RestartGroupChatSession) ||
						(session instanceof RejoinGroupChatSession)) {
					result = INITIATED;
				} else {
					result = INVITED;
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
			for (ContactId participant : participants) {
				handleAddParticipantFailed(participant, "Maximum number of participants reached");
			}
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

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionStarted()
     */
    public void handleSessionStarted() {
    	if (logger.isActivated()) {
			logger.info("Session started");
		}
		synchronized (lock) {
			// Update rich messaging history
			String chatId = getChatId();
			MessagingLog.getInstance().updateGroupChatRejoinIdOnSessionStart(chatId,
					session.getImSessionIdentity());
			// Notify event listeners
			mGroupChatEventBroadcaster.broadcastGroupChatStateChanged(chatId, STARTED);
		}
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionAborted(int)
     */
    public void handleSessionAborted(int reason) {
		if (logger.isActivated()) {
			logger.info("Session aborted (reason " + reason + ")");
		}
    	synchronized(lock) {
			// Update rich messaging history
			String chatId = getChatId();
			if (reason == ImsServiceSession.TERMINATION_BY_USER) {
				MessagingLog.getInstance().updateGroupChatStatus(chatId, CLOSED_BY_USER);
			} else {
				if (session.getDialogPath().isSessionCancelled()) {
					MessagingLog.getInstance().updateGroupChatStatus(chatId, ABORTED);
				} else {
					MessagingLog.getInstance().updateGroupChatStatus(chatId, TERMINATED);
				}
			}
			
	  		// Notify event listeners
			mGroupChatEventBroadcaster.broadcastGroupChatStateChanged(chatId, ABORTED);
	        
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(getChatId());
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionTerminatedByRemote()
     */
    public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session terminated by remote");
		}
    	synchronized(lock) {
			// Update rich messaging history
			String chatId = getChatId();
			if (session.getDialogPath().isSessionCancelled()) {
				MessagingLog.getInstance().updateGroupChatStatus(chatId, ABORTED);
			} else {
				MessagingLog.getInstance().updateGroupChatStatus(chatId, TERMINATED);
			}
			
	  		// Notify event listeners
			mGroupChatEventBroadcaster.broadcastGroupChatStateChanged(chatId, TERMINATED);
	        
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(getChatId());
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleReceiveMessage(com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage)
     */
    public void handleReceiveMessage(InstantMessage message) {
		if (logger.isActivated()) {
			logger.info("New IM received: "+message);
		}
    	synchronized(lock) {
			// Update rich messaging history
			MessagingLog.getInstance().addGroupChatMessage(session.getContributionID(), message,
					ChatLog.Message.Direction.INCOMING);
			// TODO : Update displayName of remote contact
			/*
			 * ContactsManager.getInstance().setContactDisplayName(session.getRemoteContact(),
			 * session.getRemoteDisplayName());
			 */
			// Broadcast intent related to the received message
			Intent intent = new Intent(GroupChatIntent.ACTION_NEW_GROUP_CHAT_MESSAGE);
			intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
			intent.putExtra(GroupChatIntent.EXTRA_MESSAGE_ID, message.getMessageId());
			AndroidFactory.getApplicationContext().sendBroadcast(intent);
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleImError(com.orangelabs.rcs.core.ims.service.im.chat.ChatError)
     */
    public void handleImError(ChatError error) {
		if (logger.isActivated()) {
			logger.info("IM error " + error.getErrorCode());
		}
    	synchronized(lock) {
			if (error.getErrorCode() == ChatError.SESSION_INITIATION_CANCELLED) {
				// Do nothing here, this is an aborted event
				return;
			}
			String chatId = getChatId();
			// Update rich messaging history
			switch(error.getErrorCode()){
	    		case ChatError.SESSION_NOT_FOUND:
	    		case ChatError.SESSION_RESTART_FAILED:
	    			// These errors are not logged
	    			break;
		    	default:
		    		MessagingLog.getInstance().updateGroupChatStatus(chatId, FAILED);
		    		break;
	    	}

			// Notify event listeners
			switch (error.getErrorCode()) {
				case ChatError.SESSION_INITIATION_DECLINED:
					// TODO : Add appropriate reason code as part of CR009
					mGroupChatEventBroadcaster.broadcastGroupChatStateChanged(chatId, FAILED/*, GroupChat.Error.INVITATION_DECLINED*/);
					break;
				case ChatError.SESSION_NOT_FOUND:
					// TODO : Add appropriate reason code as part of CR009
					mGroupChatEventBroadcaster.broadcastGroupChatStateChanged(chatId, FAILED/*, GroupChat.Error.CHAT_NOT_FOUND*/);
					break;
				default:
					// TODO : Add appropriate reason code as part of CR009
					mGroupChatEventBroadcaster.broadcastGroupChatStateChanged(chatId, FAILED /*, GroupChat.Error.CHAT_FAILED*/);
			}

	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(chatId);
	    }
    }
    
	@Override
	public void handleIsComposingEvent(ContactId contact, boolean status) {
     	if (logger.isActivated()) {
			logger.info(contact + " is composing status set to " + status);
		}
    	synchronized(lock) {
			// Notify event listeners
			mGroupChatEventBroadcaster.broadcastComposingEvent(getChatId(), contact, status);
		}
	}
	
	@Override
    public void handleConferenceEvent(ContactId contact, String contactDisplayname, String state) {
    	if (logger.isActivated()) {
			logger.info("New conference event " + state + " for " + contact);
		}
    	synchronized(lock) {
			// Update history and notify event listeners
			if (state.equals(User.STATE_CONNECTED)) {
				// Update rich messaging history
				MessagingLog.getInstance().addGroupChatSystemMessage(session.getContributionID(),
						contact, JOINED);
			} else if (state.equals(User.STATE_DISCONNECTED)) {
				// Update rich messaging history
				MessagingLog.getInstance().addGroupChatSystemMessage(session.getContributionID(),
						contact, DISCONNECTED);
			} else if (state.equals(User.STATE_DEPARTED)) {
				// Update rich messaging history
				MessagingLog.getInstance().addGroupChatSystemMessage(session.getContributionID(),
						contact, GONE);
			}
	    }
    }

    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleMessageDeliveryStatus(java.lang.String, java.lang.String)
     */
	public void handleSendMessageFailure(String msgId) {
		if (logger.isActivated()) {
			logger.info("New message failure status for message " + msgId);
		}
		synchronized (lock) {
			// Update rich messaging history
			MessagingLog.getInstance().updateChatMessageStatus(msgId, FAILED);

			// Notify event listeners
			mGroupChatEventBroadcaster.broadcastMessageStatusChanged(getChatId(), msgId, FAILED);
		}
	}

	@Override
    public void handleMessageDeliveryStatus(String msgId, String status, ContactId contact) {
		if (logger.isActivated()) {
			logger.info("New message delivery status for message " + msgId + ", status " + status);
		}
    	synchronized(lock) {
			// Update message log and notify event listeners
			String chatId = getChatId();
			MessagingLog messagingLog = MessagingLog.getInstance();
			if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
				messagingLog.updateGroupChatDeliveryInfoStatus(msgId, DELIVERED, NONE, contact);
				mGroupChatEventBroadcaster.broadcastDeliveryInfoStatusChanged(chatId, contact,
						msgId, DELIVERED, NONE);
			} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
				messagingLog.updateGroupChatDeliveryInfoStatus(msgId, DISPLAYED, NONE, contact);
				mGroupChatEventBroadcaster.broadcastDeliveryInfoStatusChanged(chatId, contact,
						msgId, DISPLAYED, NONE);
			} else if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
					|| ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
					|| ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
				int reasonCode = NONE;

				Pair<Integer, Integer> statusAndReasonCode = messagingLog
						.getGroupChatDeliveryInfoStatus(msgId, contact);
				int oldStatus = statusAndReasonCode.first;
				int oldReasonCode = statusAndReasonCode.second;
				if (DELIVERED == oldStatus
						|| (ChatLog.GroupChatDeliveryInfo.DeliveryStatus.FAILED == oldStatus && DELIVERY_ERROR == oldReasonCode)) {
					reasonCode = DISPLAY_ERROR;
				} else {
					reasonCode = DELIVERY_ERROR;
				}
				messagingLog.updateGroupChatDeliveryInfoStatus(msgId,
						ChatLog.GroupChatDeliveryInfo.DeliveryStatus.FAILED, reasonCode, contact);
				mGroupChatEventBroadcaster.broadcastDeliveryInfoStatusChanged(chatId, contact,
						msgId, ChatLog.GroupChatDeliveryInfo.DeliveryStatus.FAILED, reasonCode);
			}
			if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)
					&& messagingLog.isDeliveredToAllRecipients(msgId)) {
				messagingLog.updateOutgoingChatMessageDeliveryStatus(msgId, status);
				mGroupChatEventBroadcaster.broadcastMessageStatusChanged(chatId, msgId,ChatLog.Message.Status.Content.DELIVERED);

			} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)
					&& messagingLog.isDisplayedByAllRecipients(msgId)) {
				messagingLog.updateOutgoingChatMessageDeliveryStatus(msgId, status);
			}

		}
	}
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleAddParticipantSuccessful(com.gsma.services.rcs.contact.ContactId)
     */
	public void handleAddParticipantSuccessful(ContactId contact) {
		if (logger.isActivated()) {
			logger.info("Add participant request is successful");
		}
		synchronized (lock) {
			mGroupChatEventBroadcaster.broadcastParticipantInfoStatusChanged(getChatId(),
					new ParticipantInfo(contact, CONNECTED));
		}
	}

    /**
     * Request to add participant has failed
     *
     * @param contact Contact ID
     * @param reason Error reason
     */
	public void handleAddParticipantFailed(ContactId contact, String reason) {
		if (logger.isActivated()) {
			logger.info("Add participant request has failed " + reason);
		}
		synchronized (lock) {
			mGroupChatEventBroadcaster.broadcastParticipantInfoStatusChanged(getChatId(),
					new ParticipantInfo(contact, ParticipantInfo.Status.FAILED));
		}
	}

    /**
     * New geoloc message received
     * 
     * @param geoloc Geoloc message
     */
    public void handleReceiveGeoloc(GeolocMessage geoloc) {
		if (logger.isActivated()) {
			logger.info("New geoloc received");
		}
    	synchronized(lock) {
			// Update rich messaging history
			MessagingLog.getInstance().addGroupChatMessage(session.getContributionID(),
					geoloc, ChatLog.Message.Direction.INCOMING);
			// TODO : Update displayName of remote contact
			/*
			 * ContactsManager.getInstance().setContactDisplayName(session.getRemoteContact(),
			 * session.getRemoteDisplayName());
			 */
			// Broadcast intent related to the received message
			Intent intent = new Intent(GroupChatIntent.ACTION_NEW_GROUP_CHAT_MESSAGE);
			intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
			intent.putExtra(GroupChatIntent.EXTRA_MESSAGE_ID, geoloc.getMessageId());
			AndroidFactory.getApplicationContext().sendBroadcast(intent);
	    }
    }

    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleParticipantStatusChanged(com.gsma.services.rcs.chat.ParticipantInfo)
     */
	public void handleParticipantStatusChanged(ParticipantInfo participantInfo) {
		if (logger.isActivated()) {
			logger.info("handleParticipantStatusChanged ParticipantInfo [contact=" + participantInfo.getContact() + ", status="
					+ participantInfo.getStatus() + "]");
		}
		synchronized (lock) {
			mGroupChatEventBroadcaster.broadcastParticipantInfoStatusChanged(getChatId(),
					participantInfo);
		}
	}

}
