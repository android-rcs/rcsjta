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

package com.orangelabs.rcs.core.ims.protocol.msrp;

import java.util.Hashtable;

import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * Request transaction
 * 
 * @author jexa7410
 */
public class RequestTransaction extends Object {
    /**
     * MRSP request transaction timeout (in seconds)
     */
    private final static int TIMEOUT = RcsSettings.getInstance().getMsrpTransactionTimeout();

    /**
     * Received response
     */
    private int receivedResponse = -1;

    /**
     * Constructor
     */
    public RequestTransaction() {
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
                // Wait semaphore
                super.wait(TIMEOUT * 1000);
            } catch (InterruptedException e) {
                // Nothing to do
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
