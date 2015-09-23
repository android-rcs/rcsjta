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
import com.orangelabs.rcs.ri.messaging.chat.group.GroupChatDAO;
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
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * File transfer intent service
 * 
 * @author YPLO6403
 */
public class FileTransferIntentService extends IntentService {

    private static final String LOGTAG = LogUtils.getTag(FileTransferIntentService.class
            .getSimpleName());

    /* package private */static final String BUNDLE_FTDAO_ID = "ftdao";

    /* package private */static final String EXTRA_GROUP_FILE = "group_file";

    /**
     * Constructor
     */
    public FileTransferIntentService() {
        super("FileTransferIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // We want this service to stop running if forced stop
        // so return not sticky.
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action;
        if ((action = intent.getAction()) == null) {
            return;
        }
        // Check action from incoming intent
        if (!FileTransferIntent.ACTION_NEW_INVITATION.equals(action)
                && !FileTransferResumeReceiver.ACTION_FT_RESUME.equals(action)) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Unknown action ".concat(action));
            }
            return;
        }
        // Gets data from the incoming Intent
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
        // Get File Transfer from provider
        FileTransferDAO ftDao = FileTransferDAO.getFileTransferDAO(this, transferId);
        if (ftDao == null) {
            return;
        }
        /* Check if a Group CHAT session exists for this file transfer */
        intent.putExtra(EXTRA_GROUP_FILE, GroupChatDAO.isGroupChat(this, ftDao.getChatId()));

        // Check if file transfer is already rejected
        if (FileTransfer.State.REJECTED == ftDao.getState()) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "File transfer already rejected. Id=".concat(transferId));
            }
            return;
        }

        // Save FileTransferDAO into intent
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_FTDAO_ID, ftDao);
        intent.putExtras(bundle);
        if (FileTransferIntent.ACTION_NEW_INVITATION.equals(action)) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "File Transfer invitation filename=" + ftDao.getFilename() + " size="
                        + ftDao.getSize());
            }
            addFileTransferInvitationNotification(intent, ftDao);
        } else {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onHandleIntent file transfer resume with ID ".concat(transferId));
            }
            Intent intentLocal = new Intent(intent);
            if (Direction.INCOMING == ftDao.getDirection()) {
                intentLocal.setClass(this, ReceiveFileTransfer.class);
            } else {
                intentLocal.setClass(this, InitiateFileTransfer.class);
            }
            intentLocal.addFlags(Intent.FLAG_FROM_BACKGROUND);
            intentLocal.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intentLocal.setAction(FileTransferResumeReceiver.ACTION_FT_RESUME);
            startActivity(intentLocal);
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

        /* Create pending intent */
        Intent intent = new Intent(invitation);
        intent.setClass(this, ReceiveFileTransfer.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        /*
         * If the PendingIntent has the same operation, action, data, categories, components, and
         * flags it will be replaced. Invitation should be notified individually so we use a random
         * generator to provide a unique request code and reuse it for the notification.
         */
        int uniqueId = Utils.getUniqueIdForPendingIntent();
        PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueId, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String displayName = RcsContactUtil.getInstance(this).getDisplayName(contact);
        String title = getString(R.string.title_recv_file_transfer);

        /* Create notification */
        NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
        notif.setContentIntent(contentIntent);
        notif.setSmallIcon(R.drawable.ri_notif_file_transfer_icon);
        notif.setWhen(System.currentTimeMillis());
        notif.setAutoCancel(true);
        notif.setOnlyAlertOnce(true);
        notif.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        notif.setDefaults(Notification.DEFAULT_VIBRATE);
        notif.setContentTitle(title);
        notif.setContentText(getString(R.string.label_from_args, displayName));

        /* Send notification */
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(uniqueId, notif.build());
    }

}
