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

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingSession;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.StorageUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Image sharing transfer session
 * 
 * @author jexa7410
 */
public abstract class ImageTransferSession extends ContentSharingSession {
	/**
	 * Boundary tag
	 */
	private final static String BOUNDARY_TAG = "boundary1";
	
	/**
	 * Default SO_TIMEOUT value (in seconds)
	 */
	public final static int DEFAULT_SO_TIMEOUT = 30;
	
	/**
	 * Image transfered
	 */
	private boolean imageTransfered = false;

	/**
	 * Thumbnail
	 */
	MmContent thumbnail;
    
    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(ImageTransferSession.class.getSimpleName());

    /**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param content Content to be shared
	 * @param contact Remote contact Id
	 * @param thumbnail The thumbnail content
	 */
	public ImageTransferSession(ImsService parent, MmContent content, ContactId contact, MmContent thumbnail) {
		super(parent, content, contact);
		
		this.thumbnail = thumbnail;
	}
	
	/**
	 * Image has been transfered
	 */
	public void imageTransfered() {
		this.imageTransfered = true;
	}
	
	/**
	 * Is image transfered
	 * 
	 * @return Boolean
	 */
	public boolean isImageTransfered() {
		return imageTransfered; 
	}
	
	/**
	 * Returns max image sharing size
	 * 
	 * @return Size in bytes
	 */
	public static long getMaxImageSharingSize() {
		return RcsSettings.getInstance().getMaxImageSharingSize();
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
	                RichcallService.FEATURE_TAGS_IMAGE_SHARE,
	                getDialogPath().getLocalContent(),
	                BOUNDARY_TAG);
    	} else {
	        return SipMessageFactory.createInvite(
	                getDialogPath(),
	                RichcallService.FEATURE_TAGS_IMAGE_SHARE,
	                getDialogPath().getLocalContent());
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
            logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
        }

        // Close MSRP session
        closeMediaSession();

        // Remove the current session
        removeSession();

        // Notify listeners
        for(int j=0; j < getListeners().size(); j++) {
            ((ImageTransferSessionListener)getListeners().get(j)).handleSharingError(new ContentSharingError(error));
        }
    }

    /**
     * Returns the thumbnail content
     * 
     * @return Thumbnail
     */
    public MmContent getThumbnail() {
    	return thumbnail;
    }

	/**
	 * Check if image capacity is acceptable
	 * 
	 * @param imageSize Image size in bytes
	 * @return Error or null if image capacity is acceptable
	 */
	public static ContentSharingError isImageCapacityAcceptable(long imageSize) {
		boolean fileIsToBig = (ImageTransferSession.getMaxImageSharingSize() > 0) ? imageSize > ImageTransferSession.getMaxImageSharingSize() : false;
		boolean storageIsTooSmall = (StorageUtils.getExternalStorageFreeSpace() > 0) ? imageSize > StorageUtils.getExternalStorageFreeSpace() : false;
		if (fileIsToBig) {
			if (logger.isActivated()) {
				logger.warn("Image is too big, reject the image sharing");
			}
			return new ContentSharingError(ContentSharingError.MEDIA_SIZE_TOO_BIG);
		} else {
			if (storageIsTooSmall) {
				if (logger.isActivated()) {
					logger.warn("Not enough storage capacity, reject the image sharing");
				}
				return new ContentSharingError(ContentSharingError.NOT_ENOUGH_STORAGE_SPACE);
			}
		}
		return null;
	}

	@Override
	public void startSession() {
		getImsService().getImsModule().getRichcallService().addSession(this);
		start();
	}

	@Override
	public void removeSession() {
		getImsService().getImsModule().getRichcallService().removeSession(this);
	}
}
