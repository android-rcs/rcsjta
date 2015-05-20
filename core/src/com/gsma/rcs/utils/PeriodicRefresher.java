/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.logger.Logger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Periodic refresher
 * 
 * @author JM. Auffret
 */
public abstract class PeriodicRefresher {

    private static final int KITKAT_VERSION_CODE = 19;

    private static final String SET_EXACT_METHOD_NAME = "setExact";

    private static final Class[] SET_EXACT_METHOD_PARAM = new Class[] {
            int.class, long.class, PendingIntent.class
    };

    /**
     * Keep alive manager
     */
    private KeepAlive mAlarmReceiver = new KeepAlive();

    /**
     * Alarm intent
     */
    private PendingIntent mAlarmIntent;

    /**
     * Action
     */
    private String mAction;

    /**
     * Timer state
     */
    private boolean mTimerStarted = false;

    /**
     * The logger
     */
    private Logger mLogger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     */
    public PeriodicRefresher() {
        // Create a unique pending intent
        this.mAction = this.toString(); // Unique action ID
        this.mAlarmIntent = PendingIntent.getBroadcast(AndroidFactory.getApplicationContext(), 0,
                new Intent(mAction), 0);
    }

    /**
     * Periodic processing
     * 
     * @throws SipNetworkException
     * @throws SipPayloadException
     */
    public abstract void periodicProcessing() throws SipPayloadException, SipNetworkException;

    /**
     * Start the timer
     * 
     * @param currentTime Time from when the timer has to be started
     * @param expirePeriod Expiration period in milliseconds
     */
    public void startTimer(long currentTime, long expirePeriod) {
        startTimer(currentTime, expirePeriod, 1.0);
    }

    /**
     * Start the timer
     * 
     * @param currentTime Time from when the timer has to be started
     * @param expirePeriod Expiration period in milliseconds
     * @param delta Delta to apply on the expire period in percentage
     */
    public synchronized void startTimer(long currentTime, long expirePeriod, double delta) {
        // Check expire period
        if (expirePeriod <= 0) {
            // Expire period is null
            if (mLogger.isActivated()) {
                mLogger.debug("Timer is deactivated");
            }
            return;
        }

        // Calculate the effective refresh period
        long pollingPeriod = (long) (expirePeriod * delta);
        if (mLogger.isActivated()) {
            mLogger.debug(new StringBuilder("Start timer at period=").append(pollingPeriod)
                    .append("ms (expiration=").append(expirePeriod).append("ms)").toString());
        }

        final Context ctx = AndroidFactory.getApplicationContext();
        ctx.registerReceiver(mAlarmReceiver, new IntentFilter(mAction));
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT < KITKAT_VERSION_CODE) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, currentTime + pollingPeriod, mAlarmIntent);
            mTimerStarted = true;
        } else {
            try {
                Method setExactMethod = alarmManager.getClass().getDeclaredMethod(
                        SET_EXACT_METHOD_NAME, SET_EXACT_METHOD_PARAM);
                setExactMethod.invoke(alarmManager, AlarmManager.RTC_WAKEUP, currentTime
                        + pollingPeriod, mAlarmIntent);
                mTimerStarted = true;
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

    /**
     * Stop the timer
     */
    public synchronized void stopTimer() {
        if (!mTimerStarted) {
            // Already stopped
            return;
        }

        if (mLogger.isActivated()) {
            mLogger.debug("Stop timer");
        }

        // The timer is stopped
        mTimerStarted = false;

        // Cancel alarm
        AlarmManager am = (AlarmManager) AndroidFactory.getApplicationContext().getSystemService(
                Context.ALARM_SERVICE);
        am.cancel(mAlarmIntent);

        // Unregister the alarm receiver
        try {
            AndroidFactory.getApplicationContext().unregisterReceiver(mAlarmReceiver);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
    }

    /**
     * Keep alive manager
     */
    private class KeepAlive extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        periodicProcessing();
                    } catch (SipPayloadException e) {
                        mLogger.error("Failed to initiate start timer!", e);
                        stopTimer();
                    } catch (SipNetworkException e) {
                        stopTimer();
                    } catch (RuntimeException e) {
                        /*
                         * Intentionally catch runtime exceptions as else it will abruptly end the
                         * thread and eventually bring the whole system down, which is not intended.
                         */
                        mLogger.error("Failed to initiate start timer!", e);
                        stopTimer();
                    }
                }
            };
            t.start();
        }
    }
}
