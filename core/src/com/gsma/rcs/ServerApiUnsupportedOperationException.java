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

import com.gsma.services.rcs.RcsUnsupportedOperationException;

/**
 * Server side implementation of {@link RcsUnsupportedOperationException},
 * <p>
 * This exception will be thrown across AIDL layers and will come as a
 * {@link RcsUnsupportedOperationException} on the client side.
 * </p>
 * <p>
 * Thrown when a method of the service API is called that is not supported (ie does not make sense
 * within the scope of the use case) like trying to call pauseTransfer() on a non pausable file
 * transfer that does not support that etc.
 * </p>
 * <p>
 * <b> Should never be used on client side.</b>
 * </p>
 */
public class ServerApiUnsupportedOperationException extends ServerApiBaseException {

    static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     */
    public ServerApiUnsupportedOperationException(String message) {
        super(RcsUnsupportedOperationException.class, message);
    }

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     * @param cause the cause (which is saved for later retrieval by the Throwable.getCause()
     *            method). (A null value is permitted, and indicates that the cause is nonexistent
     *            or unknown.)
     */
    public ServerApiUnsupportedOperationException(String message, Throwable cause) {
        super(RcsUnsupportedOperationException.class, message, cause);
    }

    /**
     * Api for the subclasses to decide if this exception should be treated as a bug and hence to be
     * get logged or not in the service layer just before AIDL connection to client.
     * 
     * @return boolean TRUE if exception should not be logged.
     */
    @Override
    public boolean shouldNotBeLogged() {
        return true;
    }
}
