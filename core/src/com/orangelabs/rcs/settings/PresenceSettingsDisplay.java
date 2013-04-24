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
public class PresenceSettingsDisplay extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
	
	private EditTextPreference freetextEdit1;
	private EditTextPreference freetextEdit2;
	private EditTextPreference freetextEdit3;
	private EditTextPreference freetextEdit4;
	private CheckBoxPreference vibrateInvitation;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.rcs_settings_presence_preferences);
        setTitle(R.string.rcs_settings_title_presence_settings);

        freetextEdit1 = (EditTextPreference)findPreference("edit_freetext1");
        freetextEdit1.setPersistent(false);
        freetextEdit1.setOnPreferenceChangeListener(this);
        String txt1 = RcsSettings.getInstance().getPredefinedFreetext1();
    	freetextEdit1.setText(txt1);
    	freetextEdit1.setTitle(txt1);
        
        freetextEdit2 = (EditTextPreference)findPreference("edit_freetext2");
        freetextEdit2.setPersistent(false);
        freetextEdit2.setOnPreferenceChangeListener(this);
        String txt2 = RcsSettings.getInstance().getPredefinedFreetext2();
    	freetextEdit2.setText(txt2);
    	freetextEdit2.setTitle(txt2);
        
        freetextEdit3 = (EditTextPreference)findPreference("edit_freetext3");
        freetextEdit3.setPersistent(false);
        freetextEdit3.setOnPreferenceChangeListener(this);
        String txt3 = RcsSettings.getInstance().getPredefinedFreetext3();
    	freetextEdit3.setText(txt3);
    	freetextEdit3.setTitle(txt3);
        
        freetextEdit4 = (EditTextPreference)findPreference("edit_freetext4");
        freetextEdit4.setPersistent(false);
        freetextEdit4.setOnPreferenceChangeListener(this);
        String txt4 = RcsSettings.getInstance().getPredefinedFreetext4();
    	freetextEdit4.setText(txt4);
    	freetextEdit4.setTitle(txt4);
        
    	vibrateInvitation = (CheckBoxPreference)findPreference("presence_invitation_vibration");
    	vibrateInvitation.setPersistent(false);
    	vibrateInvitation.setOnPreferenceChangeListener(this);
    	vibrateInvitation.setChecked(RcsSettings.getInstance().isPhoneVibrateForPresenceInvitation());
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference.getKey().equals("edit_freetext1")) {
            String txt = (String)objValue;
            RcsSettings.getInstance().setPredefinedFreetext1(txt);
        	freetextEdit1.setTitle(txt);
        } else        	
        if (preference.getKey().equals("edit_freetext2")) {
            String txt = (String)objValue;
            RcsSettings.getInstance().setPredefinedFreetext2(txt);
        	freetextEdit2.setTitle(txt);
        } else        	
        if (preference.getKey().equals("edit_freetext3")) {
            String txt = (String)objValue;
            RcsSettings.getInstance().setPredefinedFreetext3(txt);
        	freetextEdit3.setTitle(txt);
        } else        	
        if (preference.getKey().equals("edit_freetext4")) {
            String txt = (String)objValue;
            RcsSettings.getInstance().setPredefinedFreetext4(txt);
        	freetextEdit4.setTitle(txt);
        } else
        if (preference.getKey().equals("presence_invitation_vibration")) {
        	Boolean state = (Boolean)objValue;
        	RcsSettings.getInstance().setPhoneVibrateForPresenceInvitation(state.booleanValue());
        }
        return true;
    }    
}
