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

package com.orangelabs.rcs.core.ims.service.richcall.image;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Vector;

import javax2.sip.header.ContentDispositionHeader;
import javax2.sip.header.ContentLengthHeader;
import javax2.sip.header.ContentTypeHeader;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.Multipart;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpManager;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.utils.Base64;
import com.orangelabs.rcs.utils.logger.Logger;

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
    private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param content Content to be shared
	 * @param contact Remote contact
	 * @param thumbnail Thumbnail option
	 */
	public OriginatingImageTransferSession(ImsService parent, MmContent content, String contact, byte[] thumbnail) {
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
	    	int localMsrpPort = 9; // See RFC4145, Page 4	    	
	    	
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
	            "a=path:" + msrpMgr.getLocalMsrpPath() + SipUtils.CRLF +
	            "a=setup:" + localSetup + SipUtils.CRLF +
	    		"a=accept-types:" + getContent().getEncoding() + SipUtils.CRLF +
	    		"a=file-transfer-id:" + getFileTransferId() + SipUtils.CRLF +
	    		"a=file-disposition:render" + SipUtils.CRLF +
	    		"a=sendonly" + SipUtils.CRLF;
	    	int maxSize = ImageTransferSession.getMaxImageSharingSize();
	    	if (maxSize > 0) {
	    		sdp += "a=max-size:" + maxSize + SipUtils.CRLF;
	    	}

	    	// Set File-selector attribute
	    	String selector = getFileSelectorAttribute();
	    	if (selector != null) {
	    		sdp += "a=file-selector:" + selector + SipUtils.CRLF;
	    	}

	    	// Set File-location attribute
	    	String location = getFileLocationAttribute();
	    	if (location != null) {
	    		sdp += "a=file-location:" + location + SipUtils.CRLF;
	    	}
	   
	    	if (getThumbnail() != null) {
	    		sdp += "a=file-icon:cid:image@joyn.com" + SipUtils.CRLF;
	    		
	    		// Create the thumbnail
	    		byte[] bytes = ChatUtils.createFileThumbnail(getContent().getUrl());
	    		
	    		// Encode the thumbnail file
	    	    String imageEncoded = Base64.encodeBase64ToString(bytes);

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
        // Parse the remote SDP part
        SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes());
        Vector<MediaDescription> media = parser.getMediaDescriptions();
        MediaDescription mediaDesc = media.elementAt(0);
        MediaAttribute attr = mediaDesc.getMediaAttribute("path");
        String remoteMsrpPath = attr.getValue();
        String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription.connectionInfo);
        int remotePort = mediaDesc.port;

        // Create the MSRP session
        MsrpSession session = msrpMgr.createMsrpClientSession(remoteHost, remotePort, remoteMsrpPath, this);
        session.setFailureReportOption(true);
        session.setSuccessReportOption(false);
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
                // Load data from URL
                stream = FileFactory.getFactory().openFileInputStream(getContent().getUrl());
            } else {
                // Load data from memory
                stream = new ByteArrayInputStream(data);
            }
            
            msrpMgr.sendChunks(stream, getFileTransferId(), getContent().getEncoding(), getContent().getSize());
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
    		((ImageTransferSessionListener)getListeners().get(j)).handleContentTransfered(getContent().getUrl());
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
     */
    public void msrpTransferError(String msgId, String error) {
    	if (isInterrupted()) {
			return;
		}

		if (logger.isActivated()) {
            logger.info("Data transfer error " + error);
    	}

        // Close the media session
        closeMediaSession();

		// Terminate session
		terminateSession(ImsServiceSession.TERMINATION_BY_SYSTEM);

		// Remove the current session
    	getImsService().removeSession(this);
    	
    	// Notify listeners
    	for(int j=0; j < getListeners().size(); j++) {
    		((ImageTransferSessionListener)getListeners().get(j)).handleSharingError(new ContentSharingError(ContentSharingError.MEDIA_TRANSFER_FAILED, error));
        }
	}
}
