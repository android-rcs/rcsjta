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

package com.gsma.rcs.utils;

import com.gsma.rcs.platform.AndroidFactory;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

/**
 * Network utils
 * 
 * @author hlxn7157
 */
public class NetworkUtils {
    /**
     * Network access type unknown
     */
    public static int NETWORK_ACCESS_UNKNOWN = -1;

    /**
     * Network access type 2G
     */
    public static int NETWORK_ACCESS_2G = 0;

    /**
     * Network access type 3G
     */
    public static int NETWORK_ACCESS_3G = 1;

    /**
     * Network access type 3G+
     */
    public static int NETWORK_ACCESS_3GPLUS = 2;

    /**
     * Network access type Wi-Fi
     */
    public static int NETWORK_ACCESS_WIFI = 3;

    /**
     * Network access type 4G LTE
     */
    public static int NETWORK_ACCESS_4G = 4;

    /**
     * Get network access type
     * 
     * @return Type
     */
    public static int getNetworkAccessType() {
        ConnectivityManager connectivityMgr = (ConnectivityManager) AndroidFactory
                .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityMgr.getActiveNetworkInfo();
        if (networkInfo == null) {
            return NETWORK_ACCESS_UNKNOWN;
        }
        int networkType = networkInfo.getType();
        switch (networkType) {
            case ConnectivityManager.TYPE_WIFI:
                return NETWORK_ACCESS_WIFI;

            case ConnectivityManager.TYPE_MOBILE:
                return getNetworkSubType(networkInfo.getSubtype());

            default:
                return NETWORK_ACCESS_UNKNOWN;
        }
    }

    /**
     * Returns network subType
     */
    private static int getNetworkSubType(int subType) {
        switch (subType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                /* Intentional fall back */
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return NETWORK_ACCESS_2G;

                /* ~ 400-7000 kbps */
            case TelephonyManager.NETWORK_TYPE_UMTS:
                /* Intentional fall back */
                /* ~ 700-1700 kbps */
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return NETWORK_ACCESS_3G;

                /* ~ 2-14 Mbps */
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                /* Intentional fall back */
                /* ~ 1-23 Mbps */
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                /* Intentional fall back */
                /* ~10-20 Mbps */
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NETWORK_ACCESS_3GPLUS;

                /* ~10+ Mbps */
            case TelephonyManager.NETWORK_TYPE_LTE:
                return NETWORK_ACCESS_4G;
            default:
                return NETWORK_ACCESS_UNKNOWN;
        }
    }
}
