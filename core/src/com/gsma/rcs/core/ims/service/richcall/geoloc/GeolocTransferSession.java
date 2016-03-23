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

package com.gsma.rcs.core.ims.service.richcall.geoloc;

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
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Geoloc sharing transfer session
 * 
 * @author jexa7410
 */
public abstract class GeolocTransferSession extends ContentSharingSession {
    /**
     * Default SO_TIMEOUT value (in milliseconds)
     */
    public final static long DEFAULT_SO_TIMEOUT = 30000;

    private boolean mGeolocTransferred = false;

    private Geoloc mGeoloc;

    private static final Logger sLogger = Logger.getLogger(GeolocTransferSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param parent Richcall service
     * @param content Content to be shared
     * @param contact Remote contact Id
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     * @param capabilityService
     */
    public GeolocTransferSession(RichcallService parent, MmContent content, ContactId contact,
            RcsSettings rcsSettings, long timestamp, ContactManager contactManager,
            CapabilityService capabilityService) {
        super(parent, content, contact, rcsSettings, timestamp, contactManager, capabilityService);
    }

    /**
     * Set geoloc
     * 
     * @param geoloc Geoloc
     */
    public void setGeoloc(Geoloc geoloc) {
        mGeoloc = geoloc;
    }

    /**
     * Get geoloc
     * 
     * @return Geoloc
     */
    public Geoloc getGeoloc() {
        return mGeoloc;
    }

    /**
     * Sets Geoloc transferred
     */
    public void setGeolocTransferred() {
        mGeolocTransferred = true;
    }

    /**
     * Is geoloc transferred
     * 
     * @return Boolean
     */
    public boolean isGeolocTransferred() {
        return mGeolocTransferred;
    }

    /**
     * Create an INVITE request
     * 
     * @return the INVITE request
     * @throws PayloadException
     */
    public SipRequest createInvite() throws PayloadException {
        return SipMessageFactory.createInvite(getDialogPath(),
                RichcallService.FEATURE_TAGS_GEOLOC_SHARE, getDialogPath().getLocalContent());
    }

    /**
     * Session inactivity event
     */
    public void handleInactivityEvent() {
        /* Not need in this class */
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
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Transfer error: ").append(error.getErrorCode())
                    .append(", reason=").append(error.getMessage()).toString());
        }
        closeMediaSession();
        removeSession();
        ContactId contact = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            ((GeolocTransferSessionListener) listener).onSharingError(contact,
                    new ContentSharingError(error));
        }
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
