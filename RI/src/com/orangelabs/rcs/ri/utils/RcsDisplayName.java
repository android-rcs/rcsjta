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

import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactService;
import com.gsma.services.rcs.contact.RcsContact;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.R;

import android.content.Context;
import android.util.Log;

/**
 * Utilities to manage the RCS display name
 * 
 * @author YPLO6403
 */
public class RcsDisplayName {

    private static final String LOGTAG = LogUtils.getTag(RcsDisplayName.class.getSimpleName());

    /**
     * Singleton of RcsDisplayName
     */
    private static volatile RcsDisplayName sInstance;

    private Context mContext;

    private ContactService mService;

    /**
     * The default display name
     */
    private static String sDefaultDisplayName;

    /**
     * Constructor
     * 
     * @param context
     */
    private RcsDisplayName(Context context) {
        mContext = context;
        mService = ConnectionManager.getInstance().getContactApi();
        sDefaultDisplayName = context.getString(R.string.label_no_contact);
    }

    /**
     * Get an instance of RcsDisplayName.
     * 
     * @param context the context
     * @return the singleton instance.
     */
    public static RcsDisplayName getInstance(Context context) {
        if (sInstance == null) {
            synchronized (RcsDisplayName.class) {
                if (sInstance == null) {
                    if (context == null) {
                        throw new IllegalArgumentException("Context is null");
                    }
                    sInstance = new RcsDisplayName(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * Returns RCS display name which can be displayed on UI
     * 
     * @param contact
     * @return the RCS display name
     */
    public String getDisplayName(ContactId contact) {
        if (contact == null) {
            return sDefaultDisplayName;
        }
        try {
            if (mService == null) {
                mService = ConnectionManager.getInstance().getContactApi();
            }
            RcsContact rcsContact = mService.getRcsContact(contact);
            String displayName = rcsContact.getDisplayName();
            if (displayName == null) {
                return contact.toString();
            } else {
                return displayName;
            }
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot get displayName", e);
            }
            return contact.toString();
        }
    }

    /**
     * get RCS display in a String which can be displayed on UI
     * 
     * @param number
     * @return the name which can be displayed on UI
     */
    public String getDisplayName(String number) {
        if (number == null) {
            return sDefaultDisplayName;
        }
        if (!ContactUtil.isValidContact(number)) {
            return number;
        }
        ContactId contact = ContactUtil.formatContact(number);
        return getDisplayName(contact);
    }
}
