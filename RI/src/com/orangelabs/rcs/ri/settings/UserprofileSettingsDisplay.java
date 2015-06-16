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

package com.orangelabs.rcs.ri.settings;

import com.gsma.services.rcs.RcsServiceException;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * Presence settings display
 * 
 * @author jexa7410
 */
public class UserprofileSettingsDisplay extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {

    private EditTextPreference displaynameEdit;

    private ConnectionManager mCnxManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rcs_settings_userprofile_preferences);

        displaynameEdit = (EditTextPreference) findPreference("edit_displayname");
        displaynameEdit.setPersistent(false);
        displaynameEdit.setOnPreferenceChangeListener(this);

        mCnxManager = ConnectionManager.getInstance();
        if (!mCnxManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            Utils.showMessage(this, getString(R.string.label_service_not_available));
            return;
        }

        try {
            String name = mCnxManager.getContactApi().getCommonConfiguration().getMyDisplayName();
            displaynameEdit.setText(name);
            displaynameEdit.setTitle(name);
        } catch (RcsServiceException e) {
            Utils.showMessage(this, getString(R.string.label_api_failed));
            return;
        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if ("edit_displayname".equals(preference.getKey())) {
            String name = (String) objValue;
            try {
                mCnxManager.getContactApi().getCommonConfiguration().setMyDisplayName(name);
                displaynameEdit.setTitle(name);
            } catch (RcsServiceException e) {
                Utils.showMessage(this, getString(R.string.label_api_failed));
            }
        }
        return true;
    }
}
