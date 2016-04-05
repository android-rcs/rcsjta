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

package com.gsma.rcs.ri;

import com.gsma.services.rcs.RcsService.Build;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

/**
 * About the RI
 * 
 * @author Jean-Marc AUFFRET
 */
public class AboutRI extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.app_about);

        // Display application release
        TextView releaseView = (TextView) findViewById(R.id.app_version);
        releaseView.setText(getString(R.string.label_about_app_version, getAppVersion()));

        // Display GSMA version
        TextView gsmaView = (TextView) findViewById(R.id.gsma_version);
        gsmaView.setText(getGsmaVersion());
    }

    /**
     * Returns the application version from manifest file
     *
     * @return Application version or null if not found
     */
    private String getAppVersion() {
        String version = null;
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = info.versionName + "." + info.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return version;
    }

    /**
     * Returns the GSMA version
     * 
     * @return String
     */
    private String getGsmaVersion() {
        StringBuilder version = new StringBuilder(Build.API_CODENAME);
        version.append(" ");
        switch (Build.API_VERSION) {
            case Build.VERSION_CODES.BASE:
                version.append("Albatros 2.0.");
                break;
            case Build.VERSION_CODES.BLACKBIRD:
                version.append("Blackbird 1.5.");
                break;
            case Build.VERSION_CODES.CPR:
                version.append("Crane Priority Release 1.6.");
                break;
            default:
                version.append("Unknown 0.0");
        }
        version.append(Build.API_INCREMENTAL);
        return version.toString();
    }
}
