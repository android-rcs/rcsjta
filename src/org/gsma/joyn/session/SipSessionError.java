/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.gsma.joyn.session;

import org.gsma.joyn.session.ImsSessionBasedServiceError;
import org.gsma.joyn.session.ImsServiceError;

/**
 * SIP session errors constants and utilities
 */
public class SipSessionError extends ImsSessionBasedServiceError {
	
	static final long serialVersionUID = 1L;

    /**
     * Session Description Protocol (SDP) not initialized
     */
    public static final int SDP_NOT_INITIALIZED = 141;

    /**
     * Creates a SIP session error object using an IMS error
     *
     * @param error IMS session error object
     */
    public SipSessionError(ImsServiceError error) {
        super((ImsServiceError) null);
    }

    /**
     * Creates a SIP session error using an IMS error code
     *
     * @param code Error error code
     */
    public SipSessionError(int code) {
        super((ImsServiceError) null);
    }

    /**
     * Creates a SIP session error using an IMS error code and an error message
     *
     * @param errorCode Error IMS error code
     * @param message Error message
     */
    public SipSessionError(int errorCode, String message) {
        super((ImsServiceError) null);
    }

}
