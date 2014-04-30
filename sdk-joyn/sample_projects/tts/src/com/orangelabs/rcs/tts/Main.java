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
