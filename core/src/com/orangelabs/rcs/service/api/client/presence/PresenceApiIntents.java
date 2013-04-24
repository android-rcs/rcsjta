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

package com.orangelabs.rcs.service.api.client.presence;

/**
 * Presence API intents 
 * 
 * @author jexa7410
 */
public interface PresenceApiIntents {
    /**
     * Intent broadcasted when a presence sharing invitation has been received
     */
	public final static String PRESENCE_INVITATION = "com.orangelabs.rcs.presence.PRESENCE_SHARING_INVITATION";
	
    /**
     * Intent broadcasted when my presence info has changed
     */
    public final static String MY_PRESENCE_INFO_CHANGED = "com.orangelabs.rcs.presence.MY_PRESENCE_INFO_CHANGED";

    /**
     * Intent broadcasted when a contact info has changed
     */
    public final static String CONTACT_INFO_CHANGED = "com.orangelabs.rcs.presence.CONTACT_INFO_CHANGED";

    /**
     * Intent broadcasted when a contact photo-icon has changed
     */
    public final static String CONTACT_PHOTO_CHANGED = "com.orangelabs.rcs.presence.CONTACT_PHOTO_CHANGED";

    /**
     * Intent broadcasted when a presence sharing info has changed
     */
    public final static String PRESENCE_SHARING_CHANGED = "com.orangelabs.rcs.presence.PRESENCE_SHARING_CHANGED";
}
