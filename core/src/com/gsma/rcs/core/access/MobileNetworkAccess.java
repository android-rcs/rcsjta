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

package com.gsma.rcs.core.access;

import com.gsma.rcs.core.ims.security.cert.KeyStoreManager;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;
import android.telephony.TelephonyManager;

import java.io.IOException;
import java.security.cert.CertificateException;

/**
 * Mobile access network
 * 
 * @author jexa7410
 */
public class MobileNetworkAccess extends NetworkAccess {

    private TelephonyManager mTelephonyManager;

    private static final Logger sLogger = Logger.getLogger(MobileNetworkAccess.class
            .getSimpleName());

    /**
     * Constructor
     */
    public MobileNetworkAccess(RcsSettings rcsSettings) {
        super(rcsSettings);
        mTelephonyManager = (TelephonyManager) AndroidFactory.getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (sLogger.isActivated()) {
            sLogger.info("Mobile access has been created (interface " + getNetworkName() + ")");
        }
    }

    /**
     * Connect to the network access
     * 
     * @param ipAddress Local IP address
     */
    public void connect(String ipAddress) {
        if (sLogger.isActivated()) {
            sLogger.info("Network access connected (" + ipAddress + ")");
        }
        mIpAddress = ipAddress;
        if (mRcsSettings.isSecureMsrpOverMobile()) {
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
        int type = mTelephonyManager.getNetworkType();
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
        int type = mTelephonyManager.getNetworkType();
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
