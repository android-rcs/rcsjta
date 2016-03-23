/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.services.rcs.groupdelivery;

import android.util.SparseArray;

/**
 * Delivery info (delivery information on group messages and group file transfers)
 */
public class GroupDeliveryInfo {

    /**
     * Status of the group delivery info
     */
    public enum Status {

        /**
         * Delivery notifications were unsupported at the time the message or file-transfer was sent
         * and no delivery notification has been requested.
         */
        UNSUPPORTED(0),
        /**
         * The message or file-transfer has not received any delivery report for the specified
         * contact.
         */
        NOT_DELIVERED(1),

        /**
         * The message or file-transfer has received a delivery report for the specified contact
         */
        DELIVERED(2),

        /**
         * The message or file-transfer has received a displayed report for the specified contact.
         */
        DISPLAYED(3),

        /**
         * The message or file-transfer has received a delivery report failure for the specified
         * contact.
         */
        FAILED(4);

        private final int mValue;

        private static SparseArray<Status> mValueToEnum = new SparseArray<>();
        static {
            for (Status entry : Status.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        Status(int value) {
            mValue = value;
        }

        public final int toInt() {
            return mValue;
        }

        public static Status valueOf(int value) {
            Status entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class " + Status.class.getName() + ""
                    + value);
        }
    }

    /**
     * Group chat delivery status reason
     */
    public enum ReasonCode {

        /**
         * No specific reason code specified.
         */
        UNSPECIFIED(0),

        /**
         * A delivered-error delivery report has been received.
         */
        FAILED_DELIVERY(1),

        /**
         * A displayed-error delivery report has been received.
         */
        FAILED_DISPLAY(2);

        private final int mValue;

        private static SparseArray<ReasonCode> mValueToEnum = new SparseArray<>();
        static {
            for (ReasonCode entry : ReasonCode.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        ReasonCode(int value) {
            mValue = value;
        }

        public final int toInt() {
            return mValue;
        }

        public static ReasonCode valueOf(int value) {
            ReasonCode entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class " + ReasonCode.class.getName()
                    + "" + value);
        }
    }

    private GroupDeliveryInfo() {
    }
}
