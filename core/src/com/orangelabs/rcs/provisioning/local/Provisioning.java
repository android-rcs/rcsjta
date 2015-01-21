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

package com.orangelabs.rcs.provisioning.local;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;

import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * Main
 * 
 * @author jexa7410
 */
@SuppressWarnings("deprecation")
public class Provisioning extends TabActivity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Instantiate the settings manager
        RcsSettings.createInstance(getApplicationContext());

        // Set tabs
        final TabHost tabHost = getTabHost();
        tabHost.addTab(tabHost.newTabSpec("profile")
                .setIndicator("Profile", null)
                .setContent(new Intent(this, ProfileProvisioning.class)));
        tabHost.addTab(tabHost.newTabSpec("stack")
                .setIndicator("Stack", null)
                .setContent(new Intent(this, StackProvisioning.class)));
        tabHost.addTab(tabHost.newTabSpec("ui")
                .setIndicator("Service", null)
                .setContent(new Intent(this, ServiceProvisioning.class)));
        tabHost.addTab(tabHost.newTabSpec("logger")
                .setIndicator("Logger", null)
                .setContent(new Intent(this, LoggerProvisioning.class)));
    }
	
	/**
	 * 
	 * Set edit text either from bundle or from RCS settings if bundle is null
	 * 
	 * @param activity
	 *            the activity
	 * @param viewID
	 *            the view ID for the text edit
	 * @param rcsSettingsKey
	 *            the key of the RCS parameter
	 * @param bundle
	 *            the bundle to save parameter
	 */
	public static void setEditTextParameter(final Activity activity, int viewID, String rcsSettingsKey, final Bundle bundle) {
		String parameter = null;
		if (bundle != null && bundle.containsKey(rcsSettingsKey)) {
			parameter = bundle.getString(rcsSettingsKey);
		} else {
			parameter = RcsSettings.getInstance().readParameter(rcsSettingsKey);
		}
		EditText editText = (EditText) activity.findViewById(viewID);
		editText.setText(parameter);
	}
	
	/**
	 * 
	 * Set check box either from bundle or from RCS settings if bundle is null
	 * 
	 * @param activity
	 *            the activity
	 * @param viewID
	 *            the view ID for the check box
	 * @param rcsSettingsKey
	 *            the key of the RCS parameter
	 * @param bundle
	 *            the bundle to save parameter
	 */
	public static void setCheckBoxParameter(final Activity activity, int viewID, String rcsSettingsKey, final Bundle bundle) {
		Boolean parameter = null;
		if (bundle != null && bundle.containsKey(rcsSettingsKey)) {
			parameter = bundle.getBoolean(rcsSettingsKey);
		} else {
			parameter = Boolean.parseBoolean(RcsSettings.getInstance().readParameter(rcsSettingsKey));
		}
		CheckBox box = (CheckBox) activity.findViewById(viewID);
		box.setChecked(parameter);
	}

	/**
	 * 
	 * Set spinner selection from bundle or from RCS settings if bundle is null
	 * 
	 * @param spinner
	 *            the spinner
	 * @param rcsSettingsKey
	 *            the key of the RCS parameter
	 * @param bundle
	 *            the bundle to save parameter
	 * @param selection
	 *            table of string representing choice selection
	 * @return the index of the spinner selection
	 */
	public static int setSpinnerParameter(final Spinner spinner, String rcsSettingsKey, final Bundle bundle,
			final String[] selection) {
		Integer parameter = null;
		if (bundle != null && bundle.containsKey(rcsSettingsKey)) {
			parameter = bundle.getInt(rcsSettingsKey);
		} else {
			String selected = RcsSettings.getInstance().readParameter(rcsSettingsKey);
			parameter = java.util.Arrays.asList(selection).indexOf(selected);
		}
		spinner.setSelection(parameter % selection.length);
		return parameter;
	}
	
	/**
	 * Save string either in bundle or in RCS settings if bundle is null
	 * 
	 * @param activity
	 *            the activity
	 * @param viewID
	 *            the view ID
	 * @param rcsSettingsKey
	 *            the key of the RCS parameter
	 * @param bundle
	 *            the bundle to save parameter
	 */
	public static void saveEditTextParameter(Activity activity, int viewID, String rcsSettingsKey, Bundle bundle) {
		EditText txt = (EditText) activity.findViewById(viewID);
		if (bundle != null) {
			bundle.putString(rcsSettingsKey, txt.getText().toString());
		} else {
			RcsSettings.getInstance().writeParameter(rcsSettingsKey, txt.getText().toString());
		}
	}
    
	/**
	 * Save boolean either in bundle or in RCS settings if bundle is null
	 * 
	 * @param activity
	 *            the activity
	 * @param viewID
	 *            the view ID
	 * @param rcsSettingsKey
	 *            the key of the RCS parameter
	 * @param bundle
	 *            the bundle to save parameter
	 */
	public static void saveCheckBoxParameter(Activity activity, int viewID, String rcsSettingsKey, Bundle bundle) {
		CheckBox box = (CheckBox) activity.findViewById(viewID);
		if (bundle != null) {
			bundle.putBoolean(rcsSettingsKey, box.isChecked());
		} else {
			RcsSettings.getInstance().writeBoolean(rcsSettingsKey, box.isChecked());
		}
	}
}