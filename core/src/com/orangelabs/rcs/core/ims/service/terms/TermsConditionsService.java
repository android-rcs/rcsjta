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

package com.orangelabs.rcs.core.ims.service.terms;

import java.io.ByteArrayInputStream;
import java.util.Locale;

import org.xml.sax.InputSource;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.SessionAuthenticationAgent;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Terms & conditions service via SIP
 * 
 * @author jexa7410
 */
public class TermsConditionsService extends ImsService {
	/**
	 * Request MIME type
	 */
	private final static String REQUEST_MIME_TYPE = "application/end-user-confirmation-request+xml";
	
	/**
	 * Ack MIME type
	 */
	private final static String ACK_MIME_TYPE = "application/end-user-confirmation-ack+xml";

    /**
     * User notification MIME type
     */
    private final static String USER_NOTIFICATION_MIME_TYPE = "application/end-user-notification-request+xml";

	/**
	 * Response MIME type
	 */
	private final static String RESPONSE_MIME_TYPE = "application/end-user-confirmation-response+xml";
	
	/**
	 * Accept response
	 */
	private final static String ACCEPT_RESPONSE = "accept";

	/**
	 * Decline response
	 */
	private final static String DECLINE_RESPONSE = "decline";

	/**
	 * Remote server
	 */
	private String remoteServer;
	
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
	public TermsConditionsService(ImsModule parent) throws CoreException {
        super(parent, true);
        
        // Get remote server URI
		remoteServer = RcsSettings.getInstance().getEndUserConfirmationRequestUri();
	}

	/**
	 * Start the IMS service
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
     * Receive a SIP message
     * 
     * @param message Received message
     */
    public void receiveMessage(SipRequest message) {
    	if (logger.isActivated()) {
    		logger.debug("Receive terms message");
    	}
    	
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

		// Parse received message
		try {
		    String lang = Locale.getDefault().getLanguage();
	    	if (message.getContentType().equals(REQUEST_MIME_TYPE)) {
		    	// Parse terms request
				InputSource input = new InputSource(new ByteArrayInputStream(message.getContentBytes()));
				TermsRequestParser parser = new TermsRequestParser(input, lang);

				// Notify listener
	    		getImsModule().getCore().getListener().handleUserConfirmationRequest(
	    				getRemoteIdentity(message),
	    				parser.getId(),
	    				parser.getType(),
	    				parser.getPin(),
	    				parser.getSubject(),
	    				parser.getText(),
                        parser.getButtonAccept(),
                        parser.getButtonReject(),
                        parser.getTimeout()
                        );
	    	} else
	    	if (message.getContentType().equals(ACK_MIME_TYPE)) {
		    	// Parse terms ack
				InputSource input = new InputSource(new ByteArrayInputStream(message.getContentBytes()));
				TermsAckParser parser = new TermsAckParser(input);

                // Notify listener
                getImsModule().getCore().getListener().handleUserConfirmationAck(
                        getRemoteIdentity(message),
                        parser.getId(),
                        parser.getStatus(),
                        parser.getSubject(),
                        parser.getText());
            } else
            if (message.getContentType().equals(USER_NOTIFICATION_MIME_TYPE)) {
                // Parse terms notification
                InputSource input = new InputSource(new ByteArrayInputStream(message.getContentBytes()));
                EndUserNotificationParser parser = new EndUserNotificationParser(input, lang);

                // Notify listener
                getImsModule().getCore().getListener().handleUserNotification(
                        getRemoteIdentity(message),
                        parser.getId(),
                        parser.getSubject(),
                        parser.getText(),
                        parser.getButtonOk());
            } else {
                if (logger.isActivated()) {
                    logger.warn("Unknown terms request " + message.getContentType());
                }
	    	}
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Can't parse terms request", e);
    		}
    	}
    }

    /**
	 * Accept terms
	 * 
	 * @param id Request ID
	 * @param pin Response value
	 * @return Boolean result
	 */
	public boolean acceptTerms(String id, String pin) {
		if (logger.isActivated()) {
			logger.debug("Send response for request " + id);
		}
		
		// Send SIP MESSAGE
		return sendSipMessage(remoteServer, id, ACCEPT_RESPONSE, pin);
	}

	/**
	 * Reject terms
	 * 
	 * @param id Request ID
	 * @param pin Response value
	 * @return Boolean result
	 */
	public boolean rejectTerms(String id, String pin) {
		if (logger.isActivated()) {
			logger.debug("Send response for request " + id);
		}

		// Send SIP MESSAGE
		return sendSipMessage(remoteServer, id, DECLINE_RESPONSE, pin);
	}

	/**
	 * Send SIP MESSAGE
	 * 
	 * @param remote Remote server
	 * @param id Request ID
	 * @param value Response value
	 * @param pin Response value
	 * @return Boolean result
	 */
	private boolean sendSipMessage(String remote, String id, String value, String pin) {
		if (StringUtils.isEmpty(remote)) {
			if (logger.isActivated()) {
       			logger.error("Remote URI not set");
       		}
			return false;
		}
		
		if (StringUtils.isEmpty(id)) {
			if (logger.isActivated()) {
       			logger.error("Request ID not set");
       		}
			return false;
		}

		boolean result = false;
		try {
			if (logger.isActivated()) {
       			logger.debug("Send SIP response");
       		}

			// Build response
			String response = "<?xml version=\"1.0\" standalone=\"yes\"?>" +
					"<EndUserConfirmationResponse id=\"" + id + "\" value=\"" + value + "\"";
			if (pin != null) {
				response += " pin=\"";
			}
			response += "/>";
			
		    // Create authentication agent 
       		SessionAuthenticationAgent authenticationAgent = new SessionAuthenticationAgent(getImsModule());
       		
       		// Create a dialog path
        	SipDialogPath dialogPath = new SipDialogPath(
        			getImsModule().getSipManager().getSipStack(),
        			getImsModule().getSipManager().getSipStack().generateCallId(),
    				1,
    				remote,
    				ImsModule.IMS_USER_PROFILE.getPublicUri(),
    				remote,
    				getImsModule().getSipManager().getSipStack().getServiceRoutePath());        	
        	
	        // Create MESSAGE request
        	if (logger.isActivated()) {
        		logger.info("Send first MESSAGE");
        	}
	        SipRequest msg = SipMessageFactory.createMessage(dialogPath, RESPONSE_MIME_TYPE, response);
	        
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
    	        msg = SipMessageFactory.createMessage(dialogPath, RESPONSE_MIME_TYPE, response);
    	        
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
                		logger.info("Delivery report has failed: " + ctx.getStatusCode()
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
            		logger.info("Delivery report has failed: " + ctx.getStatusCode()
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
	 * Get remote identity of the incoming request
	 * 
     * @param request Request
     * @return ID
	 */
	private String getRemoteIdentity(SipRequest request) {
		// Use the Asserted-Identity header
		return SipUtils.getAssertedIdentity(request);
	}

	/**
	 * Is a terms & conditions request
	 * 
     * @param request Request
     * @return Boolean
	 */
	public static boolean isTermsRequest(SipRequest request) {
    	String contentType = request.getContentType();
    	if ((contentType != null) &&
                contentType.startsWith("application/end-user")) {
    		return true;
    	} else {
    		return false;
    	}
	}
}
