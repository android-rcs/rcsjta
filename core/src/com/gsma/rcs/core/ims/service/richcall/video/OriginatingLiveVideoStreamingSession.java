/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.service.richcall.video;

import com.gsma.rcs.core.content.LiveVideoContent;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.IVideoPlayer;

/**
 * Originating live video content sharing session (streaming)
 * 
 * @author Jean-Marc AUFFRET
 */
public class OriginatingLiveVideoStreamingSession extends OriginatingVideoStreamingSession {
    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param player Media player
     * @param content Content to be shared
     * @param contact Remote contact Id
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public OriginatingLiveVideoStreamingSession(ImsService parent, IVideoPlayer player,
            LiveVideoContent content, ContactId contact, RcsSettings rcsSettings, long timestamp,
            ContactsManager contactManager) {
        super(parent, player, content, contact, rcsSettings, timestamp, contactManager);
    }
}
