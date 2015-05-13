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

package com.gsma.rcs.provisioning.local;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;

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
        LocalContentResolver localContentResolver = new LocalContentResolver(
                getApplicationContext());
        AndroidFactory.setApplicationContext(this, RcsSettings.createInstance(localContentResolver));

        // Set tabs
        final TabHost tabHost = getTabHost();
        tabHost.addTab(tabHost.newTabSpec("profile").setIndicator("Profile", null)
                .setContent(new Intent(this, ProfileProvisioning.class)));
        tabHost.addTab(tabHost.newTabSpec("stack").setIndicator("Stack", null)
                .setContent(new Intent(this, StackProvisioning.class)));
        tabHost.addTab(tabHost.newTabSpec("ui").setIndicator("Service", null)
                .setContent(new Intent(this, ServiceProvisioning.class)));
        tabHost.addTab(tabHost.newTabSpec("logger").setIndicator("Logger", null)
                .setContent(new Intent(this, LoggerProvisioning.class)));
    }

    /**
     * Set edit text either from bundle or from RCS settings if bundle is null
     * 
     * @param viewID the view ID for the text edit
     * @param settingsKey the key of the RCS parameter
     * @param helper
     */
    /* package private */static void setEditTextParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        String parameter = null;
        Bundle bundle = helper.getBundle();
        if (bundle != null && bundle.containsKey(settingsKey)) {
            parameter = bundle.getString(settingsKey);
        } else {
            parameter = helper.getRcsSettings().readParameter(settingsKey);
        }
        EditText editText = (EditText) helper.getActivity().findViewById(viewID);
        editText.setText(parameter);
    }

    /**
     * Set check box either from bundle or from RCS settings if bundle is null
     * 
     * @param viewID the view ID for the check box
     * @param settingsKey the key of the RCS parameter
     * @param helper
     */
    /* package private */static void setCheckBoxParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        Boolean parameter = null;
        Bundle bundle = helper.getBundle();
        if (bundle != null && bundle.containsKey(settingsKey)) {
            parameter = bundle.getBoolean(settingsKey);
        } else {
            parameter = Boolean.parseBoolean(helper.getRcsSettings().readParameter(settingsKey));
        }
        CheckBox box = (CheckBox) helper.getActivity().findViewById(viewID);
        box.setChecked(parameter);
    }

    /**
     * Set spinner selection from bundle or from RCS settings if bundle is null
     * 
     * @param spinner the spinner
     * @param settingsKey the key of the RCS parameter
     * @param selection table of string representing choice selection
     * @param helper
     * @return the index of the spinner selection
     */
    /* package private */static int setSpinnerParameter(final Spinner spinner, String settingsKey,
            final String[] selection, ProvisioningHelper helper) {
        Integer parameter = null;
        Bundle bundle = helper.getBundle();
        if (bundle != null && bundle.containsKey(settingsKey)) {
            parameter = bundle.getInt(settingsKey);
        } else {
            String selected = helper.getRcsSettings().readParameter(settingsKey);
            parameter = java.util.Arrays.asList(selection).indexOf(selected);
        }
        spinner.setSelection(parameter % selection.length);
        return parameter;
    }

    /**
     * Save string either in bundle or in RCS settings if bundle is null
     * 
     * @param viewID the view ID
     * @param settingsKey the key of the RCS parameter
     * @param helper
     */
    /* package private */static void saveEditTextParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        EditText txt = (EditText) helper.getActivity().findViewById(viewID);
        Bundle bundle = helper.getBundle();
        if (bundle != null) {
            bundle.putString(settingsKey, txt.getText().toString());
        } else {
            helper.getRcsSettings().writeParameter(settingsKey, txt.getText().toString());
        }
    }

    /**
     * Save boolean either in bundle or in RCS settings if bundle is null
     * 
     * @param viewID the view ID
     * @param settingsKey the key of the RCS parameter
     * @param helper
     */
    /* package private */static void saveCheckBoxParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        CheckBox box = (CheckBox) helper.getActivity().findViewById(viewID);
        Bundle bundle = helper.getBundle();
        if (bundle != null) {
            bundle.putBoolean(settingsKey, box.isChecked());
        } else {
            helper.getRcsSettings().writeBoolean(settingsKey, box.isChecked());
        }
    }
}
