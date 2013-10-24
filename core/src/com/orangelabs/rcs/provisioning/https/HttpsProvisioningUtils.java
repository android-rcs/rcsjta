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

package com.orangelabs.rcs.provisioning.https;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.StringTokenizer;

import android.content.Context;
import android.content.pm.PackageInfo;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.utils.HttpUtils;
import com.orangelabs.rcs.utils.StringUtils;

/**
 * HTTPS provisioning - utils
 *
 * @author Orange
 */
public class HttpsProvisioningUtils {
    /**
     * Intent key
     */
    public static final String FIRST_KEY = "first";

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
     * Retry after an 511 "Network authentication required" timeout (in
     * milliseconds)
     */
    protected static final int RETRY_AFTER_511_ERROR_TIMEOUT = 5000;

    /**
     * The action if a binary SMS received
     */
    protected static final String ACTION_BINARY_SMS_RECEIVED = "android.intent.action.DATA_SMS_RECEIVED";

    /**
     * Retry max count
     */
    protected static final int RETRY_MAX_COUNT = 5;

    /**
     * Retry after 511 "Network authentication required" max count
     */
    protected static final int RETRY_AFTER_511_ERROR_MAX_COUNT = 5;

    /**
     * Get the current device language
     * 
     * @return device language (like fr-FR)
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

    /**
     * Returns the client vendor
     * 
     * @return String(4)
     */
    protected static String getClientVendorFromContext(Context context) {
        String result = HttpsProvisioningUtils.UNKNOWN;
        String version = context.getString(R.string.rcs_client_vendor);
        if (version != null && version.length() > 0) {
            result = version;
        }
        return StringUtils.truncate(result, 4);
    }

    /**
     * Returns the client version see RCS-e implementation guideline v3.1 at
     * ID_2_4 (page 12)
     * 
     * @return String(15)
     */
    protected static String getClientVersionFromContext(Context context) {
        String result = HttpsProvisioningUtils.UNKNOWN;
        try {
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            result = new StringTokenizer(pinfo.versionName, " ").nextToken();
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            result = HttpsProvisioningUtils.UNKNOWN;
        } catch (java.util.NoSuchElementException e) {
            result = HttpsProvisioningUtils.UNKNOWN;
        }
        return HttpUtils.encodeURL(StringUtils.truncate(result, 15));
    }

    /**
     * Returns the terminal vendor
     * 
     * @return String(4)
     */
    protected static String getTerminalVendor() {
        String result = HttpsProvisioningUtils.UNKNOWN;
        String productmanufacturer = getSystemProperties("ro.product.manufacturer");
        if (productmanufacturer != null && productmanufacturer.length() > 0) {
            result = productmanufacturer;
        }
        return StringUtils.truncate(result, 4);
    }

    /**
     * Returns the terminal model
     * 
     * @return String(10)
     */
    protected static String getTerminalModel() {
        String result = HttpsProvisioningUtils.UNKNOWN;
        String devicename = getSystemProperties("ro.product.device");
        if (devicename != null && devicename.length() > 0) {
            result = devicename;
        }
        return StringUtils.truncate(result, 10);
    }

    /**
     * Returns the terminal software version
     * 
     * @return String(10)
     */
    protected static String getTerminalSoftwareVersion() {
        String result = HttpsProvisioningUtils.UNKNOWN;
        String productversion = getSystemProperties("ro.product.version");
        if (productversion != null && productversion.length() > 0) {
            result = productversion;
        }
        return StringUtils.truncate(result, 10);
    }

    /**
     * Returns a system parameter
     * 
     * @param key Key parameter
     * @return Parameter value
     */
    protected static String getSystemProperties(String key) {
        String value = null;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            value = (String) get.invoke(c, key);
            return value;
        } catch (Exception e) {
            return HttpsProvisioningUtils.UNKNOWN;
        }
    }
}
