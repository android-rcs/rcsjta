/*
 * Copyright (C) 2014 Sony Mobile Communications AB.
 * All rights, including trade secret rights, reserved.
 */
package com.orangelabs.rcs.provider.messaging;

import com.gsma.services.rcs.chat.ChatLog;

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

    public static final String KEY_DELIVERY_STATUS = ChatLog.GroupChatDeliveryInfo.DELIVERY_STATUS;

    public static final String KEY_REASON_CODE = ChatLog.GroupChatDeliveryInfo.REASON_CODE;

    public static final String KEY_MSG_ID = ChatLog.GroupChatDeliveryInfo.MESSAGE_ID;

    public static final String KEY_CHAT_ID = ChatLog.GroupChatDeliveryInfo.CHAT_ID;

    public static final String KEY_CONTACT = ChatLog.GroupChatDeliveryInfo.CONTACT;

    public static final String KEY_TIMESTAMP_DELIVERED = ChatLog.GroupChatDeliveryInfo.TIMESTAMP_DELIVERED;

    public static final String KEY_TIMESTAMP_DISPLAYED = ChatLog.GroupChatDeliveryInfo.TIMESTAMP_DISPLAYED;
}
