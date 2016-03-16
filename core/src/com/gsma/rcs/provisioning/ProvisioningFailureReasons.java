/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.provisioning;

/**
 * Provisioning failure reasons
 * 
 * @author Deutsche Telekom AG
 */
public class ProvisioningFailureReasons {

    /**
     * Action string for provisioning failure
     */
    public static final String ACTION_HTTPS_PROVISIONING_FAILS = "ProvisioningFailed";

    /**
     * Key string for provisioning failure reason in Intent's extra
     */
    public static final String EXTRA_PROVISIONING_FAILURE_REASON = "ProvisioningFailedReason";

    /**
     * Provisioning is blocked with this account
     */
    public static final int PROVISIONING_IS_BLOCKED_WITH_THIS_ACCOUNT = 0;

    /**
     * Invalid configuration (error in parsing the configuration document)
     */
    public static final int INVALID_CONFIGURATION = 1;

    /**
     * Unable to get configuration (503 "Service unavailable " on provisioning as a first launch)
     */
    public static final int UNABLE_TO_GET_CONFIGURATION = 2;

    /**
     * Provisioning forbidden (403 "Forbidden")
     */
    public static final int PROVISIONING_FORBIDDEN = 3;

    /**
     * Provisioning authentication required (511 "Network authentication required")
     */
    public static final int PROVISIONING_AUTHENTICATION_REQUIRED = 4;

    /**
     * No connectivity to HCS/RCS server
     */
    public static final int CONNECTIVITY_ISSUE = 5;

    /**
     * No configuration present (other error occurs)
     */
    public static final int NO_CONFIGURATION_PRESENT = 6;

    /**
     * No connectivity at all (Hotspot IP but not logged in)
     */
    public static final int SRV_SERVICE_UNAVAILABLE = 7;
}
