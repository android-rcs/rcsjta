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
 * Intent for multimedia session invitations
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaSessionIntent {
    /**
     * Broadcast action: a new multimedia session invitation has been received.
     * <p>Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_CONTACT} containing the MSISDN of the contact
     *  sending the invitation.
     * <li> {@link #EXTRA_DISPLAY_NAME} containing the display name of the
     *  contact sending the invitation (extracted from the SIP address).
     * <li> {@link #EXTRA_SESSION_ID} containing the unique ID of the multimedia session.
     * </ul>
     */
	public final static String ACTION_NEW_INVITATION = "org.gsma.joyn.ish.action.NEW_SESSION";

	/**
	 * MSISDN of the contact sending the invitation
	 */
	public final static String EXTRA_CONTACT = "contact";
	
	/**
	 * Display name of the contact sending the invitation
	 */
	public final static String EXTRA_DISPLAY_NAME = "contactDisplayname";

	/**
	 * Unique ID of the multimedia session
	 */
	public final static String EXTRA_SESSION_ID = "sessionId";
}
