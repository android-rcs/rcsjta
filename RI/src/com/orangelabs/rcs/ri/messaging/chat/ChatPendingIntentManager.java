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

package com.orangelabs.rcs.ri.messaging.chat;

import com.orangelabs.rcs.ri.utils.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.LruCache;

/**
 * There should only have 1 pending intent for a given chat conversation.<br>
 * If the conversation is already on the foreground, the pending intent is useless and the activity
 * is (re)started.
 * 
 * @author Philippe LEMORDANT
 */
public class ChatPendingIntentManager {

    private static volatile ChatPendingIntentManager sChatPendingIntentManager;

    private final IForegroundInfo mForegroundInfo;

    private static final int MAX_CHAT_HAVING_PENDING_MESSAGE = 10;

    /*
     * A cache of notification ID associated with each chat conversation having pending message. The
     * key is the chat ID and the value is the notification ID.
     */
    private final LruCache<String, Integer> mPendingNotificationIdCache;

    private final NotificationManager mNotifManager;

    private final Context mCtx;

    public interface IForegroundInfo {
        boolean isConversationOnForeground(String chatId);
    }

    private ChatPendingIntentManager(Context ctx, IForegroundInfo foregroundInfo) {
        mCtx = ctx;
        mForegroundInfo = foregroundInfo;
        mNotifManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        mPendingNotificationIdCache = new LruCache<String, Integer>(MAX_CHAT_HAVING_PENDING_MESSAGE) {

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

    /**
     * Try to continue chat conversation
     * 
     * @param continueChat the intent to continue the chat conversation
     * @param chatId the chat ID
     * @return the unique ID to be used to create the pending intent if chat conversation is not on
     *         foreground or null if continuing the conversation succeeded
     */
    public Integer tryContinueChatConversation(Intent continueChat, String chatId) {
        if (mForegroundInfo.isConversationOnForeground(chatId)) {
            /*
             * Do not display notification if activity is on foreground for this chatId
             */
            Integer pendingIntentId = mPendingNotificationIdCache.get(chatId);
            if (pendingIntentId != null) {
                mPendingNotificationIdCache.remove(chatId);
                mNotifManager.cancel(pendingIntentId);
            }
            /* This will trigger onNewIntent for the target activity */
            mCtx.startActivity(continueChat);
            return null;
        }
        Integer uniquePendingIntentId = mPendingNotificationIdCache.get(chatId);
        if (uniquePendingIntentId == null) {
            /*
             * If the PendingIntent has the same operation, action, data, categories, components,
             * and flags it will be replaced. Invitation should be notified individually so we use a
             * random generator to provide a unique request code and reuse it for the notification.
             */
            uniquePendingIntentId = Utils.getUniqueIdForPendingIntent();
            mPendingNotificationIdCache.put(chatId, uniquePendingIntentId);
        }
        return uniquePendingIntentId;
    }

    public void postNotification(Integer id, Notification notification) {
        mNotifManager.notify(id, notification);
    }

    /**
     * Gets Chat pending intent manager
     * 
     * @param ctx the context
     * @return the instance
     */
    public static ChatPendingIntentManager getChatPendingIntentManager(Context ctx) {
        /* "Double-Checked Locking" idiom for singleton creation */
        if (sChatPendingIntentManager != null) {
            return sChatPendingIntentManager;
        }
        synchronized (ChatPendingIntentManager.class) {
            if (sChatPendingIntentManager == null) {
                sChatPendingIntentManager = new ChatPendingIntentManager(
                        ctx.getApplicationContext(),
                        new ChatPendingIntentManager.IForegroundInfo() {

                            @Override
                            public boolean isConversationOnForeground(String chatId) {
                                return ChatView.sChatIdOnForeground != null
                                        && chatId.equals(ChatView.sChatIdOnForeground.toString());
                            }
                        });
            }
            return sChatPendingIntentManager;
        }
    }

}
