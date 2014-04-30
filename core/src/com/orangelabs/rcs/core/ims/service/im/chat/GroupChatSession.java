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

import java.util.List;

import javax2.sip.header.ExtensionHeader;

import com.gsma.services.rcs.chat.ChatLog;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.SessionAuthenticationAgent;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.event.ConferenceEventSubscribeManager;
import com.orangelabs.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Abstract Group chat session
 * 
 * @author Jean-Marc AUFFRET
 */
/**
 * @author YPLO6403
 *
 */
public abstract class GroupChatSession extends ChatSession {
	/**
	 * Conference event subscribe manager
	 */
	private ConferenceEventSubscribeManager conferenceSubscriber = new ConferenceEventSubscribeManager(this); 
		
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
	 * Constructor for originating side
	 * 
	 * @param parent IMS service
	 * @param conferenceId Conference id
	 * @param participants List of invited participants
	 */
	public GroupChatSession(ImsService parent, String conferenceId, ListOfParticipant participants) {
		super(parent, conferenceId, participants);
		
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

	/* (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSession#isGroupChat()
	 */
	public boolean isGroupChat() {
		return true;
	}
	
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSession#getConnectedParticipants()
     */
    public ListOfParticipant getConnectedParticipants() {
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
     * Receive BYE request 
     * 
     * @param bye BYE request
     */
    public void receiveBye(SipRequest bye) {
        // Stop conference subscription
        conferenceSubscriber.terminate();
        
        // Receive BYE request
        super.receiveBye(bye);
    }
    
    /**
     * Receive CANCEL request 
     * 
     * @param cancel CANCEL request
     */
    public void receiveCancel(SipRequest cancel) {
        // Stop conference subscription
        conferenceSubscriber.terminate();
        
        // Receive CANCEL request
        super.receiveCancel(cancel);
	}	
	
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSession#sendTextMessage(java.lang.String)
     */
    @Override
	public void sendTextMessage( String msgId, String txt) {
		boolean useImdn = getImdnManager().isImdnActivated();
        String imdnMsgId = null;
       
		String from = ImsModule.IMS_USER_PROFILE.getPublicAddress();
		String to = ChatUtils.ANOMYNOUS_URI;
		String content;
		if (useImdn) {
			// Send message in CPIM + IMDN delivered
            imdnMsgId = IdGenerator.generateMessageID();
			content = ChatUtils.buildCpimMessageWithDeliveredImdn(from, to, imdnMsgId, StringUtils.encodeUTF8(txt), InstantMessage.MIME_TYPE);
		} else {
			// Send message in CPIM
			content = ChatUtils.buildCpimMessage(from, to, StringUtils.encodeUTF8(txt), InstantMessage.MIME_TYPE);
		}		
		
		// Send data
		boolean result = sendDataChunks(msgId, content, CpimMessage.MIME_TYPE, TypeMsrpChunk.TextMessage);

        // Use IMDN MessageID as reference if existing
        if (useImdn) {
            msgId = imdnMsgId;
        }

		// Update rich messaging history
		InstantMessage msg = new InstantMessage(msgId, getRemoteContact(), txt, useImdn, null);
		RichMessagingHistory.getInstance().addGroupChatMessage(getContributionID(), msg,
				ChatLog.Message.Direction.OUTGOING);

		// Check if message has been sent with success or not
		if (!result) {
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
			
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleMessageDeliveryStatus(msgId, ImdnDocument.DELIVERY_STATUS_FAILED, null);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSession#sendGeolocMessage(com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush)
	 */
    @Override
	public void sendGeolocMessage(String msgId, GeolocPush geoloc) {
		boolean useImdn = getImdnManager().isImdnActivated();
        String imdnMsgId = null;
		String from = ImsModule.IMS_USER_PROFILE.getPublicAddress();
		String to = ChatUtils.ANOMYNOUS_URI;
		String geoDoc = ChatUtils.buildGeolocDocument(geoloc, ImsModule.IMS_USER_PROFILE.getPublicUri(), msgId);
		
		String content;
		if (useImdn) {
			// Send message in CPIM + IMDN delivered
            imdnMsgId = IdGenerator.generateMessageID();
			content = ChatUtils.buildCpimMessageWithDeliveredImdn(from, to, imdnMsgId, geoDoc, GeolocInfoDocument.MIME_TYPE);
		} else {
			// Send message in CPIM
			content = ChatUtils.buildCpimMessage(from, to, geoDoc, GeolocInfoDocument.MIME_TYPE);
		}
		
		// Send data
		boolean result = sendDataChunks(msgId, content, CpimMessage.MIME_TYPE, TypeMsrpChunk.GeoLocation);

        // Use IMDN MessageID as reference if existing
        if (useImdn) {
            msgId = imdnMsgId;
        }

		// Update rich messaging history
		GeolocMessage geolocMsg = new GeolocMessage(msgId, getRemoteContact(), geoloc, useImdn, null);
		RichMessagingHistory.getInstance().addGroupChatMessage(getContributionID(), geolocMsg,
				ChatLog.Message.Direction.OUTGOING);

		// Check if message has been sent with success or not
		if (!result) {
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
			
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleMessageDeliveryStatus(msgId, ImdnDocument.DELIVERY_STATUS_FAILED, null);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSession#sendIsComposingStatus(boolean)
	 */
    @Override
	public void sendIsComposingStatus(boolean status) {
		String from = ImsModule.IMS_USER_PROFILE.getPublicUri();
		String to = ChatUtils.ANOMYNOUS_URI;
		String msgId = IdGenerator.generateMessageID();
		String content = ChatUtils.buildCpimMessage(from, to, IsComposingInfo.buildIsComposingInfo(status), IsComposingInfo.MIME_TYPE);
		sendDataChunks(msgId, content, CpimMessage.MIME_TYPE, TypeMsrpChunk.IsComposing);	
	}

    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSession#sendMsrpMessageDeliveryStatus(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void sendMsrpMessageDeliveryStatus(String contact, String msgId, String status) {
        // Do not perform Message Delivery Status in Albatros for group chat 
        if (RcsSettings.getInstance().isAlbatrosRelease()) {
            return;
        }
      
        // Changed by Deutsche Telekom
        final String from = ImsModule.IMS_USER_PROFILE.getPublicUri();
        final String to = contact;
        sendMsrpMessageDeliveryStatus(contact, from, to, msgId, status);
    }

	/**
	 * Add a participant to the session
	 * 
	 * @param participant Participant
	 */
	public void addParticipant(String participant) {
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
    		String contactUri = PhoneUtils.formatNumberToSipUri(participant);
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
        	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantSuccessful();
        	        }
                } else {
                    // Error
                    if (logger.isActivated()) {
                    	logger.debug("REFER has failed (" + ctx.getStatusCode() + ")");
                    }
                    
        			// Notify listeners
        	    	for(int i=0; i < getListeners().size(); i++) {
        	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(ctx.getReasonPhrase());
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
    	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantSuccessful();
    	        }
	        } else {
	            // Error responses
            	if (logger.isActivated()) {
            		logger.debug("No response received");
            	}
            	
    			// Notify listeners
    	    	for(int i=0; i < getListeners().size(); i++) {
    	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(ctx.getReasonPhrase());
    	        }
	        }
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("REFER request has failed", e);
        	}
        	
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(e.getMessage());
	        }
        }
	}

