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
package com.orangelabs.rcs.provider.messaging;

import com.gsma.services.rcs.DeliveryInfo;

import android.net.Uri;

/**
 * Delivery info data constants
 *
 */
public class GroupChatDeliveryInfoData {

    public static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.services.rcs.provider.groupchatdeliveryinfo/deliveryinfo");

    public static final Uri CONTENT_MSG_URI = Uri
            .parse("content://com.gsma.services.rcs.provider.groupchatdeliveryinfo/deliveryinfo/messageid");

    public static final Uri CONTENT_ROW_URI = Uri
            .parse("content://com.gsma.services.rcs.provider.groupchatdeliveryinfo/deliveryinfo/rowid");

    public static final String KEY_ID = "_id";

    public static final String KEY_DELIVERY_STATUS = DeliveryInfo.DELIVERY_STATUS;

    public static final String KEY_REASON_CODE = DeliveryInfo.REASON_CODE;

    public static final String KEY_MSG_ID = DeliveryInfo.MESSAGE_ID;

    public static final String KEY_CHAT_ID = DeliveryInfo.CHAT_ID;

    public static final String KEY_CONTACT = DeliveryInfo.CONTACT;

    public static final String KEY_TIMESTAMP_DELIVERED = DeliveryInfo.TIMESTAMP_DELIVERED;

    public static final String KEY_TIMESTAMP_DISPLAYED = DeliveryInfo.TIMESTAMP_DISPLAYED;
}
