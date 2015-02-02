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

/**
 * H264RtpHeaders
 *
 * RFC 3984: Two special headers are added to each H264 packet that
 * immediately follows the RTP header:
 *
 * First Header - The FU indicator octet has the following format:
 * +---------------+
 * |0|1|2|3|4|5|6|7|
 * +-+-+-+-+-+-+-+-+
 * |F|NRI|  Type   |
 * +---------------+
 *
 * Second Header - The FU header has the following format:
 * +---------------+
 * |0|1|2|3|4|5|6|7|
 * +-+-+-+-+-+-+-+-+
 * |S|E|R|  Type   |
 * +---------------+
 *
 * @author Deutsche Telekom AG
 */
public class H264RtpHeaders {

    /**
     * AVC NAL picture parameter
     */
    public static final int AVC_NALTYPE_FUA = 28;

    private final static int FU_INDICATOR_SIZE = 1;
    private final static int FU_HEADER_SIZE = 1;

    /**
     * First Header - The FU indicator octet
     */
    private boolean FUI_F;
    private int FUI_NRI;
    private byte FUI_TYPE;

    /**
     * Second Header - The FU header
     */
    private boolean FUH_S;
    private boolean FUH_E;
    private boolean FUH_R;
    private byte FUH_TYPE;

    private boolean hasFUHeader;

    /**
     * Constructor
     *
     * @param rtpPacketData
     */
    public H264RtpHeaders(byte[] rtpPacketData) {
        // Get FU indicator
        byte data_FUI = rtpPacketData[0];
        this.FUI_F = ((data_FUI >> 7) & 0x01) != 0;
        this.FUI_NRI = ((data_FUI >> 5) & 0x07);
        this.FUI_TYPE = (byte) (data_FUI & 0x1f);
        this.hasFUHeader = false;

        if (FUI_TYPE == AVC_NALTYPE_FUA) {
            // Get FU header
            byte data_FUH = rtpPacketData[1];
            this.FUH_S = (data_FUH & 0x80) != 0;
            this.FUH_E = (data_FUH & 0x40) != 0;
            this.FUH_R = (data_FUH & 0x20) != 0;
            this.FUH_TYPE = (byte) (data_FUH & 0x1f);
            this.hasFUHeader = true;
        }
    }

    /**
     * Is Frame Non Interleaved
     *
     * @return Is Frame Non Interleaved
     */
    public boolean isFrameNonInterleaved() { // not fragmented
        return (FUI_TYPE == AVC_NALTYPE_FUA);
    }

    /**
     * Header Size
     *
     * @return Header Size
     */
    public int getHeaderSize() {
        int headerSize = FU_INDICATOR_SIZE;
        if (hasFUHeader) {
            headerSize += FU_HEADER_SIZE;
        }
        return headerSize;
    }

    /**
     * Get NAL Header
     *
     * @return NAL Header
     */
    public byte getNALHeader() {
        // Compose and copy NAL header
        if (hasFUHeader) {
            return (byte) (((getFUI_F() ? 1 : 0) << 7) | (FUI_NRI << 5) | (FUH_TYPE & 0x1F));
        } else {
            return (byte) (((getFUI_F() ? 1 : 0) << 7) | (FUI_NRI << 5) | (FUI_TYPE & 0x1F));
        }
    }

    /**
     * Verifies if packet is a code slice of a IDR picture
     *
     * @param packet packet to verify
     * @return <code>True</code> if it is, <code>false</code> otherwise
     */
    public boolean isIDRSlice() {
        if (FUI_TYPE == (byte) 0x05) {
            return true;
        }

        if (isFrameNonInterleaved() && FUH_TYPE == (byte) 0x05) {
            return true;
        }

        return false;
    }

    /**
     * Verifies if packet is a code slice of a NON IDR picture
     *
     * @param packet packet to verify
     * @return <code>True</code> if it is, <code>false</code> otherwise
     */
    public boolean isNonIDRSlice() {
        if (FUI_TYPE == (byte) 0x01) {
            return true;
        }
        
        if (isFrameNonInterleaved() && FUH_TYPE == (byte) 0x01) {
            return true;
        }
        
        return false;
    }

    /**
     * Get FUI_F
     *
     * @return FUI_F
     */
    public boolean getFUI_F() {
        return FUI_F;
    }

    /**
     * Get FUI_NRI
     *
     * @return FUI_NRI
     */
    public int getFUI_NRI() {
        return FUI_NRI;
    }

    /**
     * Get FUI_TYPE
     *
     * @return FUI_TYPE
     */
    public byte getFUI_TYPE() {
        return FUI_TYPE;
    }

    /**
     * Get FUH_S
     *
     * @return FUH_S
     */
    public boolean getFUH_S() {
        return FUH_S;
    }

    /**
     * Get FUH_E
     *
     * @return FUH_E
     */
    public boolean getFUH_E() {
        return FUH_E;
    }

    /**
     * Get FUH_R
     *
     * @return FUH_R
     */
    public boolean getFUH_R() {
        return FUH_R;
    }

    /**
     * Get FUH_TYPE
     *
     * @return FUH_TYPE
     */
    public byte getFUH_TYPE() {
        return FUH_TYPE;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("[FUI_F = " + getFUI_F() + " ");
        result.append("FUI_NRI = " + FUI_NRI + " ");
        result.append("FUI_TYPE = " + FUI_TYPE + " ");
        result.append("hasFUHeader = " + hasFUHeader + " ");

        if (hasFUHeader) {
            result.append("[FUH_S = " + FUH_S + " ");
            result.append("[FUH_E = " + FUH_E + " ");
            result.append("[FUH_R = " + FUH_R + " ");
            result.append("[FUH_TYPE = " + FUH_TYPE + " ");
        }

        result.append("HeaderSize = " + getHeaderSize() + "]");

        return result.toString();
    }
}
