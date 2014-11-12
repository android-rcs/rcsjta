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

import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.IOneToOneChat;
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
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.service.broadcaster.IOneToOneChatEventBroadcaster;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * One-to-One Chat implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class OneToOneChatImpl extends IOneToOneChat.Stub implements ChatSessionListener {
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
	public OneToOneChatImpl(ContactId contact, OneOneChatSession session,
			IOneToOneChatEventBroadcaster broadcaster) {
		this.contact = contact;
		this.session = session;
		mChatEventBroadcaster = broadcaster;
		if (session != null)  {
			session.addListener(this);
		}
	}

	private int imdnToFailedReasonCode(ImdnDocument imdn) {
		String notificationType = imdn.getNotificationType();
		if (ImdnDocument.DELIVERY_NOTIFICATION.equals(notificationType)) {
			return ReasonCode.FAILED_DELIVERY;

		} else if (ImdnDocument.DISPLAY_NOTIFICATION.equals(notificationType)) {
			return ReasonCode.FAILED_DISPLAY;
		}

		throw new IllegalArgumentException(new StringBuilder(
				"Received invalid imdn notification type:'").append(notificationType).append("'")
				.toString());
	}

	/**
	 * Constructor
	 *
	 * @param contact Remote contact
	 * @param chatEventBroadcaster IChatEventBroadcaster
	 */
	public OneToOneChatImpl(ContactId contact, IOneToOneChatEventBroadcaster chatEventBroadcaster) {
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
     * @return Chat message
     */
    public ChatMessage sendMessage(String message) {
		if (logger.isActivated()) {
			logger.debug("Send text message");
		}

    	InstantMessage msg = ChatUtils.createTextMessage(contact, message,
    			Core.getInstance().getImService().getImdnManager().isImdnActivated());

	    String msgId = sendChatMessage(msg);
        /* TODO: Return a ChatMessage with correct time-stamps in CR018. */
        return new ChatMessage(msgId, contact, message, 0, 0);
    }
    
	/**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc
     * @return Geoloc message
     */
    public com.gsma.services.rcs.chat.GeolocMessage sendMessage2(Geoloc geoloc) {
		if (logger.isActivated()) {
			logger.debug("Send geoloc message");
		}

		GeolocPush geolocPush = new GeolocPush(geoloc.getLabel(),
				geoloc.getLatitude(), geoloc.getLongitude(),
				geoloc.getExpiration(), geoloc.getAccuracy());

    	GeolocMessage msg = ChatUtils.createGeolocMessage(contact, geolocPush,
    			Core.getInstance().getImService().getImdnManager().isImdnActivated());

	    String msgId = sendChatMessage(msg);
        /* TODO: Return a ChatMessage with correct time-stamps in CR018. */
        return new com.gsma.services.rcs.chat.GeolocMessage(msgId, contact, geoloc, 0, 0);
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
		synchronized (lock) {
			// Update rich messaging history
			MessagingLog.getInstance().addIncomingOneToOneChatMessage(message);
			mChatEventBroadcaster.broadcastMessageReceived(message.getMessageId());
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
		synchronized (lock) {
			// Update rich messaging history
			MessagingLog.getInstance().addIncomingOneToOneChatMessage(geoloc);
			mChatEventBroadcaster.broadcastMessageReceived(geoloc.getMessageId());
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
		ContactId remoteContact = getRemoteContact();
		synchronized (lock) {
			ChatServiceImpl.removeChatSession(remoteContact);

			switch (error.getErrorCode()) {
				case ChatError.SESSION_INITIATION_FAILED:
				case ChatError.SESSION_INITIATION_CANCELLED:
					String msgId = session.getFirstMessage().getMessageId();
					MessagingLog.getInstance().updateChatMessageStatusAndReasonCode(msgId,
							Message.Status.Content.FAILED, ReasonCode.FAILED_SEND);
					mChatEventBroadcaster.broadcastMessageStatusChanged(remoteContact, msgId,
							Message.Status.Content.FAILED, ReasonCode.FAILED_SEND);
					break;
				default:
					break;
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
			mChatEventBroadcaster.broadcastComposingEvent(contact, status);
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
		synchronized (lock) {
			MessagingLog.getInstance().addOutgoingOneToOneChatMessage(msg,
					ChatLog.Message.Status.Content.SENDING, ReasonCode.UNSPECIFIED);
			mChatEventBroadcaster.broadcastMessageStatusChanged(getRemoteContact(), msgId,
					ChatLog.Message.Status.Content.SENDING, ReasonCode.UNSPECIFIED);
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
			logger.info("New message sent " + msgId);
		}
		synchronized (lock) {
			MessagingLog.getInstance().updateChatMessageStatusAndReasonCode(msgId,
					Message.Status.Content.SENT, ReasonCode.UNSPECIFIED);

			mChatEventBroadcaster.broadcastMessageStatusChanged(getRemoteContact(), msgId,
					Message.Status.Content.SENT, ReasonCode.UNSPECIFIED);
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
			logger.info("New message failure status for message " + msgId);
		}
		synchronized (lock) {
			MessagingLog.getInstance().updateChatMessageStatusAndReasonCode(msgId,
					Message.Status.Content.FAILED, ReasonCode.FAILED_SEND);

			mChatEventBroadcaster.broadcastMessageStatusChanged(getRemoteContact(), msgId,
					Message.Status.Content.FAILED, ReasonCode.FAILED_SEND);
		}
	}

	@Override
	public void handleMessageDeliveryStatus(ContactId contact, ImdnDocument imdn) {
		String msgId = imdn.getMsgId();
		String status = imdn.getStatus();
		if (logger.isActivated()) {
			logger.info("New message delivery status for message " + msgId + ", status " + status);
		}
		if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
				|| ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
				|| ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
			int reasonCode = imdnToFailedReasonCode(imdn);
			synchronized (lock) {
				MessagingLog.getInstance().updateChatMessageStatusAndReasonCode(msgId,
						Message.Status.Content.FAILED, reasonCode);

				mChatEventBroadcaster.broadcastMessageStatusChanged(contact, msgId,
						Message.Status.Content.FAILED, reasonCode);
			}

		} else if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
			synchronized (lock) {
				MessagingLog.getInstance().updateChatMessageStatusAndReasonCode(msgId,
						Message.Status.Content.DELIVERED, ReasonCode.UNSPECIFIED);

				mChatEventBroadcaster.broadcastMessageStatusChanged(contact, msgId,
						Message.Status.Content.DELIVERED, ReasonCode.UNSPECIFIED);
			}

		} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
			synchronized (lock) {
				MessagingLog.getInstance().updateChatMessageStatusAndReasonCode(msgId,
						Message.Status.Content.DISPLAYED, ReasonCode.UNSPECIFIED);

				mChatEventBroadcaster.broadcastMessageStatusChanged(contact, msgId,
						Message.Status.Content.DISPLAYED, ReasonCode.UNSPECIFIED);
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
    	// Not used in one-to-one chat
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleAddParticipantFailed(com.gsma.services.rcs.contact.ContactId, java.lang.String)
     */
    @Override
    public void handleAddParticipantFailed(ContactId contact, String reason) {
    	// Not used in one-to-one chat
    }

	/* (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleParticipantStatusChanged(com.gsma.services.rcs.chat.ParticipantInfo)
	 */
	@Override
	public void handleParticipantStatusChanged(ParticipantInfo participantInfo) {
		// Not used in one-to-one chat
	}

	@Override
	public void handleSessionAccepted() {
		// Not used in one-to-one chat
	}

	@Override
	public void handleSessionRejectedByUser() {
		if (logger.isActivated()) {
			logger.info("Session rejected by user.");
		}
		synchronized (lock) {
			ChatServiceImpl.removeChatSession(session.getRemoteContact());
		}
	}

	@Override
	public void handleSessionRejectedByTimeout() {
		if (logger.isActivated()) {
			logger.info("Session rejected by time-out.");
		}
		synchronized (lock) {
			ChatServiceImpl.removeChatSession(session.getRemoteContact());
		}
	}

	@Override
	public void handleSessionRejectedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session rejected by remote.");
		}
		synchronized (lock) {
			ChatServiceImpl.removeChatSession(session.getRemoteContact());
		}
	}

	@Override
	public void handleSessionInvited() {
		/* Not used by one-to-one chat */
	}

	@Override
	public void handleSessionAutoAccepted() {
		/* Not used by one-to-one chat */
	}
}
