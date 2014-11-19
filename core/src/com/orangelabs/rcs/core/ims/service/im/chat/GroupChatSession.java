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
import java.util.Date;
import java.util.Set;

import javax2.sip.header.ExtensionHeader;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.SessionAuthenticationAgent;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimIdentity;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimParser;
import com.orangelabs.rcs.core.ims.service.im.chat.event.ConferenceEventSubscribeManager;
import com.orangelabs.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Abstract Group chat session
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 *
 */
public abstract class GroupChatSession extends ChatSession {
	/**
	 * Conference event subscribe manager
	 */
	private ConferenceEventSubscribeManager conferenceSubscriber;
		
	/**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(GroupChatSession.class.getSimpleName());

    /**
	 * Constructor for originating side
	 * 
	 * @param parent IMS service
	 * @param contact remote contact identifier
	 * @param conferenceId Conference id
	 * @param participants Set of invited participants
	 */
    public GroupChatSession(ImsService parent, ContactId contact, String conferenceId, Set<ParticipantInfo> participants) {
		super(parent, contact, conferenceId, participants);
		
		conferenceSubscriber = new ConferenceEventSubscribeManager(this); 
		
		// Set feature tags
        setFeatureTags(ChatUtils.getSupportedFeatureTagsForGroupChat());
		
        // Set Accept-Contact header
        setAcceptContactTags(ChatUtils.getAcceptContactTagsForGroupChat());

		// Set accept-types
		String acceptTypes = CpimMessage.MIME_TYPE;	
        setAcceptTypes(acceptTypes);
				
		// Set accept-wrapped-types
		String wrappedTypes = InstantMessage.MIME_TYPE + " " + IsComposingInfo.MIME_TYPE;
		if (RcsSettings.getInstance().isGeoLocationPushSupported()) {
        	wrappedTypes += " " + GeolocInfoDocument.MIME_TYPE;
        }
        if (RcsSettings.getInstance().isFileTransferHttpSupported()) {
        	wrappedTypes += " " + FileTransferHttpInfoDocument.MIME_TYPE;
        }		
        setWrappedTypes(wrappedTypes);
	}

    @Override
	public boolean isGroupChat() {
		return true;
	}
	
    @Override
    public Set<ParticipantInfo> getConnectedParticipants() {
		return conferenceSubscriber.getParticipants();
	}
    
