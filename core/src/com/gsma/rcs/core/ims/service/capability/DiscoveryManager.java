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

package com.gsma.rcs.core.ims.service.capability;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Discovery manager interface
 * 
 * @author jexa7410
 */
public interface DiscoveryManager {
    /**
     * Request contact capabilities
     * 
     * @param contact Remote contact identifier
     * @throws NetworkException
     * @throws PayloadException
     * @throws ContactManagerException
     */
    public void requestCapabilities(ContactId contact) throws PayloadException, NetworkException,
            ContactManagerException;
}
