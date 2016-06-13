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

package com.gsma.rcs.core.access;

import com.gsma.rcs.provider.settings.RcsSettings;

import java.io.IOException;
import java.security.cert.CertificateException;

/**
 * Abstract network access
 * 
 * @author jexa7410
 */
public abstract class NetworkAccess {

    /**
     * Local IP address given to the network access
     */
    protected String mIpAddress;

    /**
     * Type of access
     */
    protected String mType;

    /**
     * rcs settings
     */
    protected final RcsSettings mRcsSettings;

    /**
     * Constructor
     */
    public NetworkAccess(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
    }

    /**
     * Return the local IP address
     * 
     * @return IP address
     */
    public String getIpAddress() {
        return mIpAddress;
    }

    /**
     * Return the type of access
     * 
     * @return Type
     */
    public abstract String getType();

    /**
     * Return the network name
     * 
     * @return Name
     */
    public abstract String getNetworkName();

    /**
     * Connect to the network access
     * 
     * @param ipAddress Local IP address
     * @throws CertificateException
     * @throws IOException
     */
    public abstract void connect(String ipAddress) throws CertificateException, IOException;

    /**
     * Disconnect from the network access
     */
    public abstract void disconnect();

}
