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

package com.orangelabs.rcs.utils;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import java.util.UUID;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.orangelabs.rcs.provider.settings.RcsSettings;

/***
 * Device utility functions
 *
 * @author jexa7410
 */
public class DeviceUtils {
	/**
	 * UUID
	 */
	private static UUID uuid = null;

	/**
	 * Returns unique UUID of the device
	 *
	 * @param context Context
	 * @return UUID
	 */
    public static UUID getDeviceUUID(Context context) {
		if (context == null) {
			return null;
		}

        if (uuid == null) {
            String imei = getImei(context);
            if (imei == null) {
                // For compatibility with device without telephony
                imei = getSerial();
            }
            if (imei != null) {
                uuid = UUID.nameUUIDFromBytes(imei.getBytes(UTF8));
            }
		}

		return uuid;
	}

	/**
	 * Returns the serial number of the device. Only works from OS version Gingerbread.
	 *
	 * @return Serial number
	 */
	private static String getSerial() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			return android.os.Build.SERIAL;
		} else {
			return null;
		}
	}

    /**
     * Returns instance ID
     *
     * @param context application context
     * @return instance Id
     */
    public static String getInstanceId(Context context) {
        if (context == null) {
            return null;
        }

        String instanceId = null;
        if (RcsSettings.getInstance().isImeiUsedAsDeviceId()) {
            String imei = getImei(context);
            if (imei != null) {
                instanceId = "\"<urn:gsma:imei:" + imei + ">\"";
            }
        } else {
            UUID uuid = getDeviceUUID(context);
            if (uuid != null) {
                instanceId = "\"<urn:uuid:" + uuid.toString() + ">\"";
            }
        }
        return instanceId;
    }

    /**
     * Returns the IMEI of the device
     *
     * @param context application context
     * @return IMEI of the device
     */
    private static String getImei(Context context) {
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getDeviceId();
    }
}
