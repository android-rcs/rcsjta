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

package com.orangelabs.rcs.ri.messaging.chat.single;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.OneToOneChatIntent;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.chat.ChatMessageDAO;
import com.orangelabs.rcs.ri.messaging.chat.ChatPendingIntentManager;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Single chat intent service
 * 
 * @author Philippe LEMORDANT
 */
public class SingleChatIntentService extends IntentService {

    private ChatPendingIntentManager mChatPendingIntentManager;

    private static final String LOGTAG = LogUtils.getTag(SingleChatIntentService.class
            .getSimpleName());

    /**
     * Creates an IntentService.
     */
    public SingleChatIntentService() {
        super("SingleChatIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mChatPendingIntentManager = ChatPendingIntentManager.getChatPendingIntentManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        /* We want this service to stop running if forced stop so return not sticky. */
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action;
        if ((action = intent.getAction()) == null) {
            return;
        }
        String msgId = intent.getStringExtra(OneToOneChatIntent.EXTRA_MESSAGE_ID);
        if (msgId == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read message ID");
            }
            return;
        }
        switch (action) {
            case OneToOneChatIntent.ACTION_NEW_ONE_TO_ONE_CHAT_MESSAGE:
                handleNewOneToOneChatMessage(intent, msgId);
                break;

            case OneToOneChatIntent.ACTION_MESSAGE_DELIVERY_EXPIRED:
                handleUndeliveredMessage(intent, msgId);
                break;

            default:
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Unknown action ".concat(action));
                }
                break;
        }
    }

    private void handleUndeliveredMessage(Intent intent, String msgId) {
        ContactId contact = intent.getParcelableExtra(OneToOneChatIntent.EXTRA_CONTACT);
        if (contact == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read contact for message ID=".concat(msgId));
            }
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Undelivered message ID=" + msgId + " for contact " + contact);
        }
        forwardUndeliveredMessage2UI(intent, contact);
    }

    /**
     * Handle new one to one chat message
     * 
     * @param messageIntent intent with chat message
     */
    private void handleNewOneToOneChatMessage(Intent messageIntent, String msgId) {
        String mimeType = messageIntent.getStringExtra(OneToOneChatIntent.EXTRA_MIME_TYPE);
        if (mimeType == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read message mime-type");
            }
            return;
        }
        /* Read message from provider */
        ChatMessageDAO msgDAO = ChatMessageDAO.getChatMessageDAO(this, msgId);
        if (msgDAO == null) {
            Log.e(LOGTAG, "Cannot find group chat message with ID=".concat(msgId));
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "One to one chat message ".concat(msgDAO.toString()));
        }
        forwardSingleChatMessage2UI(messageIntent, msgDAO);
    }

    /**
     * Forward one to one chat message to view activity
     * 
     * @param messageIntent intent
     * @param message the chat message DAO
     */
    private void forwardSingleChatMessage2UI(Intent messageIntent, ChatMessageDAO message) {
        ContactId contact = message.getContact();
        String content = message.getContent();
        Intent intent = SingleChatView.forgeIntentOnStackEvent(this, contact, messageIntent);
        Integer uniqueId = mChatPendingIntentManager.tryContinueChatConversation(intent,
                message.getChatId());
        if (uniqueId != null) {
            PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            String displayName = RcsContactUtil.getInstance(this).getDisplayName(contact);
            String title = getString(R.string.title_recv_chat, displayName);
            String mimeType = message.getMimeType();
            String msg;
            switch (mimeType) {
                case ChatLog.Message.MimeType.GEOLOC_MESSAGE:
                    msg = getString(R.string.label_geoloc_msg);
                    break;

                case ChatLog.Message.MimeType.TEXT_MESSAGE:
                    msg = content;
                    break;

                default:
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, "Discard message type '".concat(mimeType));
                    }
                    return;
            }
            Notification notif = buildNotification(contentIntent, title, msg);
            mChatPendingIntentManager.postNotification(uniqueId, notif);
        }
    }

    private void forwardUndeliveredMessage2UI(Intent undeliveredMessageIntent, ContactId contact) {
        Intent intent = SingleChatView.forgeIntentOnStackEvent(this, contact,
                undeliveredMessageIntent);
        Integer uniqueId = mChatPendingIntentManager.tryContinueChatConversation(intent,
                contact.toString());
        if (uniqueId != null) {
            PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            String displayName = RcsContactUtil.getInstance(this).getDisplayName(contact);
            String title = getString(R.string.title_undelivered_message);
            String msg = getString(R.string.label_undelivered_message, displayName);
            Notification notif = buildNotification(contentIntent, title, msg);
            mChatPendingIntentManager.postNotification(uniqueId, notif);
        }
    }

    /**
     * Generate a notification
     * 
     * @param invitation invitation
     * @param title title
     * @param message message
     * @return the notification
     */
    private Notification buildNotification(PendingIntent invitation, String title, String message) {
        NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
        notif.setContentIntent(invitation);
        notif.setSmallIcon(R.drawable.ri_notif_chat_icon);
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
