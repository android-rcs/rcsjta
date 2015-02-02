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
 * RFC 6184 RTP Payload Format for H.264 Video
 *
 * The first byte of the H264 payload represents the NAL Unit which has the following format:
 *
 *     +---------------+
 *     |0|1|2|3|4|5|6|7|
 *     +-+-+-+-+-+-+-+-+
 *     |F|NRI|  Type   |
 *     +---------------+
 *
 *  F:       1 bit
 *           forbidden_zero_bit.  The H.264 specification declares a
 *           value of 1 as a syntax violation.
 *
 *  NRI:     2 bits
 *           nal_ref_idc.  A value of 00 indicates that the content of
 *           the NAL unit is not used to reconstruct reference pictures
 *           for inter picture prediction.  Such NAL units can be
 *           discarded without risking the integrity of the reference
 *           pictures.  Values greater than 00 indicate that the decoding
 *           of the NAL unit is required to maintain the integrity of the
 *           reference pictures.
 *
 *   Type:   5 bits
 *           nal_unit_type.  This component specifies the NAL unit
 *           payload type
 *
 * @author Deutsche Telekom
 */
public class NalUnitHeader {

    /**
     * Forbidden zero bit
     */
    private boolean forbiddenZeroBit;

    /**
     * NAL Reference id
     */
    private int nalRefId;

    /**
     * NAL Unit Type
     */
    private NalUnitType decodeNalUnitType;

    /**
     * Class constructor
     *
     * @param forbiddenZeroBit Forbidden zero bit
     * @param nalRefId NAL Reference id
     * @param nalUnitType NAL Unit Type value
     */
    private NalUnitHeader(boolean forbiddenZeroBit, int nalRefId, int nalUnitType) {
        this.forbiddenZeroBit = forbiddenZeroBit;
        this.nalRefId = nalRefId;
        this.decodeNalUnitType = NalUnitType.parse(nalUnitType);
    }

    /**
     * Checks if the Forbidden Zero Bit is set.
     *
     * @return <code>True</code> if it is, <code>false</code> false otherwise.
     */
    public boolean isForbiddenBitSet() {
        return forbiddenZeroBit;
    }

    /**
     * Gets the NAL Reference ID
     *
     * @return NAL Reference ID
     */
    public int getNalRefId() {
        return nalRefId;
    }

    /**
     * Gets the NAL Unit Type
     *
     * @return
     */
    public NalUnitType getNalUnitType() {
        return decodeNalUnitType;
    }

    /**
     * Verifies if the H264 packet is Single NAL Unit
     *
     * @return <code>True</code> if it is, <code>false</code> false otherwise.
     */
    public boolean isSingleNalUnitPacket() {
        return decodeNalUnitType == NalUnitType.CODE_SLICE_IDR_PICTURE
                || decodeNalUnitType == NalUnitType.CODE_SLICE_NON_IDR_PICTURE
                || decodeNalUnitType == NalUnitType.CODE_SLICE_DATA_PARTITION_A
                || decodeNalUnitType == NalUnitType.CODE_SLICE_DATA_PARTITION_B
                || decodeNalUnitType == NalUnitType.CODE_SLICE_DATA_PARTITION_C
                || decodeNalUnitType == NalUnitType.SEQUENCE_PARAMETER_SET
                || decodeNalUnitType == NalUnitType.PICTURE_PARAMETER_SET
                || decodeNalUnitType == NalUnitType.OTHER_NAL_UNIT;
    }

    /**
     * Verifies if the H264 packet is an Aggregation Packet
     *
     * @return <code>True</code> if it is, <code>false</code> false otherwise.
     */
    public boolean isAggregationPacket() {
        return decodeNalUnitType == NalUnitType.STAP_A || decodeNalUnitType == NalUnitType.STAP_B
                || decodeNalUnitType == NalUnitType.MTAP16
                || decodeNalUnitType == NalUnitType.MTAP24;
    }

    /**
     * Verifies if the H264 packet is a Fragmentation Unit Packet
     *
     * @return <code>True</code> if it is, <code>false</code> false otherwise.
     */
    public boolean isFragmentationUnit() {
        return decodeNalUnitType == NalUnitType.FU_A || decodeNalUnitType == NalUnitType.FU_B;
    }

    /**
     * Extracts the NAL Unit header from a H264 Packet
     *
     * @param h264Packet H264 Packet
     * @return {@link NalUnitHeader} Extracted NAL Unit Header
     * @throws {@link RuntimeException} If the H264 packet data is null
     */
    public static NalUnitHeader extract(byte[] h264Packet) {
        if (h264Packet == null) {
            throw new RuntimeException("Cannot extract H264 header. Invalid H264 packet");
        }

        NalUnitHeader header = new NalUnitHeader(false, 0, 0);
        extract(h264Packet, header);

        return header;
    }

    /**
     * Extracts the NAL Unit header from a H264 Packet. Puts the extracted info
     * in the given header object
     *
     * @param h264Packet H264 packet
     * @param header Header object to fill with data
     * @throws {@link RuntimeException} If the H264 packet data is null or the
     *         header is null;
     */
    public static void extract(byte[] h264Packet, NalUnitHeader header) {
        if (h264Packet == null) {
            throw new RuntimeException("Cannot extract H264 header. Invalid H264 packet");
        }

        if (header == null) {
            throw new RuntimeException("Cannot extract H264 header. Invalid header packet");
        }

        byte headerByte = h264Packet[0];

        header.forbiddenZeroBit = ((headerByte & 0x80) >> 7) != 0;
        header.nalRefId = ((headerByte & 0x60) >> 5);
        int nalUnitType = (headerByte & 0x1f);
        header.decodeNalUnitType = NalUnitType.parse(nalUnitType);
    }

    /**
     * Extracts the NAL Unit header from a H264 Packet
     *
     * @param h264Packet H264 Packet
     * @return {@link NalUnitHeader} Extracted NAL Unit Header
     * @throws {@link RuntimeException} If the H264 packet data is null
     */
    public static NalUnitHeader extract(int position, byte[] h264Packet) {
        if (h264Packet == null) {
            throw new RuntimeException("Cannot extract H264 header. Invalid H264 packet");
        }

        NalUnitHeader header = new NalUnitHeader(false, 0, 0);
        extract(position, h264Packet, header);

        return header;
    }

    /**
     * Extracts the NAL Unit header from a H264 Packet. Puts the extracted info
     * in the given header object
     *
     * @param h264Packet H264 packet
     * @param header Header object to fill with data
     * @throws {@link RuntimeException} If the H264 packet data is null or the
     *         header is null;
     */
    public static void extract(int position, byte[] h264Packet, NalUnitHeader header) {
        if (h264Packet == null) {
            throw new RuntimeException("Cannot extract H264 header. Invalid H264 packet");
        }

        if (header == null) {
            throw new RuntimeException("Cannot extract H264 header. Invalid header packet");
        }

        byte headerByte = h264Packet[position];

        header.forbiddenZeroBit = ((headerByte & 0x80) >> 7) != 0;
        header.nalRefId = ((headerByte & 0x60) >> 5);
        int nalUnitType = (headerByte & 0x1f);
        header.decodeNalUnitType = NalUnitType.parse(nalUnitType);
    }
}
