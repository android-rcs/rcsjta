/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
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
 * Utility functions which permit to detect other rcs services installed on a
 * device
 * 
 * @author Jean-Marc AUFFRET
 */
public class RcsUtils {
	/**
	 * Returns the list of RCS services installed on the device (except myself).
	 * An application is identified as a RCS service by including an intent
	 * filter with the ACTION_VIEW_SETTINGS action in the Manifest.xml of the
	 * application (eg. Android service).
	 * 
	 * @param context Application context
	 * @return List of application info
	 */
	public static List<ResolveInfo> getRcsServices(Context context) {
		Intent intent = new Intent(Intents.Service.ACTION_VIEW_SETTINGS);
		List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list;
	}

	/**
	 * Detects if a particular rcs service is activated. The result is returned
	 * asynchronously via a broadcast receiver. The rcs service is identified
	 * by the object ResolveInfo recovered via the method getRcsServices. Each
	 * rcs service should implement a BroadcastReceiver with the following
	 * Intent filter in order to return its current status.
	 * 
	 * @param context Application context
	 * @param appInfo Application info
	 * @param receiverResult Broadcast receiver result
	 */
	public static void isRcsServiceActivated(Context context, ResolveInfo appInfo, BroadcastReceiver receiverResult) {
		Intent broadcastIntent = new Intent(appInfo.activityInfo.packageName
				+ Intents.Service.ACTION_SERVICE_GET_STATUS);
		context.sendOrderedBroadcast(broadcastIntent, null, receiverResult,	null, Activity.RESULT_OK, null, null);
	}

	/**
	 * Load the settings activity of a particular rcs service. The rcs service
	 * activity is identified by the object ResolveInfo recovered via the method
	 * getRcsServices.
	 * 
	 * @param context Application context
	 * @param appInfo Application info
	 */
	public static void loadRcsServiceSettings(Context context, ResolveInfo appInfo) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setComponent(new ComponentName(appInfo.activityInfo.packageName,	appInfo.activityInfo.name));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		context.startActivity(intent);
	}
}
