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
package com.orangelabs.rcs.tts;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * Text-To-Speech for incoming chat and group chat invitation
 *  
 * @author jexa7410
 */
public class Main extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private CheckBoxPreference activateCheck;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set title
        setTitle(R.string.app_title);
        
        // Set preferences
        addPreferencesFromResource(R.xml.tts_preferences);
        activateCheck = (CheckBoxPreference)getPreferenceScreen().findPreference("activate");
        activateCheck.setOnPreferenceChangeListener(this);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();

        // Load preferences
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Registry.REGISTRY, Activity.MODE_PRIVATE);
        boolean flag = Registry.readBoolean(preferences, Registry.ACTIVATE_TTS, false);
		activateCheck.setChecked(flag);
    }
    
    public boolean onPreferenceChange(Preference preference, Object objValue) {
    	// Update preferences
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Registry.REGISTRY, Activity.MODE_PRIVATE);
        if (preference.getKey().equals("activate")) {
	        boolean flag = !activateCheck.isChecked();
	        Registry.writeBoolean(preferences, Registry.ACTIVATE_TTS, flag);
        }
    	return true;
    }    
 }    
