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

package com.gsma.rcs.provider.messaging;

import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfoLog;

import android.net.Uri;

/**
 * Delivery info data constants
 */
/* package private */class GroupDeliveryInfoData {

    /* package private */static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.rcs.groupdeliveryinfo/groupdeliveryinfo");

    /* package private */static final String KEY_STATUS = GroupDeliveryInfoLog.STATUS;

    /* package private */static final String KEY_BASECOLUMN_ID = GroupDeliveryInfoLog.BASECOLUMN_ID;

    /* package private */static final String KEY_REASON_CODE = GroupDeliveryInfoLog.REASON_CODE;

    /* package private */static final String KEY_ID = GroupDeliveryInfoLog.ID;

    /* package private */static final String KEY_CHAT_ID = GroupDeliveryInfoLog.CHAT_ID;

    /* package private */static final String KEY_CONTACT = GroupDeliveryInfoLog.CONTACT;

    /* package private */static final String KEY_TIMESTAMP_DELIVERED = GroupDeliveryInfoLog.TIMESTAMP_DELIVERED;

    /* package private */static final String KEY_TIMESTAMP_DISPLAYED = GroupDeliveryInfoLog.TIMESTAMP_DISPLAYED;
}
