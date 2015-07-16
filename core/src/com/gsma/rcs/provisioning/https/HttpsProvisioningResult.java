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

package com.gsma.rcs.provisioning.https;

/**
 * HTTPS provisioning result
 * 
 * @author jexa7410
 * @author Deutsche Telekom
 */
public class HttpsProvisioningResult {

    public static int UNKNOWN_REASON_CODE = -1;

    public static int UNKNOWN_MSISDN_CODE = -2;

    /**
     * Return code
     */
    public int code = UNKNOWN_REASON_CODE;

    /**
     * Value of header RetryAfter
     */
    public long retryAfter = 0;

    /**
     * Response content
     */
    public String content = null;

    /**
     * Controls if is waiting for the SMS with the one time password (OTP)
     */
    public boolean waitingForSMSOTP = false;
}
