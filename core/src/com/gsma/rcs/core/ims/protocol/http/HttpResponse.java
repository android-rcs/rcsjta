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

package com.gsma.rcs.core.ims.protocol.http;

import android.text.TextUtils;

import java.util.Hashtable;

/**
 * HTTP response
 * 
 * @author jexa7410
 */
public class HttpResponse {

    private static final int INVALID_RESPONSE = -1;

    /**
     * Status line
     */
    private String mStatus;

    /**
     * Headers
     */
    private Hashtable<String, String> headers = new Hashtable<String, String>();

    /**
     * Content
     */
    private byte[] content = null;

    /**
     * Constructor
     */
    public HttpResponse() {
    }

    /**
     * Set the status line
     * 
     * @param status Status line
     */
    public void setStatusLine(String status) {
        mStatus = status;
    }

    /**
     * Get the status line
     * 
     * @return Status line
     */
    public String getStatusLine() {
        return mStatus;
    }

    /**
     * Add header
     * 
     * @param name Header name
     * @param value Header value
     */
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * Get header
     * 
     * @param name Header name
     * @return Header value
     */
    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    /**
     * Returns the response code
     * 
     * @return Response code or -1 in case of error
     */
    public int getResponseCode() {
        try {
            if (TextUtils.isEmpty(mStatus)) {
                return INVALID_RESPONSE;
            }
            int index1 = mStatus.indexOf(" ") + 1;
            int index2 = mStatus.indexOf(" ", index1);
            return Integer.parseInt(mStatus.substring(index1, index2));

        } catch (NumberFormatException e) {
            return INVALID_RESPONSE;
        }
    }

    /**
     * Is a successful response
     * 
     * @return Boolean
     */
    public boolean isSuccessfullResponse() {
        int code = getResponseCode();
        if ((code >= 200) && (code < 300)) {
            return true;
        }
        return false;
    }

    /**
     * Is not found response
     * 
     * @return Boolean
     */
    public boolean isNotFoundResponse() {
        int code = getResponseCode();
        if (code == 404) {
            return true;
        }
        return false;
    }

    /**
     * Returns the response content
     * 
     * @return Content as byte array
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Set the response content
     * 
     * @param content Content
     */
    public void setContent(byte[] content) {
        this.content = content;
    }
}
