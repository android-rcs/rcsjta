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

/**
 * Session state
 * 
 * @author jexa7410
 */
public interface SessionState {
	/**
	 * Session state is unknown (i.e. session dialog path does not exist)
	 */
	public final static int UNKNOWN = -1;
	
	/**
	 * Session has been cancelled (i.e. SIP CANCEL exchanged)
	 */
	public final static int CANCELLED = 0;
	
	/**
	 * Session has been established (i.e. 200 OK/ACK exchanged)
	 */
	public final static int ESTABLISHED = 1;
	
	/**
	 * Session has been terminated (i.e. SIP BYE exchanged)
	 */
	public final static int TERMINATED = 2;
	
	/**
	 * Session is pending (not yet accepted by a final response by the remote)
	 */
	public final static int PENDING = 3;
}
