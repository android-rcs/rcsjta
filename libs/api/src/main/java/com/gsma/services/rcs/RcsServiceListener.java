/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.services.rcs;

import android.util.SparseArray;

/**
 * RCS service event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public interface RcsServiceListener {
    /**
     * ReasonCode
     */
    enum ReasonCode {

        /**
         * Internal error
         */
        INTERNAL_ERROR(0),

        /**
         * Service has been disabled
         */
        SERVICE_DISABLED(1),

        /**
         * Service connection has been lost
         */
        CONNECTION_LOST(2);

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

        /**
         * Gets integer value associated to ReasonCode instance
         * 
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a ReasonCode instance for the specified integer value.
         * 
         * @param value the value associated with the ReasonCode
         * @return instance
         */
        public static ReasonCode valueOf(int value) {
            ReasonCode entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class " + ReasonCode.class.getName()
                    + "" + value + "!");
        }

    }

    /**
     * Callback called when service is connected. This method is called when the service is well
     * connected to the RCS service (binding procedure successful): this means the methods of the
     * API may be used.
     */
    void onServiceConnected();

    /**
     * Callback called when service has been disconnected. This method is called when the service is
     * disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param reasonCode the reason code
     * @see ReasonCode
     */
    void onServiceDisconnected(ReasonCode reasonCode);
}
