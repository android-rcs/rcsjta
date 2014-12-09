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

package com.orangelabs.rcs.core.ims.service.im;

import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import javax2.sip.header.ContactHeader;
import javax2.sip.message.Response;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message.ReasonCode;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ft.FileTransfer;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.capability.Capabilities;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.DelayedDisplayNotificationManager;
import com.orangelabs.rcs.core.ims.service.im.chat.FileTransferMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.OneOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.OriginatingAdhocGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.OriginatingOne2OneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ParticipantInfoUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.RejoinGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.RestartGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.TerminatingAdhocGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.TerminatingOne2OneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.orangelabs.rcs.core.ims.service.im.chat.standfw.StoreAndForwardManager;
import com.orangelabs.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardMsgSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FtHttpResumeManager;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.OriginatingHttpFileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.OriginatingHttpGroupFileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.TerminatingHttpFileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.msrp.OriginatingMsrpFileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.msrp.TerminatingMsrpFileSharingSession;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.MimeManager;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Instant messaging services (1-1 chat, group chat and file transfer)
 * 
 * @author Jean-Marc AUFFRET
 */
public class InstantMessagingService extends ImsService {

    /**
     * Chat features tags
     */
    public final static String[] CHAT_FEATURE_TAGS = { FeatureTags.FEATURE_OMA_IM };

    /**
     * File transfer features tags
     */
    public final static String[] FT_FEATURE_TAGS = { FeatureTags.FEATURE_OMA_IM };

	/**
	 * Max chat sessions
	 */
	private int maxChatSessions;

	/**
	 * Max file transfer sessions
	 */
	private int maxFtSessions;
	
	/**
	 * Max file transfer size
	 */
	private int maxFtSize;

	/**
	 * IMDN manager
	 */
	private ImdnManager imdnMgr;	
	
	private FtHttpResumeManager resumeManager;

	/**
	 * Store & Forward manager
	 */
	private StoreAndForwardManager storeAndFwdMgr = new StoreAndForwardManager(this);

	/**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(InstantMessagingService.class.getName());


	/**
     * Constructor
     * 
     * @param parent IMS module
     * @throws CoreException
     */
	public InstantMessagingService(ImsModule parent) throws CoreException {
        super(parent, true);

		maxChatSessions = RcsSettings.getInstance().getMaxChatSessions();
        maxFtSessions = RcsSettings.getInstance().getMaxFileTransferSessions();
        maxFtSize = FileSharingSession.getMaxFileSharingSize();
	}

	private void handleFileTransferInvitationRejected(SipRequest invite, int reasonCode) {
		ContactId contact = ContactUtils.createContactId(SipUtils.getAssertedIdentity(invite));
		MmContent content = ContentManager.createMmContentFromSdp(invite);
		MmContent fileIcon = FileTransferUtils.extractFileIcon(invite);
		getImsModule().getCore().getListener()
				.handleFileTransferInvitationRejected(contact, content, fileIcon, reasonCode);
	}

	private void handleGroupChatInvitationRejected(SipRequest invite, int reasonCode) {
		String chatId = ChatUtils.getContributionId(invite);
		ContactId contact = ChatUtils.getReferredIdentityAsContactId(invite);
		String subject = ChatUtils.getSubject(invite);
		Set<ParticipantInfo> participants = ChatUtils.getListOfParticipants(invite);
		getImsModule().getCore().getListener()
				.handleGroupChatInvitationRejected(chatId, contact, subject, participants, reasonCode);
	}

	/**
	 * Start the IMS service
	 */
	public synchronized void start() {
		if (isServiceStarted()) {
			// Already started
			return;
		}
		setServiceStarted(true);
		
		// Start IMDN manager
        imdnMgr = new ImdnManager(this);
		imdnMgr.start();

		// Send delayed displayed notifications for read messages if they were
		// not sent before already
		new DelayedDisplayNotificationManager(this);
		// Start resuming FT HTTP
		resumeManager = new FtHttpResumeManager(this);
	}

    /**
     * Stop the IMS service
     */
	public synchronized void stop() {
		if (!isServiceStarted()) {
			// Already stopped
			return;
		}
		setServiceStarted(false);
		
		// Stop IMDN manager
		imdnMgr.terminate();
        imdnMgr.interrupt();
        if (resumeManager != null)
        	resumeManager.terminate();
	}

	/**
     * Check the IMS service
     */
	public void check() {
	}
	
	/**
	 * Returns the IMDN manager
	 * 
	 * @return IMDN manager
	 */
	public ImdnManager getImdnManager() {
		return imdnMgr;
	}	

