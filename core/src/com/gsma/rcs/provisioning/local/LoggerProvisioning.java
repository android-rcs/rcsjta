/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2016 Sony Mobile Communications Inc.
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
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provisioning.local;

import static com.gsma.rcs.provisioning.local.Provisioning.saveCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveStringEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setStringEditTextParam;

import com.gsma.rcs.R;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * End user profile parameters provisioning
 * 
 * @author jexa7410
 */
public class LoggerProvisioning extends Activity {
    /**
     * Trace level
     */
    private static final String[] TRACE_LEVEL = {
            "DEBUG", "INFO", "WARN", "ERROR", "FATAL"
    };
    private boolean mInFront;

    private RcsSettings mRcsSettings;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        // Set layout
        setContentView(R.layout.rcs_provisioning_logger);
        // Set buttons callback
        Button btn = (Button) findViewById(R.id.save_btn);
        btn.setOnClickListener(saveBtnListener);
        mRcsSettings = RcsSettings.getInstance(new LocalContentResolver(this));
        updateView(bundle);
        mInFront = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mInFront) {
            mInFront = true;
            // Update UI (from DB)
            updateView(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mInFront = false;
    }

    /**
     * Update view
     * 
     * @param bundle The bundle to save settings
     */
    private void updateView(Bundle bundle) {
        // Display parameters
        ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);

        setCheckBoxParam(R.id.TraceActivated, RcsSettingsData.TRACE_ACTIVATED, helper);
        setCheckBoxParam(R.id.SipTraceActivated, RcsSettingsData.SIP_TRACE_ACTIVATED, helper);
        setCheckBoxParam(R.id.MediaTraceActivated, RcsSettingsData.MEDIA_TRACE_ACTIVATED, helper);
        setStringEditTextParam(R.id.SipTraceFile, RcsSettingsData.SIP_TRACE_FILE, helper);

        Spinner spinner = (Spinner) findViewById(R.id.TraceLevel);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, TRACE_LEVEL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        Integer parameter;
        if (bundle != null && bundle.containsKey(RcsSettingsData.TRACE_LEVEL)) {
            parameter = bundle.getInt(RcsSettingsData.TRACE_LEVEL);
        } else {
            parameter = mRcsSettings.getTraceLevel();
        }
        spinner.setSelection(parameter);
    }

    /**
     * Save button listener
     */
    private OnClickListener saveBtnListener = new OnClickListener() {
        public void onClick(View v) {
            // Save parameters
            save();
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveInstanceState(outState);
    }

    /**
     * Save parameters either in bundle or in RCS settings
     */
    private void saveInstanceState(Bundle bundle) {
        ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);
        saveCheckBoxParam(R.id.TraceActivated, RcsSettingsData.TRACE_ACTIVATED, helper);
        saveCheckBoxParam(R.id.SipTraceActivated, RcsSettingsData.SIP_TRACE_ACTIVATED, helper);
        saveCheckBoxParam(R.id.MediaTraceActivated, RcsSettingsData.MEDIA_TRACE_ACTIVATED, helper);
        saveStringEditTextParam(R.id.SipTraceFile, RcsSettingsData.SIP_TRACE_FILE, helper);
        Spinner spinner = (Spinner) findViewById(R.id.TraceLevel);
        if (bundle != null) {
            bundle.putInt(RcsSettingsData.TRACE_LEVEL, spinner.getSelectedItemPosition());
        } else {
            Integer value = spinner.getSelectedItemPosition();
            mRcsSettings.writeInteger(RcsSettingsData.TRACE_LEVEL, value);
        }
    }

    /**
     * Save parameters
     */
    private void save() {
        saveInstanceState(null);
        Toast.makeText(this, getString(R.string.label_reboot_service), Toast.LENGTH_LONG).show();
    }
}
