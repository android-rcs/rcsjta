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

package com.orangelabs.rcs.ri.service;

import com.gsma.services.rcs.CommonServiceConfiguration;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMethod;
import com.gsma.services.rcs.CommonServiceConfiguration.MinimumBatteryLevel;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;

import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.LogUtils;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/**
 * Service configuration
 * 
 * @author LEMORDANT Philippe
 */
public class ServiceConfigurationActivity extends RcsActivity {

    private static final String LOGTAG = LogUtils.getTag(ServiceConfigurationActivity.class
            .getSimpleName());

    private Spinner mSpinnerDefMessaginMethod;
    private Spinner mSpinnerMinBatteryLevel;
    private TextView mTextEditDisplayName;
    private CheckBox mCheckBoxIsConfigValid;
    private TextView mTextEditMessagingUX;
    private TextView mTextEditContactId;
    private TextView mTextRcsServiceActivation;

    private CommonServiceConfiguration mConfiguration;

    private String mIntialDisplayName;

    private RcsServiceControl mRcsServiceControl;

    private static SparseArray<MinimumBatteryLevel> sPosToMinimumBatteryLevel = new SparseArray<>();
    private static Map<MinimumBatteryLevel, Integer> sMinimumBatteryLevelToPos = new HashMap<>();
    static {
        int order = 0;
        for (MinimumBatteryLevel entry : MinimumBatteryLevel.values()) {
            sPosToMinimumBatteryLevel.put(order, entry);
            sMinimumBatteryLevelToPos.put(entry, order++);
        }
    }

    private MinimumBatteryLevel getMinimumBatteryLevelFromSpinnerPosition(int position) {
        return sPosToMinimumBatteryLevel.get(position);
    }

    private int getSpinnerPositionFromMinimumBatteryLevel(MinimumBatteryLevel level) {
        return sMinimumBatteryLevelToPos.get(level);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRcsServiceControl = RiApplication.getRcsServiceControl();

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.service_configuration);

        mCheckBoxIsConfigValid = (CheckBox) findViewById(R.id.label_service_configuration_valid);
        mTextEditMessagingUX = (TextView) findViewById(R.id.label_messaging_mode);
        mTextEditContactId = (TextView) findViewById(R.id.label_my_contact_id);
        mTextEditDisplayName = (TextView) findViewById(R.id.text_my_display_name);
        mTextRcsServiceActivation = (TextView) findViewById(R.id.text_service_activation);

        /* Register to API connection manager */
        if (!isServiceConnected(RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.CONTACT);
        try {
            mConfiguration = getContactApi().getCommonConfiguration();
            mIntialDisplayName = mConfiguration.getMyDisplayName();
            if (mIntialDisplayName == null) {
                mIntialDisplayName = "";
            }
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return;
        }

        String[] messagingMethods = getResources().getStringArray(R.array.messaging_method);
        mSpinnerDefMessaginMethod = (Spinner) findViewById(R.id.spinner_default_messaging_method);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, messagingMethods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerDefMessaginMethod.setAdapter(adapter);

        mSpinnerDefMessaginMethod.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                    int position, long id) {
                MessagingMethod method = MessagingMethod.valueOf(mSpinnerDefMessaginMethod
                        .getSelectedItemPosition());
                try {
                    MessagingMethod oldMethod = mConfiguration.getDefaultMessagingMethod();
                    if (!oldMethod.equals(method)) {
                        mConfiguration.setDefaultMessagingMethod(method);
                        if (LogUtils.isActive) {
                            Log.d(LOGTAG,
                                    "onClick DefaultMessagingMethod ".concat(method.toString()));
                        }
                    }
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        String[] batteryLevels = getResources().getStringArray(R.array.minimum_battery_level);
        mSpinnerMinBatteryLevel = (Spinner) findViewById(R.id.spinner_label_min_battery_level);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, batteryLevels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerMinBatteryLevel.setAdapter(adapter);

        mSpinnerMinBatteryLevel.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                    int position, long id) {
                MinimumBatteryLevel level = getMinimumBatteryLevelFromSpinnerPosition(mSpinnerMinBatteryLevel
                        .getSelectedItemPosition());
                try {
                    MinimumBatteryLevel oldLevel = mConfiguration.getMinimumBatteryLevel();
                    if (!oldLevel.equals(level)) {
                        mConfiguration.setMinimumBatteryLevel(level);
                        if (LogUtils.isActive) {
                            Log.d(LOGTAG, "onClick MinimumBatteryLevel ".concat(level.toString()));
                        }
                    }
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        startMonitorServices(RcsServiceName.CONTACT);
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onCreate");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        String newDisplayName = mTextEditDisplayName.getText().toString();
        if (mIntialDisplayName != null && !mIntialDisplayName.equals(newDisplayName)) {
            setDisplayName(newDisplayName);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isExiting()) {
            return;
        }
        displayServiceConfiguration();
    }

    private void displayServiceConfiguration() {
        try {
            mCheckBoxIsConfigValid.setChecked(mConfiguration.isConfigValid());
            mSpinnerDefMessaginMethod.setSelection(mConfiguration.getDefaultMessagingMethod()
                    .toInt());
            mSpinnerMinBatteryLevel
                    .setSelection(getSpinnerPositionFromMinimumBatteryLevel(mConfiguration
                            .getMinimumBatteryLevel()));
            mTextEditMessagingUX.setText(mConfiguration.getMessagingUX().name());
            mTextEditDisplayName.setText(mConfiguration.getMyDisplayName());
            mTextEditContactId.setText(mConfiguration.getMyContactId().toString());
            boolean rcsServiceActivationchangeable = mRcsServiceControl
                    .isActivationModeChangeable();
            if (rcsServiceActivationchangeable) {
                mTextRcsServiceActivation
                        .setText(getString(R.string.label_service_activate_changeable));
            } else {
                mTextRcsServiceActivation
                        .setText(getString(R.string.label_service_activate_unchangeable));
            }
        } catch (RcsServiceException e) {
            showException(e);
        }
    }

    private void setDisplayName(String newDisplayName) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Refresh display name to ".concat(newDisplayName));
        }
        try {
            mConfiguration.setMyDisplayName(newDisplayName);
        } catch (RcsServiceException e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Exception occurred", e);
            }
        }
    }
}