	/**
	 * Get Store & Forward manager
	 */
	public StoreAndForwardManager getStoreAndForwardManager() {
		return storeAndFwdMgr;
	}

    /**
     * Returns IM sessions
     * 
     * @return List of sessions
     */
	public Vector<ChatSession> getImSessions() {
		// Search all IM sessions
		Vector<ChatSession> result = new Vector<ChatSession>();
		Enumeration<ImsServiceSession> list = getSessions();
		while(list.hasMoreElements()) {
			ImsServiceSession session = list.nextElement();
			if (session instanceof ChatSession) {
				result.add((ChatSession)session);
			}
		}

		return result;
    }

	/**
     * Returns IM sessions with a given contact
     * 
     * @param contact Contact
     * @return List of sessions
     */
	public Vector<ChatSession> getImSessionsWith(ContactId contact) {
		// Search all IM sessions
		Vector<ChatSession> result = new Vector<ChatSession>();
		Enumeration<ImsServiceSession> list = getSessions();
		while(list.hasMoreElements()) {
			ImsServiceSession session = list.nextElement();
			if ((session instanceof OneOneChatSession) && contact != null && contact.equals(session.getRemoteContact())) {
				result.add((ChatSession)session);
			}
		}

		return result;
    }

	/**
     * Returns Group chat session with a given chatId
     *
     * @param chatId
     * @return Group chat session
     */
	public GroupChatSession getGroupChatSession(String chatId) {
		// Search all IM sessions
		Enumeration<ImsServiceSession> imsServiceSessions = getSessions();
		while (imsServiceSessions.hasMoreElements()) {
			ImsServiceSession imsServiceSession = imsServiceSessions.nextElement();
			if (imsServiceSession instanceof GroupChatSession) {
				GroupChatSession groupChatSession = (GroupChatSession)imsServiceSession;
				if ((groupChatSession.getContributionID()).equals(chatId)) {
					return groupChatSession;
				}
			}
		}
		return null;
	}

	/**
     * Returns active file transfer sessions
     * 
     * @return List of sessions
     */
	public Vector<FileSharingSession> getFileTransferSessions() {
		Vector<FileSharingSession> result = new Vector<FileSharingSession>();
		Enumeration<ImsServiceSession> list = getSessions();
		while(list.hasMoreElements()) {
			ImsServiceSession session = list.nextElement();
			if (session instanceof FileSharingSession) {
				result.add((FileSharingSession)session);
			}
		}

		return result;
    }

	/**
	 * Initiate a file transfer session
	 * 
	 * @param contact
	 *            Remote contact identifier
	 * @param content
	 *            Content of file to sent
	 * @param fileIcon
	 *            true if the stack must try to attach fileIcon
	 * @return File transfer session
	 * @throws CoreException
	 */
	public FileSharingSession initiateFileTransferSession(ContactId contact, MmContent content, boolean fileIcon) throws CoreException {
		if (logger.isActivated()) {
			logger.info("Initiate a file transfer session with contact " + contact + ", file " + content.toString());
		}

		// Test number of sessions
		if ((maxFtSessions != 0) && (getFileTransferSessions().size() >= maxFtSessions)) {
			if (logger.isActivated()) {
				logger.debug("The max number of file transfer sessions is achieved: cancel the initiation");
			}
			throw new CoreException("Max file transfer sessions achieved");
		}

        // Test max size
        if (maxFtSize > 0 && content.getSize() > maxFtSize) {
            if (logger.isActivated()) {
                logger.debug("File exceeds max size: cancel the initiation");
            }
            throw new CoreException("File exceeds max size");
        }

		// Check contact capabilities
		boolean isFToHttpSupportedByRemote = false;
		Capabilities capability = ContactsManager.getInstance().getContactCapabilities(contact);
		if (capability != null) {
			isFToHttpSupportedByRemote = capability.isFileTransferHttpSupported();
		}

		// Select default protocol
		Capabilities myCapability = RcsSettings.getInstance().getMyCapabilities();
		boolean isHttpProtocol = false;
		if (isFToHttpSupportedByRemote && myCapability.isFileTransferHttpSupported()) {
			if (FileTransferProtocol.HTTP.equals(RcsSettings.getInstance().getFtProtocol())) {
				isHttpProtocol = true;
			}
		}

		if (fileIcon && (MimeManager.isImageType(content.getEncoding()) == false)) {
			fileIcon = false;
		}

		// Initiate session
		FileSharingSession session;
		if (isHttpProtocol) {
			// Create a new session
			session = new OriginatingHttpFileSharingSession(this, content, contact,
					PhoneUtils.formatContactIdToUri(contact), fileIcon, UUID.randomUUID()
							.toString());
		} else {
			if (fileIcon) {
				// Check thumbnail capabilities
				if (capability != null && capability.isFileTransferThumbnailSupported() == false) {
					fileIcon = false;
					if (logger.isActivated()) {
						logger.warn("Thumbnail not supported by remote");
					}
				}
				if (fileIcon && myCapability.isFileTransferThumbnailSupported() == false) {
					fileIcon = false;
					if (logger.isActivated()) {
						logger.warn("Thumbnail not supported !");
					}
				}
			}
			// Create a new session
			session = new OriginatingMsrpFileSharingSession(this, content, contact, fileIcon);
		}
		return session;
	}
	
