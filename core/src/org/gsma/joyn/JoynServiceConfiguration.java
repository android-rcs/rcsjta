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
package org.gsma.joyn;

/**
 * joyn Service configuration
 *  
 * @author Jean-Marc AUFFRET
 */
public class JoynServiceConfiguration {
	/**
	 * Returns True if the joyn service is activated, else returns False. The service may be activated or
	 * deactivated by the end user via the joyn settings application.
	 * 
	 * @return Boolean
	 */
	public static boolean isServiceActivated() {
		// TODO
		return true;
	}
	
	
	/**
	 * Returns the display name associated to the joyn user account. The display name may be updated by
	 * the end user via the joyn settings application.
	 * 
	 * @return Display name
	 */
	public static String getUserDisplayName() {
		// TODO
		return null;
	}
}
