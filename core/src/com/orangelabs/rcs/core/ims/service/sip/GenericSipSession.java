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

import java.io.ByteArrayInputStream;
import java.util.Vector;

import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpManager;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.capability.CapabilityUtils;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Generic SIP session 
 * 
 * @author jexa7410
 */
public abstract class GenericSipSession extends ImsServiceSession implements MsrpEventListener {
	/**
	 * Feature tag
	 */
	private String featureTag;	
	
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
	 * @param contact Remote contact
	 * @param featureTag Feature tag
	 */
	public GenericSipSession(ImsService parent, String contact, String featureTag) {
		super(parent, contact);

		// Set the service feature tag
		this.featureTag = featureTag;

        // Create the MSRP manager
		int localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort();
		String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface().getNetworkAccess().getIpAddress();
		msrpMgr = new MsrpManager(localIpAddress, localMsrpPort);
	}
	
	/**
	 * Returns feature tag of the service
	 * 
	 * @return Feature tag
	 */
	public String getFeatureTag() {
		return featureTag;
	}
	
	/**
	 * Returns the service ID
	 * 
	 * @return Service ID
	 */
	public String getServiceId() {
		return CapabilityUtils.extractServiceId(featureTag);
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
     * Create an INVITE request
     *
     * @return the INVITE request
     * @throws SipException 
     */
    public SipRequest createInvite() throws SipException {
        return SipMessageFactory.createInvite(
                getDialogPath(),
                new String [] { getFeatureTag() },
                getDialogPath().getLocalContent());
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
        String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
        int remotePort = mediaDesc.port;

        // Create the MSRP session
        MsrpSession session = getMsrpMgr().createMsrpClientSession(remoteHost, remotePort, remoteMsrpPath, this);
        session.setFailureReportOption(false);
        session.setSuccessReportOption(false);

        // Open the MSRP session
        getMsrpMgr().openMsrpSession();
    }

    /**
     * Start media session
     * 
     * @throws Exception 
     */
    public void startMediaSession() throws Exception {
        // Nothing to do
    }

    /**
     * Close media session
     */
    public void closeMediaSession() {
    	if (msrpMgr != null) {
    		msrpMgr.closeSession();
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
        if (isSessionInterrupted()) {
        	return;
        }
        
        // Error
        if (logger.isActivated()) {
            logger.info("Session error: " + error.getErrorCode() + ", reason="
                    + error.getMessage());
        }

        // Close media session
        closeMediaSession();

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        for (int j = 0; j < getListeners().size(); j++) {
            ((SipSessionListener) getListeners().get(j))
                    .handleSessionError(new SipSessionError(error));
        }
    }
    
    /**
     * Sends a message in real time
     * 
     * @param content Message content
	 * @return Returns true if sent successfully else returns false
     */
    public boolean sendMessage(byte[] content) {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(content); 
	    	String msgId = IdGenerator.getIdentifier().replace('_', '-');
			msrpMgr.sendChunks(stream, msgId, SipService.MIME_TYPE, content.length);
			return true;
		} catch(Exception e) {
			// Error
	   		if (logger.isActivated()) {
	   			logger.error("Problem while sending data chunks", e);
	   		}
			return false;
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
    	
    	if ((data == null) || (data.length == 0)) {
    		// By-pass empty data
        	if (logger.isActivated()) {
        		logger.debug("By-pass received empty data");
        	}
    		return;
    	}

        // Notify listeners
    	for(int i=0; i < getListeners().size(); i++) {
            ((SipSessionListener)getListeners().get(i)).handleReceiveData(data);
        }
	}
    
	/**
	 * Data transfer in progress
	 * 
	 * @param currentSize Current transfered size in bytes
	 * @param totalSize Total size in bytes
	 */
	public void msrpTransferProgress(long currentSize, long totalSize) {
		// Not used here
	}

    /**
     * Data transfer in progress
     *
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     * @param data received data chunk
     */
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
		// Not used here
        return false;
    }

	/**
	 * Data transfer has been aborted
	 */
	public void msrpTransferAborted() {
		// Not used here
	}	

    /**
     * Data transfer error
     *
     * @param msgId Message ID
     * @param error Error code
     */
    public void msrpTransferError(String msgId, String error) {
		if (isSessionInterrupted()) {
			return;
		}
		
		if (logger.isActivated()) {
            logger.info("Data transfer error " + error);
        }

        // Notify listeners
        for(int i=0; i < getListeners().size(); i++) {
            ((SipSessionListener)getListeners().get(i)).handleSessionError(new SipSessionError(SipSessionError.MEDIA_TRANSFER_FAILED, error));
        }
    }
}
