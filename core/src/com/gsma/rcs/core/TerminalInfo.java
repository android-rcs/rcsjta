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

package com.gsma.rcs.core;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

/**
 * Terminal information
 * 
 * @author JM. Auffret
 * @author Deutsche Telekom AG
 */
public class TerminalInfo {

    private static final Logger sLogger = Logger.getLogger(TerminalInfo.class.getName());

    /**
     * Product name
     */
    private static final String productName = "RCS-client";

    /**
     * Product version
     */
    private static String sProductVersion;

    /**
     * RCS client version. Client Version Value = Platform "-" VersionMajor "." VersionMinor
     * Platform = Alphanumeric (max 9) VersionMajor = Number (2 char max) VersionMinor = Number (2
     * char max)
     */
    private static final String CLIENT_VERSION_PREFIX = "RCSAndr-";

    private static final String UNKNOWN = "unknown";

    private static final char FORWARD_SLASH = '/';

    private static final char HYPHEN = '-';

    private static String sClientVersion;

    private static String sBuildInfo;

    private static String sClientInfo;

    /**
     * Returns the product name
     * 
     * @return Name
     */
    public static String getProductName() {
        return productName;
    }

    /**
     * Returns the product version
     * 
     * @return Version
     */
    public static String getProductVersion(Context ctx) {
        if (sProductVersion == null) {
            try {
                sProductVersion = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
            } catch (NameNotFoundException e) {
                if (sLogger.isActivated()) {
                    sLogger.error("Version Name not defined in Manifest", e);
                }
                sProductVersion = UNKNOWN;
            }
        }
        return sProductVersion;
    }

    /**
     * Returns the product name + version
     * 
     * @return product information
     */
    public static String getProductInfo() {
        return new StringBuilder(productName).append(FORWARD_SLASH).append(sProductVersion)
                .toString();
    }

    /**
     * Returns the client version as mentioned under versionName in AndroidManifest, prefixed with
     * CLIENT_VERSION_PREFIX.
     * <p>
     * In case versionName is not found under AndroidManifest it will default to UNKNOWN.
     * </p>
     * 
     * @param ctx
     * @return Client version
     */
    public static String getClientVersion(Context ctx) {
        if (sClientVersion == null) {
            sClientVersion = new StringBuilder(CLIENT_VERSION_PREFIX)
                    .append(getProductVersion(ctx)).toString();
        }
        return sClientVersion;
    }

    /**
     * Returns the client vendor
     * 
     * @return Build.MANUFACTURER
     */
    public static String getClientVendor() {
        return (Build.MANUFACTURER != null) ? Build.MANUFACTURER : UNKNOWN;
    }

    /**
     * Returns the terminal vendor
     * 
     * @return Build.MANUFACTURER
     */
    public static String getTerminalVendor() {
        return (Build.MANUFACTURER != null) ? Build.MANUFACTURER : UNKNOWN;
    }

    /**
     * Returns the terminal model
     * 
     * @return Build.DEVICE
     */
    public static String getTerminalModel() {
        return (Build.DEVICE != null) ? Build.DEVICE : UNKNOWN;
    }

    /**
     * Returns the terminal software version
     * 
     * @return Build.DISPLAY
     */
    public static String getTerminalSoftwareVersion() {
        return (Build.DISPLAY != null) ? Build.DISPLAY : UNKNOWN;
    }

    /**
     * Get the build info
     * 
     * @return build info
     */
    public static String getBuildInfo() {
        if (sBuildInfo == null) {
            final String buildVersion = new StringBuilder(getTerminalModel()).append(HYPHEN)
                    .append(getTerminalSoftwareVersion()).toString();
            sBuildInfo = new StringBuilder(getTerminalVendor()).append(FORWARD_SLASH)
                    .append(buildVersion).toString();
        }
        return sBuildInfo;
    }

    /**
     * Returns the client_vendor '/' client_version
     * 
     * @return client information
     */
    public static String getClientInfo() {
        if (sClientInfo == null) {
            sClientInfo = new StringBuilder(getClientVendor()).append(FORWARD_SLASH)
                    .append(getClientVersion(AndroidFactory.getApplicationContext())).toString();
        }
        return sClientInfo;
    }
}
