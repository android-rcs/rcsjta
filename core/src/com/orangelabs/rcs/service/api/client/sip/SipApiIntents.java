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

package com.orangelabs.rcs.service.api.client.sip;

/**
 * SIP API intents
 * 
 * @author jexa7410
 */
public class SipApiIntents {
	/**
     * Intent broadcasted when a new session invitation has been received
     * 
     * <p>The intent will have the following extra values:
     * <ul>
     *   <li><em>contact</em> - Contact phone number.</li>
     *   <li><em>contactDisplayname</em> - Display name associated to the contact.</li>
     *   <li><em>sessionId</em> - Session ID of the file transfer session.</li>
     * </ul>
     * </ul>
     */
	public final static String SESSION_INVITATION = "com.orangelabs.rcs.sip.SESSION_INVITATION";
	
	/**
     * Intent broadcasted when a new instant message has been received
     * 
     * <p>The intent will have the following extra values:
     * <ul>
     *   <li><em>contact</em> - Contact phone number.</li>
     *   <li><em>contactDisplayname</em> - Display name associated to the contact.</li>
     *   <li><em>content</em> - Content of the message.</li>
     *   <li><em>contentType</em> - Content type of the message.</li>
     * </ul>
     * </ul>
     */
	public final static String INSTANT_MESSAGE = "com.orangelabs.rcs.sip.INSTANT_MESSAGE";
}
