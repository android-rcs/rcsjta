/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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

package com.gsma.rcs.provisioning.https;

import java.util.Locale;

/**
 * HTTPS provisioning - utils
 * 
 * @author Orange
 */
public class HttpsProvisioningUtils {

    /**
     * Input MSISDN timeout
     */
    protected static final int INPUT_MSISDN_TIMEOUT = 30000;

    /**
     * Unknown value
     */
    protected static final String UNKNOWN = "unknown";

    /**
     * Retry base timeout - 5min
     */
    protected static final int RETRY_BASE_TIMEOUT = 300000;

    /**
     * Retry after an 511 "Network authentication required" timeout (in milliseconds)
     */
    protected static final int RETRY_AFTER_511_ERROR_TIMEOUT = 5000;

    /**
     * The action if a binary SMS received
     */
    public static final String ACTION_BINARY_SMS_RECEIVED = "android.intent.action.DATA_SMS_RECEIVED";

    /**
     * Char sequence in a binary SMS to indicate a network initiated configuration
     */
    public static final String RESET_CONFIG_SUFFIX = "-rcscfg";

    /**
     * Retry max count
     */
    protected static final int RETRY_MAX_COUNT = 5;

    /**
     * Retry after 511 "Network authentication required" max count
     */
    protected static final int RETRY_AFTER_511_ERROR_MAX_COUNT = 5;

    /**
     * Default SMS port
     */
    public static final int DEFAULT_SMS_PORT = 37273;

    /**
     * Get the current device language
     * 
     * @return Device language (like fr-FR)
     */
    protected static String getUserLanguage() {
        return Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
    }

    /**
     * Returns the RCS version
     * 
     * @return String(4)
     */
    protected static String getRcsVersion() {
        return "5.1B";
    }

    /**
     * Returns the RCS profile
     * 
     * @return String(15)
     */
    protected static String getRcsProfile() {
        return "joyn_blackbird";
    }
}
