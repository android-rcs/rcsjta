/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.services.rcs;

/**
 * Rcs Generic Exception
 * <p>
 * A Generic exception that must be thrown from a service API when the requested operation failed to
 * fully complete its scope of responsibility and none of the more specified exceptions can be
 * thrown. The client must be able to trust that in case of any failure what so ever and its not
 * possible to throw a more specific exception this exception will be thrown as a kind of default
 * exception to signify that some error occurred that not necessarily need to be more specific than
 * that.
 * </p>
 */
public class RcsGenericException extends RcsServiceException {

    static final long serialVersionUID = 1L;

    /**
     * Constructs a new Exception with the current stack trace and the specified cause.
     * 
     * @param cause the cause (which is saved for later retrieval by the Throwable.getCause()
     *            method). (A null value is permitted, and indicates that the cause is nonexistent
     *            or unknown.)
     */
    public RcsGenericException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     * @param cause the cause (which is saved for later retrieval by the Throwable.getCause()
     *            method). (A null value is permitted, and indicates that the cause is nonexistent
     *            or unknown.)
     */
    public RcsGenericException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Asserts {@link RcsGenericException}
     * <p>
     * An utility method that will translate the Server side exception to client specific exception
     * by parsing exception message which will have a special formatted exception message with a
     * pre-defined delimiter.
     * </p>
     * 
     * @param e Exception
     * @throws RcsGenericException
     */
    public static void assertException(Exception e) throws RcsGenericException {
        if (isIntendedException(e, RcsGenericException.class)) {
            throw new RcsGenericException(extractServerException(e), e);
        }
    }
}
