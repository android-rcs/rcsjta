/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.access;

import com.gsma.rcs.core.ims.security.cert.KeyStoreManager;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.security.cert.CertificateException;

/**
 * Wifi access network
 * 
 * @author jexa7410
 */
public class WifiNetworkAccess extends NetworkAccess {

    private WifiManager mWifiManager;

    private static final Logger sLogger = Logger.getLogger(WifiNetworkAccess.class.getSimpleName());

    /**
     * Constructor
     */
    public WifiNetworkAccess(RcsSettings rcsSettings) {
        super(rcsSettings);
        mWifiManager = (WifiManager) AndroidFactory.getApplicationContext().getSystemService(
                Context.WIFI_SERVICE);
        if (sLogger.isActivated()) {
            sLogger.info("Wi-Fi access has been created (interface " + getType() + ")");
        }
    }

    /**
     * Connect to the network access
     * 
     * @param ipAddress Local IP address
     * @throws CertificateException
     * @throws IOException
     */
    public void connect(String ipAddress) throws CertificateException, IOException {
        if (sLogger.isActivated()) {
            sLogger.info("Network access connected (" + ipAddress + ")");
        }
        mIpAddress = ipAddress;
        if (mRcsSettings.isSecureMsrpOverWifi()) {
            try {
                KeyStoreManager.updateClientCertificate(ipAddress);

            } catch (CertificateException | IOException e) {
                if (sLogger.isActivated()) {
                    sLogger.error(e.getMessage());
                }
            }
        }
    }

    /**
     * Disconnect from the network access
     */
    public void disconnect() {
        if (sLogger.isActivated()) {
            sLogger.info("Network access disconnected");
        }
        mIpAddress = null;
    }

    /**
     * Return the type of access
     * 
     * @return Type
     */
    public String getType() {
        WifiInfo info = mWifiManager.getConnectionInfo();
        if (info.getLinkSpeed() <= 11) {
            return "IEEE-802.11b";
        }
        return "IEEE-802.11a";
    }

    /**
     * Return the network name
     * 
     * @return Name
     */
    public String getNetworkName() {
        String name = "Wi-Fi ";
        WifiInfo info = mWifiManager.getConnectionInfo();
        if (info.getLinkSpeed() <= 11) {
            name += "802.11b";
        } else {
            name += "802.11a";
        }
        name += ", SSID=" + mWifiManager.getConnectionInfo().getSSID();
        return name;
    }
}
