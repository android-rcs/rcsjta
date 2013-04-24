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

package com.orangelabs.rcs.core.ims.service.richcall.video;

import com.orangelabs.rcs.core.content.LiveVideoContent;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;

/**
 * Originating live video content sharing session (streaming)
 *
 * @author jexa7410
 */
public class OriginatingLiveVideoStreamingSession extends OriginatingVideoStreamingSession {
    /**
     * Constructor
     *
     * @param parent IMS service
     * @param player Media player
     * @param content Content to be shared
     * @param contact Remote contact
     */
    public OriginatingLiveVideoStreamingSession(ImsService parent, IMediaPlayer player,
            LiveVideoContent content, String contact) {
        super(parent, player, content, contact);
    }
}
