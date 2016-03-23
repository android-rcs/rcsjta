/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core;

import com.gsma.rcs.core.ims.ImsError;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Observer of core events
 * 
 * @author Jean-Marc AUFFRET
 */
public interface CoreListener {
    /**
     * Core layer has been started
     */
    public void onCoreLayerStarted();

    /**
     * Core layer has been stopped
     */
    public void onCoreLayerStopped();

    /**
     * Registered to IMS
     */
    public void onRegistrationSuccessful();

    /**
     * IMS registration has failed
     * 
     * @param error Error
     */
    public void onRegistrationFailed(ImsError error);

    /**
     * Unregistered from IMS
     * 
     * @param reason reason code for registration termmination
     */
    public void onRegistrationTerminated(RcsServiceRegistration.ReasonCode reason);

    /**
     * User terms confirmation request
     * 
     * @param contact Remote server
     * @param id Request ID
     * @param type Type of request
     * @param pin PIN number requested
     * @param subject Subject
     * @param text Text
     * @param btnLabelAccept Label of Accept button
     * @param btnLabelReject Label of Reject button
     * @param timeout Timeout request in milliseconds
     */
    public void onUserConfirmationRequest(ContactId contact, String id, String type, boolean pin,
            String subject, String text, String btnLabelAccept, String btnLabelReject, long timeout);

    /**
     * User terms confirmation acknowledge
     * 
     * @param contact Remote server
     * @param id Request ID
     * @param status Status
     * @param subject Subject
     * @param text Text
     */
    public void onUserConfirmationAck(ContactId contact, String id, String status, String subject,
            String text);

    /**
     * User terms notification
     * 
     * @param contact Remote server
     * @param id Request ID
     * @param subject Subject
     * @param text Text
     * @param btnLabel Label of OK button
     */
    public void onUserNotification(ContactId contact, String id, String subject, String text,
            String btnLabel);

    /**
     * SIM has changed
     */
    public void onSimChangeDetected();

}
