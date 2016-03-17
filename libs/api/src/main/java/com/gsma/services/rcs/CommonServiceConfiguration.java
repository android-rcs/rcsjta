/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.services.rcs;

import com.gsma.services.rcs.contact.ContactId;

import android.util.SparseArray;

/**
 * This class represents the common configuration of RCS Services
 * 
 * @author Philippe LEMORDANT
 */
public class CommonServiceConfiguration {

    private final ICommonServiceConfiguration mIConfig;

    /**
     * The messaging client mode
     */
    public enum MessagingMode {
        /**
         * Messaging mode not defined
         */
        NONE(0),
        /**
         * Messaging mode integrated
         */
        INTEGRATED(1),
        /**
         * Messaging mode converged
         */
        CONVERGED(2),
        /**
         * Messaging mode seamless
         */
        SEAMLESS(3);

        private int mValue;

        private static SparseArray<MessagingMode> mValueToEnum = new SparseArray<>();
        static {
            for (MessagingMode entry : MessagingMode.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        MessagingMode(int value) {
            mValue = value;
        }

        /**
         * Gets integer value associated to MessagingMode instance
         * 
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a MessagingMode instance for the specified integer value.
         * 
         * @param value the value associated to the MessagingMode
         * @return instance
         */
        public static MessagingMode valueOf(int value) {
            MessagingMode entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class "
                    + MessagingMode.class.getName() + "" + value);

        }

    }

    /**
     * The messaging method
     */
    public enum MessagingMethod {
        /**
         * Messaging method automatic
         */
        AUTOMATIC(0),
        /**
         * Messaging method RCS
         */
        RCS(1),
        /**
         * Messaging method non RCS
         */
        NON_RCS(2);

        private int mValue;

        private static SparseArray<MessagingMethod> mValueToEnum = new SparseArray<>();
        static {
            for (MessagingMethod entry : MessagingMethod.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        MessagingMethod(int value) {
            mValue = value;
        }

        /**
         * Gets integer value associated to MessagingMethod instance
         * 
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a MessagingMethod instance for the specified integer value.
         * 
         * @param value the value associated to the MessagingMethod
         * @return instance
         */
        public static MessagingMethod valueOf(int value) {
            MessagingMethod entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class "
                    + MessagingMethod.class.getName() + "" + value);
        }

    }

    /**
     * The minimum battery level
     */
    public enum MinimumBatteryLevel {
        /**
         * Discard minimum battery level
         */
        NEVER_STOP(0),
        /**
         * 5% is the minimum battery level
         */
        PERCENT_5(5),
        /**
         * 10% is the minimum battery level
         */
        PERCENT_10(10),
        /**
         * 20% is the minimum battery level
         */
        PERCENT_20(20);

        private int mValue;

        private static SparseArray<MinimumBatteryLevel> mValueToEnum = new SparseArray<>();
        static {
            for (MinimumBatteryLevel entry : MinimumBatteryLevel.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        MinimumBatteryLevel(int value) {
            mValue = value;
        }

        /**
         * Gets integer value associated to MinimumBatteryLevel instance
         * 
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a MinimumBatteryLevel instance for the specified integer value.
         * 
         * @param value the value associated to the MinimumBatteryLevel
         * @return instance
         */
        public static MinimumBatteryLevel valueOf(int value) {
            MinimumBatteryLevel entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class "
                    + MinimumBatteryLevel.class.getName() + "" + value);
        }

    }

    /**
     * Constructor
     * 
     * @param iConfig ICommonServiceConfiguration instance
     * @hide
     */
    /* package private */CommonServiceConfiguration(ICommonServiceConfiguration iConfig) {
        mIConfig = iConfig;
    }

    /**
     * Returns True if RCS configuration is valid.
     * 
     * @return boolean True if RCS configuration is valid.
     * @throws RcsGenericException
     */
    public boolean isConfigValid() throws RcsGenericException {
        try {
            return mIConfig.isConfigValid();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the display name associated to the RCS user account.<br>
     * The display name may be updated by the end user via the RCS settings application.
     * 
     * @return String Display name
     * @throws RcsGenericException
     */
    public String getMyDisplayName() throws RcsGenericException {
        try {
            return mIConfig.getMyDisplayName();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sets the display name associated to the RCS user account.
     * 
     * @param name the new display name
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void setMyDisplayName(String name) throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            mIConfig.setMyDisplayName(name);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the user contact Identifier (i.e. username part of the IMPU).
     * 
     * @return ContactId the contact ID
     * @throws RcsGenericException
     */
    public ContactId getMyContactId() throws RcsGenericException {
        try {
            return mIConfig.getMyContactId();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the messaging client mode which can be INTEGRATED, CONVERGED, SEAMLESS or NONE.
     * 
     * @return MessagingMode the messaging client mode
     * @throws RcsGenericException
     */
    public MessagingMode getMessagingUX() throws RcsGenericException {
        try {
            return MessagingMode.valueOf(mIConfig.getMessagingUX());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the minimum battery level which can be NEVER_STOP, PERCENT_5, PERCENT_10 or
     * PERCENT_20.
     * 
     * @return MinimumBatteryLevel the minimum battery level
     * @throws RcsGenericException
     */
    public MinimumBatteryLevel getMinimumBatteryLevel() throws RcsGenericException {
        try {
            return MinimumBatteryLevel.valueOf(mIConfig.getMinimumBatteryLevel());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sets the minimum battery level.
     * 
     * @param level the minimum battery level which can be NEVER_STOP, PERCENT_5, PERCENT_10 or
     *            PERCENT_20.
     * @throws RcsGenericException
     */
    public void setMinimumBatteryLevel(MinimumBatteryLevel level) throws RcsGenericException {
        try {
            mIConfig.setMinimumBatteryLevel(level.toInt());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the default messaging method which can be AUTOMATIC, RCS or NON_RCS.
     * 
     * @return MessagingMethod the default messaging method
     * @throws RcsGenericException
     */
    public MessagingMethod getDefaultMessagingMethod() throws RcsGenericException {
        try {
            return MessagingMethod.valueOf(mIConfig.getDefaultMessagingMethod());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sets the default messaging method.
     * 
     * @param method the default messaging method which can be AUTOMATIC, RCS or NON_RCS.
     * @throws RcsGenericException
     */
    public void setDefaultMessagingMethod(MessagingMethod method) throws RcsGenericException {
        try {
            mIConfig.setDefaultMessagingMethod(method.toInt());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

}
