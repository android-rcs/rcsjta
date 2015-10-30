/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.ri.messaging.filetransfer;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.chat.ChatPendingIntentManager;
import com.orangelabs.rcs.ri.messaging.chat.single.SingleChatView;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * File transfer intent service
 * 
 * @author Philippe LEMORDANT
 */
public class FileTransferIntentService extends IntentService {

    private static final String LOGTAG = LogUtils.getTag(FileTransferIntentService.class
            .getSimpleName());

    /**
     * Constructor
     */
    public FileTransferIntentService() {
        super("FileTransferIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        /*
         * We want this service to stop running if forced stop so return not sticky.
         */
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action;
        if ((action = intent.getAction()) == null) {
            return;
        }
        /* Check action from incoming intent */
        if (!FileTransferIntent.ACTION_NEW_INVITATION.equals(action)
                && !FileTransferIntent.ACTION_RESUME.equals(action)
                && !FileTransferIntent.ACTION_FILE_TRANSFER_DELIVERY_EXPIRED.equals(action)) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Unknown action ".concat(action));
            }
            return;
        }
        String transferId = intent.getStringExtra(FileTransferIntent.EXTRA_TRANSFER_ID);
        if (transferId == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read transfer ID");
            }
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onHandleIntent file transfer with ID ".concat(transferId));
        }
        if (FileTransferIntent.ACTION_FILE_TRANSFER_DELIVERY_EXPIRED.equals(action)) {
            handleUndeliveredFileTransfer(intent, transferId);
            return;
        }
        /* Get File Transfer from provider */
        FileTransferDAO ftDao = FileTransferDAO.getFileTransferDAO(this, transferId);
        if (ftDao == null) {
            return;
        }
        /* Check if file transfer is already rejected */
        if (FileTransfer.State.REJECTED == ftDao.getState()) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "File transfer already rejected. Id=".concat(transferId));
            }
            return;
        }
        if (FileTransferIntent.ACTION_NEW_INVITATION.equals(action)) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "File Transfer invitation filename=" + ftDao.getFilename() + " size="
                        + ftDao.getSize());
            }
            addFileTransferInvitationNotification(intent, ftDao);
            return;
        }
        /* File transfer is resuming */
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onHandleIntent file transfer resume with ID ".concat(transferId));
        }
        if (Direction.INCOMING == ftDao.getDirection()) {
            startActivity(ReceiveFileTransfer.forgeResumeIntent(this, ftDao, intent));
        } else {
            startActivity(InitiateFileTransfer.forgeResumeIntent(this, ftDao, intent));
        }
    }

    /**
     * Add file transfer notification
     * 
     * @param invitation Intent invitation
     * @param ftDao the file transfer data object
     */
    private void addFileTransferInvitationNotification(Intent invitation, FileTransferDAO ftDao) {
        ContactId contact = ftDao.getContact();
        if (ftDao.getContact() == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "addFileTransferInvitationNotification failed: cannot parse contact");
            }
            return;
        }
        Intent intent = ReceiveFileTransfer.forgeInvitationIntent(this, ftDao, invitation);
        /*
         * If the PendingIntent has the same operation, action, data, categories, components, and
         * flags it will be replaced. Invitation should be notified individually so we use a random
         * generator to provide a unique request code and reuse it for the notification.
         */
        int uniqueId = Utils.getUniqueIdForPendingIntent();
        PendingIntent pi = PendingIntent.getActivity(this, uniqueId, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String displayName = RcsContactUtil.getInstance(this).getDisplayName(contact);
        String title = getString(R.string.title_recv_file_transfer);
        String message = getString(R.string.label_from_args, displayName);

        /* Send notification */
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notif = buildNotification(pi, title, message);
        notificationManager.notify(uniqueId, notif);
    }

    private void handleUndeliveredFileTransfer(Intent intent, String transferId) {
        ContactId contact = intent.getParcelableExtra(FileTransferIntent.EXTRA_CONTACT);
        if (contact == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read contact for ftId=".concat(transferId));
            }
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Undelivered file transfer ID=" + transferId + " for contact " + contact);
        }
        forwardUndeliveredFileTransferToUi(intent, contact, transferId);
    }

    private void forwardUndeliveredFileTransferToUi(Intent undeliveredIntent, ContactId contact,
            String transferId) {
        Intent intent = SingleChatView.forgeIntentOnStackEvent(this, contact, undeliveredIntent);
        ChatPendingIntentManager pendingIntentmanager = ChatPendingIntentManager
                .getChatPendingIntentManager(this);
        Integer uniqueId = pendingIntentmanager.tryContinueChatConversation(intent,
                contact.toString());
        if (uniqueId != null) {
            PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            String displayName = RcsContactUtil.getInstance(this).getDisplayName(contact);
            String title = getString(R.string.title_undelivered_filetransfer);
            String msg = getString(R.string.label_undelivered_filetransfer, displayName);
            Notification notif = buildNotification(contentIntent, title, msg);
            pendingIntentmanager.postNotification(uniqueId, notif);
        }
    }

    /**
     * Generate a notification
     * 
     * @param pendingIntent pending intent
     * @param title title
     * @param message message
     * @return the notification
     */
    private Notification buildNotification(PendingIntent pendingIntent, String title, String message) {
        NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
        notif.setContentIntent(pendingIntent);
        notif.setSmallIcon(R.drawable.ri_notif_file_transfer_icon);
        notif.setWhen(System.currentTimeMillis());
        notif.setAutoCancel(true);
        notif.setOnlyAlertOnce(true);
        notif.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        notif.setDefaults(Notification.DEFAULT_VIBRATE);
        notif.setContentTitle(title);
        notif.setContentText(message);
        return notif.build();
    }

}
