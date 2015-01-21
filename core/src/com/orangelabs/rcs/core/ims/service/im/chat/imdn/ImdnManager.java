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
package com.orangelabs.rcs.core.ims.service.im.chat.imdn;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.SessionAuthenticationAgent;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.FifoBuffer;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * IMDN manager (see RFC5438)
 * 
 * @author jexa7410
 */
public class ImdnManager extends Thread {
    /**
     * IMS service
     */
    private ImsService imsService;	
	
	/**
	 * Buffer
	 */
	private FifoBuffer buffer = new FifoBuffer();
    
	/**
	 * Activation flag
	 */
	private boolean activated;

    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(ImdnManager.class.getSimpleName());
    
    /**
     * Constructor
     * 
     * @param imsService IMS service
     */    
    public ImdnManager(ImsService imsService) {
    	this.imsService = imsService;
    	this.activated = RcsSettings.getInstance().isImReportsActivated();
    }    
    
    /**
     * Terminate manager
     */
    public void terminate() {
    	if (logger.isActivated()) {
    		logger.info("Terminate the IMDN manager");
    	}
        buffer.close();
    }
    
    /**
     * Is IMDN activated
     * 
     * @return Boolean
     */
    public boolean isImdnActivated() {
    	return activated;
    }
    
    /**
     * Background processing
     */
    public void run() {
		if (logger.isActivated()) {
			logger.info("Start background processing");
		}
		DeliveryStatus delivery = null; 
		while((delivery = (DeliveryStatus)buffer.getObject()) != null) {
			try {
				// Send SIP MESSAGE
				sendSipMessageDeliveryStatus(delivery, null); // TODO: add sip.instance

				// Update rich messaging history when sending DISPLAYED report
				// Since the requested display report was now successfully send we mark this message as fully received
				if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(delivery.getStatus()))
					MessagingLog.getInstance().markIncomingChatMessageAsReceived(
							delivery.getMsgId());
			} catch(Exception e) {
				if (logger.isActivated()) {
					logger.error("Unexpected exception", e);
				}
			}
		}
		if (logger.isActivated()) {
			logger.info("End of background processing");
		}
    }
       
	/**
	 * Send a message delivery status
	 * 
	 * @param contact Contact identifier
	 * @param msgId Message ID
	 * @param status Delivery status
	 */
	public void sendMessageDeliveryStatus(ContactId contact, String msgId, String status) {
		// Add request in the buffer for background processing
		DeliveryStatus delivery = new DeliveryStatus(contact, msgId, status);
		buffer.addObject(delivery);
	}

    /**
     * Send a message delivery status immediately
     * 
     * @param contact Contact identifier
     * @param msgId Message ID
     * @param status Delivery status
     */
    public void sendMessageDeliveryStatusImmediately(ContactId contact, String msgId, String status, final String remoteInstanceId) {
        // Execute request in background
        final DeliveryStatus delivery = new DeliveryStatus(contact, msgId, status);
        new Thread(){
            public void run() {
                // Send SIP MESSAGE
                sendSipMessageDeliveryStatus(delivery, remoteInstanceId);
            }
        }.start();
    }

	/**
	 * Send message delivery status via SIP MESSAGE
	 *
	 * @param deliveryStatus Delivery status
	 * @param remoteInstanceId Remote SIP instance
	 */
	private void sendSipMessageDeliveryStatus(DeliveryStatus deliveryStatus, String remoteInstanceId) {
		try {
            if (!RcsSettings.getInstance().isRespondToDisplayReports() && ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(deliveryStatus.getStatus())) {
                return;
            }

			if (logger.isActivated()) {
       			logger.debug("Send delivery status " + deliveryStatus.getStatus() + " for message " + deliveryStatus.getMsgId());
       		}

	   		// Create CPIM/IDMN document
			String from = ChatUtils.ANOMYNOUS_URI;
			String to = ChatUtils.ANOMYNOUS_URI;
			String imdn = ChatUtils.buildDeliveryReport(deliveryStatus.getMsgId(), deliveryStatus.getStatus());
			String cpim = ChatUtils.buildCpimDeliveryReport(from, to, imdn);
			
		    // Create authentication agent 
       		SessionAuthenticationAgent authenticationAgent = new SessionAuthenticationAgent(imsService.getImsModule());
       		
       		String toUri = PhoneUtils.formatContactIdToUri(deliveryStatus.getContact());
       		// Create a dialog path
        	SipDialogPath dialogPath = new SipDialogPath(
        			imsService.getImsModule().getSipManager().getSipStack(),
        			imsService.getImsModule().getSipManager().getSipStack().generateCallId(),
    				1,
    				toUri,
    				ImsModule.IMS_USER_PROFILE.getPublicUri(),
    				toUri,
    				imsService.getImsModule().getSipManager().getSipStack().getServiceRoutePath());        	
            dialogPath.setRemoteSipInstance(remoteInstanceId);

	        // Create MESSAGE request
        	if (logger.isActivated()) {
        		logger.info("Send first MESSAGE.");
        	}
	        SipRequest msg = SipMessageFactory.createMessage(dialogPath,
	        		FeatureTags.FEATURE_OMA_IM, CpimMessage.MIME_TYPE, cpim.getBytes(UTF8));
	        
	        // Send MESSAGE request
	        SipTransactionContext ctx = imsService.getImsModule().getSipManager().sendSipMessageAndWait(msg);

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
                	logger.info("Send second MESSAGE.");
                }
    	        msg = SipMessageFactory.createMessage(dialogPath,
    	        		FeatureTags.FEATURE_OMA_IM, CpimMessage.MIME_TYPE, cpim.getBytes(UTF8));
    	        
    	        // Set the Authorization header
    	        authenticationAgent.setProxyAuthorizationHeader(msg);
                
                // Send MESSAGE request
    	        ctx = imsService.getImsModule().getSipManager().sendSipMessageAndWait(msg);

                // Analyze received message
                if ((ctx.getStatusCode() == 200) || (ctx.getStatusCode() == 202)) {
                    // 200 OK response
                	if (logger.isActivated()) {
                		logger.info("20x OK response received");
                	}
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
	        } else {
	            // Error responses
            	if (logger.isActivated()) {
            		logger.info("Delivery report has failed: " + ctx.getStatusCode()
	                    + " response received");
            	}
	        }
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Delivery report has failed", e);
        	}
        }
	}
	
	/**
	 * Delivery status
	 */
	private static class DeliveryStatus {
		private ContactId contact;
		private String msgId;
		private String status;
		
		public DeliveryStatus(ContactId contact, String msgId, String status) {
			this.contact = contact;
			this.msgId = msgId;
			this.status = status;
		}
		
		public ContactId getContact() {
			return contact;
		}

		public String getMsgId() {
			return msgId;
		}

		public String getStatus() {
			return status;
		}
	}	
}
