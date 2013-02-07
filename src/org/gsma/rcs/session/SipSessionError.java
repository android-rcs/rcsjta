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

package org.gsma.rcs.session;

/**
 * Class SipSessionError.
 */
public class SipSessionError extends org.gsma.rcs.session.ImsSessionBasedServiceError {
	
	static final long serialVersionUID = 1L;

    /**
     * Constant SDP_NOT_INITIALIZED.
     */
    public static final int SDP_NOT_INITIALIZED = 141;

    /**
     * Creates a new instance of SipSessionError.
     *
     * @param arg1 The arg1.
     */
    public SipSessionError(org.gsma.rcs.session.ImsServiceError arg1) {
        super((org.gsma.rcs.session.ImsServiceError) null);
    }

    /**
     * Creates a new instance of SipSessionError.
     *
     * @param arg1 The arg1.
     */
    public SipSessionError(int arg1) {
        super((org.gsma.rcs.session.ImsServiceError) null);
    }

    /**
     * Creates a new instance of SipSessionError.
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public SipSessionError(int arg1, String arg2) {
        super((org.gsma.rcs.session.ImsServiceError) null);
    }

}
