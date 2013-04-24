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

package com.orangelabs.rcs.utils;

import com.orangelabs.rcs.platform.AndroidFactory;

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
        int result = NETWORK_ACCESS_UNKNOWN;
        try {
            ConnectivityManager connectivityMgr = (ConnectivityManager) AndroidFactory.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityMgr.getActiveNetworkInfo();
            if (networkInfo != null) {
                int type = networkInfo.getType();
                if (type == ConnectivityManager.TYPE_WIFI) {
                    result = NETWORK_ACCESS_WIFI;
                } else if (type == ConnectivityManager.TYPE_MOBILE) {
                    int subtype = networkInfo.getSubtype();
                    switch (subtype) {
                        case TelephonyManager.NETWORK_TYPE_GPRS:
                        case TelephonyManager.NETWORK_TYPE_EDGE:
                            result = NETWORK_ACCESS_2G;
                            break;
                        case TelephonyManager.NETWORK_TYPE_UMTS:    // ~ 400-7000 kbps
                        case TelephonyManager.NETWORK_TYPE_HSPA:    // ~ 700-1700 kbps
                            result = NETWORK_ACCESS_3G;
                            break;
                        case TelephonyManager.NETWORK_TYPE_HSDPA:   // ~ 2-14 Mbps
                        case TelephonyManager.NETWORK_TYPE_HSUPA:   // ~ 1-23 Mbps
                        case 15: //TelephonyManager.NETWORK_TYPE_HSPAP (available on API level 13) // ~ 10-20 Mbps
                            result = NETWORK_ACCESS_3GPLUS;
                            break;
                        case 13: //TelephonyManager.NETWORK_TYPE_LTE (available on API level 11) // ~ 10+ Mbps
                            result = NETWORK_ACCESS_4G;
                            break;
                    }
                }
            }
        } catch (Exception e) {
            // Nothing to do
        }
        return result;
    }
}
