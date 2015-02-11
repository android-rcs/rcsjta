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
import com.gsma.rcs.utils.ContactUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * User profile read from RCS settings database
 * 
 * @author JM. Auffret
 */
public class SettingsUserProfileInterface extends UserProfileInterface {

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(SettingsUserProfileInterface.class
            .getSimpleName());

    /**
     * Constructor
     */
    public SettingsUserProfileInterface() {
        super();
    }

    /**
     * Read the user profile
     * 
     * @return User profile
     */
    public UserProfile read() {
        // Read profile info from the database settings
        try {
            ContactId contact = ContactUtils.createContactId(RcsSettings.getInstance()
                    .getUserProfileImsUserName());
            String homeDomain = RcsSettings.getInstance().getUserProfileImsDomain();
            String privateID = RcsSettings.getInstance().getUserProfileImsPrivateId();
            String password = RcsSettings.getInstance().getUserProfileImsPassword();
            String realm = RcsSettings.getInstance().getUserProfileImsRealm();
            String xdmServer = RcsSettings.getInstance().getXdmServer();
            String xdmLogin = RcsSettings.getInstance().getXdmLogin();
            String xdmPassword = RcsSettings.getInstance().getXdmPassword();
            String imConfUri = RcsSettings.getInstance().getImConferenceUri();

            return new UserProfile(contact, homeDomain, privateID, password, realm, xdmServer,
                    xdmLogin, xdmPassword, imConfUri);
        } catch (RcsContactFormatException e) {
            if (logger.isActivated()) {
                logger.error("cannot parse UserProfileImsUserName "
                        + RcsSettings.getInstance().getUserProfileImsUserName());
            }
            return null;
        }
    }
}
