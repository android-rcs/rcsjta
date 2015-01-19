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

import javax2.sip.header.SubjectHeader;

import android.text.TextUtils;

import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Rejoin a group chat session
 * 
 * @author Jean-Marc AUFFRET
 */
public class RejoinGroupChatSession extends GroupChatSession {
	/**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(RejoinGroupChatSession.class.getSimpleName());

    /**
	 * Constructor
	 * 
	 * @param parent IMS service
     * @param groupChatInfo Group Chat information
     * @param rcsSettings Rcs settings
     * @param messagingLog Messaging log
	 */
	public RejoinGroupChatSession(ImsService parent, GroupChatInfo groupChatInfo,
			RcsSettings rcsSettings, MessagingLog messagingLog) {
		super(parent, null, groupChatInfo.getRejoinId(), groupChatInfo.getParticipants(),
				rcsSettings, messagingLog);

		// Set subject
		if (!TextUtils.isEmpty(groupChatInfo.getSubject())) {
			setSubject(groupChatInfo.getSubject());		
		}

		// Create dialog path
		createOriginatingDialogPath();
		
		// Set contribution ID
		setContributionID(groupChatInfo.getContributionId());
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
            int localMsrpPort;
            if ("active".equals(localSetup)) {
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
                getFeatureTags(),
                getAcceptContactTags(),
        		content);

        // Test if there is a subject
        if (getSubject() != null) {
            // Add a subject header
            invite.addHeader(SubjectHeader.NAME, getSubject());
        }

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
		// Rejoin session has failed, we update the database with status terminated by remote

		// TODO Once after CR18 is implemented we will check if this callback is
		// really required and act accordingly

		//MessagingLog.getInstance().updateGroupChatStatus(getContributionID(),
				//GroupChat.State.TERMINATED, GroupChat.ReasonCode.NONE);

        handleError(new ChatError(ChatError.SESSION_NOT_FOUND, resp.getReasonPhrase()));
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
}
