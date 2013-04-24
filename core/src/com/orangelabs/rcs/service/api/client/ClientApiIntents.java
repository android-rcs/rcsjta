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
 * Client API intents
 *  
 * @author jexa7410
 */
public class ClientApiIntents {
    /**
     * Service status "starting"
     */
	public final static int SERVICE_STATUS_STARTING = 0;
	
    /**
     * Service status "started"
     */
	public final static int SERVICE_STATUS_STARTED = 1;

	/**
     * Service status "stopping"
     */
	public final static int SERVICE_STATUS_STOPPING = 2;

	/**
     * Service status "stopped"
     */
	public final static int SERVICE_STATUS_STOPPED = 3;
	
	/**
     * Service status "failed"
     */
	public final static int SERVICE_STATUS_FAILED = 4;

	/**
     * Intent which permits to load the RCS settings application 
     */
	public final static String RCS_SETTINGS = "com.orangelabs.rcs.SETTINGS";

	/**
     * Intent broadcasted when the RCS service status has changed (see constant attribute "status").
     * 
     * <p>The intent will have the following extra values:
     * <ul>
     *   <li><em>status</em> - Service status.</li>
     * </ul>
     * </ul>
     */
	public final static String SERVICE_STATUS = "com.orangelabs.rcs.SERVICE_STATUS";
}
