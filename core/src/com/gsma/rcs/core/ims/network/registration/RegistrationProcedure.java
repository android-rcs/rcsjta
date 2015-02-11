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

package com.gsma.rcs.core.ims.network.registration;

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;

/**
 * Abstract registration procedure
 * 
 * @author jexa7410
 */
public abstract class RegistrationProcedure {
    /**
     * Initialize procedure
     */
    public abstract void init();

    /**
     * Returns the home domain name
     * 
     * @return Domain name
     */
    public abstract String getHomeDomain();

    /**
     * Returns the public URI or IMPU for registration
     * 
     * @return Public URI
     */
    public abstract String getPublicUri();

    /**
     * Write the security header to REGISTER request
     * 
     * @param request Request
     * @throws CoreException
     */
    public abstract void writeSecurityHeader(SipRequest request) throws CoreException;

    /**
     * Read the security header from REGISTER response
     * 
     * @param response Response
     * @throws CoreException
     */
    public abstract void readSecurityHeader(SipResponse response) throws CoreException;
}
