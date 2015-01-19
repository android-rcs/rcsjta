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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.gsma.services.rcs.chat.ParticipantInfo;
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
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Terminating ad-hoc group chat session
 * 
 * @author jexa7410
 */
public class TerminatingAdhocGroupChatSession extends GroupChatSession implements MsrpEventListener {

	/**
	 * Set of missing participants in case of restart 
	 */
	Set<ContactId> missingParticipants;
	
    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(TerminatingAdhocGroupChatSession.class.getSimpleName());
    
	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param invite Initial INVITE request
	 * @param contact remote contact
	 * @param remoteUri the remote Uri
	 * @param participants set of participants
	 * @param rcsSettings RCS settings
	 * @param messagingLog Messaging log
	 */
	public TerminatingAdhocGroupChatSession(ImsService parent, SipRequest invite,
			ContactId contact, String remoteUri, Set<ParticipantInfo> participants,
			RcsSettings rcsSettings, MessagingLog messagingLog) {
		super(parent, contact, remoteUri, participants, rcsSettings, messagingLog);

		Set<ParticipantInfo> sessionParticipants = getParticipants();
		// Set subject
		String subject = ChatUtils.getSubject(invite);
		setSubject(subject);

		// Create dialog path
		createTerminatingDialogPath(invite);

		setRemoteDisplayName(SipUtils.getDisplayNameFromUri(SipUtils.getAssertedIdentityHeader(invite)));
		
		// Set contribution ID
		String chatId = ChatUtils.getContributionId(invite);
		setContributionID(chatId);

		boolean logActivated = logger.isActivated();
		// Detect if it's a rejoin
		if (sessionParticipants.size() == 0) {
			if (logActivated) {
				logger.info("Invite to join a group chat");
			}
		} else {
			if (logActivated) {
				logger.info("Set of invited participants: " + getListOfParticipants(sessionParticipants));
			}
			// Detect if it's a restart: retrieve set of initial participants
			Set<ParticipantInfo> initialParticipants = MessagingLog.getInstance().getGroupChatConnectedParticipants(
					getContributionID());
			if (initialParticipants != null && initialParticipants.size() > 0) {
				if (logActivated) {
					logger.info("Set of initial participants: " + getListOfParticipants(initialParticipants));
				}
				missingParticipants = new HashSet<ContactId>();
				// Run through the set of initial participants
				for (ParticipantInfo participantInfo : initialParticipants) {
					// Is initial participant in the invited list ?
					if (ParticipantInfoUtils.getItem(getParticipants(), participantInfo.getContact()) == null) {
						// Initial participant does not belong to list of invited.
						// Participant is missing: should be re-invited
						missingParticipants.add(participantInfo.getContact());
					}
				}
				if (missingParticipants.size() != 0) {
					if (logActivated) {
						StringBuilder sb = new StringBuilder("Invite to restart with missing participants: ");
						for (ContactId missing : missingParticipants) {
							sb.append(missing.toString()).append(" ");
						}
						logger.info(sb.toString());
					}
				}
			} else {
				if (logActivated) {
					logger.info("No initial Group Chat");
				}
			}
		}

		if(shouldBeAutoAccepted()) {
			setSessionAccepted();
		}
	}

	/**
	 * Check is session should be auto accepted. This method should only be
	 * called once per session
	 *
	 * @return true if group chat session should be auto accepted
	 */
	private boolean shouldBeAutoAccepted() {
		/*
		 * In case the invite contains a http file transfer info the chat session
		 * should be auto-accepted so that the file transfer session can be started.
		 */
		if (FileTransferUtils.getHttpFTInfo(getDialogPath().getInvite()) != null) {
			return true;
		}

		return RcsSettings.getInstance().isGroupChatAutoAccepted();
	}

