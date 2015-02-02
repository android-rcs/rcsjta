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

package com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Default H264 Settings
 *
 * @author hlxn7157
 * @author Deutsche Telekom AG
 */
public class H264Config {

    /** Constant values */
    public static final int QCIF_WIDTH = 176;
    public static final int QCIF_HEIGHT = 144;

    public static final int CIF_WIDTH = 352;
    public static final int CIF_HEIGHT = 288;

    public static final int QVGA_WIDTH = 320;
    public static final int QVGA_HEIGHT = 240;

    public static final int VGA_WIDTH = 640;
    public static final int VGA_HEIGHT = 480;

    /**
     * Codec name
     */
    public final static String CODEC_NAME = "H264";

    /**
     * Default clock rate
     */
    public final static int CLOCK_RATE = 90000;

    /**
     * H264 OPTIONAL payload format parameter "profile-level-id" - RFC 3984
     */
    public static final String CODEC_PARAM_PROFILEID = "profile-level-id";

    /**
     * H264 OPTIONAL payload format parameter "packetization-mode" - RFC 3984
     */
    public static final String CODEC_PARAM_PACKETIZATIONMODE = "packetization-mode";

    /**
     * H264 OPTIONAL payload format parameter "sprop-parameter-sets" - RFC 3984
     */
    public static final String CODEC_PARAM_SPROP_PARAMETER_SETS = "sprop-parameter-sets";

    /**
     * Default codec params
     */
    public final static String CODEC_PARAMS = "profile-level-id=42900b;packetization-mode=1";

    /**
     * Default video width
     */
    public final static int VIDEO_WIDTH = QCIF_WIDTH;

    /**
     * Default video height
     */
    public final static int VIDEO_HEIGHT = QCIF_HEIGHT;

    /**
     * Default video frame rate
     */
    public final static int FRAME_RATE = 15;

    /**
     * Default video bit rate
     */
    public final static int BIT_RATE = 64000;

    /**
     * Get value of packetization mode
     *
     * @param codecParams
     * @return 
     */
    public static int getCodecPacketizationMode(String codecParams) {
        int packetization_mode = 0;
        String valPackMode = getParameterValue(CODEC_PARAM_PACKETIZATIONMODE, codecParams);
        if (valPackMode != null) {
            try {
                packetization_mode = Integer.parseInt(valPackMode);
            } catch (Exception e) {
            }
        }
        return packetization_mode;
    }

    /**
     * Get value of profile level ID
     *
     * @param codecParams
     * @return
     */
    public static String getCodecProfileLevelId(String codecParams) {
        return getParameterValue(CODEC_PARAM_PROFILEID, codecParams);
    }

    /**
     * Get parameter value from SDP parameters string with parameter-value
     * format 'key1=value1; ... keyN=valueN'
     *
     * @param paramKey parameter name
     * @param params parameters string
     * @return if parameter exists return {@link String} with value, otherwise
     *         return <code>null</code>
     */
    private static String getParameterValue(String paramKey, String params) {
        String value = null;
        if (params != null && params.length() > 0) {
            try {
                Pattern p = Pattern.compile("(?<=" + paramKey + "=).*?(?=;|$)");
                Matcher m = p.matcher(params);
                if (m.find()) {
                    value = m.group(0);
                }
            } catch (PatternSyntaxException e) {
                // Nothing to do
            }
        }
        return value;
    }
}
