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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;

import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * A class to display RCS service availability and activate/deactivate RCS stack
 */
public class ServiceActivation extends Activity {

    private RcsServiceControl mRcsServiceControl;
    private boolean mIsActivated;
    private boolean mIsActivationChangeable;
    private CheckBox mActivationModeCheckBox;
    private TextView mStackAvailabilityTxt;
    private RcsServiceReceiver mRcsServiceReceiver;

    /**
     * A locker to exit only once
     */
    private LockAccess mExitOnce = new LockAccess();

    /**
     * The log tag for this class
     */
    private static final String LOGTAG = LogUtils.getTag(ServiceActivation.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.service_activation);

        mRcsServiceControl = RcsServiceControl.getInstance(this);
        mActivationModeCheckBox = (CheckBox) findViewById(R.id.rcs_stack_activate);
        mStackAvailabilityTxt = (TextView) findViewById(R.id.rcs_stack_availability);
        mActivationModeCheckBox.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                boolean activate = mActivationModeCheckBox.isChecked();
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "RCS service activation: ".concat(String.valueOf(activate)));
                }
                try {
                    mRcsServiceControl.setActivationMode(mActivationModeCheckBox.isChecked());
                    displayRcsStackServiceStates();
                } catch (RcsPermissionDeniedException e) {
                    Utils.showMessageAndExit(ServiceActivation.this,
                            getString(R.string.text_service_activate_unchangeable),
                            mExitOnce, e);
                }
            }

        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        displayRcsStackServiceStates();
        if (mRcsServiceReceiver != null) {
            return;
        }
        // Register the broadcast receiver to catch ACTION_SERVICE_UP
        IntentFilter filter = new IntentFilter();
        filter.addAction(RcsService.ACTION_SERVICE_UP);
        mRcsServiceReceiver = new RcsServiceReceiver();
        registerReceiver(mRcsServiceReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRcsServiceReceiver == null) {
            return;
        }
        try {
            unregisterReceiver(mRcsServiceReceiver);
        } catch (IllegalArgumentException e) {
            // Do nothing
        }
        mRcsServiceReceiver = null;
    }

    private void displayRcsStackServiceStates() {
        boolean rcsStackAvailable = mRcsServiceControl.isAvailable();
        mStackAvailabilityTxt
                .setText(getString(rcsStackAvailable ? R.string.label_rcs_stack_available
                        : R.string.label_rcs_stack_not_available));
        if (!rcsStackAvailable) {
            mActivationModeCheckBox.setVisibility(View.INVISIBLE);
            return;
        } else {
            mActivationModeCheckBox.setVisibility(View.VISIBLE);
        }
        try {
            mIsActivationChangeable = mRcsServiceControl.isActivationModeChangeable();
            mIsActivated = mRcsServiceControl.isActivated();
            if (mIsActivationChangeable) {
                mActivationModeCheckBox.setEnabled(true);
                mActivationModeCheckBox.setChecked(mIsActivated);
                if (mIsActivated) {
                    mActivationModeCheckBox.setText(getString(R.string.label_service_deactivate));
                } else {
                    mActivationModeCheckBox.setText(getString(R.string.label_service_activate));
                }
            } else {
                mActivationModeCheckBox.setEnabled(false);
                mActivationModeCheckBox.setChecked(mIsActivated);
                mActivationModeCheckBox
                        .setText(getString(R.string.text_service_activate_unchangeable));
            }
            mActivationModeCheckBox.setChecked(mIsActivated);
        } catch (RcsServiceException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
        }
    }

    /**
     * A broadcast receiver to catch ACTION_SERVICE_UP from the RCS stack
     */
    private class RcsServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null
                    && intent.getAction().equals(RcsService.ACTION_SERVICE_UP)) {
                Utils.showMessageAndExit(ServiceActivation.this,
                        getString(R.string.label_service_is_up));
            }
        }
    }
}
