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
package com.orangelabs.rcs.core.ims.service.im.filetransfer.msrp;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;

import javax2.sip.header.ContentDispositionHeader;
import javax2.sip.header.ContentLengthHeader;
import javax2.sip.header.ContentTypeHeader;

import android.net.Uri;

import com.gsma.services.rcs.contacts.ContactId;
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
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.ContributionIdGenerator;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.ImsFileSharingSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.Base64;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Originating file transfer session
 * 
 * @author jexa7410
 */
public class OriginatingMsrpFileSharingSession extends ImsFileSharingSession implements MsrpEventListener {
	/**
	 * Boundary tag
	 */
	private final static String BOUNDARY_TAG = "boundary1";
	
	/**
	 * MSRP manager
	 */
	private MsrpManager msrpMgr;
	
	/**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(OriginatingMsrpFileSharingSession.class.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            IMS service
	 * @param content
	 *            Content to be shared
	 * @param contact
	 *            Remote contact identifier
	 * @param fileIcon
	 *            true if the stack must try to attach file icon
	 */
	public OriginatingMsrpFileSharingSession(ImsService parent, MmContent content, ContactId contact, boolean fileIcon) {
		super(parent, content, contact, null, IdGenerator.generateMessageID());
		
		if (logger.isActivated()) {
			logger.debug("OriginatingFileSharingSession contact=" + contact + " filename="+content.getName()+" fileIcon="+fileIcon);
		}
		// Create dialog path
		createOriginatingDialogPath();
		
		// Set contribution ID
		String id = ContributionIdGenerator.getContributionId(getDialogPath().getCallId());
		setContributionID(id);
		
		if (fileIcon) {
			try {
				// Create the file icon
				setFileicon(FileTransferUtils.createFileicon(content.getUri(), getSessionID()));
			} catch (SecurityException e) {
				if (logger.isActivated()) {
					logger.error(
							"File icon creation has failed due to that the file is not accessible!",
							e);
				}
				/*TODO: Take appropriate action in CR037.*/
			}
		}
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a file transfer session as originating");
	    	}
	    	
    		// Set setup mode
	    	String localSetup = createSetupOffer();
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
			msrpMgr = new MsrpManager(localIpAddress, localMsrpPort,  getImsService());
            if (getImsService().getImsModule().isConnectedToWifiAccess()) {
                msrpMgr.setSecured(RcsSettings.getInstance().isSecureMsrpOverWifi());
            }

			// Build SDP part
	    	String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
	    	String encoding = getContent().getEncoding();
	    	int maxSize = FileSharingSession.getMaxFileSharingSize();
	    	// Set File-selector attribute
	    	String selector = getFileSelectorAttribute();
	    	String sdp = SdpUtils.buildFileSDP(ipAddress, localMsrpPort,
                    msrpMgr.getLocalSocketProtocol(), encoding, getFileTransferIdAttribute(), selector,
                    "attachment", localSetup, msrpMgr.getLocalMsrpPath(),
                    SdpUtils.DIRECTION_SENDONLY, maxSize);

	    	// Set File-location attribute
	    	Uri location = getFileLocationAttribute();
	    	if (location != null) {
	    		sdp += "a=file-location:" + location.toString() + SipUtils.CRLF;
	    	}

	    	if (getFileicon() != null) {
	    		sdp += "a=file-icon:cid:image@joyn.com" + SipUtils.CRLF;

	    		// Encode the file icon file
	    	    String imageEncoded = Base64.encodeBase64ToString(getFileicon().getData());

	    		// Build multipart
	    		String multipart = 
	    				Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +
	    				ContentTypeHeader.NAME + ": application/sdp" + SipUtils.CRLF +
	    				ContentLengthHeader.NAME + ": " + sdp.getBytes().length + SipUtils.CRLF +
	    				SipUtils.CRLF +
	    				sdp + SipUtils.CRLF + 
	    				Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +
	    				ContentTypeHeader.NAME + ": " + getFileicon().getEncoding() + SipUtils.CRLF +
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
			handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION,
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

        Thread thread = new Thread() {
            public void run() {
                try {
                    // Start sending data chunks
                    byte[] data = getContent().getData();
                    InputStream stream; 
                    if (data == null) {
                        // Load data from URL
                        stream = AndroidFactory.getApplicationContext().getContentResolver().openInputStream(getContent().getUri());
                    } else {
                        // Load data from memory
                        stream = new ByteArrayInputStream(data);
                    }
                    msrpMgr.sendChunks(stream, IdGenerator.generateMessageID(), getContent().getEncoding(), getContent().getSize(), TypeMsrpChunk.FileSharing);
                } catch (SecurityException e){
                    if (logger.isActivated()) {
                        logger.error("Session initiation has failed due to that the file is not accessible!", e);
                    }
                    Collection<ImsSessionListener> listeners = getListeners();
                    for (ImsSessionListener listener : listeners) {
                        ((FileSharingSessionListener)listener).handleTransferNotAllowedToSend();
                    }
                } catch(Exception e) {
                    // Unexpected error
                    if (logger.isActivated()) {
                        logger.error("Session initiation has failed", e);
                    }
                    handleError(new ImsServiceError(ImsServiceError.UNEXPECTED_EXCEPTION,
                            e.getMessage()));
                }
            }
        };
        thread.start();
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
    	
    	// File has been transfered
    	fileTransfered();
    	
        // Close the media session
        closeMediaSession();
		
		// Terminate session
		terminateSession(ImsServiceSession.TERMINATION_BY_USER);
	   	
    	// Remove the current session
    	getImsService().removeSession(this);

    	// Notify listeners
    	for(int j=0; j < getListeners().size(); j++) {
    		((FileSharingSessionListener)getListeners().get(j)).handleFileTransfered(getContent());
        }
    	InstantMessagingService imService = ((InstantMessagingService) getImsService());
    	ContactId contact = getRemoteContact();
    	String fileTransferId = getFileTransferId();
    	imService.receiveFileDeliveryStatus(contact, new ImdnDocument(fileTransferId, ImdnDocument.POSITIVE_DELIVERY,
    			ImdnDocument.DELIVERY_STATUS_DELIVERED));
    	imService.receiveFileDeliveryStatus(contact, new ImdnDocument(fileTransferId, ImdnDocument.DISPLAY,
    			ImdnDocument.DELIVERY_STATUS_DISPLAYED));
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
    		((FileSharingSessionListener)getListeners().get(j)).handleTransferProgress(currentSize, totalSize);
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
     * Close media session
     */
    public void closeMediaSession() {
        // Close MSRP session
        if (msrpMgr != null) {
            msrpMgr.closeSession();
        }
        if (logger.isActivated()) {
            logger.debug("MSRP session has been closed");
        }
    }

	@Override
	public boolean isInitiatedByRemote() {
		return false;
	}

}
