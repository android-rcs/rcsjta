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

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpException;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.DequeueTask;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.OneToOneChatImpl;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;

/**
 * OneToOneChatMessageDequeueTask tries to dequeues and sends the queued one-one chat messages of a
 * specific contact.
 */
public class OneToOneChatMessageDequeueTask extends DequeueTask {

    private final ContactId mContact;

    private final ChatServiceImpl mChatService;

    public OneToOneChatMessageDequeueTask(Object lock, Core core, ContactId contact,
            MessagingLog messagingLog, ChatServiceImpl chatService, RcsSettings rcsSettings,
            ContactManager contactManager) {
        super(lock, core, contactManager, messagingLog, rcsSettings);
        mContact = contact;
        mChatService = chatService;
    }

    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue one-to-one chat messages for contact "
                    .concat(mContact.toString()));
        }
        Cursor cursor = null;
        try {
            synchronized (mLock) {
                if (mCore.isStopping()) {
                    if (logActivated) {
                        mLogger.debug("Core service is stopped, exiting dequeue task to dequeue one-to-one chat messages for contact "
                                .concat(mContact.toString()));
                    }
                    return;
                }
                cursor = mMessagingLog.getQueuedOneToOneChatMessages(mContact);
                /* TODO: Handle cursor when null. */
                int msgIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID);
                int contentIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_CONTENT);
                int mimeTypeIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MIME_TYPE);
                OneToOneChatImpl oneToOneChat = mChatService.getOrCreateOneToOneChat(mContact);
                while (cursor.moveToNext()) {
                    if (mCore.isStopping()) {
                        if (logActivated) {
                            mLogger.debug("Core service is stopped, exiting dequeue task to dequeue one-to-one chat messages for contact "
                                    .concat(mContact.toString()));
                        }
                        return;
                    }
                    if (!isAllowedToDequeueOneToOneChatMessage(mContact)) {
                        continue;
                    }
                    String msgId = cursor.getString(msgIdIdx);
                    String content = cursor.getString(contentIdx);
                    String mimeType = cursor.getString(mimeTypeIdx);
                    long timestamp = System.currentTimeMillis();
                    /* For outgoing message, timestampSent = timestamp */
                    ChatMessage message = ChatUtils.createChatMessage(msgId, mimeType, content,
                            mContact, null, timestamp, timestamp);
                    try {
                        try {
                            oneToOneChat.dequeueOneToOneChatMessage(message);
                        } catch (MsrpException e) {
                            if (logActivated) {
                                mLogger.debug(new StringBuilder(
                                        "Failed to dequeue one-one chat message '").append(msgId)
                                        .append("' message for contact '").append(mContact)
                                        .append("' due to: ").append(e.getMessage()).toString());
                            }
                        }
                    } catch (RuntimeException e) {
                        /*
                         * Break only for terminal exception, in rest of the cases dequeue and try
                         * to send other messages.
                         */
                        mLogger.error(
                                new StringBuilder(
                                        "Exception occured while dequeueing one-to-one chat message with msgId '")
                                        .append(msgId).append("'for contact '").append(mContact)
                                        .append("' ").toString(), e);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
