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

package com.orangelabs.rcs.core.ims.service.richcall.image;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax2.sip.header.ContentDispositionHeader;
import javax2.sip.header.ContentLengthHeader;
import javax2.sip.header.ContentTypeHeader;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.Multipart;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpManager;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.Base64;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

import android.net.Uri;

/**
 * Originating content sharing session (transfer)
 * 
 * @author jexa7410
 */
public class OriginatingImageTransferSession extends ImageTransferSession implements MsrpEventListener {
	/**
	 * Boundary tag
	 */
	private final static String BOUNDARY_TAG = "boundary1";
	
	/**
	 * MSRP manager
	 */
	private MsrpManager msrpMgr = null;

	/**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(OriginatingImageTransferSession.class.getName());

	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param content Content to be shared
	 * @param contact Remote contact
	 * @param thumbnail Thumbnail content option
	 */
	public OriginatingImageTransferSession(ImsService parent, MmContent content, String contact, MmContent thumbnail) {
		super(parent, content, contact, thumbnail);

		// Create dialog path
		createOriginatingDialogPath();
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a new sharing session as originating");
	    	}
		    
    		// Set setup mode
	    	String localSetup = createMobileToMobileSetupOffer();
            if (logger.isActivated()){
				logger.debug("Local setup attribute is " + localSetup);
			}

            // Set local port
            int localMsrpPort;
            if ("active".equals(localSetup)) {
                localMsrpPort = 9; // See RFC4145, Page 4
            } else {
                localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort();
            }

			// Create the MSRP manager
			String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface().getNetworkAccess().getIpAddress();
			msrpMgr = new MsrpManager(localIpAddress, localMsrpPort, getImsService());
            if (getImsService().getImsModule().isConnectedToWifiAccess()) {
                msrpMgr.setSecured(RcsSettings.getInstance().isSecureMsrpOverWifi());
            }

			// Build SDP part
	    	String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
	    	String encoding = getContent().getEncoding();
	    	int maxSize = ImageTransferSession.getMaxImageSharingSize();
	    	// Set File-selector attribute
	    	String selector = getFileSelectorAttribute();
	    	String sdp = SdpUtils.buildFileSDP(ipAddress, localMsrpPort,
	                    msrpMgr.getLocalSocketProtocol(), encoding, getFileTransferId(), selector,
	                    "render", localSetup, msrpMgr.getLocalMsrpPath(), SdpUtils.DIRECTION_SENDONLY,
	                    maxSize);

	    	// Set File-location attribute
	    	Uri location = getFileLocationAttribute();
	    	if (location != null) {
	    		sdp += "a=file-location:" + location.toString() + SipUtils.CRLF;
	    	}
	   
	    	if (getThumbnail() != null) {
	    		sdp += "a=file-icon:cid:image@joyn.com" + SipUtils.CRLF;
	    		
	    		// Encode the thumbnail file
	    	    String imageEncoded = Base64.encodeBase64ToString(getThumbnail().getData());

	    		// Build multipart
	    		String multipart = 
	    				Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +
	    				ContentTypeHeader.NAME + ": application/sdp" + SipUtils.CRLF +
	    				ContentLengthHeader.NAME + ": " + sdp.getBytes().length + SipUtils.CRLF +
	    				SipUtils.CRLF +
	    				sdp + SipUtils.CRLF + 
	    				Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +
	    				ContentTypeHeader.NAME + ": " + getContent().getEncoding() + SipUtils.CRLF +
	    				SipUtils.HEADER_CONTENT_TRANSFER_ENCODING + ": base64" + SipUtils.CRLF +
	    				SipUtils.HEADER_CONTENT_ID + ": <image@joyn.com>" + SipUtils.CRLF +
	    				ContentLengthHeader.NAME + ": "+ imageEncoded.length() + SipUtils.CRLF +
	    				ContentDispositionHeader.NAME + ": icon" + SipUtils.CRLF +
	    				SipUtils.CRLF +
	    				imageEncoded + SipUtils.CRLF +
	    				Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + Multipart.BOUNDARY_DELIMITER;

	    		// Set the local SDP part in the dialog path
	    		getDialogPath().setLocalContent(multipart);	    		
	    	} else {
	    		// Set the local SDP part in the dialog path
	    		getDialogPath().setLocalContent(sdp);
	    	}

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
			handleError(new ContentSharingError(ContentSharingError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
		}		

		if (logger.isActivated()) {
    		logger.debug("End of thread");
    	}
	}

