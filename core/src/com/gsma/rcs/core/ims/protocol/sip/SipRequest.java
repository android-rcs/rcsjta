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

import javax2.sip.header.ExpiresHeader;
import javax2.sip.message.Request;

/**
 * SIP request
 * 
 * @author jexa7410
 */
public class SipRequest extends SipMessage {

    /**
     * Constructor
     * 
     * @param request SIP stack request
     */
    public SipRequest(Request request) {
        super(request);
    }

    /**
     * Return the SIP stack message
     * 
     * @return SIP request
     */
    public Request getStackMessage() {
        return (Request) mStackMessage;
    }

    /**
     * Returns the method value
     * 
     * @return Method name or null is case of response
     */
    public String getMethod() {
        return getStackMessage().getMethod();
    }

    /**
     * Return the request URI
     * 
     * @return String
     */
    public String getRequestURI() {
        return getStackMessage().getRequestURI().toString();
    }

    /**
     * Return the expires value
     * 
     * @return Expire value
     */
    public int getExpires() {
        ExpiresHeader expires = (ExpiresHeader) getStackMessage().getHeader(ExpiresHeader.NAME);
        if (expires != null) {
            return expires.getExpires();
        }
        return -1;
    }
}
