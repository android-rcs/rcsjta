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

package com.gsma.rcs.core.ims.protocol.sip;

import javax2.sip.message.Response;

/**
 * SIP response
 * 
 * @author jexa7410
 */
public class SipResponse extends SipMessage {

    /**
     * Constructor
     * 
     * @param response SIP stack response
     */
    public SipResponse(Response response) {
        super(response);
    }

    /**
     * Return the SIP stack message
     * 
     * @return SIP response
     */
    public Response getStackMessage() {
        return (Response) mStackMessage;
    }

    /**
     * Returns the status code value
     * 
     * @return Status code or -1
     */
    public int getStatusCode() {
        Response response = getStackMessage();
        if (response != null) {
            return response.getStatusCode();
        }
        return -1;
    }

    /**
     * Returns the reason phrase of the response
     * 
     * @return String or null
     */
    public String getReasonPhrase() {
        Response response = getStackMessage();
        if (response != null) {
            return getStackMessage().getReasonPhrase();
        }
        return null;
    }
}
