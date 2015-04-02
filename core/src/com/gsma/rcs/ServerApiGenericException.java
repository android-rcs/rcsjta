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

package com.gsma.rcs;

import com.gsma.services.rcs.RcsGenericException;

/**
 * Server side implementation of {@link RcsGenericException},
 * <p>
 * This exception will be thrown across AIDL layers and will come as a {@link RcsGenericException}
 * on the client side.
 * </p>
 * <p>
 * This Generic exception must be thrown when the requested operation failed to fully complete its
 * scope of responsibility and none of the more specified exceptions can be thrown. The client must
 * be able to trust that in case of any failure what so ever and its not possible to throw a more
 * specific exception this exception will be thrown as a kind of default exception to signify that
 * some error occurred that not necessarily need to be more specific than that.
 * </p>
 * <p>
 * <b> Should never be used on client side.</b>
 * </p>
 */
public class ServerApiGenericException extends ServerApiBaseException {

    static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     */
    public ServerApiGenericException(String message) {
        super(RcsGenericException.class, message);
    }

    /**
     * Constructs a new Exception with the current stack trace and the specified cause.
     * 
     * @param cause the cause (which is saved for later retrieval by the Throwable.getCause()
     *            method). (A null value is permitted, and indicates that the cause is nonexistent
     *            or unknown.)
     */
    public ServerApiGenericException(Throwable cause) {
        super(RcsGenericException.class, cause.getMessage(), cause);
    }

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     * @param cause the cause (which is saved for later retrieval by the Throwable.getCause()
     *            method). (A null value is permitted, and indicates that the cause is nonexistent
     *            or unknown.)
     */
    public ServerApiGenericException(String message, Throwable cause) {
        super(RcsGenericException.class, message, cause);
    }

    /**
     * Api for the subclasses to decide if this exception should be treated as a bug and hence to be
     * get logged or not in the service layer just before AIDL connection to client.
     * 
     * @return boolean TRUE if exception should not be logged.
     */
    @Override
    public boolean shouldNotBeLogged() {
        return false;
    }
}
