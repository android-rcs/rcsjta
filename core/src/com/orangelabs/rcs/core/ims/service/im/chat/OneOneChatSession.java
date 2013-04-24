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

import java.util.ArrayList;
import java.util.List;

import javax2.sip.header.SubjectHeader;

import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.service.api.client.messaging.GeolocMessage;
import com.orangelabs.rcs.service.api.client.messaging.GeolocPush;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.utils.StringUtils;

/**
 * Abstract 1-1 chat session
 * 
 * @author jexa7410
 */
public abstract class OneOneChatSession extends ChatSession {
	/**
	 * Boundary tag
	 */
	private final static String BOUNDARY_TAG = "boundary1";

    /**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param contact Remote contact
	 */
	public OneOneChatSession(ImsService parent, String contact) {
		super(parent, contact);
		
		// Set list of participants
		ListOfParticipant participants = new ListOfParticipant();
		participants.addParticipant(contact);
		setParticipants(participants);		
	}
	
	/**
	 * Is group chat
	 * 
	 * @return Boolean
	 */
	public boolean isGroupChat() {
		return false;
	}
	
	/**
	 * Returns the list of participants currently connected to the session
	 * 
	 * @return List of participants
	 */
    public ListOfParticipant getConnectedParticipants() {
		return getParticipants();
	}

    /**
     * Close media session
     */
    public void closeMediaSession() {
        // Stop the activity manager
        getActivityManager().stop();

        // Close MSRP session
        closeMsrpSession();
    }
    
