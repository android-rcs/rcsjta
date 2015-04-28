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

package com.gsma.rcs.core.ims.service.im.chat;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Is-composing events generator
 * 
 * @author jexa7410
 */
public class IsComposingGenerator {

    private static final Logger sLogger = Logger.getLogger(IsComposingGenerator.class
            .getSimpleName());

    private static enum ComposingEvent {
        IS_COMPOSING, IS_NOT_COMPOSING
    };

    private ExpirationTimer mExpirationTimer;

    private ComposingEvent mLastComposingEvent;

    private long mLastOnComposingTimestamp;

    private boolean mIsLastCommandSucessfull;

    private RcsSettings mRcsSettings;

    private ChatSession mSession;

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    /**
     * Constructor
     * 
     * @param session Chat session
     * @param rcsSettings Settings
     */
    public IsComposingGenerator(ChatSession session, RcsSettings rcsSettings) {
        mSession = session;
        mRcsSettings = rcsSettings;
        resetParameters();
    }

    /**
     * Reset parameters used by the expiration timer.
     */
    private void resetParameters() {
        mLastComposingEvent = ComposingEvent.IS_NOT_COMPOSING;
        mLastOnComposingTimestamp = -1;
        mIsLastCommandSucessfull = true;
    }

    /**
     * Handle isComposingEvent from API.
     * 
     * @param enabled
     */
    public void handleIsComposingEvent(final boolean enabled) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("handleIsComposingEvent : ".concat(String.valueOf(enabled)));
        }

        if (!enabled) {
            synchronized (mLock) {
                if (mExpirationTimer != null) {
                    mExpirationTimer.stop();
                    mExpirationTimer = null;
                }
            }
            if (ComposingEvent.IS_COMPOSING == mLastComposingEvent && mIsLastCommandSucessfull) {
                mIsLastCommandSucessfull = mSession.sendIsComposingStatus(false);
            }
            resetParameters();
            return;
        }

        mLastOnComposingTimestamp = System.currentTimeMillis();
        if (ComposingEvent.IS_NOT_COMPOSING == mLastComposingEvent && mIsLastCommandSucessfull) {
            mIsLastCommandSucessfull = mSession.sendIsComposingStatus(true);
        }
        mLastComposingEvent = ComposingEvent.IS_COMPOSING;
        if (mExpirationTimer == null) {
            if (logActivated) {
                sLogger.debug("No active ExpirationTimer : schedule new task");
            }
            if (!mIsLastCommandSucessfull && logActivated) {
                sLogger.debug("The last sendIsComposingStatus command has failed");
            }
            mExpirationTimer = new ExpirationTimer(mLastOnComposingTimestamp);
        }
    }

    /**
     * This method handles "message was sent" event from the session. It will cancel the existing
     * timer and set the IsComposingGenerator in its initial state
     */
    public void handleMessageWasSentEvent() {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("--> handleMessageWasSentEvent");
        }
        synchronized (mLock) {
            if (mExpirationTimer != null) {
                mExpirationTimer.stop();
                mExpirationTimer = null;
            }
        }
        resetParameters();
        if (logActivated) {
            sLogger.debug("<-- handleMessageWasSentEvent");
        }
    }

    /**
     * Expiration Timer
     */
    private class ExpirationTimer extends TimerTask {

        private final static String TIMER_NAME = "IS_COMPOSING_GENERATOR_TIMER";

        private long mActivationDate;
        private Timer mTimer;

        /**
         * Default constructor
         * 
         * @param activationDate
         */
        public ExpirationTimer(long activationDate) {
            mActivationDate = activationDate;
            mTimer = new Timer(TIMER_NAME);
            mTimer.schedule(this, mRcsSettings.getIsComposingTimeout());
        }

        public void stop() {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }

        @Override
        public void run() {
            synchronized (mLock) {
                boolean logActivated = sLogger.isActivated();
                if (logActivated) {
                    sLogger.debug("OnComposing timer has expired: ");
                }

                long now = System.currentTimeMillis();
                if (mActivationDate < mLastOnComposingTimestamp) {
                    if (logActivated) {
                        sLogger.debug(" --> user is still composing");
                    }
                    if (!mIsLastCommandSucessfull) {
                        if (logActivated) {
                            sLogger.debug(" --> The last sendIsComposingStatus command has failed. Send a new one");
                        }
                        mIsLastCommandSucessfull = mSession.sendIsComposingStatus(true);
                    }
                    mExpirationTimer = new ExpirationTimer(now);
                } else {
                    if (logActivated) {
                        sLogger.debug(" --> go into IDLE state");
                    }
                    mLastComposingEvent = ComposingEvent.IS_NOT_COMPOSING;
                    if (mIsLastCommandSucessfull) {
                        mIsLastCommandSucessfull = mSession.sendIsComposingStatus(false);
                    }
                    mExpirationTimer = null;
                }
                stop();
            }
        }
    }

}
