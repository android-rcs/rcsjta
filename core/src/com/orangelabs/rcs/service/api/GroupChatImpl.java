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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax2.sip.message.Response;

import com.gsma.services.rcs.GroupDeliveryInfoLog;
import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.event.User;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.messaging.GroupChatStateAndReasonCode;
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

	private GroupChatStateAndReasonCode toStateAndReasonCode(ChatError error) {
		switch (error.getErrorCode()) {
			case ChatError.SESSION_INITIATION_CANCELLED:
			case ChatError.SESSION_INITIATION_DECLINED:
				return new GroupChatStateAndReasonCode(GroupChat.State.REJECTED, GroupChat.ReasonCode.REJECTED_BY_REMOTE);
			case ChatError.SESSION_INITIATION_FAILED:
			case ChatError.SESSION_NOT_FOUND:
			case ChatError.SESSION_RESTART_FAILED:
			case ChatError.SUBSCRIBE_CONFERENCE_FAILED:
			case ChatError.UNEXPECTED_EXCEPTION:
				return new GroupChatStateAndReasonCode(GroupChat.State.FAILED, GroupChat.ReasonCode.FAILED_INITIATION);
			case ChatError.MEDIA_SESSION_BROKEN:
			case ChatError.MEDIA_SESSION_FAILED:
				return new GroupChatStateAndReasonCode(GroupChat.State.ABORTED,
						GroupChat.ReasonCode.ABORTED_BY_SYSTEM);
			default:
				throw new IllegalArgumentException(
						"Unknown reason in GroupChatImpl.toStateAndReasonCode; error="
								+ error + "!");
		}
	}

    private int imdnToMessageFailedReasonCode(ImdnDocument imdn) {
        String notificationType = imdn.getNotificationType();
        if (ImdnDocument.DELIVERY_NOTIFICATION.equals(notificationType)) {
            return ChatLog.Message.ReasonCode.FAILED_DELIVERY;

        } else if (ImdnDocument.DISPLAY_NOTIFICATION.equals(notificationType)) {
            return ChatLog.Message.ReasonCode.FAILED_DISPLAY;
        }
        throw new IllegalArgumentException(new StringBuilder(
                "Received invalid imdn notification type:'").append(notificationType).append("'")
                .toString());
    }

	private int sessionAbortedReasonToReasonCode(int reason) {
		switch (reason) {
			case ImsServiceSession.TERMINATION_BY_SYSTEM:
			case ImsServiceSession.TERMINATION_BY_TIMEOUT:
				return GroupChat.ReasonCode.ABORTED_BY_SYSTEM;
			case ImsServiceSession.TERMINATION_BY_USER:
				return GroupChat.ReasonCode.ABORTED_BY_USER;
			default:
				throw new IllegalArgumentException(
						"Unknown reason in GroupChatImpl.sessionAbortedReasonToReasonCode; reason="
								+ reason + "!");
		}
	}

	private void handleSessionRejected(int reasonCode) {
		String chatId = getChatId();
		synchronized (lock) {
			ChatServiceImpl.removeGroupChatSession(chatId);

			MessagingLog.getInstance().updateGroupChatStateAndReasonCode(chatId,
					GroupChat.State.REJECTED, reasonCode);

			mGroupChatEventBroadcaster.broadcastStateChanged(chatId,
					GroupChat.State.REJECTED, reasonCode);
		}
	}

    private void handleMessageDeliveryStatusDelivered(ContactId contact, String msgId) {
        String chatId = getChatId();
        MessagingLog messagingLog = MessagingLog.getInstance();
        synchronized (lock) {
            messagingLog.updateGroupChatDeliveryInfoStatusAndReasonCode(msgId, contact,
                    GroupDeliveryInfoLog.Status.DELIVERED,
                    GroupDeliveryInfoLog.ReasonCode.UNSPECIFIED);
            mGroupChatEventBroadcaster.broadcastMessageGroupDeliveryInfoChanged(chatId, contact,
                    msgId, GroupDeliveryInfoLog.Status.DELIVERED,
                    GroupDeliveryInfoLog.ReasonCode.UNSPECIFIED);
            if (messagingLog.isDeliveredToAllRecipients(msgId)) {
                messagingLog.updateChatMessageStatusAndReasonCode(msgId,
                        ChatLog.Message.Status.Content.DELIVERED,
                        ChatLog.Message.ReasonCode.UNSPECIFIED);
                mGroupChatEventBroadcaster.broadcastMessageStatusChanged(chatId, msgId,
                        ChatLog.Message.Status.Content.DELIVERED,
                        ChatLog.Message.ReasonCode.UNSPECIFIED);
            }
        }
    }

    private void handleMessageDeliveryStatusDisplayed(ContactId contact, String msgId) {
        String chatId = getChatId();
        MessagingLog messagingLog = MessagingLog.getInstance();
        synchronized (lock) {
            messagingLog.updateGroupChatDeliveryInfoStatusAndReasonCode(msgId, contact,
                    GroupDeliveryInfoLog.Status.DISPLAYED,
                    GroupDeliveryInfoLog.ReasonCode.UNSPECIFIED);
            mGroupChatEventBroadcaster.broadcastMessageGroupDeliveryInfoChanged(chatId, contact,
                    msgId, GroupDeliveryInfoLog.Status.DISPLAYED,
                    GroupDeliveryInfoLog.ReasonCode.UNSPECIFIED);
            if (messagingLog.isDisplayedByAllRecipients(msgId)) {
                messagingLog.updateChatMessageStatusAndReasonCode(msgId,
                        ChatLog.Message.Status.Content.DISPLAYED,
                        ChatLog.Message.ReasonCode.UNSPECIFIED);
                mGroupChatEventBroadcaster.broadcastMessageStatusChanged(chatId, msgId,
                        ChatLog.Message.Status.Content.DISPLAYED,
                        ChatLog.Message.ReasonCode.UNSPECIFIED);
            }
        }
    }

    private void handleMessageDeliveryStatusFailed(ContactId contact, String msgId, int reasonCode) {
        String chatId = getChatId();
        MessagingLog messagingLog = MessagingLog.getInstance();
        synchronized (lock) {
            if (ChatLog.Message.ReasonCode.FAILED_DELIVERY == reasonCode) {
                messagingLog.updateGroupChatDeliveryInfoStatusAndReasonCode(msgId, contact,
                        GroupDeliveryInfoLog.Status.FAILED,
                        GroupDeliveryInfoLog.ReasonCode.FAILED_DELIVERY);
                mGroupChatEventBroadcaster.broadcastMessageGroupDeliveryInfoChanged(chatId,
                        contact, msgId, GroupDeliveryInfoLog.Status.FAILED,
                        GroupDeliveryInfoLog.ReasonCode.FAILED_DELIVERY);
            } else {
                messagingLog.updateGroupChatDeliveryInfoStatusAndReasonCode(msgId, contact,
                        GroupDeliveryInfoLog.Status.FAILED,
                        GroupDeliveryInfoLog.ReasonCode.FAILED_DISPLAY);
                mGroupChatEventBroadcaster.broadcastMessageGroupDeliveryInfoChanged(chatId,
                        contact, msgId, GroupDeliveryInfoLog.Status.FAILED,
                        GroupDeliveryInfoLog.ReasonCode.FAILED_DISPLAY);
            }
        }
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
			return Direction.INCOMING;
		} else {
			return Direction.OUTGOING;
		}
	}		
	
	/**
	 * Returns the state of the group chat
	 * 
	 * @return State
	 */
	public int getState() {
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null && dialogPath.isSessionEstablished()) {
				return GroupChat.State.STARTED;

		} else if (session.isInitiatedByRemote()) {
			if (session.isSessionAccepted()) {
				return GroupChat.State.ACCEPTING;
			}

			return GroupChat.State.INVITED;
		}

		return GroupChat.State.INITIATED;
	}

	/**
	 * Returns the reason code of the state of the group chat
	 *
	 * @return ReasonCode
	 */
	public int getReasonCode() {
		return GroupChat.ReasonCode.UNSPECIFIED;
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
		
        // Reject invitation
        new Thread() {
    		public void run() {
    			session.rejectSession(Response.DECLINE);
    		}
    	}.start();
	}

	/**
	 * Quits a group chat conversation. The conversation will continue between
	 * other participants if there are enough participants.
	 */
	public void leave() {
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
			case ParticipantInfo.Status.DEPARTED:
			case ParticipantInfo.Status.DECLINED:
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
			StringBuilder listOfParticipants = new StringBuilder("Add ");
			for (ContactId contactId : participants) {
				listOfParticipants.append(contactId.toString()).append(" ");
			}
			listOfParticipants.append("participants to the session");
			logger.info(listOfParticipants.toString());
		}

		int max = session.getMaxParticipants() - 1;
		// PDD 6.3.5.9 Adding participants to a Group Chat (Clarification)
		// For the maximum user count, the rcs client shall take into account both the active and inactive users,
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
	 * @return Chat message
	 */
	public ChatMessage sendMessage(final String text) {
		final String msgId = IdGenerator.generateMessageID();

		// Send text message
        new Thread() {
    		public void run() {
    			session.sendTextMessage(msgId, text);
    		}
    	}.start();
    	/* TODO: Return a ChatMessage with correct time-stamps in CR018. */
    	return new ChatMessage(msgId, null, text, 0, 0);
	}
	
	/**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc
     * @return Geoloc message
     */
    public com.gsma.services.rcs.chat.GeolocMessage sendMessage2(Geoloc geoloc) {
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
    	/* TODO: Return a GeolocMessage with correct time-stamps in CR018. */
    	return new com.gsma.services.rcs.chat.GeolocMessage(msgId, null, geoloc, 0, 0);
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
		String chatId = getChatId();
		synchronized (lock) {
			MessagingLog.getInstance().updateGroupChatRejoinIdOnSessionStart(chatId,
					session.getImSessionIdentity());

			mGroupChatEventBroadcaster.broadcastStateChanged(chatId, GroupChat.State.STARTED,
					GroupChat.ReasonCode.UNSPECIFIED);
		}
    }
    
	/*
	 * (non-Javadoc)
	 * @see
	 * com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionAborted
	 * (int)
	 */
	public void handleSessionAborted(int reason) {
		if (logger.isActivated()) {
			logger.info("Session aborted (reason " + reason + ")");
		}
		String chatId = getChatId();
		synchronized (lock) {
			ChatServiceImpl.removeGroupChatSession(chatId);

			int reasonCode = sessionAbortedReasonToReasonCode(reason);
			MessagingLog.getInstance().updateGroupChatStateAndReasonCode(chatId,
					GroupChat.State.ABORTED, reasonCode);

			mGroupChatEventBroadcaster.broadcastStateChanged(chatId,
					GroupChat.State.ABORTED, reasonCode);
		}
	}
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionTerminatedByRemote()
     */
	public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session terminated by remote");
		}
		String chatId = getChatId();
		synchronized (lock) {
			ChatServiceImpl.removeGroupChatSession(chatId);

			MessagingLog.getInstance().updateGroupChatStateAndReasonCode(chatId,
					GroupChat.State.ABORTED, GroupChat.ReasonCode.ABORTED_BY_REMOTE);

				mGroupChatEventBroadcaster.broadcastStateChanged(chatId,
						GroupChat.State.ABORTED, GroupChat.ReasonCode.ABORTED_BY_REMOTE);
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
					Direction.INCOMING, ChatLog.Message.Status.Content.RECEIVED,
					ChatLog.Message.ReasonCode.UNSPECIFIED);
			// Update displayName of remote contact
			 ContactsManager.getInstance().setContactDisplayName(message.getRemote(), message.getDisplayName());

			 mGroupChatEventBroadcaster.broadcastMessageReceived(message.getMessageId());
	    }
    }

	/*
	 * (non-Javadoc)
	 * @see
	 * com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleImError
	 * (com.orangelabs.rcs.core.ims.service.im.chat.ChatError)
	 */
	public void handleImError(ChatError error) {
		if (logger.isActivated()) {
			logger.info("IM error " + error.getErrorCode());
		}
		String chatId = getChatId();
		synchronized (lock) {
			ChatServiceImpl.removeGroupChatSession(chatId);

			if (error.getErrorCode() != ChatError.SESSION_NOT_FOUND
					&& error.getErrorCode() != ChatError.SESSION_RESTART_FAILED) {
				GroupChatStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
				int state = stateAndReasonCode.getState();
				int reasonCode = stateAndReasonCode.getReasonCode();
				MessagingLog.getInstance().updateGroupChatStateAndReasonCode(getChatId(), state,
						reasonCode);

				mGroupChatEventBroadcaster.broadcastStateChanged(chatId, state,
						reasonCode);
			}
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


	/*
	 * (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#
	 * handleMessageSending(
	 * com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage)
	 */
	@Override
	public void handleMessageSending(InstantMessage msg) {
		String msgId = msg.getMessageId();
		if (logger.isActivated()) {
			logger.info(new StringBuilder("Inserting message with status sending; id=").append(
					msgId).toString());
		}
		String chatId = getChatId();
		synchronized (lock) {
			MessagingLog.getInstance().addGroupChatMessage(chatId, msg, Direction.OUTGOING,
					ChatLog.Message.Status.Content.SENDING, ChatLog.Message.ReasonCode.UNSPECIFIED);
			mGroupChatEventBroadcaster.broadcastMessageStatusChanged(chatId, msgId,
					ChatLog.Message.Status.Content.SENDING, ChatLog.Message.ReasonCode.UNSPECIFIED);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#
	 * handleMessageFailedSend(java.lang.String)
	 */
	@Override
	public void handleMessageFailedSend(String msgId) {
		if (logger.isActivated()) {
			logger.info("Handle message send failure;msgId=".concat(msgId));
		}
		synchronized (lock) {
			MessagingLog.getInstance().updateChatMessageStatusAndReasonCode(msgId,
					ChatLog.Message.Status.Content.FAILED, ChatLog.Message.ReasonCode.FAILED_SEND);

			mGroupChatEventBroadcaster.broadcastMessageStatusChanged(getChatId(), msgId,
					ChatLog.Message.Status.Content.FAILED, ChatLog.Message.ReasonCode.FAILED_SEND);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#
	 * handleMessageSent(java.lang.String)
	 */
	@Override
	public void handleMessageSent(String msgId) {
		if (logger.isActivated()) {
			logger.info("Handle message sent; msgId=".concat(msgId));
		}
		synchronized (lock) {
			MessagingLog.getInstance().updateChatMessageStatusAndReasonCode(msgId,
					ChatLog.Message.Status.Content.SENT, ChatLog.Message.ReasonCode.UNSPECIFIED);

			mGroupChatEventBroadcaster.broadcastMessageStatusChanged(getChatId(), msgId,
					ChatLog.Message.Status.Content.SENT, ChatLog.Message.ReasonCode.UNSPECIFIED);
		}
	}

	@Override
    public void handleConferenceEvent(ContactId contact, String contactDisplayname, String state) {
    	if (logger.isActivated()) {
			logger.info("New conference event " + state + " for " + contact);
		}
		String chatId = getChatId();
    	synchronized(lock) {
			if (User.STATE_CONNECTED.equals(state)) {
				MessagingLog.getInstance().addGroupChatSystemMessage(session.getContributionID(),
						contact, Message.Status.System.JOINED);
				mGroupChatEventBroadcaster.broadcastParticipantInfoStatusChanged(chatId,
						new ParticipantInfo(contact, ParticipantInfo.Status.CONNECTED));

			} else if (User.STATE_DISCONNECTED.equals(state)) {
				MessagingLog.getInstance().addGroupChatSystemMessage(session.getContributionID(),
						contact, Message.Status.System.DISCONNECTED);

				mGroupChatEventBroadcaster.broadcastParticipantInfoStatusChanged(chatId,
						new ParticipantInfo(contact, ParticipantInfo.Status.DISCONNECTED));

			} else if (User.STATE_DEPARTED.equals(state)) {
				MessagingLog.getInstance().addGroupChatSystemMessage(session.getContributionID(),
						contact, Message.Status.System.GONE);

				mGroupChatEventBroadcaster.broadcastParticipantInfoStatusChanged(chatId,
						new ParticipantInfo(contact, ParticipantInfo.Status.DEPARTED));
			}
	    }
    }

    @Override
    public void handleMessageDeliveryStatus(ContactId contact, ImdnDocument imdn) {
        String status = imdn.getStatus();
        String msgId = imdn.getMsgId();
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Handling message delivery status; contact=")
                    .append(contact).append(", msgId=").append(msgId).append(", status=")
                    .append(status).append(", notificationType=")
                    .append(imdn.getNotificationType()).toString());
        }
        if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
            handleMessageDeliveryStatusDelivered(contact, msgId);
        } else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
            handleMessageDeliveryStatusDisplayed(contact, msgId);
        } else if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
            int reasonCode = imdnToMessageFailedReasonCode(imdn);
            handleMessageDeliveryStatusFailed(contact, msgId, reasonCode);
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
					new ParticipantInfo(contact, ParticipantInfo.Status.CONNECTED));
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
			MessagingLog.getInstance().addGroupChatMessage(session.getContributionID(), geoloc,
					Direction.INCOMING, ChatLog.Message.Status.Content.RECEIVED,
					ChatLog.Message.ReasonCode.UNSPECIFIED);

			// Update displayName of remote contact
			ContactsManager.getInstance().setContactDisplayName(geoloc.getRemote(), geoloc.getDisplayName());

			 mGroupChatEventBroadcaster.broadcastMessageReceived(geoloc.getMessageId());
	    }
    }

    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleParticipantStatusChanged(com.gsma.services.rcs.chat.ParticipantInfo)
     */
	public void handleParticipantStatusChanged(ParticipantInfo participantInfo) {
		if (logger.isActivated()) {
			logger.info("handleParticipantStatusChanged " + participantInfo);
		}
		synchronized (lock) {
			mGroupChatEventBroadcaster.broadcastParticipantInfoStatusChanged(getChatId(),
					participantInfo);
		}
	}

	@Override
	public void handleSessionAccepted() {
		if (logger.isActivated()) {
			logger.info("Accepting group chat session");
		}
		String chatId = getChatId();
		synchronized (lock) {
			MessagingLog.getInstance().updateGroupChatStateAndReasonCode(chatId,
					GroupChat.State.ACCEPTING, GroupChat.ReasonCode.UNSPECIFIED);

			mGroupChatEventBroadcaster.broadcastStateChanged(chatId,
					GroupChat.State.ACCEPTING, GroupChat.ReasonCode.UNSPECIFIED);
		}
	}

	@Override
	public void handleSessionRejectedByUser() {
		if (logger.isActivated()) {
			logger.info("Session rejected by user");
		}
		handleSessionRejected(GroupChat.ReasonCode.REJECTED_BY_USER);
	}

	@Override
	public void handleSessionRejectedByTimeout() {
		if (logger.isActivated()) {
			logger.info("Session rejected by time out");
		}
		handleSessionRejected(GroupChat.ReasonCode.REJECTED_TIME_OUT);
	}

	@Override
	public void handleSessionRejectedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session rejected by time out");
		}
		handleSessionRejected(GroupChat.ReasonCode.REJECTED_BY_REMOTE);
	}

	@Override
	public void handleSessionInvited() {
		if (logger.isActivated()) {
			logger.info("Invited to group chat session");
		}
		String chatId = getChatId();
		synchronized (lock) {
			MessagingLog.getInstance().addGroupChat(chatId, session.getRemoteContact(),
					getSubject(), session.getParticipants(), GroupChat.State.INVITED,
					GroupChat.ReasonCode.UNSPECIFIED, Direction.INCOMING);
		}

		mGroupChatEventBroadcaster.broadcastInvitation(chatId);
	}

	@Override
	public void handleSessionAutoAccepted() {
		if (logger.isActivated()) {
			logger.info("Session auto accepted");
		}
		String chatId = getChatId();
		synchronized (lock) {
			MessagingLog.getInstance().addGroupChat(chatId, session.getRemoteContact(),
					getSubject(), session.getParticipants(), GroupChat.State.ACCEPTING,
					GroupChat.ReasonCode.UNSPECIFIED, Direction.INCOMING);
		}

		mGroupChatEventBroadcaster.broadcastInvitation(chatId);
	}
}
