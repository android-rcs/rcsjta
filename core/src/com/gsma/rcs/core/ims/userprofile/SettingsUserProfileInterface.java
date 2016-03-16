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
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

/**
 * User profile read from RCS settings database
 * 
 * @author JM. Auffret
 */
public class SettingsUserProfileInterface extends UserProfileInterface {

    private static final Logger sLogger = Logger.getLogger(SettingsUserProfileInterface.class
            .getSimpleName());

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param rcsSettings
     */
    public SettingsUserProfileInterface(RcsSettings rcsSettings) {
        super();
        mRcsSettings = rcsSettings;
    }

    /**
     * Read the user profile
     * 
     * @return User profile
     */
    public UserProfile read() {
        ContactId contact = mRcsSettings.getUserProfileImsUserName();
        if (contact == null) {
            if (sLogger.isActivated()) {
                sLogger.error("IMS user name not provisioned");
            }
            return null;

        }
        return new UserProfile(contact, mRcsSettings.getUserProfileImsDomain(),
                mRcsSettings.getUserProfileImsPrivateId(),
                mRcsSettings.getUserProfileImsPassword(), mRcsSettings.getUserProfileImsRealm(),
                mRcsSettings.getXdmServer(), mRcsSettings.getXdmLogin(),
                mRcsSettings.getXdmPassword(), mRcsSettings.getImConferenceUri(), mRcsSettings);
    }
}
