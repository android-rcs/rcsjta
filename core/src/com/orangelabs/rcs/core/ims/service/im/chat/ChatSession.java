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

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpManager;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimParser;
import com.orangelabs.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingManager;
import com.orangelabs.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardMsgSession;
import com.orangelabs.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardNotifSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.TerminatingHttpFileSharingSession;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Chat session
 *
 * @author jexa7410
 */
public abstract class ChatSession extends ImsServiceSession implements MsrpEventListener {
	/**
	 * Subject
	 */
	private String subject;

	/**
	 * List of participants
	 */
	private Set<ParticipantInfo> participants = new HashSet<ParticipantInfo>();

	/**
	 * MSRP manager
	 */
	private final MsrpManager mMsrpMgr;

	/**
	 * Is composing manager
	 */
	private final IsComposingManager mIsComposingMgr = new IsComposingManager(this);

	/**
	 * Chat activity manager
	 */
	private final ChatActivityManager mActivityMgr = new ChatActivityManager(this);

    /**
     * Max number of participants in the session
     */
    private int maxParticipants = RcsSettings.getInstance().getMaxChatParticipants();

    /**
     * Contribution ID
     */
    private String contributionId;

    /**
     * Feature tags
     */
    private List<String> featureTags = new ArrayList<String>();

    /**
     * Feature tags
     */
    private List<String> acceptContactTags = new ArrayList<String>();

    /**
     * Accept types
     */
    private String acceptTypes;

    /**
     * Wrapped types
     */
    private String wrappedTypes;

    /**
     * Geolocation push supported by remote
     */
    private boolean geolocSupportedByRemote = false;

    /**
     * File transfer supported by remote
     */
    private boolean ftSupportedByRemote = false;

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(ChatSession.class.getSimpleName());

    protected final RcsSettings mRcsSettings;

    private final MessagingLog mMessagingLog;

	/**
	 * Receive chat message
	 *
	 * @param msg Chat message
	 * @param imdnDisplayedRequested Indicates whether display report was requested
	 */
	protected void receive(ChatMessage msg, boolean imdnDisplayedRequested) {
		if (mMessagingLog.isMessagePersisted(msg.getMessageId())) {
			// Message already received
			return;
		}

		// Is composing event is reset
		mIsComposingMgr.receiveIsComposingEvent(msg.getRemoteContact(), false);

		for (ImsSessionListener listener : getListeners()) {
			((ChatSessionListener)listener).handleReceiveMessage(msg, imdnDisplayedRequested);
		}
	}

    /**
	 * Constructor
	 *
	 * @param parent IMS service
     * @param contact Remote contactId
     * @param remoteUri Remote URI
     * @param participants List of participants
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
	 */
	public ChatSession(ImsService parent, ContactId contact, String remoteUri, Set<ParticipantInfo> participants, RcsSettings rcsSettings, MessagingLog messagingLog) {
		super(parent, contact, remoteUri);

		mRcsSettings = rcsSettings;
		mMessagingLog = messagingLog;
		// Set the session participants
		this.participants = participants;

        // Create the MSRP manager
		int localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort();
		String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface().getNetworkAccess().getIpAddress();
		mMsrpMgr = new MsrpManager(localIpAddress, localMsrpPort,parent);
		if (parent.getImsModule().isConnectedToWifiAccess()) {
			mMsrpMgr.setSecured(RcsSettings.getInstance().isSecureMsrpOverWifi());
		}
	}

	/**
	 * Get feature tags
	 *
	 * @return Feature tags
	 */
	public String[] getFeatureTags() {
		return featureTags.toArray(new String[featureTags.size()]);
	}

	/**
     * Get Accept-Contact tags
     *
     * @return Feature tags
     */
    public String[] getAcceptContactTags() {
        return acceptContactTags.toArray(new String[acceptContactTags.size()]);
    }

    /**
	 * Set feature tags
	 *
	 * @param tags Feature tags
	 */
	public void setFeatureTags(List<String> tags) {
		this.featureTags = tags;
	}

