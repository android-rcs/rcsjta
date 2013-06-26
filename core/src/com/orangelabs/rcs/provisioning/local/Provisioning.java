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

import android.app.TabActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.TabHost;

import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * Main
 * 
 * @author jexa7410
 */
public class Provisioning extends TabActivity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Instanciate the settings manager
        RcsSettings.createInstance(getApplicationContext());
        
		// Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set tabs
        final TabHost tabHost = getTabHost();
        tabHost.addTab(tabHost.newTabSpec("profile")
                .setIndicator("Profile", null)
                .setContent(new Intent(this, ProfileProvisioning.class)));
        tabHost.addTab(tabHost.newTabSpec("stack")
                .setIndicator("Stack", null)
                .setContent(new Intent(this, StackProvisioning.class)));
        tabHost.addTab(tabHost.newTabSpec("ui")
                .setIndicator("Service", null)
                .setContent(new Intent(this, ServiceProvisioning.class)));
        tabHost.addTab(tabHost.newTabSpec("logger")
                .setIndicator("Logger", null)
                .setContent(new Intent(this, LoggerProvisioning.class)));
    }
}