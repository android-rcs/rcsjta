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

import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;

/**
 * Content sharing streaming session listener
 * 
 * @author jexa7410
 */
public interface VideoStreamingSessionListener extends ImsSessionListener  {
    /**
     * Content sharing error
     * 
     * @param error Error
     */
    public void handleSharingError(ContentSharingError error);

    /**
     * Video stream has been resized
     *
     * @param width Video width
     * @param height Video height
     */
    public void handleVideoResized(int width, int height);
    
    /**
     * Destination user agent received INVITE, and is alerting user of call
     */
    public void handle180Ringing();
}
