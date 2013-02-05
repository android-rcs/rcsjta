/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.core.ims.protocol.rtp.codec.video.h264;

/**
 * Class H264Config.
 */
public class H264Config {
    /**
     * Constant QCIF_WIDTH.
     */
    public static final int QCIF_WIDTH = 176;

    /**
     * Constant QCIF_HEIGHT.
     */
    public static final int QCIF_HEIGHT = 144;

    /**
     * Constant CIF_WIDTH.
     */
    public static final int CIF_WIDTH = 352;

    /**
     * Constant CIF_HEIGHT.
     */
    public static final int CIF_HEIGHT = 288;

    /**
     * Constant QVGA_WIDTH.
     */
    public static final int QVGA_WIDTH = 320;

    /**
     * Constant QVGA_HEIGHT.
     */
    public static final int QVGA_HEIGHT = 240;

    /**
     * Constant VGA_WIDTH.
     */
    public static final int VGA_WIDTH = 640;

    /**
     * Constant VGA_HEIGHT.
     */
    public static final int VGA_HEIGHT = 480;

    /**
     * Constant CODEC_NAME.
     */
    public static final String CODEC_NAME = "H264";

    /**
     * Constant CLOCK_RATE.
     */
    public static final int CLOCK_RATE = 90000;

    /**
     * Constant CODEC_PARAM_PROFILEID.
     */
    public static final String CODEC_PARAM_PROFILEID = "profile-level-id";

    /**
     * Constant CODEC_PARAM_PACKETIZATIONMODE.
     */
    public static final String CODEC_PARAM_PACKETIZATIONMODE = "packetization-mode";

    /**
     * Constant CODEC_PARAM_SPROP_PARAMETER_SETS.
     */
    public static final String CODEC_PARAM_SPROP_PARAMETER_SETS = "sprop-parameter-sets";

    /**
     * Constant CODEC_PARAMS.
     */
    public static final String CODEC_PARAMS = "profile-level-id=42900b;packetization-mode=1";

    /**
     * Constant VIDEO_WIDTH.
     */
    public static final int VIDEO_WIDTH = 176;

    /**
     * Constant VIDEO_HEIGHT.
     */
    public static final int VIDEO_HEIGHT = 144;

    /**
     * Constant FRAME_RATE.
     */
    public static final int FRAME_RATE = 15;

    /**
     * Constant BIT_RATE.
     */
    public static final int BIT_RATE = 64000;

    /**
     * Creates a new instance of H264Config.
     */
    public H264Config() {

    }

    /**
     * Returns the codec profile level id.
     *  
     * @param arg1 The arg1.
     * @return  The codec profile level id.
     */
    public static String getCodecProfileLevelId(String arg1) {
        return (java.lang.String) null;
    }

    /**
     * Returns the codec packetization mode.
     *  
     * @param arg1 The arg1.
     * @return  The codec packetization mode.
     */
    public static int getCodecPacketizationMode(String arg1) {
        return 0;
    }

} // end H264Config
