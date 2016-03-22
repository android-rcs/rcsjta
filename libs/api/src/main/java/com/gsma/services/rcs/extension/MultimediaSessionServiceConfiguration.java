/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.RcsGenericException;

/**
 * Multimedia session configuration
 * 
 * @author Jean-Marc AUFFRET
 * @author yplo6403
 */
public class MultimediaSessionServiceConfiguration {

    private final IMultimediaSessionServiceConfiguration mIConfig;

    /**
     * Constructor
     * 
     * @param iConfig IMultimediaSessionServiceConfiguration instance
     */
    /* package private */MultimediaSessionServiceConfiguration(
            IMultimediaSessionServiceConfiguration iConfig) {
        mIConfig = iConfig;
    }

    /**
     * Return maximum length of a multimedia message
     * 
     * @return int Number of bytes
     * @throws RcsGenericException
     */
    public int getMessageMaxLength() throws RcsGenericException {
        try {
            return mIConfig.getMessageMaxLength();
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the inactivity timeout of a multimedia messaging session.
     *
     * @param serviceId Service ID
     * @return long Timeout in milliseconds
     * @throws RcsGenericException
     */
    public long getMessagingSessionInactivityTimeout(String serviceId) throws RcsGenericException {
        try {
            return mIConfig.getMessagingSessionInactivityTimeout(serviceId);
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns True if the given service ID is activated, else returns False.
     *
     * @param serviceId Service ID
     * @return boolean
     * @throws RcsGenericException
     */
    public boolean isServiceActivated(String serviceId) throws RcsGenericException {
        try {
            return mIConfig.isServiceActivated(serviceId);
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }
}
