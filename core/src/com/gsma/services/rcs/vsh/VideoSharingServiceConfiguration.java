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
package com.gsma.services.rcs.vsh;

import com.gsma.services.rcs.RcsServiceException;

/**
 * Video sharing service configuration
 * 
 * @author Jean-Marc AUFFRET
 * @author yplo6403
 *
 */
public class VideoSharingServiceConfiguration {
	
	IVideoSharingServiceConfiguration mConfiguration;

	/**
	 * Constructor
	 * 
	 * @param configuration
	 * @hide
	 */
	public VideoSharingServiceConfiguration(IVideoSharingServiceConfiguration configuration) {
		mConfiguration = configuration;
	}

    /**
	 * Returns the maximum authorized duration of the video sharing. It returns 0 if
	 * there is no limitation.
	 * 
	 * @return Duration in seconds 
     * @throws RcsServiceException 
	 */
	public long getMaxTime() throws RcsServiceException {
		try {
			return mConfiguration.getMaxTime();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}
}
