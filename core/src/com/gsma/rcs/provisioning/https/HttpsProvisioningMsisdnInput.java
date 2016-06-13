/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2016 Sony Mobile Communications Inc.
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

package com.gsma.rcs.provisioning.https;

import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

/**
 * HTTPS provisioning - Input of MSISDN
 * 
 * @author Orange
 */
public final class HttpsProvisioningMsisdnInput {

    /* package private */static final String EXTRA_CONTACT = "contact";

    /**
     * Singleton instance
     */
    private static volatile HttpsProvisioningMsisdnInput sInstance;

    private ContactId mMsisdn;

    /**
     * Private constructor to prevent instantiation
     */
    private HttpsProvisioningMsisdnInput() {
        super();
    }

    /**
     * Get the MSISDN
     * 
     * @return MSISDN
     */
    protected ContactId getMsisdn() {
        return mMsisdn;
    }

    /**
     * Returns the Instance of HttpsProvisioningMSISDNDialog
     * 
     * @return Instance of HttpsProvisioningMSISDNDialog
     */
    public static HttpsProvisioningMsisdnInput getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (HttpsProvisioningMsisdnInput.class) {
            if (sInstance == null) {
                sInstance = new HttpsProvisioningMsisdnInput();
            }
            return sInstance;
        }
    }

    /**
     * Display the MSISDN popup
     *
     * @param contact the contact ID to display/edit or null
     * @return ContactId
     */
    protected ContactId displayPopupAndWaitResponse(Context context, ContactId contact) {
        Intent intent = new Intent(context, HttpsProvisioningAlertDialog.class);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        try {
            synchronized (sInstance) {
                super.wait();
            }
        } catch (InterruptedException e) {
            // nothing to do
        }
        return mMsisdn;
    }

    /**
     * Callback of the MSISDN
     * 
     * @param contact the contact ID
     */
    protected void responseReceived(ContactId contact) {
        synchronized (sInstance) {
            mMsisdn = contact;
            super.notify();
        }
    }
}
