/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications AB.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provisioning.local;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.services.rcs.contact.ContactId;

import android.app.TabActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;

/**
 * Main
 * 
 * @author jexa7410
 * @author Philippe LEMORDANT
 */
@SuppressWarnings("deprecation")
public class Provisioning extends TabActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalContentResolver localContentResolver = new LocalContentResolver(
                getApplicationContext());
        AndroidFactory
                .setApplicationContext(this, RcsSettings.createInstance(localContentResolver));

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
     * @param helper the provisioning helper
     */
    /* package private */static void setStringEditTextParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        String parameter;
        Bundle bundle = helper.getBundle();
        if (bundle != null && bundle.containsKey(settingsKey)) {
            parameter = bundle.getString(settingsKey);
        } else {
            parameter = helper.getRcsSettings().readString((settingsKey));
        }
        EditText editText = (EditText) helper.getActivity().findViewById(viewID);
        editText.setText(parameter == null ? "" : parameter);
    }

    /**
     * Set edit text either from bundle or from RCS settings if bundle is null
     * 
     * @param viewID the view ID for the text edit
     * @param settingsKey the key of the RCS parameter
     * @param helper the provisioning helper
     */
    /* package private */static void setIntegerEditTextParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        String parameter;
        Bundle bundle = helper.getBundle();
        if (bundle != null && bundle.containsKey(settingsKey)) {
            parameter = bundle.getString(settingsKey);
        } else {
            parameter = Integer.toString(helper.getRcsSettings().readInteger((settingsKey)));
        }
        EditText editText = (EditText) helper.getActivity().findViewById(viewID);
        editText.setText(parameter);
    }

    /**
     * Set edit text either from bundle or from RCS settings if bundle is null
     * 
     * @param viewID the view ID for the text edit
     * @param settingsKey the key of the RCS parameter
     * @param helper the provisioning helper
     */
    /* package private */static void setLongEditTextParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        String parameter;
        Bundle bundle = helper.getBundle();
        if (bundle != null && bundle.containsKey(settingsKey)) {
            parameter = bundle.getString(settingsKey);
        } else {
            parameter = Long.toString(helper.getRcsSettings().readLong((settingsKey)));
        }
        EditText editText = (EditText) helper.getActivity().findViewById(viewID);
        editText.setText(parameter);
    }

    /**
     * Set edit text either from bundle or from RCS settings if bundle is null
     * 
     * @param viewID the view ID for the text edit
     * @param settingsKey the key of the RCS parameter
     * @param helper the provisioning helper
     */
    /* package private */static void setUriEditTextParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        String parameter;
        Bundle bundle = helper.getBundle();
        if (bundle != null && bundle.containsKey(settingsKey)) {
            parameter = bundle.getString(settingsKey);
        } else {
            Uri dbValue = helper.getRcsSettings().readUri(settingsKey);
            parameter = (dbValue == null ? "" : dbValue.toString());
        }
        EditText editText = (EditText) helper.getActivity().findViewById(viewID);
        editText.setText(parameter);
    }

    /**
     * Set edit text either from bundle or from RCS settings if bundle is null
     * 
     * @param viewID the view ID for the text edit
     * @param settingsKey the key of the RCS parameter
     * @param helper the provisioning helper
     */
    /* package private */static void setContactIdEditTextParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        String parameter;
        Bundle bundle = helper.getBundle();
        if (bundle != null && bundle.containsKey(settingsKey)) {
            parameter = bundle.getString(settingsKey);
        } else {
            ContactId dbValue = helper.getRcsSettings().readContactId(settingsKey);
            parameter = (dbValue == null ? "" : dbValue.toString());
        }
        EditText editText = (EditText) helper.getActivity().findViewById(viewID);
        editText.setText(parameter);
    }

    /**
     * Set check box either from bundle or from RCS settings if bundle is null
     * 
     * @param viewID the view ID for the check box
     * @param settingsKey the key of the RCS parameter
     * @param helper the provisioning helper
     */
    /* package private */static void setCheckBoxParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        Boolean parameter;
        Bundle bundle = helper.getBundle();
        if (bundle != null && bundle.containsKey(settingsKey)) {
            parameter = bundle.getBoolean(settingsKey);
        } else {
            parameter = helper.getRcsSettings().readBoolean((settingsKey));
        }
        CheckBox box = (CheckBox) helper.getActivity().findViewById(viewID);
        box.setChecked(parameter);
    }

    /**
     * Set spinner selection from bundle or from RCS settings if bundle is null
     * 
     * @param spinner the spinner
     * @param settingsKey the key of the RCS parameter
     * @param isSettingInteger True is setting value is of integer type
     * @param selection table of string representing choice selection
     * @param helper the provisioning helper
     * @return the index of the spinner selection
     */
    /* package private */static int setSpinnerParameter(final Spinner spinner, String settingsKey,
            boolean isSettingInteger, final String[] selection, ProvisioningHelper helper) {
        Integer parameter;
        Bundle bundle = helper.getBundle();
        if (bundle != null && bundle.containsKey(settingsKey)) {
            parameter = bundle.getInt(settingsKey);
        } else {
            if (isSettingInteger) {
                parameter = helper.getRcsSettings().readInteger(settingsKey);
            } else {
                String selected = helper.getRcsSettings().readString(settingsKey);
                parameter = java.util.Arrays.asList(selection).indexOf(selected);
            }
        }
        spinner.setSelection(parameter % selection.length);
        return parameter;
    }

    /**
     * Save string either in bundle or in RCS settings if bundle is null
     * 
     * @param viewID the view ID
     * @param settingsKey the key of the RCS parameter
     * @param helper the provisioning helper
     */
    /* package private */static void saveStringEditTextParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        EditText editText = (EditText) helper.getActivity().findViewById(viewID);
        Bundle bundle = helper.getBundle();
        String text = editText.getText().toString().trim();
        if (bundle != null) {
            bundle.putString(settingsKey, text);
        } else {
            helper.getRcsSettings().writeString(settingsKey, "".equals(text) ? null : text);
        }
    }

    /**
     * Save string either in bundle or in RCS settings if bundle is null
     * 
     * @param viewID the view ID
     * @param settingsKey the key of the RCS parameter
     * @param helper the provisioning helper
     */
    /* package private */static void saveContactIdEditTextParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        EditText txt = (EditText) helper.getActivity().findViewById(viewID);
        Bundle bundle = helper.getBundle();
        if (bundle != null) {
            bundle.putString(settingsKey, txt.getText().toString());
        } else {
            String text = txt.getText().toString();
            PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(text);
            helper.getRcsSettings().writeContactId(settingsKey,
                    "".equals(text) ? null : ContactUtil.createContactIdFromValidatedData(number));
        }
    }

    /**
     * Save integer either in bundle or in RCS settings if bundle is null
     * 
     * @param viewID the view ID
     * @param settingsKey the key of the RCS parameter
     * @param helper the provisioning helper
     */
    /* package private */static void saveIntegerEditTextParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        EditText txt = (EditText) helper.getActivity().findViewById(viewID);
        Bundle bundle = helper.getBundle();
        if (bundle != null) {
            bundle.putString(settingsKey, txt.getText().toString());
        } else {
            helper.getRcsSettings().writeInteger(settingsKey,
                    Integer.parseInt(txt.getText().toString()));
        }
    }

    /**
     * Save long either in bundle or in RCS settings if bundle is null
     * 
     * @param viewID the view ID
     * @param settingsKey the key of the RCS parameter
     * @param helper the provisioning helper
     */
    /* package private */static void saveLongEditTextParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        EditText txt = (EditText) helper.getActivity().findViewById(viewID);
        Bundle bundle = helper.getBundle();
        if (bundle != null) {
            bundle.putString(settingsKey, txt.getText().toString());
        } else {
            helper.getRcsSettings()
                    .writeLong(settingsKey, Long.parseLong(txt.getText().toString()));
        }
    }

    /**
     * Save uri either in bundle or in RCS settings if bundle is null
     * 
     * @param viewID the view ID
     * @param settingsKey the key of the RCS parameter
     * @param helper the provisioning helper
     */
    /* package private */static void saveUriEditTextParam(int viewID, String settingsKey,
            ProvisioningHelper helper) {
        EditText txt = (EditText) helper.getActivity().findViewById(viewID);
        Bundle bundle = helper.getBundle();
        if (bundle != null) {
            bundle.putString(settingsKey, txt.getText().toString());
        } else {
            String text = txt.getText().toString();
            helper.getRcsSettings().writeUri(settingsKey, "".equals(text) ? null : Uri.parse(text));
        }
    }

    /**
     * Save boolean either in bundle or in RCS settings if bundle is null
     * 
     * @param viewID the view ID
     * @param settingsKey the key of the RCS parameter
     * @param helper the provisioning helper
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
