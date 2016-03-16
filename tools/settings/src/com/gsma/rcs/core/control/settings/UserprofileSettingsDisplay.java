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

package com.gsma.rcs.core.control.settings;

import com.gsma.services.rcs.RcsServiceException;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.RcsPreferenceActivity;
import com.gsma.rcs.core.control.R;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;

/**
 * Presence settings display
 * 
 * @author jexa7410
 */
public class UserprofileSettingsDisplay extends RcsPreferenceActivity implements
        Preference.OnPreferenceChangeListener {

    private EditTextPreference displaynameEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rcs_settings_userprofile_preferences);

        displaynameEdit = (EditTextPreference) findPreference("edit_displayname");
        displaynameEdit.setPersistent(false);
        displaynameEdit.setOnPreferenceChangeListener(this);

        if (!isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            showMessage(R.string.label_service_not_available);
            return;
        }

        try {
            String name = getContactApi().getCommonConfiguration().getMyDisplayName();
            displaynameEdit.setText(name);
            displaynameEdit.setTitle(name);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if ("edit_displayname".equals(preference.getKey())) {
            String name = (String) objValue;
            try {
                getContactApi().getCommonConfiguration().setMyDisplayName(name);
                displaynameEdit.setTitle(name);

            } catch (RcsServiceException e) {
                showExceptionThenExit(e);
            }
        }
        return true;
    }
}
