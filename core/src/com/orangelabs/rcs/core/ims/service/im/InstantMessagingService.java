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

package com.orangelabs.rcs.core.ims.service.im;

import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax2.sip.header.ContactHeader;
import javax2.sip.message.Response;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.CoreException;
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
import com.orangelabs.rcs.core.ims.service.im.filetransfer.OriginatingFileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.TerminatingFileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FtHttpResumeManager;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.OriginatingHttpFileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.OriginatingHttpGroupFileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.TerminatingHttpFileSharingSession;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.StringUtils;
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
	private ImdnManager imdnMgr = null;	
	
	private FtHttpResumeManager resumeManager = null;
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

		this.maxChatSessions = RcsSettings.getInstance().getMaxChatSessions();
        this.maxFtSessions = RcsSettings.getInstance().getMaxFileTransferSessions();
        this.maxFtSize = FileSharingSession.getMaxFileSharingSize();
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
	public Vector<ChatSession> getImSessionsWith(String contact) {
		// Search all IM sessions
		Vector<ChatSession> result = new Vector<ChatSession>();
		Enumeration<ImsServiceSession> list = getSessions();
		while(list.hasMoreElements()) {
			ImsServiceSession session = list.nextElement();
			if ((session instanceof OneOneChatSession) && PhoneUtils.compareNumbers(session.getRemoteContact(), contact)) {
				result.add((ChatSession)session);
			}
		}

		return result;
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
	 *            Remote contact
	 * @param content
	 *            Content of file to sent
	 * @param tryAttachThumbnail
	 *            true if the stack must try to attach thumbnail
	 * @return File transfer session
	 * @throws CoreException
	 */
	public FileSharingSession initiateFileTransferSession(String contact, MmContent content, boolean tryAttachThumbnail) throws CoreException {
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
			if (RcsSettings.getInstance().getFtProtocol().equals(RcsSettingsData.FT_PROTOCOL_HTTP)) {
				isHttpProtocol = true;
			}
		}

		if (tryAttachThumbnail && (content.getEncoding().startsWith("image/") == false)) {
			tryAttachThumbnail = false;
		}

		// Initiate session
		FileSharingSession session;
		if (isHttpProtocol) {
			// Create a new session
			session = new OriginatingHttpFileSharingSession(this, content, PhoneUtils.formatNumberToSipUri(contact), tryAttachThumbnail);
		} else {
			if (tryAttachThumbnail) {
				// Check thumbnail capabilities
				if (capability != null && capability.isFileTransferThumbnailSupported() == false) {
					tryAttachThumbnail = false;
					if (logger.isActivated()) {
						logger.warn("Thumbnail not supported by remote");
					}
				}
				if (myCapability.isFileTransferThumbnailSupported() == false) {
					tryAttachThumbnail = false;
					if (logger.isActivated()) {
						logger.warn("Thumbnail not supported !");
					}
				}
			}
			// Create a new session
			session = new OriginatingFileSharingSession(this, content, PhoneUtils.formatNumberToSipUri(contact), tryAttachThumbnail);
		}
		return session;
	}
	
	/**
	 * Initiate a group file transfer session
	 * 
	 * @param contacts
	 *            List of remote contacts
	 * @param content
	 *            The file content to be sent
	 * @param tryAttachThumbnail
	 *            true if the stack must try to attach thumbnail
	 * @param chatSessionId
	 *            Chat session ID
	 * @param chatContributionId
	 *            Chat contribution ID
	 * @return File transfer session
	 * @throws CoreException
	 */
	public FileSharingSession initiateGroupFileTransferSession(Set<ParticipantInfo> contactList, MmContent content, boolean tryAttachThumbnail,
			String chatSessionId, String chatContributionId) throws CoreException {
		if (logger.isActivated()) {
			logger.info("Send file " + content.toString() + " to " + contactList.size() + " contacts");
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

		// Create a new session
		FileSharingSession session = new OriginatingHttpGroupFileSharingSession(this, content, tryAttachThumbnail,
				ImsModule.IMS_USER_PROFILE.getImConferenceUri(), contactList, chatSessionId,
				chatContributionId);

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

		// Test if the contact is blocked
		String remote = SipUtils.getAssertedIdentity(invite);
	    if (ContactsManager.getInstance().isFtBlockedForContact(remote)) {
			if (logger.isActivated()) {
				logger.debug("Contact " + remote + " is blocked: automatically reject the file transfer invitation");
			}
			
			// Send a 603 Decline response
			sendErrorResponse(invite, Response.DECLINE);
			return;
	    }

		// Test number of sessions
		if ((maxFtSessions != 0) && (getFileTransferSessions().size() >= maxFtSessions)) {
			if (logger.isActivated()) {
				logger.debug("The max number of file transfer sessions is achieved: reject the invitation");
			}
			
			// Send a 603 Decline response
			sendErrorResponse(invite, Response.DECLINE);
			return;
		}

    	// Create a new session
		FileSharingSession session = new TerminatingFileSharingSession(this, invite);

		// Start the session
		session.startSession();

		// Notify listener
		getImsModule().getCore().getListener().handleFileTransferInvitation(session, false);
	}

    /**
     * Initiate a one-to-one chat session
     * 
     * @param contact Remote contact
     * @param firstMsg First message
     * @return IM session
     * @throws CoreException
     */
	public ChatSession initiateOne2OneChatSession(String contact, InstantMessage firstMsg)
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
		String number = PhoneUtils.formatNumberToSipUri(contact);
		// Create a new session
		OriginatingOne2OneChatSession session = new OriginatingOne2OneChatSession(this, number, firstMsg);
		// Save the message
		if (firstMsg != null) {
			RichMessagingHistory.getInstance().addChatMessage(firstMsg, ChatLog.Message.Direction.OUTGOING);
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

		String remote = ChatUtils.getReferredIdentity(invite);
		// Discard invitation if message ID is already received
		InstantMessage firstMsg = ChatUtils.getFirstMessage(invite);
		if (firstMsg != null) {
			String msgId = ChatUtils.getMessageId(invite);
			if (msgId != null) {
				if (RichMessagingHistory.getInstance().isNewMessage(remote, msgId) == false) {
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
				RichMessagingHistory.getInstance().addSpamMessage(firstMsg);
			}

			// Send message delivery report if requested
			if (ChatUtils.isImdnDeliveredRequested(invite)) {
				// Check notification disposition
				String msgId = ChatUtils.getMessageId(invite);
				if (msgId != null) {
                    String remoteInstanceId = null;
                    ContactHeader inviteContactHeader = (ContactHeader)invite.getHeader(ContactHeader.NAME);
                    if (inviteContactHeader != null) {
                        remoteInstanceId = inviteContactHeader.getParameter(SipUtils.SIP_INSTANCE_PARAM);
                    }
					// Send message delivery status via a SIP MESSAGE
					getImdnManager().sendMessageDeliveryStatusImmediately(SipUtils.getAssertedIdentity(invite),
							msgId, ImdnDocument.DELIVERY_STATUS_DELIVERED, remoteInstanceId);
				}
			}
			
			// Send a 486 Busy response
			sendErrorResponse(invite, Response.BUSY_HERE);
			return;
	    }

		// Save the message
		if (firstMsg != null) {
			RichMessagingHistory.getInstance().addChatMessage(firstMsg, ChatLog.Message.Direction.INCOMING);
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
		TerminatingOne2OneChatSession session = new TerminatingOne2OneChatSession(this, invite);

		// Start the session
		session.startSession();

		// Notify listener
		getImsModule().getCore().getListener().handleOneOneChatSessionInvitation(session);
    }

    /**
     * Initiate an ad-hoc group chat session
     * 
     * @param contacts List of contacts
     * @param subject Subject
     * @return IM session
     * @throws CoreException
     */
    public ChatSession initiateAdhocGroupChatSession(List<String> contacts, String subject) throws CoreException {
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

		// Create a new session
		OriginatingAdhocGroupChatSession session = new OriginatingAdhocGroupChatSession(
				this,
				ImsModule.IMS_USER_PROFILE.getImConferenceUri(),
				subject,
				ParticipantInfoUtils.getParticipantInfoFromContacts(contacts));

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

		// Test if the contact is blocked
		String remote = ChatUtils.getReferredIdentity(invite);
	    if (ContactsManager.getInstance().isImBlockedForContact(remote)) {
			if (logger.isActivated()) {
				logger.debug("Contact " + remote + " is blocked: automatically reject the chat invitation");
			}
			
			// Send a 486 Busy response
			sendErrorResponse(invite, Response.BUSY_HERE);
			return;
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
		TerminatingAdhocGroupChatSession session = new TerminatingAdhocGroupChatSession(this, invite);

		/*--
		 * 6.3.3.1 Leaving a Group Chat that is idle
		 * In case the user expresses their desire to leave the Group Chat while it is inactive, the device will not offer the user
		 * the possibility any more to enter new messages and restart the chat and automatically decline the first incoming INVITE 
		 * request for the chat with a SIP 603 DECLINE response. Subsequent INVITE requests should not be rejected as they may be
		 * received when the user is added again to the Chat by one of the participants.
		 */
		boolean reject = RichMessagingHistory.getInstance().isGroupChatNextInviteRejected(session.getContributionID());
		if (reject) {
			if (logger.isActivated()) {
				logger.debug("Chat Id " + session.getContributionID() + " is declined since previously terminated by user while disconnected");
			}
			// Send a 603 Decline response
			sendErrorResponse(invite, Response.DECLINE);
			RichMessagingHistory.getInstance().acceptGroupChatNextInvitation(session.getContributionID());
			return;
		}

		
		// Start the session
		session.startSession();

		// Notify listener
		getImsModule().getCore().getListener().handleAdhocGroupChatSessionInvitation(session);
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
		GroupChatInfo groupChat = RichMessagingHistory.getInstance().getGroupChatInfo(chatId); 
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
		GroupChatInfo groupChat = RichMessagingHistory.getInstance().getGroupChatInfo(chatId);
		if (groupChat == null) {
			if (logger.isActivated()) {
				logger.warn("Group chat " + chatId + " can't be restarted: conversation not found");
			}
			throw new CoreException("Group chat conversation not found in database");
		}

		// TODO check whether participants of GroupChatInfo cannot be used instead
		
		// Get the connected participants from database
		Set<ParticipantInfo> participants = RichMessagingHistory.getInstance().getGroupChatConnectedParticipants(chatId);
		
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

		// Parse received message
		ImdnDocument imdn = ChatUtils.parseCpimDeliveryReport(message.getContent());
    	if ((imdn != null) && (imdn.getMsgId() != null) && (imdn.getStatus() != null)) {
	    	String contact = SipUtils.getAssertedIdentity(message);
	    	String status = imdn.getStatus();
	    	String msgId = imdn.getMsgId();

            // Check if message delivery of a file transfer
            String ftSessionId = RichMessagingHistory.getInstance().getFileTransferId(msgId);
            if (!StringUtils.isEmpty(ftSessionId)) {
                // Notify the file delivery outside of the chat session
                receiveFileDeliveryStatus(ftSessionId, status, contact);
            } else {
    			// Get session associated to the contact
    			Vector<ChatSession> sessions = Core.getInstance().getImService().getImSessionsWith(contact);
    			if (sessions.size() > 0) {
    				// Notify the message delivery from the chat session
    				for(int i=0; i < sessions.size(); i++) {
    					ChatSession session = sessions.elementAt(i);
    			 	    session.handleMessageDeliveryStatus(msgId, status, contact);
    				}
    			} else {
    				// Notify the message delivery outside of the chat session
    				getImsModule().getCore().getListener().handleMessageDeliveryStatus(contact, msgId, status);
    			}
            }
        }
    }

    /**
     * @param ftSessionId
     * @param status
     * @param contact
     */
    public void receiveFileDeliveryStatus(String ftSessionId, String status, String contact) {
        // Notify the file delivery outside of the chat session
        getImsModule().getCore().getListener().handleFileDeliveryStatus(ftSessionId, status, contact);
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

		String remote = ChatUtils.getReferredIdentity(invite);
		// Discard invitation if message ID is already received
		InstantMessage firstMsg = ChatUtils.getFirstMessage(invite);
		if (firstMsg != null) {
			String msgId = ChatUtils.getMessageId(invite);
			if (msgId != null) {
				if (RichMessagingHistory.getInstance().isNewMessage(remote, msgId) == false) {
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
    	
		// Create a new session
    	getStoreAndForwardManager().receiveStoredMessages(invite);
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
    	
    	// Test if the contact is blocked
    	String remote = ChatUtils.getReferredIdentity(invite);
	    if (ContactsManager.getInstance().isImBlockedForContact(remote)) {
			if (logger.isActivated()) {
				logger.debug("Contact " + remote + " is blocked: automatically reject the S&F invitation");
			}

			// Send a 486 Busy response
			sendErrorResponse(invite, 486);
			return;
	    }
    	
		// Create a new session
    	getStoreAndForwardManager().receiveStoredNotifications(invite);
    }
	
    /**
     * Receive HTTP file transfer invitation
     *
     * @param invite Received invite
     * @param ftinfo File transfer info document
     */
	public void receiveHttpFileTranferInvitation(SipRequest invite, FileTransferHttpInfoDocument ftinfo) {
		if (logger.isActivated()){
			logger.info("Receive a single HTTP file transfer invitation");
		}

		// Test if the contact is blocked
		String remote = ChatUtils.getReferredIdentity(invite);
	    if (ContactsManager.getInstance().isFtBlockedForContact(remote)) {
			if (logger.isActivated()) {
				logger.debug("Contact " + remote + " is blocked, automatically reject the HTTP File transfer");
			}

			// Send a 603 Decline response
			sendErrorResponse(invite, Response.DECLINE);
			return;
	    }

		// Test number of sessions
		if ((maxFtSessions != 0) && (getFileTransferSessions().size() >= maxFtSessions)) {
			if (logger.isActivated()) {
				logger.debug("The max number of FT sessions is achieved, reject the HTTP File transfer");
			}

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
            return;
        }

		// Save the first message
		RichMessagingHistory.getInstance().addChatMessage(ChatUtils.getFirstMessage(invite), ChatLog.Message.Direction.INCOMING);

        // Create and start a chat session
        TerminatingOne2OneChatSession one2oneChatSession = new TerminatingOne2OneChatSession(this, invite);
        one2oneChatSession.startSession();
        
		// Create and start a new HTTP file transfer session
		TerminatingHttpFileSharingSession httpFiletransferSession = new TerminatingHttpFileSharingSession(this, one2oneChatSession,
				ftinfo, ChatUtils.getMessageId(invite), one2oneChatSession.getRemoteContact());
        httpFiletransferSession.startSession();
        
        // Notify listener
        getImsModule().getCore().getListener().handle1to1FileTransferInvitation(httpFiletransferSession, one2oneChatSession);
	}

	/**
     * Receive S&F HTTP file transfer invitation
     *
     * @param invite Received invite
     * @param ftinfo File transfer info document
     */
    public void receiveStoredAndForwardHttpFileTranferInvitation(SipRequest invite, FileTransferHttpInfoDocument ftinfo) {
        if (logger.isActivated()) {
            logger.info("Receive a single S&F HTTP file transfer invitation");
        }

        // Create and start a chat session
        TerminatingStoreAndForwardMsgSession one2oneChatSession = new TerminatingStoreAndForwardMsgSession(this, invite);
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
				ftinfo, ChatUtils.getMessageId(invite), one2oneChatSession.getRemoteContact());
        httpFiletransferSession.startSession();
        
        // Notify listener
        getImsModule().getCore().getListener().handle1to1FileTransferInvitation(httpFiletransferSession, one2oneChatSession);
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