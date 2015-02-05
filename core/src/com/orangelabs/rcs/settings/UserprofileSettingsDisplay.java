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

package com.orangelabs.rcs.settings;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * Presence settings display
 * 
 * @author jexa7410
 */
public class UserprofileSettingsDisplay extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {

    private EditTextPreference displaynameEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rcs_settings_userprofile_preferences);
        setTitle(R.string.rcs_settings_title_userprofile_settings);

        displaynameEdit = (EditTextPreference) findPreference("edit_displayname");
        displaynameEdit.setPersistent(false);
        displaynameEdit.setOnPreferenceChangeListener(this);
        String name = RcsSettings.getInstance().getUserProfileImsDisplayName();
        displaynameEdit.setText(name);
        displaynameEdit.setTitle(name);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference.getKey().equals("edit_displayname")) {
            String name = (String) objValue;
            RcsSettings.getInstance().setUserProfileImsDisplayName(name);
            displaynameEdit.setTitle(name);
        }
        return true;
    }
}