	/**
     * Set Accept-Contact tags
     *
     * @param tags Feature tags
     */
    public void setAcceptContactTags(List<String> tags) {
        this.acceptContactTags = tags;
    }

    /**
	 * Get accept types
	 *
	 * @return Accept types
	 */
	public String getAcceptTypes() {
		return acceptTypes;
	}

	/**
	 * Set accept types
	 *
	 * @param types Accept types
	 */
	public void setAcceptTypes(String types) {
		this.acceptTypes = types;
	}

	/**
	 * Get wrapped types
	 *
	 * @return Wrapped types
	 */
	public String getWrappedTypes() {
		return wrappedTypes;
	}

	/**
	 * Set wrapped types
	 *
	 * @param types Wrapped types
	 */
	public void setWrappedTypes(String types) {
		this.wrappedTypes = types;
	}

    /**
     * Returns the subject of the session
     *
     * @return String
     */
    public String getSubject() {
    	return subject;
    }

    /**
     * Set the subject of the session
     *
     * @param subject Subject
     */
    public void setSubject(String subject) {
    	this.subject = subject;
    }

	/**
	 * Returns the IMDN manager
	 *
	 * @return IMDN manager
	 */
	public ImdnManager getImdnManager() {
		return ((InstantMessagingService)getImsService()).getImdnManager();
	}

	/**
	 * Returns the session activity manager
	 *
	 * @return Activity manager
	 */
	public ChatActivityManager getActivityManager() {
		return mActivityMgr;
	}

	/**
	 * Return the contribution ID
	 *
	 * @return Contribution ID
	 */
	public String getContributionID() {
		return contributionId;
	}

	/**
	 * Set the contribution ID
	 *
	 * @param id Contribution ID
	 */
	public void setContributionID(String id) {
		this.contributionId = id;
	}

	/**
	 * Returns the list of participants
	 *
	 * @return List of participants
	 */
    public Set<ParticipantInfo> getParticipants() {
		return participants;
	}

	/**
	 * Returns the list of participants currently connected to the session
	 *
	 * @return List of participants
	 */
    public abstract Set<ParticipantInfo> getConnectedParticipants();

    /**
	 * Returns the IM session identity
	 *
	 * @return Identity (e.g. SIP-URI)
	 */
	public String getImSessionIdentity() {
		if (getDialogPath() != null) {
			return getDialogPath().getTarget();
		} else {
			return null;
		}
	}

	/**
	 * Returns the MSRP manager
	 *
	 * @return MSRP manager
	 */
	public MsrpManager getMsrpMgr() {
		return mMsrpMgr;
	}

	/**
	 * Is geolocation supported by remote
	 *
	 * @return Boolean
	 */
	public boolean isGeolocSupportedByRemote() {
		return geolocSupportedByRemote;
	}

	/**
	 * Set geolocation supported by remote
	 *
	 * @param supported Supported
	 */
	public void setGeolocSupportedByRemote(boolean supported) {
		this.geolocSupportedByRemote = supported;
	}

	/**
	 * Is file transfer supported by remote
	 *
	 * @return Boolean
	 */
	public boolean isFileTransferSupportedByRemote() {
		return ftSupportedByRemote;
	}

	/**
	 * Set file transfer supported by remote
	 *
	 * @param supported Supported
	 */
	public void setFileTransferSupportedByRemote(boolean supported) {
		this.ftSupportedByRemote = supported;
	}

	/**
	 * Close the MSRP session
	 */
	public void closeMsrpSession() {
    	if (getMsrpMgr() != null) {
    		getMsrpMgr().closeSession();
			if (logger.isActivated()) {
				logger.debug("MSRP session has been closed");
			}
    	}
	}

	/**
	 * Handle error
	 *
	 * @param error Error
	 */
	public void handleError(ImsServiceError error) {
		if (isSessionInterrupted()) {
			return;
		}

		// Error
		if (logger.isActivated()) {
			logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
		}

		// Close media session
		closeMediaSession();

		// Remove the current session
		removeSession();

		for (int i = 0; i < getListeners().size(); i++) {
			((ChatSessionListener) getListeners().get(i)).handleImError(new ChatError(error));
		}
	}

