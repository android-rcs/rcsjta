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
        // Read profile info from the database settings
        ContactId contact = mRcsSettings.getUserProfileImsUserName();
        if (contact == null) {
            if (sLogger.isActivated()) {
                sLogger.error("IMS user name not provisioned");
            }
            return null;

        }
        String homeDomain = mRcsSettings.getUserProfileImsDomain();
        String privateID = mRcsSettings.getUserProfileImsPrivateId();
        String password = mRcsSettings.getUserProfileImsPassword();
        String realm = mRcsSettings.getUserProfileImsRealm();
        String xdmServer = mRcsSettings.getXdmServer();
        String xdmLogin = mRcsSettings.getXdmLogin();
        String xdmPassword = mRcsSettings.getXdmPassword();
        String imConfUri = mRcsSettings.getImConferenceUri();

        return new UserProfile(contact, homeDomain, privateID, password, realm, xdmServer,
                xdmLogin, xdmPassword, imConfUri, mRcsSettings);
    }
}
