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

package com.orangelabs.rcs.service.api;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.service.extension.ServiceExtensionManager;
import com.orangelabs.rcs.platform.AndroidFactory;

/**
 * Server API utils
 * 
 * @author Jean-Marc AUFFRET
 */
public class ServerApiUtils {
	/**
	 * Test core
	 * 
	 * @throws ServerApiException
	 */
	public static void testCore() throws ServerApiException {
		if (Core.getInstance() == null) {
			throw new ServerApiException("Core is not instanciated");
		}
	}
	
	/**
	 * Test IMS connection
	 * 
	 * @throws ServerApiException
	 */
	public static void testIms() throws ServerApiException {
		if (!isImsConnected()) { 
			throw new ServerApiException("Core is not connected to IMS"); 
		}
	}
	
	/**
	 * Is connected to IMS
	 * 
	 * @return Boolean
	 */
	public static boolean isImsConnected(){
		return ((Core.getInstance() != null) &&
				(Core.getInstance().getImsModule().getCurrentNetworkInterface() != null) &&
				(Core.getInstance().getImsModule().getCurrentNetworkInterface().isRegistered()));
	}
	
	/**
	 * Test API extension permission
	 * 
	 * @param ext Extension ID
	 * @throws ServerApiException
	 */
	public static void testApiExtensionPermission(String ext) throws ServerApiException {
		boolean authorized = false;

		// Check extension authorization 
		int pid = Binder.getCallingPid();
		RunningAppProcessInfo processInfo = null;
		ActivityManager manager = (ActivityManager)AndroidFactory.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
		for(RunningAppProcessInfo info : manager.getRunningAppProcesses()){
			if (info.pid == pid) {
				processInfo = info;
				break;
			}
		}
		if (processInfo != null) {
			PackageManager pm = AndroidFactory.getApplicationContext().getPackageManager();
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			for(ResolveInfo info : pm.queryIntentActivities(intent, 0)) {
				if (processInfo.processName.equals(info.activityInfo.packageName)) {
					if (ServiceExtensionManager.isExtensionAuthorized(AndroidFactory.getApplicationContext(), info, ext)) {
						authorized = true;
						break;
					}
				}
			}
		}
		
		if (!authorized) {
			throw new ServerApiException("Extension " + ext + " is not authorized"); 
		}
	}
}