	/**
	 * Initiate a group file transfer session
	 * 
	 * @param contacts
	 *            Set of remote contacts
	 * @param content
	 *            The file content to be sent
	 * @param fileIcon
	 *            true if the stack must try to attach fileIcon
	 * @param chatContributionId
	 *            Chat contribution ID
	 * @return File transfer session
	 * @throws CoreException
	 */
	public FileSharingSession initiateGroupFileTransferSession(Set<ParticipantInfo> participants, MmContent content, boolean fileIcon,
			String chatContributionId) throws CoreException {
		if (logger.isActivated()) {
			logger.info("Send file " + content.toString() + " to " + participants.size() + " contacts");
		}

		// Select default protocol
		Capabilities myCapability = RcsSettings.getInstance().getMyCapabilities();
		if (!myCapability.isFileTransferHttpSupported()) {
			throw new CoreException("Group file transfer not supported");
		}

		// Test number of sessions
		if ((maxFtSessions != 0) && (getFileTransferSessions().size() >= maxFtSessions)) {
			if (logger.isActivated()) {
				logger.debug("The max number of file transfer sessions is achieved: cancel the initiation");
			}
			throw new CoreException("Max file transfer sessions achieved");
		}

		// Test max size
		if (maxFtSize > 0 && content.getSize() > maxFtSize) {
			if (logger.isActivated()) {
				logger.debug("File exceeds max size: cancel the initiation");
			}
			throw new CoreException("File exceeds max size");
		}

		GroupChatSession groupChatSession = getGroupChatSession(chatContributionId);
		if (groupChatSession == null) {
			throw new CoreException("Cannot transfer file: Group Chat not established");
		}
		// TODO Cannot transfer file to group if Group Chat is not established: to implement with CR018
		// Create a new session
		FileSharingSession session = new OriginatingHttpGroupFileSharingSession(this, content,
				fileIcon, ImsModule.IMS_USER_PROFILE.getImConferenceUri(), participants,
				groupChatSession.getSessionID(), chatContributionId, UUID.randomUUID()
				.toString());

		return session;
	}

