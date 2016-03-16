/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.service.richcall.image;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.capability.CapabilityService;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingError;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingSession;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.StorageUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Image sharing transfer session
 * 
 * @author jexa7410
 */
public abstract class ImageTransferSession extends ContentSharingSession {

    private final static String BOUNDARY_TAG = "boundary1";

    /**
     * Default SO_TIMEOUT value (in milliseconds)
     */
    public final static long DEFAULT_SO_TIMEOUT = 30000;

    private boolean mImageTransfered = false;

    private MmContent mThumbnail;

    private static final Logger sLogger = Logger.getLogger(ImageTransferSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param parent Richcall service
     * @param content Content to be shared
     * @param contact Remote contact Id
     * @param thumbnail The thumbnail content
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     * @param capabilityService
     */
    public ImageTransferSession(RichcallService parent, MmContent content, ContactId contact,
            MmContent thumbnail, RcsSettings rcsSettings, long timestamp,
            ContactManager contactManager, CapabilityService capabilityService) {
        super(parent, content, contact, rcsSettings, timestamp, contactManager, capabilityService);
        mThumbnail = thumbnail;
    }

    /**
     * Sets image transferred
     */
    public void setImageTransferred() {
        mImageTransfered = true;
    }

    /**
     * Is image transferred
     * 
     * @return Boolean
     */
    public boolean isImageTransferred() {
        return mImageTransfered;
    }

    /**
     * Returns max image sharing size
     * 
     * @param rcsSettings
     * @return Size in bytes
     */
    public static long getMaxImageSharingSize(RcsSettings rcsSettings) {
        return rcsSettings.getMaxImageSharingSize();
    }

    /**
     * Create an INVITE request
     * 
     * @return the INVITE request
     * @throws PayloadException
     */
    public SipRequest createInvite() throws PayloadException {
        if (mThumbnail != null) {
            return SipMessageFactory.createMultipartInvite(getDialogPath(),
                    RichcallService.FEATURE_TAGS_IMAGE_SHARE, getDialogPath().getLocalContent(),
                    BOUNDARY_TAG);
        }
        return SipMessageFactory.createInvite(getDialogPath(),
                RichcallService.FEATURE_TAGS_IMAGE_SHARE, getDialogPath().getLocalContent());
    }

    @Override
    public void handleError(ImsServiceError error) {
        if (isSessionInterrupted()) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Transfer error: ").append(error.getErrorCode())
                    .append(", reason=").append(error.getMessage()).toString());
        }
        closeMediaSession();
        removeSession();
        ContactId contact = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            ((ImageTransferSessionListener) listener).onSharingError(contact,
                    new ContentSharingError(error));
        }
    }

    /**
     * Returns the thumbnail content
     * 
     * @return Thumbnail
     */
    public MmContent getThumbnail() {
        return mThumbnail;
    }

    /**
     * Check if image capacity is acceptable
     * 
     * @param imageSize Image size in bytes
     * @param rcsSettings
     * @return Error or null if image capacity is acceptable
     */
    public static ContentSharingError isImageCapacityAcceptable(long imageSize,
            RcsSettings rcsSettings) {
        boolean fileIsToBig = (ImageTransferSession.getMaxImageSharingSize(rcsSettings) > 0) ? imageSize > ImageTransferSession
                .getMaxImageSharingSize(rcsSettings) : false;
        boolean storageIsTooSmall = (StorageUtils.getExternalStorageFreeSpace() > 0) ? imageSize > StorageUtils
                .getExternalStorageFreeSpace() : false;
        if (fileIsToBig) {
            if (sLogger.isActivated()) {
                sLogger.warn("Image is too big, reject the image sharing");
            }
            return new ContentSharingError(ContentSharingError.MEDIA_SIZE_TOO_BIG);
        }
        if (storageIsTooSmall) {
            if (sLogger.isActivated()) {
                sLogger.warn("Not enough storage capacity, reject the image sharing");
            }
            return new ContentSharingError(ContentSharingError.NOT_ENOUGH_STORAGE_SPACE);
        }
        return null;
    }

    @Override
    public void handleInactivityEvent() {
        /* Not need in this class */
    }

    @Override
    public void startSession() {
        mRichcallService.addSession(this);
        start();
    }

    @Override
    public void removeSession() {
        mRichcallService.removeSession(this);
    }
}
