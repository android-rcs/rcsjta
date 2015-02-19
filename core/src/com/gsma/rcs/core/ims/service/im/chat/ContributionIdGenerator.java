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

package com.gsma.rcs.core.ims.service.im.chat;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.DeviceUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsServiceException;

/**
 * Contribution ID generator based on RFC draft-kaplan-dispatch-session-id-03
 * 
 * @author jexa7410
 */
public class ContributionIdGenerator {
    /**
     * The logger
     */
    private static Logger logger = Logger.getLogger(ContributionIdGenerator.class.getName());

    /**
     * Secret Key generator.
     */
    private static byte[] generateSecretKey() {
        final byte[] rawKey = generateRawKey();
        /**
         * Keep only 128 bits
         */
        byte[] secretKey = new byte[16];
        for (int i = 0; i < 16; i++) {
            if (rawKey != null && rawKey.length >= 16) {
                secretKey[i] = rawKey[i];
            } else {
                secretKey[i] = '0';
            }
        }
        return secretKey;
    }

    /**
     * Raw Key generator.
     */
    private static byte[] generateRawKey() {
        try {
            // Get device ID
            final UUID uuid = DeviceUtils.getDeviceUUID(AndroidFactory.getApplicationContext());
            return uuid.toString().getBytes(UTF8);
        } catch (RcsServiceException e) {
            if (logger.isActivated()) {
                logger.error(new StringBuilder(
                        "Exception caught in ContributionIdGenerator while generating Raw secret key; exception-msg=")
                        .append(e.getMessage()).append("!").toString());
            }
            return String.valueOf(System.currentTimeMillis()).getBytes(UTF8);
        }
    }

    /**
     * Returns the Contribution ID
     * 
     * @param callId Call-ID header value
     * @return the Contribution ID
     */
    public synchronized static String getContributionId(String callId) {
        try {
            // HMAC-SHA1 operation
            SecretKeySpec sks = new SecretKeySpec(generateSecretKey(), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(sks);
            byte[] contributionId = mac.doFinal(callId.getBytes(UTF8));

            // Convert to Hexa and keep only 128 bits
            StringBuilder hexString = new StringBuilder(32);
            for (int i = 0; i < 16 && i < contributionId.length; i++) {
                String hex = Integer.toHexString(0xFF & contributionId[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String id = hexString.toString();
            return id;
        } catch (Exception e) {
            return null;
        }
    }
}
