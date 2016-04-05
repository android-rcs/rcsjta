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
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.capability.Capabilities;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * My capabilities
 */
public class MyCapabilities extends RcsActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_mine);

        /* Register to API connection manager */
        if (!isServiceConnected(RcsServiceName.CAPABILITY)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.CAPABILITY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isExiting()) {
            return;
        }
        try {
            // Get the current capabilities from the RCS contacts API
            Capabilities capabilities = getCapabilityApi().getMyCapabilities();
            // Set capabilities
            CheckBox imageCSh = (CheckBox) findViewById(R.id.image_sharing);
            imageCSh.setChecked(capabilities.hasCapabilities(Capabilities.CAPABILITY_IMAGE_SHARING));
            CheckBox videoCSh = (CheckBox) findViewById(R.id.video_sharing);
            videoCSh.setChecked(capabilities.hasCapabilities(Capabilities.CAPABILITY_VIDEO_SHARING));
            CheckBox ft = (CheckBox) findViewById(R.id.file_transfer);
            ft.setChecked(capabilities.hasCapabilities(Capabilities.CAPABILITY_FILE_TRANSFER));
            CheckBox im = (CheckBox) findViewById(R.id.im);
            im.setChecked(capabilities.hasCapabilities(Capabilities.CAPABILITY_IM));
            CheckBox geolocationPush = (CheckBox) findViewById(R.id.geoloc_push);
            geolocationPush.setChecked(capabilities
                    .hasCapabilities(Capabilities.CAPABILITY_GEOLOC_PUSH));
            TextView extensions = (TextView) findViewById(R.id.extensions);
            extensions.setText(RequestCapabilities.getExtensions(capabilities));
            CheckBox automata = (CheckBox) findViewById(R.id.automata);
            automata.setChecked(capabilities.isAutomata());

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

}
