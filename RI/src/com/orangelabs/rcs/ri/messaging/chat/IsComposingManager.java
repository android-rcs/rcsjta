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

package com.orangelabs.rcs.ri.messaging.chat;

import android.os.Handler;
import android.os.Message;

/**
 * Utility class to handle is_typing timers (see RFC3994)
 */
public class IsComposingManager {
    // Idle time out (in ms)
    private int idleTimeOut = 0;

    // Active state refresh interval (in ms)
    private final static int ACTIVE_STATE_REFRESH = 60 * 1000;

    // Clock handler
    private ClockHandler handler = new ClockHandler();

    // Is composing state
    private boolean isComposing = false;

    // Event IDs
    private final static int IS_STARTING_COMPOSING = 1;

    private final static int IS_STILL_COMPOSING = 2;

    private final static int MESSAGE_WAS_SENT = 3;

    private final static int ACTIVE_MESSAGE_NEEDS_REFRESH = 4;

    private final static int IS_IDLE = 5;

    private INotifyComposing mNotifyComposing;

    /**
     * Constructor
     * 
     * @param timeout
     * @param notifyComposing interface to notify isComposing status
     */
    public IsComposingManager(int timeout, INotifyComposing notifyComposing) {
        idleTimeOut = timeout;
        mNotifyComposing = notifyComposing;
    }

    /**
     * Interface to notify isComposing status
     */
    public interface INotifyComposing {
        /**
         * @param status
         */
        public void setTypingStatus(boolean status);
    }

    // Clock handler class
    private class ClockHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case IS_STARTING_COMPOSING: {
                    // Send a typing status "active"
                    mNotifyComposing.setTypingStatus(true);

                    // In IDLE_TIME_OUT we will need to send a is-idle status
                    // message
                    handler.sendEmptyMessageDelayed(IS_IDLE, idleTimeOut);

                    // In ACTIVE_STATE_REFRESH we will need to send an active
                    // status message refresh
                    handler.sendEmptyMessageDelayed(ACTIVE_MESSAGE_NEEDS_REFRESH,
                            ACTIVE_STATE_REFRESH);
                    break;
                }
                case IS_STILL_COMPOSING: {
                    // Cancel the IS_IDLE messages in queue, if there was one
                    handler.removeMessages(IS_IDLE);

                    // In IDLE_TIME_OUT we will need to send a is-idle status
                    // message
                    handler.sendEmptyMessageDelayed(IS_IDLE, idleTimeOut);
                    break;
                }
                case MESSAGE_WAS_SENT: {
                    // We are now going to idle state
                    hasNoActivity();

                    // Cancel the IS_IDLE messages in queue, if there was one
                    handler.removeMessages(IS_IDLE);

                    // Cancel the ACTIVE_MESSAGE_NEEDS_REFRESH messages in
                    // queue, if there was one
                    handler.removeMessages(ACTIVE_MESSAGE_NEEDS_REFRESH);
                    break;
                }
                case ACTIVE_MESSAGE_NEEDS_REFRESH: {
                    // We have to refresh the "active" state
                    mNotifyComposing.setTypingStatus(true);

                    // In ACTIVE_STATE_REFRESH we will need to send an active
                    // status message refresh
                    handler.sendEmptyMessageDelayed(ACTIVE_MESSAGE_NEEDS_REFRESH,
                            ACTIVE_STATE_REFRESH);
                    break;
                }
                case IS_IDLE: {
                    // End of typing
                    hasNoActivity();

                    // Send a typing status "idle"
                    mNotifyComposing.setTypingStatus(false);

                    // Cancel the ACTIVE_MESSAGE_NEEDS_REFRESH messages in
                    // queue, if there was one
                    handler.removeMessages(ACTIVE_MESSAGE_NEEDS_REFRESH);
                    break;
                }
            }
        }
    }

    /**
     * Edit text has activity
     */
    public void hasActivity() {
        // We have activity on the edit text
        if (!isComposing) {
            // If we were not already in isComposing state
            handler.sendEmptyMessage(IS_STARTING_COMPOSING);
            isComposing = true;
        } else {
            // We already were composing
            handler.sendEmptyMessage(IS_STILL_COMPOSING);
        }
    }

    /**
     * Edit text has no activity anymore
     */
    public void hasNoActivity() {
        isComposing = false;
    }

    /**
     * The message was sent
     */
    public void messageWasSent() {
        handler.sendEmptyMessage(MESSAGE_WAS_SENT);
    }
}
