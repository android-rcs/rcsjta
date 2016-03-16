/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.messaging.filetransfer;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;
import com.gsma.services.rcs.filetransfer.FileTransferLog;

import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.messaging.OneToOneTalkView;
import com.gsma.rcs.ri.messaging.TalkList;
import com.gsma.rcs.ri.messaging.chat.ChatPendingIntentManager;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.rcs.ri.utils.Utils;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * File transfer intent service
 * 
 * @author Philippe LEMORDANT
 */
public class FileTransferIntentService extends IntentService {

    private static final String LOGTAG = LogUtils.getTag(FileTransferIntentService.class.getName());
    private final static String[] PROJ_UNDELIVERED_FT = new String[] {
        FileTransferLog.FT_ID
    };

    private static final String SEL_UNDELIVERED_FTS = FileTransferLog.CHAT_ID + "=? AND "
            + FileTransferLog.EXPIRED_DELIVERY + "='1'";

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
        switch (action) {
            case FileTransferIntent.ACTION_FILE_TRANSFER_DELIVERY_EXPIRED:
                handleUndeliveredFileTransfer(intent, transferId);
                break;
            case FileTransferIntent.ACTION_NEW_INVITATION:
                handleFileTransferInvitation(intent, transferId);
                break;
            case FileTransferIntent.ACTION_RESUME:
                handleFileTransferResume(intent, transferId);
                break;
            default:
                Log.e(LOGTAG, "Unknown action ".concat(action));
        }
    }

    private void handleFileTransferResume(Intent intent, String transferId) {
        FileTransferDAO ftDao = FileTransferDAO.getFileTransferDAO(this, transferId);
        if (ftDao != null) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onHandleIntent file transfer resume with ID ".concat(transferId));
            }
            if (Direction.INCOMING == ftDao.getDirection()) {
                startActivity(ReceiveFileTransfer.forgeResumeIntent(this, ftDao, intent));
            } else {
                startActivity(InitiateFileTransfer.forgeResumeIntent(this, ftDao, intent));
            }
        }
    }

    private void handleFileTransferInvitation(Intent intent, String transferId) {
        FileTransferDAO ftDao = FileTransferDAO.getFileTransferDAO(this, transferId);
        if (ftDao != null) {
            if (FileTransfer.State.REJECTED == ftDao.getState()) {
                Log.e(LOGTAG, "File transfer already rejected. Id=".concat(transferId));
                return;
            }
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "File Transfer invitation filename=" + ftDao.getFilename() + " size="
                        + ftDao.getSize());
            }
            forwardFileTransferInvitationToUi(intent, ftDao);
        }
    }

    /**
     * Forward file transfer invitation to UI
     * 
     * @param invitation Intent invitation
     * @param ftDao the file transfer data object
     */
    private void forwardFileTransferInvitationToUi(Intent invitation, FileTransferDAO ftDao) {
        ContactId contact = ftDao.getContact();
        if (ftDao.getContact() == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "forwardFileTransferInvitationToUi failed: cannot parse contact");
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
        TalkList.notifyNewConversationEvent(this, FileTransferIntent.ACTION_NEW_INVITATION);
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
        forwardUndeliveredFileTransferToUi(intent, contact);
    }

    private void forwardUndeliveredFileTransferToUi(Intent undeliveredIntent, ContactId contact) {
        Intent intent = OneToOneTalkView.forgeIntentOnStackEvent(this, contact, undeliveredIntent);
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

    /**
     * Get set of undelivered file transfers
     *
     * @param ctx The context
     * @param contact The contact
     * @return set of undelivered file transfers
     */
    public static Set<String> getUndelivered(Context ctx, ContactId contact) {
        Set<String> ids = new HashSet<>();
        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(FileTransferLog.CONTENT_URI,
                    PROJ_UNDELIVERED_FT, SEL_UNDELIVERED_FTS, new String[] {
                        contact.toString()
                    }, null);
            if (cursor == null) {
                throw new SQLException("Cannot query undelivered file transfers for contact="
                        + contact);
            }
            if (!cursor.moveToFirst()) {
                return ids;
            }
            int idColumnIdx = cursor.getColumnIndexOrThrow(FileTransferLog.FT_ID);
            do {
                ids.add(cursor.getString(idColumnIdx));
            } while (cursor.moveToNext());
            return ids;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
