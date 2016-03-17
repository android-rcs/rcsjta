/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.services.rcs;

/**
 * Service registration events listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class RcsServiceRegistrationListener {

    /**
     * Callback called when service is registered to the network platform
     */
    public abstract void onServiceRegistered();

    /**
     * Callback called when service is unregistered from the network platform
     * 
     * @param reasonCode the reason code
     */
    public abstract void onServiceUnregistered(RcsServiceRegistration.ReasonCode reasonCode);
}
