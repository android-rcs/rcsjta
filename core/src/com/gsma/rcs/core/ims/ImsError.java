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

package com.gsma.rcs.core.ims;

/**
 * IMS error
 * 
 * @author jexa7410
 */
public class ImsError extends Error {
    static final long serialVersionUID = 1L;

    /**
     * Unexpected exception occurs in the module (e.g. internal exception)
     */
    public final static int UNEXPECTED_EXCEPTION = 0x01;

    /**
     * Registration has failed
     */
    public final static int REGISTRATION_FAILED = 0x02;

    /**
     * Error code
     */
    private int mErrorCode;

    /**
     * Constructor
     * 
     * @param code Error code
     */
    public ImsError(int code) {
        super();
        mErrorCode = code;
    }

    /**
     * Constructor
     * 
     * @param code Error code
     * @param msg Detail message
     */
    public ImsError(int code, String msg) {
        super(msg);
        mErrorCode = code;
    }

    /**
     * Constructor
     * 
     * @param code Error code
     * @param cause the cause (which is saved for later retrieval by the Throwable.getCause()
     *            method). (A null value is permitted, and indicates that the cause is nonexistent
     *            or unknown.)
     */
    public ImsError(int code, Throwable cause) {
        super(cause);
        mErrorCode = code;
    }

    /**
     * Returns the error code
     * 
     * @return Error code
     */
    public int getErrorCode() {
        return mErrorCode;
    }
}
