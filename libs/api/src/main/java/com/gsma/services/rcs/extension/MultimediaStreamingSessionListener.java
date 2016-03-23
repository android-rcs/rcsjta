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

package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.MultimediaSession.ReasonCode;
import com.gsma.services.rcs.extension.MultimediaSession.State;

/**
 * This class offers callback methods on multimedia streaming session events
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class MultimediaStreamingSessionListener {
    /**
     * Callback called when the multimedia messaging session state/reasoncode is changed.
     * 
     * @param contact Contact ID
     * @param sessionId Session ID
     * @param state State
     * @param reasonCode Reason code
     */
    public abstract void onStateChanged(ContactId contact, String sessionId, State state,
            ReasonCode reasonCode);

    /**
     * Callback called when a multimedia message or data is received.
     *
     * @param contact Contact ID
     * @param sessionId Session ID
     * @param content Payload content
     */
    public abstract void onPayloadReceived(ContactId contact, String sessionId, byte[] content);
}
