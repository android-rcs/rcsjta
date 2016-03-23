/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.services.rcs.sharing.geoloc;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Content provider for geoloc sharing history
 */
public class GeolocSharingLog {

    /**
     * Content provider URI for geoloc sharings
     */
    public static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.services.rcs.provider.geolocshare/geolocshare");

    /**
     * History log member id
     */
    public static final int HISTORYLOG_MEMBER_ID = 5;

    /**
     * The name of the column containing the unique id across provider tables.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String BASECOLUMN_ID = BaseColumns._ID;

    /**
     * The name of the column containing the unique sharing ID.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String SHARING_ID = "sharing_id";

    /**
     * The name of the column containing the ContactId formatted number of the remote contact.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String CONTACT = "contact";

    /**
     * The name of the column containing the geolocation stored as a String that you can pass as an
     * argument to the Geoloc constructor.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String CONTENT = "content";

    /**
     * The name of the column containing the MIME-type of the geoloc sharing.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String MIME_TYPE = "mime_type";

    /**
     * The name of the column containing the geoloc sharing direction.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String DIRECTION = "direction";

    /**
     * The name of the column containing the time when geoloc sharing is created.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String TIMESTAMP = "timestamp";

    /**
     * The name of the column containing the geoloc sharing state.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String STATE = "state";

    /**
     * The name of the column containing the reason code associated with the geoloc sharing state.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String REASON_CODE = "reason_code";

    private GeolocSharingLog() {
    }
}
