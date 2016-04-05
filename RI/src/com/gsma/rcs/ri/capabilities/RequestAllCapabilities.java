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

package com.gsma.rcs.ri.capabilities;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Request capabilities of all contacts
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class RequestAllCapabilities extends RcsActivity {

    private OnClickListener mBtnSyncListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intialize();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_refresh);

        /* Set buttons callback */
        Button refreshBtn = (Button) findViewById(R.id.refresh_btn);
        refreshBtn.setOnClickListener(mBtnSyncListener);

        /* Register to API connection manager */
        if (!isServiceConnected(RcsServiceName.CAPABILITY)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.CAPABILITY);
    }

    private void intialize() {
        mBtnSyncListener = new OnClickListener() {
            public void onClick(View v) {
                try {
                    /* Check if the service is available */
                    try {
                        if (!getCapabilityApi().isServiceRegistered()) {
                            showMessage(R.string.error_not_registered);
                            return;
                        }
                    } catch (RcsServiceNotAvailableException e) {
                        showMessage(R.string.label_service_not_available);
                        return;
                    }

                    /* Refresh all contacts */
                    getCapabilityApi().requestAllContactsCapabilities();

                    /* Display message */
                    Utils.displayLongToast(RequestAllCapabilities.this,
                            getString(R.string.label_refresh_success));

                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
            }
        };
    }

}
