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

/**
 * Intent for geoloc sharing invitations
 * 
 * @author Jean-Marc AUFFRET
 */
public class GeolocSharingIntent {
    /**
     * Broadcast action: a new geoloc sharing invitation has been received.
     * <p>
     * Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_SHARING_ID} containing the unique ID of the geoloc sharing.
     * </ul>
     */
    public final static String ACTION_NEW_INVITATION = "com.gsma.services.rcs.sharing.geoloc.action.NEW_GEOLOC_SHARING";

    /**
     * Unique ID of the geoloc sharing
     */
    public final static String EXTRA_SHARING_ID = "sharingId";

    private GeolocSharingIntent() {
    }
}