	/**
     * Handle 480 Temporarily Unavailable
     *
     * @param resp 480 response
     */
    public void handle480Unavailable(SipResponse resp) {
        handleError(new ChatError(ChatError.SESSION_INITIATION_DECLINED, resp.getReasonPhrase()));
    }

    /**
     * Handle 486 Busy
     *
     * @param resp 486 response
     */
    public void handle486Busy(SipResponse resp) {
        handleError(new ChatError(ChatError.SESSION_INITIATION_DECLINED, resp.getReasonPhrase()));
    }

    /**
     * Handle 603 Decline
     *
     * @param resp 603 response
     */
    public void handle603Declined(SipResponse resp) {
        handleDefaultError(resp);
    }

	/**
	 * Data has been transfered
	 *
	 * @param msgId Message ID
	 */
	public void msrpDataTransfered(String msgId) {
    	if (logger.isActivated()) {
    		logger.info("Data transfered");
    	}

		// Update the activity manager
		mActivityMgr.updateActivity();
	}

	/**
	 * Data transfer has been received
	 *
	 * @param msgId Message ID
	 * @param data Received data
	 * @param mimeType Data mime-type
	 */
	public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
    	if (logger.isActivated()) {
    		logger.info("Data received (type " + mimeType + ")");
    	}

		// Update the activity manager
		mActivityMgr.updateActivity();

    	if ((data == null) || (data.length == 0)) {
    		// By-pass empty data
        	if (logger.isActivated()) {
        		logger.debug("By-pass received empty data");
        	}
    		return;
    	}

