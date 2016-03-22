/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.services.rcs.history;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Content provider for history log
 */
public class HistoryLog {

    /**
     * Content provider URI for history log
     */
    public static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.services.rcs.provider.history/history");

    /**
     * The name of the column containing the unique id across provider tables.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String BASECOLUMN_ID = BaseColumns._ID;

    /**
     * The name of the column containing the provider the entry originates from.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String PROVIDER_ID = "provider_id";

    /**
     * The name of the column containing the entry ID.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String ID = "id";

    /**
     * The name of the column containing the MIME-type.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String MIME_TYPE = "mime_type";

    /**
     * The name of the column containing the disposition.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String DISPOSITION = "disposition";

    /**
     * The name of the column containing the direction.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String DIRECTION = "direction";

    /**
     * The name of the column containing the ContactId formatted number.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String CONTACT = "contact";

    /**
     * The name of the column containing the time when the entry was inserted.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String TIMESTAMP = "timestamp";

    /**
     * The name of the column containing the time when the entry was sent. 0 means not sent.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String TIMESTAMP_SENT = "timestamp_sent";

    /**
     * The name of the column containing the time when the entry was delivered. 0 means not
     * delivered.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String TIMESTAMP_DELIVERED = "timestamp_delivered";

    /**
     * The name of the column containing the time when the entry was displayed. 0 means not
     * displayed.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String TIMESTAMP_DISPLAYED = "timestamp_displayed";

    /**
     * The name of the column denoting if delivery has expired for this file. Values: 1 (true), 0
     * (false)
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String EXPIRED_DELIVERY = "expired_delivery";

    /**
     * The name of the column containing the status (or state).
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String STATUS = "status";

    /**
     * The name of the column containing the reason code associated with the entry status.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String REASON_CODE = "reason_code";

    /**
     * The name of the column containing the read status of the event.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String READ_STATUS = "read_status";

    /**
     * The name of the column containing the chat ID.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String CHAT_ID = "chat_id";

    /**
     * The name of the column containing the content of the message if this entry corresponds to a
     * content message or the file URI if this entry is a file transfer, image share, video share
     * etc.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String CONTENT = "content";

    /**
     * The name of the column containing the file icon URI.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String FILEICON = "fileicon";

    /**
     * The name of the column containing the file icon MIME-type.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String FILEICON_MIME_TYPE = "fileicon_mime_type";

    /**
     * The name of the column containing the filename of a file transfer.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String FILENAME = "filename";

    /**
     * The name of the column containing the file size of a file transfer.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String FILESIZE = "filesize";

    /**
     * The name of the column containing the transferred amount of data (in bytes).
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String TRANSFERRED = "transferred";

    /**
     * The name of the column containing the duration of a call or sharing (in milliseconds).
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String DURATION = "duration";

    private HistoryLog() {
    }
}
