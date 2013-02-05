/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.provider.eventlogs;

/**
 * Class EventLogData.
 */
public class EventLogData {
    /**
     * Constant CONTENT_URI.
     */
    public static final android.net.Uri CONTENT_URI = null;

    /**
     * Constant SMS_MIMETYPE.
     */
    public static final String SMS_MIMETYPE = "sms/text";

    /**
     * Constant MMS_MIMETYPE.
     */
    public static final String MMS_MIMETYPE = "mms/text";

    /**
     * Constant SMS_URI.
     */
    public static final android.net.Uri SMS_URI = null;

    /**
     * Constant MMS_URI.
     */
    public static final android.net.Uri MMS_URI = null;

    /**
     * Constant KEY_EVENT_ROW_ID.
     */
    public static final String KEY_EVENT_ROW_ID = "_id";

    /**
     * Constant KEY_EVENT_TYPE.
     */
    public static final String KEY_EVENT_TYPE = "type";

    /**
     * Constant KEY_EVENT_SESSION_ID.
     */
    public static final String KEY_EVENT_SESSION_ID = "session_id";

    /**
     * Constant KEY_EVENT_DATE.
     */
    public static final String KEY_EVENT_DATE = "_date";

    /**
     * Constant KEY_EVENT_CONTACT.
     */
    public static final String KEY_EVENT_CONTACT = "contact";

    /**
     * Constant KEY_EVENT_STATUS.
     */
    public static final String KEY_EVENT_STATUS = "status";

    /**
     * Constant KEY_EVENT_DATA.
     */
    public static final String KEY_EVENT_DATA = "_data";

    /**
     * Constant KEY_EVENT_MESSAGE_ID.
     */
    public static final String KEY_EVENT_MESSAGE_ID = "message_id";

    /**
     * Constant KEY_EVENT_IS_SPAM.
     */
    public static final String KEY_EVENT_IS_SPAM = "is_spam";

    /**
     * Constant KEY_EVENT_MIMETYPE.
     */
    public static final String KEY_EVENT_MIMETYPE = "mime_type";

    /**
     * Constant KEY_EVENT_NAME.
     */
    public static final String KEY_EVENT_NAME = "name";

    /**
     * Constant KEY_EVENT_SIZE.
     */
    public static final String KEY_EVENT_SIZE = "size";

    /**
     * Constant KEY_EVENT_TOTAL_SIZE.
     */
    public static final String KEY_EVENT_TOTAL_SIZE = "total_size";

    /**
     * Constant KEY_EVENT_CHAT_ID.
     */
    public static final String KEY_EVENT_CHAT_ID = "chat_id";

    /**
     * Constant VALUE_EVENT_DEST_INCOMING.
     */
    public static final int VALUE_EVENT_DEST_INCOMING = 1;

    /**
     * Constant VALUE_EVENT_DEST_OUTGOING.
     */
    public static final int VALUE_EVENT_DEST_OUTGOING = 2;

    /**
     * Constant VALUE_EVENT_DEST_MISSED.
     */
    public static final int VALUE_EVENT_DEST_MISSED = 3;

    /**
     * Creates a new instance of EventLogData.
     */
    public EventLogData() {

    }

} // end EventLogData
