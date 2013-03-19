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

package org.gsma.joyn.richcall;

import android.net.Uri;
import java.lang.String;

/**
 * Class RichCallData.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class RichCallData {

    /**
     * Rich call 
     */
    public static final Uri CONTENT_URI = Uri.parse("content://org.gsma.joyn.csh/csh");

    /**
     * Column name KEY_ID.
     */
    public static final String KEY_ID = "_id";

    /**
     * Column name KEY_CONTACT.
     */
    public static final String KEY_CONTACT = "contact";

    /**
     * Column name KEY_DESTINATION.
     */
    public static final String KEY_DESTINATION = "destination";

    /**
     * Column name KEY_MIME_TYPE.
     */
    public static final String KEY_MIME_TYPE = "mime_type";

    /**
     * Column name KEY_NAME.
     */
    public static final String KEY_NAME = "name";

    /**
     * Column name KEY_SIZE.
     */
    public static final String KEY_SIZE = "size";

    /**
     * Column name KEY_DATA.
     */
    public static final String KEY_DATA = "_data";

    /**
     * Column name KEY_TIMESTAMP.
     */
    public static final String KEY_TIMESTAMP = "_date";

    /**
     * Column name KEY_NUMBER_MESSAGES.
     */
    public static final String KEY_NUMBER_MESSAGES = "number_of_messages";

    /**
     * Column name KEY_STATUS.
     */
    public static final String KEY_STATUS = "status";

    /**
     * Column name KEY_SESSION_ID.
     */
    public static final String KEY_SESSION_ID = "sessionId";

    /**
     * Event direction EVENT_INCOMING.
     */
    public static final int EVENT_INCOMING = 1;

    /**
     * Event direction EVENT_OUTGOING.
     */
    public static final int EVENT_OUTGOING = 2;

    /**
     * Status value STATUS_STARTED.
     */
    public static final int STATUS_STARTED = 0;

    /**
     * Status value STATUS_FAILED.
     */
    public static final int STATUS_FAILED = 2;

    /**
     * Status value STATUS_CANCELED.
     */
    public static final int STATUS_CANCELED = 20;

    /**
     * Status value STATUS_TERMINATED.
     */
    public static final int STATUS_TERMINATED = 1;

    /**
     * Creates a new instance of RichCallData.
     */
    public RichCallData() {

    }

}
