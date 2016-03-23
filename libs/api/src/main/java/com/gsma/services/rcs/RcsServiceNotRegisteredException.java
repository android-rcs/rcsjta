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

package com.gsma.services.rcs;

/**
 * Rcs service not registered exception
 * <p>
 * Thrown when a method of the service API using the RCS service platform is called and the terminal
 * which requires that the RcsCoreService is registered and connected to the IMS server is not
 * registered to the RCS service platform (e.g. not yet registered)
 * </p>
 * <p>
 * Should not be thrown when a service API method is called that fully could perform its scope of
 * responsibility without having to be connected to the IMS.
 * </p>
 * 
 * @author Jean-Marc AUFFRET
 */
public class RcsServiceNotRegisteredException extends RcsServiceException {

    static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     */
    public RcsServiceNotRegisteredException(String message) {
        super(message);
    }

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     * @param cause the cause (which is saved for later retrieval by the Throwable.getCause()
     *            method). (A null value is permitted, and indicates that the cause is nonexistent
     *            or unknown.)
     */
    public RcsServiceNotRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Asserts {@link RcsServiceNotRegisteredException}
     * <p>
     * An utility method that will translate the Server side exception to client specific exception
     * by parsing exception message which will have a special formatted exception message with a
     * pre-defined delimiter.
     * </p>
     * 
     * @param e Exception
     * @throws RcsServiceNotRegisteredException
     */
    public static void assertException(Exception e) throws RcsServiceNotRegisteredException {
        if (isIntendedException(e, RcsServiceNotRegisteredException.class)) {
            throw new RcsServiceNotRegisteredException(extractServerException(e), e);
        }
    }
}
