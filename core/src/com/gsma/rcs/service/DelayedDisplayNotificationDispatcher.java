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

package com.gsma.rcs.service;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.utils.ContactUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;

import android.database.Cursor;

/**
 * Delayed Display Notification Dispatcher retrieves those text messages for which requested display
 * reports have not yet been successfully sent.
 */
/* package private */class DelayedDisplayNotificationDispatcher implements Runnable {

    private final static String SELECTION_READ_CHAT_MESSAGES_WITH_DISPLAY_REPORT_REQUESTED = new StringBuilder(
            Message.CHAT_ID).append("=").append(Message.CONTACT).append(" AND ")
            .append(Message.MIME_TYPE).append(" IN('").append(MimeType.TEXT_MESSAGE).append("','")
            .append(MimeType.GEOLOC_MESSAGE).append("') AND ").append(Message.READ_STATUS)
            .append("=").append(ReadStatus.READ.toInt()).append(" AND ").append(Message.STATUS)
            .append("=").append(Status.DISPLAY_REPORT_REQUESTED.toInt()).toString();

    private static final String ORDER_BY_TIMESTAMP_ASC = Message.TIMESTAMP.concat(" ASC");

    private static Logger logger = Logger.getLogger(DelayedDisplayNotificationDispatcher.class
            .getName());

    private LocalContentResolver mLocalContentResolver;

    private ChatServiceImpl mChatApi;

    /* package private */DelayedDisplayNotificationDispatcher(
            LocalContentResolver localContentResolver, ChatServiceImpl chatApi) {
        mLocalContentResolver = localContentResolver;
        mChatApi = chatApi;
    }

    @Override
    public void run() {
        Cursor cursor = null;
        try {
            String[] projection = new String[] {
                    Message.MESSAGE_ID, Message.CONTACT, Message.TIMESTAMP_DISPLAYED
            };
            cursor = mLocalContentResolver.query(Message.CONTENT_URI, projection,
                    SELECTION_READ_CHAT_MESSAGES_WITH_DISPLAY_REPORT_REQUESTED, null,
                    ORDER_BY_TIMESTAMP_ASC);
            while (cursor.moveToNext()) {
                String msgId = cursor.getString(cursor.getColumnIndexOrThrow(Message.MESSAGE_ID));
                String contactNumber = cursor.getString(cursor
                        .getColumnIndexOrThrow(Message.CONTACT));
                long timestamp = cursor.getLong(cursor
                        .getColumnIndexOrThrow(Message.TIMESTAMP_DISPLAYED));
                try {
                    mChatApi.tryToSendOne2OneDisplayedDeliveryReport(msgId,
                            ContactUtils.createContactId(contactNumber), timestamp);
                } catch (RcsContactFormatException e) {
                    if (logger.isActivated()) {
                        logger.error("Cannot parse contact " + contactNumber);
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error(
                        "Could not retrieve messages for which delayed display notification should be send!",
                        e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