	/**
	 * Add a list of participants to the session
	 * 
	 * @param participants List of participants
	 */
	public void addParticipants(List<String> participants) {
		try {
			if (participants.size() == 1) {
				addParticipant(participants.get(0));
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
        	    	for(int i=0; i < getListeners().size(); i++) {
        	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantSuccessful();
        	        }
                } else {
                    // Error
                    if (logger.isActivated()) {
                    	logger.debug("REFER has failed (" + ctx.getStatusCode() + ")");
                    }
                    
        			// Notify listeners
        	    	for(int i=0; i < getListeners().size(); i++) {
        	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(ctx.getReasonPhrase());
        	        }
                }
            } else
            if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
	            // 200 OK received
            	if (logger.isActivated()) {
            		logger.debug("20x OK response received");
            	}
            	
    			// Notify listeners
    	    	for(int i=0; i < getListeners().size(); i++) {
    	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantSuccessful();
    	        }
	        } else {
	            // Error responses
            	if (logger.isActivated()) {
            		logger.debug("No response received");
            	}
            	
    			// Notify listeners
    	    	for(int i=0; i < getListeners().size(); i++) {
    	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(ctx.getReasonPhrase());
    	        }
	        }
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("REFER request has failed", e);
        	}
        	
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(e.getMessage());
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
}
