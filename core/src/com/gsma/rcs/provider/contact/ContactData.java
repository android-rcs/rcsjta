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

package com.gsma.rcs.provider.contact;

import com.gsma.services.rcs.capability.CapabilitiesLog;

import android.net.Uri;

/**
 * RCS Contact address book data constants
 * 
 * @author Jean-Marc AUFFRET
 */
/* package private */final class ContactData {
    /**
     * Database URI
     */
    /* package private */static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.rcs.capability/capability");

    /**
     * Unique base column id
     */
    /* package private */static final String KEY_BASECOLUMN_ID = CapabilitiesLog.BASECOLUMN_ID;

    /**
     * ContactId formatted number of contact associated to the capabilities.
     */
    /* package private */static final String KEY_CONTACT = CapabilitiesLog.CONTACT;

    /**
     * Image sharing capability. Values: 1 (true), 0 (false)
     */
    /* package private */static final String KEY_CAPABILITY_IMAGE_SHARE = CapabilitiesLog.CAPABILITY_IMAGE_SHARE;

    /**
     * Video sharing capability. Values: 1 (true), 0 (false)
     */
    /* package private */static final String KEY_CAPABILITY_VIDEO_SHARE = CapabilitiesLog.CAPABILITY_VIDEO_SHARE;

    /**
     * File transfer capability. Values: 1 (true), 0 (false)
     */
    /* package private */static final String KEY_CAPABILITY_FILE_TRANSFER = CapabilitiesLog.CAPABILITY_FILE_TRANSFER;

    /**
     * IM/Chat capability. Values: 1 (true), 0 (false)
     */
    /* package private */static final String KEY_CAPABILITY_IM_SESSION = CapabilitiesLog.CAPABILITY_IM_SESSION;

    /**
     * Geolocation push capability. Values: 1 (true), 0 (false)
     */
    /* package private */static final String KEY_CAPABILITY_GEOLOC_PUSH = CapabilitiesLog.CAPABILITY_GEOLOC_PUSH;

    /**
     * Supported RCS extensions. List of features tags semicolon separated (e.g. <TAG1>;<TAG2>;
     * ;TAGn)
     */
    /* package private */static final String KEY_CAPABILITY_EXTENSIONS = CapabilitiesLog.CAPABILITY_EXTENSIONS;

    /**
     * Is an automata. Values: 1 (true), 0 (false).
     */
    /* package private */static final String KEY_AUTOMATA = CapabilitiesLog.AUTOMATA;

    /**
     * Timestamp of the last capability response
     */
    /* package private */static final String KEY_CAPABILITY_TIMESTAMP_LAST_RESPONSE = CapabilitiesLog.TIMESTAMP;

    /**
     * Column Name
     */
    /* package private */static final String KEY_DISPLAY_NAME = "display_name";

    /**
     * Column name
     */
    /* package private */static final String KEY_PRESENCE_SHARING_STATUS = "presence_sharing_status";

    /**
     * Column name
     */
    /* package private */static final String KEY_TIMESTAMP_CONTACT_UPDATED = "timestamp_contact_updated";

    /**
     * Column name
     */
    /* package private */static final String KEY_RCS_STATUS = "rcs_status";

    /**
     * Column name
     */
    /* package private */static final String KEY_REGISTRATION_STATE = "registration_state";

    /**
     * Column name
     */
    /* package private */static final String KEY_RCS_STATUS_TIMESTAMP = "rcs_status_timestamp";

    /**
     * Column name
     */
    /* package private */static final String KEY_PRESENCE_FREE_TEXT = "presence_free_text";

    /**
     * Column name
     */
    /* package private */static final String KEY_PRESENCE_WEBLINK_NAME = "presence_weblink_name";

    /**
     * Column name
     */
    /* package private */static final String KEY_PRESENCE_WEBLINK_URL = "presence_weblink_url";

    /**
     * Column name
     */
    /* package private */static final String KEY_PRESENCE_PHOTO_EXIST_FLAG = "presence_photo_exist_flag";

    /**
     * Column name
     */
    /* package private */static final String KEY_PRESENCE_PHOTO_ETAG = "presence_photo_etag";

    /**
     * Column name
     */
    /* package private */static final String KEY_PRESENCE_PHOTO_DATA = "presence_photo_data";

    /**
     * Column name
     */
    /* package private */static final String KEY_PRESENCE_GEOLOC_EXIST_FLAG = "presence_geoloc_exist_flag";

    /**
     * Column name
     */
    /* package private */static final String KEY_PRESENCE_GEOLOC_LATITUDE = "presence_geoloc_latitude";

    /**
     * Column name
     */
    /* package private */static final String KEY_PRESENCE_GEOLOC_LONGITUDE = "presence_geoloc_longitude";

    /**
     * Column name
     */
    /* package private */static final String KEY_PRESENCE_GEOLOC_ALTITUDE = "presence_geoloc_altitude";

    /**
     * Column name
     */
    /* package private */static final String KEY_PRESENCE_TIMESTAMP = "presence_timestamp";

    /**
     * Column name
     */
    /* package private */static final String KEY_CAPABILITY_TIMESTAMP_LAST_REQUEST = "capability_time_last_rqst";

    /**
     * Column name
     */
    /* package private */static final String KEY_CAPABILITY_CS_VIDEO = "capability_cs_video";

    /**
     * Column name
     */
    /* package private */static final String KEY_CAPABILITY_PRESENCE_DISCOVERY = "capability_presence_discovery";

    /**
     * Column name
     */
    /* package private */static final String KEY_CAPABILITY_SOCIAL_PRESENCE = "capability_social_presence";

    /**
     * Column name
     */
    /* package private */static final String KEY_CAPABILITY_FILE_TRANSFER_HTTP = "capability_file_transfer_http";

    /**
     * Column name
     */
    /* package private */static final String KEY_CAPABILITY_FILE_TRANSFER_THUMBNAIL = "capability_file_transfer_thumbnail";

    /**
     * Column name
     */
    // TODO: Refer to Ipcall api here in future releases
    // /* package private */static final String KEY_CAPABILITY_IP_VOICE_CALL =
    // CapabilitiesLog.CAPABILITY_IP_VOICE_CALL;
    /* package private */static final String KEY_CAPABILITY_IP_VOICE_CALL = "capability_ip_voice_call";

    /**
     * Column name
     */
    // TODO: Refer to Ipcall api here in future releases
    // /* package private */static final String KEY_CAPABILITY_IP_VIDEO_CALL =
    // CapabilitiesLog.CAPABILITY_IP_VIDEO_CALL;
    /* package private */static final String KEY_CAPABILITY_IP_VIDEO_CALL = "capability_ip_video_call";

    /**
     * Column name
     */
    /* package private */static final String KEY_CAPABILITY_GROUP_CHAT_SF = "capability_group_chat_sf";

    /**
     * Column name
     */
    /* package private */static final String KEY_CAPABILITY_FILE_TRANSFER_SF = "capability_file_transfer_sf";

    /**
     * Column name
     */
    /* package private */static final String KEY_BLOCKING_TIMESTAMP = "blocking_timestamp";

    /**
     * Column name
     */
    /* package private */static final String KEY_BLOCKED = "blocked";

    /**
     * TRUE value
     */
    /* package private */static final String TRUE_VALUE = Boolean.toString(true);

    /**
     * FALSE value
     */
    /* package private */static final String FALSE_VALUE = Boolean.toString(false);

    /**
     * BLOCKED value is not set
     */
    /* package private */static final int BLOCKED_VALUE_NOT_SET = 0;

    /**
     * BLOCKED value is set
     */
    /* package private */static final int BLOCKED_VALUE_SET = 1;

    /**
     * Aggregation data constants
     */
    /* package private */static final class AggregationData {
        /**
         * Database URI
         */
        /* package private */static final Uri CONTENT_URI = Uri
                .parse("content://com.gsma.rcs.capability/aggregation");

        /**
         * Column name
         */
        /* package private */static final String KEY_ID = "_id";

        /**
         * Column name
         */
        /* package private */static final String KEY_RCS_NUMBER = "rcs_number";

        /**
         * Column name
         */
        /* package private */static final String KEY_RAW_CONTACT_ID = "raw_contact_id";

        /**
         * Column name
         */
        /* package private */static final String KEY_RCS_RAW_CONTACT_ID = "rcs_raw_contact_id";
    }
}
