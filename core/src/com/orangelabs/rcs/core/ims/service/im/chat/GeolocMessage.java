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
package com.orangelabs.rcs.core.ims.service.im.chat;

import java.util.Date;

import com.orangelabs.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;

/**
 * Geoloc message
 * 
 * @author Jean-Marc AUFFRET
 */
public class GeolocMessage extends InstantMessage {
	/**
	 * MIME type
	 */
	public static final String MIME_TYPE = GeolocInfoDocument.MIME_TYPE;
	
	/**
	 * Geoloc info
	 */
	private GeolocPush geoloc = null;
		
    /**
     * Constructor for outgoing message
     * 
     * @param messageId Message Id
     * @param remote Remote user
     * @param geoloc Geoloc info
     * @param imdnDisplayedRequested Flag indicating that an IMDN "displayed" is requested
	 */
	public GeolocMessage(String messageId, String remote, GeolocPush geoloc, boolean imdnDisplayedRequested) {
		super(messageId, remote, geoloc.getLabel(), imdnDisplayedRequested);
		
		this.geoloc = geoloc;
	}
	
	/**
     * Constructor for incoming message
     * 
     * @param messageId Message Id
     * @param remote Remote user
     * @param geoloc Geoloc info
     * @param imdnDisplayedRequested Flag indicating that an IMDN "displayed" is requested
	 * @param serverReceiptAt Receipt date of the message on the server
	 */
	public GeolocMessage(String messageId, String remote, GeolocPush geoloc, boolean imdnDisplayedRequested, Date serverReceiptAt) {
		super(messageId, remote, geoloc.getLabel(), imdnDisplayedRequested, serverReceiptAt);
		
		this.geoloc = geoloc;
	}

    /**
	 * Get geoloc info
	 * 
	 * @return Geoloc info
	 */
	public GeolocPush getGeoloc() {
		return geoloc;
	}
}
