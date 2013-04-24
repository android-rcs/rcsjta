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

package com.orangelabs.rcs.core.ims.service.presence;

import java.util.Vector;

import javax2.sip.header.ExpiresHeader;
import javax2.sip.header.SIPETagHeader;

import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.SessionAuthenticationAgent;
import com.orangelabs.rcs.platform.registry.RegistryFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.PeriodicRefresher;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Publish manager for sending current user presence status
 *
 * @author JM. Auffret
 */
public class PublishManager extends PeriodicRefresher {
	/**
	 * Last min expire period (in seconds)
	 */
	private static final String REGISTRY_MIN_EXPIRE_PERIOD = "MinPublishExpirePeriod";

	/**
	 * Last SIP Etag
	 */
	private static final String REGISTRY_SIP_ETAG = "SipEntityTag";

	/**
	 * SIP Etag expiration (in milliseconds)
	 */
	private static final String REGISTRY_SIP_ETAG_EXPIRATION = "SipETagExpiration";	
	
	/**
     * IMS module
     */
    private ImsModule imsModule;
    
    /**
     * Expire period
     */
    private int expirePeriod;

    /**
     * Dialog path
     */
    private SipDialogPath dialogPath = null;

    /**
     * Entity tag
     */
    private String entityTag = null;

    /**
     * Published flag
     */
    private boolean published = false;
    
	/**
	 * Authentication agent
	 */
	private SessionAuthenticationAgent authenticationAgent;

	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     */
    public PublishManager(ImsModule parent) {
    	this.imsModule = parent;
		this.authenticationAgent = new SessionAuthenticationAgent(imsModule);

    	int defaultExpirePeriod = RcsSettings.getInstance().getPublishExpirePeriod();
    	int minExpireValue = RegistryFactory.getFactory().readInteger(REGISTRY_MIN_EXPIRE_PERIOD, -1);
    	if ((minExpireValue != -1) && (defaultExpirePeriod < minExpireValue)) {
        	this.expirePeriod = minExpireValue;
    	} else {
    		this.expirePeriod = defaultExpirePeriod;
    	}

		// Restore the last SIP-ETag from the registry
        readEntityTag();
    }

    /**
     * Is published
     * 
     * @return Return True if the terminal has published, else return False
     */
    public boolean isPublished() {
        return published;
    }

    /**
     * Terminate manager
     */
    public void terminate() {
    	if (logger.isActivated()) {
    		logger.info("Terminate the publish manager");
    	}
    	
    	// Do not unpublish for RCS, just stop timer
    	if (published) {
	    	// Stop timer
	    	stopTimer();
	    	published = false;
    	}
    	
        if (logger.isActivated()) {
        	logger.info("Publish manager is terminated");
        }
    }

