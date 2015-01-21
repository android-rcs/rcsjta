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

package com.orangelabs.rcs.core.ims.service.im.chat;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Abstract 1-1 chat session
 *
 * @author Jean-Marc AUFFRET
 */
public abstract class OneToOneChatSession extends ChatSession {
	/**
	 * Boundary tag
	 */
	private final static String BOUNDARY_TAG = "boundary1";

	private final ChatMessage mFirstMsg;

	/**
	 * The logger
	 */
	private final static Logger logger = Logger
			.getLogger(OneToOneChatSession.class.getSimpleName());

	/**
	 * Constructor
	 *
	 * @param parent IMS service
	 * @param contact Remote contact identifier
	 * @param remoteUri Remote URI
	 * @param firstMsg First chat message
	 * @param rcsSettings RCS settings
	 * @param messagingLog Messaging log
	 */
	public OneToOneChatSession(ImsService parent, ContactId contact, String remoteUri,
			ChatMessage firstMsg, RcsSettings rcsSettings, MessagingLog messagingLog) {
		super(parent, contact, remoteUri, OneToOneChatSession.generateOneOneParticipants(contact),
				rcsSettings, messagingLog);

		// Set feature tags
		List<String> featureTags = ChatUtils.getSupportedFeatureTagsForChat();
		setFeatureTags(featureTags);

		// Set Accept-Contact header
		setAcceptContactTags(featureTags);

		String acceptTypes = new StringBuilder(CpimMessage.MIME_TYPE).append(" ")
				.append(IsComposingInfo.MIME_TYPE).toString();
		setAcceptTypes(acceptTypes);

		StringBuilder wrappedTypes = new StringBuilder(MimeType.TEXT_MESSAGE).append(" ")
				.append(ImdnDocument.MIME_TYPE);
		if (mRcsSettings.isGeoLocationPushSupported()) {
			wrappedTypes.append(" ").append(GeolocInfoDocument.MIME_TYPE);
		}
		if (mRcsSettings.isFileTransferHttpSupported()) {
			wrappedTypes.append(" ").append(FileTransferHttpInfoDocument.MIME_TYPE);
		}
		setWrappedTypes(wrappedTypes.toString());

		mFirstMsg = firstMsg;
	}

	/**
	 * Is group chat
	 *
	 * @return Boolean
	 */
	public boolean isGroupChat() {
		return false;
	}

	/**
	 * Generate the set of participants for a 1-1 chat
	 *
	 * @param contact ContactId
	 * @return Set of participants
	 */
	private static Set<ParticipantInfo> generateOneOneParticipants(ContactId contact) {
		Set<ParticipantInfo> set = new HashSet<ParticipantInfo>();
		ParticipantInfoUtils.addParticipant(set, contact);
		return set;
	}

	/**
	 * Returns the set of participants currently connected to the session
	 *
	 * @return Set of participants
	 */
	public Set<ParticipantInfo> getConnectedParticipants() {
		return getParticipants();
	}

	/**
	 * Close media session
	 */
	public void closeMediaSession() {
		// Stop the activity manager
		getActivityManager().stop();

		// Close MSRP session
		closeMsrpSession();
	}

	/**
	 * Send a text message
	 * @param msg Chat message
	 */
	@Override
	public void sendChatMessage(ChatMessage msg) {
		String from = ChatUtils.ANOMYNOUS_URI;
		String to = ChatUtils.ANOMYNOUS_URI;
		String msgId = msg.getMessageId();
		String networkContent;
		boolean useImdn = getImdnManager().isImdnActivated()
				&& !mRcsSettings.isAlbatrosRelease();
		String mimeType = msg.getMimeType();
		if (useImdn) {
			networkContent = ChatUtils.buildCpimMessageWithImdn(from, to, msgId,
					msg.getContent(), mimeType);

		} else {
			networkContent = ChatUtils.buildCpimMessage(from, to,
					msg.getContent(), mimeType);
		}

		Collection<ImsSessionListener> listeners = getListeners();
		for (ImsSessionListener listener : listeners) {
			((ChatSessionListener)listener).handleMessageSending(msg);
		}

		boolean sendOperationSucceeded = false;
		if (ChatUtils.isGeolocType(mimeType)) {
			sendOperationSucceeded = sendDataChunks(IdGenerator.generateMessageID(), networkContent,
					CpimMessage.MIME_TYPE, TypeMsrpChunk.GeoLocation);
		} else {
			sendOperationSucceeded = sendDataChunks(IdGenerator.generateMessageID(), networkContent,
					CpimMessage.MIME_TYPE, TypeMsrpChunk.TextMessage);
		}

		if (sendOperationSucceeded) {
			for (ImsSessionListener listener : getListeners()) {
				((ChatSessionListener)listener).handleMessageSent(msgId,
				        ChatUtils.networkMimeTypeToApiMimeType(mimeType));
			}
		} else {
			for (ImsSessionListener listener : getListeners()) {
				((ChatSessionListener)listener).handleMessageFailedSend(msgId,
				        ChatUtils.networkMimeTypeToApiMimeType(mimeType));
			}
		}
	}

