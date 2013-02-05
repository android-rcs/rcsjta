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
 * Class JavaPacketizer.
 */
public class JavaPacketizer extends org.gsma.rcs.core.ims.protocol.rtp.codec.video.VideoCodec {
    /**
     * Constant H264_ENABLED_PACKETIZATION_MODE_1.
     */
    public static final boolean H264_ENABLED_PACKETIZATION_MODE_1 = true;

    /**
     * Constant H264_MAX_RTP_PKTS.
     */
    public static final int H264_MAX_RTP_PKTS = 32;

    /**
     * Constant H264_FU_HEADER_SIZE.
     */
    public static final int H264_FU_HEADER_SIZE = 2;

    /**
     * Constant AVC_NALTYPE_SPS.
     */
    public static final int AVC_NALTYPE_SPS = 7;

    /**
     * Constant AVC_NALTYPE_PPS.
     */
    public static final int AVC_NALTYPE_PPS = 8;

    /**
     * The h264_ m a x_ p a c k e t_ f r a m e_ s i z e.
     */
    public static int H264_MAX_PACKET_FRAME_SIZE;

    /**
     * Creates a new instance of JavaPacketizer.
     */
    public JavaPacketizer() {
        super();
    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The int.
     */
    public int process(org.gsma.rcs.core.ims.protocol.rtp.util.Buffer arg1, org.gsma.rcs.core.ims.protocol.rtp.util.Buffer arg2) {
        return 0;
    }

} // end JavaPacketizer
