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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.session;

/**
 * IMS service error constants and utilities
 */
public class ImsServiceError extends Error {
	
	static final long serialVersionUID = 1L;

    /**
     * Unexpected errors
     */
    public static final int UNEXPECTED_EXCEPTION = 1;

    /**
     * IMS session errors
     */
    protected static final int SESSION_ERROR_CODES = 0;

    /**
     * Capability errors
     */
    protected static final int CAPABILITY_ERROR_CODES = 0;

    /**
     * Presence errors
     */
    protected static final int PRESENCE_ERROR_CODES = 0;

    /**
     * Creates a new IMS service error object using an IMS service error code
     *
     * @param code IMS service error code
     */
    public ImsServiceError(int code) {
        super();
    }

    /**
     * Creates a new IMS service error object using an IMS service error code and a message
     *
     * @param code IMS service error code 
     * @param message IMS error message
     */
    public ImsServiceError(int code, String message) {
        super();
    }

    /**
     * Gets the IMS error code.
     *
     * @return  The IMS error code
     */
    public int getErrorCode() {
        return 0;
    }

}
