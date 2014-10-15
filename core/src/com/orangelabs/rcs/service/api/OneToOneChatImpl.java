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

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.chat.IOneToOneChat;
import com.gsma.services.rcs.chat.IOneToOneChatListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ft.FileTransfer;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.FileTransferMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettings.ImSessionStartMode;
import com.orangelabs.rcs.service.broadcaster.IOneToOneChatEventBroadcaster;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.logger.Logger;

import android.text.GetChars;

/**
 * One-to-One Chat implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class OneToOneChatImpl extends IOneToOneChat.Stub implements ChatSessionListener {

	private final ContactId mContact;

	private final IOneToOneChatEventBroadcaster mBroadcaster;

	private final InstantMessagingService mImService;

	private final MessagingLog mMessagingLog;

	private final ChatServiceImpl mChatService;

	private final RcsSettings mRcsSettings;

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
	 * @param broadcaster IChatEventBroadcaster
	 * @param imService InstantMessagingService
	 * @param messagingLog MessagingLog
	 * @param rcsSettings RcsSettings
	 * @param mChatService ChatServiceImpl
	 */
	public OneToOneChatImpl(ContactId contact, IOneToOneChatEventBroadcaster broadcaster,
			InstantMessagingService imService, MessagingLog messagingLog,
			RcsSettings rcsSettings, ChatServiceImpl chatService) {
		mContact = contact;
		mBroadcaster = broadcaster;
		mImService = imService;
		mMessagingLog = messagingLog;
		mChatService = chatService;
		mRcsSettings = rcsSettings;
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
	 * Returns the remote contact identifier
	 * 
	 * @return ContactId
	 * @throws Exception
	 */
	public ContactId getRemoteContact() {
		return mContact;
	}

	/**
	 * Add chat message to Db
	 * 
	 * @param msg InstantMessage
	 * @param state state of message
	 */
	private void addOutgoingChatMessage(InstantMessage msg, int state) {
		mMessagingLog.addOutgoingOneToOneChatMessage(msg, state,
				ReasonCode.UNSPECIFIED);
		mBroadcaster.broadcastMessageStatusChanged(mContact, msg.getMessageId(), state,
				ReasonCode.UNSPECIFIED);
	}

	/**
	 * Sends a plain text message
	 * 
	 * @param message Text message
     * @return Chat message
     */
    public IChatMessage sendMessage(String message) {
		if (logger.isActivated()) {
			logger.debug("Send text message");
		}
		InstantMessage msg = ChatUtils.createTextMessage(mContact, message, mImService
				.getImdnManager().isImdnActivated());
		ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
				mMessagingLog, msg.getMessageId(), msg.getRemote(), msg.getTextMessage(),
				InstantMessage.MIME_TYPE, mContact.toString(), msg.getDate().getTime(),
				Direction.OUTGOING);

		/* If the IMS is connected at this time then send this message. */
		if (ServerApiUtils.isImsConnected()) {
			sendChatMessage(msg);
		} else {
			/* If the IMS is NOT connected at this time then queue message. */
			addOutgoingChatMessage(msg, Message.Status.Content.QUEUED);
		}
		return new ChatMessageImpl(persistentStorage);
	}

	/**
	 * Sends a geoloc message
	 *
	 * @param geoloc Geoloc
	 * @return ChatMessage
	 */
	public IChatMessage sendMessage2(Geoloc geoloc) {
		if (logger.isActivated()) {
			logger.debug("Send geoloc message");
		}
		GeolocPush geolocPush = new GeolocPush(geoloc.getLabel(), geoloc.getLatitude(),
				geoloc.getLongitude(), geoloc.getExpiration(), geoloc.getAccuracy());
		GeolocMessage msg = ChatUtils.createGeolocMessage(mContact, geolocPush, mImService
				.getImdnManager().isImdnActivated());
		ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
				mMessagingLog, msg.getMessageId(), msg.getRemote(), msg.toString(),
				GeolocMessage.MIME_TYPE, mContact.toString(), msg.getDate().getTime(),
				Direction.OUTGOING);

		/* If the IMS is connected at this time then send this message. */
		if (ServerApiUtils.isImsConnected()) {
			sendChatMessage(msg);
		} else {
			/* If the IMS is NOT connected at this time then queue message. */
			addOutgoingChatMessage(msg, Message.Status.Content.QUEUED);
		}
		return new ChatMessageImpl(persistentStorage);
	}

	/**
     * Sends a chat message
     * 
     * @param msg Message
     */
	private void sendChatMessage(final InstantMessage msg) {
		synchronized (lock) {
			if (logger.isActivated()) {
				logger.debug("Send chat message");
			}
			final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
			if (session == null) {
				try {
					if (logger.isActivated()) {
						logger.debug("Core session is not yet established: initiate a new session to send the message");
					}
					addOutgoingChatMessage(msg, Message.Status.Content.SENDING);
					final OneToOneChatSession newSession = mImService.initiateOneToOneChatSession(
							mContact, msg);
					new Thread() {
						public void run() {
							newSession.startSession();
						}
					}.start();
					newSession.addListener(this);
					mChatService.addOneToOneChat(mContact, this);
					handleMessageSent(msg.getMessageId());

				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't send a new chat message", e);
					}
					handleMessageFailedSend(msg.getMessageId());
				}
			} else {
				if (session.isMediaEstablished()) {
					if (logger.isActivated()) {
						logger.debug("Core session is established: use existing one to send the message");
					}
					addOutgoingChatMessage(msg, Message.Status.Content.SENDING);
					if (msg instanceof GeolocMessage) {
						session.sendGeolocMessage((GeolocMessage)msg);
					} else {
						session.sendTextMessage(msg);
					}
					return;
				}
				addOutgoingChatMessage(msg, Message.Status.Content.QUEUED);
				if (!session.isInitiatedByRemote()) {
					return;
				}
				if (logger.isActivated()) {
					logger.debug("Core chat session is pending: auto accept it.");
				}
				new Thread() {
					public void run() {
						session.acceptSession();
					}
				}.start();
			}
		}
	}

	/**
	 * Sends a displayed delivery report for a given message ID
	 * 
	 * @param contact Contact ID
	 * @param msgId Message ID
	 */
	/* package private */void sendDisplayedDeliveryReport(final ContactId contact,
			final String msgId) {
		try {
			if (logger.isActivated()) {
				logger.debug("Set displayed delivery report for " + msgId);
			}
			final OneToOneChatSession session = mImService.getOneToOneChatSession(contact);
			if (session != null && session.isMediaEstablished()) {
				if (logger.isActivated()) {
					logger.info("Use the original session to send the delivery status for " + msgId);
				}

				new Thread() {
					public void run() {
						session.sendMsrpMessageDeliveryStatus(contact, msgId,
								ImdnDocument.DELIVERY_STATUS_DISPLAYED);
					}
				}.start();
			} else {
				if (logger.isActivated()) {
					logger.info("No suitable session found to send the delivery status for "
							+ msgId + " : use SIP message");
				}
				mImService.getImdnManager().sendMessageDeliveryStatus(contact, msgId,
						ImdnDocument.DELIVERY_STATUS_DISPLAYED);
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Could not send MSRP delivery status", e);
			}
		}
	}

	/**
	 * Sends an is-composing event. The status is set to true when typing a
	 * message, else it is set to false.
	 * 
	 * @param status Is-composing status
	 */
	public void sendIsComposingEvent(final boolean status) {
		final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
		if (session == null) {
			if (logger.isActivated()) {
				logger.debug("Unable to send composing event '" + status
						+ "' since oneToOne chat session found with contact '" + mContact
						+ "' does not exist for now");
			}
			return;
		}
		if (session.getDialogPath().isSessionEstablished()) {
			session.sendIsComposingStatus(status);
			return;
		}
		if (!session.isInitiatedByRemote()) {
			return;
		}
		ImSessionStartMode imSessionStartMode = mRcsSettings.getImSessionStartMode();
		switch (imSessionStartMode) {
			case ON_OPENING:
			case ON_COMPOSING:
				if (logger.isActivated()) {
					logger.debug("Core chat session is pending: auto accept it.");
				}
				session.acceptSession();
				break;
			default:
				break;
		}
	}

	/**
	 * open the chat conversation. Note: if it’s an incoming pending chat
	 * session and the parameter IM SESSION START is 0 then the session is
	 * accepted now.
	 */
	public void openChat() {
		if (logger.isActivated()) {
			logger.info("Open a 1-1 chat session with " + mContact);
		}
		try {
			final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
			if (session == null) {
				/*
				 * If there is no session ongoing right now then we do not need
				 * to open anything right now so we just return here. A sending
				 * of a new message on this one-to-ont chat will anyway result
				 * in creating a new session so we do not need to do anything
				 * more here for now.
				 */
				return;
			}
			if (!session.getDialogPath().isSessionEstablished()) {
				ImSessionStartMode imSessionStartMode = mRcsSettings.getImSessionStartMode();
				if (!session.isInitiatedByRemote()) {
					/*
					 * This method needs to accept pending invitation if
					 * IM_SESSION_START_MODE is 0, which is not applicable if
					 * session is remote originated so we return here.
					 */
					return;
				}
				if (ImSessionStartMode.ON_OPENING == imSessionStartMode) {
					if (logger.isActivated()) {
						logger.debug("Core chat session is pending: auto accept it, as IM_SESSION_START mode = 0");
					}
					session.acceptSession();
				}
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			// TODO: Exception handling in CR037
		}
	}

	/*------------------------------- SESSION EVENTS ----------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see
	 * com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionStarted
	 * ()
	 */
	@Override
	public void handleSessionStarted() {
		if (logger.isActivated()) {
			logger.info("Session started");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionAborted
	 * (int)
	 */
	@Override
	public void handleSessionAborted(int reason) {
		if (logger.isActivated()) {
			logger.info(new StringBuilder("Session aborted (reason ").append(reason).append(")")
					.toString());
		}
		synchronized (lock) {
			mChatService.removeOneToOneChat(mContact);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#
	 * handleSessionTerminatedByRemote()
	 */
	@Override
	public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session terminated by remote");
		}
		synchronized (lock) {
			mChatService.removeOneToOneChat(mContact);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#
	 * handleReceiveMessage
	 * (com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage)
	 */
	@Override
	public void handleReceiveMessage(InstantMessage message) {
		String msgId = message.getMessageId();
		if (logger.isActivated()) {
			logger.info(new StringBuilder("New IM with messageId '").append(msgId)
					.append("' received from ").append(mContact).toString());
		}
		synchronized (lock) {
			mMessagingLog.addIncomingOneToOneChatMessage(message);
			mBroadcaster.broadcastMessageReceived(msgId);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#
	 * handleReceiveGeoloc
	 * (com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage)
	 */
	@Override
	public void handleReceiveGeoloc(GeolocMessage geoloc) {
		if (logger.isActivated()) {
			logger.info("New geoloc received");
		}
		synchronized (lock) {
			mMessagingLog.addIncomingOneToOneChatMessage(geoloc);
			mBroadcaster.broadcastMessageReceived(geoloc.getMessageId());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleImError
	 * (com.orangelabs.rcs.core.ims.service.im.chat.ChatError)
	 */
	@Override
	public void handleImError(ChatError error) {
		if (logger.isActivated()) {
			logger.info("IM error " + error.getErrorCode());
		}
		synchronized (lock) {
			mChatService.removeOneToOneChat(mContact);

			switch (error.getErrorCode()) {
				case ChatError.SESSION_INITIATION_FAILED:
				case ChatError.SESSION_INITIATION_CANCELLED:
					final OneToOneChatSession session = mImService.getOneToOneChatSession(mContact);
					String msgId = session.getFirstMessage().getMessageId();
					mMessagingLog.setChatMessageStatusAndReasonCode(msgId,
							Message.Status.Content.FAILED, ReasonCode.FAILED_SEND);
					mBroadcaster.broadcastMessageStatusChanged(mContact, msgId,
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
			logger.info(new StringBuilder("").append(contact)
					.append(" is composing status set to ").append(status).toString());
		}
		synchronized (lock) {
			mBroadcaster.broadcastComposingEvent(contact, status);
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
			logger.info(new StringBuilder("Set message with status ")
					.append(Message.Status.Content.SENDING).append(" id=").append(msgId).toString());
		}
		synchronized (lock) {
			mMessagingLog.setChatMessageStatusAndReasonCode(msgId,
					Message.Status.Content.SENDING, ReasonCode.UNSPECIFIED);
			mBroadcaster.broadcastMessageStatusChanged(mContact, msgId,
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
			logger.info(new StringBuilder("New message status ").append(Message.Status.Content.SENT)
					.append(msgId).toString());
		}
		synchronized (lock) {
			mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Message.Status.Content.SENT,
					ReasonCode.UNSPECIFIED);

			mBroadcaster.broadcastMessageStatusChanged(mContact, msgId,
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
			logger.info(new StringBuilder("New message status ")
					.append(Message.Status.Content.FAILED).append(" for message ").append(msgId)
					.toString());
		}
		synchronized (lock) {
			mMessagingLog.setChatMessageStatusAndReasonCode(msgId,
					Message.Status.Content.FAILED, ReasonCode.FAILED_SEND);

			mBroadcaster.broadcastMessageStatusChanged(mContact, msgId,
					Message.Status.Content.FAILED, ReasonCode.FAILED_SEND);
		}
	}

	@Override
	public void handleMessageDeliveryStatus(ContactId contact, ImdnDocument imdn) {
		String msgId = imdn.getMsgId();
		String status = imdn.getStatus();
		if (logger.isActivated()) {
			logger.info(new StringBuilder("New message delivery status for message ").append(msgId)
					.append(", status ").append(status).toString());
		}
		if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
				|| ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
				|| ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
			int reasonCode = imdnToFailedReasonCode(imdn);
			synchronized (lock) {
				mMessagingLog.setChatMessageStatusAndReasonCode(msgId,
						Message.Status.Content.FAILED, reasonCode);

				mBroadcaster.broadcastMessageStatusChanged(contact, msgId,
						Message.Status.Content.FAILED, reasonCode);
			}

		} else if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
			synchronized (lock) {
				mMessagingLog.setChatMessageStatusAndReasonCode(msgId,
						Message.Status.Content.DELIVERED, ReasonCode.UNSPECIFIED);

				mBroadcaster.broadcastMessageStatusChanged(contact, msgId,
						Message.Status.Content.DELIVERED, ReasonCode.UNSPECIFIED);
			}

		} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
			synchronized (lock) {
				mMessagingLog.setChatMessageStatusAndReasonCode(msgId,
						Message.Status.Content.DISPLAYED, ReasonCode.UNSPECIFIED);

				mBroadcaster.broadcastMessageStatusChanged(contact, msgId,
						Message.Status.Content.DISPLAYED, ReasonCode.UNSPECIFIED);
			}
		}
	}

	@Override
	public void handleSessionRejectedByUser() {
		if (logger.isActivated()) {
			logger.info("Session rejected by user.");
		}
		synchronized (lock) {
			mChatService.removeOneToOneChat(mContact);
		}
	}

	@Override
	public void handleSessionRejectedByTimeout() {
		if (logger.isActivated()) {
			logger.info("Session rejected by time-out.");
		}
		synchronized (lock) {
			mChatService.removeOneToOneChat(mContact);
		}
	}

	@Override
	public void handleSessionRejectedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session rejected by remote.");
		}
		synchronized (lock) {
			mChatService.removeOneToOneChat(mContact);
		}
	}

	@Override
	public void handleConferenceEvent(ContactId contact, String contactDisplayname, String state) {
		/* Not used by one-to-one chat */
	}

	/*
	 * (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#
	 * handleAddParticipantSuccessful(com.gsma.services.rcs.contact.ContactId)
	 */
	@Override
	public void handleAddParticipantSuccessful(ContactId contact) {
		/* Not used by one-to-one chat */
	}

	/*
	 * (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#
	 * handleAddParticipantFailed(com.gsma.services.rcs.contact.ContactId,
	 * java.lang.String)
	 */
	@Override
	public void handleAddParticipantFailed(ContactId contact, String reason) {
		/* Not used by one-to-one chat */
	}

	/*
	 * (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#
	 * handleParticipantStatusChanged
	 * (com.gsma.services.rcs.chat.ParticipantInfo)
	 */
	@Override
	public void handleParticipantStatusChanged(ParticipantInfo participantInfo) {
		/* Not used by one-to-one chat */
	}

	@Override
	public void handleSessionAccepted() {
		/* Not used by one-to-one chat */
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
