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

package com.gsma.rcs.core.ims.protocol.sip;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PeriodicRefresher;
import com.gsma.rcs.utils.logger.Logger;

/**
 * Keep-alive manager (see RFC 5626)
 * 
 * @author BJ
 */
public class KeepAliveManager extends PeriodicRefresher {
    /**
     * Keep-alive period (in milliseconds)
     */
    private long mPeriod;

    /**
     * SIP interface
     */
    private SipInterface mSip;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param sip
     * @param rcsSettings
     */
    public KeepAliveManager(SipInterface sip, RcsSettings rcsSettings) {
        mSip = sip;
        mPeriod = rcsSettings.getSipKeepAlivePeriod();
    }

    /**
     * Start
     */
    public void start() {
        if (logger.isActivated()) {
            logger.debug("Start keep-alive");
        }
        startTimer(System.currentTimeMillis(), mPeriod);
    }

    /**
     * Start
     */
    public void stop() {
        if (logger.isActivated()) {
            logger.debug("Stop keep-alive");
        }
        stopTimer();
    }

    /**
     * Keep-alive processing
     */
    public void periodicProcessing() {
        try {
            if (logger.isActivated()) {
                logger.debug("Send keep-alive");
            }

            // Send a double-CRLF
            mSip.getDefaultSipProvider().getListeningPoints()[0].sendHeartbeat(
                    mSip.getOutboundProxyAddr(), mSip.getOutboundProxyPort());

            // Start timer
            startTimer(System.currentTimeMillis(), mPeriod);
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("SIP heartbeat has failed", e);
            }
        }
    }

    /**
     * @param period the keep alive period in milliseconds
     */
    public void setPeriod(long period) {
        mPeriod = period;
        if (logger.isActivated()) {
            logger.debug("Set keep-alive period \"" + period + "\"");
        }
    }
}
