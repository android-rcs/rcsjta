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

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo.ReasonCode;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo.Status;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Class to interface the deliveryinfo table
 */
public class GroupDeliveryInfoLog implements IGroupDeliveryInfoLog {

    private static final String SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT = new StringBuilder(
            GroupDeliveryInfoData.KEY_ID).append("=? AND ")
            .append(GroupDeliveryInfoData.KEY_CONTACT).append("=?").toString();

    private static final String SELECTION_CONTACTS_NOT_RECEIVED_MESSAGE = new StringBuilder(
            GroupDeliveryInfoData.KEY_STATUS).append("=").append(Status.NOT_DELIVERED.toInt())
            .append(" OR (").append(GroupDeliveryInfoData.KEY_STATUS).append("=")
            .append(Status.FAILED.toInt()).append(" AND ")
            .append(GroupDeliveryInfoData.KEY_REASON_CODE).append(" IN (")
            .append(ReasonCode.FAILED_DELIVERY.toInt()).append(",")
            .append(ReasonCode.FAILED_DISPLAY.toInt()).append("))").toString();

    private static final String SELECTION_DELIVERY_INFO_NOT_DISPLAYED = new StringBuilder(
            GroupDeliveryInfoData.KEY_STATUS).append("<>").append(Status.DISPLAYED.toInt())
            .toString();

    private final LocalContentResolver mLocalContentResolver;

    /**
     * Constructor
     *
     * @param localContentResolver Local content resolver
     */
    /* package private */GroupDeliveryInfoLog(LocalContentResolver localContentResolver) {
        mLocalContentResolver = localContentResolver;
    }

    @Override
    public Uri addGroupChatDeliveryInfoEntry(String chatId, ContactId contact, String msgId,
            Status status, ReasonCode reasonCode, long timestampDelivered, long timestampDisplayed) {
        ContentValues values = new ContentValues();
        values.put(GroupDeliveryInfoData.KEY_CHAT_ID, chatId);
        values.put(GroupDeliveryInfoData.KEY_ID, msgId);
        values.put(GroupDeliveryInfoData.KEY_CONTACT, contact.toString());
        values.put(GroupDeliveryInfoData.KEY_STATUS, status.toInt());
        values.put(GroupDeliveryInfoData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(GroupDeliveryInfoData.KEY_TIMESTAMP_DELIVERED, timestampDelivered);
        values.put(GroupDeliveryInfoData.KEY_TIMESTAMP_DISPLAYED, timestampDisplayed);
        return mLocalContentResolver.insert(GroupDeliveryInfoData.CONTENT_URI, values);
    }

    /**
     * Set delivery status for outgoing group chat messages and files
     *
     * @param chatId Group chat ID
     * @param contact The contact ID for which the entry is to be updated
     * @param msgId Message ID
     * @param status Status
     * @param reasonCode Reason code
     * @return true if an entry was updated, otherwise false
     */
    public boolean setGroupChatDeliveryInfoStatusAndReasonCode(String chatId, ContactId contact,
            String msgId, Status status, ReasonCode reasonCode) {
        ContentValues values = new ContentValues();
        values.put(GroupDeliveryInfoData.KEY_STATUS, status.toInt());
        values.put(GroupDeliveryInfoData.KEY_REASON_CODE, reasonCode.toInt());
        String[] selectionArgs = new String[] {
                msgId, contact.toString()
        };

        return (mLocalContentResolver.update(GroupDeliveryInfoData.CONTENT_URI, values,
                SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT, selectionArgs) >= 1);
    }

    @Override
    public boolean isDeliveredToAllRecipients(String msgId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(GroupDeliveryInfoData.CONTENT_URI, msgId), null,
                    SELECTION_CONTACTS_NOT_RECEIVED_MESSAGE, null, null);
            /* TODO: Handle cursor when null. */
            return !cursor.moveToFirst();

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public boolean isDisplayedByAllRecipients(String msgId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(GroupDeliveryInfoData.CONTENT_URI, msgId), null,
                    SELECTION_DELIVERY_INFO_NOT_DISPLAYED, null, null);
            /* TODO: Handle cursor when null. */
            return !cursor.moveToFirst();

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Set outgoing group chat message or file to delivered
     *
     * @param chatId Group chat ID
     * @param contact The contact ID for which the entry is to be updated
     * @param msgId Message ID
     * @param timestampDelivered Time for delivery
     */
    public void setGroupChatDeliveryInfoDelivered(String chatId, ContactId contact, String msgId,
            long timestampDelivered) {
        GroupDeliveryInfo.Status status = GroupDeliveryInfo.Status.DELIVERED;
        GroupDeliveryInfo.ReasonCode reason = GroupDeliveryInfo.ReasonCode.UNSPECIFIED;

        ContentValues values = new ContentValues();
        values.put(GroupDeliveryInfoData.KEY_STATUS, status.toInt());
        values.put(GroupDeliveryInfoData.KEY_TIMESTAMP_DELIVERED, timestampDelivered);
        values.put(GroupDeliveryInfoData.KEY_REASON_CODE, reason.toInt());
        String[] selectionArgs = new String[] {
                msgId, contact.toString()
        };

        if (mLocalContentResolver.update(GroupDeliveryInfoData.CONTENT_URI, values,
                SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT, selectionArgs) < 1) {
            /*
             * No entry updated means that there was no matching row. Adding row and setting
             * displayed timestamp to 0.
             */
            addGroupChatDeliveryInfoEntry(chatId, contact, msgId, status, reason,
                    timestampDelivered, 0);
        }
    }

    /**
     * Set outgoing group chat message or file to displayed
     *
     * @param chatId Group chat ID
     * @param contact The contact ID for which the entry is to be updated
     * @param msgId Message ID
     * @param timestampDisplayed Time for display
     */
    public void setGroupChatDeliveryInfoDisplayed(String chatId, ContactId contact, String msgId,
            long timestampDisplayed) {
        ContentValues values = new ContentValues();
        GroupDeliveryInfo.Status status = GroupDeliveryInfo.Status.DISPLAYED;
        GroupDeliveryInfo.ReasonCode reason = GroupDeliveryInfo.ReasonCode.UNSPECIFIED;

        values.put(GroupDeliveryInfoData.KEY_STATUS, status.toInt());
        values.put(GroupDeliveryInfoData.KEY_TIMESTAMP_DISPLAYED, timestampDisplayed);
        values.put(GroupDeliveryInfoData.KEY_REASON_CODE, reason.toInt());
        String[] selectionArgs = new String[] {
                msgId, contact.toString()
        };

        if (mLocalContentResolver.update(GroupDeliveryInfoData.CONTENT_URI, values,
                SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT, selectionArgs) < 1) {
            /*
             * No entry updated means that there was no matching row. Adding row and setting
             * delivered timestamp to same as displayed timestamp. This is the most reasonable value
             * we can set at this point.
             */
            addGroupChatDeliveryInfoEntry(chatId, contact, msgId, status, reason,
                    timestampDisplayed, timestampDisplayed);
        }
    }
}
