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
package com.gsma.services.rcs;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/**
 * Utility functions which permit to detect other rcs client installed on a
 * device
 * 
 * @author Jean-Marc AUFFRET
 */
public class RcsUtils {
	/**
	 * Returns the list of rcs clients installed on the device. An application
	 * is identified as a rcs client by including an intent filter with the
	 * ACTION_CLIENT_SETTINGS action in the Manifest.xml of the application.
	 * 
	 * @param context Application context
	 * @return List of application info
	 */
	public static List<ResolveInfo> getRcsClients(Context context) {
		Intent intent = new Intent(Intents.Client.ACTION_VIEW_SETTINGS);
		List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list;
	}

	/**
	 * Is a particular rcs client activated. The result is returned
	 * asynchronously via a broadcast receiver.
	 * 
	 * @param context Application context
	 * @param appInfo Application info
	 * @param receiverResult Broadcast receiver result
	 */
	public static void isRcsClientActivated(Context context, ResolveInfo appInfo, BroadcastReceiver receiverResult) {
		Intent broadcastIntent = new Intent(appInfo.activityInfo.packageName
				+ Intents.Client.ACTION_CLIENT_GET_STATUS);
		context.sendOrderedBroadcast(broadcastIntent, null, receiverResult,	null, Activity.RESULT_OK, null, null);
	}

	/**
	 * Load the settings activity of a particular rcs client to enable or
	 * disable the client
	 * 
	 * @param context Application context
	 * @param appInfo Application info
	 */
	public static void loadRcsClientSettings(Context context, ResolveInfo appInfo) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setComponent(new ComponentName(appInfo.activityInfo.packageName,	appInfo.activityInfo.name));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		context.startActivity(intent);
	}
}
