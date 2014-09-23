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
package com.orangelabs.rcs.service;

import java.util.List;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

/**
 * Client API utils
 * 
 * @author Jean-Marc AUFFRET
 */
public class ServiceUtils {
	/**
	 * RCS service name
	 */
	public static final String RCS_SERVICE_NAME = "com.orangelabs.rcs.SERVICE";

    /**
     * Startup service name
     */
    public static final String STARTUP_SERVICE_NAME = "com.orangelabs.rcs.STARTUP";	
	
    /**
     * Provisioning service name
     */
    public static final String PROVISIONING_SERVICE_NAME = "com.orangelabs.rcs.PROVISIONING";

    /**
	 * Is service started
	 *
	 * @param ctx Context
	 * @return Boolean
	 */
	public static boolean isServiceStarted(Context ctx) {
	    ActivityManager activityManager = (ActivityManager)ctx.getSystemService(Context.ACTIVITY_SERVICE);
	    List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (serviceList != null) {
            for (int i = 0; i < serviceList.size(); i++) {
                ActivityManager.RunningServiceInfo serviceInfo = serviceList.get(i);
                if (serviceInfo != null && serviceInfo.service != null && "com.orangelabs.rcs.service.RcsCoreService".equals(serviceInfo.service.getClassName())) {
                    if (serviceInfo.pid != 0) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
	    return false;
	}	
	
	/**
	 * Start RCS service
	 *
	 * @param ctx Context
	 */
	public static void startRcsService(Context ctx) {
        ctx.startService(new Intent(STARTUP_SERVICE_NAME));
	}

	/**
	 * Stop RCS service
	 *
	 * @param ctx Context
	 */
	public static void stopRcsService(Context ctx) {
        ctx.stopService(new Intent(STARTUP_SERVICE_NAME));
        ctx.stopService(new Intent(PROVISIONING_SERVICE_NAME));
        ctx.stopService(new Intent(RCS_SERVICE_NAME));
	}
}
