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

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.SessionTimerManager;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Terminating one-to-one chat session
 * 
 * @author jexa7410
 */
public class TerminatingOneToOneChatSession extends OneToOneChatSession implements
		MsrpEventListener {
	/**
	 * The logger
	 */
	private final static Logger logger = Logger.getLogger(TerminatingOneToOneChatSession.class
			.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param invite Initial INVITE request
	 * @param contact the remote contactId
	 */
	public TerminatingOneToOneChatSession(ImsService parent, SipRequest invite, ContactId contact) {
		super(parent, contact, PhoneUtils.formatContactIdToUri(contact));

		// Set first message
		InstantMessage firstMsg = ChatUtils.getFirstMessage(invite);
		setFirstMesssage(firstMsg);

		// Create dialog path
		createTerminatingDialogPath(invite);

		// Set contribution ID
		String id = ChatUtils.getContributionId(invite);
		setContributionID(id);

		if (shouldBeAutoAccepted()) {
			setSessionAccepted();
		}
	}

	/**
	 * Check is session should be auto accepted. This method should only be
	 * called once per session
	 * 
	 * @return true if one-to-one chat session should be auto accepted
	 */
	private boolean shouldBeAutoAccepted() {
		/*
		 * In case the invite contains a http file transfer info the chat
		 * session should be auto-accepted so that the file transfer session can
		 * be started.
		 */
		if (FileTransferUtils.getHttpFTInfo(getDialogPath().getInvite()) != null) {
			return true;
		}

		return RcsSettings.getInstance().isChatAutoAccepted();
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
			if (logger.isActivated()) {
				logger.info("Initiate a new 1-1 chat session as terminating");
			}

			// Send message delivery report if requested
			if ((ChatUtils.isImdnDeliveredRequested(getDialogPath().getInvite()))
					|| (ChatUtils.isFileTransferOverHttp(getDialogPath().getInvite()))) {
				// Check notification disposition
				String msgId = ChatUtils.getMessageId(getDialogPath().getInvite());
				if (msgId != null) {
					try {
						ContactId remote = ContactUtils.createContactId(getDialogPath()
								.getRemoteParty());
						// Send message delivery status via a SIP MESSAGE
						getImdnManager().sendMessageDeliveryStatusImmediately(remote, msgId,
								ImdnDocument.DELIVERY_STATUS_DELIVERED,
								SipUtils.getRemoteInstanceID(getDialogPath().getInvite()));
					} catch (RcsContactFormatException e) {
						if (logger.isActivated()) {
							logger.warn("Cannot parse contact " + getDialogPath().getRemoteParty());
						}
					}
				}
			}

			Collection<ImsSessionListener> listeners = getListeners();
			/* Check if session should be auto-accepted once */
			if (isSessionAccepted()) {
				if (logger.isActivated()) {
					logger.debug("Received one-to-one chat invitation marked for auto-accept");
				}

				for (ImsSessionListener listener : listeners) {
					((ChatSessionListener)listener).handleSessionAutoAccepted();
				}
			} else {
				if (logger.isActivated()) {
					logger.debug("Received one-to-one chat invitation marked for manual accept");
				}

				for (ImsSessionListener listener : listeners) {
					listener.handleSessionInvited();
				}

				send180Ringing(getDialogPath().getInvite(), getDialogPath().getLocalTag());

				int answer = waitInvitationAnswer();
				switch (answer) {
					case ImsServiceSession.INVITATION_REJECTED:
						if (logger.isActivated()) {
							logger.debug("Session has been rejected by user");
						}

						removeSession();

						for (ImsSessionListener listener : listeners) {
							listener.handleSessionRejectedByUser();
						}
						return;

					case ImsServiceSession.INVITATION_NOT_ANSWERED:
						if (logger.isActivated()) {
							logger.debug("Session has been rejected on timeout");
						}

						// Ringing period timeout
						send486Busy(getDialogPath().getInvite(), getDialogPath().getLocalTag());

						removeSession();

						for (ImsSessionListener listener : listeners) {
							listener.handleSessionRejectedByTimeout();
						}
						return;

					case ImsServiceSession.INVITATION_CANCELED:
						if (logger.isActivated()) {
							logger.debug("Session has been rejected by remote");
						}

						removeSession();

						for (ImsSessionListener listener : listeners) {
							listener.handleSessionRejectedByRemote();
						}
						return;

					case ImsServiceSession.INVITATION_ACCEPTED:
						setSessionAccepted();

						for (ImsSessionListener listener : listeners) {
							listener.handleSessionAccepted();
						}
						break;

					default:
						if (logger.isActivated()) {
							logger.debug("Unknown invitation answer in run; answer=".concat(String
									.valueOf(answer)));
						}
						break;
				}
			}

			// Parse the remote SDP part
			String remoteSdp = getDialogPath().getInvite().getSdpContent();
			SdpParser parser = new SdpParser(remoteSdp.getBytes());
			Vector<MediaDescription> media = parser.getMediaDescriptions();
			MediaDescription mediaDesc = media.elementAt(0);
			MediaAttribute attr1 = mediaDesc.getMediaAttribute("path");
			String remotePath = attr1.getValue();
			String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
			int remotePort = mediaDesc.port;

			// Changed by Deutsche Telekom
			String fingerprint = SdpUtils.extractFingerprint(parser, mediaDesc);

			// Extract the "setup" parameter
			String remoteSetup = "passive";
			MediaAttribute attr2 = mediaDesc.getMediaAttribute("setup");
			if (attr2 != null) {
				remoteSetup = attr2.getValue();
			}
			if (logger.isActivated()) {
				logger.debug("Remote setup attribute is " + remoteSetup);
			}

			// Set setup mode
			String localSetup = createSetupAnswer(remoteSetup);
			if (logger.isActivated()) {
				logger.debug("Local setup attribute is " + localSetup);
			}

			// Set local port
			int localMsrpPort;
			if (localSetup.equals("active")) {
				localMsrpPort = 9; // See RFC4145, Page 4
			} else {
				localMsrpPort = getMsrpMgr().getLocalMsrpPort();
			}

			// Build SDP part
			// String ntpTime =
			// SipUtils.constructNTPtime(System.currentTimeMillis());
			String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
			String sdp = SdpUtils.buildChatSDP(ipAddress, localMsrpPort, getMsrpMgr()
					.getLocalSocketProtocol(), getAcceptTypes(), getWrappedTypes(), localSetup,
					getMsrpMgr().getLocalMsrpPath(), getSdpDirection());

			// Set the local SDP part in the dialog path
			getDialogPath().setLocalContent(sdp);

			// Test if the session should be interrupted
			if (isInterrupted()) {
				if (logger.isActivated()) {
					logger.debug("Session has been interrupted: end of processing");
				}
				return;
			}

			// Create the MSRP server session
			if (localSetup.equals("passive")) {
				// Passive mode: client wait a connection
				MsrpSession session = getMsrpMgr().createMsrpServerSession(remotePath, this);
				session.setFailureReportOption(false);
				session.setSuccessReportOption(false);

				// Open the connection
				Thread thread = new Thread() {
					public void run() {
						try {
							// Open the MSRP session
							getMsrpMgr().openMsrpSession();

							// Even if local setup is passive, an empty chunk
							// must be sent to open the NAT
							// and so enable the active endpoint to initiate a
							// MSRP connection.
							sendEmptyDataChunk();
						} catch (IOException e) {
							if (logger.isActivated()) {
								logger.error("Can't create the MSRP server session", e);
							}
						}
					}
				};
				thread.start();
			}

			// Create a 200 OK response
			if (logger.isActivated()) {
				logger.info("Send 200 OK");
			}
			SipResponse resp = SipMessageFactory.create200OkInviteResponse(getDialogPath(),
					getFeatureTags(), sdp);

			// The signalisation is established
			getDialogPath().sigEstablished();

			// Send response
			SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
					.sendSipMessageAndWait(resp);

			// Analyze the received response
			if (ctx.isSipAck()) {
				// ACK received
				if (logger.isActivated()) {
					logger.info("ACK request received");
				}

				// The session is established
				getDialogPath().sessionEstablished();

				// Create the MSRP client session
				if (localSetup.equals("active")) {
					// Active mode: client should connect
					MsrpSession session = getMsrpMgr().createMsrpClientSession(remoteHost,
							remotePort, remotePath, this, fingerprint);
					session.setFailureReportOption(false);
					session.setSuccessReportOption(false);

					// Open the MSRP session
					getMsrpMgr().openMsrpSession();

					// Send an empty packet
					sendEmptyDataChunk();
				}

				for (ImsSessionListener listener : listeners) {
					listener.handleSessionStarted();
				}

				// Start session timer
				if (getSessionTimerManager().isSessionTimerActivated(resp)) {
					getSessionTimerManager().start(SessionTimerManager.UAS_ROLE,
							getDialogPath().getSessionExpireTime());
				}

				// Start the activity manager
				getActivityManager().start();

			} else {
				if (logger.isActivated()) {
					logger.debug("No ACK received for INVITE");
				}

				// No response received: timeout
				handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED));
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Session initiation has failed", e);
			}

			// Unexpected error
			handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION, e.getMessage()));
		}
	}

	// Changed by Deutsche Telekom
	@Override
	public String getSdpDirection() {
		return SdpUtils.DIRECTION_SENDRECV;
	}

	@Override
	public boolean isInitiatedByRemote() {
		return true;
	}

	@Override
	public void startSession() {
		ContactId contact = getRemoteContact();
		if (logger.isActivated()) {
			logger.debug("Start OneToOneChatSession with '" + contact + "'");
		}
		InstantMessagingService imService = getImsService().getImsModule()
				.getInstantMessagingService();
		OneToOneChatSession currentSession = imService.getOneToOneChatSession(contact);
		if (currentSession != null) {
			boolean currentSessionInitiatedByRemote = currentSession.isInitiatedByRemote();
			boolean currentSessionEstablished = currentSession.getDialogPath()
					.isSessionEstablished();
			if (!currentSessionEstablished && !currentSessionInitiatedByRemote) {
				/*
				 * Rejecting the NEW invitation since there is already a PENDING
				 * OneToOneChatSession that was locally originated with the same
				 * contact.
				 */
				if (logger.isActivated()) {
					logger.warn("Rejecting OneToOneChatSession (session id '" + getSessionID()
							+ "') with '" + contact + "'");
				}
				rejectSession();
				return;
			}
			/*
			 * If this oneToOne session does NOT already contain another
			 * oneToOne chat session which in state PENDING and also LOCALLY
			 * originating we should leave (reject or abort) the CURRENT rcs
			 * chat session if there is one and replace it with the new one.
			 */
			if (logger.isActivated()) {
				logger.warn("Rejecting/Aborting existing OneToOneChatSession (session id '"
						+ getSessionID() + "') with '" + contact + "'");
			}
			if (currentSessionInitiatedByRemote) {
				if (currentSessionEstablished) {
					currentSession.abortSession(ImsServiceSession.TERMINATION_BY_USER);
				} else {
					currentSession.rejectSession();
				}
			} else {
				currentSession.abortSession(ImsServiceSession.TERMINATION_BY_USER);
			}
		}
		imService.addSession(this);
		start();
	}

	@Override
	public void removeSession() {
		getImsService().getImsModule().getInstantMessagingService().removeSession(this);
	}
}
