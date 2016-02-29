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

package com.gsma.rcs.core.ims.network;

import com.gsma.rcs.core.access.MobileNetworkAccess;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.net.ConnectivityManager;

/**
 * Mobile network interface
 * 
 * @author JM. Auffret
 */
public class MobileNetworkInterface extends ImsNetworkInterface {
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param imsModule IMS module
     * @param rcsSettings the RCS settings accessor
     */
    public MobileNetworkInterface(ImsModule imsModule, RcsSettings rcsSettings) {
        super(imsModule, ConnectivityManager.TYPE_MOBILE, new MobileNetworkAccess(rcsSettings),
                rcsSettings.getImsProxyAddrForMobile(), rcsSettings.getImsProxyPortForMobile(),
                rcsSettings.getSipDefaultProtocolForMobile(), rcsSettings
                        .getImsAuthenticationProcedureForMobile(), rcsSettings);

        if (logger.isActivated()) {
            logger.info("Mobile network interface has been loaded");
        }
    }
}
