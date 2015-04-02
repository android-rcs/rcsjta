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

package com.gsma.rcs.provider.history;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.DequeueTask;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.service.api.GroupChatImpl;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.database.Cursor;
import android.net.Uri;

public class GroupChatDequeueTask extends DequeueTask {

    private final String mChatId;

    private final ChatServiceImpl mChatService;

    private final FileTransferServiceImpl mFileTransferService;

    private final HistoryLog mHistoryLog;

    public GroupChatDequeueTask(Object lock, String chatId, InstantMessagingService imService,
            MessagingLog messagingLog, ChatServiceImpl chatService,
            FileTransferServiceImpl fileTransferService, RcsSettings rcsSettings,
            HistoryLog historyLog, ContactsManager contactManager) {
        super(lock, imService, contactManager, messagingLog, rcsSettings);
        mChatId = chatId;
        mChatService = chatService;
        mFileTransferService = fileTransferService;
        mHistoryLog = historyLog;
    }

    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue group chat messages and group file transfers for chatId "
                    .concat(mChatId));
        }
        Cursor cursor = null;
        try {
            synchronized (mLock) {
                cursor = mHistoryLog.getQueuedGroupChatMessagesAndGroupFileTransfers(mChatId);
                int providerIdIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_PROVIDER_ID);
                int idIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_ID);
                int contentIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_CONTENT);
                int mimeTypeIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_MIME_TYPE);
                int fileIconIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_FILEICON);
                GroupChatImpl groupChat = mChatService.getOrCreateGroupChat(mChatId);
                while (cursor.moveToNext()) {
                    int providerId = cursor.getInt(providerIdIdx);
                    String id = cursor.getString(idIdx);
                    try {
                        switch (providerId) {
                            case Message.HISTORYLOG_MEMBER_ID:
                                String content = cursor.getString(contentIdx);
                                String mimeType = cursor.getString(mimeTypeIdx);
                                long timestamp = System.currentTimeMillis();
                                /* For outgoing message, timestampSent = timestamp */
                                ChatMessage message = ChatUtils.createChatMessage(id, mimeType,
                                        content, null, null, timestamp, timestamp);
                                groupChat.dequeueChatMessage(message);
                                break;
                            case FileTransferData.HISTORYLOG_MEMBER_ID:
                                if (isAllowedToDequeueGroupFileTransfer(mChatId)) {
                                    Uri file = Uri.parse(cursor.getString(contentIdx));
                                    MmContent fileContent = FileTransferUtils.createMmContent(file);
                                    MmContent fileIconContent = null;
                                    String fileIcon = cursor.getString(fileIconIdx);
                                    if (fileIcon != null) {
                                        Uri fileIconUri = Uri.parse(fileIcon);
                                        fileIconContent = FileTransferUtils
                                                .createMmContent(fileIconUri);
                                    }
                                    mFileTransferService.dequeueGroupFileTransfer(id, fileContent,
                                            fileIconContent, mChatId);
                                }
                                break;
                            default:
                                break;
                        }
                    } catch (SecurityException e) {
                        if (logActivated) {
                            mLogger.error(new StringBuilder(
                                    "Security exception occured while dequeueing file transfer with transferId '")
                                    .append(id).append("', so mark as failed").toString());
                        }
                        mFileTransferService.setGroupFileTransferStateAndReasonCode(id, mChatId,
                                State.FAILED, ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
                    } catch (Exception e) { /*
                                             * Exceptions will be handled better in CR037 Break only
                                             * for terminal exception, in rest of the cases dequeue
                                             * and try to send other messages.
                                             */
                        if (logActivated) {
                            mLogger.error(
                                    new StringBuilder(
                                            "Exception occured while dequeueing group chat message and group file transfer with chatId '")
                                            .append(mChatId).append("'").toString(), e);
                        }
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
