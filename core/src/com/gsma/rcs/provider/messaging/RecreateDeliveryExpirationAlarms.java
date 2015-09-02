/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.service.api.OneToOneUndeliveredImManager;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;

public class RecreateDeliveryExpirationAlarms implements Runnable {

    private final OneToOneUndeliveredImManager mOneToOneUndeliveredImManager;

    private final MessagingLog mMessagingLog;

    private final Object mLock;

    private static final Logger sLogger = Logger.getLogger(RecreateDeliveryExpirationAlarms.class
            .getName());

    public RecreateDeliveryExpirationAlarms(MessagingLog messagingLog,
            OneToOneUndeliveredImManager oneToOneUndeliveredImManager, Object lock) {
        mMessagingLog = messagingLog;
        mOneToOneUndeliveredImManager = oneToOneUndeliveredImManager;
        mLock = lock;
    }

    @Override
    public void run() {
        if (sLogger.isActivated()) {
            sLogger.debug("Execute task to recreate delivery expiration alarms.");
        }
        Cursor cursor = null;
        try {
            synchronized (mLock) {
                long currentTime = System.currentTimeMillis();
                cursor = mMessagingLog.getUndeliveredOneToOneChatMessages();
                int msgIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID);
                int chatMessageContactIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_CONTACT);
                int chatMessageDeliveryExpirationIdx = cursor
                        .getColumnIndexOrThrow(MessageData.KEY_DELIVERY_EXPIRATION);
                while (cursor.moveToNext()) {
                    String msgId = cursor.getString(msgIdIdx);
                    ContactId contact = ContactUtil.createContactIdFromTrustedData(cursor
                            .getString(chatMessageContactIdx));
                    long deliveryExpiration = cursor.getLong(chatMessageDeliveryExpirationIdx);
                    if (deliveryExpiration > currentTime) {
                        mOneToOneUndeliveredImManager
                                .scheduleOneToOneChatMessageDeliveryTimeoutAlarm(contact, msgId,
                                        deliveryExpiration);
                    } else {
                        mOneToOneUndeliveredImManager.handleChatMessageDeliveryExpiration(contact,
                                msgId);
                    }
                }
                CursorUtil.close(cursor);

                cursor = mMessagingLog.getUnDeliveredOneToOneFileTransfers();

                int fileTransferIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FT_ID);
                int fileTransferContactIdx = cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_CONTACT);
                int fileTransferDeliveryExpirationIdx = cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_DELIVERY_EXPIRATION);
                while (cursor.moveToNext()) {
                    String fileTransferId = cursor.getString(fileTransferIdIdx);
                    ContactId contact = ContactUtil.createContactIdFromTrustedData(cursor
                            .getString(fileTransferContactIdx));
                    long deliveryExpiration = cursor.getLong(fileTransferDeliveryExpirationIdx);
                    if (deliveryExpiration > currentTime) {
                        mOneToOneUndeliveredImManager
                                .scheduleOneToOneFileTransferDeliveryTimeoutAlarm(contact,
                                        fileTransferId, deliveryExpiration);
                    } else {
                        mOneToOneUndeliveredImManager.handleFileTransferDeliveryExpiration(contact,
                                fileTransferId);
                    }
                }
            }
        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error(
                    "Exception occured while recreating delivery expiration alarms for one-to-one chat message and one-to-one file transfer ",
                    e);
        } finally {
            CursorUtil.close(cursor);
        }
    }
}
