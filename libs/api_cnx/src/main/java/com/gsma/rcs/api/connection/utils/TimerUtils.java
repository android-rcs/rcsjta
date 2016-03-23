/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.api.connection.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;

public class TimerUtils {

    /**
     * Schedule an alarm with exact timer
     * 
     * @param alarmManager The alarm manager
     * @param triggerAtMillis time in milliseconds that the alarm should go off. @param operation
     *            Action to perform when the alarm goes off
     */
    public static void setExactTimer(AlarmManager alarmManager, long triggerAtMillis,
            PendingIntent operation) {
        /*
         * Beginning with API 19 (KITKAT), alarm delivery is inexact. The OS will shift alarms in
         * order to minimize wake ups and battery use. The new API setExact(int, long,
         * PendingIntent) will support applications which need strict delivery guarantees.
         * Applications whose targetSdkVersion is earlier than API 19 will continue to see the
         * previous behavior in which all alarms are delivered exactly when requested.
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
        }
    }
}
