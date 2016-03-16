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

package com.gsma.rcs.core.control.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Control Core receiver
 * 
 * @author YPLO6403
 */
public class ControlCoreReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("[RI]", "Broadcast is received");
        Intent displaySettings = new Intent(context, SettingsDisplay.class);
        displaySettings.putExtras(intent);
        displaySettings.setAction(intent.getAction());
        displaySettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(displaySettings);
    }

}
