/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.service.im.chat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax2.sip.header.SubjectHeader;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.StringUtils;

/**
 * Abstract 1-1 chat session
 * 
 * @author Jean-Marc AUFFRET
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
		super(parent, contact, OneOneChatSession.generateOneOneParticipants(contact));
		
		// Set feature tags
        List<String> featureTags = ChatUtils.getSupportedFeatureTagsForChat();
        setFeatureTags(featureTags);
		
        // Set Accept-Contact header
        setAcceptContactTags(featureTags);

		// Set accept-types
		String acceptTypes = CpimMessage.MIME_TYPE + " " + IsComposingInfo.MIME_TYPE;
        setAcceptTypes(acceptTypes);
				
		// Set accept-wrapped-types
		String wrappedTypes = InstantMessage.MIME_TYPE + " " + ImdnDocument.MIME_TYPE;
		if (RcsSettings.getInstance().isGeoLocationPushSupported()) {
        	wrappedTypes += " " + GeolocInfoDocument.MIME_TYPE;
        }
		if (RcsSettings.getInstance().isFileTransferHttpSupported()) {
        	wrappedTypes += " " + FileTransferHttpInfoDocument.MIME_TYPE;
        }
        setWrappedTypes(wrappedTypes);
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
	 * Generate the list of participants for a 1-1 chat
	 * 
	 * @param contact Contact
	 * @return List of participants
	 */
    private static Set<ParticipantInfo> generateOneOneParticipants(String contact) {
    	Set<ParticipantInfo> set = new HashSet<ParticipantInfo>();
    	ParticipantInfoUtils.addParticipant(set, contact);
		return set;
	}

    /**
	 * Returns the list of participants currently connected to the session
	 * 
	 * @return List of participants
	 */
    public Set<ParticipantInfo> getConnectedParticipants() {
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
        String imdnMsgId = null;
        String mime = CpimMessage.MIME_TYPE;
		String from = ChatUtils.ANOMYNOUS_URI;
		String to = ChatUtils.ANOMYNOUS_URI;

		String content;
		if (useImdn) {
            // Send message in CPIM + IMDN
            imdnMsgId = IdGenerator.generateMessageID();
			content = ChatUtils.buildCpimMessageWithImdn(from, to, imdnMsgId, StringUtils.encodeUTF8(txt), InstantMessage.MIME_TYPE);
		} else {
			// Send message in CPIM
			content = ChatUtils.buildCpimMessage(from, to, StringUtils.encodeUTF8(txt), InstantMessage.MIME_TYPE);
		}

		// Send content
		boolean result = sendDataChunks(msgId, content, mime, MsrpSession.TypeMsrpChunk.TextMessage);

        // Use IMDN MessageID as reference if existing
        if (useImdn) {
            msgId = imdnMsgId;
        }

		// Update rich messaging history
		InstantMessage msg = new InstantMessage(msgId, getRemoteContact(), txt, useImdn, null);
		RichMessagingHistory.getInstance().addChatMessage(msg, ChatLog.Message.Direction.OUTGOING);

		// Check if message has been sent with success or not
		if (!result) {
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
			
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleSendMessageFailure(msgId);
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
        String imdnMsgId = null;
		String mime = CpimMessage.MIME_TYPE;
		String from = ChatUtils.ANOMYNOUS_URI;
		String to = ChatUtils.ANOMYNOUS_URI;
		String geoDoc = ChatUtils.buildGeolocDocument(geoloc, ImsModule.IMS_USER_PROFILE.getPublicUri(), msgId);

		String content;
		if (useImdn) {
			// Send message in CPIM + IMDN
            imdnMsgId = IdGenerator.generateMessageID();
			content = ChatUtils.buildCpimMessageWithImdn(from, to, imdnMsgId, geoDoc, GeolocInfoDocument.MIME_TYPE);
		} else {
			// Send message in CPIM
			content = ChatUtils.buildCpimMessage(from, to, geoDoc, GeolocInfoDocument.MIME_TYPE);
		}

		// Send content
		boolean result = sendDataChunks(msgId, content, mime, MsrpSession.TypeMsrpChunk.GeoLocation);

        // Use IMDN MessageID as reference if existing
        if (useImdn) {
            msgId = imdnMsgId;
        }

		// Update rich messaging history
		GeolocMessage geolocMsg = new GeolocMessage(msgId, getRemoteContact(), geoloc, useImdn, null);
		RichMessagingHistory.getInstance().addChatMessage(geolocMsg, ChatLog.Message.Direction.OUTGOING);

		// Check if message has been sent with success or not
		if (!result) {
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
			
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleSendMessageFailure(msgId);
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
		String msgId = IdGenerator.generateMessageID();
		sendDataChunks(msgId, content, IsComposingInfo.MIME_TYPE, MsrpSession.TypeMsrpChunk.IsComposing);
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

        // Test if there is a text message
        if ((getFirstMessage() != null) && (getFirstMessage().getTextMessage() != null)) {
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

    /**
     * Get SDP direction
     *
     * @return Direction
     *
     * @see com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils#DIRECTION_RECVONLY
     * @see com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils#DIRECTION_SENDONLY
     * @see com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils#DIRECTION_SENDRECV
     */
    public abstract String getDirection();
    
    // Changed by Deutsche Telekom
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSession#msrpTransferError(java.lang.String, java.lang.String, com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk)
     */
    @Override
    public void msrpTransferError(String msgId, String error, MsrpSession.TypeMsrpChunk typeMsrpChunk) {
    	super.msrpTransferError(msgId, error, typeMsrpChunk);
    	
        // Request capabilities
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(getDialogPath().getRemoteParty());
    }

}
