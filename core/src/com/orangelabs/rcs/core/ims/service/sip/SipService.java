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

package com.orangelabs.rcs.core.ims.service.sip;

import java.util.Enumeration;
import java.util.Vector;

import android.content.Intent;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.SessionAuthenticationAgent;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * SIP service
 * 
 * @author jexa7410
 */
public class SipService extends ImsService {
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @throws CoreException
     */
	public SipService(ImsModule parent) throws CoreException {
        super(parent, true);
	}

    /**
     * /** Start the IMS service
     */
	public synchronized void start() {
		if (isServiceStarted()) {
			// Already started
			return;
		}
		setServiceStarted(true);
	}

    /**
     * Stop the IMS service
     */
	public synchronized void stop() {
		if (!isServiceStarted()) {
			// Already stopped
			return;
		}
		setServiceStarted(false);
	}

	/**
     * Check the IMS service
     */
	public void check() {
	}

    /**
     * Initiate a session
     * 
     * @param contact Remote contact
     * @param featureTag Feature tag of the service
     * @param offer SDP offer
     * @return SIP session
     */
	public GenericSipSession initiateSession(String contact, String featureTag, String offer) {
		if (logger.isActivated()) {
			logger.info("Initiate a session with contact " + contact);
		}

		// Create a new session
		OriginatingSipSession session = new OriginatingSipSession(
				this,
				PhoneUtils.formatNumberToSipUri(contact),
				featureTag,
				offer);

		// Start the session
		session.startSession();
		return session;
	}

    /**
     * Receive a session invitation
     * 
     * @param intent Resolved intent
     * @param invite Initial invite
     */
	public void receiveSessionInvitation(Intent intent, SipRequest invite) {
		// Create a new session
    	TerminatingSipSession session = new TerminatingSipSession(
					this,
					invite);

		// Start the session
		session.startSession();

		// Update intent
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());
		intent.putExtra("contact", number);
		intent.putExtra("contactDisplayname", session.getRemoteDisplayName());
		intent.putExtra("sessionId", session.getSessionID());

