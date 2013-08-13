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

package org.gsma.joyn.session;

/**
 * Intent for incoming multimedia messages
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaMessageIntent {
    /**
     * Broadcast action: a new multimedia message has been received.
     * <p>Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_CONTACT} containing the MSISDN of the contact
     *  sending the message.
     * <li> {@link #EXTRA_DISPLAY_NAME} containing the display name of the
     *  contact sending the message (extracted from the SIP address).
     * <li> {@link #EXTRA_CONTENT} containing the multimedia message content.
     * <li> {@link #EXTRA_CONTENT_TYPE} containing the multimedia message content type.
     * <li> Service ID is read from the method Intent.getType() which returns the MIME type included
     *  in the intent and corresponding to the invoked service.
     * </ul>
     */
	public final static String ACTION_NEW_MESSAGE = "org.gsma.joyn.session.action.NEW_MESSAGE";

	/**
	 * MSISDN of the contact sending the message
	 */
	public final static String EXTRA_CONTACT = "contact";
	
	/**
	 * Display name of the contact sending the message
	 */
	public final static String EXTRA_DISPLAY_NAME = "contactDisplayname";

	/**
	 * Multimedia message content
	 */
	public final static String EXTRA_CONTENT = "content";	

	/**
	 * Multimedia message content type
	 */
	public final static String EXTRA_CONTENT_TYPE = "contentType";	
}
