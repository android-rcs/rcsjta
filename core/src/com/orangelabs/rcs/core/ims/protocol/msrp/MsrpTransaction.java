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
package com.orangelabs.rcs.core.ims.protocol.msrp;

import java.util.Timer;
import java.util.TimerTask;

/**
 * MSRP transaction
 *
 * @author B. JOGUET
 */
public class MsrpTransaction extends Object {
    /**
     * MRSP report transaction timeout (in seconds)
     */
    private final static int TIMEOUT = 30;

    /**
     * Count number of sent requests without response 
     */
    private int waitingCount = 0;

    /**
     * Count number of sent requests without response 
     */
    private boolean isWaiting = false;

    /**
     * is MSRP session terminated ? 
     */
    private boolean isTerminated = false;

    /**
     * Timer
     */
    private Timer timer = new Timer();

    /**
     * Constructor
     */
    public MsrpTransaction() {
    }

    /**
     * Wait all MSRP responses
     */
    public synchronized void waitAllResponses() {
        if (waitingCount > 0) {
            isWaiting = true;
            try {
                // Start timeout
                startTimer();

                // Wait semaphore
                super.wait();
            } catch(InterruptedException e) {
                // Nothing to do
            }
        }
    }

    /**
     * Handle new request
     */
    public void handleRequest() {
        waitingCount++;
    }

    /**
     * Handle new response
     */
    public synchronized void handleResponse() {
        waitingCount--;
        if (isWaiting) {
            if (waitingCount == 0) {
                // Unblock semaphore
                super.notify();
            } else {
                // ReInit timeout
                stopTimer();
                startTimer();
            }
        }
    }

    /**
     * Is all responses received
     *
     * @return Boolean
     */
    public boolean isAllResponsesReceived() {
        return (waitingCount == 0);
    }

    /**
     * Terminate transaction
     */
    public synchronized void terminate() {
        isTerminated = true;
        // Unblock semaphore
        super.notify();
        // Stop timer
        stopTimer();
    }

    /** 
     * Return isTerminated status.
     *
     * @return true if terminated
     */
    public boolean isTerminated() {
        return isTerminated;
    }

    /**
     * Start the timer
     */
    private void startTimer() {
        timer = new Timer();
        TimerTask timertask = new TimerTask() {
            @Override
            public void run() {
                timerExpire();
            }
        };
        timer.schedule(timertask, TIMEOUT * 1000);
    }

    /**
     * Stop the timer
     */
    private void stopTimer() {
        timer.cancel();
    }

    /** 
     * Timer execution
     */
    private synchronized void timerExpire() {
        // Unblock semaphore
        super.notify();
    }
}