    /**
     * Publish refresh processing
     */
    public void periodicProcessing() {
        // Make a publish
    	if (logger.isActivated()) {
    		logger.info("Execute re-publish");
    	}

    	try {
	        // Create a new dialog path for each publish
	        dialogPath = createDialogPath();
	        
	        // Create PUBLISH request with no SDP and expire period 
	        SipRequest publish = SipMessageFactory.createPublish(createDialogPath(),
	        		expirePeriod,
	        		entityTag,
	        		null);
	        
	        // Send PUBLISH request 
	        sendPublish(publish);
        } catch (Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Publish has failed", e);
        	}
        	handleError(new PresenceError(PresenceError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }
    
    /**
     * Publish presence status
     * 
     * @param info Presence info
     * @return Boolean
     */
    public synchronized boolean publish(String info) {
        try {
	        // Create a new dialog path for each publish
	        dialogPath = createDialogPath();

			// Set the local SDP part in the dialog path
	    	dialogPath.setLocalContent(info);
	    	
	    	// Create PUBLISH request 
            SipRequest publish = SipMessageFactory.createPublish(dialogPath,
            		expirePeriod,
            		entityTag,
            		info);
            
            // Send PUBLISH request
	        sendPublish(publish);
        } catch (Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Publish has failed", e);
        	}
        	handleError(new PresenceError(PresenceError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
        return published;
    }

    /**
     * Unpublish
     */
    public synchronized void unPublish() {
    	if (!published) {
			// Already unpublished
			return;
    	}    	

    	try {
	        // Stop periodic publish
	        stopTimer();

	        // Create a new dialog path for each publish
	        dialogPath = createDialogPath();
	        
	        // Create PUBLISH request with no SDP and expire period 
	        SipRequest publish = SipMessageFactory.createPublish(dialogPath,
	        		0,
	        		entityTag,
	        		null);
            
	        // Send PUBLISH request
	        sendPublish(publish);
	    	 
	        // Force publish flag to false
	        published = false;
        } catch (Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Publish has failed", e);
        	}
        	handleError(new PresenceError(PresenceError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }
    
    /**
	 * Send PUBLISH message
	 * 
	 * @param publish SIP PUBLISH
	 * @throws Exception
	 */
	private void sendPublish(SipRequest publish) throws Exception {
        if (logger.isActivated()) {
        	logger.info("Send PUBLISH, expire=" + publish.getExpires());
        }

        if (published) {
	        // Set the Authorization header
            authenticationAgent.setProxyAuthorizationHeader(publish);
        }
        
        // Send PUBLISH request
        SipTransactionContext ctx = imsModule.getSipManager().sendSipMessageAndWait(publish);

        // Analyze the received response 
        if (ctx.isSipResponse()) {
        	// A response has been received
            if (ctx.getStatusCode() == 200) {
            	// 200 OK
        		if (publish.getExpires() != 0) {
        			handle200OK(ctx);
        		} else {
        			handle200OkUnpublish(ctx);
        		}
            } else
            if (ctx.getStatusCode() == 407) {
            	// 407 Proxy Authentication Required
            	handle407Authentication(ctx);
            } else
            if (ctx.getStatusCode() == 412) {
            	// 412 Error
            	handle412ConditionalRequestFailed(ctx);
            } else
            if (ctx.getStatusCode() == 423) {
            	// 423 Interval Too Brief
            	handle423IntervalTooBrief(ctx);
            } else {
            	// Other error response
    			handleError(new PresenceError(PresenceError.PUBLISH_FAILED,
    					ctx.getStatusCode() + " " + ctx.getReasonPhrase()));    					
            }
        } else {
    		if (logger.isActivated()) {
        		logger.debug("No response received for PUBLISH");
        	}

    		// No response received: timeout
        	handleError(new PresenceError(PresenceError.PUBLISH_FAILED));
        }
	}    

	/**
	 * Handle 200 0K response 
	 * 
	 * @param ctx SIP transaction context
	 */
	private void handle200OK(SipTransactionContext ctx) {
        // 200 OK response received
    	if (logger.isActivated()) {
    		logger.info("200 OK response received");
    	}
    	published = true;

    	SipResponse resp = ctx.getSipResponse();

    	// Set the Proxy-Authorization header
    	authenticationAgent.readProxyAuthenticateHeader(resp);

        // Retrieve the expire value in the response
        retrieveExpirePeriod(resp);
        
    	// Retrieve the entity tag in the response
    	saveEntityTag((SIPETagHeader)resp.getHeader(SIPETagHeader.NAME));
    	
    	// Start the periodic publish
        startTimer(expirePeriod, 0.5);
	}	
	
	/**
	 * Handle 200 0K response of UNPUBLISH
	 * 
	 * @param ctx SIP transaction context
	 */
	private void handle200OkUnpublish(SipTransactionContext ctx) {
        // 200 OK response received
        if (logger.isActivated()) {
            logger.info("200 OK response received");
        }

    	SipResponse resp = ctx.getSipResponse();
    	
    	// Retrieve the entity tag in the response
    	saveEntityTag((SIPETagHeader)resp.getHeader(SIPETagHeader.NAME));
	}
	
	/**
	 * Handle 407 response 
	 * 
	 * @param ctx SIP transaction context
	 * @throws Exception
	 */
	private void handle407Authentication(SipTransactionContext ctx) throws Exception {
        // 407 response received
    	if (logger.isActivated()) {
    		logger.info("407 response received");
    	}

    	SipResponse resp = ctx.getSipResponse();

    	// Set the Proxy-Authorization header
    	authenticationAgent.readProxyAuthenticateHeader(resp);

        // Increment the Cseq number of the dialog path
        dialogPath.incrementCseq();

        // Create a second PUBLISH request with the right token
        if (logger.isActivated()) {
        	logger.info("Send second PUBLISH");
        }
    	SipRequest publish = SipMessageFactory.createPublish(dialogPath,
    			ctx.getTransaction().getRequest().getExpires().getExpires(),
        		entityTag,
        		dialogPath.getLocalContent());
    	
        // Set the Authorization header
        authenticationAgent.setProxyAuthorizationHeader(publish);
    	
        // Send PUBLISH request
    	sendPublish(publish);
	}	

	/**
	 * Handle 412 response 
	 * 
	 * @param ctx SIP transaction context
	 */
	private void handle412ConditionalRequestFailed(SipTransactionContext ctx) throws Exception {
		// 412 response received
    	if (logger.isActivated()) {
    		logger.info("412 conditional response received");
    	}

        // Increment the Cseq number of the dialog path
        dialogPath.incrementCseq();

        // Reset Sip-Etag
        saveEntityTag(null);

        // Create a PUBLISH request without ETag 
        SipRequest publish = SipMessageFactory.createPublish(dialogPath,
        		expirePeriod,
        		entityTag,
        		dialogPath.getLocalContent());

        // Send PUBLISH request
        sendPublish(publish);        
	}	
	
	/**
	 * Handle 423 response 
	 * 
	 * @param ctx SIP transaction context
	 * @throws Exception
	 */
	private void handle423IntervalTooBrief(SipTransactionContext ctx) throws Exception {
		// 423 response received
    	if (logger.isActivated()) {
    		logger.info("423 interval too brief response received");
    	}

    	SipResponse resp = ctx.getSipResponse();

    	// Increment the Cseq number of the dialog path
        dialogPath.incrementCseq();

        // Extract the Min-Expire value
        int minExpire = SipUtils.getMinExpiresPeriod(resp);
        if (minExpire == -1) {
            if (logger.isActivated()) {
            	logger.error("Can't read the Min-Expires value");
            }
        	handleError(new PresenceError(PresenceError.PUBLISH_FAILED, "No Min-Expires value found"));
        	return;
        }
        
        // Save the min expire value in the terminal registry
        RegistryFactory.getFactory().writeInteger(REGISTRY_MIN_EXPIRE_PERIOD, minExpire);

        // Set the default expire value
    	expirePeriod = minExpire;
    	
        // Create a new PUBLISH request with the right expire period
        SipRequest publish = SipMessageFactory.createPublish(dialogPath,
        		expirePeriod,
        		entityTag,
        		dialogPath.getLocalContent());

        // Send a PUBLISH request
        sendPublish(publish);        
	}	
	
	/**
	 * Handle error response 
	 * 
	 * @param error Error
	 */
	private void handleError(PresenceError error) {
        // Error
    	if (logger.isActivated()) {
    		logger.info("Publish has failed: " + error.getErrorCode() + ", reason=" + error.getMessage());
    	}
        published = false;
        
        // Publish has failed, stop the periodic publish
		stopTimer();
        
        // Error
        if (logger.isActivated()) {
        	logger.info("Publish has failed");
        }
	}

    /**
     * Retrieve the expire period
     * 
     * @param response SIP response
     */
    private void retrieveExpirePeriod(SipResponse response) {
        // Extract expire value from Expires header
        ExpiresHeader expiresHeader = (ExpiresHeader)response.getHeader(ExpiresHeader.NAME);
    	if (expiresHeader != null) {
    		int expires = expiresHeader.getExpires();
		    if (expires != -1) {
	    		expirePeriod = expires;
	    	}
        }
	}
	
	/**
	 * Save the SIP entity tag
	 * 
	 * @param etagHeader Header tag
	 */
	private void saveEntityTag(SIPETagHeader etagHeader) {
		if (etagHeader == null) {
			entityTag = null;
		} else {
			entityTag = etagHeader.getETag();
		}
		if (entityTag != null) {
			RegistryFactory.getFactory().writeString(REGISTRY_SIP_ETAG, entityTag);
			long etagExpiration = System.currentTimeMillis() + (expirePeriod * 1000);
	    	RegistryFactory.getFactory().writeLong(REGISTRY_SIP_ETAG_EXPIRATION, etagExpiration);
	        if (logger.isActivated()) {
	        	logger.debug("New entity tag: " + entityTag + ", expire at=" + etagExpiration);
	        }
		} else {
			RegistryFactory.getFactory().removeParameter(REGISTRY_SIP_ETAG);
	    	RegistryFactory.getFactory().removeParameter(REGISTRY_SIP_ETAG_EXPIRATION);
	        if (logger.isActivated()) {
	        	logger.debug("Entity tag has been reset");
	        }
		}
	}
	
	/**
	 * Read the SIP entity tag
	 */
	private void readEntityTag() {
		entityTag = RegistryFactory.getFactory().readString(REGISTRY_SIP_ETAG, null);
    	long etagExpiration = RegistryFactory.getFactory().readLong(REGISTRY_SIP_ETAG_EXPIRATION, -1);
        if (logger.isActivated()) {
        	logger.debug("New entity tag: " + entityTag + ", expire at=" + etagExpiration);
        }
	}
	
	/**
	 * Create a new dialog path
	 * 
	 * @return Dialog path
	 */
	private SipDialogPath createDialogPath() {
        // Set Call-Id
    	String callId = imsModule.getSipManager().getSipStack().generateCallId();

    	// Set target
    	String target = ImsModule.IMS_USER_PROFILE.getPublicUri();

        // Set local party
    	String localParty = ImsModule.IMS_USER_PROFILE.getPublicUri();

        // Set remote party
    	String remoteParty = ImsModule.IMS_USER_PROFILE.getPublicUri();

    	// Set the route path
    	Vector<String> route = imsModule.getSipManager().getSipStack().getServiceRoutePath();

    	// Create a dialog path
    	SipDialogPath dialog = new SipDialogPath(
        		imsModule.getSipManager().getSipStack(),
        		callId,
        		1,
        		target,
        		localParty,
        		remoteParty,
        		route);
    	return dialog;
	}
}
