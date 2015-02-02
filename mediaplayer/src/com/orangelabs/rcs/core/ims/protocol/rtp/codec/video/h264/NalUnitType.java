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
 * H264 NAL Unit Types
 *
 * @author Deutsche Telekom
 */
public enum NalUnitType {

    RESERVED,
    CODE_SLICE_NON_IDR_PICTURE,
    CODE_SLICE_DATA_PARTITION_A,
    CODE_SLICE_DATA_PARTITION_B,
    CODE_SLICE_DATA_PARTITION_C,
    CODE_SLICE_IDR_PICTURE,
    SEQUENCE_PARAMETER_SET,
    PICTURE_PARAMETER_SET,
    STAP_A,
    STAP_B,
    MTAP16,
    MTAP24,
    FU_A,
    FU_B,
    OTHER_NAL_UNIT;

    /**
     * Decodes the NAL Unit type
     *
     * @param value NAL value
     * @return NAL Unit Type
     */
    public static NalUnitType parse(int value) {
        switch (value) {
            case 1:
                return CODE_SLICE_NON_IDR_PICTURE;

            case 2:
                return CODE_SLICE_DATA_PARTITION_A;

            case 3:
                return CODE_SLICE_DATA_PARTITION_B;

            case 4:
                return CODE_SLICE_DATA_PARTITION_C;

            case 5:
                return CODE_SLICE_IDR_PICTURE;

            case 7:
                return SEQUENCE_PARAMETER_SET;

            case 8:
                return PICTURE_PARAMETER_SET;

            case 24:
                return STAP_A;

            case 25:
                return STAP_B;

            case 26:
                return MTAP16;

            case 27:
                return MTAP24;

            case 28:
                return FU_A;

            case 29:
                return FU_B;

            case 0:
            case 30:
            case 31:
                return RESERVED;

            default:
                return OTHER_NAL_UNIT;
        }
    }
}
