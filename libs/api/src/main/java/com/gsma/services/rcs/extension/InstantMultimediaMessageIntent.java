/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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
 * Intent for instant multimedia message
 *
 * @author Jean-Marc AUFFRET
 */
public class InstantMultimediaMessageIntent {
    /**
     * Broadcast action: a new multimedia instant message has been received.
     * <p>
     * Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_CONTACT} containing the contact ID of remote contact.
     * <li> {@link #EXTRA_SERVICE_ID} containing the service ID related to the multimedia message.
     * <li>The service ID is read from the method Intent.getType() which returns the MIME type
     * included in the intent and corresponding to the invoked service.
     * <li> {@link #EXTRA_CONTENT} containing the content of the multimedia message.
     * <li> {@link #EXTRA_CONTENT_TYPE} containing the content type of the multimedia message.
     * </ul>
     */
    public final static String ACTION_NEW_INSTANT_MESSAGE = "com.gsma.services.rcs.extension.action.NEW_INSTANT_MESSAGE";

    /**
     * ContactId of remote contact
     */
    public final static String EXTRA_CONTACT = "contact";

    /**
     * Unique service ID
     */
    public final static String EXTRA_SERVICE_ID = "serviceId";

    /**
     * Content of the multimedia message (byte[])
     */
    public final static String EXTRA_CONTENT = "content";

    /**
     * Content type of the multimedia message
     */
    public final static String EXTRA_CONTENT_TYPE = "contentType";

    private InstantMultimediaMessageIntent() {
    }
}
