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
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.SessionUnavailableException;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.DequeueTask;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.service.api.OneToOneChatImpl;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.database.Cursor;

/**
 * OneToOneChatMessageDequeueTask tries to dequeues and sends the queued one-one chat messages of a
 * specific contact.
 */
public class OneToOneChatMessageDequeueTask extends DequeueTask {

    private final ContactId mContact;

    public OneToOneChatMessageDequeueTask(Context ctx, Core core, ContactId contact,
            MessagingLog messagingLog, ChatServiceImpl chatService, RcsSettings rcsSettings,
            ContactManager contactManager, FileTransferServiceImpl fileTransferService) {
        super(ctx, core, contactManager, messagingLog, rcsSettings, chatService,
                fileTransferService);
        mContact = contact;
    }

    @Override
    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue one-to-one chat messages for contact "
                    .concat(mContact.toString()));
        }
        String id = null;
        String mimeType = null;
        Cursor cursor = null;
        try {
            if (!isImsConnected()) {
                if (logActivated) {
                    mLogger.debug("IMS not connected, exiting dequeue task to dequeue one-to-one chat messages for contact "
                            .concat(mContact.toString()));
                }
                return;
            }
            if (isShuttingDownOrStopped()) {
                if (logActivated) {
                    mLogger.debug("Core service is shutting down/stopped, exiting dequeue task to dequeue one-to-one chat messages for contact "
                            .concat(mContact.toString()));
                }
                return;
            }
            cursor = mMessagingLog.getQueuedOneToOneChatMessages(mContact);
            int msgIdIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MESSAGE_ID);
            int contentIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_CONTENT);
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(MessageData.KEY_MIME_TYPE);
            OneToOneChatImpl oneToOneChat = mChatService.getOrCreateOneToOneChat(mContact);
            while (cursor.moveToNext()) {
                try {
                    if (!isImsConnected()) {
                        if (logActivated) {
                            mLogger.debug("IMS not connected, exiting dequeue task to dequeue one-to-one chat messages for contact "
                                    .concat(mContact.toString()));
                        }
                        return;
                    }
                    if (isShuttingDownOrStopped()) {
                        if (logActivated) {
                            mLogger.debug("Core service is shutting down/stopped, exiting dequeue task to dequeue one-to-one chat messages for contact "
                                    .concat(mContact.toString()));
                        }
                        return;
                    }
                    id = cursor.getString(msgIdIdx);
                    mimeType = cursor.getString(mimeTypeIdx);
                    if (!isPossibleToDequeueOneToOneChatMessage(mContact)) {
                        setOneToOneChatMessageAsFailedDequeue(mContact, id, mimeType);
                        continue;
                    }
                    if (!isAllowedToDequeueOneToOneChatMessage(mContact)) {
                        continue;
                    }
                    String content = cursor.getString(contentIdx);
                    long timestamp = System.currentTimeMillis();

                    /* For outgoing message, timestampSent = timestamp */
                    ChatMessage msg = ChatUtils.createChatMessage(id, mimeType, content, mContact,
                            null, timestamp, timestamp);
                    oneToOneChat.dequeueOneToOneChatMessage(msg);

                } catch (SessionUnavailableException e) {
                    if (logActivated) {
                        mLogger.debug(new StringBuilder("Failed to dequeue one-one chat message '")
                                .append(id).append("' message for contact '").append(mContact)
                                .append("' due to: ").append(e.getMessage()).toString());
                    }
                } catch (NetworkException e) {
                    if (logActivated) {
                        mLogger.debug(new StringBuilder("Failed to dequeue one-one chat message '")
                                .append(id).append("' message for contact '").append(mContact)
                                .append("' due to: ").append(e.getMessage()).toString());
                    }
                } catch (PayloadException e) {
                    mLogger.error(new StringBuilder("Failed to dequeue one-one chat message '")
                            .append(id).append("' message for contact '").append(mContact)
                            .toString(), e);
                    setOneToOneChatMessageAsFailedDequeue(mContact, id, mimeType);
                } catch (RuntimeException e) {
                    /*
                     * Normally all the terminal and non-terminal cases should be handled above so
                     * if we come here that means that there is a bug and so we output a stack trace
                     * so the bug can then be properly tracked down and fixed. We also mark the
                     * respective entry that failed to dequeue as FAILED.
                     */
                    mLogger.error(new StringBuilder("Failed to dequeue one-one chat message '")
                            .append(id).append("'for contact '").append(mContact).append("' ")
                            .toString(), e);
                    setOneToOneChatMessageAsFailedDequeue(mContact, id, mimeType);
                }
            }

        } catch (RuntimeException e) {
            /*
             * Normally all the terminal and non-terminal cases should be handled above so if we
             * come here that means that there is a bug and so we output a stack trace so the bug
             * can then be properly tracked down and fixed. We also mark the respective entry that
             * failed to dequeue as FAILED.
             */
            mLogger.error(new StringBuilder(
                    "Exception occured while dequeueing one-to-one chat message with msgId '")
                    .append(id).append("'for contact '").append(mContact).append("' ").toString(),
                    e);
            if (id == null) {
                return;
            }
            setOneToOneChatMessageAsFailedDequeue(mContact, id, mimeType);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
