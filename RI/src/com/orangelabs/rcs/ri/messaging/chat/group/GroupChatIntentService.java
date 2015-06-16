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

package com.orangelabs.rcs.ri.messaging.chat.group;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.chat.ChatMessageDAO;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

/**
 * File transfer intent service
 * 
 * @author YPLO6403
 */
public class GroupChatIntentService extends IntentService {

    private static final int MAX_GC_HAVING_PENDING_MESSAGE = 5;

    /*
     * A cache of notification ID associated with each group chat having pending message. The key is
     * the chat ID and the value is the notification ID.
     */
    private static LruCache<String, Integer> sChatIdMessagePendingNotificationIdCache;

    private NotificationManager mNotifManager;

    private static final String LOGTAG = LogUtils.getTag(GroupChatIntentService.class
            .getSimpleName());

    /**
     * Creates an IntentService.
     */
    public GroupChatIntentService() {
        super("GroupChatIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (sChatIdMessagePendingNotificationIdCache == null) {
            sChatIdMessagePendingNotificationIdCache = new LruCache<String, Integer>(
                    MAX_GC_HAVING_PENDING_MESSAGE) {

                @Override
                protected void entryRemoved(boolean evicted, String key, Integer oldValue,
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
        if (GroupChatMessageReceiver.ACTION_NEW_GC_MSG.equals(action)) {
            // Gets message ID from the incoming Intent
            String messageId = intent.getStringExtra(GroupChatIntent.EXTRA_MESSAGE_ID);
            if (messageId == null) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Cannot read message ID");
                }
                return;
            }
            String mimeType = intent.getStringExtra(GroupChatIntent.EXTRA_MIME_TYPE);
            if (mimeType == null) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Cannot read message mime-type");
                }
                return;
            }
            handleNewGroupChatMessage(messageId);
        } else {
            if (GroupChatInvitationReceiver.ACTION_NEW_GC.equals(action)) {
                // Gets chat ID from the incoming Intent
                String chatId = intent.getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
                if (chatId != null) {
                    handleNewGroupChatInvitation(chatId);
                }
            } else {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Unknown action ".concat(action));
                }
            }
        }
    }

    /**
     * Handle Group chat invitation
     * 
     * @param invitation
     */
    private void handleNewGroupChatInvitation(String chatId) {
        // Get Chat from provider
        GroupChatDAO groupChatDAO = GroupChatDAO.getGroupChatDao(this, chatId);
        if (groupChatDAO == null) {
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Group chat invitation =".concat(groupChatDAO.toString()));
        }

        // Check if it's a spam
        if (groupChatDAO.getReasonCode() == GroupChat.ReasonCode.REJECTED_SPAM) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Do nothing on a spam");
            }
            return;
        }
        forwardGCInvitation2UI(chatId, groupChatDAO);
    }

    /**
     * Handle new group chat message
     * 
     * @param messageIntent intent with chat message
     */
    private void handleNewGroupChatMessage(String messageId) {
        // Get ChatMessage from provider
        ChatMessageDAO messageDAO = ChatMessageDAO.getChatMessageDAO(this, messageId);
        if (messageDAO == null) {
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Group chat message =".concat(messageDAO.toString()));
        }
        forwardGCMessage2UI(messageDAO);
    }

    /**
     * Forward Group Chat message to view activity
     * 
     * @param message message
     */
    private void forwardGCMessage2UI(ChatMessageDAO message) {
        Intent intent = GroupChatView.forgeIntentNewMessage(this, message);
        String chatId = message.getChatId();
        String content = message.getContent();

        /*
         * Do not display notification if activity is on foreground for this ChatID.
         */
        if (GroupChatView.isDisplayed() && chatId.equals(GroupChatView.chatIdOnForeground)) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        new StringBuilder("New message '").append(content).append("' for chatId ")
                                .append(chatId).toString());
            }
            Integer uniqueId = sChatIdMessagePendingNotificationIdCache.get(chatId);
            if (uniqueId != null) {
                sChatIdMessagePendingNotificationIdCache.remove(chatId);
                mNotifManager.cancel(uniqueId);
            }
            /* This will trigger onNewIntent for the target activity */
            startActivity(intent);
        } else {
            /*
             * If the PendingIntent has the same operation, action, data, categories, components,
             * and flags it will be replaced. Invitation should be notified individually so we use a
             * random generator to provide a unique request code and reuse it for the notification.
             */
            Integer uniqueId = sChatIdMessagePendingNotificationIdCache.get(chatId);
            if (uniqueId == null) {
                uniqueId = Utils.getUniqueIdForPendingIntent();
                sChatIdMessagePendingNotificationIdCache.put(chatId, uniqueId);
            }
            PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            /* Create notification */
            ContactId contact = message.getContact();
            String mimeType = message.getMimeType();
            String displayName = RcsDisplayName.getInstance(this).getDisplayName(contact);
            String title = getString(R.string.title_recv_chat, displayName);

            String msg;
            if (ChatLog.Message.MimeType.GEOLOC_MESSAGE.equals(mimeType)) {
                msg = getString(R.string.label_geoloc_msg);
            } else if (ChatLog.Message.MimeType.TEXT_MESSAGE.equals(mimeType)) {
                msg = content;
            } else {
                /* If the GC message does not convey user content then discards */
                if (LogUtils.isActive) {
                    Log.w(LOGTAG, new StringBuilder("Discard message of type '").append(mimeType)
                            .append("' for chatId ").append(chatId).toString());
                }
                return;
            }
            Notification notif = buildNotification(contentIntent, title, msg);

            /* Send notification */
            mNotifManager.notify(uniqueId, notif);
        }
    }

    /**
     * Forward Group chat invitation to View Activity
     * 
     * @param chatId the Chat ID
     * @param groupChat Group Chat DAO
     */
    public void forwardGCInvitation2UI(String chatId, GroupChatDAO groupChat) {
        /* Create pending intent */
        Intent intent = GroupChatView.forgeIntentInvitation(this, chatId, groupChat);
        /*
         * If the PendingIntent has the same operation, action, data, categories, components, and
         * flags it will be replaced. Invitation should be notified individually so we use a random
         * generator to provide a unique request code and reuse it for the notification.
         */
        Integer uniqueId = sChatIdMessagePendingNotificationIdCache.get(chatId);
        if (uniqueId == null) {
            uniqueId = Utils.getUniqueIdForPendingIntent();
            sChatIdMessagePendingNotificationIdCache.put(chatId, uniqueId);
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        /* Create notification */
        String title = getString(R.string.title_group_chat);
        /* Try to retrieve display name of remote contact */
        String displayName = getGcDisplayNameOfRemoteContact(groupChat.getChatId());
        if (displayName != null) {
            title = getString(R.string.title_recv_group_chat, displayName);
        }
        String subject = groupChat.getSubject();
        if (TextUtils.isEmpty(subject)) {
            subject = new StringBuilder("<").append(getString(R.string.label_no_subject))
                    .append(">").toString();
        }
        String msg = getString(R.string.label_subject_notif, subject);
        Notification notif = buildNotification(contentIntent, title, msg);

        /* Send notification */
        mNotifManager.notify(uniqueId, notif);
    }

    /**
     * Get the RCS display name of remote contact in Group Chat
     * 
     * @param chatId
     * @return the RCS display name or null
     */
    private String getGcDisplayNameOfRemoteContact(String chatId) {
        try {
            GroupChat gc = ConnectionManager.getInstance().getChatApi().getGroupChat(chatId);
            if (gc != null) {
                ContactId contact = gc.getRemoteContact();
                return RcsDisplayName.getInstance(this).getDisplayName(contact);
            }
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot get displayName", e);
            }
        }
        return null;
    }

    /**
     * Generate a notification
     * 
     * @param invitation
     * @param title
     * @param message
     * @return notification
     */
    private Notification buildNotification(PendingIntent invitation, String title, String message) {
        // Create notification
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
