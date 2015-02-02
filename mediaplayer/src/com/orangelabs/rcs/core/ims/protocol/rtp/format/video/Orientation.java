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

package com.orangelabs.rcs.core.ims.protocol.rtp.format.video;

/**
 * Represents the Video frame orientation
 *
 * @author Deutsche Telekom
 */
public enum Orientation {
    /**
     * Orientation values
     */
    NONE(0),
    ROTATE_90_CW(1),
    ROTATE_180(2),
    ROTATE_90_CCW(3),
    FLIP_HORIZONTAL(4),
    ROTATE_90_CW_FLIP_HORIZONTAL(5),
    ROTATE_180_FLIP_HORIZONTAL(6),
    ROTATE_90_CCW_FLIP_HORIZONTAL(7);

    /**
     * Private value
     */
    private int value;

    /**
     * Constructor
     *
     * @param value Enum value
     */
    private Orientation(int value) {
        this.value = value;
    }

    /**
     * Return the value of the Orientation
     *
     * @return Orientation value
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Converts the Value into an Orientation
     *
     * @param value value to convert
     * @return Orientation
     */
    public static Orientation convert(int value) {
        if (NONE.value == value) {
            return NONE;
        } else if (ROTATE_90_CW.value == value) {
            return ROTATE_90_CW;
        } else if (ROTATE_180.value == value) {
            return ROTATE_180;
        } else if (ROTATE_90_CCW.value == value) {
            return ROTATE_90_CCW;
        } else if (FLIP_HORIZONTAL.value == value) {
            return FLIP_HORIZONTAL;
        } else if (ROTATE_90_CW_FLIP_HORIZONTAL.value == value) {
            return ROTATE_90_CW_FLIP_HORIZONTAL;
        } else if (ROTATE_180_FLIP_HORIZONTAL.value == value) {
            return ROTATE_180_FLIP_HORIZONTAL;
        } else if (ROTATE_90_CCW_FLIP_HORIZONTAL.value == value) {
            return ROTATE_90_CCW_FLIP_HORIZONTAL;
        }
        return NONE;
    }
}
