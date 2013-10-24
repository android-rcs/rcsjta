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

package com.orangelabs.rcs.provisioning.https;

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
    private static volatile HttpsProvionningMSISDNInput instance = null;

    /**
     * MSISDN
     */
    private String inputMSISDN;

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
    protected String getMsisdn() {
        return inputMSISDN;
    }

    /**
     * Returns the Instance of HttpsProvionningMSISDNDialog
     *
     * @return Instance of HttpsProvionningMSISDNDialog
     */
    public final static HttpsProvionningMSISDNInput getInstance() {
        if (HttpsProvionningMSISDNInput.instance == null) {
            synchronized (HttpsProvionningMSISDNInput.class) {
                if (HttpsProvionningMSISDNInput.instance == null) {
                    HttpsProvionningMSISDNInput.instance = new HttpsProvionningMSISDNInput();
                }
            }
        }
        return HttpsProvionningMSISDNInput.instance;
    }

    /**
     * Display the MSISDN popup
     *
     * @param context
     * @return 
     */
    protected String displayPopupAndWaitResponse(Context context) {
        Intent intent = new Intent(context, HttpsProvisioningAlertDialog.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        try {
            synchronized (HttpsProvionningMSISDNInput.instance) {
                super.wait();
            }
        } catch (InterruptedException e) {
            // nothing to do
        }

        return inputMSISDN;
    }

    /**
     * Callback of the MSISDN
     *
     * @param value
     */
    protected void responseReceived(String value) {
        synchronized (HttpsProvionningMSISDNInput.instance) {
            inputMSISDN = value;
            super.notify();
        }
    }
}
