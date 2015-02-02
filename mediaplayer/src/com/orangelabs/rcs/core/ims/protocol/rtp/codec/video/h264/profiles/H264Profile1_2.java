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

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.JavaPacketizer;

/**
 * Represent H264 Profile to Level 1.2
 *
 * @author Deutsche Telekom AG
 */
public class H264Profile1_2 extends H264Profile {

    /**
     * Profile name
     */
    public static final String PROFILE_NAME = "H264Profile1.2";

    /**
     * Profile Id
     * 42 (Baseline 66), 80 (Constrained Baseline), 0c (level 1.2)
     */
    public static final String BASELINE_PROFILE_ID = "42800c";

    private static final int BASELINE_PROFILE_BITRATE = 384000;

//    private static final int HIGH_PROFILE_BITRATE = 480000;

    private static final String base64CodeSPS = "J0KADJY1BYnI"; // QCIF
    // static final String base64CodeSPS = "J0KADJo1AoPy"; // QVGA

    private static final String base64CodePPS = "KM4C/IA="; // QCIF
    // static final String base64CodePPS = "KM4C/IA="; // QVGA

    private static String profileParams;

    static {
        profileParams = H264Config.CODEC_PARAM_PROFILEID + "=" + BASELINE_PROFILE_ID + ";" +
                        H264Config.CODEC_PARAM_PACKETIZATIONMODE + "=1;" +
                        H264Config.CODEC_PARAM_SPROP_PARAMETER_SETS + "=" +
                        base64CodeSPS + "," + base64CodePPS + ";";
    }

    /**
     * Constructor
     */
    public H264Profile1_2() {
        super(PROFILE_NAME,
                H264TypeLevel.LEVEL_1_2,
                H264TypeProfile.PROFILE_BASELINE,
                BASELINE_PROFILE_ID,
                176, 144, 15.0f, // QCIF
                // 320, 240, 20.0f, // QVGA
                BASELINE_PROFILE_BITRATE,
                JavaPacketizer.H264_MAX_PACKET_FRAME_SIZE,
                profileParams);
    } 
}
