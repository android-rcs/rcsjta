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
package com.orangelabs.rcs.popup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Popup receiver
 * 
 * @author Jean-Marc AUFFRET
 */
public class PopupReceiver extends BroadcastReceiver {
	private static final String TAG = "Popup";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "Receive popup");
		
        // Display popup
		Intent popup = new Intent(intent);
		popup.setClass(context, ReceivePopup.class);
		popup.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		popup.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(popup);
    }
}
