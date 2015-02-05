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

package com.orangelabs.rcs.core.ims.service.im.chat.imdn;

/**
 * IMDN document
 * 
 * @author jexa7410
 */
public class ImdnDocument {
    /**
     * MIME type
     */
    public static final String MIME_TYPE = "message/imdn+xml";

    /**
     * Imdn tag indicating delivery notification
     */
    public static final String DELIVERY_NOTIFICATION = "delivery-notification";

    /**
     * Imdn tag indicating display notification
     */
    public static final String DISPLAY_NOTIFICATION = "display-notification";

    public static final String DELIVERY_STATUS_DELIVERED = "delivered";

    public static final String DELIVERY_STATUS_DISPLAYED = "displayed";

    public static final String DELIVERY_STATUS_FAILED = "failed";

    public static final String DELIVERY_STATUS_ERROR = "error";

    public static final String DELIVERY_STATUS_FORBIDDEN = "forbidden";

    public static final String IMDN_TAG = "imdn";

    public static final String MESSAGE_ID_TAG = "message-id";

    public static final String IMDN_NAMESPACE = "imdn <urn:ietf:params:imdn>";

    /**
     * Disposition notification header positive delivery value
     */
    public static final String POSITIVE_DELIVERY = "positive-delivery";

    /**
     * Disposition notification header display value
     */
    public static final String DISPLAY = "display";

    /**
     * Content-Disposition header notification value
     */
    public static final String NOTIFICATION = "notification";

    private final String mMsgId;

    private final String mStatus;

    private final String mNotificationType;

    public ImdnDocument(String msgId, String notificationType, String status) {
        mMsgId = msgId;
        mNotificationType = notificationType;
        mStatus = status;
    }

    /**
     * Get message ID
     * 
     * @return Message ID
     */
    public String getMsgId() {
        return mMsgId;
    }

    /**
     * Get status
     * 
     * @return Status
     */
    public String getStatus() {
        return mStatus;
    }

    /**
     * Get notification type
     * 
     * @return Notification type
     */
    public String getNotificationType() {
        return mNotificationType;
    }
}
