/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * HTTPS provisioning - Input of MSISDN
 * 
 * @author Orange
 */
public final class HttpsProvisioningMSISDNInput {

    /**
     * HttpsProvisioningMSISDNInput instance
     */
    private static volatile HttpsProvisioningMSISDNInput sInstance;

    /**
     * MSISDN
     */
    private ContactId mMsisdn;

    /**
     * Constructor
     */
    private HttpsProvisioningMSISDNInput() {
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
    public final static HttpsProvisioningMSISDNInput getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (HttpsProvisioningMSISDNInput.class) {
            if (sInstance == null) {
                sInstance = new HttpsProvisioningMSISDNInput();
            }
            return sInstance;
        }
    }

    /**
     * Display the MSISDN popup
     * 
     * @param context
     * @param bundle
     * @return ContactId
     */
    protected ContactId displayPopupAndWaitResponse(Context context, Bundle savedInstance) {
        Intent intent = new Intent(context, HttpsProvisioningAlertDialog.class);
        intent.putExtra(HttpsProvisioningManager.RETRY_EXTRA, savedInstance);
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
     * @param contact
     * @param context
     */
    protected void responseReceived(ContactId contact, Context context) {
        synchronized (sInstance) {
            mMsisdn = contact;
            final RcsSettings rcsSettings = RcsSettings.getInstance(new LocalContentResolver(
                    context));
            rcsSettings.setUserProfileImsUserName(contact);
            super.notify();
        }
    }
}
