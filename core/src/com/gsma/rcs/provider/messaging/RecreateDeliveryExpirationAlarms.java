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

import com.gsma.rcs.service.api.OneToOneUndeliveredImManager;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;
import android.database.SQLException;

public class RecreateDeliveryExpirationAlarms implements Runnable {

    private final OneToOneUndeliveredImManager mOneToOneUndeliveredImManager;

    private final MessagingLog mMessagingLog;

    private final Object mLock;

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    public RecreateDeliveryExpirationAlarms(MessagingLog messagingLog,
            OneToOneUndeliveredImManager oneToOneUndeliveredImManager, Object lock) {
        mMessagingLog = messagingLog;
        mOneToOneUndeliveredImManager = oneToOneUndeliveredImManager;
        mLock = lock;
    }

    @Override
    public void run() {
        if (mLogger.isActivated()) {
            mLogger.debug("Execute task to recreate delivery expiration alarms.");
        }
        Cursor cursor = null;
        try {
            synchronized (mLock) {
                long currentTime = System.currentTimeMillis();
                cursor = mMessagingLog.getOneToOneChatMessagesWithUnexpiredDelivery(currentTime);
                int msgIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID);
                int chatMessageContactIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_CONTACT);
                int chatMessageDeliveryExpirationIdx = cursor
                        .getColumnIndexOrThrow(MessageData.KEY_DELIVERY_EXPIRATION);
                while (cursor.moveToNext()) {
                    String msgId = cursor.getString(msgIdIdx);
                    ContactId contact = ContactUtil.createContactIdFromTrustedData(cursor
                            .getString(chatMessageContactIdx));
                    long deliveryExpiration = cursor.getInt(chatMessageDeliveryExpirationIdx);
                    mOneToOneUndeliveredImManager.scheduleOneToOneChatMessageDeliveryTimeoutAlarm(
                            contact, msgId, deliveryExpiration);
                }
                cursor.close();

                cursor = mMessagingLog.getOneToOneFileTransfersWithUnexpiredDelivery(currentTime);
                int fileTransferIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FT_ID);
                int fileTransferContactIdx = cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_CONTACT);
                int fileTransferDeliveryExpirationIdx = cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_DELIVERY_EXPIRATION);
                while (cursor.moveToNext()) {
                    String fileTransferId = cursor.getString(fileTransferIdIdx);
                    ContactId contact = ContactUtil.createContactIdFromTrustedData(cursor
                            .getString(fileTransferContactIdx));
                    long deliveryExpiration = cursor.getInt(fileTransferDeliveryExpirationIdx);
                    mOneToOneUndeliveredImManager.scheduleOneToOneFileTransferDeliveryTimeoutAlarm(
                            contact, fileTransferId, deliveryExpiration);
                }
            }
        } catch (SQLException e) {
            /*
             * Exceptions will be handled better in CR037.
             */
            mLogger.error(
                    "Exception occured while recreating delivery expiration alarms for one-to-one chat message and one-to-one file transfer ",
                    e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
