/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
    private static String productVersion = "v2.2";

    /**
     * RCS client version. Client Version Value = Platform "-" VersionMajor "." VersionMinor
     * Platform = Alphanumeric (max 9) VersionMajor = Number (2 char max) VersionMinor = Number (2
     * char max)
     */
    private static final String CLIENT_VERSION_PREFIX = "RCSAndr-";

    private static final String UNKNOWN = "unknown";

    private static String sClientVersion;

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
    public static String getProductVersion() {
        return productVersion;
    }

    /**
     * Set the product version
     * 
     * @param version Version
     */
    public static void setProductVersion(String version) {
        TerminalInfo.productVersion = version;
    }

    /**
     * Returns the product name + version
     * 
     * @return product information
     */
    public static String getProductInfo() {
        return productName + "/" + productVersion;
    }

    /**
     * Returns the client version as mentioned under versionName in AndroidManifest, prefixed with
     * CLIENT_VERSION_PREFIX.
     * <p>
     * In case versionName is not found under AndroidManifest it will default to
     * DEFAULT_CLIENT_VERSION.
     * </p>
     * 
     * @return Client version
     */
    public static String getClientVersion(Context ctx) {
        if (sClientVersion == null) {
            try {
                final String versionFromManifest = ctx.getPackageManager().getPackageInfo(
                        ctx.getPackageName(), 0).versionName;
                sClientVersion = new StringBuilder(CLIENT_VERSION_PREFIX).append(
                        versionFromManifest).toString();
            } catch (NameNotFoundException e) {
                if (sLogger.isActivated()) {
                    sLogger.error("Version Name not defined in Manifest", e);
                }
                sClientVersion = UNKNOWN;
            }
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
}
