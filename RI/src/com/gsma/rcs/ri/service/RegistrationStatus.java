/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.service;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.RcsServiceRegistrationListener;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

/**
 * Display and monitor the registration status
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class RegistrationStatus extends RcsActivity {

    private final Handler mHandler = new Handler();

    private MyRegistrationListener registrationListener = new MyRegistrationListener();

    private static final String LOGTAG = LogUtils.getTag(RegistrationStatus.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.service_registration);

        // Display registration status by default
        displayRegistrationStatus(false);

        // Register to API connection manager
        if (!isServiceConnected(RcsServiceName.CAPABILITY)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.CAPABILITY);
        try {
            getCapabilityApi().addEventListener(registrationListener);
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isServiceConnected(RcsServiceName.CAPABILITY)) {
            try {
                getCapabilityApi().removeEventListener(registrationListener);
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isExiting()) {
            return;
        }
        try {
            displayRegistrationStatus(getCapabilityApi().isServiceRegistered());
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private class MyRegistrationListener extends RcsServiceRegistrationListener {
        // Service is registered to the network platform
        public void onServiceRegistered() {
            mHandler.post(new Runnable() {
                public void run() {
                    displayRegistrationStatus(true);
                }
            });
        }

        // Service is unregistered from the network platform
        public void onServiceUnregistered(RcsServiceRegistration.ReasonCode reason) {
            mHandler.post(new Runnable() {
                public void run() {
                    displayRegistrationStatus(false);
                }
            });
        }
    }

    private void displayRegistrationStatus(boolean status) {
        TextView statusTxt = (TextView) findViewById(R.id.registration_status);
        statusTxt.setText(String.valueOf(status));
    }
}
