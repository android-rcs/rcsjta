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

import com.gsma.services.rcs.RcsIllegalArgumentException;

/**
 * Server side implementation of {@link RcsIllegalArgumentException},
 * <p>
 * This exception will be thrown across AIDL layers and will come as a
 * {@link RcsIllegalArgumentException} on the client side.
 * </p>
 * <p>
 * Thrown when a method of the service API is called with one or multiple illegal input parameter.
 * Such as a calling a method and passing null as a parameter in the case that null is not valid for
 * that parameter.
 * </p>
 * <p>
 * <b> Should never be used on client side.</b>
 * </p>
 */
public class ServerApiIllegalArgumentException extends ServerApiBaseException {

    static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     */
    public ServerApiIllegalArgumentException(String message) {
        super(RcsIllegalArgumentException.class, message);
    }

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     * @param cause the cause (which is saved for later retrieval by the Throwable.getCause()
     *            method). (A null value is permitted, and indicates that the cause is nonexistent
     *            or unknown.)
     */
    public ServerApiIllegalArgumentException(String message, Throwable cause) {
        super(RcsIllegalArgumentException.class, message, cause);
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
