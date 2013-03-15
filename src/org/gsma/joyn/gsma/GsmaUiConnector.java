/*
 * Copyright 2013, France Telecom
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *    http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.joyn.gsma;

import java.lang.String;

/**
 * Class GsmaUiConnector.
 */
public class GsmaUiConnector {
    /**
     * Constant ACTION_GET_RCS_STATUS.
     */
    public static final String ACTION_GET_RCS_STATUS = "android.net.rcs.GET_RCS_STATUS";

    /**
     * Constant ACTION_REGISTRATION_CHANGED.
     */
    public static final String ACTION_REGISTRATION_CHANGED = "android.net.rcs.REGISTRATION_CHANGED";

    /**
     * Constant EXTRA_RCS_STATUS.
     */
    public static final String EXTRA_RCS_STATUS = "rcs";

    /**
     * Constant EXTRA_REGISTRATION_STATUS.
     */
    public static final String EXTRA_REGISTRATION_STATUS = "registration";

    /**
     * Constant ACTION_GET_CONTACT_CAPABILITIES.
     */
    public static final String ACTION_GET_CONTACT_CAPABILITIES = "android.net.rcs.GET_CONTACT_CAPABILITIES";

    /**
     * Constant ACTION_GET_MY_CAPABILITIES.
     */
    public static final String ACTION_GET_MY_CAPABILITIES = "android.net.rcs.GET_MY_CAPABILITIES";

    /**
     * Constant ACTION_CAPABILITIES_CHANGED.
     */
    public static final String ACTION_CAPABILITIES_CHANGED = "android.net.rcs.CAPABILITIES_CHANGED";

    /**
     * Constant EXTRA_CONTACT.
     */
    public static final String EXTRA_CONTACT = "contact";

    /**
     * Constant EXTRA_CAPABILITY_CHAT.
     */
    public static final String EXTRA_CAPABILITY_CHAT = "chat";

    /**
     * Constant EXTRA_CAPABILITY_FT.
     */
    public static final String EXTRA_CAPABILITY_FT = "filetransfer";

    /**
     * Constant EXTRA_CAPABILITY_IMAGE_SHARE.
     */
    public static final String EXTRA_CAPABILITY_IMAGE_SHARE = "imageshare";

    /**
     * Constant EXTRA_CAPABILITY_VIDEO_SHARE.
     */
    public static final String EXTRA_CAPABILITY_VIDEO_SHARE = "videoshare";

    /**
     * Constant EXTRA_CAPABILITY_SF.
     */
    public static final String EXTRA_CAPABILITY_SF = "standfw";

    /**
     * Constant EXTRA_CAPABILITY_PRESENCE_DISCOVERY.
     */
    public static final String EXTRA_CAPABILITY_PRESENCE_DISCOVERY = "presencediscovery";

    /**
     * Constant EXTRA_CAPABILITY_SOCIAL_PRESENCE.
     */
    public static final String EXTRA_CAPABILITY_SOCIAL_PRESENCE = "socialpresence";

    /**
     * Constant EXTRA_CAPABILITY_CS_VIDEO.
     */
    public static final String EXTRA_CAPABILITY_CS_VIDEO = "csvideo";

    /**
     * Constant EXTRA_CAPABILITY_EXTENSIONS.
     */
    public static final String EXTRA_CAPABILITY_EXTENSIONS = "extensions";

    /**
     * Constant ACTION_VIEW_CHAT.
     */
    public static final String ACTION_VIEW_CHAT = "android.net.rcs.VIEW_CHAT";

    /**
     * Constant ACTION_INITIATE_CHAT.
     */
    public static final String ACTION_INITIATE_CHAT = "android.net.rcs.INITIATE_CHAT";

    /**
     * Constant ACTION_VIEW_CHAT_GROUP.
     */
    public static final String ACTION_VIEW_CHAT_GROUP = "android.net.rcs.VIEW_CHAT_GROUP";

    /**
     * Constant ACTION_INITIATE_CHAT_GROUP.
     */
    public static final String ACTION_INITIATE_CHAT_GROUP = "android.net.rcs.INITIATE_CHAT_GROUP";

    /**
     * Constant ACTION_VIEW_FT.
     */
    public static final String ACTION_VIEW_FT = "android.net.rcs.VIEW_FT";

    /**
     * Constant ACTION_INITIATE_FT.
     */
    public static final String ACTION_INITIATE_FT = "android.net.rcs.INITIATE_FT";

    /**
     * Creates a new instance of GsmaUiConnector.
     */
    public GsmaUiConnector() {

    }

} // end GsmaUiConnector
