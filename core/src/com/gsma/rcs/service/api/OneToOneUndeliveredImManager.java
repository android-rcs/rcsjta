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

package com.gsma.rcs.service.api;

import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.OneToOneChatIntent;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class OneToOneUndeliveredImManager {

    /* package private */static final String ACTION_CHAT_MESSAGE_DELIVERY_TIMEOUT = "com.gsma.rcs.action.ONE_TO_ONE_CHAT_MESSAGE_DELIVERY_TIMEOUT";

    /* package private */static final String ACTION_FILE_TRANSFER_DELIVERY_TIMEOUT = "com.gsma.rcs.action.ONE_TO_ONE_FILE_TRANSFER_DELIVERY_TIMEOUT";

    private static final String EXTRA_ID = "id";

    private static final String EXTRA_CONTACT = "contact";

    private final Map<String, PendingIntent> mUndeliveredImAlarms = new HashMap<String, PendingIntent>();

    private final AlarmManager mAlarmManager;

    private final Context mCtx;

    private final MessagingLog mMessagingLog;

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    public OneToOneUndeliveredImManager(Context ctx, MessagingLog messagingLog) {
        mCtx = ctx;
        mMessagingLog = messagingLog;
        mAlarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
    }

    private void cancelTimeOutAlarm(String msgId, PendingIntent pendingIntent) {
        mAlarmManager.cancel(pendingIntent);
    }

    public void cleanup() {
        synchronized (mUndeliveredImAlarms) {
            for (String msgId : mUndeliveredImAlarms.keySet()) {
                cancelTimeOutAlarm(msgId, mUndeliveredImAlarms.get(msgId));
            }
            mUndeliveredImAlarms.clear();
        }
    }

    private PendingIntent createUndeliveredChatMessagePendingIntent(ContactId contact, String msgId) {
        Intent undeliveredMessage = new Intent(mCtx, OneToOneDeliveryExpirationService.class);
        undeliveredMessage.setAction(ACTION_CHAT_MESSAGE_DELIVERY_TIMEOUT);
        undeliveredMessage.putExtra(EXTRA_ID, msgId);
        /*
         * Passing contact as a string due to issues in deserializing parcelable extra from the
         * PendingIntent
         */
        undeliveredMessage.putExtra(EXTRA_CONTACT, contact.toString());
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(undeliveredMessage);
        IntentUtils.tryToSetReceiverForegroundFlag(undeliveredMessage);
        int requestCode = msgId.hashCode();
        return PendingIntent.getService(mCtx, requestCode, undeliveredMessage,
                PendingIntent.FLAG_ONE_SHOT);
    }

    private PendingIntent createUndeliveredFileTransferPendingIntent(ContactId contact, String msgId) {
        Intent undeliveredFile = new Intent(mCtx, OneToOneDeliveryExpirationService.class);
        undeliveredFile.setAction(ACTION_FILE_TRANSFER_DELIVERY_TIMEOUT);
        undeliveredFile.putExtra(EXTRA_ID, msgId);
        /*
         * Passing contact as a string due to issues in deserializing parcelable extra from the
         * PendingIntent
         */
        undeliveredFile.putExtra(EXTRA_CONTACT, contact.toString());
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(undeliveredFile);
        IntentUtils.tryToSetReceiverForegroundFlag(undeliveredFile);
        int requestCode = msgId.hashCode();
        return PendingIntent.getService(mCtx, requestCode, undeliveredFile,
                PendingIntent.FLAG_ONE_SHOT);
    }

    private void scheduleDeliveryTimeoutAlarm(String id, long triggerTime,
            PendingIntent undeliveredIntent) {
        synchronized (mUndeliveredImAlarms) {
            if (!mUndeliveredImAlarms.containsKey(id)) {
                mUndeliveredImAlarms.put(id, undeliveredIntent);

                mAlarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, undeliveredIntent);

            }
        }
    }

    /* package private */void cancelDeliveryTimeoutAlarm(String id) {
        if (mLogger.isActivated()) {
            mLogger.debug("Cancel delivery expiration timer for Id ".concat(id));
        }
        synchronized (mUndeliveredImAlarms) {
            PendingIntent undeliveredMessageAlarm = mUndeliveredImAlarms.remove(id);
            if (undeliveredMessageAlarm != null) {
                cancelTimeOutAlarm(id, undeliveredMessageAlarm);
            }
        }
    }

    public void scheduleOneToOneChatMessageDeliveryTimeoutAlarm(ContactId contact, String msgId,
            long triggerTime) {
        if (mLogger.isActivated()) {
            mLogger.debug("Schedule delivery expiration timer for message with msgId "
                    .concat(msgId));
        }
        scheduleDeliveryTimeoutAlarm(msgId, triggerTime,
                createUndeliveredChatMessagePendingIntent(contact, msgId));
    }

    public void scheduleOneToOneFileTransferDeliveryTimeoutAlarm(ContactId contact,
            String fileTransferId, long triggerTime) {
        if (mLogger.isActivated()) {
            mLogger.debug("Schedule delivery expiration timer for file with fileTransferId "
                    .concat(fileTransferId));
        }
        scheduleDeliveryTimeoutAlarm(fileTransferId, triggerTime,
                createUndeliveredFileTransferPendingIntent(contact, fileTransferId));
    }

    public void handleChatMessageDeliveryExpiration(ContactId contact, String msgId) {
        cancelDeliveryTimeoutAlarm(msgId);
        mMessagingLog.setChatMessageDeliveryExpired(msgId);

        Intent undeliveredMessage = new Intent(OneToOneChatIntent.ACTION_MESSAGE_DELIVERY_EXPIRED);
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(undeliveredMessage);
        IntentUtils.tryToSetReceiverForegroundFlag(undeliveredMessage);
        undeliveredMessage.putExtra(OneToOneChatIntent.EXTRA_CONTACT, (Parcelable) contact);
        undeliveredMessage.putExtra(OneToOneChatIntent.EXTRA_MESSAGE_ID, msgId);
        mCtx.sendBroadcast(undeliveredMessage);
    }

    public void handleFileTransferDeliveryExpiration(ContactId contact, String fileTransferId) {
        cancelDeliveryTimeoutAlarm(fileTransferId);
        mMessagingLog.setFileTransferDeliveryExpired(fileTransferId);

        Intent undeliveredFile = new Intent(
                FileTransferIntent.ACTION_FILE_TRANSFER_DELIVERY_EXPIRED);
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(undeliveredFile);
        IntentUtils.tryToSetReceiverForegroundFlag(undeliveredFile);
        undeliveredFile.putExtra(FileTransferIntent.EXTRA_CONTACT, (Parcelable) contact);
        undeliveredFile.putExtra(FileTransferIntent.EXTRA_TRANSFER_ID, fileTransferId);
        mCtx.sendBroadcast(undeliveredFile);
    }

    public void handleChatMessageDeliveryExpiration(Intent intent) {
        ContactId contact = ContactUtil.createContactIdFromTrustedData(intent
                .getStringExtra(EXTRA_CONTACT));
        String msgId = intent.getStringExtra(EXTRA_ID);
        handleChatMessageDeliveryExpiration(contact, msgId);
    }

    public void handleFileTransferDeliveryExpiration(Intent intent) {
        ContactId contact = ContactUtil.createContactIdFromTrustedData(intent
                .getStringExtra(EXTRA_CONTACT));
        String fileTransferId = intent.getStringExtra(EXTRA_ID);
        handleFileTransferDeliveryExpiration(contact, fileTransferId);
    }
}
