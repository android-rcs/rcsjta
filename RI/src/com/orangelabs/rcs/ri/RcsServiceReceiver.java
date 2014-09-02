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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * RCS service receiver : trap the intent SERVICE_UP
 * 
 * @author YPLO6403
 * 
 */
public class RcsServiceReceiver extends BroadcastReceiver {

	/**
	 * Action Service is UP
	 */
	/* package private */static final String ACTION_SERVICE_UP = "ACTION_SERVICE_UP";

	@Override
	public void onReceive(Context context, Intent intent) {
		// Send intent to service
		Intent receiverIntent = new Intent(context, RcsServiceIntentService.class);
		receiverIntent.putExtras(intent);
		receiverIntent.setAction(ACTION_SERVICE_UP);
		context.startService(receiverIntent);
	}

}
