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

package com.orangelabs.rcs.core.ims.service.im.chat.standfw;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

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
import com.orangelabs.rcs.core.ims.service.im.chat.ChatError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Terminating Store & Forward session for messages
 * 
 * @author jexa7410
 */
public class TerminatingStoreAndForwardMsgSession extends OneToOneChatSession implements MsrpEventListener {
	/**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(TerminatingStoreAndForwardMsgSession.class.getSimpleName());

    /**
     * Constructor
     * 
	 * @param parent IMS service
     * @param invite Initial INVITE request
     * @param contact the remote ContactId
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
	 */
	public TerminatingStoreAndForwardMsgSession(ImsService parent, SipRequest invite,
			ContactId contact, RcsSettings rcsSettings, MessagingLog messagingLog) {
		super(parent, contact, PhoneUtils.formatContactIdToUri(contact), ChatUtils
				.getFirstMessage(invite), rcsSettings, messagingLog);

		// Set feature tags
		setFeatureTags(ChatUtils.getSupportedFeatureTagsForChat());
		
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
		 * In case the invite contains a http file transfer info the chat session
		 * should be auto-accepted so that the file transfer session can be started.
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
		final boolean logActivated = logger.isActivated();
		try {
	    	if (logActivated) {
	    		logger.info("Initiate a store & forward session for messages");
	    	}

			// Send message delivery report if requested
			if (ChatUtils.isImdnDeliveredRequested(getDialogPath().getInvite())) {
				// Check notification disposition
				String msgId = ChatUtils.getMessageId(getDialogPath().getInvite());
				if (msgId != null) {
					// Send message delivery status via a SIP MESSAGE
					getImdnManager().sendMessageDeliveryStatusImmediately(getRemoteContact(),
							msgId, ImdnDocument.DELIVERY_STATUS_DELIVERED,
							SipUtils.getRemoteInstanceID(getDialogPath().getInvite()));
				}
			}

			Collection<ImsSessionListener> listeners = getListeners();
			/* Check if session should be auto-accepted once */
			if (isSessionAccepted()) {
				if (logActivated) {
					logger.debug("Auto accept store and forward chat invitation");
				}

				for (ImsSessionListener listener : listeners) {
					((ChatSessionListener)listener).handleSessionAutoAccepted();
				}
			} else {
				if (logActivated) {
					logger.debug("Accept manually store and forward chat invitation");
				}

				for (ImsSessionListener listener : listeners) {
					listener.handleSessionInvited();
				}

				send180Ringing(getDialogPath().getInvite(), getDialogPath().getLocalTag());

				int answer = waitInvitationAnswer();
				switch (answer) {
					case ImsServiceSession.INVITATION_REJECTED:
						if (logActivated) {
							logger.debug("Session has been rejected by user");
						}

						removeSession();

						for (ImsSessionListener listener : listeners) {
							listener.handleSessionRejectedByUser();
						}
						return;

					case ImsServiceSession.INVITATION_NOT_ANSWERED:
						if (logActivated) {
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
						if (logActivated) {
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
						if (logActivated) {
							logger.debug("Unknown invitation answer in run; answer="
									.concat(String.valueOf(answer)));
						}
						return;
				}
			}

        	// Parse the remote SDP part
			String remoteSdp = getDialogPath().getInvite().getSdpContent();
        	SdpParser parser = new SdpParser(remoteSdp.getBytes(UTF8));
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
			if (logActivated) {
				logger.debug("Remote setup attribute is " + remoteSetup);
			}
            
    		// Set setup mode
            String localSetup = createSetupAnswer(remoteSetup);
			if (logActivated) {
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
	    	String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
	    	String sdp = SdpUtils.buildChatSDP(ipAddress, localMsrpPort, getMsrpMgr().getLocalSocketProtocol(),
                    getAcceptTypes(), getWrappedTypes(), localSetup, getMsrpMgr().getLocalMsrpPath(), getSdpDirection());

	    	// Set the local SDP part in the dialog path
	        getDialogPath().setLocalContent(sdp);

	        // Test if the session should be interrupted
            if (isInterrupted()) {
            	if (logActivated) {
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
    			Thread thread = new Thread(){
    				public void run(){
    					try {
							// Open the MSRP session
							getMsrpMgr().openMsrpSession();
							
			    	        // Send an empty packet
			            	sendEmptyDataChunk();
						} catch (IOException e) {
							if (logActivated) {
				        		logger.error("Can't create the MSRP server session", e);
				        	}
						}		
    				}
    			};
    			thread.start();
            }
            
            // Create a 200 OK response
        	if (logActivated) {
        		logger.info("Send 200 OK");
        	}
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(getDialogPath(),
            		getFeatureTags(), sdp);

            // The signalisation is established
            getDialogPath().sigEstablished();

            // Send response
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSipMessageAndWait(resp);

            // Analyze the received response 
            if (ctx.isSipAck()) {
    	        // ACK received
    			if (logActivated) {
    				logger.info("ACK request received");
    			}

                // The session is established
    	        getDialogPath().sessionEstablished();
    	                        
        		// Create the MSRP client session
                if (localSetup.equals("active")) {
                	// Active mode: client should connect
                	MsrpSession session = getMsrpMgr().createMsrpClientSession(remoteHost, remotePort, remotePath, this, fingerprint);
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
            		getSessionTimerManager().start(SessionTimerManager.UAS_ROLE, getDialogPath().getSessionExpireTime());
            	}

            	// Start the activity manager
    			getActivityManager().start();
    	    	
            } else {
        		if (logActivated) {
            		logger.debug("No ACK received for INVITE");
            	}

        		// No response received: timeout
            	handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED));
            }
		} catch(Exception e) {
        	if (logActivated) {
        		logger.error("Session initiation has failed", e);
        	}

        	// Unexpected error
			handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
		}
	}
	
    @Override
    public String getSdpDirection() {
        return SdpUtils.DIRECTION_RECVONLY;
    }

	@Override
	public boolean isInitiatedByRemote() {
		return true;
	}

	@Override
	public void startSession() {
		final boolean logActivated = logger.isActivated();
		ContactId contact = getRemoteContact();
		if (logActivated) {
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
				if (logActivated) {
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
			if (logActivated) {
				logger.warn("Rejecting/Aborting existing OneToOneChatSession (session id '"
						+ getSessionID() + "') with '" + contact + "'");
			}
			if (currentSessionInitiatedByRemote) {
				if (currentSessionEstablished) {
					currentSession.abortSession(ImsServiceSession.TERMINATION_BY_SYSTEM);
				} else {
					currentSession.rejectSession();
				}
			} else {
				currentSession.abortSession(ImsServiceSession.TERMINATION_BY_SYSTEM);
			}
			/*
			 * Since the current session was already established and we are now
			 * replacing that session with a new session then we make sure to
			 * auto-accept that new replacement session also so to leave the
			 * client in the same situation for the replacement session as for
			 * the original "current" session regardless if the the provisioning
			 * setting for chat is set to non-auto-accept or not.
			 */
			if (currentSessionEstablished) {
				setSessionAccepted();
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
