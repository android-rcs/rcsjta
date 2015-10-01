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

package com.gsma.rcs.core.ims.service.im.chat.imdn;

import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.service.api.OneToOneDeliveryExpirationService;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.TimerUtils;
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

public class DeliveryExpirationManager {

    public static final String ACTION_CHAT_MESSAGE_DELIVERY_TIMEOUT = "com.gsma.rcs.action.ONE_TO_ONE_CHAT_MESSAGE_DELIVERY_TIMEOUT";

    public static final String ACTION_FILE_TRANSFER_DELIVERY_TIMEOUT = "com.gsma.rcs.action.ONE_TO_ONE_FILE_TRANSFER_DELIVERY_TIMEOUT";

    private static final String EXTRA_ID = "id";

    private static final String EXTRA_CONTACT = "contact";

    private final Map<String, PendingIntent> mUndeliveredImAlarms = new HashMap<String, PendingIntent>();

    private final AlarmManager mAlarmManager;

    private final Context mCtx;

    private final MessagingLog mMessagingLog;

    private final InstantMessagingService mImService;

    private static final Logger sLogger = Logger.getLogger(DeliveryExpirationManager.class
            .getSimpleName());

    public DeliveryExpirationManager(InstantMessagingService imService, Context ctx,
            MessagingLog messagingLog) {
        mCtx = ctx;
        mMessagingLog = messagingLog;
        mImService = imService;
        mAlarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
    }

    private void cancelTimeOutAlarm(PendingIntent pendingIntent) {
        mAlarmManager.cancel(pendingIntent);
    }

    public void cleanup() {
        synchronized (mUndeliveredImAlarms) {
            for (String msgId : mUndeliveredImAlarms.keySet()) {
                cancelTimeOutAlarm(mUndeliveredImAlarms.get(msgId));
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
        undeliveredMessage.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
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
        undeliveredFile.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
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
                TimerUtils.setExactTimer(mAlarmManager, triggerTime, undeliveredIntent);
            }
        }
    }

    public void cancelDeliveryTimeoutAlarm(String id) {
        if (sLogger.isActivated()) {
            sLogger.debug("Cancel delivery expiration timer for Id ".concat(id));
        }
        synchronized (mUndeliveredImAlarms) {
            PendingIntent undeliveredMessageAlarm = mUndeliveredImAlarms.remove(id);
            if (undeliveredMessageAlarm != null) {
                cancelTimeOutAlarm(undeliveredMessageAlarm);
            }
        }
    }

    public void scheduleOneToOneChatMessageDeliveryTimeoutAlarm(ContactId contact, String msgId,
            long triggerTime) {
        if (sLogger.isActivated()) {
            sLogger.debug("Schedule delivery expiration timer for message with msgId "
                    .concat(msgId));
        }
        scheduleDeliveryTimeoutAlarm(msgId, triggerTime,
                createUndeliveredChatMessagePendingIntent(contact, msgId));
    }

    public void scheduleOneToOneFileTransferDeliveryTimeoutAlarm(ContactId contact,
            String fileTransferId, long triggerTime) {
        if (sLogger.isActivated()) {
            sLogger.debug("Schedule delivery expiration timer for file with fileTransferId "
                    .concat(fileTransferId));
        }
        scheduleDeliveryTimeoutAlarm(fileTransferId, triggerTime,
                createUndeliveredFileTransferPendingIntent(contact, fileTransferId));
    }

    public void onChatMessageDeliveryExpirationReceived(ContactId contact, String msgId) {
        cancelDeliveryTimeoutAlarm(msgId);
        mMessagingLog.setChatMessageDeliveryExpired(msgId);
        mImService.getImsModule().getCapabilityService().requestContactCapabilities(contact);

        Intent undeliveredMessage = new Intent(OneToOneChatIntent.ACTION_MESSAGE_DELIVERY_EXPIRED);
        undeliveredMessage.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
        IntentUtils.tryToSetReceiverForegroundFlag(undeliveredMessage);
        undeliveredMessage.putExtra(OneToOneChatIntent.EXTRA_CONTACT, (Parcelable) contact);
        undeliveredMessage.putExtra(OneToOneChatIntent.EXTRA_MESSAGE_ID, msgId);
        mCtx.sendBroadcast(undeliveredMessage);
    }

    public void onFileTransferDeliveryExpirationReceived(ContactId contact, String fileTransferId) {
        cancelDeliveryTimeoutAlarm(fileTransferId);
        mMessagingLog.setFileTransferDeliveryExpired(fileTransferId);
        mImService.getImsModule().getCapabilityService().requestContactCapabilities(contact);

        Intent undeliveredFile = new Intent(
                FileTransferIntent.ACTION_FILE_TRANSFER_DELIVERY_EXPIRED);
        undeliveredFile.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
        IntentUtils.tryToSetReceiverForegroundFlag(undeliveredFile);
        undeliveredFile.putExtra(FileTransferIntent.EXTRA_CONTACT, (Parcelable) contact);
        undeliveredFile.putExtra(FileTransferIntent.EXTRA_TRANSFER_ID, fileTransferId);
        mCtx.sendBroadcast(undeliveredFile);
    }

    /**
     * Handle one-one chat message delivery expiration
     *
     * @param intent
     */
    public void onChatMessageDeliveryExpirationReceived(Intent intent) {
        ContactId contact = ContactUtil.createContactIdFromTrustedData(intent
                .getStringExtra(EXTRA_CONTACT));
        String msgId = intent.getStringExtra(EXTRA_ID);
        onChatMessageDeliveryExpirationReceived(contact, msgId);
    }

    /**
     * Handle one-one file transfer delivery expiration
     *
     * @param intent
     */
    public void onFileTransferDeliveryExpirationReceived(Intent intent) {
        ContactId contact = ContactUtil.createContactIdFromTrustedData(intent
                .getStringExtra(EXTRA_CONTACT));
        String fileTransferId = intent.getStringExtra(EXTRA_ID);
        onFileTransferDeliveryExpirationReceived(contact, fileTransferId);
    }
}
