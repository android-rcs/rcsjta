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
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.DequeueTask;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.service.api.OneToOneChatImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.database.Cursor;
import android.net.Uri;

public class OneToOneChatDequeueTask extends DequeueTask {

    private final ChatServiceImpl mChatService;

    private final FileTransferServiceImpl mFileTransferService;

    private final HistoryLog mHistoryLog;

    public OneToOneChatDequeueTask(Object lock, InstantMessagingService imService,
            ChatServiceImpl chatService, FileTransferServiceImpl fileTransferService,
            HistoryLog historyLog, MessagingLog messagingLog, ContactManager contactManager,
            RcsSettings rcsSettings) {
        super(lock, imService, contactManager, messagingLog, rcsSettings);
        mChatService = chatService;
        mFileTransferService = fileTransferService;
        mHistoryLog = historyLog;
    }

    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue all one-to-one chat messages and one-to-one file transfers");
        }
        Cursor cursor = null;
        try {
            synchronized (mLock) {
                cursor = mHistoryLog.getQueuedOneToOneChatMessagesAndOneToOneFileTransfers();
                int providerIdIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_PROVIDER_ID);
                int idIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_ID);
                int contactIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_CONTACT);
                int contentIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_CONTENT);
                int mimeTypeIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_MIME_TYPE);
                int fileIconIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_FILEICON);
                while (cursor.moveToNext()) {
                    int providerId = cursor.getInt(providerIdIdx);
                    String id = cursor.getString(idIdx);
                    String phoneNumber = cursor.getString(contactIdx);
                    ContactId contact = ContactUtil.createContactIdFromTrustedData(phoneNumber);
                    try {
                        switch (providerId) {
                            case Message.HISTORYLOG_MEMBER_ID:
                                String msgId = cursor.getString(idIdx);
                                String content = cursor.getString(contentIdx);
                                String mimeType = cursor.getString(mimeTypeIdx);
                                long timestamp = System.currentTimeMillis();
                                /* For outgoing message, timestampSent = timestamp */
                                ChatMessage message = ChatUtils.createChatMessage(msgId, mimeType,
                                        content, contact, null, timestamp, timestamp);
                                OneToOneChatImpl oneToOneChat = mChatService
                                        .getOrCreateOneToOneChat(contact);
                                if (isAllowedToDequeueOneToOneChatMessage(contact)) {
                                    OneToOneChatSession session = mImService
                                            .getOneToOneChatSession(contact);
                                    if (session == null) {
                                        if (mImService.isChatSessionAvailable()) {
                                            oneToOneChat.dequeueChatMessageInNewSession(message);
                                        }
                                    } else if (!session.isMediaEstablished()
                                            && session.isInitiatedByRemote()) {
                                        session.acceptSession();
                                    } else if (session.isMediaEstablished()) {
                                        oneToOneChat.dequeueChatMessageWithinSession(message,
                                                session);
                                    }
                                }
                                break;
                            case FileTransferData.HISTORYLOG_MEMBER_ID:
                                Uri file = Uri.parse(cursor.getString(contentIdx));
                                MmContent fileContent = FileTransferUtils.createMmContent(file);
                                MmContent fileIconContent = null;
                                String fileIcon = cursor.getString(fileIconIdx);
                                if (fileIcon != null) {
                                    Uri fileIconUri = Uri.parse(fileIcon);
                                    fileIconContent = FileTransferUtils
                                            .createMmContent(fileIconUri);
                                }
                                if (isAllowedToDequeueOneToOneFileTransfer()) {
                                    mFileTransferService.dequeueOneToOneFileTransfer(id, contact,
                                            fileContent, fileIconContent);
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
                        mFileTransferService.setOneToOneFileTransferStateAndReasonCode(id, contact,
                                State.FAILED, ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
                    } catch (Exception e) {
                        /*
                         * Exceptions will be handled better in CR037 Break only for terminal
                         * exception, in rest of the cases dequeue and try to send other messages.
                         */
                        if (logActivated) {
                            mLogger.error(
                                    "Exception occured while dequeueing one-to-one chat message and one-to-one file transfer ",
                                    e);
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
