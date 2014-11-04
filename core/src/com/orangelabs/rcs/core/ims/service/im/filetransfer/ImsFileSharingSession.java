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
package com.orangelabs.rcs.core.ims.service.im.filetransfer;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import android.net.Uri;

/**
 * Abstract IMS file transfer session
 * 
 * @author jexa7410
 */
public abstract class ImsFileSharingSession extends FileSharingSession {
	/**
	 * Boundary tag
	 */
	private final static String BOUNDARY_TAG = "boundary1";
	
	/**
	 * Default SO_TIMEOUT value (in seconds)
	 */
	public final static int DEFAULT_SO_TIMEOUT = 30;

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(ImsFileSharingSession.class.getSimpleName());

    /**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param content Content of file to be shared
	 * @param contact Remote contact identifier
	 * @param fileIcon Content of file icon
	 * @param filetransferId File transfer Id
	 */
	public ImsFileSharingSession(ImsService parent, MmContent content, ContactId contact, MmContent fileIcon, String filetransferId) {
		super(parent, content, contact, PhoneUtils.formatContactIdToUri(contact), fileIcon, filetransferId);
	}
	
	/**
	 * Returns the "file-transfer-id" attribute
	 * 
	 * @return String
	 */
	public String getFileTransferIdAttribute() {
		return "" + System.currentTimeMillis();
	}	
	
	/**
	 * Returns the "file-selector" attribute
	 * 
	 * @return String
	 */
	public String getFileSelectorAttribute() {
		return "name:\"" + getContent().getName() + "\"" + 
			" type:" + getContent().getEncoding() +
			" size:" + getContent().getSize();
	}
	
	/**
	 * Returns the "file-location" attribute
	 * 
	 * @return Uri
	 */
	public Uri getFileLocationAttribute() {
		Uri file = getContent().getUri();
		if ((file != null) && file.getScheme().startsWith("http")) {
			return file;
		} else {
			return null;
		}
	}

    /**
     * Create an INVITE request
     *
     * @return the INVITE request
     * @throws SipException 
     */
    public SipRequest createInvite() throws SipException {
        SipRequest invite;
    	if (getFileicon() != null) {
	        invite = SipMessageFactory.createMultipartInvite(
	                getDialogPath(),
	                InstantMessagingService.FT_FEATURE_TAGS,
	                getDialogPath().getLocalContent(),
	                BOUNDARY_TAG);
    	} else {
	        invite = SipMessageFactory.createInvite(
	                getDialogPath(),
	                InstantMessagingService.FT_FEATURE_TAGS,
	                getDialogPath().getLocalContent());
    	}
        
    	// Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());

        return invite;
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
     * Data transfer error
     *
     * @param msgId Message ID
     * @param error Error code
     */
    public void msrpTransferError(String msgId, String error, MsrpSession.TypeMsrpChunk typeMsrpChunk) {
        if (isSessionInterrupted() || getDialogPath().isSessionTerminated()) {
            return;
        }
        
        if (logger.isActivated()) {
            logger.info("Data transfer error " + error);
        }

        try {
			// Terminate session
			terminateSession(ImsServiceSession.TERMINATION_BY_SYSTEM);

			// Close the media session
            closeMediaSession();
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't close correctly the file transfer session", e);
            }
        }

		try {
			ContactId remote = ContactUtils.createContactId(getDialogPath().getRemoteParty());
			// Request capabilities
	        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(remote);

		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.warn("Cannot request capabilities for contact "+getDialogPath().getRemoteParty() );
			}
		}
        
        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        if (!isSessionInterrupted() && !isSessionTerminatedByRemote()) {
            for(int j=0; j < getListeners().size(); j++) {
                ((FileSharingSessionListener)getListeners().get(j)).handleTransferError(new FileSharingError(FileSharingError.MEDIA_TRANSFER_FAILED, error));
            }
        }
    }

}
