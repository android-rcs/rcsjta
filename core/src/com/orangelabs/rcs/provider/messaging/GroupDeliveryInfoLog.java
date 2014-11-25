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

package com.orangelabs.rcs.provider.messaging;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.provider.LocalContentResolver;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Class to interface the deliveryinfo table
 */
public class GroupDeliveryInfoLog implements IGroupDeliveryInfoLog {

    private static final String SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT = new StringBuilder(
            GroupDeliveryInfoData.KEY_ID).append("=? AND ")
            .append(GroupDeliveryInfoData.KEY_CONTACT).append("=?").toString();

    private static final String SELECTION_CONTACTS_NOT_RECEIVED_MESSAGE = new StringBuilder(
            GroupDeliveryInfoData.KEY_DELIVERY_STATUS).append("=")
            .append(com.gsma.services.rcs.GroupDeliveryInfoLog.Status.NOT_DELIVERED).append(" OR (")
            .append(GroupDeliveryInfoData.KEY_DELIVERY_STATUS).append("=")
            .append(com.gsma.services.rcs.GroupDeliveryInfoLog.Status.FAILED).append(" AND ")
            .append(GroupDeliveryInfoData.KEY_REASON_CODE).append(" IN (")
            .append(com.gsma.services.rcs.GroupDeliveryInfoLog.ReasonCode.FAILED_DELIVERY).append(",")
            .append(com.gsma.services.rcs.GroupDeliveryInfoLog.ReasonCode.FAILED_DISPLAY).append("))").toString();

    private static final String SELECTION_DELIVERY_INFO_NOT_DISPLAYED = new StringBuilder(
            GroupDeliveryInfoData.KEY_DELIVERY_STATUS).append("!=")
            .append(com.gsma.services.rcs.GroupDeliveryInfoLog.Status.DISPLAYED).toString();

    private static final Logger logger = Logger.getLogger(GroupDeliveryInfoLog.class
            .getSimpleName());

    private final LocalContentResolver mLocalContentResolver;

    /**
     * Constructor
     *
     * @param localContentResolver Local content resolver
     */
    /* package private */ GroupDeliveryInfoLog(LocalContentResolver localContentResolver) {
        mLocalContentResolver = localContentResolver;
    }

    @Override
    public Uri addGroupChatDeliveryInfoEntry(String chatId, ContactId contact, String msgId) {
        if (logger.isActivated()) {
            logger.debug("Add new entry: chatID=" + chatId + ", messageID=" + msgId + ", contact="
                    + contact);
        }
        ContentValues values = new ContentValues();
        values.put(GroupDeliveryInfoData.KEY_CHAT_ID, chatId);
        values.put(GroupDeliveryInfoData.KEY_ID, msgId);
        values.put(GroupDeliveryInfoData.KEY_CONTACT, contact.toString());
        values.put(GroupDeliveryInfoData.KEY_DELIVERY_STATUS, com.gsma.services.rcs.GroupDeliveryInfoLog.Status.NOT_DELIVERED);
        values.put(GroupDeliveryInfoData.KEY_REASON_CODE, com.gsma.services.rcs.GroupDeliveryInfoLog.ReasonCode.UNSPECIFIED);
        values.put(GroupDeliveryInfoData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(GroupDeliveryInfoData.KEY_TIMESTAMP_DISPLAYED, 0);
        return mLocalContentResolver.insert(GroupDeliveryInfoData.CONTENT_URI, values);
    }

    /**
     * Set delivery status for outgoing group chat messages and files
     *
     * @param msgID Message ID
     * @param contact The contact ID for which the entry is to be updated
     * @param status Status
     * @param reasonCode Reason code
     */
    public void updateGroupChatDeliveryInfoStatusAndReasonCode(String msgId, ContactId contact,
            int status, int reasonCode) {
        ContentValues values = new ContentValues();
        values.put(GroupDeliveryInfoData.KEY_DELIVERY_STATUS, status);
        values.put(GroupDeliveryInfoData.KEY_TIMESTAMP_DELIVERED, System.currentTimeMillis());
        values.put(GroupDeliveryInfoData.KEY_REASON_CODE, reasonCode);
        String[] selectionArgs = new String[] {
                msgId, contact.toString()
        };
        if (mLocalContentResolver.update(GroupDeliveryInfoData.CONTENT_URI, values,
                SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT, selectionArgs) < 1) {
            /*
             * TODO: Skip catching exception, which should be implemented in
             * CR037.
             */
            if (logger.isActivated()) {
                logger.warn("There was not group chat delivery into for msgId '" + msgId
                        + "' and contact '" + contact + "' to update!");
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#
     * isDeliveredToAllRecipients(java.lang.String)
     */
    @Override
    public boolean isDeliveredToAllRecipients(String msgId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(GroupDeliveryInfoData.CONTENT_URI, msgId), null,
                    SELECTION_CONTACTS_NOT_RECEIVED_MESSAGE, null, null);
            return !cursor.moveToFirst();

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#
     * isDisplayedByAllRecipients(java.lang.String)
     */
    @Override
    public boolean isDisplayedByAllRecipients(String msgId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(GroupDeliveryInfoData.CONTENT_URI, msgId), null,
                    SELECTION_DELIVERY_INFO_NOT_DISPLAYED, null, null);

            return !cursor.moveToFirst();

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
