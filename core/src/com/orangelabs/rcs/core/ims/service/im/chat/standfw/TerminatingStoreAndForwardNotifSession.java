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

package com.orangelabs.rcs.core.ims.service.im.chat.standfw;

import java.io.IOException;
import java.util.Vector;

import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpManager;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.OneOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimParser;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Terminating Store & Forward session for push notifications
 * 
 * @author jexa7410
 */
public class TerminatingStoreAndForwardNotifSession extends OneOneChatSession implements MsrpEventListener {
	/**
	 * MSRP manager
	 */
	private MsrpManager msrpMgr = null;

	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
	 * @param parent IMS service
	 * @param invite Initial INVITE request
	 */
	public TerminatingStoreAndForwardNotifSession(ImsService parent, SipRequest invite) {
		super(parent, ChatUtils.getReferredIdentity(invite));

		// Create the MSRP manager
		int localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort();
		String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface().getNetworkAccess().getIpAddress();
		msrpMgr = new MsrpManager(localIpAddress, localMsrpPort);
		if (parent.getImsModule().isConnectedToWifiAccess()) {
			msrpMgr.setSecured(RcsSettings.getInstance().isSecureMsrpOverWifi());
		}
		
		// Create dialog path
		createTerminatingDialogPath(invite);
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a new store & forward session for notifications");
	    	}
	    	
