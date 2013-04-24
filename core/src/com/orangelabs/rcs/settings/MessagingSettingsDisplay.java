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
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * Messaging settings display
 * 
 * @author jexa7410
 */
public class MessagingSettingsDisplay extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
	private CheckBoxPreference filetransfer_vibrate;
	private CheckBoxPreference chat_vibrate;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.rcs_settings_messaging_preferences);
        setTitle(R.string.rcs_settings_title_messaging_settings);
        
        filetransfer_vibrate = (CheckBoxPreference)findPreference("filetransfer_invitation_vibration");
        filetransfer_vibrate.setPersistent(false);
        filetransfer_vibrate.setOnPreferenceChangeListener(this);
        filetransfer_vibrate.setChecked(RcsSettings.getInstance().isPhoneVibrateForFileTransferInvitation());        

        chat_vibrate = (CheckBoxPreference)findPreference("chat_invitation_vibration");
        chat_vibrate.setPersistent(false);
        chat_vibrate.setOnPreferenceChangeListener(this);
        chat_vibrate.setChecked(RcsSettings.getInstance().isPhoneVibrateForChatInvitation());        
	}

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference.getKey().equals("filetransfer_invitation_vibration")) {
        	Boolean state = (Boolean)objValue;
        	RcsSettings.getInstance().setPhoneVibrateForFileTransferInvitation(state.booleanValue());
        }
        if (preference.getKey().equals("chat_invitation_vibration")) {
        	Boolean state = (Boolean)objValue;
        	RcsSettings.getInstance().setPhoneVibrateForChatInvitation(state.booleanValue());
        }
        return true;
    }    
}