    /**
	 * Get replaced session ID
	 * 
	 * @return Session ID
	 */
	public String getReplacedSessionId() {
		String result = null;
		ExtensionHeader sessionReplace = (ExtensionHeader)getDialogPath().getInvite().getHeader(SipUtils.HEADER_SESSION_REPLACES);
		if (sessionReplace != null) {
			result = sessionReplace.getValue();
		} else {
			String content = getDialogPath().getRemoteContent();
			if (content != null) {
				int index1 = content.indexOf("Session-Replaces=");
				if (index1 != -1) {
					int index2 = content.indexOf("\"", index1);
					result = content.substring(index1+17, index2);
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns the conference event subscriber
	 * 
	 * @return Subscribe manager
	 */
	public ConferenceEventSubscribeManager getConferenceEventSubscriber() {
		return conferenceSubscriber;
	}	

    /**
     * Close media session
     */
    public void closeMediaSession() {
        // Close MSRP session
        closeMsrpSession();
    }

    /**
	 * Terminate session
	 *  
	 * @param reason Reason
	 */
	public void terminateSession(int reason) {
		// Stop conference subscription
		conferenceSubscriber.terminate();
		
		// Terminate session
		super.terminateSession(reason);
	}	
	
	/**
	 * Request capabilities to contact
	 * @param contact
	 */
	private void requestContactCapabilities(String contact) {
		try {
			ContactId remote = ContactUtils.createContactId(contact);
			// Request capabilities to the remote
			getImsService().getImsModule().getCapabilityService().requestContactCapabilities(remote);
		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.debug("Failed to request capabilities: cannot parse contact " + contact);
			}
		}
	}

    /**
     * Receive BYE request 
     * 
     * @param bye BYE request
     */
    public void receiveBye(SipRequest bye) {
        // Stop conference subscription
        conferenceSubscriber.terminate();
        
        // Receive BYE request
        super.receiveBye(bye);
        
        // Request capabilities if remote contact is valid
        requestContactCapabilities(getDialogPath().getRemoteParty());
    }
    
    /**
     * Receive CANCEL request 
     * 
     * @param cancel CANCEL request
     */
    public void receiveCancel(SipRequest cancel) {
        // Stop conference subscription
        conferenceSubscriber.terminate();
        
        super.receiveCancel(cancel);
        
        // Request capabilities if remote contact is valid 
        requestContactCapabilities(getDialogPath().getRemoteParty());
	}

	@Override
	public void sendTextMessage(String msgId, String txt) {
		boolean useImdn = getImdnManager().isImdnActivated();
		String from = ImsModule.IMS_USER_PROFILE.getPublicAddress();
		String to = ChatUtils.ANOMYNOUS_URI;
		String networkContent;
		if (useImdn) {
			networkContent = ChatUtils.buildCpimMessageWithDeliveredImdn(from, to, msgId,
					StringUtils.encodeUTF8(txt), InstantMessage.MIME_TYPE);

		} else {
			networkContent = ChatUtils.buildCpimMessage(from, to, StringUtils.encodeUTF8(txt),
					InstantMessage.MIME_TYPE);
		}
		InstantMessage msg = new InstantMessage(msgId, getRemoteContact(), txt, useImdn, null);

		Collection<ImsSessionListener> listeners = getListeners();
		for (ImsSessionListener listener : listeners) {
			((ChatSessionListener)listener).handleMessageSending(msg);
		}

		boolean result = sendDataChunks(IdGenerator.generateMessageID(), networkContent,
				CpimMessage.MIME_TYPE, TypeMsrpChunk.TextMessage);

		/* TODO:This will be redone with CR037 */
		if (result) {
			for (ImsSessionListener listener : listeners) {
				((ChatSessionListener)listener).handleMessageSent(msgId);
			}

		} else {
			for (ImsSessionListener listener : listeners) {
				((ChatSessionListener)listener).handleMessageFailedSend(msgId);
			}
		}
	}

	@Override
	public void sendGeolocMessage(String msgId, GeolocPush geoloc) {
		boolean useImdn = getImdnManager().isImdnActivated();
		String from = ImsModule.IMS_USER_PROFILE.getPublicAddress();
		String to = ChatUtils.ANOMYNOUS_URI;
		String geoDoc = ChatUtils.buildGeolocDocument(geoloc,
				ImsModule.IMS_USER_PROFILE.getPublicUri(), msgId);
		String networkContent;
		if (useImdn) {
			networkContent = ChatUtils.buildCpimMessageWithDeliveredImdn(from, to, msgId,
					geoDoc, GeolocInfoDocument.MIME_TYPE);

		} else {
			networkContent = ChatUtils.buildCpimMessage(from, to, geoDoc,
					GeolocInfoDocument.MIME_TYPE);
		}
		GeolocMessage geolocMsg = new GeolocMessage(msgId, getRemoteContact(), geoloc, useImdn, null);

		Collection<ImsSessionListener> listeners = getListeners();
		for (ImsSessionListener listener : listeners) {
			((ChatSessionListener)listener).handleMessageSending(geolocMsg);
		}

		boolean result = sendDataChunks(IdGenerator.generateMessageID(), networkContent, CpimMessage.MIME_TYPE,
				TypeMsrpChunk.GeoLocation);

		/* TODO:This will be redone with CR037 */
		if (result) {
			for (ImsSessionListener listener : listeners) {
				((ChatSessionListener)listener).handleMessageSent(msgId);
			}

		} else {
			for (ImsSessionListener listener : listeners) {
				((ChatSessionListener)listener).handleMessageFailedSend(msgId);
			}
		}
	}

    @Override
	public void sendIsComposingStatus(boolean status) {
		String from = ImsModule.IMS_USER_PROFILE.getPublicUri();
		String to = ChatUtils.ANOMYNOUS_URI;
		String msgId = IdGenerator.generateMessageID();
		String content = ChatUtils.buildCpimMessage(from, to, IsComposingInfo.buildIsComposingInfo(status), IsComposingInfo.MIME_TYPE);
		sendDataChunks(msgId, content, CpimMessage.MIME_TYPE, TypeMsrpChunk.IsComposing);	
	}

    @Override
    public void sendMsrpMessageDeliveryStatus(ContactId remote, String msgId, String status) {
		// Send status in CPIM + IMDN headers
		String to = (remote != null) ? remote.toString() : ChatUtils.ANOMYNOUS_URI;
		sendMsrpMessageDeliveryStatus(null, to, msgId, status);
    }
    
    @Override
    public void sendMsrpMessageDeliveryStatus(String fromUri, String toUri, String msgId, String status) {
		// Do not perform Message Delivery Status in Albatros for group chat
    	// Only perform delivery status delivered in GC
		if (RcsSettings.getInstance().isAlbatrosRelease() || !status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
			return;
		}
		if (logger.isActivated()) {
			logger.debug("Send delivery status delivered for message " + msgId);
		}
		// Send status in CPIM + IMDN headers
		String imdn = ChatUtils.buildDeliveryReport(msgId, status);
		String content = ChatUtils.buildCpimDeliveryReport(ImsModule.IMS_USER_PROFILE.getPublicUri(), toUri, imdn);

		// Send data
		sendDataChunks(IdGenerator.generateMessageID(), content, CpimMessage.MIME_TYPE, TypeMsrpChunk.MessageDeliveredReport);
    }

	/**
	 * Add a participant to the session
	 * 
	 * @param participant Participant
	 */
	public void addParticipant(ContactId participant) {
		try {
        	if (logger.isActivated()) {
        		logger.debug("Add one participant (" + participant + ") to the session");
        	}
    		
    		// Re-use INVITE dialog path
    		SessionAuthenticationAgent authenticationAgent = getAuthenticationAgent();
    		
    		// Increment the Cseq number of the dialog path   
            getDialogPath().incrementCseq();   

            // Send REFER request
    		if (logger.isActivated()) {
        		logger.debug("Send REFER");
        	}
    		String contactUri = PhoneUtils.formatContactIdToUri(participant);
	        SipRequest refer = SipMessageFactory.createRefer(getDialogPath(), contactUri, getSubject(), getContributionID());
    		SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);
	
	        // Analyze received message
            if (ctx.getStatusCode() == 407) {
                // 407 response received
            	if (logger.isActivated()) {
            		logger.debug("407 response received");
            	}

    	        // Set the Proxy-Authorization header
            	authenticationAgent.readProxyAuthenticateHeader(ctx.getSipResponse());

                // Increment the Cseq number of the dialog path
                getDialogPath().incrementCseq();

                // Create a second REFER request with the right token
                if (logger.isActivated()) {
                	logger.info("Send second REFER");
                }
    	        refer = SipMessageFactory.createRefer(getDialogPath(), contactUri, getSubject(), getContributionID());
                
    	        // Set the Authorization header
    	        authenticationAgent.setProxyAuthorizationHeader(refer);
                
                // Send REFER request
        		ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);

                // Analyze received message
                if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
                    // 200 OK response
                	if (logger.isActivated()) {
                		logger.debug("200 OK response received");
                	}
                	
        			// Notify listeners
        	    	for(int i=0; i < getListeners().size(); i++) {
        	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantSuccessful(participant);
        	        }
                } else {
                    // Error
                    if (logger.isActivated()) {
                    	logger.debug("REFER has failed (" + ctx.getStatusCode() + ")");
                    }
                    
        			// Notify listeners
        	    	for(int i=0; i < getListeners().size(); i++) {
        	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(participant, ctx.getReasonPhrase());
        	        }
                }
            } else
            if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
	            // 200 OK received
            	if (logger.isActivated()) {
            		logger.debug("200 OK response received");
            	}
            	
    			// Notify listeners
    	    	for(int i=0; i < getListeners().size(); i++) {
    	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantSuccessful(participant);
    	        }
	        } else {
	            // Error responses
            	if (logger.isActivated()) {
            		logger.debug("No response received");
            	}
            	
    			// Notify listeners
    	    	for(int i=0; i < getListeners().size(); i++) {
    	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(participant, ctx.getReasonPhrase());
    	        }
	        }
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("REFER request has failed", e);
        	}
        	
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(participant, e.getMessage());
	        }
        }
	}
	
	/**
	 * Add a set of participants to the session
	 * 
	 * @param participants set of participants
	 */
	public void addParticipants(Set<ContactId> participants) {
		try {
			if (participants.size() == 1) {
				addParticipant(participants.iterator().next());
				return;
			}
			
        	if (logger.isActivated()) {
        		logger.debug("Add " + participants.size()+ " participants to the session");
        	}
    		
    		// Re-use INVITE dialog path
    		SessionAuthenticationAgent authenticationAgent = getAuthenticationAgent();
    		
            // Increment the Cseq number of the dialog path
    		getDialogPath().incrementCseq();
            
	        // Send REFER request
    		if (logger.isActivated()) {
        		logger.debug("Send REFER");
        	}
	        SipRequest refer = SipMessageFactory.createRefer(getDialogPath(), participants, getSubject(), getContributionID());
	        SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);
	
	        // Analyze received message
            if (ctx.getStatusCode() == 407) {
                // 407 response received
            	if (logger.isActivated()) {
            		logger.debug("407 response received");
            	}

    	        // Set the Proxy-Authorization header
            	authenticationAgent.readProxyAuthenticateHeader(ctx.getSipResponse());

                // Increment the Cseq number of the dialog path
            	getDialogPath().incrementCseq();

    			// Create a second REFER request with the right token
                if (logger.isActivated()) {
                	logger.info("Send second REFER");
                }
    	        refer = SipMessageFactory.createRefer(getDialogPath(), participants, getSubject(), getContributionID());
                
    	        // Set the Authorization header
    	        authenticationAgent.setProxyAuthorizationHeader(refer);
                
                // Send REFER request
    	        ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);

                // Analyze received message
                if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
                    // 200 OK response
                	if (logger.isActivated()) {
                		logger.debug("20x OK response received");
                	}
                	
                    // Notify listeners
                    for (ContactId participant : participants) {
                        for (int i = 0; i < getListeners().size(); i++) {
                            ((ChatSessionListener)getListeners().get(i))
                                    .handleAddParticipantSuccessful(participant);
                        }
                    }
                } else {
                    // Error
                    if (logger.isActivated()) {
                    	logger.debug("REFER has failed (" + ctx.getStatusCode() + ")");
                    }
                    
                    // Notify listeners
                    for (ContactId participant : participants) {
                        for (int i = 0; i < getListeners().size(); i++) {
                            ((ChatSessionListener)getListeners().get(i))
                                    .handleAddParticipantFailed(participant, ctx.getReasonPhrase());
                        }
                    }
                }
            } else
            if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
	            // 200 OK received
            	if (logger.isActivated()) {
            		logger.debug("20x OK response received");
            	}
            	
                // Notify listeners
                for (ContactId participant : participants) {
                    for (int i = 0; i < getListeners().size(); i++) {
                        ((ChatSessionListener)getListeners().get(i))
                                .handleAddParticipantSuccessful(participant);
                    }
                }
	        } else {
	            // Error responses
            	if (logger.isActivated()) {
            		logger.debug("No response received");
            	}
            	
                // Notify listeners
                for (ContactId participant : participants) {
                    for (int i = 0; i < getListeners().size(); i++) {
                        ((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(
                                participant, ctx.getReasonPhrase());
                    }
                }
	        }
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("REFER request has failed", e);
        	}
        	
            // Notify listeners
            for (ContactId participant : participants) {
                for (int i = 0; i < getListeners().size(); i++) {
                    ((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(
                            participant, e.getMessage());
                }
            }
        }
	}
	
	/**
	 * Reject the session invitation
	 */
	public void rejectSession() {
		rejectSession(603);
	}

    /**
     * Create an INVITE request
     *
     * @return the INVITE request
     * @throws SipException 
     */
    public SipRequest createInvite() throws SipException {
        // Nothing to do in terminating side
        return null;
    }

    /**
     * Handle 200 0K response 
     *
     * @param resp 200 OK response
     */
    public void handle200OK(SipResponse resp) {
        super.handle200OK(resp);

        // Subscribe to event package
        getConferenceEventSubscriber().subscribe();
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSession#msrpDataReceived(java.lang.String, byte[], java.lang.String)
     */
    @Override
	public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
		if (logger.isActivated()) {
			logger.info("Data received (type " + mimeType + ")");
		}

		// Update the activity manager
		getActivityManager().updateActivity();

		if (data == null || data.length == 0) {
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
			receiveText(getRemoteContact(), StringUtils.decodeUTF8(data), null, false, new Date(), null);
			return;
		}
		if (!ChatUtils.isMessageCpimType(mimeType)) {
			// Not supported content
			if (logger.isActivated()) {
				logger.debug("Not supported content " + mimeType + " in chat session");
			}
			return;
		}
			
		// Receive a CPIM message
		CpimParser cpimParser = null;
		try {
			cpimParser = new CpimParser(data);
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't parse the CPIM message", e);
			}
			return;
		}
		CpimMessage cpimMsg = cpimParser.getCpimMessage();
		if (cpimMsg == null) {
			return;
		}
		Date date = cpimMsg.getMessageDate();
		String cpimMsgId = cpimMsg.getHeader(ImdnUtils.HEADER_IMDN_MSG_ID);
		if (cpimMsgId == null) {
			cpimMsgId = msgId;
		}

		String contentType = cpimMsg.getContentType();
		ContactId remoteId = getRemoteContact();
		String pseudo = null;
		// In GC, the MSRP 'FROM' header of the SEND message is set to the remote URI
		// Extract URI and optional display name to get pseudo and remoteId
		try {
			CpimIdentity cpimIdentity = new CpimIdentity(cpimMsg.getHeader(CpimMessage.HEADER_FROM));
			pseudo = cpimIdentity.getDisplayName();
			remoteId = ContactUtils.createContactId(cpimIdentity.getUri());
			if (logger.isActivated()) {
				logger.info("Cpim FROM Identity: " + cpimIdentity);
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.warn("Cannot parse FROM Cpim Identity: " + cpimMsg.getHeader(CpimMessage.HEADER_FROM));
			}
		}
		// Extract local contactId from "TO" header
		ContactId localId = null;
		try {
			CpimIdentity cpimIdentity = new CpimIdentity(cpimMsg.getHeader(CpimMessage.HEADER_TO));
			localId = ContactUtils.createContactId(cpimIdentity.getUri());
			if (logger.isActivated()) {
				logger.info("Cpim TO Identity: " + cpimIdentity);
			}
		} catch (Exception e) {
			// Purposely left blank
		}

		// Check if the message needs a delivery report
		String dispositionNotification = cpimMsg.getHeader(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);

		boolean isFToHTTP = FileTransferUtils.isFileTransferHttpType(contentType);

		// Analyze received message thanks to the MIME type
		if (isFToHTTP) {
			// File transfer over HTTP message
			// Parse HTTP document
			FileTransferHttpInfoDocument fileInfo = FileTransferUtils.parseFileTransferHttpDocument(cpimMsg.getMessageContent()
					.getBytes());
			if (fileInfo != null) {
				receiveHttpFileTransfer(remoteId, pseudo, fileInfo, cpimMsgId);
			} else {
				// TODO : else return error to Originating side
			}
			// Process delivery request
			sendMsrpMessageDeliveryStatus(remoteId, cpimMsgId, ImdnDocument.DELIVERY_STATUS_DELIVERED);
		} else {
			if (ChatUtils.isTextPlainType(contentType)) {
				// Text message
				receiveText(remoteId, StringUtils.decodeUTF8(cpimMsg.getMessageContent()), cpimMsgId, false, date, pseudo);
			} else {
				if (ChatUtils.isApplicationIsComposingType(contentType)) {
					// Is composing event
					receiveIsComposing(remoteId, cpimMsg.getMessageContent().getBytes());
				} else {
					if (ChatUtils.isMessageImdnType(contentType)) {
						// Delivery report
						try {
							ContactId me = ContactUtils.createContactId(ImsModule.IMS_USER_PROFILE.getPublicUri());
							// Only consider delivery report if sent to me
							if (localId != null && localId.equals(me)) {
								receiveMessageDeliveryStatus(remoteId, cpimMsg.getMessageContent());
							} else {
								if (logger.isActivated()) {
									logger.debug("Discard delivery report send to " + localId);
								}
							}
						} catch (RcsContactFormatException e) {
							// Purposely left blank
						}
					} else {
						if (ChatUtils.isGeolocType(contentType)) {
							// Geoloc message
							receiveGeoloc(remoteId, StringUtils.decodeUTF8(cpimMsg.getMessageContent()), cpimMsgId, false, date,
									pseudo);
						}
					}
				}
			}
			// Process delivery request
			if (dispositionNotification != null) {
				if (dispositionNotification.contains(ImdnDocument.POSITIVE_DELIVERY)) {
					// Positive delivery requested, send MSRP message with status "delivered"
					sendMsrpMessageDeliveryStatus(remoteId, cpimMsgId, ImdnDocument.DELIVERY_STATUS_DELIVERED);
				}
			}
		}
	}

}
