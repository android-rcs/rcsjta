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

import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Rejoin a group chat session
 * 
 * @author jexa7410
 */
public class RejoinGroupChatSession extends GroupChatSession {
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param rejoinId Rejoin ID
	 * @param chatId Chat ID or contribution ID
	 * @param subject Subject
	 * @param participants List of participants 
	 */
	public RejoinGroupChatSession(ImsService parent, String rejoinId, String chatId, String subject, List<String> participants) {
		super(parent, rejoinId, new ListOfParticipant(participants));

		// Set subject
		if ((subject != null) && (subject.length() > 0)) {
			setSubject(subject);		
		}

		// Create dialog path
		createOriginatingDialogPath();
		
		// Set contribution ID
		setContributionID(chatId);
	}
	
	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Rejoin an existing group chat session");
	    	}

    		// Set setup mode
	    	String localSetup = createSetupOffer();
            if (logger.isActivated()){
				logger.debug("Local setup attribute is " + localSetup);
			}
            
	    	// Set local port
	    	int localMsrpPort = 9; // See RFC4145, Page 4
	    	
	    	// Build SDP part
	    	String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
	    	String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
	    	String sdp =
	    		"v=0" + SipUtils.CRLF +
	            "o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
	            "s=-" + SipUtils.CRLF +
				"c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
	            "t=0 0" + SipUtils.CRLF +			
	            "m=message " + localMsrpPort + " " + getMsrpMgr().getLocalSocketProtocol() + " *" + SipUtils.CRLF +
	            "a=path:" + getMsrpMgr().getLocalMsrpPath() + SipUtils.CRLF +
	            "a=setup:" + localSetup + SipUtils.CRLF +
	    		"a=accept-types:" + getAcceptTypes() + SipUtils.CRLF +
	            "a=accept-wrapped-types:" + getWrappedTypes() + SipUtils.CRLF +
	    		"a=sendrecv" + SipUtils.CRLF;

			// Set the local SDP part in the dialog path
	    	getDialogPath().setLocalContent(sdp);

	        // Create an INVITE request
	        if (logger.isActivated()) {
	        	logger.info("Send INVITE");
	        }
	        SipRequest invite = createInviteRequest(sdp);

	        // Set the Authorization header
	        getAuthenticationAgent().setAuthorizationHeader(invite);

	        // Set initial request in the dialog path
	        getDialogPath().setInvite(invite);
	        
	        // Send INVITE request
	        sendInvite(invite);	        
		} catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Session initiation has failed", e);
        	}

        	// Unexpected error
			handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
		}		
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
        return createInviteRequest(getDialogPath().getLocalContent());
    }

    /**
     * Handle 404 Session Not Found
     *
     * @param resp 404 response
     */
    public void handle404SessionNotFound(SipResponse resp) {
		// Rejoin session has failed, we update the database with sttaus terminted by remote
        RichMessaging.getInstance().addChatSessionTerminationByRemote(this);

		// Notify listener
        handleError(new ChatError(ChatError.SESSION_NOT_FOUND, resp.getReasonPhrase()));
    }
}
