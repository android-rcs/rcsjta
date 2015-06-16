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

package com.gsma.rcs.provisioning.https;

import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.content.Intent;

/**
 * HTTPS provisioning - Input of MSISDN
 * 
 * @author Orange
 */
public final class HttpsProvionningMSISDNInput {

    /**
     * HttpsProvionningMSISDNInput instance
     */
    private static volatile HttpsProvionningMSISDNInput sInstance;

    /**
     * MSISDN
     */
    private ContactId mMsisdn;

    /**
     * Constructor
     */
    private HttpsProvionningMSISDNInput() {
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
     * Returns the Instance of HttpsProvionningMSISDNDialog
     * 
     * @return Instance of HttpsProvionningMSISDNDialog
     */
    public final static HttpsProvionningMSISDNInput getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (HttpsProvionningMSISDNInput.class) {
            if (sInstance == null) {
                sInstance = new HttpsProvionningMSISDNInput();
            }
        }
        return sInstance;
    }

    /**
     * Display the MSISDN popup
     * 
     * @param context
     * @return
     */
    protected ContactId displayPopupAndWaitResponse(Context context) {
        Intent intent = new Intent(context, HttpsProvisioningAlertDialog.class);
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
     */
    protected void responseReceived(ContactId contact) {
        synchronized (sInstance) {
            mMsisdn = contact;
            super.notify();
        }
    }
}
