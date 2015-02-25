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

package com.gsma.rcs.core.ims.network;

import android.net.ConnectivityManager;

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.access.MobileNetworkAccess;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

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
     * @param rcsSettings
     * @throws CoreException
     */
    public MobileNetworkInterface(ImsModule imsModule, RcsSettings rcsSettings)
            throws CoreException {
        super(imsModule, ConnectivityManager.TYPE_MOBILE, new MobileNetworkAccess(), rcsSettings
                .getImsProxyAddrForMobile(), rcsSettings.getImsProxyPortForMobile(), rcsSettings
                .getSipDefaultProtocolForMobile(), rcsSettings
                .getImsAuthenticationProcedureForMobile(), rcsSettings);

        if (logger.isActivated()) {
            logger.info("Mobile network interface has been loaded");
        }
    }
}
