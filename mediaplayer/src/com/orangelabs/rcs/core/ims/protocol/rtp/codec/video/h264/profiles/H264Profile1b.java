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
 * Represent H264 Profile to Level 1b
 *
 * @author Deutsche Telekom AG
 */
public class H264Profile1b extends H264Profile {

    /**
     * Profile name
     */
    public static final String PROFILE_NAME = "H264Profile1b";

    /**
     * Profile Id
     * 42 (Baseline 66), 90 (Constrained baseline with level 1.b), 11 (level 1.b, because of the constraint_set3_flag)
     */
    public static final String BASELINE_PROFILE_ID = "42900b";

    private static final int BASELINE_PROFILE_BITRATE = 128000;

//    private static final int HIGH_PROFILE_BITRATE = 160000;

    private static final String base64CodeSPS = "J0KQCZY1BYnI";

    private static final String base64CodePPS = "KM4C/IA=";

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
    public H264Profile1b() {
        super(PROFILE_NAME,
                H264TypeLevel.LEVEL_1B,
                H264TypeProfile.PROFILE_BASELINE,
                BASELINE_PROFILE_ID,
                176, 144, 15.0f,
                BASELINE_PROFILE_BITRATE,
                JavaPacketizer.H264_MAX_PACKET_FRAME_SIZE,
                profileParams);
    }
}
