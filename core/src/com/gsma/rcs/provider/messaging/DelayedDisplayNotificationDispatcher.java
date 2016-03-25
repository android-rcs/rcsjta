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

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;

/**
 * Delayed Display Notification Dispatcher retrieves those text messages for which requested display
 * reports have not yet been successfully sent.
 */
public class DelayedDisplayNotificationDispatcher implements Runnable {

    private static final Logger sLogger = Logger
            .getLogger(DelayedDisplayNotificationDispatcher.class.getName());

    private static final String[] PROJECTION_CHAT_MESSAGE = new String[] {
            MessageData.KEY_MESSAGE_ID, MessageData.KEY_CHAT_ID, MessageData.KEY_CONTACT,
            MessageData.KEY_TIMESTAMP_DISPLAYED
    };

    private final static String SELECTION_READ_CHAT_MESSAGES_WITH_DISPLAY_REPORT_REQUESTED = MessageData.KEY_MIME_TYPE
            + " IN('"
            + MimeType.TEXT_MESSAGE
            + "','"
            + MimeType.GEOLOC_MESSAGE
            + "') AND "
            + MessageData.KEY_READ_STATUS
            + "="
            + ReadStatus.READ.toInt()
            + " AND "
            + MessageData.KEY_STATUS + "=" + Status.DISPLAY_REPORT_REQUESTED.toInt();

    private static final String ORDER_BY_TIMESTAMP_ASC = MessageData.KEY_TIMESTAMP.concat(" ASC");

    private LocalContentResolver mLocalContentResolver;

    private ChatServiceImpl mChatApi;

    public DelayedDisplayNotificationDispatcher(LocalContentResolver localContentResolver,
            ChatServiceImpl chatApi) {
        mLocalContentResolver = localContentResolver;
        mChatApi = chatApi;
    }

    @Override
    public void run() {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, PROJECTION_CHAT_MESSAGE,
                    SELECTION_READ_CHAT_MESSAGES_WITH_DISPLAY_REPORT_REQUESTED, null,
                    ORDER_BY_TIMESTAMP_ASC);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            int columIdxMessageId = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID);
            int columnIdxContact = cursor.getColumnIndexOrThrow(MessageData.KEY_CONTACT);
            int columnIdxTimestampDisplayed = cursor
                    .getColumnIndexOrThrow(MessageData.KEY_TIMESTAMP_DISPLAYED);
            int columnIdxChatId = cursor.getColumnIndexOrThrow(MessageData.KEY_CHAT_ID);
            while (cursor.moveToNext()) {
                String contactNumber = cursor.getString(columnIdxContact);
                String chatId = cursor.getString(columnIdxChatId);
                String msgId = cursor.getString(columIdxMessageId);
                long timestampDisplayed = cursor.getLong(columnIdxTimestampDisplayed);
                /* Do no check validity for trusted data */
                ContactId contact = ContactUtil.createContactIdFromTrustedData(contactNumber);

                if (chatId.equals(contactNumber)) {
                    mChatApi.sendOne2OneDisplayedDeliveryReport(msgId, contact, timestampDisplayed);
                } else {
                    mChatApi.sendGroupChatDisplayedDeliveryReport(msgId, contact,
                            timestampDisplayed, chatId);
                }
            }
        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to dispatch display notifications!", e);
        } finally {
            CursorUtil.close(cursor);
        }
    }
}
