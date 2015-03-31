/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.gsma.rcs.provider.sharing;

import com.gsma.services.rcs.sharing.geoloc.GeolocSharingLog;

import android.net.Uri;

/**
 * Geoloc sharing data constants
 */
public class GeolocSharingData {

    public static final Uri CONTENT_URI = GeolocSharingLog.CONTENT_URI;

    public static final int HISTORYLOG_MEMBER_ID = GeolocSharingLog.HISTORYLOG_MEMBER_ID;

    /* package private */static final String KEY_BASECOLUMN_ID = GeolocSharingLog.BASECOLUMN_ID;

    /* package private */static final String KEY_SHARING_ID = GeolocSharingLog.SHARING_ID;

    /* package private */static final String KEY_CONTACT = GeolocSharingLog.CONTACT;

    /* package private */static final String KEY_CONTENT = GeolocSharingLog.CONTENT;

    /* package private */static final String KEY_MIME_TYPE = GeolocSharingLog.MIME_TYPE;

    /* package private */static final String KEY_DIRECTION = GeolocSharingLog.DIRECTION;

    /* package private */static final String KEY_STATE = GeolocSharingLog.STATE;

    /* package private */static final String KEY_REASON_CODE = GeolocSharingLog.REASON_CODE;

    /* package private */static final String KEY_TIMESTAMP = GeolocSharingLog.TIMESTAMP;
}
