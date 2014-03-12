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
package com.orangelabs.rcs.popup;

import com.orangelabs.rcs.popup.utils.DateUtils;

/**
 * Popup manager
 *  
 * @author Jean-Marc AUFFRET
 */
public class PopupManager {
	/**
	 * CRLF constant
	 */
	private static final String CRLF = "\r\n";
    
	/**
	 * Service ID for the service
	 */
	public static final String SERVICE_ID = "mnc01.mcc208.popup";
	
	/**
	 * Generate popup
	 * 
	 * @param message Message
	 * @param animation Animation
	 * @param tts TTS
	 * @return XML document
	 */
	public static byte[] generatePopup(String message, String animation, boolean tts) {
		String popup = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CRLF +
				"<popup>" + CRLF +		
				"<message>" + message + "</message>" + CRLF  +
				"<animation>" + animation + "</animation>" + CRLF +
				"<timeout>" + DateUtils.encodeDate(System.currentTimeMillis()) + "</timeout>" + CRLF +
				"<tts>" + Boolean.toString(tts) + "</tts>" + CRLF +
			 	"</popup>";
		return popup.getBytes();
	}
}
