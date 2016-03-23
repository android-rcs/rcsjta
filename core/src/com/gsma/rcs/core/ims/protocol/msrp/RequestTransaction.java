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

package com.gsma.rcs.core.ims.protocol.msrp;

import com.gsma.rcs.provider.settings.RcsSettings;

import java.util.Hashtable;

/**
 * Request transaction
 * 
 * @author jexa7410
 */
public class RequestTransaction extends Object {
    /**
     * RcsSettings
     */
    private RcsSettings mRcsSettings;

    /**
     * Received response
     */
    private int receivedResponse = -1;

    /**
     * Constructor
     * 
     * @param rcsSettings
     */
    public RequestTransaction(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
    }

    /**
     * Notify response
     * 
     * @param code Response code
     * @param headers MSRP headers
     */
    public void notifyResponse(int code, Hashtable<String, String> headers) {
        synchronized (this) {
            // Set response code
            this.receivedResponse = code;

            // Unblock semaphore
            super.notify();
        }
    }

    /**
     * Wait response
     */
    public void waitResponse() {
        synchronized (this) {
            try {
                super.wait(mRcsSettings.getMsrpTransactionTimeout());
            } catch (InterruptedException e) {
                /* Nothing to do */
            }
        }
    }

    /**
     * Terminate transaction
     */
    public void terminate() {
        synchronized (this) {
            // Unblock semaphore
            super.notify();
        }
    }

    /**
     * Is response received
     * 
     * @return Boolean
     */
    public boolean isResponseReceived() {
        return (receivedResponse != -1);
    }

    /**
     * Returns received response
     * 
     * @return Code
     */
    public int getResponse() {
        return receivedResponse;
    }
}
