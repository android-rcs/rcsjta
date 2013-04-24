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

package com.orangelabs.rcs.core.ims.service.im.chat.iscomposing;

import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.utils.DateUtils;


/**
 * Is composing info document (see RFC3994)
 */
public class IsComposingInfo {
	/**
	 * MIME type
	 */
	public static String MIME_TYPE = "application/im-iscomposing+xml";
	
	/**
	 * CRLF constant
	 */
	private static final String CRLF = "\r\n";

	/**
	 * State
	 */
	private boolean state = false;
	
	/**
	 * Last active state in seconds
	 */
	private long lastActiveDate = 0L;
	
	/**
	 * Refresh time in seconds
	 */
	private long refreshTime = 0L;
	
	/**
	 * Content type
	 */
	private String contentType = "";
	
	/**
	 * Constructor
	 */
	public IsComposingInfo() {
	}
	
	public void setState(String state) {
		if (state.equalsIgnoreCase("active")){
			this.state = true;
		} else {
			this.state = false;
		}
	}

	public void setLastActiveDate(String lastActiveTimeStamp){
		this.lastActiveDate = DateUtils.decodeDate(lastActiveTimeStamp)/1000;
	}
	
	public void setRefreshTime(String refreshTime){
		this.refreshTime = Long.parseLong(refreshTime);
	}
	
	public void setContentType(String contentType){
		this.contentType = contentType;
	}
	
	public boolean isStateActive(){
		return state;
	}
	
	public long getLastActiveDate(){
		return lastActiveDate;
	}
	
	public long getRefreshTime(){
		return refreshTime;
	}
	
	public String getContentType(){
		return contentType;
	}

	/**
	 * Build is composing document
	 * 
	 * @param status Status
	 * @return XML document
	 */
	public static String buildIsComposingInfo(boolean status) {
		String state = "idle";
		if (status) {
			state = "active";
		}
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CRLF +
			"<isComposing xmlns=\"urn:ietf:params:xml:ns:im-iscomposing\"" + CRLF +
			"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + CRLF +
			"xsi:schemaLocation=\"urn:ietf:params:xml:ns:im-composing iscomposing.xsd\">" + CRLF +		
			"<state>" + state + "</state>" + CRLF  +
			"<contenttype>" + InstantMessage.MIME_TYPE + "</contenttype>" + CRLF +
			"<lastactive>" + DateUtils.encodeDate(System.currentTimeMillis()) + "</lastactive>" + CRLF +
			"<refresh>60</refresh>" + CRLF +
		 	"</isComposing>";
	}
}
