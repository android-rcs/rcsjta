/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.service.im.chat.iscomposing;

import static com.orangelabs.rcs.utils.StringUtils.UTF8_STR;

import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
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
        return new StringBuilder("<?xml version=\"1.0\" encoding=\"")
                .append(UTF8_STR)
                .append("\"?>")
                .append(CRLF)
                .append("<isComposing xmlns=\"urn:ietf:params:xml:ns:im-iscomposing\"")
                .append(CRLF)
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
                .append(CRLF)
                .append("xsi:schemaLocation=\"urn:ietf:params:xml:ns:im-composing iscomposing.xsd\">")
                .append(CRLF).append("<state>").append(status ? "active" : "idle")
                .append("</state>").append(CRLF).append("<contenttype>")
                .append(MimeType.TEXT_MESSAGE).append("</contenttype>").append(CRLF)
                .append("<lastactive>").append(DateUtils.encodeDate(System.currentTimeMillis()))
                .append("</lastactive>").append(CRLF).append("<refresh>60</refresh>").append(CRLF)
                .append("</isComposing>").toString();
    }
}
