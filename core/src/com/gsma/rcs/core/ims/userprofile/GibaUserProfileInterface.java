/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.userprofile;

import com.gsma.rcs.provider.settings.RcsSettings;

/**
 * User profile derived from IMSI
 * 
 * @author JM. Auffret
 */
public class GibaUserProfileInterface extends UserProfileInterface {

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param rcsSettings
     */
    public GibaUserProfileInterface(RcsSettings rcsSettings) {
        super();
        mRcsSettings = rcsSettings;
    }

    /**
     * Read the user profile
     * 
     * @return User profile
     */
    public UserProfile read() {
        return new UserProfile(null, /* User name derived from GIBA procedure */
        null, /* Domain derived from GIBA procedure */
        null, /* No private ID with GIBA procedure */
        null, /* No password with GIBA procedure */
        null, /* No realm with GIBA procedure */
        mRcsSettings.getXdmServer(), null, /* Login derived from GIBA procedure */
        mRcsSettings.getUserProfileImsPassword(), mRcsSettings.getImConferenceUri(), mRcsSettings);
    }
}
