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

package com.gsma.rcs.service.api;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.upload.IFileUploadServiceConfiguration;

import android.os.RemoteException;

/**
 * A class that implements interface to allow access to file upload service configuration from API
 * 
 * @author yplo6403
 */
public class FileUploadServiceConfigurationImpl extends IFileUploadServiceConfiguration.Stub {

    private final RcsSettings mRcsSettings;

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    /**
     * Constructor
     * 
     * @param rcsSettings RCS settings accessor
     */
    public FileUploadServiceConfigurationImpl(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
    }

    @Override
    public long getMaxSize() throws RemoteException {
        try {
            return mRcsSettings.getMaxImageSharingSize();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

}
