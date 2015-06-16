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

package com.orangelabs.rcs.ri.capabilities;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.capability.Capabilities;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * My capabilities
 */
public class MyCapabilities extends Activity {

    /**
     * API connection manager
     */
    private ConnectionManager mCnxManager;

    /**
     * A locker to exit only once
     */
    private LockAccess mExitOnce = new LockAccess();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_mine);

        // Register to API connection manager
        mCnxManager = ConnectionManager.getInstance();
        if (!mCnxManager.isServiceConnected(RcsServiceName.CAPABILITY)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
            return;
        }
        mCnxManager.startMonitorServices(this, null, RcsServiceName.CAPABILITY);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCnxManager.stopMonitorServices(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Get the current capabilities from the RCS contacts API
            Capabilities capabilities = mCnxManager.getCapabilityApi().getMyCapabilities();

            // Set capabilities
            CheckBox imageCSh = (CheckBox) findViewById(R.id.image_sharing);
            imageCSh.setChecked(capabilities.isImageSharingSupported());
            CheckBox videoCSh = (CheckBox) findViewById(R.id.video_sharing);
            videoCSh.setChecked(capabilities.isVideoSharingSupported());
            CheckBox ft = (CheckBox) findViewById(R.id.file_transfer);
            ft.setChecked(capabilities.isFileTransferSupported());
            CheckBox im = (CheckBox) findViewById(R.id.im);
            im.setChecked(capabilities.isImSessionSupported());
            CheckBox geolocationPush = (CheckBox) findViewById(R.id.geoloc_push);
            geolocationPush.setChecked(capabilities.isGeolocPushSupported());

            // Set extensions
            TextView extensions = (TextView) findViewById(R.id.extensions);
            extensions.setText(RequestCapabilities.getExtensions(capabilities));

            // Set automata
            CheckBox automata = (CheckBox) findViewById(R.id.automata);
            automata.setChecked(capabilities.isAutomata());

        } catch (RcsServiceNotAvailableException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_unavailable), mExitOnce, e);
        } catch (RcsServiceException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
        }
    }

}
