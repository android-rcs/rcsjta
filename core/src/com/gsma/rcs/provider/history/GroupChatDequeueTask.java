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

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpException;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessageData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.DequeueTask;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.service.api.GroupChatImpl;
import com.gsma.rcs.service.api.GroupFileTransferImpl;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * GroupChatDequeueTask tries to dequeue all group chat messages that are QUEUED and all file
 * transfers that are either QUEUED or UPLOADED but not trasnferred for a specific group chat.
 */
public class GroupChatDequeueTask extends DequeueTask {

    private final String mChatId;

    private final HistoryLog mHistoryLog;

    private final boolean mDisplayedReportEnabled;

    private final boolean mDeliveryReportEnabled;

    public GroupChatDequeueTask(Object lock, Context ctx, Core core, String chatId,
            MessagingLog messagingLog, ChatServiceImpl chatService,
            FileTransferServiceImpl fileTransferService, RcsSettings rcsSettings,
            HistoryLog historyLog, ContactManager contactManager) {
        super(lock, ctx, core, contactManager, messagingLog, rcsSettings, chatService,
                fileTransferService);
        mChatId = chatId;
        mHistoryLog = historyLog;
        final ImdnManager imdnManager = mImService.getImdnManager();
        mDisplayedReportEnabled = imdnManager.isRequestGroupDeliveryDisplayedReportsEnabled();
        mDeliveryReportEnabled = imdnManager.isDeliveryDeliveredReportsEnabled();
    }

    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue group chat messages and group file transfers for chatId "
                    .concat(mChatId));
        }
        int providerId = -1;
        String id = null;
        Cursor cursor = null;
        try {
            synchronized (mLock) {
                if (mCore.isStopping()) {
                    if (logActivated) {
                        mLogger.debug("Core service is stopped, exiting dequeue task to dequeue group chat messages and group file transfers for chatId "
                                .concat(mChatId));
                    }
                    return;
                }
                cursor = mHistoryLog.getQueuedGroupChatMessagesAndGroupFileTransfers(mChatId);
                /* TODO: Handle cursor when null. */
                int providerIdIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_PROVIDER_ID);
                int idIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_ID);
                int contentIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_CONTENT);
                int mimeTypeIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_MIME_TYPE);
                int fileIconIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_FILEICON);
                int statusIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_STATUS);
                int fileSizeIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_FILESIZE);
                GroupChatImpl groupChat = mChatService.getOrCreateGroupChat(mChatId);
                while (cursor.moveToNext()) {
                    if (mCore.isStopping()) {
                        if (logActivated) {
                            mLogger.debug("Core service is stopped, exiting dequeue task to dequeue group chat messages and group file transfers for chatId "
                                    .concat(mChatId));
                        }
                        return;
                    }
                    providerId = cursor.getInt(providerIdIdx);
                    id = cursor.getString(idIdx);
                    switch (providerId) {
                        case MessageData.HISTORYLOG_MEMBER_ID:
                            if (!isPossibleToDequeueGroupChatMessage(mChatId)) {
                                setGroupChatMessageAsFailed(mChatId, id);
                                continue;
                            }
                            try {
                                String content = cursor.getString(contentIdx);
                                String mimeType = cursor.getString(mimeTypeIdx);
                                long timestamp = System.currentTimeMillis();
                                /* For outgoing message, timestampSent = timestamp */
                                ChatMessage message = ChatUtils.createChatMessage(id, mimeType,
                                        content, null, null, timestamp, timestamp);
                                groupChat.dequeueGroupChatMessage(message);

                            } catch (MsrpException e) {
                                if (logActivated) {
                                    mLogger.debug(new StringBuilder(
                                            "Failed to dequeue group chat message '").append(id)
                                            .append("' message on group chat '").append(mChatId)
                                            .append("' due to: ").append(e.getMessage()).toString());
                                }
                            }
                            break;
                        case FileTransferData.HISTORYLOG_MEMBER_ID:
                            Uri file = Uri.parse(cursor.getString(contentIdx));
                            if (!isPossibleToDequeueGroupFileTransfer(mChatId, file,
                                    cursor.getLong(fileSizeIdx))) {
                                setGroupFileTransferAsFailed(mChatId, id);
                                continue;
                            }
                            State state = State.valueOf(cursor.getInt(statusIdx));
                            switch (state) {
                                case QUEUED:
                                    if (!isAllowedToDequeueGroupFileTransfer()) {
                                        continue;
                                    }
                                    try {
                                        MmContent fileContent = FileTransferUtils
                                                .createMmContent(file);
                                        MmContent fileIconContent = null;
                                        String fileIcon = cursor.getString(fileIconIdx);
                                        if (fileIcon != null) {
                                            Uri fileIconUri = Uri.parse(fileIcon);
                                            fileIconContent = FileTransferUtils
                                                    .createMmContent(fileIconUri);
                                        }
                                        mFileTransferService.dequeueGroupFileTransfer(mChatId, id,
                                                fileContent, fileIconContent);
                                    } catch (MsrpException e) {
                                        if (logActivated) {
                                            mLogger.debug(new StringBuilder(
                                                    "Failed to dequeue group file transfer with fileTransferId '")
                                                    .append(id).append("' on group chat '")
                                                    .append(mChatId).append("' due to: ")
                                                    .append(e.getMessage()).toString());
                                        }
                                    }
                                    break;
                                case STARTED:
                                    if (!isPossibleToDequeueGroupChatMessage(mChatId)) {
                                        setGroupFileTransferAsFailed(mChatId, id);
                                        continue;
                                    }
                                    try {
                                        GroupFileTransferImpl groupFileTransfer = mFileTransferService
                                                .getOrCreateGroupFileTransfer(mChatId, id);
                                        String fileInfo = FileTransferUtils
                                                .createHttpFileTransferXml(mMessagingLog
                                                        .getGroupFileDownloadInfo(id));
                                        groupChat.dequeueGroupFileInfo(id, fileInfo,
                                                mDisplayedReportEnabled, mDeliveryReportEnabled,
                                                groupFileTransfer);

                                    } catch (MsrpException e) {
                                        if (logActivated) {
                                            mLogger.debug(new StringBuilder(
                                                    "Failed to dequeue group file info '")
                                                    .append(id).append("' message on group chat '")
                                                    .append(mChatId).append("' due to: ")
                                                    .append(e.getMessage()).toString());
                                        }
                                    }
                                    break;
                                default:
                                    break;
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (RuntimeException e) {
            /*
             * Normally all the terminal and non-terminal cases should be handled above so if we
             * come here that means that there is a bug and so we output a stack trace so the bug
             * can then be properly tracked down and fixed. We also mark the respective entry that
             * failed to dequeue as FAILED.
             */
            mLogger.error(
                    new StringBuilder(
                            "Exception occured while dequeueing group chat message and group file transfer with id '")
                            .append(id).append("' and chatId '").append(mChatId).append("'")
                            .toString(), e);
            if (id == null) {
                return;
            }
            switch (providerId) {
                case MessageData.HISTORYLOG_MEMBER_ID:
                    setGroupChatMessageAsFailed(mChatId, id);
                    break;
                case FileTransferData.HISTORYLOG_MEMBER_ID:
                    setGroupFileTransferAsFailed(mChatId, id);
                    break;
                default:
                    break;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
