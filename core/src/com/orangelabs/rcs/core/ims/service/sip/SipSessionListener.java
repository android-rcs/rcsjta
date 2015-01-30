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

package com.orangelabs.rcs.core.ims.service.sip;

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;

import android.content.Intent;

/**
 * SIP session listener
 * 
 * @author jexa7410
 */
public interface SipSessionListener extends ImsSessionListener  {
    /**
     * SIP session error
     * @param contact Remote contact
     * @param error Error
     */
    public void handleSessionError(ContactId contact, SipSessionError error);
    
    /**
     * Receive data
     * @param contact Remote contact
     * @param error Error
     */
    public void handleReceiveData(ContactId contact, byte[] data);
    
    /**
     * Destination user agent received INVITE, and is alerting user of call
     * @param contact Remote contact
     */
    public void handle180Ringing(ContactId contact);

	/**
	 * A session invitation has been received
	 * @param contact Remote contact
	 * @param sessionInvite
	 */
	public void handleSessionInvited(ContactId contact, Intent sessionInvite);
}
