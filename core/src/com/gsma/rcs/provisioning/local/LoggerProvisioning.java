/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

import com.gsma.rcs.R;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.utils.logger.Logger;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * Logger provisioning fragment
 * 
 * @author jexa7410
 */
public class LoggerProvisioning extends Fragment implements IProvisioningFragment {

    private static final Logger sLogger = Logger.getLogger(LoggerProvisioning.class.getName());

    private static final String[] TRACE_LEVEL = {
            "DEBUG", "INFO", "WARN", "ERROR", "FATAL"
    };

    private static RcsSettings sRcsSettings;
    private View mRootView;
    private ProvisioningHelper mHelper;

    public static LoggerProvisioning newInstance(RcsSettings rcsSettings) {
        if (sLogger.isActivated()) {
            sLogger.debug("new instance");
        }
        LoggerProvisioning f = new LoggerProvisioning();
        /*
         * If Android decides to recreate your Fragment later, it's going to call the no-argument
         * constructor of your fragment. So overloading the constructor is not a solution. A way to
         * pass argument to new fragment is to store it as static.
         */
        sRcsSettings = rcsSettings;
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.provisioning_logger, container, false);
        mHelper = new ProvisioningHelper(mRootView, sRcsSettings);
        displayRcsSettings();
        return mRootView;
    }

    @Override
    public void displayRcsSettings() {
        if (sLogger.isActivated()) {
            sLogger.debug("displayRcsSettings");
        }
        mHelper.setBoolCheckBox(R.id.TraceActivated, RcsSettingsData.TRACE_ACTIVATED);
        mHelper.setBoolCheckBox(R.id.SipTraceActivated, RcsSettingsData.SIP_TRACE_ACTIVATED);
        mHelper.setBoolCheckBox(R.id.MediaTraceActivated, RcsSettingsData.MEDIA_TRACE_ACTIVATED);
        mHelper.setStringEditText(R.id.SipTraceFile, RcsSettingsData.SIP_TRACE_FILE);

        Spinner spinner = (Spinner) mRootView.findViewById(R.id.TraceLevel);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, TRACE_LEVEL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(sRcsSettings.getTraceLevel());
    }

    @Override
    public void persistRcsSettings() {
        if (sLogger.isActivated()) {
            sLogger.debug("persistRcsSettings");
        }
        mHelper.saveBoolCheckBox(R.id.TraceActivated, RcsSettingsData.TRACE_ACTIVATED);
        mHelper.saveBoolCheckBox(R.id.SipTraceActivated, RcsSettingsData.SIP_TRACE_ACTIVATED);
        mHelper.saveBoolCheckBox(R.id.MediaTraceActivated, RcsSettingsData.MEDIA_TRACE_ACTIVATED);
        mHelper.saveStringEditText(R.id.SipTraceFile, RcsSettingsData.SIP_TRACE_FILE);
        Spinner spinner = (Spinner) mRootView.findViewById(R.id.TraceLevel);
        sRcsSettings.writeInteger(RcsSettingsData.TRACE_LEVEL, spinner.getSelectedItemPosition());
    }

}