	/**
	 * Send is composing status
	 *
	 * @param status Status
	 */
	public void sendIsComposingStatus(boolean status) {
		String content = IsComposingInfo.buildIsComposingInfo(status);
		String msgId = IdGenerator.generateMessageID();
		sendDataChunks(msgId, content, IsComposingInfo.MIME_TYPE,
				MsrpSession.TypeMsrpChunk.IsComposing);
	}

	/**
	 * Reject the session invitation
	 */
	public void rejectSession() {
		rejectSession(486);
	}

	/**
	 * Create INVITE request
	 *
	 * @param content Content part
	 * @return Request
	 * @throws SipException
	 */
	private SipRequest createMultipartInviteRequest(String content)
			throws SipException {
		SipRequest invite = SipMessageFactory.createMultipartInvite(getDialogPath(),
				getFeatureTags(), content, BOUNDARY_TAG);

		// Add a contribution ID header
		invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());

		return invite;
	}

	/**
	 * Create INVITE request
	 *
	 * @param content Content part
	 * @return Request
	 * @throws SipException
	 */
	private SipRequest createInviteRequest(String content) throws SipException {
		SipRequest invite = SipMessageFactory.createInvite(getDialogPath(),
				InstantMessagingService.CHAT_FEATURE_TAGS, content);

		// Add a contribution ID header
		invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());

		return invite;
	}

	/**
	 * Create an INVITE request
	 *
	 * @return the INVITE request
	 * @throws SipException
	 */
	public SipRequest createInvite() throws SipException {
		// If there is a first message then builds a multipart content else
		// builds a SDP content
		String content = getDialogPath().getLocalContent();
		if (getFirstMessage() != null) {
			return createMultipartInviteRequest(content);
		}
		return createInviteRequest(content);
	}

	/**
	 * Handle 200 0K response
	 *
	 * @param resp 200 OK response
	 */
	public void handle200OK(SipResponse resp) {
		super.handle200OK(resp);

		// Start the activity manager
		getActivityManager().start();
	}

	/**
	 * Get SDP direction
	 *
	 * @return Direction
	 * @see com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils#DIRECTION_RECVONLY
	 * @see com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils#DIRECTION_SENDONLY
	 * @see com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils#DIRECTION_SENDRECV
	 */
	public abstract String getSdpDirection();

	/**
	 * Get first message.
	 *
	 * @param ChatMessage First chat message
	 */
	public ChatMessage getFirstMessage() {
		return mFirstMsg;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.orangelabs.rcs.core.ims.service.im.chat.ChatSession#msrpTransferError
	 * (java.lang.String, java.lang.String,
	 * com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk)
	 */
	@Override
	public void msrpTransferError(String msgId, String error,
			MsrpSession.TypeMsrpChunk typeMsrpChunk) {
		super.msrpTransferError(msgId, error, typeMsrpChunk);
		try {
			ContactId remote = ContactUtils.createContactId(getDialogPath().getRemoteParty());
			// Request capabilities to the remote
			getImsService().getImsModule().getCapabilityService()
					.requestContactCapabilities(remote);
		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.warn("Cannot parse contact " + getDialogPath().getRemoteParty());
			}
		}
	}

	@Override
	public void receiveBye(SipRequest bye) {
		super.receiveBye(bye);

		// Request capabilities to the remote
		getImsService().getImsModule().getCapabilityService()
				.requestContactCapabilities(getRemoteContact());
	}

	@Override
	public void receiveCancel(SipRequest cancel) {
		super.receiveCancel(cancel);

		// Request capabilities to the remote
		getImsService().getImsModule().getCapabilityService()
				.requestContactCapabilities(getRemoteContact());
	}
}
