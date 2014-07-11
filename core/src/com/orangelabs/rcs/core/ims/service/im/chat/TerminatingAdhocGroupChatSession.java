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

package com.orangelabs.rcs.core.ims.service.im.chat;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
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
import com.orangelabs.rcs.core.ims.service.SessionTimerManager;
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
	 */
	public TerminatingAdhocGroupChatSession(ImsService parent, SipRequest invite, ContactId contact, String remoteUri, Set<ParticipantInfo> participants) {
		super(parent, contact, remoteUri, participants);

		// Set subject
		String subject = ChatUtils.getSubject(invite);
		setSubject(subject);

		// Create dialog path
		createTerminatingDialogPath(invite);

		// Set contribution ID
		String id = ChatUtils.getContributionId(invite);
		setContributionID(id);

		// Detect if it's a rejoin
		if (getParticipants().size() == 0) {
			if (logger.isActivated()) {
				logger.info("Invite to join a group chat");
			}
		} else {
			if (logger.isActivated()) {
				logger.info("Set of invited participants: " + Arrays.toString(getParticipants().toArray()));
			}
			// Detect if it's a restart: retrieve set of initial participants
			Set<ParticipantInfo> initialParticipants = MessagingLog.getInstance().getGroupChatConnectedParticipants(
					getContributionID());
			if (initialParticipants != null && initialParticipants.size() > 0) {
				if (logger.isActivated()) {
					logger.info("Set of initial participants: " + Arrays.toString(initialParticipants.toArray()));
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
					if (logger.isActivated()) {
						logger.info("Invite to restart with missing participants: "
								+ Arrays.toString(missingParticipants.toArray()));
					}
				}
			} else {
				if (logger.isActivated()) {
					logger.info("No initial Group Chat");
				}
			}
		}
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a new ad-hoc group chat session as terminating");
	    	}

            if (RcsSettings.getInstance().isGroupChatAutoAccepted() || FileTransferUtils.getHttpFTInfo(getDialogPath().getInvite()) != null) {
                if (logger.isActivated()) {
                    logger.debug("Auto accept group chat invitation");
                }
            } else {
                if (logger.isActivated()) {
                    logger.debug("Accept manually group chat invitation");
                }
    	    	// Send a 180 Ringing response
    			send180Ringing(getDialogPath().getInvite(), getDialogPath().getLocalTag());
    			
    			// Wait invitation answer
    	    	int answer = waitInvitationAnswer();
    			if (answer == ImsServiceSession.INVITATION_REJECTED) {
    				if (logger.isActivated()) {
    					logger.debug("Session has been rejected by user");
    				}
    				
    		    	// Remove the current session
    		    	getImsService().removeSession(this);
    
    		    	// Notify listeners
    		    	for(int i=0; i < getListeners().size(); i++) {
    		    		getListeners().get(i).handleSessionAborted(ImsServiceSession.TERMINATION_BY_USER);
    		        }
    				return;
    			} else
    			if (answer == ImsServiceSession.INVITATION_NOT_ANSWERED) {
    				if (logger.isActivated()) {
    					logger.debug("Session has been rejected on timeout");
    				}
    
    				// Ringing period timeout
    				send486Busy(getDialogPath().getInvite(), getDialogPath().getLocalTag());
    				
    		    	// Remove the current session
    		    	getImsService().removeSession(this);
    
    		    	// Notify listeners
        	    	for(int i=0; i < getListeners().size(); i++) {
        	    		getListeners().get(i).handleSessionAborted(ImsServiceSession.TERMINATION_BY_TIMEOUT);
    		        }
    				return;
    			} else
                if (answer == ImsServiceSession.INVITATION_CANCELED) {
                    if (logger.isActivated()) {
                        logger.debug("Session has been canceled");
                    }
                    return;
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
            if (logger.isActivated()){
				logger.debug("Remote setup attribute is " + remoteSetup);
			}
            
    		// Set setup mode
            String localSetup = createSetupAnswer(remoteSetup);
            if (logger.isActivated()){
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
    			new Thread(){
    				public void run(){
    					try {
    						// Open the MSRP session
    						getMsrpMgr().openMsrpSession();
    						
    						// Even if local setup is passive, an empty packet must be sent to open the NAT
							// and so enable the active endpoint to initiate a MSRP connection.
    		            	sendEmptyDataChunk();    						
						} catch (IOException e) {
							if (logger.isActivated()) {
				        		logger.error("Can't create the MSRP server session", e);
				        	}
						}		
    				}
    			}.start();
            }
            
            // Create a 200 OK response
        	if (logger.isActivated()) {
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
    			if (logger.isActivated()) {
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

            	// Notify listeners
    	    	for(int i=0; i < getListeners().size(); i++) {
    	    		getListeners().get(i).handleSessionStarted();
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
        		if (logger.isActivated()) {
            		logger.debug("No ACK received for INVITE");
            	}

        		// No response received: timeout
            	handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED));
            }
		} catch(Exception e) {
        	if (logger.isActivated()) {
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
		if (logger.isActivated()) {
			logger.info("Invite missing participants: " + Arrays.toString(missingParticipants.toArray()));
		}
		new Thread() {
			public void run() {
				try {
					addParticipants(participants);
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Session initiation has failed", e);
					}
				}
			}
		}.start();
	}
	
}
