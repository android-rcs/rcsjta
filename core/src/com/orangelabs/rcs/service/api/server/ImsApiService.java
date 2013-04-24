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

package com.orangelabs.rcs.service.api.server;

import com.orangelabs.rcs.service.api.client.IImsApi;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * IMS API service
 */
public class ImsApiService extends IImsApi.Stub {
    /**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 */
	public ImsApiService() {
		if (logger.isActivated()) {
			logger.info("IMS API service is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
	}
    
	/** 
	 * Is client connected to IMS
	 * 
	 * @return Boolean
	 */
    public boolean isImsConnected()throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Is client connected to IMS");
		}

		try {
			// Test IMS connection
			return ServerApiUtils.isImsConnected();
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}
}
