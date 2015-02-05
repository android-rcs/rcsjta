/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.orangelabs.rcs.provider.eab;

import android.net.Uri;

/**
 * Aggregation data constants
 */
public class AggregationData {
    /**
     * Database URI
     */
    static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.services.rcs.provider.capability/aggregation");

    /**
     * Column name
     */
    static final String KEY_ID = "_id";

    /**
     * Column name
     */
    static final String KEY_RCS_NUMBER = "rcs_number";

    /**
     * Column name
     */
    static final String KEY_RAW_CONTACT_ID = "raw_contact_id";

    /**
     * Column name
     */
    static final String KEY_RCS_RAW_CONTACT_ID = "rcs_raw_contact_id";
}