		// Notify listener
		getImsModule().getCore().getListener().handleSipSessionInvitation(intent, session);
	}

    /**
     * Returns SIP sessions
     * 
     * @return List of sessions
     */
	public Vector<GenericSipSession> getSipSessions() {
		// Search all SIP sessions
		Vector<GenericSipSession> result = new Vector<GenericSipSession>();
		Enumeration<ImsServiceSession> list = getSessions();
		while(list.hasMoreElements()) {
			ImsServiceSession session = list.nextElement();
			if (session instanceof GenericSipSession) {
				result.add((GenericSipSession)session);
			}
		}

		return result;
    }

	/**
     * Returns SIP sessions with a given contact
     * 
     * @param contact Contact
     * @return List of sessions
     */
	public Vector<GenericSipSession> getSipSessionsWith(String contact) {
		// Search all SIP sessions
		Vector<GenericSipSession> result = new Vector<GenericSipSession>();
		Enumeration<ImsServiceSession> list = getSessions();
		while(list.hasMoreElements()) {
			ImsServiceSession session = list.nextElement();
			if ((session instanceof GenericSipSession) && PhoneUtils.compareNumbers(session.getRemoteContact(), contact)) {
				result.add((GenericSipSession)session);
			}
		}

		return result;
    }

	/**
	 * Send an instant message (SIP MESSAGE)
	 * 
     * @param contact Contact
	 * @param featureTag Feature tag of the service
     * @param content Content
     * @param contentType Content type
	 * @return True if successful else returns false
	 */
	public boolean sendInstantMessage(String contact, String featureTag, String content, String contentType) {
		boolean result = false;
		try {
			if (logger.isActivated()) {
       			logger.debug("Send instant message to " + contact);
       		}
			
		    // Create authentication agent 
       		SessionAuthenticationAgent authenticationAgent = new SessionAuthenticationAgent(getImsModule());
       		
       		// Create a dialog path
        	String contactUri = PhoneUtils.formatNumberToSipUri(contact);
        	SipDialogPath dialogPath = new SipDialogPath(
        			getImsModule().getSipManager().getSipStack(),
        			getImsModule().getSipManager().getSipStack().generateCallId(),
    				1,
    				contactUri,
    				ImsModule.IMS_USER_PROFILE.getPublicUri(),
    				contactUri,
    				getImsModule().getSipManager().getSipStack().getServiceRoutePath());        	
        	
	        // Create MESSAGE request
        	if (logger.isActivated()) {
        		logger.info("Send first MESSAGE");
        	}
	        SipRequest msg = SipMessageFactory.createMessage(dialogPath,
	        		featureTag,	contentType, content);
	        
	        // Send MESSAGE request
	        SipTransactionContext ctx = getImsModule().getSipManager().sendSipMessageAndWait(msg);
	
	        // Analyze received message
            if (ctx.getStatusCode() == 407) {
                // 407 response received
            	if (logger.isActivated()) {
            		logger.info("407 response received");
            	}

    	        // Set the Proxy-Authorization header
            	authenticationAgent.readProxyAuthenticateHeader(ctx.getSipResponse());

                // Increment the Cseq number of the dialog path
                dialogPath.incrementCseq();

                // Create a second MESSAGE request with the right token
                if (logger.isActivated()) {
                	logger.info("Send second MESSAGE");
                }
    	        msg = SipMessageFactory.createMessage(dialogPath,
    	        		featureTag,	contentType, content);

    	        // Set the Authorization header
    	        authenticationAgent.setProxyAuthorizationHeader(msg);
                
                // Send MESSAGE request
    	        ctx = getImsModule().getSipManager().sendSipMessageAndWait(msg);

                // Analyze received message
                if ((ctx.getStatusCode() == 200) || (ctx.getStatusCode() == 202)) {
                    // 200 OK response
                	if (logger.isActivated()) {
                		logger.info("20x OK response received");
                	}
                	result = true;
                } else {
                    // Error
                	if (logger.isActivated()) {
                		logger.info("Send instant message has failed: " + ctx.getStatusCode()
    	                    + " response received");
                	}
                }
            } else
            if ((ctx.getStatusCode() == 200) || (ctx.getStatusCode() == 202)) {
	            // 200 OK received
            	if (logger.isActivated()) {
            		logger.info("20x OK response received");
            	}
            	result = true;
	        } else {
	            // Error responses
            	if (logger.isActivated()) {
            		logger.info("Send instant message has failed: " + ctx.getStatusCode()
	                    + " response received");
            	}
	        }
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Can't send MESSAGE request", e);
        	}
        }
        return result;
	}

    /**
     * Receive an instant message
     * 
     * @param intent Resolved intent
     * @param message Instant message request
     */
	public void receiveInstantMessage(Intent intent, SipRequest message) {
		// Send a 200 OK response
		try {
			if (logger.isActivated()) {
				logger.info("Send 200 OK");
			}
	        SipResponse response = SipMessageFactory.createResponse(message,
	        		IdGenerator.getIdentifier(), 200);
			getImsModule().getSipManager().sendSipResponse(response);
		} catch(Exception e) {
	       	if (logger.isActivated()) {
	    		logger.error("Can't send 200 OK response", e);
	    	}
	       	return;
		}

		// Update intent
		String contact = SipUtils.getAssertedIdentity(message);
		String number = PhoneUtils.extractNumberFromUri(contact);
		intent.putExtra("contact", number);
		intent.putExtra("contactDisplayname", SipUtils.getDisplayNameFromUri(message.getFrom()));
		intent.putExtra("content", message.getContent());
		intent.putExtra("contentType", message.getContentType());
		
		// Notify listener
		getImsModule().getCore().getListener().handleSipInstantMessageReceived(intent);
	}
}
