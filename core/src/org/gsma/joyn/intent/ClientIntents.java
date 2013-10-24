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
package org.gsma.joyn.intent;

/**
 * Intents for joyn client detection and settings
 * 
 * @author Jean-Marc AUFFRET
 */
public class ClientIntents {
	/**
	 * Intent to load the settings activity to enable or disable the client
	 */
	public static final String ACTION_VIEW_SETTINGS = "org.gsma.joyn.action.VIEW_SETTINGS";

	/**
	 * Intent to request the client status. The result is received via an Intent
	 * having the following extras:
	 * <ul>
	 * <li> {@link #EXTRA_CLIENT} containing the client package name.
	 * <li> {@link #EXTRA_STATUS} containing the boolean status of the client. True
	 *  means that the client is activated, else the client is not activated.
	 */
	public static final String ACTION_CLIENT_GET_STATUS = ".client.action.GET_STATUS";

	/**
	 * Client package name
	 */
	public final static String EXTRA_CLIENT = "client";
	
	/**
	 * Client status
	 */
	public final static String EXTRA_STATUS = "status";

	private ClientIntents() {
    }    	
}
