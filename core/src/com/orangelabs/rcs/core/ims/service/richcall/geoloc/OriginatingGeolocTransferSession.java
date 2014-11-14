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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import android.net.Uri;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpManager;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Originating geoloc sharing session (transfer)
 * 
 * @author jexa7410
 */
public class OriginatingGeolocTransferSession extends GeolocTransferSession implements MsrpEventListener {
	/**
	 * MSRP manager
	 */
	private MsrpManager msrpMgr;
	
	/**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(OriginatingGeolocTransferSession.class.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param content Content to be shared
	 * @param contact Remote contact Id
     * @param geoloc Geoloc info
	 */
	public OriginatingGeolocTransferSession(ImsService parent, MmContent content, ContactId contact, GeolocPush geoloc) {
		super(parent, content, contact);

		// Create dialog path
		createOriginatingDialogPath();
		
		// Set geoloc to be sent
		setGeoloc(geoloc);
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

	    	// Set File-selector attribute
	    	String selector = getFileSelectorAttribute();
	    	if (selector != null) {
	    		sdp += "a=file-selector:" + selector + SipUtils.CRLF;
	    	}

	    	// Set File-location attribute
	    	Uri location = getFileLocationAttribute();
	    	if (location != null) {
	    		sdp += "a=file-location:" + location.toString() + SipUtils.CRLF;
	    	}
	   
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
    }

    /**
     * Start media session
     * 
     * @throws Exception 
     */
    public void startMediaSession() throws Exception {
        // Open the MSRP session
        msrpMgr.openMsrpSession();

		new Thread() {
			public void run() {
				try {
					// Start sending data chunks
					byte[] data = getContent().getData();
					InputStream stream = new ByteArrayInputStream(data);
					msrpMgr.sendChunks(stream, getFileTransferId(), getContent().getEncoding(), getContent().getSize(),
							TypeMsrpChunk.GeoLocation);
				} catch (Exception e) {
					// Unexpected error
					if (logger.isActivated()) {
						logger.error("Session initiation has failed", e);
					}
					handleError(new ImsServiceError(ImsServiceError.UNEXPECTED_EXCEPTION, e.getMessage()));
				}
			}
		}.start();
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
    	
    	// Geoloc has been transfered
    	geolocTransfered();
    	
        // Close the media session
        closeMediaSession();
		
		// Terminate session
		terminateSession(ImsServiceSession.TERMINATION_BY_USER);
	   	
    	// Remove the current session
    	getImsService().removeSession(this);

    	// Notify listeners
    	for (ImsSessionListener listener : getListeners()) {
    		((GeolocTransferSessionListener)listener).handleContentTransfered(getGeoloc());
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
		// Not used for geolocation sharing
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
		for (ImsSessionListener listener : getListeners()) {
			((GeolocTransferSessionListener) listener).handleSharingError(new ContentSharingError(
					ContentSharingError.MEDIA_TRANSFER_FAILED, error));
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
			((GeolocTransferSessionListener)listener).handle180Ringing();
		}
	}
}
