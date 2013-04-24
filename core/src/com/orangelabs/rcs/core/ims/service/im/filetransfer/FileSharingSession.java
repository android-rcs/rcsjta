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
package com.orangelabs.rcs.core.ims.service.im.filetransfer;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File transfer session
 * 
 * @author jexa7410
 */
public abstract class FileSharingSession extends ImsServiceSession {
	/**
	 * Boundary tag
	 */
	private final static String BOUNDARY_TAG = "boundary1";
	
	/**
	 * Default SO_TIMEOUT value (in seconds)
	 */
	public final static int DEFAULT_SO_TIMEOUT = 30;
	
	/**
	 * Content to be shared
	 */
	private MmContent content;
	
	/**
	 * File transfered
	 */
	private boolean fileTransfered = false;

	/**
	 * Thumbnail
	 */
	private byte[] thumbnail = null;
	
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
	 * @param thumbnail Thumbnail
	 */
	public FileSharingSession(ImsService parent, MmContent content, String contact, byte[] thumbnail) {
		super(parent, contact);
		
		this.content = content;
		this.thumbnail = thumbnail;
	}
	
	/**
	 * Returns the content
	 * 
	 * @return Content 
	 */
	public MmContent getContent() {
		return content;
	}
	
	/**
	 * Set the content
	 * 
	 * @param content Content  
	 */
	public void setContent(MmContent content) {
		this.content = content;
	}	
	
	/**
	 * Returns the "file-selector" attribute
	 * 
	 * @return String
	 */
	public String getFileSelectorAttribute() {
		return "name:\"" + content.getName() + "\"" + 
			" type:" + content.getEncoding() +
			" size:" + content.getSize();
	}
	
	/**
	 * Returns the "file-location" attribute
	 * 
	 * @return String
	 */
	public String getFileLocationAttribute() {
		if ((content.getUrl() != null) && content.getUrl().startsWith("http")) {
			return content.getUrl();
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the "file-transfer-id" attribute
	 * 
	 * @return String
	 */
	public String getFileTransferId() {
		return "" + System.currentTimeMillis();
	}	
	
	/**
	 * File has been transfered
	 */
	public void fileTransfered() {
		this.fileTransfered = true;
	}
	
	/**
	 * Is file transfered
	 * 
	 * @retrurn Boolean
	 */
	public boolean isFileTransfered() {
		return fileTransfered; 
	}
		
	/**
	 * Receive BYE request 
	 * 
	 * @param bye BYE request
	 */
	public void receiveBye(SipRequest bye) {
		super.receiveBye(bye);
		
		// If the content is not fully transfered then request capabilities to the remote
		if (!isFileTransfered()) {
			getImsService().getImsModule().getCapabilityService().requestContactCapabilities(getDialogPath().getRemoteParty());
		}
	}

	/**
	 * Returns max file sharing size
	 * 
	 * @return Size in bytes
	 */
	public static int getMaxFileSharingSize() {
		return RcsSettings.getInstance().getMaxFileTransferSize()*1024;
	}

    /**
     * Create an INVITE request
     *
     * @return the INVITE request
     * @throws SipException 
     */
    public SipRequest createInvite() throws SipException {
    	if (thumbnail != null) {
	        return SipMessageFactory.createMultipartInvite(
	                getDialogPath(),
	                InstantMessagingService.FT_FEATURE_TAGS,
	                getDialogPath().getLocalContent(),
	                BOUNDARY_TAG);
    	} else {
	        return SipMessageFactory.createInvite(
	                getDialogPath(),
	                InstantMessagingService.FT_FEATURE_TAGS,
	                getDialogPath().getLocalContent());
    	}
    }

    /**
     * Handle error 
     * 
     * @param error Error
     */
    public void handleError(ImsServiceError error) {
        if (isInterrupted()) {
            return;
        }

        // Error    
        if (logger.isActivated()) {
            logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
        }

        // Close media session
        closeMediaSession();

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        for(int j=0; j < getListeners().size(); j++) {
            ((FileSharingSessionListener)getListeners().get(j)).handleTransferError(new FileSharingError(error));
        }
    }

    /**
     * Returns the thumbnail
     * 
     * @return Thumbnail
     */
    public byte[] getThumbnail() {
    	return thumbnail;
    }
}
