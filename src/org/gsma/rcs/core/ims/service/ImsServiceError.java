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

package org.gsma.rcs.core.ims.service;

/**
 * The Class ImsServiceError.
 */
public class ImsServiceError extends Error {
	
	static final long serialVersionUID = 1L;

    /**
     * The Constant UNEXPECTED_EXCEPTION.
     */
    public static final int UNEXPECTED_EXCEPTION = 1;

    /**
     * The Constant SESSION_ERROR_CODES.
     */
    protected static final int SESSION_ERROR_CODES = 0;

    /**
     * The Constant CAPABILITY_ERROR_CODES.
     */
    protected static final int CAPABILITY_ERROR_CODES = 0;

    /**
     * The Constant PRESENCE_ERROR_CODES.
     */
    protected static final int PRESENCE_ERROR_CODES = 0;

    /**
     * Instantiates a new ims service error.
     *  
     * @param arg1 the arg1
     */
    public ImsServiceError(int arg1) {
        super();
    }

    /**
     * Instantiates a new ims service error.
     *  
     * @param arg1 the arg1
     * @param arg2 the arg2
     */
    public ImsServiceError(int arg1, String arg2) {
        super();
    }

    /**
     * Gets the error code.
     *  
     * @return  the error code
     */
    public int getErrorCode() {
        return 0;
    }

} // end ImsServiceError
