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

package com.orangelabs.rcs.settings;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.utils.AppUtils;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * About the application display
 * 
 * @author jexa7410
 */
public class AboutDisplay extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set title
        setTitle(R.string.rcs_settings_title_about);
        setContentView(R.layout.rcs_settings_about_layout);

        // Set the release number
        TextView releaseView = (TextView) findViewById(R.id.settings_label_release);
        String relLabel = getString(R.string.rcs_settings_label_release);
        String relNumber = AppUtils.getApplicationVersion(this);
        releaseView.setText(relLabel + " " + relNumber);
    }
}
