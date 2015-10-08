/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 * Copyright (C) 2010 France Telecom S.A.
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
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.SessionNotEstablishedException;
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
 * transfers that are either QUEUED or UPLOADED but not transferred for a specific group chat.
 */
public class GroupChatDequeueTask extends DequeueTask {

    private final String mChatId;

    private final HistoryLog mHistoryLog;

    public GroupChatDequeueTask(Context ctx, Core core, MessagingLog messagingLog,
            ChatServiceImpl chatService, FileTransferServiceImpl fileTransferService,
            RcsSettings rcsSettings, ContactManager contactManager, HistoryLog historyLog,
            String chatId) {
        super(ctx, core, contactManager, messagingLog, rcsSettings, chatService,
                fileTransferService);
        mChatId = chatId;
        mHistoryLog = historyLog;
    }

    private void setGroupChatEntryAsFailedDequeue(int providerId, String chatId, String id,
            String mimeType) {
        switch (providerId) {
            case MessageData.HISTORYLOG_MEMBER_ID:
                setGroupChatMessageAsFailedDequeue(chatId, id, mimeType);
                break;
            case FileTransferData.HISTORYLOG_MEMBER_ID:
                setGroupFileTransferAsFailedDequeue(chatId, id);
                break;
            default:
                throw new IllegalArgumentException("Provider id " + providerId
                        + " not supported in this context!");
        }
    }

