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

package com.gsma.rcs.service;

import java.util.List;

import android.app.ActivityManager;
import android.content.Context;

/**
 * Service utilities
 * 
 * @author Jean-Marc AUFFRET
 */
public class ServiceUtils {

    private static String CORE_SERVICE_CLASSNAME = "com.gsma.rcs.service.RcsCoreService";

    /**
     * Is service started
     * 
     * @param ctx Context
     * @return Boolean
     */
    public static boolean isServiceStarted(Context ctx) {
        ActivityManager activityManager = (ActivityManager) ctx
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                .getRunningServices(Integer.MAX_VALUE);
        if (serviceList == null) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo serviceInfo : serviceList) {
            if (serviceInfo == null) {
                continue;

            }
            if (serviceInfo.service == null) {
                continue;

            }
            if (CORE_SERVICE_CLASSNAME.equals(serviceInfo.service.getClassName())) {
                return serviceInfo.pid != 0;

            }
        }
        return false;
    }

}
