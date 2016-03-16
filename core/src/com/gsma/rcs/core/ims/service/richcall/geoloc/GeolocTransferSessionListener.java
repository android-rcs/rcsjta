/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.service.richcall.geoloc;

import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingError;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Geoloc sharing transfer session listener
 * 
 * @author jexa7410
 */
public interface GeolocTransferSessionListener extends ImsSessionListener {
    /**
     * Content sharing error
     * 
     * @param contact Remote contact
     * @param error Error
     */
    public void onSharingError(ContactId contact, ContentSharingError error);

    /**
     * Content has been transfered
     * 
     * @param contact Remote contact
     * @param geoloc Geolocation
     * @param initiatedByRemote
     */
    public void onContentTransferred(ContactId contact, Geoloc geoloc, boolean initiatedByRemote);

    /**
     * Destination user agent received INVITE, and is alerting user of call
     * 
     * @param contact Remote contact
     */
    public void onSessionRinging(ContactId contact);

    /**
     * A session invitation has been received
     * 
     * @param contact Remote contact
     * @param timestamp Local timestamp when got invitation
     */
    public void onInvitationReceived(ContactId contact, long timestamp);
}
