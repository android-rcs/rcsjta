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
 * RCS service not available exception
 * <p>
 * Thrown when a method of the service API is called and the service API is not bound to the RCS
 * service (e.g. RCS service not yet started or API not yet connected).
 * </p>
 * 
 * @author Jean-Marc AUFFRET
 */
public class RcsServiceNotAvailableException extends RcsServiceException {

    static final long serialVersionUID = 1L;

    private static final String ERROR_CNX = "Service not connected";

    /**
     * No Argument Constructor
     * <p>
     * This is a special case and reason for providing such constructor is due to the fact that we
     * will either have all or none of the services available at any given instance. So we can keep
     * the error message centralized here itself.
     * </p>
     */
    public RcsServiceNotAvailableException() {
        super(ERROR_CNX);
    }

    /**
     * Constructor
     * 
     * @param message Error message obtained either from a constant string or through e.getMessage()
     */
    public RcsServiceNotAvailableException(String message) {
        super(message);
    }
}