    /**
     * Prepare media session
     * 
     * @throws Exception 
     */
    public void prepareMediaSession() throws Exception {
        // Changed by Deutsche Telekom
        // Get the remote SDP part
        byte[] sdp = getDialogPath().getRemoteContent().getBytes();

        // Changed by Deutsche Telekom
        // Create the MSRP session
        MsrpSession session = msrpMgr.createMsrpSession(sdp, this);

        session.setFailureReportOption(true);
        session.setSuccessReportOption(false);
        // Changed by Deutsche Telekom
        // Do not use right now the mapping to do not increase memory and cpu consumption
        session.setMapMsgIdFromTransationId(false);
    }

    /**
     * Start media session
     * 
     * @throws Exception 
     */
    public void startMediaSession() throws Exception {
        // Open the MSRP session
        msrpMgr.openMsrpSession();

        try {
            // Start sending data chunks
            byte[] data = getContent().getData();
            InputStream stream; 
            if (data == null) {
                // Load data from Uri
                stream = AndroidFactory.getApplicationContext().getContentResolver().openInputStream(getContent().getUri());
            } else {
                // Load data from memory
                stream = new ByteArrayInputStream(data);
            }
            
            msrpMgr.sendChunks(stream, getFileTransferId(), getContent().getEncoding(), getContent().getSize(), TypeMsrpChunk.FileSharing);
        } catch(Exception e) {
            // Unexpected error
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }
            handleError(new ImsServiceError(ImsServiceError.UNEXPECTED_EXCEPTION,
                    e.getMessage()));
        }
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

	/**
	 * Data has been transfered
	 * 
	 * @param msgId Message ID
	 */
	public void msrpDataTransfered(String msgId) {
    	if (logger.isActivated()) {
    		logger.info("Data transfered");
    	}
    	
    	// Image has been transfered
    	imageTransfered();
    	
        // Close the media session
        closeMediaSession();
		
		// Terminate session
		terminateSession(ImsServiceSession.TERMINATION_BY_USER);
	   	
    	// Remove the current session
    	getImsService().removeSession(this);
    	
    	// Notify listeners
    	for(int j=0; j < getListeners().size(); j++) {
    		((ImageTransferSessionListener)getListeners().get(j)).handleContentTransfered(getContent().getUri());
        }
	}
	
	/**
	 * Data transfer has been received
	 * 
	 * @param msgId Message ID
	 * @param data Received data
	 * @param mimeType Data mime-type 
	 */
	public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
		// Not used in originating side
	}
    
	/**
	 * Data transfer in progress
	 * 
	 * @param currentSize Current transfered size in bytes
	 * @param totalSize Total size in bytes
	 */
	public void msrpTransferProgress(long currentSize, long totalSize) {
		// Notify listeners
    	for(int j=0; j < getListeners().size(); j++) {
    		((ImageTransferSessionListener)getListeners().get(j)).handleSharingProgress(currentSize, totalSize);
        }
	}	

    /**
     * Data transfer in progress
     *
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     * @param data received data chunk
     */
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        // Not used in originating side
        return false;
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
        if (isSessionInterrupted() || isInterrupted() || getDialogPath().isSessionTerminated()) {
			return;
		}

		if (logger.isActivated()) {
            logger.info("Data transfer error " + error);
    	}

		// Terminate session
		terminateSession(ImsServiceSession.TERMINATION_BY_SYSTEM);

        // Close the media session
        closeMediaSession();
        
        // Request capabilities
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(getDialogPath().getRemoteParty());

		// Remove the current session
    	getImsService().removeSession(this);
    	
    	// Notify listeners
        if (!isSessionInterrupted() && !isSessionTerminatedByRemote()) {
            for(int j=0; j < getListeners().size(); j++) {
                ((ImageTransferSessionListener)getListeners().get(j)).handleSharingError(new ContentSharingError(ContentSharingError.MEDIA_TRANSFER_FAILED, error));
            }
        }
	}
}
