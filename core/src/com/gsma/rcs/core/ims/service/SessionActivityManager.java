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

package com.gsma.rcs.core.ims.service;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.utils.PeriodicRefresher;
import com.gsma.rcs.utils.logger.Logger;

/**
 * Session activity manager which manages the idle state of the session. It maintains a timer that
 * is canceled and restarted when the session has activity, i.e. when MSRP chunks are received or
 * emitted. If the timer expires, the session is aborted.
 */
public class SessionActivityManager extends PeriodicRefresher {
    /**
     * Last activity timestamp
     */
    private long mActivityTimestamp = 0L;

    /**
     * ImsServiceSession
     */
    private final ImsServiceSession mSession;

    /**
     * Timeout
     */
    private final long mTimeout;

    private static final Logger sLogger = Logger.getLogger(SessionActivityManager.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param session IM session
     * @param timeout Idle timeout
     */
    public SessionActivityManager(ImsServiceSession session, long timeout) {
        mSession = session;
        mTimeout = timeout;
    }

    /**
     * Update the session activity
     */
    public void updateActivity() {
        mActivityTimestamp = System.currentTimeMillis();
    }

    /**
     * Start manager
     */
    public void start() {
        if (mTimeout == 0) {
            if (sLogger.isActivated()) {
                sLogger.info("Activity manager is disabled (no idle timeout)");
            }
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.info("Start the activity manager for " + mTimeout + "ms");
        }
        // Reset the inactivity timestamp
        updateActivity();

        // Start a timer to check if the inactivity period has been reach or not each 10seconds
        startTimer(System.currentTimeMillis(), mTimeout);
    }

    /**
     * Stop manager
     */
    public void stop() {
        if (sLogger.isActivated()) {
            sLogger.info("Stop the activity manager");
        }
        stopTimer();
    }

    /**
     * Periodic processing
     * 
     * @throws NetworkException
     * @throws PayloadException
     */
    public void periodicProcessing() throws PayloadException, NetworkException {
        long timeout = mTimeout;
        long inactivityPeriod = System.currentTimeMillis() - mActivityTimestamp;
        long remainingPeriod = timeout - inactivityPeriod;
        if (sLogger.isActivated()) {
            sLogger.debug("Check inactivity period: inactivity=" + inactivityPeriod
                    + ", remaining=" + remainingPeriod);
        }
        if (inactivityPeriod >= timeout) {
            if (sLogger.isActivated()) {
                sLogger.debug("No activity on the session during " + timeout
                        + "ms: abort the session");
            }
            mSession.handleInactivityEvent();
        } else {
            startTimer(System.currentTimeMillis(), remainingPeriod);
        }
    }
}
