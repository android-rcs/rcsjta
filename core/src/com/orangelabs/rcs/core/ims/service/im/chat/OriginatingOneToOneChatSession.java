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

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.Multipart;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Originating one-to-one chat session
 * 
 * @author jexa7410
 */
public class OriginatingOneToOneChatSession extends OneToOneChatSession {
	/**
	 * Boundary tag
	 */
	private final static String BOUNDARY_TAG = "boundary1";

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param contact Remote contact identifier
	 * @param msg First message of the session
	 */
	public OriginatingOneToOneChatSession(ImsService parent, ContactId contact, InstantMessage msg) {
		super(parent, contact, PhoneUtils.formatContactIdToUri(contact));
		// Set first message
		setFirstMesssage(msg);
		// Create dialog path
		createOriginatingDialogPath();
		// Set contribution ID
		String id = ContributionIdGenerator.getContributionId(getDialogPath().getCallId());
		setContributionID(id);
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
			if (logger.isActivated()) {
				logger.info("Initiate a new 1-1 chat session as originating");
			}

			// Set setup mode
			String localSetup = createSetupOffer();
			if (logger.isActivated()) {
				logger.debug("Local setup attribute is " + localSetup);
			}

			// Set local port
			int localMsrpPort;
			if ("active".equals(localSetup)) {
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

			// If there is a first message then builds a multipart content else
			// builds a SDP content
			InstantMessage firstMessage = getFirstMessage();
			if (firstMessage != null) {
				// Build CPIM part
				String from = ChatUtils.ANOMYNOUS_URI;
				String to = ChatUtils.ANOMYNOUS_URI;

				boolean useImdn = getImdnManager().isImdnActivated();
				String formattedMsg;
				String mime;
				if (firstMessage instanceof GeolocMessage) {
					GeolocMessage geolocMsg = (GeolocMessage)firstMessage;
					formattedMsg = ChatUtils.buildGeolocDocument(geolocMsg.getGeoloc(),
							ImsModule.IMS_USER_PROFILE.getPublicUri(), firstMessage
									.getMessageId());
					mime = GeolocInfoDocument.MIME_TYPE;
				} else if (firstMessage instanceof FileTransferMessage) {
					FileTransferMessage fileMsg = (FileTransferMessage)firstMessage;
					formattedMsg = fileMsg.getFileInfo();
					mime = FileTransferHttpInfoDocument.MIME_TYPE;
				} else {
					formattedMsg = firstMessage.getTextMessage();
					mime = InstantMessage.MIME_TYPE;
				}

				String cpim;
				if (useImdn) {
					// Send message in CPIM + IMDN
					cpim = ChatUtils.buildCpimMessageWithImdn(from, to, firstMessage
							.getMessageId(), StringUtils.encodeUTF8(formattedMsg), mime);
				} else {
					// Send message in CPIM
					cpim = ChatUtils.buildCpimMessage(from, to,
							StringUtils.encodeUTF8(formattedMsg), mime);
				}

				// Build multipart
				String multipart = new StringBuilder(Multipart.BOUNDARY_DELIMITER)
						.append(BOUNDARY_TAG).append(SipUtils.CRLF)
						.append("Content-Type: application/sdp").append(SipUtils.CRLF)
						.append("Content-Length: ").append(sdp.getBytes().length)
						.append(SipUtils.CRLF).append(SipUtils.CRLF).append(sdp)
						.append(SipUtils.CRLF).append(Multipart.BOUNDARY_DELIMITER)
						.append(BOUNDARY_TAG).append(SipUtils.CRLF).append("Content-Type: ")
						.append(CpimMessage.MIME_TYPE).append(SipUtils.CRLF)
						.append("Content-Length: ").append(cpim.getBytes().length)
						.append(SipUtils.CRLF).append(SipUtils.CRLF).append(cpim)
						.append(SipUtils.CRLF).append(Multipart.BOUNDARY_DELIMITER)
						.append(BOUNDARY_TAG).append(Multipart.BOUNDARY_DELIMITER).toString();

				// Set the local SDP part in the dialog path
				getDialogPath().setLocalContent(multipart);
			} else {
				// Set the local SDP part in the dialog path
				getDialogPath().setLocalContent(sdp);
			}
			SipRequest invite = createInvite();

			// Set the Authorization header
			getAuthenticationAgent().setAuthorizationHeader(invite);

			// Set initial request in the dialog path
			getDialogPath().setInvite(invite);

			// Send INVITE request
			sendInvite(invite);

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
		return false;
	}

	@Override
	public void startSession() {
		getImsService().getImsModule().getInstantMessagingService().addSession(this);
		start();
	}

	@Override
	public void removeSession() {
		getImsService().getImsModule().getInstantMessagingService().removeSession(this);
	}
}
