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

package com.orangelabs.rcs.ri.utils;

import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.ri.RiApplication;

import android.util.Log;

/**
 * @author yplo6403
 */
public class ContactUtil {

    private static final String LOGTAG = LogUtils.getTag(ContactUtil.class.getSimpleName());
    private static volatile com.gsma.services.rcs.contact.ContactUtil mContactUtil;

    /**
     * Formats the given contact to uniquely represent a RCS contact phone number.
     * 
     * @param contact the contact phone number
     * @return the ContactId
     */
    public static ContactId formatContact(String contact) {
        synchronized (ContactUtil.class) {
            if (mContactUtil == null) {
                mContactUtil = com.gsma.services.rcs.contact.ContactUtil.getInstance(RiApplication
                        .getAppContext());
            }
        }
        try {
            return mContactUtil.formatContact(contact);
        } catch (RcsPermissionDeniedException e) {
            /*
             * This exception should not occur since RI application cannot be started if country
             * code cannot be resolved.
             */
            String errorMessage = new StringBuilder("Failed to convert phone number '")
                    .append(contact).append("' into contactId!").toString();
            throw new IllegalStateException(errorMessage,e);
        }
    }

    /**
     * Verifies the validity of a contact number.
     * 
     * @param number the contact number
     * @return True if the given contact has the syntax of valid RCS ContactId.
     */
    public static boolean isValidContact(String number) {
        synchronized (ContactUtil.class) {
            if (mContactUtil == null) {
                mContactUtil = com.gsma.services.rcs.contact.ContactUtil.getInstance(RiApplication
                        .getAppContext());
            }
        }
        try {
            return mContactUtil.isValidContact(number);
        } catch (RcsPermissionDeniedException e) {
            /*
             * This exception should not occur since RI application cannot be started if country
             * code cannot be resolved.
             */
            String errorMessage = new StringBuilder("Failed to validate phone number '")
                    .append(number).append("'!").toString();
            Log.e(LOGTAG, errorMessage);
            return false;
        }
    }
}