	/**
     * Receive a file transfer invitation
     * 
     * @param invite Initial invite
     */
	public void receiveFileTransferInvitation(SipRequest invite) {
		if (logger.isActivated()) {
    		logger.info("Receive a file transfer session invitation");
    	}

		try {
			// Test if the contact is blocked
			ContactId remote = ContactUtils.createContactId(SipUtils.getAssertedIdentity(invite));
			if (ContactsManager.getInstance().isFtBlockedForContact(remote)) {
				if (logger.isActivated()) {
					logger.debug("Contact " + remote + " is blocked: automatically reject the file transfer invitation");
				}

				handleFileTransferInvitationRejected(invite, FileTransfer.ReasonCode.REJECTED_SPAM);

				// Send a 603 Decline response
				sendErrorResponse(invite, Response.DECLINE);
				return;
			}

			// Test number of sessions
			if ((maxFtSessions != 0) && (getFileTransferSessions().size() >= maxFtSessions)) {
				if (logger.isActivated()) {
					logger.debug("The max number of file transfer sessions is achieved: reject the invitation");
				}

				handleFileTransferInvitationRejected(invite,
						FileTransfer.ReasonCode.REJECTED_MAX_FILE_TRANSFERS);

				// Send a 603 Decline response
				sendErrorResponse(invite, Response.DECLINE);
				return;
			}

			// Create a new session
			FileSharingSession session = new TerminatingMsrpFileSharingSession(this, invite);

			getImsModule().getCore().getListener().handleFileTransferInvitation(session, false, remote, session.getRemoteDisplayName());

			session.startSession();

		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.warn("Cannot parse contact from FT invitation");
			}
		}
	}

    /**
     * Initiate a one-to-one chat session
     * 
     * @param contact Remote contact identifier
     * @param firstMsg First message
     * @return IM session
     * @throws CoreException
     */
	public ChatSession initiateOne2OneChatSession(ContactId contact, InstantMessage firstMsg)
			throws CoreException {
		if (logger.isActivated()) {
			logger.info("Initiate 1-1 chat session with " + contact);
		}
		// Test number of sessions
		if ((maxChatSessions != 0) && (getImSessions().size() >= maxChatSessions)) {
			if (logger.isActivated()) {
				logger.debug("The max number of chat sessions is achieved: cancel the initiation");
			}
			throw new CoreException("Max chat sessions achieved");
		}
		// Create a new session
		OriginatingOne2OneChatSession session = new OriginatingOne2OneChatSession(this, contact, firstMsg);
		// Save the message
		if (firstMsg != null && !(firstMsg instanceof FileTransferMessage)) {
			MessagingLog.getInstance().addOutgoingOneToOneChatMessage(firstMsg,
					ChatLog.Message.Status.Content.SENT, ReasonCode.UNSPECIFIED);
		}
		return session;
	}

    /**
     * Receive a one-to-one chat session invitation
     * 
     * @param invite Initial invite
     */
    public void receiveOne2OneChatSession(SipRequest invite) {
		if (logger.isActivated()){
			logger.info("Receive a 1-1 chat session invitation");
		}
		try {
			ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);

			// Discard invitation if message ID is already received
			InstantMessage firstMsg = ChatUtils.getFirstMessage(invite);
			if (firstMsg != null) {
				String msgId = ChatUtils.getMessageId(invite);
				if (msgId != null) {
					if (MessagingLog.getInstance().isMessagePersisted(msgId)) {
						// Send a 603 Decline response
						sendErrorResponse(invite, Response.DECLINE);
						return;
					}
				}
			}

			// Test if the contact is blocked
			if (ContactsManager.getInstance().isImBlockedForContact(remote)) {
				if (logger.isActivated()) {
					logger.debug("Contact " + remote + " is blocked: automatically reject the chat invitation");
				}

				// Save the message in the spam folder
				if (firstMsg != null) {
					MessagingLog.getInstance().addSpamMessage(firstMsg);
				}

				// Send message delivery report if requested
				if (ChatUtils.isImdnDeliveredRequested(invite)) {
					// Check notification disposition
					String msgId = ChatUtils.getMessageId(invite);
					if (msgId != null) {
						String remoteInstanceId = null;
						ContactHeader inviteContactHeader = (ContactHeader) invite.getHeader(ContactHeader.NAME);
						if (inviteContactHeader != null) {
							remoteInstanceId = inviteContactHeader.getParameter(SipUtils.SIP_INSTANCE_PARAM);
						}
						// Send message delivery status via a SIP MESSAGE
						getImdnManager().sendMessageDeliveryStatusImmediately(remote, msgId,
								ImdnDocument.DELIVERY_STATUS_DELIVERED, remoteInstanceId);
					}
				}

				// Send a 486 Busy response
				sendErrorResponse(invite, Response.BUSY_HERE);
				return;
			}

			// Save the message
			if (firstMsg != null) {
				MessagingLog.getInstance().addIncomingOneToOneChatMessage(firstMsg);
			}

			// Test number of sessions
			if ((maxChatSessions != 0) && (getImSessions().size() >= maxChatSessions)) {
				if (logger.isActivated()) {
					logger.debug("The max number of chat sessions is achieved: reject the invitation");
				}

				// Send a 486 Busy response
				sendErrorResponse(invite, Response.BUSY_HERE);
				return;
			}

			// Create a new session
			TerminatingOne2OneChatSession session = new TerminatingOne2OneChatSession(this, invite, remote);

			getImsModule().getCore().getListener().handleOneOneChatSessionInvitation(session);

			session.startSession();
			
		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.error( "Cannot parse remote contact");
			}
		}
    }

	/**
	 * Initiate an ad-hoc group chat session
	 * 
	 * @param contacts
	 *            List of contact identifiers
	 * @param subject
	 *            Subject
	 * @return IM session
	 * @throws CoreException
	 */
	public ChatSession initiateAdhocGroupChatSession(List<ContactId> contacts, String subject) throws CoreException {
		if (logger.isActivated()) {
			logger.info("Initiate an ad-hoc group chat session");
		}

		// Test number of sessions
		if ((maxChatSessions != 0) && (getImSessions().size() >= maxChatSessions)) {
			if (logger.isActivated()) {
				logger.debug("The max number of chat sessions is achieved: cancel the initiation");
			}
			throw new CoreException("Max chat sessions achieved");
		}

		Set<ParticipantInfo> participants = ParticipantInfoUtils
				.getParticipantInfos(contacts);

		// Create a new session
		OriginatingAdhocGroupChatSession session = new OriginatingAdhocGroupChatSession(this,
				ImsModule.IMS_USER_PROFILE.getImConferenceUri(), subject, participants);

		return session;
	}

    /**
     * Receive ad-hoc group chat session invitation
     * 
     * @param invite Initial invite
     */
	public void receiveAdhocGroupChatSession(SipRequest invite) {
		if (logger.isActivated()) {
			logger.info("Receive an ad-hoc group chat session invitation");
		}
		ContactId contact = null;
		String remoteUri = null;
		
		try {
			contact = ChatUtils.getReferredIdentityAsContactId(invite);
			// Test if the contact is blocked
			if (ContactsManager.getInstance().isImBlockedForContact(contact)) {
				if (logger.isActivated()) {
					logger.debug("Contact " + contact + " is blocked: automatically reject the chat invitation");
				}

				handleGroupChatInvitationRejected(invite, GroupChat.ReasonCode.REJECTED_SPAM);

				// Send a 486 Busy response
				sendErrorResponse(invite, Response.BUSY_HERE);
				return;
			}
		} catch (RcsContactFormatException e) {
			// GC invitation is out of the blue (i.e. Store & Forward)
			remoteUri = ChatUtils.getReferredIdentityAsContactUri(invite);
			if (logger.isActivated()) {
				logger.info("Receive a forward GC invitation from "+remoteUri);
			}
		}
		Set<ParticipantInfo> participants = ChatUtils.getListOfParticipants(invite);

		// Test number of sessions
		if ((maxChatSessions != 0) && (getImSessions().size() >= maxChatSessions)) {
			if (logger.isActivated()) {
				logger.debug("The max number of chat sessions is achieved: reject the invitation");
			}

			handleGroupChatInvitationRejected(invite, GroupChat.ReasonCode.REJECTED_MAX_CHATS);

			// Send a 486 Busy response
			sendErrorResponse(invite, Response.BUSY_HERE);
			return;
		}

		// Create a new session
		TerminatingAdhocGroupChatSession session = new TerminatingAdhocGroupChatSession(this, invite, contact, remoteUri, participants);

		/*--
		 * 6.3.3.1 Leaving a Group Chat that is idle
		 * In case the user expresses their desire to leave the Group Chat while it is inactive, the device will not offer the user
		 * the possibility any more to enter new messages and restart the chat and automatically decline the first incoming INVITE 
		 * request for the chat with a SIP 603 DECLINE response. Subsequent INVITE requests should not be rejected as they may be
		 * received when the user is added again to the Chat by one of the participants.
		 */
		boolean reject = MessagingLog.getInstance().isGroupChatNextInviteRejected(session.getContributionID());
		if (reject) {
			if (logger.isActivated()) {
				logger.debug("Chat Id " + session.getContributionID()
						+ " is declined since previously terminated by user while disconnected");
			}
			// Send a 603 Decline response
			sendErrorResponse(invite, Response.DECLINE);
			MessagingLog.getInstance().acceptGroupChatNextInvitation(session.getContributionID());
			return;
		}

		getImsModule().getCore().getListener().handleAdhocGroupChatSessionInvitation(session);

		session.startSession();
    }

    /**
     * Rejoin a group chat session
     * 
     * @param chatId Chat ID
     * @return IM session
     * @throws CoreException
     */
    public ChatSession rejoinGroupChatSession(String chatId) throws CoreException {
		if (logger.isActivated()) {
			logger.info("Rejoin group chat session");
		}

		// Test number of sessions
		if ((maxChatSessions != 0) && (getImSessions().size() >= maxChatSessions)) {
			if (logger.isActivated()) {
				logger.debug("The max number of chat sessions is achieved: cancel the initiation");
			}
			throw new CoreException("Max chat sessions achieved");
		}

		// Get the group chat info from database
		GroupChatInfo groupChat = MessagingLog.getInstance().getGroupChatInfo(chatId); 
		if (groupChat == null) {
			if (logger.isActivated()) {
				logger.warn("Group chat " + chatId + " can't be rejoined: conversation not found");
			}
			throw new CoreException("Group chat conversation not found in database");
		}
		if (groupChat.getRejoinId() == null) {
			if (logger.isActivated()) {
				logger.warn("Group chat " + chatId + " can't be rejoined: rejoin ID not found");
			}
			throw new CoreException("Rejoin ID not found in database");
		}

		Set<ParticipantInfo> participants = groupChat.getParticipants(); // Added by Deutsche Telekom AG
		if (participants.size() == 0) {
			if (logger.isActivated()) {
				logger.warn("Group chat " + chatId + " can't be rejoined: participants not found");
			}
			throw new CoreException("Group chat participants not found in database");
		}

		// Create a new session
		if (logger.isActivated()) {
			logger.debug("Rejoin group chat: " + groupChat.toString());
		}

		return new RejoinGroupChatSession(this, groupChat);
    }
    
    /**
     * Restart a group chat session
     * 
     * @param chatId Chat ID
     * @return IM session
     * @throws CoreException
     */
    public ChatSession restartGroupChatSession(String chatId) throws CoreException {
		if (logger.isActivated()) {
			logger.info("Restart group chat session");
		}

		// Test number of sessions
		if ((maxChatSessions != 0) && (getImSessions().size() >= maxChatSessions)) {
			if (logger.isActivated()) {
				logger.debug("The max number of chat sessions is achieved: cancel the initiation");
			}
			throw new CoreException("Max chat sessions achieved");
		}
		
		// Get the group chat info from database
		GroupChatInfo groupChat = MessagingLog.getInstance().getGroupChatInfo(chatId);
		if (groupChat == null) {
			if (logger.isActivated()) {
				logger.warn("Group chat " + chatId + " can't be restarted: conversation not found");
			}
			throw new CoreException("Group chat conversation not found in database");
		}

		// TODO check whether participants of GroupChatInfo cannot be used instead
		
		// Get the connected participants from database
		Set<ParticipantInfo> participants = MessagingLog.getInstance().getGroupChatConnectedParticipants(chatId);
		
		if (participants.size() == 0) {
			if (logger.isActivated()) {
				logger.warn("Group chat " + chatId + " can't be restarted: participants not found");
			}
			throw new CoreException("Group chat participants not found in database");
		}

		// Create a new session
		if (logger.isActivated()) {
			logger.debug("Restart group chat: " + groupChat.toString());
		}

		return new RestartGroupChatSession(this, ImsModule.IMS_USER_PROFILE.getImConferenceUri(), groupChat.getSubject(),
				participants, chatId);
    }    
    
    /**
     * Receive a conference notification
     * 
     * @param notify Received notify
     */
    public void receiveConferenceNotification(SipRequest notify) {
    	// Dispatch the notification to the corresponding session
    	Vector<ChatSession> sessions = getImSessions();
    	for (int i=0; i < sessions.size(); i++) {
    		ChatSession session = (ChatSession)sessions.get(i);
    		if (session instanceof GroupChatSession) {
    			GroupChatSession groupChatSession = (GroupChatSession)session;
	    		if (groupChatSession.getConferenceEventSubscriber().isNotifyForThisSubscriber(notify)) {
	    			groupChatSession.getConferenceEventSubscriber().receiveNotification(notify);
	    		}
    		}
    	}
    }

	/**
     * Receive a message delivery status
     * 
     * @param message Received message
     */
    public void receiveMessageDeliveryStatus(SipRequest message) {
		// Send a 200 OK response
		try {
			if (logger.isActivated()) {
				logger.info("Send 200 OK");
			}
	        SipResponse response = SipMessageFactory.createResponse(message,
	        		IdGenerator.getIdentifier(), 200);
			getImsModule().getSipManager().sendSipResponse(response);
		} catch(Exception e) {
	       	if (logger.isActivated()) {
	    		logger.error("Can't send 200 OK response", e);
	    	}
	       	return;
		}

		try {
			// Parse received message
			ImdnDocument imdn = ChatUtils.parseCpimDeliveryReport(message.getContent());
			if (imdn == null) {
				return;
			}

			ContactId contact = ContactUtils.createContactId(SipUtils.getAssertedIdentity(message));
			String msgId = imdn.getMsgId();
			// Note: FileTransferId is always generated to equal the
			// associated msgId of a FileTransfer invitation message.
			String fileTransferId = msgId;

			// Check if message delivery of a file transfer
			boolean isFileTransfer = MessagingLog.getInstance().isFileTransfer(fileTransferId);
			if (isFileTransfer) {
				// Notify the file delivery outside of the chat session
				receiveFileDeliveryStatus(contact, imdn);
			} else {
				// Get session associated to the contact
				Vector<ChatSession> sessions = Core.getInstance().getImService()
						.getImSessionsWith(contact);
				if (sessions.size() > 0) {
					// Notify the message delivery from the chat session
					for (int i = 0; i < sessions.size(); i++) {
						ChatSession session = sessions.elementAt(i);
						session.handleMessageDeliveryStatus(contact, imdn);
					}
				} else {
					// Notify the message delivery outside of the chat session
					getImsModule().getCore().getListener()
							.handleMessageDeliveryStatus(contact, imdn);
				}
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.warn("Cannot parse message delivery status");
			}
		}
    }

    /**
     * Receive 1-1 file delivery status
     * @param contact Contact identifier
     * @param imdn Imdn document
     */
    public void receiveFileDeliveryStatus(ContactId contact, ImdnDocument imdn){
        // Notify the file delivery outside of the chat session
        getImsModule().getCore().getListener().handleFileDeliveryStatus(contact, imdn);
    }

	/**
	 * Receive group file delivery status
	 *
	 * @param chatId Chat Id
	 * @param contact Contact identifier
	 * @param ImdnDocument imdn Imdn document
	 */
	public void receiveGroupFileDeliveryStatus(String chatId, ContactId contact, ImdnDocument imdn) {
		getImsModule().getCore().getListener()
				.handleGroupFileDeliveryStatus(chatId, contact, imdn);
	}

    /**
     * Receive S&F push messages
     * 
     * @param invite Received invite
     */
    public void receiveStoredAndForwardPushMessages(SipRequest invite) {
    	if (logger.isActivated()) {
			logger.debug("Receive S&F push messages invitation");
		}
		ContactId remote;
		try {
			remote = ChatUtils.getReferredIdentityAsContactId(invite);
		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.error("Cannot parse remote contact");
			}
			return;
		}
		// Discard invitation if message ID is already received
		InstantMessage firstMsg = ChatUtils.getFirstMessage(invite);
		if (firstMsg != null) {
			String msgId = ChatUtils.getMessageId(invite);
			if (msgId != null) {
				if (MessagingLog.getInstance().isMessagePersisted(msgId)) {
					// Send a 603 Decline response
					sendErrorResponse(invite, Response.DECLINE);
					return;
				}
			}
		}
		
    	// Test if the contact is blocked
	    if (ContactsManager.getInstance().isImBlockedForContact(remote)) {
			if (logger.isActivated()) {
				logger.debug("Contact " + remote + " is blocked: automatically reject the S&F invitation");
			}

			// Send a 486 Busy response
			sendErrorResponse(invite, 486);
			return;
	    }
	    
		// Save the message
		if (firstMsg != null) {
			MessagingLog.getInstance().addIncomingOneToOneChatMessage(firstMsg);
		}
    	
		// Create a new session
    	getStoreAndForwardManager().receiveStoredMessages(invite, remote);
    }
    	
    /**
     * Receive S&F push notifications
     * 
     * @param invite Received invite
     */
    public void receiveStoredAndForwardPushNotifications(SipRequest invite) {
    	if (logger.isActivated()) {
			logger.debug("Receive S&F push notifications invitation");
		}    	
    	ContactId remote;
		try {
			remote = ChatUtils.getReferredIdentityAsContactId(invite);
		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.error("Cannot parse remote contact");
			}
			return;
		}
    	// Test if the contact is blocked
	    if (ContactsManager.getInstance().isImBlockedForContact(remote)) {
			if (logger.isActivated()) {
				logger.debug("Contact " + remote + " is blocked: automatically reject the S&F invitation");
			}

			// Send a 486 Busy response
			sendErrorResponse(invite, 486);
			return;
	    }
    	
		// Create a new session
    	getStoreAndForwardManager().receiveStoredNotifications(invite,remote);
    }
	
    /**
     * Receive HTTP file transfer invitation
     *
     * @param invite Received invite
     * @param ftinfo File transfer info document
     */
	public void receiveOneToOneHttpFileTranferInvitation(SipRequest invite, FileTransferHttpInfoDocument ftinfo) {
		if (logger.isActivated()){
			logger.info("Receive a single HTTP file transfer invitation");
		}

		try {
			ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);
			// Test if the contact is blocked
			if (ContactsManager.getInstance().isFtBlockedForContact(remote)) {
				if (logger.isActivated()) {
					logger.debug("Contact " + remote + " is blocked, automatically reject the HTTP File transfer");
				}

				handleFileTransferInvitationRejected(invite, FileTransfer.ReasonCode.REJECTED_SPAM);
				// Send a 603 Decline response
				sendErrorResponse(invite, Response.DECLINE);
				return;
			}

			// Test number of sessions
			if ((maxFtSessions != 0) && (getFileTransferSessions().size() >= maxFtSessions)) {
				if (logger.isActivated()) {
					logger.debug("The max number of FT sessions is achieved, reject the HTTP File transfer");
				}

				handleFileTransferInvitationRejected(invite, FileTransfer.ReasonCode.REJECTED_MAX_FILE_TRANSFERS);
				// Send a 603 Decline response
				sendErrorResponse(invite, 603);
				return;
			}

			// Reject if file is too big or size exceeds device storage capacity. This control should be done
			// on UI. It is done after end user accepts invitation to enable prior handling by the application.
			FileSharingError error = FileSharingSession.isFileCapacityAcceptable(ftinfo.getFileSize());
			if (error != null) {
				// Send a 603 Decline response
				sendErrorResponse(invite, 603);
				int errorCode = error.getErrorCode();
				switch (errorCode) {
					case FileSharingError.MEDIA_SIZE_TOO_BIG:
						handleFileTransferInvitationRejected(invite,
								FileTransfer.ReasonCode.REJECTED_MAX_SIZE);
					case FileSharingError.NOT_ENOUGH_STORAGE_SPACE:
						handleFileTransferInvitationRejected(invite,
								FileTransfer.ReasonCode.REJECTED_LOW_SPACE);
					default:
						if (logger.isActivated()) {
							logger.error("Encountered unexpected error while receiving HTTP file transfer invitation"
									+ errorCode);
						}
				}
				return;
			}

			// Create and start a chat session
			TerminatingOne2OneChatSession oneToOneChatSession = new TerminatingOne2OneChatSession(this, invite, remote);
			oneToOneChatSession.startSession();

			// Create and start a new HTTP file transfer session
			TerminatingHttpFileSharingSession httpFiletransferSession = new TerminatingHttpFileSharingSession(this,
					oneToOneChatSession, ftinfo, ChatUtils.getMessageId(invite), oneToOneChatSession.getRemoteContact(),
					oneToOneChatSession.getRemoteDisplayName());

			getImsModule().getCore().getListener().handleOneToOneFileTransferInvitation(httpFiletransferSession, oneToOneChatSession);

			httpFiletransferSession.startSession();

		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.error( "receiveHttpFileTranferInvitation: cannot parse remote contact");
			}
		}
	}

	/**
     * Receive S&F HTTP file transfer invitation
     *
     * @param invite Received invite
     * @param ftinfo File transfer info document
     */
    public void receiveStoredAndForwardOneToOneHttpFileTranferInvitation(SipRequest invite, FileTransferHttpInfoDocument ftinfo) {
        if (logger.isActivated()) {
            logger.info("Receive a single S&F HTTP file transfer invitation");
        }
        ContactId remote;
		try {
			remote = ChatUtils.getReferredIdentityAsContactId(invite);
		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.error("receiveStoredAndForwardHttpFileTranferInvitation: cannot parse remote contact");
			}
			return;
		}

        // Create and start a chat session
        TerminatingStoreAndForwardMsgSession one2oneChatSession = new TerminatingStoreAndForwardMsgSession(this, invite,remote);
        one2oneChatSession.startSession();
        
        // Auto reject if file too big
        if (isFileSizeExceeded(ftinfo.getFileSize())) {
            if (logger.isActivated()) {
                logger.debug("File is too big, reject file transfer invitation");
            }

            // Send a 403 Decline response
            //TODO add warning header "xxx Size exceeded"
            one2oneChatSession.sendErrorResponse(invite, one2oneChatSession.getDialogPath().getLocalTag(), 403);

            // Close session
            one2oneChatSession.handleError(new FileSharingError(FileSharingError.MEDIA_SIZE_TOO_BIG));
            return;
        }
        
        // Create and start a new HTTP file transfer session
		TerminatingHttpFileSharingSession httpFiletransferSession = new TerminatingHttpFileSharingSession(this, one2oneChatSession,
				ftinfo, ChatUtils.getMessageId(invite), one2oneChatSession.getRemoteContact(),
				one2oneChatSession.getRemoteDisplayName());

		getImsModule().getCore().getListener().handleOneToOneFileTransferInvitation(httpFiletransferSession, one2oneChatSession);

        httpFiletransferSession.startSession();
    }
	
    /**
     * Check whether file size exceeds the limit
     * 
     * @param size of file
     * @return {@code true} if file size limit is exceeded, otherwise {@code false}
     */
    private boolean isFileSizeExceeded(long size) {
        // Auto reject if file too big
        int maxSize = FileSharingSession.getMaxFileSharingSize();
        if (maxSize > 0 && size > maxSize) {
            return true;
        }

        return false;
    }
}