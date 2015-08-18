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

package com.orangelabs.rcs.core.control.utils;

import com.orangelabs.rcs.core.control.R;

import android.app.Activity;
import android.app.AlertDialog;

/**
 * Utility functions
 * 
 * @author Jean-Marc AUFFRET
 */
public class MessageUtils {

    /**
     * Show an message
     * 
     * @param activity Activity
     * @param msg Message to be displayed
     * @return Dialog
     */
    public static AlertDialog showMessage(Activity activity, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(msg);
        builder.setTitle(activity.getString(R.string.title_msg));
        builder.setCancelable(false);
        builder.setPositiveButton(activity.getString(R.string.rcs_settings_label_ok), null);
        AlertDialog alert = builder.create();
        alert.show();
        return alert;
    }

}
