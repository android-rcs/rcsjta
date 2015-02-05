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

package com.orangelabs.rcs.core.access;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Mobile access network
 * 
 * @author jexa7410
 */
public class MobileNetworkAccess extends NetworkAccess {
    /**
     * Telephony manager
     */
    private TelephonyManager telephonyManager;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @throws CoreException
     */
    public MobileNetworkAccess() throws CoreException {
        super();

        // Get telephony info
        telephonyManager = (TelephonyManager) AndroidFactory.getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE);

        if (logger.isActivated()) {
            logger.info("Mobile access has been created (interface " + getNetworkName() + ")");
        }
    }

    /**
     * Connect to the network access
     * 
     * @param ipAddress Local IP address
     */
    public void connect(String ipAddress) {
        if (logger.isActivated()) {
            logger.info("Network access connected (" + ipAddress + ")");
        }
        this.ipAddress = ipAddress;
    }

    /**
     * Disconnect from the network access
     */
    public void disconnect() {
        if (logger.isActivated()) {
            logger.info("Network access disconnected");
        }
        ipAddress = null;
    }

    /**
     * Return the type of access
     * 
     * @return Type
     */
    public String getType() {
        int type = telephonyManager.getNetworkType();
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "3GPP-GERAN";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "3GPP-UTRAN-FDD";
            default:
                return null;
        }
    }

    /**
     * Return the network name
     * 
     * @return Name
     */
    public String getNetworkName() {
        int type = telephonyManager.getNetworkType();
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "EDGE";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "UMTS";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "HSPA";
            default:
                return "unknown";
        }
    }
}
