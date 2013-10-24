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
 * Intents for IP call service
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCallIntents {
	/**
	 * Load the IP call application to view a call. This Intent takes into parameter an URI on
	 * the call (i.e. content://ipcalls/ipcall_ID). If no parameter found the main entry of the
	 * IP call application is displayed.
	 */
	public static final String ACTION_VIEW_IPCALL = "org.gsma.joyn.action.VIEW_IPCALL";

	/**
	 * Load the IP call application to start a new call to a given contact. This Intent takes
	 * into parameter a contact URI (i.e. content://contacts/people/contact_ID). If no parameter
	 * the main entry of the IP call application is displayed.
	 */
	public static final String ACTION_INITIATE_IPCALL = "org.gsma.joyn.action.INITIATE_IPCALL";

	private IPCallIntents() {
    }    	
}
