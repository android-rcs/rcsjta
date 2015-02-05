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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.security.cert.KeyStoreManager;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Wifi access network
 * 
 * @author jexa7410
 */
public class WifiNetworkAccess extends NetworkAccess {
    /**
     * Wi-Fi manager
     */
    private WifiManager wifiManager;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @throws CoreException
     */
    public WifiNetworkAccess() throws CoreException {
        super();

        // Get Wi-Fi info
        wifiManager = (WifiManager) AndroidFactory.getApplicationContext().getSystemService(
                Context.WIFI_SERVICE);

        if (logger.isActivated()) {
            logger.info("Wi-Fi access has been created (interface " + getType() + ")");
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

        // Changed by Deutsche Telekom
        KeyStoreManager.updateClientCertificate(ipAddress);
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
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info.getLinkSpeed() <= 11) {
            return "IEEE-802.11b";
        } else {
            return "IEEE-802.11a";
        }
    }

    /**
     * Return the network name
     * 
     * @return Name
     */
    public String getNetworkName() {
        String name = "Wi-Fi ";
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info.getLinkSpeed() <= 11) {
            name += "802.11b";
        } else {
            name += "802.11a";
        }
        name += ", SSID=" + wifiManager.getConnectionInfo().getSSID();
        return name;
    }
}
