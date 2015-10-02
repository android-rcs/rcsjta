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
import android.util.LruCache;

/**
 * Single chat intent service
 * 
 * @author YPLO6403
 */
public class SingleChatIntentService extends IntentService {

    private static final int MAX_1TO1_CHAT_HAVING_PENDING_MESSAGE = 5;

    /*
     * A cache of notification ID associated with each contact having pending message. The key is
     * the contact ID and the value is the notification ID.
     */
    private static LruCache<ContactId, Integer> sContactMessagePendingNotificationIdCache;

    private NotificationManager mNotifManager;

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
        mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (sContactMessagePendingNotificationIdCache == null) {
            sContactMessagePendingNotificationIdCache = new LruCache<ContactId, Integer>(
                    MAX_1TO1_CHAT_HAVING_PENDING_MESSAGE) {

                @Override
                protected void entryRemoved(boolean evicted, ContactId key, Integer oldValue,
                        Integer newValue) {
                    super.entryRemoved(evicted, key, oldValue, newValue);
                    if (evicted) {
                        mNotifManager.cancel(oldValue);
                    }
                }
            };
        }
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
        if (OneToOneChatIntent.ACTION_NEW_ONE_TO_ONE_CHAT_MESSAGE.equals(action)) {
            handleNewOneToOneChatMessage(intent);

        } else if (OneToOneChatIntent.ACTION_MESSAGE_DELIVERY_EXPIRED.equals(action)) {
            handleUndeliveredMessage(intent);

        } else {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Unknown action ".concat(action));
            }
        }
    }

    private void handleUndeliveredMessage(Intent intent) {
        /* Gets data from the incoming Intent */
        String msgId = intent.getStringExtra(OneToOneChatIntent.EXTRA_MESSAGE_ID);
        if (msgId == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read message ID");
            }
            return;
        }
        ContactId contact = intent.getParcelableExtra(OneToOneChatIntent.EXTRA_CONTACT);
        if (contact == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read contact");
            }
            return;
        }
        if (LogUtils.isActive) {
            Log.e(LOGTAG, "Undelivered message ID=" + msgId + " for contact " + contact);
        }
        // TODO implement CR019 undelivered messages
        Utils.displayLongToast(this, getString(R.string.label_todo));
    }

    /**
     * Handle new one to one chat message
     * 
     * @param messageIntent intent with chat message
     */
    private void handleNewOneToOneChatMessage(Intent messageIntent) {
        /* Gets data from the incoming Intent */
        String msgId = messageIntent.getStringExtra(OneToOneChatIntent.EXTRA_MESSAGE_ID);
        if (msgId == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read message ID");
            }
            return;
        }
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
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "One to one chat message ".concat(msgDAO.toString()));
        }
        forwardSingleChatMessage2UI(this, msgDAO);
    }

    /**
     * Forward one to one chat message to view activity
     * 
     * @param context Context
     * @param message the chat message DAO
     */
    private void forwardSingleChatMessage2UI(Context context, ChatMessageDAO message) {
        ContactId contact = message.getContact();
        if (contact == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Contact is null");
            }
            return;
        }
        String content = message.getContent();
        Intent intent = SingleChatView.forgeIntentToStart(context, contact);
        /*
         * Do not display notification if activity is on foreground for this contact
         */
        if (SingleChatView.isDisplayed() && contact.equals(SingleChatView.contactOnForeground)) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        new StringBuilder("New message '").append(content).append("' for contact ")
                                .append(contact.toString()).toString());
            }
            Integer pendingIntentId = sContactMessagePendingNotificationIdCache.get(contact);
            if (pendingIntentId != null) {
                sContactMessagePendingNotificationIdCache.remove(contact);
                mNotifManager.cancel(pendingIntentId);
            }
            /* This will trigger onNewIntent for the target activity */
            startActivity(intent);
        } else {
            /*
             * If the PendingIntent has the same operation, action, data, categories, components,
             * and flags it will be replaced. Invitation should be notified individually so we use a
             * random generator to provide a unique request code and reuse it for the notification.
             */
            Integer uniqueId = sContactMessagePendingNotificationIdCache.get(contact);
            if (uniqueId == null) {
                uniqueId = Utils.getUniqueIdForPendingIntent();
                sContactMessagePendingNotificationIdCache.put(contact, uniqueId);
            }
            PendingIntent contentIntent = PendingIntent.getActivity(context, uniqueId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            String displayName = RcsContactUtil.getInstance(this).getDisplayName(contact);
            String title = getString(R.string.title_recv_chat, displayName);
            String mimeType = message.getMimeType();
            String msg;
            if (ChatLog.Message.MimeType.GEOLOC_MESSAGE.equals(mimeType)) {
                msg = getString(R.string.label_geoloc_msg);
            } else if (ChatLog.Message.MimeType.TEXT_MESSAGE.equals(mimeType)) {
                msg = content;
            } else {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Discard message type '".concat(mimeType));
                }
                return;
            }
            Notification notif = buildNotification(contentIntent, title, msg);
            mNotifManager.notify(uniqueId, notif);
        }
    }

    /**
     * Generate a notification
     * 
     * @param invitation
     * @param title
     * @param message
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
