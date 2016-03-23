/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.contact.ContactManagerException;
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
    private final KeepAlive mAlarmReceiver = new KeepAlive();

    private final PendingIntent mAlarmIntent;

    private final String mAction;

    private boolean mTimerStarted = false;

    private final Context mContext;

    private final AlarmManager mAlarmManager;

    private static final Logger sLogger = Logger.getLogger(PeriodicRefresher.class.getName());

    /**
     * Constructor
     */
    public PeriodicRefresher() {
        mContext = AndroidFactory.getApplicationContext();
        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        /* Create a unique pending intent */
        mAction = new StringBuilder(getClass().getName()).append('_')
                .append(System.currentTimeMillis()).toString(); /* Unique action ID */
        mAlarmIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(mAction), 0);
    }

    /**
     * Periodic processing
     * 
     * @throws NetworkException
     * @throws PayloadException
     * @throws ContactManagerException
     */
    public abstract void periodicProcessing() throws PayloadException, NetworkException,
            ContactManagerException;

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
        /* Check expire period */
        if (expirePeriod <= 0) {
            /* Expire period is null */
            if (sLogger.isActivated()) {
                sLogger.debug("Timer is deactivated");
            }
            return;
        }

        /* Calculate the effective refresh period */
        long pollingPeriod = (long) (expirePeriod * delta);
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Start timer at period=").append(pollingPeriod)
                    .append("ms (expiration=").append(expirePeriod).append("ms)").toString());
        }
        mContext.registerReceiver(mAlarmReceiver, new IntentFilter(mAction));
        TimerUtils.setExactTimer(mAlarmManager, currentTime + pollingPeriod, mAlarmIntent);
        mTimerStarted = true;
    }

    /**
     * Stop the timer
     */
    public synchronized void stopTimer() {
        if (!mTimerStarted) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Stop timer");
        }
        /* The timer is stopped */
        mTimerStarted = false;
        mAlarmManager.cancel(mAlarmIntent);

        try {
            mContext.unregisterReceiver(mAlarmReceiver);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
    }

    /**
     * Keep alive manager
     */
    private class KeepAlive extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Core.getInstance().scheduleCoreOperation(new Runnable() {
                @Override
                public void run() {
                    try {
                        periodicProcessing();
                    } catch (ContactManagerException e) {
                        sLogger.error("IMS re-registration unsuccessful!", e);
                    } catch (PayloadException e) {
                        sLogger.error("IMS re-registration unsuccessful!", e);
                    } catch (NetworkException e) {
                        /* Nothing to be handled here */
                        if (sLogger.isActivated()) {
                            sLogger.debug(e.getMessage());
                        }
                    } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                        sLogger.error("IMS re-registration unsuccessful!", e);
                        stopTimer();
                    }
                }
            });
        }
    }
}
