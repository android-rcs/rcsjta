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

package com.gsma.rcs.core.ims.service.presence;

/**
 * Presence utility functions
 * 
 * @author jexa7410
 */
public class PresenceUtils {
    /**
     * RCS 2.0 video share feature tag
     */
    public final static String FEATURE_RCS2_VIDEO_SHARE = "org.gsma.videoshare";

    /**
     * RCS 2.0 image share feature tag
     */
    public final static String FEATURE_RCS2_IMAGE_SHARE = "org.gsma.imageshare";

    /**
     * RCS 2.0 file transfer feature tag
     */
    public final static String FEATURE_RCS2_FT = "org.openmobilealliance:File-Transfer";

    /**
     * RCS 2.0 chat feature tag
     */
    public final static String FEATURE_RCS2_CHAT = "org.openmobilealliance:IM-session";

    /**
     * RCS 2.0 CS video feature tag
     */
    public final static String FEATURE_RCS2_CS_VIDEO = "org.3gpp.cs-videotelephony";
}
