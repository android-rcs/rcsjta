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
package com.gsma.services.rcs.ipcall;

import com.gsma.services.rcs.RcsServiceException;

/**
 * IP call service configuration
 * 
 * @author Jean-Marc AUFFRET
 * @author yplo6403
 *
 */
public class IPCallServiceConfiguration {

	private final IIPCallServiceConfiguration mIConfig;

	/**
	 * Constructor
	 * 
	 * @param iConfig
	 *            IPCallServiceConfiguration instance
	 * @hide
	 */
	public IPCallServiceConfiguration(IIPCallServiceConfiguration iConfig) {
		mIConfig = iConfig;
	}

	/**
	 * Is voice call breakout activated. It returns True if the service can reach any user, else
	 * returns False if only RCS users supporting the IP call capability may be called.
	 * 
	 * @return Boolean
	 * @throws RcsServiceException
	 */
	public boolean isVoiceCallBreakout() throws RcsServiceException {
		try {
			return mIConfig.isVoiceCallBreakout();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}
}
