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

package com.gsma.rcs.core.ims.service;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Listener of events sent during an IMS session
 * 
 * @author JM. Auffret
 */
public interface ImsSessionListener {
    /**
     * Session is started
     * 
     * @param contact Remote contact
     */
    public void handleSessionStarted(ContactId contact);

    /**
     * Session has been aborted
     * 
     * @param contact Remote contact
     * @param reason Termination reason
     */
    public void handleSessionAborted(ContactId contact, int reason);

    /**
     * Session has been terminated by remote
     * 
     * @param contact Remote contact
     */
    public void handleSessionTerminatedByRemote(ContactId contact);

    /**
     * Session is being rejected by user
     * 
     * @param contact Remote contact
     */
    public void handleSessionRejectedByUser(ContactId contact);

    /**
     * Session is being rejected due to time out
     * 
     * @param contact Remote contact
     */
    public void handleSessionRejectedByTimeout(ContactId contact);

    /**
     * Session is being rejected by remote
     * 
     * @param contact Remote contact
     */
    public void handleSessionRejectedByRemote(ContactId contact);

    /**
     * Accept has been called and the session is in the process of being started
     * 
     * @param contact Remote contact
     */
    public void handleSessionAccepted(ContactId contact);
}