	/**
	 * Send a text message
	 * 
	 * @param id Message-ID
	 * @param txt Text message
	 */
	public void sendTextMessage(String msgId, String txt) {
		boolean useImdn = getImdnManager().isImdnActivated();
		String mime = CpimMessage.MIME_TYPE;
		String from = ChatUtils.ANOMYNOUS_URI;
		String to = ChatUtils.ANOMYNOUS_URI;

		String content;
		if (useImdn) {
			// Send message in CPIM + IMDN
			content = ChatUtils.buildCpimMessageWithImdn(from, to, msgId, StringUtils.encodeUTF8(txt), InstantMessage.MIME_TYPE);
		} else {
			// Send message in CPIM
			content = ChatUtils.buildCpimMessage(from, to, StringUtils.encodeUTF8(txt), InstantMessage.MIME_TYPE);
		}

		// Send content
		boolean result = sendDataChunks(msgId, content, mime);

		// Update rich messaging history
		InstantMessage msg = new InstantMessage(msgId, getRemoteContact(), txt, useImdn);
		RichMessaging.getInstance().addOutgoingChatMessage(msg, this);

		// Check if message has been sent with success or not
		if (!result) {
			// Update rich messaging history
			RichMessaging.getInstance().markChatMessageFailed(msgId);
			
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleMessageDeliveryStatus(msgId, ImdnDocument.DELIVERY_STATUS_FAILED);
			}
		}
	}

	/**
	 * Send a geoloc message
	 * 
	 * @param msgId Message ID
	 * @param geoloc Geoloc info
	 */
	public void sendGeolocMessage(String msgId, GeolocPush geoloc) {
		boolean useImdn = getImdnManager().isImdnActivated();
		String mime = CpimMessage.MIME_TYPE;
		String from = ChatUtils.ANOMYNOUS_URI;
		String to = ChatUtils.ANOMYNOUS_URI;
		String geoDoc = ChatUtils.buildGeolocDocument(geoloc, ImsModule.IMS_USER_PROFILE.getPublicUri(), msgId);

		String content;
		if (useImdn) {
			// Send message in CPIM + IMDN
			content = ChatUtils.buildCpimMessageWithImdn(from, to, msgId, geoDoc, GeolocInfoDocument.MIME_TYPE);
		} else {
			// Send message in CPIM
			content = ChatUtils.buildCpimMessage(from, to, geoDoc, GeolocInfoDocument.MIME_TYPE);
		}

		// Send content
		boolean result = sendDataChunks(msgId, content, mime);

		// Update rich messaging history
		GeolocMessage geolocMsg = new GeolocMessage(msgId, getRemoteContact(), geoloc, useImdn);
		RichMessaging.getInstance().addOutgoingGeoloc(geolocMsg, this);

		// Check if message has been sent with success or not
		if (!result) {
			// Update rich messaging history
			RichMessaging.getInstance().markChatMessageFailed(msgId);
			
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleMessageDeliveryStatus(msgId, ImdnDocument.DELIVERY_STATUS_FAILED);
			}
		}
	}
	
	/**
	 * Send is composing status
	 * 
	 * @param status Status
	 */
	public void sendIsComposingStatus(boolean status) {
		String content = IsComposingInfo.buildIsComposingInfo(status);
		String msgId = ChatUtils.generateMessageId();
		sendDataChunks(msgId, content, IsComposingInfo.MIME_TYPE);
	}
	
	/**
	 * Add a participant to the session
	 * 
	 * @param participant Participant
	 */
	public void addParticipant(String participant) {
		ArrayList<String> participants = new ArrayList<String>();
		participants.add(participant);
		addParticipants(participants);
	}

	/**
	 * Add a list of participants to the session
	 * 
	 * @param participants List of participants
	 */
	public void addParticipants(List<String> participants) {
		// Build the list of participants
    	String existingParticipant = getParticipants().getList().get(0);
    	participants.add(existingParticipant);
		
		// Create a new session
		ExtendOneOneChatSession session = new ExtendOneOneChatSession(
			getImsService(),
			ImsModule.IMS_USER_PROFILE.getImConferenceUri(),
			this,
			new ListOfParticipant(participants));
		
		// Start the session
		session.startSession();
	}
	
	/**
	 * Send message delivery status via MSRP
	 * 
	 * @param contact Contact that requested the delivery status
	 * @param msgId Message ID
	 * @param status Status
	 */
	public void sendMsrpMessageDeliveryStatus(String contact, String msgId, String status) {
		// Send status in CPIM + IMDN headers
		String from = ChatUtils.ANOMYNOUS_URI;
		String to = ChatUtils.ANOMYNOUS_URI;
		String imdn = ChatUtils.buildDeliveryReport(msgId, status);
		String content = ChatUtils.buildCpimDeliveryReport(from, to, imdn);
		
		// Send data
		boolean result = sendDataChunks(msgId, content, CpimMessage.MIME_TYPE);
		if (result) {
			// Update rich messaging history
			RichMessaging.getInstance().setChatMessageDeliveryStatus(msgId, status);
		}
	}	

	/**
	 * Reject the session invitation
	 */
	public void rejectSession() {
		rejectSession(486);
	}

    /**
     * Create INVITE request
     * 
     * @param content Content part
     * @return Request
     * @throws SipException
     */
    private SipRequest createMultipartInviteRequest(String content) throws SipException {
    	SipRequest invite = SipMessageFactory.createMultipartInvite(getDialogPath(), 
                    getFeatureTags(), 
                    content,
                    BOUNDARY_TAG);

        // Test if there is a first message
        if (getFirstMessage() != null) {
            // Add a subject header
            invite.addHeader(SubjectHeader.NAME, StringUtils.encodeUTF8(getFirstMessage().getTextMessage()));
        }

        // Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID()); 

        return invite;
    }

    /**
     * Create INVITE request
     * 
     * @param content Content part
     * @return Request
     * @throws SipException
     */
    private SipRequest createInviteRequest(String content) throws SipException {
    	SipRequest invite = SipMessageFactory.createInvite(getDialogPath(), 
                    InstantMessagingService.CHAT_FEATURE_TAGS, 
                    content);

        // Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID()); 

        return invite;
    }

    /**
     * Create an INVITE request
     *
     * @return the INVITE request
     * @throws SipException 
     */
    public SipRequest createInvite() throws SipException {
        // If there is a first message then builds a multipart content else builds a SDP content
        SipRequest invite; 
        if (getFirstMessage() != null) {
            invite = createMultipartInviteRequest(getDialogPath().getLocalContent());
        } else {
            invite = createInviteRequest(getDialogPath().getLocalContent());
        }
        return invite;
    }

    /**
     * Handle 200 0K response 
     *
     * @param resp 200 OK response
     */
    public void handle200OK(SipResponse resp) {
        super.handle200OK(resp);

        // Start the activity manager
        getActivityManager().start();
    }
}
