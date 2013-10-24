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
 * Intents for file transfer service
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferIntents {
	/**
	 * Load the file transfer application to view a file transfer. This Intent
	 * takes into parameter an URI on the file transfer (i.e. content://filetransfers/ft_ID).
	 * If no parameter found the main entry of the file transfer application is displayed.
	 */
	public static final String ACTION_VIEW_FT = "org.gsma.joyn.action.VIEW_FT";

	/**
	 * Load the file transfer application to start a new file transfer to a given
	 * contact. This Intent takes into parameter a contact URI (i.e. content://contacts/people/contact_ID).
	 * If no parameter the main entry of the file transfer application is displayed.
	 */
	public static final String ACTION_INITIATE_FT = "org.gsma.joyn.action.INITIATE_FT";

	private FileTransferIntents() {
    }    	
}
