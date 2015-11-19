/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.service.api;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.extension.IMultimediaSessionServiceConfiguration;

import android.os.RemoteException;

/**
 * A class that implements interface to allow access to multimedia session service configuration
 * from API
 * 
 * @author Philippe LEMORDANT
 */
public class MultimediaSessionServiceConfigurationImpl extends
        IMultimediaSessionServiceConfiguration.Stub {

    private static final Logger sLogger = Logger
            .getLogger(MultimediaSessionServiceConfigurationImpl.class.getSimpleName());

    private final RcsSettings mRcsSettings;

    /**
     * @param rcsSettings RCS settings accessor
     */
    public MultimediaSessionServiceConfigurationImpl(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
    }

    @Override
    public int getMessageMaxLength() throws RemoteException {
        try {
            return mRcsSettings.getMaxMsrpLengthForExtensions();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

}
