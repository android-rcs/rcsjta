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
import android.text.TextUtils;
import android.util.Log;

/**
 * File transfer intent service
 * 
 * @author Philippe LEMORDANT
 */
public class GroupChatIntentService extends IntentService {

    private ChatPendingIntentManager mChatPendingIntentManager;

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
        mChatPendingIntentManager = ChatPendingIntentManager.getChatPendingIntentManager(this);
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
        if (GroupChatIntent.ACTION_NEW_GROUP_CHAT_MESSAGE.equals(action)) {
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
            handleNewGroupChatMessage(intent, messageId);

        } else if (GroupChatIntent.ACTION_NEW_INVITATION.equals(action)) {
            /* Gets chat ID from the incoming Intent */
            String chatId = intent.getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
            if (chatId != null) {
                handleNewGroupChatInvitation(intent, chatId);
            }

        } else {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Unknown action ".concat(action));
            }
        }
    }

    private void handleNewGroupChatInvitation(Intent invitation, String chatId) {
        /* Get Chat from provider */
        GroupChatDAO groupChatDAO = GroupChatDAO.getGroupChatDao(this, chatId);
        if (groupChatDAO == null) {
            Log.e(LOGTAG, "Cannot find group chat with ID=".concat(chatId));
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Group chat invitation =".concat(groupChatDAO.toString()));
        }
        /* Check if it's a spam */
        if (groupChatDAO.getReasonCode() == GroupChat.ReasonCode.REJECTED_SPAM) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Do nothing on a spam");
            }
            return;
        }
        forwardGCInvitation2UI(invitation, chatId, groupChatDAO);
    }

    private void handleNewGroupChatMessage(Intent newGroupChatMessage, String messageId) {
        /* Get ChatMessage from provider */
        ChatMessageDAO messageDAO = ChatMessageDAO.getChatMessageDAO(this, messageId);
        if (messageDAO == null) {
            Log.e(LOGTAG, "Cannot find group chat message with ID=".concat(messageId));
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Group chat message =".concat(messageDAO.toString()));
        }
        forwardGCMessage2UI(newGroupChatMessage, messageDAO);
    }

    private void forwardGCMessage2UI(Intent newGroupChatMessage, ChatMessageDAO message) {
        String chatId = message.getChatId();
        Intent intent = GroupChatView.forgeIntentNewMessage(this, newGroupChatMessage, chatId);
        String content = message.getContent();
        Integer uniqueId = mChatPendingIntentManager.tryContinueChatConversation(intent, chatId);
        if (uniqueId != null) {
            PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            /* Create notification */
            ContactId contact = message.getContact();
            String mimeType = message.getMimeType();
            String displayName = RcsContactUtil.getInstance(this).getDisplayName(contact);
            String title = getString(R.string.title_recv_chat, displayName);

            String msg;
            if (ChatLog.Message.MimeType.GEOLOC_MESSAGE.equals(mimeType)) {
                msg = getString(R.string.label_geoloc_msg);

            } else if (ChatLog.Message.MimeType.TEXT_MESSAGE.equals(mimeType)) {
                msg = content;

            } else {
                /* If the GC message does not convey user content then discards */
                if (LogUtils.isActive) {
                    Log.w(LOGTAG, "Discard message of type '" + mimeType + "' for chatId " + chatId);
                }
                return;
            }
            Notification notif = buildNotification(contentIntent, title, msg);
            /* Send notification */
            mChatPendingIntentManager.postNotification(uniqueId, notif);
        }
    }

    private void forwardGCInvitation2UI(Intent invitation, String chatId, GroupChatDAO groupChat) {
        /* Create pending intent */
        Intent intent = GroupChatView.forgeIntentInvitation(this, invitation);
        /*
         * If the PendingIntent has the same operation, action, data, categories, components, and
         * flags it will be replaced. Invitation should be notified individually so we use a random
         * generator to provide a unique request code and reuse it for the notification.
         */
        Integer uniqueId = mChatPendingIntentManager.tryContinueChatConversation(intent, chatId);
        if (uniqueId != null) {
            PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            /* Create notification */
            String title = getString(R.string.title_group_chat);
            /* Try to retrieve display name of remote contact */
            String displayName = RcsContactUtil.getInstance(this).getDisplayName(
                    groupChat.getContact());
            if (displayName != null) {
                title = getString(R.string.title_recv_group_chat, displayName);
            }
            String subject = groupChat.getSubject();
            if (TextUtils.isEmpty(subject)) {
                subject = "<" + getString(R.string.label_no_subject) + ">";
            }
            String msg = getString(R.string.label_subject_notif, subject);
            Notification notif = buildNotification(contentIntent, title, msg);
            /* Send notification */
            mChatPendingIntentManager.postNotification(uniqueId, notif);
        } else {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "Received invitation for an existing group chat conversation chatId="
                        + chatId + "!");
            }
        }
    }

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
