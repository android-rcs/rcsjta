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

package com.orangelabs.rcs.ri.settings;

import com.gsma.services.rcs.RcsService.Build;
import com.gsma.services.rcs.RcsServiceControl;

import com.orangelabs.rcs.ri.R;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

/**
 * About the RI
 * 
 * @author Jean-Marc AUFFRET
 */
public class AboutSettings extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.app_about);

        String versionName, versionCode;
        try {
            PackageInfo infos = getPackageManager().getPackageInfo(
                    RcsServiceControl.RCS_STACK_PACKAGENAME, 0);
            versionName = infos.versionName;
            versionCode = getBuildNumber(infos.versionCode);
        } catch (NameNotFoundException e) {
            versionName = getString(R.string.label_api_unavailable);
            versionCode = getString(R.string.label_api_unavailable);
        }
        TextView releaseView = (TextView) findViewById(R.id.release);
        releaseView.setText(getString(R.string.label_about_release, versionName));
        TextView apiView = (TextView) findViewById(R.id.api);
        apiView.setText(getString(R.string.label_about_api, versionCode));

    }

    /**
     * Returns the string representation of the current build
     * 
     * @return String
     */
    private String getBuildNumber(int versionCode) {
        return new StringBuilder(Build.API_CODENAME).append(" ")
                .append(getGsmaVersion(versionCode)).toString();
    }

    /**
     * Returns the string representation of the supported GSMA version
     * 
     * @return String
     */
    private String getGsmaVersion(int versionCode) {
        switch (versionCode) {
            case Build.VERSION_CODES.BASE:
                return getString(R.string.rcs_settings_label_albatros_20);
            case Build.VERSION_CODES.BLACKBIRD:
                return getString(R.string.rcs_settings_label_blackbird_15);
            default:
                return getString(R.string.rcs_settings_label_unknown);
        }
    }
}
