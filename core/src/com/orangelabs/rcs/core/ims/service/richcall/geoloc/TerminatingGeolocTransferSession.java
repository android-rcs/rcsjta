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

package com.orangelabs.rcs.core.ims.service.richcall.geoloc;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpManager;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.SessionTimerManager;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Terminating geoloc sharing session (transfer)
 * 
 * @author jexa7410
 */
public class TerminatingGeolocTransferSession extends GeolocTransferSession implements MsrpEventListener {
	/**
	 * MSRP manager
	 */
	private MsrpManager msrpMgr;
	
	/**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(TerminatingGeolocTransferSession.class.getSimpleName());

    /**
     * Constructor
     * 
	 * @param parent IMS service
	 * @param invite Initial INVITE request
	 * @param contact Contact Id
	 */
	public TerminatingGeolocTransferSession(ImsService parent, SipRequest invite, ContactId contact) {
		super(parent, ContentManager.createMmContentFromSdp(invite), contact);

		// Create dialog path
		createTerminatingDialogPath(invite);
	}
	
	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a new sharing session as terminating");
	    	}

	    	send180Ringing(getDialogPath().getInvite(), getDialogPath().getLocalTag());

	    	// Check if the MIME type is supported
	    	if (getContent() == null) {
	    		if (logger.isActivated()){
    				logger.debug("MIME type is not supported");
    			}

    			// Send a 415 Unsupported media type response
				send415Error(getDialogPath().getInvite());

				// Unsupported media type
				handleError(new ContentSharingError(ContentSharingError.UNSUPPORTED_MEDIA_TYPE));
        		return;
        	}

			Collection<ImsSessionListener> listeners = getListeners();
			for (ImsSessionListener listener : listeners) {
				listener.handleSessionInvited();
			}

			int answer = waitInvitationAnswer();
			switch (answer) {
				case ImsServiceSession.INVITATION_REJECTED:
					if (logger.isActivated()) {
						logger.debug("Session has been rejected by user");
					}

					getImsService().removeSession(this);

					for (ImsSessionListener listener : listeners) {
						listener.handleSessionRejectedByUser();
					}
					return;

				case ImsServiceSession.INVITATION_NOT_ANSWERED:
					if (logger.isActivated()) {
						logger.debug("Session has been rejected on timeout");
					}

					// Ringing period timeout
					send486Busy(getDialogPath().getInvite(), getDialogPath().getLocalTag());

					getImsService().removeSession(this);

					for (ImsSessionListener listener : listeners) {
						listener.handleSessionRejectedByTimeout();
					}
					return;

				case ImsServiceSession.INVITATION_CANCELED:
					if (logger.isActivated()) {
						logger.debug("Session has been rejected by remote");
					}

					getImsService().removeSession(this);

					for (ImsSessionListener listener : listeners) {
						listener.handleSessionRejectedByRemote();
					}
					return;
				case ImsServiceSession.INVITATION_ACCEPTED:
					setSessionAccepted();

					for (ImsSessionListener listener : listeners) {
						listener.handleSessionAccepted();
					}
					break;

				default:
					if (logger.isActivated()) {
						logger.debug("Unknown invitation answer in run; answer="
									.concat(String.valueOf(answer)));
					}
					return;
			}

	    	// Parse the remote SDP part
			String remoteSdp = getDialogPath().getInvite().getSdpContent();
        	SdpParser parser = new SdpParser(remoteSdp.getBytes());
    		Vector<MediaDescription> media = parser.getMediaDescriptions();
			MediaDescription mediaDesc = media.elementAt(0);
			MediaAttribute attr1 = mediaDesc.getMediaAttribute("file-selector");
            String fileSelector = attr1.getName() + ":" + attr1.getValue();
			MediaAttribute attr2 = mediaDesc.getMediaAttribute("file-transfer-id");
            String fileTransferId = attr2.getName() + ":" + attr2.getValue();
			MediaAttribute attr3 = mediaDesc.getMediaAttribute("path");
            String remotePath = attr3.getValue();
            String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
    		int remotePort = mediaDesc.port;
			
            // Extract the "setup" parameter
            String remoteSetup = "passive";
			MediaAttribute attr4 = mediaDesc.getMediaAttribute("setup");
			if (attr4 != null) {
				remoteSetup = attr4.getValue();
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
		    	localMsrpPort = 9; // See RFC4145, Page 4
	    	} else {
				localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort();
	    	}
	    	
            // Create the MSRP manager
			String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface().getNetworkAccess().getIpAddress();
			msrpMgr = new MsrpManager(localIpAddress, localMsrpPort);
    		
			// Build SDP part
	    	String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
	    	String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
	    	String sdp =
	    		"v=0" + SipUtils.CRLF +
	            "o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
	            "s=-" + SipUtils.CRLF +
				"c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
	            "t=0 0" + SipUtils.CRLF +			
	            "m=message " + localMsrpPort + " TCP/MSRP *" + SipUtils.CRLF +
	            "a=" + fileSelector + SipUtils.CRLF +
	    		"a=" + fileTransferId + SipUtils.CRLF +
	            "a=accept-types:" + getContent().getEncoding() + SipUtils.CRLF +
	            "a=setup:" + localSetup + SipUtils.CRLF +
	            "a=path:" + msrpMgr.getLocalMsrpPath() + SipUtils.CRLF +
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
            	msrpMgr.createMsrpServerSession(remotePath, this);
            	
    			// Open the connection
    			Thread thread = new Thread(){
    				public void run(){
    					try {
    						// Open the MSRP session
    						msrpMgr.openMsrpSession(GeolocTransferSession.DEFAULT_SO_TIMEOUT);

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
            		RichcallService.FEATURE_TAGS_GEOLOC_SHARE, sdp);

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

    	        // Create the MSRP client session
                if (localSetup.equals("active")) {
                	String fingerprint = SdpUtils.extractFingerprint(parser, mediaDesc);
                	// Active mode: client should connect
                	msrpMgr.createMsrpClientSession(remoteHost, remotePort, remotePath, this, fingerprint);

					// Open the MSRP session
					msrpMgr.openMsrpSession(GeolocTransferSession.DEFAULT_SO_TIMEOUT);
					
	    	        // Send an empty packet
	            	sendEmptyDataChunk();
                }

                // The session is established
                getDialogPath().sessionEstablished();

                for (ImsSessionListener listener : listeners) {
                    listener.handleSessionStarted();
            }

            	// Start session timer
            	if (getSessionTimerManager().isSessionTimerActivated(resp)) {        	
            		getSessionTimerManager().start(SessionTimerManager.UAS_ROLE, getDialogPath().getSessionExpireTime());
            	}
            } else {
        		if (logger.isActivated()) {
            		logger.debug("No ACK received for INVITE");
            	}

        		// No response received: timeout
            	handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED));
            }
		} catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Session initiation has failed", e);
        	}

        	// Unexpected error
			handleError(new ContentSharingError(ContentSharingError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
		}		

		if (logger.isActivated()) {
    		logger.debug("End of thread");
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
     * @param data Last received data chunk
	 * @param mimeType Data mime-type 
	 */
	public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
    	if (logger.isActivated()) {
    		logger.info("Data received");
    	}
    	
	   	try {
            // Parse received geoloc info
			String geolocDoc = new String(data);
            GeolocPush geoloc = ChatUtils.parseGeolocDocument(geolocDoc);
        	
        	// Set geoloc
        	setGeoloc(geoloc);
        	
        	// Geoloc has been transfered
        	geolocTransfered();

        	// Notify listeners
	    	for(int j=0; j < getListeners().size(); j++) {
	    		((GeolocTransferSessionListener)getListeners().get(j)).handleContentTransfered(geoloc);
	    	}
	   	} catch(Exception e) {
	   		// Notify listeners
	    	for(int j=0; j < getListeners().size(); j++) {
	    		((GeolocTransferSessionListener)getListeners().get(j)).handleSharingError(new ContentSharingError(ContentSharingError.MEDIA_TRANSFER_FAILED));
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
        // Not used
    }

    /**
     * Data transfer in progress
     *
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     * @param data received data chunk
     */
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        // Not used for geolocation sharing
        return true;
	}

	/**
	 * Data transfer has been aborted
	 */
	public void msrpTransferAborted() {
    	if (logger.isActivated()) {
    		logger.info("Data transfer aborted");
    	}
	}

    /**
     * Data transfer error
     *
     * @param msgId Message ID
     * @param error Error code
     * @param typeMsrpChunk Type of MSRP chunk
     */
    public void msrpTransferError(String msgId, String error, TypeMsrpChunk typeMsrpChunk) {
        if (isSessionInterrupted()) {
            return;
        }

		if (logger.isActivated()) {
            logger.info("Data transfer error " + error);
    	}

        // Close the media session
        closeMediaSession();
        
		// Terminate session
		terminateSession(ImsServiceSession.TERMINATION_BY_SYSTEM);

        try {
			ContactId remote = ContactUtils.createContactId(getDialogPath().getRemoteParty());
			// Request capabilities to the remote
	        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(remote);
		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.warn("Cannot parse contact "+getDialogPath().getRemoteParty());
			}
		}
        
    	// Remove the current session
    	getImsService().removeSession(this);

    	// Notify listeners
    	for(int j=0; j < getListeners().size(); j++) {
    		((GeolocTransferSessionListener)getListeners().get(j)).handleSharingError(new ContentSharingError(ContentSharingError.MEDIA_TRANSFER_FAILED, error));
        }
	}

    /**
     * Prepare media session
     * 
     * @throws Exception 
     */
    public void prepareMediaSession() throws Exception {
        // Nothing to do in terminating side
    }

    /**
     * Start media session
     * 
     * @throws Exception 
     */
    public void startMediaSession() throws Exception {
        // Nothing to do in terminating side
    }

    /**
     * Close media session
     */
    public void closeMediaSession() {
        // Close the MSRP session
        if (msrpMgr != null) {
            msrpMgr.closeSession();
        }
        if (logger.isActivated()) {
            logger.debug("MSRP session has been closed");
        }
    }

	@Override
	public boolean isInitiatedByRemote() {
		return true;
	}
}