		if (ChatUtils.isApplicationIsComposingType(mimeType)) {
		    // Is composing event
			receiveIsComposing(getRemoteContact(), data);
			return;
		}
		if (ChatUtils.isTextPlainType(mimeType)) {
	    	// Text message

			ChatMessage msg = new ChatMessage(msgId, getRemoteContact(), new String(data,
					UTF8), MimeType.TEXT_MESSAGE, new Date(),
					null);
			boolean imdnDisplayedRequested = false;
			receive(msg, imdnDisplayedRequested);
			return;
		}
		if (ChatUtils.isMessageCpimType(mimeType)) {
	    	// Receive a CPIM message
			try {
    			CpimParser cpimParser = new CpimParser(data);
				CpimMessage cpimMsg = cpimParser.getCpimMessage();
				if (cpimMsg != null) {
			    	Date date = cpimMsg.getMessageDate();
			    	String cpimMsgId = cpimMsg.getHeader(ImdnUtils.HEADER_IMDN_MSG_ID);
                    if (cpimMsgId == null) {
                        cpimMsgId = msgId;
                    }

					String contentType = cpimMsg.getContentType();
					ContactId contact = getRemoteContact();
					// In One to One chat, the MSRP 'from' header is '<sip:anonymous@anonymous.invalid>'

					// Check if the message needs a delivery report
	    			boolean imdnDisplayedRequested = false;

	    			String dispositionNotification = cpimMsg.getHeader(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);
					if (!isGroupChat()) {
						// There is no display notification in Group Chat
						if (dispositionNotification != null && dispositionNotification.contains(ImdnDocument.DISPLAY)) {
							// Check if respond to displayed delivery report is enabled
							if (RcsSettings.getInstance().isRespondToDisplayReports()) {
								imdnDisplayedRequested = true;
							}
						}
					}

                    boolean isFToHTTP = FileTransferUtils.isFileTransferHttpType(contentType);

			    	// Analyze received message thanks to the MIME type
                    if (isFToHTTP) {
						// File transfer over HTTP message
						// Parse HTTP document
						FileTransferHttpInfoDocument fileInfo = FileTransferUtils.parseFileTransferHttpDocument(cpimMsg
								.getMessageContent().getBytes(UTF8));
						if (fileInfo != null) {
							receiveHttpFileTransfer(contact, getRemoteDisplayName(), fileInfo, cpimMsgId);
							// Mark the message as waiting a displayed report if needed
							if (imdnDisplayedRequested) {
								// TODO set File Transfer status to DISPLAY_REPORT_REQUESTED ??
							}
						} else {
							// TODO : else return error to Originating side
						}
					} else {
						if (ChatUtils.isTextPlainType(contentType)) {
							ChatMessage msg = new ChatMessage(cpimMsgId, contact,
									cpimMsg.getMessageContent(), MimeType.TEXT_MESSAGE, date, null);
							receive(msg, imdnDisplayedRequested);
						} else if (ChatUtils.isApplicationIsComposingType(contentType)) {
							receiveIsComposing(contact, cpimMsg.getMessageContent().getBytes(UTF8));
						} else if (ChatUtils.isMessageImdnType(contentType)) {
							receiveMessageDeliveryStatus(contact, cpimMsg.getMessageContent());
						} else if (ChatUtils.isGeolocType(contentType)) {
							ChatMessage msg = new ChatMessage(cpimMsgId, contact,
									cpimMsg.getMessageContent(), GeolocInfoDocument.MIME_TYPE,
									date, null);
							receive(msg, imdnDisplayedRequested);
						}
					}

					if (isFToHTTP) {
						sendMsrpMessageDeliveryStatus(null, cpimMsgId, ImdnDocument.DELIVERY_STATUS_DELIVERED);
					} else {
						if (dispositionNotification != null) {
							if (dispositionNotification.contains(ImdnDocument.POSITIVE_DELIVERY)) {
								// Positive delivery requested, send MSRP message with status "delivered"
								sendMsrpMessageDeliveryStatus(null, cpimMsgId, ImdnDocument.DELIVERY_STATUS_DELIVERED);
							}
						}
					}
				}
	    	} catch(Exception e) {
		   		if (logger.isActivated()) {
		   			logger.error("Can't parse the CPIM message", e);
		   		}
		   	}
		} else {
			// Not supported content
        	if (logger.isActivated()) {
        		logger.debug("Not supported content " + mimeType + " in chat session");
        	}
		}
	}

	/**
	 * Data transfer in progress
	 *
	 * @param currentSize Current transfered size in bytes
	 * @param totalSize Total size in bytes
	 */
	public void msrpTransferProgress(long currentSize, long totalSize) {
		// Not used by chat
	}

    /**
     * Data transfer in progress
     *
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     * @param data received data chunk
     * @return always false TODO
     */
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        // Not used by chat
        return false;
    }

	/**
	 * Data transfer has been aborted
	 */
	public void msrpTransferAborted() {
    	// Not used by chat
	}

    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.protocol.msrp.MsrpEventListener#msrpTransferError(java.lang.String, java.lang.String, com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk)
     */
	@Override
    public void msrpTransferError(String msgId, String error, TypeMsrpChunk typeMsrpChunk) {
		if (isSessionInterrupted()) {
			return;
		}
		if (logger.isActivated()) {
			logger.info("Data transfer error " + error + " for message " + msgId + " (MSRP chunk type: " + typeMsrpChunk + ")");
        }

        // Changed by Deutsche Telekom
		// first: handle affected message
        if (TypeMsrpChunk.MessageDeliveredReport.equals(typeMsrpChunk)) {
            if (logger.isActivated()) {
                logger.info("Failed to send delivered message via MSRP, so try to send via SIP message to " + getRemoteContact() + ". (msgId = " + msgId + ")");
            }

            // Send the delivered notification by SIP
            getImdnManager().sendMessageDeliveryStatus(getRemoteContact(), msgId, ImdnDocument.DELIVERY_STATUS_DELIVERED);
        } else if (TypeMsrpChunk.MessageDisplayedReport.equals(typeMsrpChunk)) {
            if (logger.isActivated()) {
                logger.info("Failed to send displayed message via MSRP, so try to send via SIP message to " + getRemoteContact() + ". (msgId = " + msgId + ")");
            }

            // Send the displayed notification by SIP
            getImdnManager().sendMessageDeliveryStatus(getRemoteContact(), msgId, ImdnDocument.DELIVERY_STATUS_DISPLAYED);
        } else if ((msgId != null) && TypeMsrpChunk.TextMessage.equals(typeMsrpChunk)) {
            for (int i = 0; i < getListeners().size(); i++) {
                ImdnDocument imdn = new ImdnDocument(msgId, ImdnDocument.DELIVERY_NOTIFICATION,
                        ImdnDocument.DELIVERY_STATUS_FAILED);
                ContactId contact = null;
                ((ChatSessionListener)getListeners().get(i))
                        .handleMessageDeliveryStatus(contact, imdn);
	        }
        } else {
            // do nothing
            if (logger.isActivated()) {
                logger.debug("MSRP transfer error not handled!");
            }
        }

        // Changed by Deutsche Telekom
        // second: take care of the associated MSRP session

        int errorCode;

        if ((error != null) && (error.contains("413") || error.contains("408"))) {
            // session should not be torn down immediately as there may be more errors to come
            // but as errors occurred we shouldn't use it for sending any longer

            // RFC 4975
            // 408: An endpoint MUST treat a 408 response in the same manner as it would
            // treat a local timeout.
            // 413: If a message sender receives a 413 in a response, or in a REPORT
            // request, it MUST NOT send any further chunks in the message, that is,
            // any further chunks with the same Message-ID value. If the sender
            // receives the 413 while in the process of sending a chunk, and the
            // chunk is interruptible, the sender MUST interrupt it.

            errorCode = ChatError.MEDIA_SESSION_BROKEN;
        } else {
            // default error; used e.g. for 481 or any other error
            // RFC 4975
            // 481: A 481 response indicates that the indicated session does not exist.
            // The sender should terminate the session.

            errorCode = ChatError.MEDIA_SESSION_FAILED;
        }

        // Notify listeners
        for (int i = 0; i < getListeners().size(); i++) {
            ((ChatSessionListener) getListeners().get(i)).handleImError(new ChatError(
                    errorCode, error));
        }
    }

	/**
	 * Receive is composing event
	 *
	 * @param contact Contact
	 * @param event Event
	 */
	protected void receiveIsComposing(ContactId contact, byte[] event) {
	    mIsComposingMgr.receiveIsComposingEvent(contact, event);
	}

	/**
	 * Send an empty data chunk
	 */
	public void sendEmptyDataChunk() {
		try {
			mMsrpMgr.sendEmptyChunk();
		} catch(Exception e) {
	   		if (logger.isActivated()) {
	   			logger.error("Problem while sending empty data chunk", e);
	   		}
		}
	}

	/**
	 * Receive HTTP file transfer event
	 *
	 * @param contact Contact
	 * @param displayName Display Name
	 * @param fileTransferInfo Information of on file to transfer over HTTP
	 * @param msgId Message ID
	 */
	protected void receiveHttpFileTransfer(ContactId contact, String displayName, FileTransferHttpInfoDocument fileTransferInfo,
			String msgId) {
		// Test if the contact is blocked
		if (ContactsManager.getInstance().isFtBlockedForContact(contact)) {
			if (logger.isActivated()) {
				logger.debug("Contact " + contact + " is blocked, automatically reject the HTTP File transfer");
			}
			// TODO : reject (SIP MESSAGE ?)
			return;
		}

		// Auto reject if file too big
		long maxSize = mRcsSettings.getMaxFileTransferSize();
		if (maxSize > 0 && fileTransferInfo.getFileSize() > maxSize) {
			if (logger.isActivated()) {
				logger.debug("File is too big, reject the HTTP File transfer");
			}

			// TODO : reject (SIP MESSAGE ?)
			return;
		}

		// Auto reject if number max of FT reached
		if (!getImsService().getImsModule().getInstantMessagingService().isFileTransferSessionAvailable()) {
			if (logger.isActivated()) {
				logger.debug("Max number of File Tranfer reached, rejecting the HTTP File transfer");
			}

			// TODO : reject (SIP MESSAGE ?)
			return;
		}

		// Create a new session
		FileSharingSession session = new TerminatingHttpFileSharingSession(getImsService(), this, fileTransferInfo, msgId, contact, displayName);

		getImsService().getImsModule().getCoreListener().handleFileTransferInvitation(session, isGroupChat(), contact, displayName);

		session.startSession();
	}

	/**
	 * Send data chunk with a specified MIME type
	 *
	 * @param msgId Message ID
	 * @param data Data
	 * @param mime MIME type
     * @param typeMsrpChunk Type of MSRP chunk
	 * @return Boolean result
	 */
	public boolean sendDataChunks(String msgId, String data, String mime, TypeMsrpChunk typeMsrpChunk) {
		try {
			byte[] bytes = data.getBytes(UTF8);
			ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
			mMsrpMgr.sendChunks(stream, msgId, mime, bytes.length, typeMsrpChunk);
			return true;
		} catch(Exception e) {
			// Error
	   		if (logger.isActivated()) {
	   			logger.error("Problem while sending data chunks", e);
	   		}
			return false;
		}
	}

	/**
	 * Is group chat
	 *
	 * @return Boolean
	 */
	public abstract boolean isGroupChat();

	/**
	 * Is Store & Forward
	 *
	 * @return Boolean
	 */
	public boolean isStoreAndForward() {
		if (this instanceof TerminatingStoreAndForwardMsgSession ||
				this instanceof TerminatingStoreAndForwardNotifSession) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Send a chat message
	 * @param msg Chat message
	 */
	public abstract void sendChatMessage(ChatMessage msg);

	/**
	 * Send is composing status
	 *
	 * @param status Status
	 */
	public abstract void sendIsComposingStatus(boolean status);

//	/**
//	 * Add a participant to the session
//	 *
//	 * @param participant Participant
//	 */
//	public abstract void addParticipant(String participant);
//
//	/**
//	 * Add a list of participants to the session
//	 *
//	 * @param participants List of participants
//	 */
//	public abstract void addParticipants(List<String> participants);

	/**
	 * Send message delivery status via MSRP
	 *
	 * @param contact Contact that requested the delivery status
	 * @param msgId Message ID
	 * @param status Status
	 */
	public void sendMsrpMessageDeliveryStatus(ContactId contact, String msgId, String status) {
		// Send status in CPIM + IMDN headers
		String from = ChatUtils.ANOMYNOUS_URI;
		String to = ChatUtils.ANOMYNOUS_URI;
		sendMsrpMessageDeliveryStatus(from, to, msgId, status);
	}

		/**
		 * Send message delivery status via MSRP
		 *
		 * @param fromUri Uri from who will send the delivery status
		 * @param toUri Uri from who requested the delivery status
		 * @param msgId Message ID
		 * @param status Status
		 */
	public void sendMsrpMessageDeliveryStatus(String fromUri, String toUri, String msgId, String status) {
		if (logger.isActivated()) {
			logger.debug("Send delivery status " + status + " for message " + msgId);
		}
		// Send status in CPIM + IMDN headers
		String imdn = ChatUtils.buildDeliveryReport(msgId, status);

		String content = ChatUtils.buildCpimDeliveryReport(fromUri, toUri, imdn);

		TypeMsrpChunk typeMsrpChunk = TypeMsrpChunk.OtherMessageDeliveredReportStatus;
		if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
			typeMsrpChunk = TypeMsrpChunk.MessageDisplayedReport;
		} else {
			if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
				typeMsrpChunk = TypeMsrpChunk.MessageDeliveredReport;
			}
		}

		// Send data
		boolean result = sendDataChunks(IdGenerator.generateMessageID(), content, CpimMessage.MIME_TYPE, typeMsrpChunk);
		if (result && ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
			if (mMessagingLog.isFileTransfer(msgId)) {
			    //TODO update file transfer status
			} else {
				mMessagingLog.markIncomingChatMessageAsReceived(msgId);
			}
		}
	}

	/**
	 * Handle a message delivery status from a SIP message
	 *
	 * @param contact contact identifier who notified status
	 * @param imdn Imdn document
	 */
	public void handleMessageDeliveryStatus(ContactId contact, ImdnDocument imdn) {
		Collection<ImsSessionListener> listeners = getListeners();
		for (ImsSessionListener listener : listeners) {
			((ChatSessionListener)listener).handleMessageDeliveryStatus(contact, imdn);
		}
	}

	/**
     * Receive a message delivery status from an XML document
     *
     * @param contact Contact identifier
     * @param xml XML document
     */
    public void receiveMessageDeliveryStatus(ContactId contact, String xml) {
        try {
            ImdnDocument imdn = ChatUtils.parseDeliveryReport(xml);
            if (imdn == null) {
                return;
            }

            boolean isFileTransfer = mMessagingLog.isFileTransfer(imdn.getMsgId());
            if (isFileTransfer) {
                if (isGroupChat()) {
                    ((InstantMessagingService)getImsService()).receiveGroupFileDeliveryStatus(
                            contributionId, contact, imdn);
                } else {
                    ((InstantMessagingService)getImsService()).receiveFileDeliveryStatus(contact,
                            imdn);
                }
            } else {
                Collection<ImsSessionListener> listeners = getListeners();
                for (ImsSessionListener listener : listeners) {
                    ((ChatSessionListener)listener).handleMessageDeliveryStatus(contact, imdn);
                }
            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't parse IMDN document", e);
            }
        }
    }

    /**
     * Get max number of participants in the session including the initiator
     *
     * @return Integer
     */
    public int getMaxParticipants() {
        return maxParticipants;
    }

    /**
     * Set max number of participants in the session including the initiator
     *
     * @param maxParticipants Max number
     */
    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    /**
     * Prepare media session
     *
     * @throws Exception
     */
    public void prepareMediaSession() throws Exception {
        // Changed by Deutsche Telekom
        // Get the remote SDP part
        byte[] sdp = getDialogPath().getRemoteContent().getBytes(UTF8);

        // Changed by Deutsche Telekom
        // Create the MSRP session
		MsrpSession session = getMsrpMgr().createMsrpSession(sdp, this);

        session.setFailureReportOption(false);
        session.setSuccessReportOption(false);
    }

    /**
     * Start media session
     *
     * @throws Exception
     */
    public void startMediaSession() throws Exception {
        // Open the MSRP session
        getMsrpMgr().openMsrpSession();

        // Send an empty packet
        sendEmptyDataChunk();
    }

	/**
	 * Reject the session invitation
	 */
	public abstract void rejectSession();

	/**
	 * Chat inactivity event
	 */
	public void handleChatInactivityEvent() {
        if (logger.isActivated()) {
        	logger.debug("Chat inactivity event");
        }

        // Abort the session
        abortSession(ImsServiceSession.TERMINATION_BY_TIMEOUT);
	}

    /**
     * Handle 200 0K response
     *
     * @param resp 200 OK response
     */
    public void handle200OK(SipResponse resp) {
        super.handle200OK(resp);

        // Check if geolocation push supported by remote
        setGeolocSupportedByRemote(SipUtils.isFeatureTagPresent(resp, FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH));

        // Check if file transfer supported by remote
        setFileTransferSupportedByRemote(SipUtils.isFeatureTagPresent(resp, FeatureTags.FEATURE_RCSE_FT) ||
        		SipUtils.isFeatureTagPresent(resp, FeatureTags.FEATURE_RCSE_FT_HTTP));
    }

	/**
	 * Is media session established
	 *
	 * @return true If the empty packet was sent successfully
	 */
	public boolean isMediaEstablished() {
		return (getMsrpMgr().isEstablished() && !getDialogPath().isSessionTerminated());
	}
}
