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
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData;
import com.orangelabs.rcs.utils.logger.Logger;

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
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.rcs_provisioning_logger);
        
		// Set buttons callback
        Button btn = (Button)findViewById(R.id.save_btn);
        btn.setOnClickListener(saveBtnListener);        
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
        // Display parameters
    	CheckBox check = (CheckBox)this.findViewById(R.id.TraceActivated);
        check.setChecked(RcsSettings.getInstance().isTraceActivated());
        
    	check = (CheckBox)this.findViewById(R.id.SipTraceActivated);
        check.setChecked(RcsSettings.getInstance().isSipTraceActivated());

		check = (CheckBox)this.findViewById(R.id.MediaTraceActivated);
        check.setChecked(RcsSettings.getInstance().isMediaTraceActivated());

		EditText txt = (EditText)this.findViewById(R.id.SipTraceFile);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.SIP_TRACE_FILE));

		Spinner spinner = (Spinner)findViewById(R.id.TraceLevel);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, TRACE_LEVEL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int level = RcsSettings.getInstance().getTraceLevel();
        if (level == Logger.DEBUG_LEVEL) {
            spinner.setSelection(0);
        } else
        if (level == Logger.INFO_LEVEL) {
            spinner.setSelection(1);
        } else
        if (level == Logger.WARN_LEVEL) {
            spinner.setSelection(2);
        } else
        if (level == Logger.ERROR_LEVEL) {
            spinner.setSelection(3);
        } else
        if (level == Logger.FATAL_LEVEL) {
            spinner.setSelection(4);
        }
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
    
    /**
     * Save parameters
     */
    private void save() {	
        CheckBox check = (CheckBox)this.findViewById(R.id.TraceActivated);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.TRACE_ACTIVATED, Boolean.toString(check.isChecked()));

        check = (CheckBox)this.findViewById(R.id.SipTraceActivated);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_TRACE_ACTIVATED, Boolean.toString(check.isChecked()));

		check = (CheckBox)this.findViewById(R.id.MediaTraceActivated);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.MEDIA_TRACE_ACTIVATED, Boolean.toString(check.isChecked()));

		Spinner spinner = (Spinner)findViewById(R.id.TraceLevel);
		String value = (String)spinner.getSelectedItem();
        int level = Logger.ERROR_LEVEL;
        if (value.equals(TRACE_LEVEL[0])) {
            level = Logger.DEBUG_LEVEL;
        } else
        if (value.equals(TRACE_LEVEL[1])) {
            level = Logger.INFO_LEVEL;
        } else
        if (value.equals(TRACE_LEVEL[2])) {
            level = Logger.WARN_LEVEL;
        } else
        if (value.equals(TRACE_LEVEL[3])) {
            level = Logger.ERROR_LEVEL;
        } else
        if (value.equals(TRACE_LEVEL[4])) {
            level = Logger.FATAL_LEVEL;
        }
		RcsSettings.getInstance().writeParameter(RcsSettingsData.TRACE_LEVEL, ""+level);

		EditText txt = (EditText)this.findViewById(R.id.SipTraceFile);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_TRACE_FILE, txt.getText().toString());

		Toast.makeText(this, getString(R.string.label_reboot_service), Toast.LENGTH_LONG).show();				
	}
}
