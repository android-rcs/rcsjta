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

/**
 * Intent for multimedia streaming session invitations
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaStreamingSessionIntent {
    /**
     * Broadcast action: a new messaging session invitation has been received.
     * <p>
     * Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_SESSION_ID} containing the unique ID of the multimedia session.
     * <li>The service ID is read from the method Intent.getType() which returns the MIME type
     * included in the intent and corresponding to the invoked service.
     * </ul>
     */
    public final static String ACTION_NEW_INVITATION = "com.gsma.services.rcs.extension.action.NEW_STREAMING_SESSION";

    /**
     * Unique ID of the multimedia session
     */
    public final static String EXTRA_SESSION_ID = "sessionId";

    private MultimediaStreamingSessionIntent() {
    }
}
