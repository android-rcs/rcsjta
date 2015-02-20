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

import com.gsma.services.rcs.GroupDeliveryInfo;

import android.net.Uri;

/**
 * Delivery info data constants
 */
public class GroupDeliveryInfoData {

    public static final Uri CONTENT_URI = GroupDeliveryInfo.CONTENT_URI;

    public static final String KEY_DELIVERY_STATUS = GroupDeliveryInfo.STATUS;

    public static final String KEY_BASECOLUMN_ID = GroupDeliveryInfo.BASECOLUMN_ID;

    public static final String KEY_REASON_CODE = GroupDeliveryInfo.REASON_CODE;

    public static final String KEY_ID = GroupDeliveryInfo.ID;

    public static final String KEY_CHAT_ID = GroupDeliveryInfo.CHAT_ID;

    public static final String KEY_CONTACT = GroupDeliveryInfo.CONTACT;

    public static final String KEY_TIMESTAMP_DELIVERED = GroupDeliveryInfo.TIMESTAMP_DELIVERED;

    public static final String KEY_TIMESTAMP_DISPLAYED = GroupDeliveryInfo.TIMESTAMP_DISPLAYED;
}
