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

package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.RcsService.Direction;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Content provider for video sharing history
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingLog {
    /**
     * Content provider URI
     */
    public static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.services.rcs.provider.videoshare/videoshare");

    /**
     * History log member id
     */
    public static final int HISTORYLOG_MEMBER_ID = 4;

    /**
     * The name of the column containing the unique id across provider tables.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String BASECOLUMN_ID = BaseColumns._ID;

    /**
     * The name of the column containing the unique ID of the video sharing.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String SHARING_ID = "sharing_id";

    /**
     * The name of the column containing the MSISDN of the remote contact.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String CONTACT = "contact";

    /**
     * The name of the column containing the direction of the sharing.
     * <P>
     * Type: INTEGER
     * </P>
     * 
     * @see Direction
     */
    public static final String DIRECTION = "direction";

    /**
     * The name of the column containing the date of the sharing (in milliseconds).
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String TIMESTAMP = "timestamp";

    /**
     * The name of the column containing the duration of the sharing (in milliseconds). The value is
     * only set at the end of the call.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String DURATION = "duration";

    /**
     * The name of the column containing the state of the sharing.
     * <P>
     * Type: INTEGER
     * </P>
     * 
     * @see VideoSharing.State
     */
    public static final String STATE = "state";

    /**
     * The name of the column containing the reason code of the state.
     * <P>
     * Type: INTEGER
     * </P>
     * 
     * @see VideoSharing.ReasonCode
     */
    public static final String REASON_CODE = "reason_code";

    /**
     * The name of the column containing the encoding type of video.
     * <P>
     * Type: INTEGER
     * </P>
     * 
     * @see VideoSharing.Encoding
     */
    public static final String VIDEO_ENCODING = "video_encoding";

    /**
     * The name of the column containing the width of video.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String WIDTH = "width";

    /**
     * The name of the column containing the height of video.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String HEIGHT = "height";

    private VideoSharingLog() {
    }
}
