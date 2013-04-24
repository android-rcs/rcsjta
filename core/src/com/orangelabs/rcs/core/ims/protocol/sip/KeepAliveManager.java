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

package com.orangelabs.rcs.core.ims.protocol.sip;

import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.PeriodicRefresher;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Keep-alive manager (see RFC 5626)
 *
 * @author BJ
 */
public class KeepAliveManager extends PeriodicRefresher {
    /**
     * Keep-alive period (in seconds)
     */
	private int period;
	
    /**
     * SIP interface
     */
    private SipInterface sip;
    
	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 */
	public KeepAliveManager(SipInterface sip) {
		this.sip = sip;
		this.period = RcsSettings.getInstance().getSipKeepAlivePeriod();
	}
	
	/**
	 * Start
	 */
	public void start() {
		if (logger.isActivated()) {
			logger.debug("Start keep-alive");
		}
		startTimer(period, 1);
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
        	sip.getDefaultSipProvider().getListeningPoints()[0].sendHeartbeat(sip.getOutboundProxyAddr(), sip.getOutboundProxyPort());
        	
        	// Start timer
    		startTimer(period, 1);
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("SIP heartbeat has failed", e);
            }
        }
    }
}
