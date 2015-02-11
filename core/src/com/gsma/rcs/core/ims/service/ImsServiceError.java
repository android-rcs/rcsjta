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

package com.gsma.rcs.core.ims.service;

/**
 * IMS service error
 * 
 * @author jexa7410
 */
public class ImsServiceError extends Error {
    static final long serialVersionUID = 1L;

    /**
     * Error code base for IMS sessions
     */
    protected static final int SESSION_ERROR_CODES = 100;

    /**
     * Error code base for capabitity service
     */
    protected static final int CAPABILITY_ERROR_CODES = 200;

    /**
     * Error code base for presence service
     */
    protected static final int PRESENCE_ERROR_CODES = 300;

    /**
     * Unexpected exception occurs in the module (e.g. internal exception)
     */
    public final static int UNEXPECTED_EXCEPTION = 1;

    /**
     * Error code
     */
    private int code;

    /**
     * Constructor
     * 
     * @param code Error code
     */
    public ImsServiceError(int code) {
        super();

        this.code = code;
    }

    /**
     * Constructor
     * 
     * @param code Error code
     * @param msg Detail message
     */
    public ImsServiceError(int code, String msg) {
        super(msg);

        this.code = code;
    }

    /**
     * Returns the error code
     * 
     * @return Error code
     */
    public int getErrorCode() {
        return code;
    }
}
