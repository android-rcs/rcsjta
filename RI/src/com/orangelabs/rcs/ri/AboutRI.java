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

package com.orangelabs.rcs.ri;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.TextView;

import com.gsma.services.rcs.RcsService.Build;
import com.orangelabs.rcs.ri.utils.Utils;

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
        
        // Display release number
        String appRelease = Utils.getApplicationVersion(this); 
        TextView releaseView = (TextView)findViewById(R.id.release);
        releaseView.setText(getString(R.string.label_about_release, appRelease));

        String apiRelease = getBuildNumber() + "/" + getGsmaVersion();
        TextView apiView = (TextView)findViewById(R.id.api);
        apiView.setText(getString(R.string.label_about_api, apiRelease));
    }

	/**
	 * Returns the string representation of the current build
	 * 
	 * @return String
	 */
	private static String getBuildNumber() {
		return Build.API_CODENAME + " " + Build.API_VERSION + "." + Build.API_INCREMENTAL;
	}
    
	/**
	 * Returns the string representation of the supported GSMA version
	 * 
	 * @return String
	 */
	private static String getGsmaVersion() {
		switch(Build.API_VERSION) {
			case Build.VERSION_CODES.BASE:
				return "Albatros";
			case Build.VERSION_CODES.BLACKBIRD:
				return "Blackbird";
			default:
				return "Unknown";
		}
	}
}
