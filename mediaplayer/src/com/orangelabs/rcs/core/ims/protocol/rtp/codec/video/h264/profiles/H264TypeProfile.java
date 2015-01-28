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

package com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles;

/**
 * Targeted profile to encode
 *
 * @author Deutsche Telekom AG
 */
public enum H264TypeProfile {

    /* Non-scalable profile */
    PROFILE_BASELINE(66),
    PROFILE_MAIN(77),
    PROFILE_EXTENDED(88),
    PROFILE_HIGH(100),
    PROFILE_HIGH10(110),
    PROFILE_HIGH422(122),
    PROFILE_HIGH444(244),
    PROFILE_CAVLC444(44);

    /**
     * Type value
     */
    public int decimalValue;

    /**
     * Constructor
     *
     * @param decimalValue type
     */
    private H264TypeProfile(int decimalValue) {
        this.decimalValue = decimalValue;
    }

    /**
     * Get Type
     *
     * @return type value
     */
    public int getDecimalValue() {
        return decimalValue;
    }

    /**
     * Get instance of {@link H264TypeProfile}, using decimal value
     *
     * @param decimalValue
     * @return {@link H264TypeProfile} if valid decimal, otherwise
     *         <code>null</code>
     */
    public static H264TypeProfile getH264ProfileType(int decimalValue) {
        for (H264TypeProfile h264ProfileType : H264TypeProfile.values()) {
            if (h264ProfileType.getDecimalValue() == decimalValue) {
                return h264ProfileType;
            }
        }
        return null;
    }
}
