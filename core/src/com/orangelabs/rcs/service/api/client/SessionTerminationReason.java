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
package com.orangelabs.rcs.service.api.client;

import com.orangelabs.rcs.core.ims.service.ImsServiceSession;

/**
 * Session termination reason
 * 
 * @author jexa7410
 */
public interface SessionTerminationReason {
	/**
	 * Session has been terminated by system (e.g. lost network connectivity or service shutdown)
	 */
	public final static int SYSTEM = ImsServiceSession.TERMINATION_BY_SYSTEM;
	
	/**
	 * Session has been terminated by end user
	 */
	public final static int USER = ImsServiceSession.TERMINATION_BY_USER;
	
	/**
	 * Session has been terminated after a timeout (e.g. inactivity)
	 */
	public final static int TIMEOUT = ImsServiceSession.TERMINATION_BY_TIMEOUT;
}