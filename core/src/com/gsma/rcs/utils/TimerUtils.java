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

package com.gsma.rcs.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utilities for timer management
 */
public class TimerUtils {

    private static final int KITKAT_VERSION_CODE = 19;

    private static final String SET_EXACT_METHOD_NAME = "setExact";

    private static final Class<?>[] SET_EXACT_METHOD_PARAM = new Class[] {
            int.class, long.class, PendingIntent.class
    };

    /**
     * Schedule an alarm with exact timer
     * 
     * @param alarmManager
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
        if (Build.VERSION.SDK_INT < KITKAT_VERSION_CODE) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
        } else {
            try {
                Method setExactMethod = alarmManager.getClass().getDeclaredMethod(
                        SET_EXACT_METHOD_NAME, SET_EXACT_METHOD_PARAM);
                setExactMethod.invoke(alarmManager, AlarmManager.RTC_WAKEUP, triggerAtMillis,
                        operation);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOperationException("Failed to get setExact method!", e);

            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(
                        "No access to the definition of setExact method!", e);

            } catch (InvocationTargetException e) {
                throw new UnsupportedOperationException("Can't invoke setExact method!", e);
            }
        }
    }
}
