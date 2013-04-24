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
 * IMS disconnection reason
 *
 * @author jexa7410
 */
public interface ImsDisconnectionReason {
	/**
	 * Unknown reason
	 */
	public final static int UNKNOWN = 0;

    /**
     * Service has been terminated by user or by the system
     */
    public final static int SERVICE_TERMINATED = 1;

    /**
     * Registration failed
     */
    public final static int REGISTRATION_FAILED = 2;

    /**
     * Battery low
     */
    public final static int BATTERY_LOW = 3;
}
