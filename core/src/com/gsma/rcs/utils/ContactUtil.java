/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.gsma.rcs.utils;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Contact utility functions
 */
public class ContactUtil {

    private static volatile com.gsma.services.rcs.contact.ContactUtil mContactUtil;

    /**
     * A class to hold a valid phone number
     */
    public final class PhoneNumber {
        private String mNumber;

        /**
         * Constructor
         * 
         * @param number a valid phone number
         */
        /* package private */PhoneNumber(String number) {
            mNumber = number;
        }

        /**
         * Gets the valid phone number
         * 
         * @return the valid phone number
         */
        public String getNumber() {
            return mNumber;
        }
    };

    /**
     * Gets a valid phone number from a URI
     * 
     * @param uri phone number
     * @return the phone number without URI formating or null if not valid
     */
    public static PhoneNumber getValidPhoneNumberFromUri(String uri) {
        String number = PhoneUtils.extractNumberFromUriWithoutFormatting(uri);
        synchronized (ContactUtil.class) {
            if (mContactUtil == null) {
                mContactUtil = com.gsma.services.rcs.contact.ContactUtil.getInstance(AndroidFactory
                        .getApplicationContext());
            }
        }
        if (mContactUtil.isValidContact(number)) {
            return new ContactUtil().new PhoneNumber(number);
        }
        return null;
    }

    /**
     * Gets a valid phone number from a contact got from Android system.<br>
     * (By Android system, we mean telephony manager, address book, etc)
     * 
     * @param contact from Android system
     * @return the phone number or null if not valid
     */
    public static PhoneNumber getValidPhoneNumberFromAndroid(String contact) {
        synchronized (ContactUtil.class) {
            if (mContactUtil == null) {
                mContactUtil = com.gsma.services.rcs.contact.ContactUtil.getInstance(AndroidFactory
                        .getApplicationContext());
            }
        }
        if (mContactUtil.isValidContact(contact)) {
            return new ContactUtil().new PhoneNumber(contact);
        }
        return null;
    }

    /**
     * Creates a ContactId from a validated phone number
     * 
     * @param phoneNumber the validated phone number
     * @return the Contact Identifier
     */
    public static ContactId createContactIdFromValidatedData(PhoneNumber phoneNumber) {
        synchronized (ContactUtil.class) {
            if (mContactUtil == null) {
                mContactUtil = com.gsma.services.rcs.contact.ContactUtil.getInstance(AndroidFactory
                        .getApplicationContext());
            }
        }
        return mContactUtil.formatContact(phoneNumber.getNumber());
    }

    /**
     * Creates a ContactId from a trusted data<br>
     * (By trusted data, we mean RCS providers)
     * 
     * @param phoneNumber from a trusted data
     * @return the Contact Identifier
     */
    public static ContactId createContactIdFromTrustedData(String phoneNumber) {
        synchronized (ContactUtil.class) {
            if (mContactUtil == null) {
                mContactUtil = com.gsma.services.rcs.contact.ContactUtil.getInstance(AndroidFactory
                        .getApplicationContext());
            }
        }
        return mContactUtil.formatContact(phoneNumber);
    }

}
