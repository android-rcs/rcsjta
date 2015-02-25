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

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.network.ImsNetworkInterface;
import com.gsma.services.rcs.RcsServiceRegistration;

/**
 * Server API utils
 * 
 * @author Jean-Marc AUFFRET
 */
public class ServerApiUtils {
    /**
     * Test core
     * 
     * @throws ServerApiException
     */
    public static void testCore() throws ServerApiException {
        if (Core.getInstance() == null) {
            throw new ServerApiException("Core is not instanciated");
        }
    }

    /**
     * Test IMS connection
     * 
     * @throws ServerApiException
     */
    public static void testIms() throws ServerApiException {
        if (!isImsConnected()) {
            throw new ServerApiException("Core is not connected to IMS");
        }
    }

    /**
     * Is connected to IMS
     * 
     * @return Boolean
     */
    public static boolean isImsConnected() {
        Core core = Core.getInstance();
        if (core == null) {
            return false;
        }
        ImsNetworkInterface networkInterface = core.getImsModule().getCurrentNetworkInterface();
        if (networkInterface == null) {
            return false;
        }
        return networkInterface.isRegistered();
    }

    /**
     * Gets the reason code for IMS service registration
     * 
     * @return reason code
     */
    public static RcsServiceRegistration.ReasonCode getServiceRegistrationReasonCode() {
        Core core = Core.getInstance();
        if (core == null) {
            return RcsServiceRegistration.ReasonCode.UNSPECIFIED;
        }
        ImsNetworkInterface networkInterface = core.getImsModule().getCurrentNetworkInterface();
        if (networkInterface == null) {
            return RcsServiceRegistration.ReasonCode.UNSPECIFIED;
        }
        return networkInterface.getRegistrationReasonCode();
    }

    /**
     * Test API extension permission
     * 
     * @param ext Extension ID
     * @throws ServerApiException
     */
    public static void testApiExtensionPermission(String ext) throws ServerApiException {
        // No control done in this release
    }
}