    @Override
    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue group chat messages and group file transfers for chatId "
                    .concat(mChatId));
        }
        ImdnManager imdnManager = mImService.getImdnManager();
        boolean displayedReportEnabled = imdnManager
                .isRequestGroupDeliveryDisplayedReportsEnabled();
        boolean deliveryReportEnabled = imdnManager.isDeliveryDeliveredReportsEnabled();
        int providerId = -1;
        String id = null;
        String mimeType = null;
        Cursor cursor = null;
        try {
            if (!isImsConnected()) {
                if (logActivated) {
                    mLogger.debug("IMS not connected, exiting dequeue task to dequeue group chat messages and group file transfers for chatId "
                            .concat(mChatId));
                }
                return;
            }
            if (isShuttingDownOrStopped()) {
                if (logActivated) {
                    mLogger.debug("Core service is shutting down/stopped, exiting dequeue task to dequeue group chat messages and group file transfers for chatId "
                            .concat(mChatId));
                }
                return;
            }
            cursor = mHistoryLog.getQueuedGroupChatMessagesAndGroupFileTransfers(mChatId);
            int providerIdIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_PROVIDER_ID);
            int idIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_ID);
            int contentIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_CONTENT);
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_MIME_TYPE);
            int fileIconIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_FILEICON);
            int statusIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_STATUS);
            int fileSizeIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_FILESIZE);
            GroupChatImpl groupChat = mChatService.getOrCreateGroupChat(mChatId);
            while (cursor.moveToNext()) {
                try {
                    if (!isImsConnected()) {
                        if (logActivated) {
                            mLogger.debug("IMS not connected, exiting dequeue task to dequeue group chat messages and group file transfers for chatId "
                                    .concat(mChatId));
                        }
                        return;
                    }
                    if (isShuttingDownOrStopped()) {
                        if (logActivated) {
                            mLogger.debug("Core service is shutting down/stopped, exiting dequeue task to dequeue group chat messages and group file transfers for chatId "
                                    .concat(mChatId));
                        }
                        return;
                    }
                    providerId = cursor.getInt(providerIdIdx);
                    id = cursor.getString(idIdx);
                    mimeType = cursor.getString(mimeTypeIdx);
                    switch (providerId) {
                        case MessageData.HISTORYLOG_MEMBER_ID:
                            if (!isPossibleToDequeueGroupChatMessagesAndGroupFileTransfers(mChatId)) {
                                setGroupChatMessageAsFailedDequeue(mChatId, id, mimeType);
                                continue;
                            }
                            long timestamp = System.currentTimeMillis();
                            String content = cursor.getString(contentIdx);
                            /* For outgoing message, timestampSent = timestamp */
                            ChatMessage message = ChatUtils.createChatMessage(id, mimeType,
                                    content, null, null, timestamp, timestamp);
                            groupChat.dequeueGroupChatMessage(message);
                            break;

                        case FileTransferData.HISTORYLOG_MEMBER_ID:
                            Uri file = Uri.parse(cursor.getString(contentIdx));
                            if (!isPossibleToDequeueGroupFileTransfer(mChatId, file,
                                    cursor.getLong(fileSizeIdx))) {
                                setGroupFileTransferAsFailedDequeue(mChatId, id);
                                continue;
                            }
                            State state = State.valueOf(cursor.getInt(statusIdx));
                            if (logActivated) {
                                mLogger.debug("Dequeue chatId=" + mChatId + " in state=" + state
                                        + " file=" + file);
                            }
                            GroupFileTransferImpl groupFileTransfer;
                            switch (state) {
                                case QUEUED:
                                    if (!isAllowedToDequeueGroupFileTransfer()) {
                                        continue;
                                    }
                                    MmContent fileContent = FileTransferUtils.createMmContent(file);
                                    MmContent fileIconContent = null;
                                    String fileIcon = cursor.getString(fileIconIdx);
                                    if (fileIcon != null) {
                                        Uri fileIconUri = Uri.parse(fileIcon);
                                        fileIconContent = FileTransferUtils
                                                .createMmContent(fileIconUri);
                                    }
                                    mFileTransferService.dequeueGroupFileTransfer(mChatId, id,
                                            fileContent, fileIconContent);
                                    break;

                                case STARTED:
                                    if (!isPossibleToDequeueGroupChatMessagesAndGroupFileTransfers(mChatId)) {
                                        setGroupFileTransferAsFailedDequeue(mChatId, id);
                                        continue;
                                    }
                                    groupFileTransfer = mFileTransferService
                                            .getOrCreateGroupFileTransfer(mChatId, id);
                                    String fileInfo = FileTransferUtils
                                            .createHttpFileTransferXml(mMessagingLog
                                                    .getGroupFileDownloadInfo(id));
                                    groupChat.dequeueGroupFileInfo(id, fileInfo,
                                            displayedReportEnabled, deliveryReportEnabled,
                                            groupFileTransfer);
                                    break;

                                default:
                                    break;
                            }
                            break;
                        default:
                            break;
                    }

                } catch (SessionNotEstablishedException e) {
                    if (logActivated) {
                        mLogger.debug(new StringBuilder(
                                "Failed to dequeue group chat entry with id '").append(id)
                                .append("' on group chat '").append(mChatId).append("' due to: ")
                                .append(e.getMessage()).toString());
                    }
                } catch (NetworkException e) {
                    if (logActivated) {
                        mLogger.debug(new StringBuilder(
                                "Failed to dequeue group chat entry with id '").append(id)
                                .append("' on group chat '").append(mChatId).append("' due to: ")
                                .append(e.getMessage()).toString());
                    }

                } catch (PayloadException e) {
                    mLogger.error(new StringBuilder("Failed to dequeue group chat entry with id '")
                            .append(id).append("' on group chat '").append(mChatId).toString(), e);
                    setGroupChatEntryAsFailedDequeue(providerId, mChatId, id, mimeType);

                } catch (RuntimeException e) {
                    /*
                     * Normally all the terminal and non-terminal cases should be handled above so
                     * if we come here that means that there is a bug and so we output a stack trace
                     * so the bug can then be properly tracked down and fixed. We also mark the
                     * respective entry that failed to dequeue as FAILED.
                     */
                    mLogger.error(new StringBuilder("Failed to dequeue group chat entry with id '")
                            .append(id).append("' and chatId '").append(mChatId).append("'")
                            .toString(), e);
                    setGroupChatEntryAsFailedDequeue(providerId, mChatId, id, mimeType);
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
            setGroupChatEntryAsFailedDequeue(providerId, mChatId, id, mimeType);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