        	// Parse the remote SDP part
        	SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes());
    		Vector<MediaDescription> media = parser.getMediaDescriptions();
			MediaDescription mediaDesc = media.elementAt(0);
			MediaAttribute attr1 = mediaDesc.getMediaAttribute("path");
            String remotePath = attr1.getValue();
    		String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription.connectionInfo);
    		int remotePort = mediaDesc.port;
			
            // Extract the "setup" parameter
            String remoteSetup = "passive";
			MediaAttribute attr2 = mediaDesc.getMediaAttribute("setup");
			if (attr2 != null) {
				remoteSetup = attr2.getValue();
			}
            if (logger.isActivated()){
				logger.debug("Remote setup attribute is " + remoteSetup);
			}
            
    		// Set setup mode
            String localSetup = createSetupAnswer(remoteSetup);
            if (logger.isActivated()){
				logger.debug("Local setup attribute is " + localSetup);
			}
			
    		// Set local port
	    	int localMsrpPort;
	    	if (localSetup.equals("active")) {
	    		localMsrpPort = getMsrpMgr().getLocalMsrpPort();
	    	} else {
		    	localMsrpPort = 9; // See RFC4145, Page 4
	    	}            

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
	    		"a=accept-types:" + getAcceptTypes() + SipUtils.CRLF +
	            "a=accept-wrapped-types:" + getWrappedTypes() + SipUtils.CRLF +
	            "a=setup:" + localSetup + SipUtils.CRLF +
	            "a=path:" + getMsrpMgr().getLocalMsrpPath() + SipUtils.CRLF +
	    		"a=recvonly" + SipUtils.CRLF;

	    	// Set the local SDP part in the dialog path
	        getDialogPath().setLocalContent(sdp);

	        // Test if the session should be interrupted
			if (isInterrupted()) {
				if (logger.isActivated()) {
					logger.debug("Session has been interrupted: end of processing");
				}
				return;
			}
	        
    		// Create the MSRP server session
            if (localSetup.equals("passive")) {
            	// Passive mode: client wait a connection
            	MsrpSession session = getMsrpMgr().createMsrpServerSession(remotePath, this);
    			session.setFailureReportOption(false);
    			session.setSuccessReportOption(false);
    			
    			// Open the connection
    			Thread thread = new Thread(){
    				public void run(){
    					try {
							// Open the MSRP session
							getMsrpMgr().openMsrpSession();
							
			    	        // Send an empty packet
			            	sendEmptyDataChunk();							
						} catch (IOException e) {
							if (logger.isActivated()) {
				        		logger.error("Can't create the MSRP server session", e);
				        	}
						}		
    				}
    			};
    			thread.start();
            }
            
            // Create a 200 OK response
        	if (logger.isActivated()) {
        		logger.info("Send 200 OK");
        	}
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(getDialogPath(),
            		InstantMessagingService.CHAT_FEATURE_TAGS, sdp);

            // The signalisation is established
            getDialogPath().sigEstablished();

            // Send response
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSipMessageAndWait(resp);

            // Analyze the received response 
            if (ctx.isSipAck()) {
    	        // ACK received
    			if (logger.isActivated()) {
    				logger.info("ACK request received");
    			}

    			// The session is established
    	        getDialogPath().sessionEstablished();
    	                        
    			// Create the MSRP client session
                if (localSetup.equals("active")) {
                	// Active mode: client should connect
                	MsrpSession session = getMsrpMgr().createMsrpClientSession(remoteHost, remotePort, remotePath, this);
        			session.setFailureReportOption(false);
        			session.setSuccessReportOption(false);
        			
					// Open the MSRP session
					getMsrpMgr().openMsrpSession();
					
	    	        // Send an empty packet
	            	sendEmptyDataChunk();
                }
                
    			// Start the activity manager
    			getActivityManager().start();
                
            } else {
        		if (logger.isActivated()) {
            		logger.debug("No ACK received for INVITE");
            	}

        		// No response received: timeout
            	handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED));
            }
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
	 * Returns the MSRP manager
	 * 
	 * @return MSRP manager
	 */
	public MsrpManager getMsrpMgr() {
		return msrpMgr;
	}
	
	/**
	 * Close the MSRP session
	 */
	public void closeMsrpSession() {
    	if (getMsrpMgr() != null) {
    		getMsrpMgr().closeSession();
			if (logger.isActivated()) {
				logger.debug("MSRP session has been closed");
			}
    	}
	}	
	
	/**
	 * Handle error 
	 * 
	 * @param error Error
	 */
	public void handleError(ImsServiceError error) {
        // Error	
    	if (logger.isActivated()) {
    		logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
    	}

		// Close media session
    	closeMediaSession();

    	// Remove the current session
    	getImsService().removeSession(this);
	}

	/**
	 * Data has been transfered
	 * 
	 * @param msgId Message ID
	 */
	public void msrpDataTransfered(String msgId) {
		// Not used in terminating side
	}
	
	/**
	 * Data transfer has been received
	 * 
	 * @param msgId Message ID
	 * @param data Received data
	 * @param mimeType Data mime-type 
	 */
	public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
    	if (logger.isActivated()) {
    		logger.info("Data received (type " + mimeType + ")");
    	}
    	
		// Update the activity manager
    	getActivityManager().updateActivity();
    	
    	if ((data == null) || (data.length == 0)) {
    		// By-pass empty data
        	if (logger.isActivated()) {
        		logger.debug("By-pass received empty data");
        	}
    		return;
    	}

		if (ChatUtils.isMessageCpimType(mimeType)) {
	    	// Receive a CPIM message
			try {
    			CpimParser cpimParser = new CpimParser(data);
				CpimMessage cpimMsg = cpimParser.getCpimMessage();
				if (cpimMsg != null) {
			    	String contentType = cpimMsg.getContentType();
			    	String from = cpimMsg.getHeader(CpimMessage.HEADER_FROM);
			    	if (ChatUtils.isMessageImdnType(contentType)) {
						// Receive an IMDN report
						receiveMessageDeliveryStatus(from, cpimMsg.getMessageContent());
			    	}
				}
	    	} catch(Exception e) {
		   		if (logger.isActivated()) {
		   			logger.error("Can't parse the CPIM message", e);
		   		}
		   	}
		} else {
			// Not supported content
        	if (logger.isActivated()) {
        		logger.debug("Not supported content " + mimeType + " in chat session");
        	}
		}
	}

	/**
	 * Data transfer in progress
	 * 
	 * @param currentSize Current transfered size in bytes
	 * @param totalSize Total size in bytes
	 */
	public void msrpTransferProgress(long currentSize, long totalSize) {
		// Not used by S&F
	}

	/**
	 * Data transfer has been aborted
	 */
	public void msrpTransferAborted() {
		// Not used by S&F
	}	

	/**
     * Data transfer error
     *
     * @param msgId Message ID
     * @param error Error code
     */
    public void msrpTransferError(String msgId, String error) {
		if (logger.isActivated()) {
            logger.info("Data transfer error " + error);
        }
    }
	
	/**
	 * Send an empty data chunk
	 */
	public void sendEmptyDataChunk() {
		try {
			msrpMgr.sendEmptyChunk();
		} catch(Exception e) {
	   		if (logger.isActivated()) {
	   			logger.error("Problem while sending empty data chunk", e);
	   		}
		}
	}
	
	/**
     * Receive a message delivery status (XML document)
     * 
     * @param contact Contact
     * @param xml XML document
     */
    public void receiveMessageDeliveryStatus(String contact, String xml) {
    	try {
	    	// Parse the IMDN document
    		ImdnDocument imdn = ChatUtils.parseDeliveryReport(xml);
			if (imdn != null) {
				// Notify the message delivery outside of the chat session
				getImsService().getImsModule().getCore().getListener().handleMessageDeliveryStatus(contact,
						imdn.getMsgId(), imdn.getStatus());
			}
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Can't parse IMDN document", e);
    		}
    	}
    }
}
