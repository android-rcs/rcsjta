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
package com.orangelabs.rcs.core.ims.service.sip.streaming;

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionError;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionListener;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Originating SIP RTP session
 *  
 * @author Jean-Marc AUFFRET
 */
public class OriginatingSipRtpSession extends GenericSipRtpSession {
	/**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(OriginatingSipRtpSession.class.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param contact Remote contact Id
	 * @param featureTag Feature tag
	 */
	public OriginatingSipRtpSession(ImsService parent, ContactId contact, String featureTag) {
		super(parent, contact, featureTag);
		
		// Create dialog path
		createOriginatingDialogPath();
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a new RTP session as originating");
	    	}
	    	
			// Build SDP part
	    	String sdp = generateSdp();
	    	
	    	// Set the local SDP part in the dialog path
	        getDialogPath().setLocalContent(sdp);

	        // Create an INVITE request
	        if (logger.isActivated()) {
	        	logger.info("Send INVITE");
	        }
	        SipRequest invite = createInvite();

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
			handleError(new SipSessionError(SipSessionError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
		}
	}

	@Override
	public boolean isInitiatedByRemote() {
		return false;
	}
	
	@Override
	public void handle180Ringing(SipResponse response) {
		if (logger.isActivated()) {
			logger.debug("handle180Ringing");
		}
		// Notify listeners
		for (ImsSessionListener listener : getListeners()) {
			((SipSessionListener)listener).handle180Ringing();
		}
	}
}
