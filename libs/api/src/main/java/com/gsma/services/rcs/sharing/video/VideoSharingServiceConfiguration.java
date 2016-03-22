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

package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.RcsGenericException;

/**
 * Video sharing service configuration
 * 
 * @author Jean-Marc AUFFRET
 * @author yplo6403
 */
public class VideoSharingServiceConfiguration {

    IVideoSharingServiceConfiguration mConfiguration;

    /**
     * Constructor
     * 
     * @param configuration the configuration
     */
    /* package private */VideoSharingServiceConfiguration(
            IVideoSharingServiceConfiguration configuration) {
        mConfiguration = configuration;
    }

    /**
     * Returns the maximum authorized duration of the video sharing. It returns 0 if there is no
     * limitation.
     * 
     * @return long Duration in milliseconds
     * @throws RcsGenericException
     */
    public long getMaxTime() throws RcsGenericException {
        try {
            return mConfiguration.getMaxTime();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }
}
