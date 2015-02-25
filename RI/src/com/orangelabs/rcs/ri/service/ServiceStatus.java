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
import android.widget.TextView;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.orangelabs.rcs.ri.R;

/**
 * Display and monitor the service status
 * 
 * @author Jean-Marc AUFFRET
 */
public class ServiceStatus extends Activity implements RcsServiceListener {
    /**
     * Service API
     */
    private RcsService mApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.service_status);

        // Display service status by default
        displayServiceStatus(false);

        // Register service up event listener
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RcsService.ACTION_SERVICE_UP);
        registerReceiver(serviceUpListener, intentFilter);

        // Instantiate API
        mApi = new CapabilityService(getApplicationContext(), this);

        // Connect API
        mApi.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister service up event listener
        try {
            unregisterReceiver(serviceUpListener);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }

        // Disconnect API
        mApi.disconnect();
    }

    /**
     * Callback called when service is connected. This method is called when the service is well
     * connected to the RCS service (binding procedure successful): this means the methods of the
     * API may be used.
     */
    public void onServiceConnected() {
        // Display service status
        displayServiceStatus(true);
    }

    /**
     * Callback called when service has been disconnected. This method is called when the service is
     * disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see RcsService.Error
     */
    public void onServiceDisconnected(ReasonCode error) {
        // Display service status
        displayServiceStatus(false);
    }

    /**
     * Display service status
     * 
     * @param status Status
     */
    private void displayServiceStatus(boolean status) {
        TextView statusTxt = (TextView) findViewById(R.id.service_status);
        statusTxt.setText(String.valueOf(status));
    }

    /**
     * RCS service up event listener
     */
    private BroadcastReceiver serviceUpListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            // Retry a connection to the service
            mApi.connect();
        }
    };
}