	/**
	 * Background processing
	 */
	public void run() {
		final boolean logActivated = logger.isActivated();
		try {
	    	if (logActivated) {
	    		logger.info("Initiate a new ad-hoc group chat session as terminating");
	    	}

			Collection<ImsSessionListener> listeners = getListeners();
			/* Check if session should be auto-accepted once */
			if (isSessionAccepted()) {
				if (logActivated) {
					logger.debug("Received group chat invitation marked for auto-accept");
				}

				for (ImsSessionListener listener : listeners) {
					((ChatSessionListener)listener).handleSessionAutoAccepted();
				}
			} else {
				if (logActivated) {
					logger.debug("Received group chat invitation marked for manual accept");
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
							((ChatSessionListener)listener).handleSessionAccepted();
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
	    	String sdp = SdpUtils.buildGroupChatSDP(ipAddress, localMsrpPort, getMsrpMgr().getLocalSocketProtocol(),
                    getAcceptTypes(), getWrappedTypes(), localSetup, getMsrpMgr().getLocalMsrpPath(),
                    SdpUtils.DIRECTION_SENDRECV);

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
    			new Thread(){
    				public void run(){
    					try {
    						// Open the MSRP session
    						getMsrpMgr().openMsrpSession();
    						
    						// Even if local setup is passive, an empty packet must be sent to open the NAT
							// and so enable the active endpoint to initiate a MSRP connection.
    		            	sendEmptyDataChunk();    						
						} catch (IOException e) {
							if (logActivated) {
				        		logger.error("Can't create the MSRP server session", e);
				        	}
						}		
    				}
    			}.start();
            }
            
            // Create a 200 OK response
        	if (logActivated) {
        		logger.info("Send 200 OK");
        	}
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(getDialogPath(),
            		getFeatureTags(), getAcceptContactTags(), sdp);

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

    	    	// Check if some participants are missing
    	    	if (missingParticipants != null && missingParticipants.size() > 0) {
					// Only keep participants who are not invited by the AS
    	    		inviteMissingParticipants(missingParticipants);
    	    	}
    	    	
    	    	// Subscribe to event package
            	getConferenceEventSubscriber().subscribe();

            	// Start session timer
            	if (getSessionTimerManager().isSessionTimerActivated(resp)) {        	
            		getSessionTimerManager().start(SessionTimerManager.UAS_ROLE, getDialogPath().getSessionExpireTime());
            	}
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
			handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION, e.getMessage()));
		}		
	}
	
	/**
	 * Invite missing participants.
	 * 
	 * @param participants
	 *            Set of missing participant identifiers
	 */
	private void inviteMissingParticipants(final Set<ContactId> participants) {
		final boolean logActivated = logger.isActivated();
		if (logActivated) {
			logger.info("Invite missing participants: " + Arrays.toString(missingParticipants.toArray()));
		}
		new Thread() {
			public void run() {
				try {
					addParticipants(participants);
				} catch (Exception e) {
					if (logActivated) {
						logger.error("Session initiation has failed", e);
					}
				}
			}
		}.start();
	}

	@Override
	public boolean isInitiatedByRemote() {
		return true;
	}
	
	/**
	 * Returns the list of participants separated by commas
	 * 
	 * @param participants
	 *            set of participants
	 * @return the list of participants separated by commas
	 */
	private String getListOfParticipants(Set<ParticipantInfo> participants) {
		StringBuilder sb = new StringBuilder();
		for (ParticipantInfo participantInfo : participants) {
			sb.append(participantInfo.getContact().toString()).append(",");
		}
		return sb.toString();
	}

	@Override
	public void startSession() {
		final boolean logActivated = logger.isActivated();
		String chatId = getContributionID();
		if (logActivated) {
			logger.debug("Start GroupChatSession with chatID '" + chatId + "'");
		}
		InstantMessagingService imService = getImsService().getImsModule()
				.getInstantMessagingService();
		GroupChatSession currentSession = imService.getGroupChatSession(chatId);
		if (currentSession != null) {
			/*
			 * If there is already a groupchat session with same chatId
			 * existing, we should not reject the new session but update cache
			 * with this groupchat session and mark the old groupchat session
			 * pending for removal which will timeout eventually
			 */
			if (logActivated) {
				logger.debug("Ongoing GrooupChat session detected for chatId '" + chatId
						+ "' marking that session pending for removal");
			}
			currentSession.markForPendingRemoval();
			/*
			 * Since the current session was already established and we are now
			 * replacing that session with a new session then we make sure to
			 * auto-accept that new replacement session also so to leave the
			 * client in the same situation for the replacement session as for
			 * the original "current" session regardless if the the provisioning
			 * setting for chat is set to non-auto-accept or not.
			 */
			if (currentSession.getDialogPath().isSessionEstablished()) {
				setSessionAccepted();
			}
		}
		imService.addSession(this);
		start();
	}
}
