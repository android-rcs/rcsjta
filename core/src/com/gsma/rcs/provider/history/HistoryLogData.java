/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.provider.history;

import com.gsma.services.rcs.history.HistoryLog;

import android.net.Uri;

public class HistoryLogData {

    /**
     * Database URI
     */
    public static final Uri CONTENT_URI = Uri.parse("content://com.gsma.rcs.history/history");

    /* package private */static final String KEY_BASECOLUMN_ID = HistoryLog.BASECOLUMN_ID;

    /* package private */static final String KEY_PROVIDER_ID = HistoryLog.PROVIDER_ID;

    /* package private */static final String KEY_ID = HistoryLog.ID;

    /* package private */static final String KEY_MIME_TYPE = HistoryLog.MIME_TYPE;

    /* package private */static final String KEY_DISPOSITION = HistoryLog.DISPOSITION;

    /* package private */static final String KEY_DIRECTION = HistoryLog.DIRECTION;

    /* package private */static final String KEY_CONTACT = HistoryLog.CONTACT;

    /* package private */static final String KEY_TIMESTAMP = HistoryLog.TIMESTAMP;

    /* package private */static final String KEY_TIMESTAMP_SENT = HistoryLog.TIMESTAMP_SENT;

    /* package private */static final String KEY_TIMESTAMP_DELIVERED = HistoryLog.TIMESTAMP_DELIVERED;

    /* package private */static final String KEY_TIMESTAMP_DISPLAYED = HistoryLog.TIMESTAMP_DISPLAYED;

    /* package private */static final String KEY_EXPIRED_DELIVERY = HistoryLog.EXPIRED_DELIVERY;

    /* package private */static final String KEY_STATUS = HistoryLog.STATUS;

    /* package private */static final String KEY_REASON_CODE = HistoryLog.REASON_CODE;

    /* package private */static final String KEY_READ_STATUS = HistoryLog.READ_STATUS;

    /* package private */static final String KEY_CHAT_ID = HistoryLog.CHAT_ID;

    /* package private */static final String KEY_CONTENT = HistoryLog.CONTENT;

    /* package private */static final String KEY_FILEICON = HistoryLog.FILEICON;

    /* package private */static final String KEY_FILEICON_MIME_TYPE = HistoryLog.FILEICON_MIME_TYPE;

    /* package private */static final String KEY_FILENAME = HistoryLog.FILENAME;

    /* package private */static final String KEY_FILESIZE = HistoryLog.FILESIZE;

    /* package private */static final String KEY_TRANSFERRED = HistoryLog.TRANSFERRED;

    /* package private */static final String KEY_DURATION = HistoryLog.DURATION;

}
