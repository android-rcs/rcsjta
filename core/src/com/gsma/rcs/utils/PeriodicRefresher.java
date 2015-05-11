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

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.logger.Logger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Periodic refresher
 * 
 * @author JM. Auffret
 */
public abstract class PeriodicRefresher {
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
     */
    public abstract void periodicProcessing();

    /**
     * Start the timer
     * @param currentTime Time from when the timer has to be started
     * @param expirePeriod Expiration period in milliseconds
     */
    public void startTimer(long currentTime, long expirePeriod) {
        startTimer(currentTime, expirePeriod, 1.0);
    }

    /**
     * Start the timer
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

        // Register the alarm receiver
        AndroidFactory.getApplicationContext().registerReceiver(mAlarmReceiver,
                new IntentFilter(mAction));

        // Start alarm from now to the expire value
        AlarmManager am = (AlarmManager) AndroidFactory.getApplicationContext().getSystemService(
                Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, currentTime + pollingPeriod, mAlarmIntent);

        // The timer is started
        mTimerStarted = true;
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
                    // Processing
                    periodicProcessing();
                }
            };
            t.start();
        }
    }
}
