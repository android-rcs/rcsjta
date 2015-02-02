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
 * Targeted level to encode
 *
 * @author Deutsche Telekom AG
 */
public enum H264TypeLevel {

    LEVEL_AUTODETECT(0, H264ConstraintSetFlagType.ANY),
    LEVEL_1(10, H264ConstraintSetFlagType.ANY),
    LEVEL_1B(11, H264ConstraintSetFlagType.TRUE),
    LEVEL_1_1(11, H264ConstraintSetFlagType.FALSE),
    LEVEL_1_2(12, H264ConstraintSetFlagType.ANY),
    LEVEL_1_3(13, H264ConstraintSetFlagType.ANY),
    LEVEL_2(20, H264ConstraintSetFlagType.ANY),
    LEVEL_2_1(21, H264ConstraintSetFlagType.ANY),
    LEVEL_2_2(22, H264ConstraintSetFlagType.ANY),
    LEVEL_3(30, H264ConstraintSetFlagType.ANY),
    LEVEL_3_1(31, H264ConstraintSetFlagType.ANY),
    LEVEL_3_2(32, H264ConstraintSetFlagType.ANY),
    LEVEL_4(40, H264ConstraintSetFlagType.ANY),
    LEVEL_4_1(41, H264ConstraintSetFlagType.ANY),
    LEVEL_4_2(42, H264ConstraintSetFlagType.ANY),
    LEVEL_5(50, H264ConstraintSetFlagType.ANY),
    LEVEL_5_1(51, H264ConstraintSetFlagType.ANY);

    /**
     * Level value
     */
    private int decimalValue;

    /**
     * Constraint Flag
     */
    private H264ConstraintSetFlagType constraintSet3Flag;

    /**
     * Constructor
     *
     * @param decimalValue level
     * @param constraintSet3Flag constraint Flag
     */
    private H264TypeLevel(int decimalValue, H264ConstraintSetFlagType constraintSet3Flag) {
        this.decimalValue = decimalValue;
        this.constraintSet3Flag = constraintSet3Flag;
    }

    /**
     * Get Level value
     *
     * @return level
     */
    public int getDecimalValue() {
        return decimalValue;
    }

    /**
     * Get Constraint Flag
     *
     * @return Constraint Flag
     */
    public H264ConstraintSetFlagType getH264ConstraintSet3Flag() {
        return constraintSet3Flag;
    }

    /**
     * Get H264TypeLevel
     *
     * @param decimalValue level
     * @param constraintSet3Flag constraint Flag
     * @return H264TypeLevel
     */
    public static H264TypeLevel getH264LevelType(int decimalValue, H264ConstraintSetFlagType constraintSet3Flag) {
        for (H264TypeLevel h264LevelType : H264TypeLevel.values()) {
            if ((h264LevelType.getDecimalValue() == decimalValue) &&
                    ((h264LevelType.getH264ConstraintSet3Flag() == H264ConstraintSetFlagType.ANY) ||
                    (h264LevelType.getH264ConstraintSet3Flag() == constraintSet3Flag))) {
                return h264LevelType;
            }
        }
        return null;
    }

    /**
     * Targeted constrains set flags to encode.
     */
    public enum H264ConstraintSetFlagType {
        ANY(0),
        FALSE(1),
        TRUE(2);

        /**
         * Constraint flag value
         */
        private int decimalValue;

        /**
         * Constructor
         *
         * @param decimalValue constraint flag value
         */
        private H264ConstraintSetFlagType(int decimalValue) {
            this.decimalValue = decimalValue;
        }

        /**
         * Get constraint flag value
         *
         * @return constraint flag
         */
        public int getDecimalValue() {
            return decimalValue;
        }
    }
}
