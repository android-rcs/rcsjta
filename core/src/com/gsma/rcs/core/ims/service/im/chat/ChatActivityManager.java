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

package com.gsma.rcs.core.ims.service.im.chat;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PeriodicRefresher;
import com.gsma.rcs.utils.logger.Logger;

/**
 * Chat activity manager which manages the idle state of the session. It maintains a timer that is
 * canceled and restarted when the session has activity, i.e. when MSRP chunks are received or
 * emitted. If the timer expires, the session is aborted.
 */
public class ChatActivityManager extends PeriodicRefresher {
    /**
     * Last activity timestamp
     */
    private long activityTimesamp = 0L;

    /**
     * Session timeout (in seconds)
     */
    private int timeout;

    /**
     * IM session
     */
    private ChatSession session;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param session IM session
     */
    public ChatActivityManager(ChatSession session) {
        this.session = session;
        this.timeout = RcsSettings.getInstance().getChatIdleDuration();
    }

    /**
     * Update the session activity
     */
    public void updateActivity() {
        activityTimesamp = System.currentTimeMillis();
    }

    /**
     * Start manager
     */
    public void start() {
        if (logger.isActivated()) {
            logger.info("Start the activity manager for " + timeout + "s");
        }

        // Reset the inactivity timestamp
        updateActivity();

        // Start a timer to check if the inactivity period has been reach or not each 10seconds
        startTimer(timeout, 1.0);
    }

    /**
     * Stop manager
     */
    public void stop() {
        if (logger.isActivated()) {
            logger.info("Stop the activity manager");
        }

        // Stop timer
        stopTimer();
    }

    /**
     * Periodic processing
     */
    public void periodicProcessing() {
        long currentTime = System.currentTimeMillis();
        int inactivityPeriod = (int) ((currentTime - activityTimesamp) / 1000) + 1;
        int remainingPeriod = timeout - inactivityPeriod;
        if (logger.isActivated()) {
            logger.debug("Check inactivity period: inactivity=" + inactivityPeriod + ", remaining="
                    + remainingPeriod);
        }

        if (inactivityPeriod >= timeout) {
            if (logger.isActivated()) {
                logger.debug("No activity on the session during " + timeout
                        + "s: abort the session");
            }
            session.handleChatInactivityEvent();
        } else {
            // Restart timer
            startTimer(remainingPeriod, 1.0);
        }
    }
}
