/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.services.rcs.sharing.geoloc;

import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing.ReasonCode;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing.State;

import java.util.Set;

/**
 * Geoloc sharing event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class GeolocSharingListener {
    /**
     * Callback called when the geoloc sharing state changes
     * 
     * @param contact Contact ID
     * @param sharingId ID of geoloc sharing
     * @param state State of image sharing
     * @param reasonCode Reason code of geoloc sharing state
     */
    public abstract void onStateChanged(ContactId contact, String sharingId, State state,
            ReasonCode reasonCode);

    /**
     * Callback called during the sharing progress
     * 
     * @param contact Contact ID
     * @param sharingId ID of geoloc sharing
     * @param currentSize Current transferred size in bytes
     * @param totalSize Total size to transfer in bytes
     */
    public abstract void onProgressUpdate(ContactId contact, String sharingId, long currentSize,
            long totalSize);

    /**
     * Callback called when a delete operation completed that resulted in that one or several geoloc
     * sharings was deleted specified by the sharingIds parameter corresponding to a specific
     * contact.
     *
     * @param contact Contact ID
     * @param sharingIds ids of those deleted geoloc sharing
     */
    public abstract void onDeleted(ContactId contact, Set<String> sharingIds);
}
