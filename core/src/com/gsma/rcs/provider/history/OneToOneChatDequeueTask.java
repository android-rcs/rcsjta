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
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.SessionUnavailableException;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessageData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.DequeueTask;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.service.api.OneToOneChatImpl;
import com.gsma.rcs.service.api.OneToOneFileTransferImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * OneToOneChatDequeueTask tries to dequeue and send all QUEUED one-one chat messages and QUEUED or
 * UPLOADED but not transferred one-one file transfers.
 */
public class OneToOneChatDequeueTask extends DequeueTask {

    private final HistoryLog mHistoryLog;

    public OneToOneChatDequeueTask(Context ctx, Core core, MessagingLog messagingLog,
            RcsSettings rcsSettings, ChatServiceImpl chatService,
            FileTransferServiceImpl fileTransferService, ContactManager contactManager,
            HistoryLog historyLog) {
        super(ctx, core, contactManager, messagingLog, rcsSettings, chatService,
                fileTransferService);
        mHistoryLog = historyLog;
    }

    private void setOneToOneChatEntryAsFailedDequeue(int providerId, ContactId contact, String id,
            String mimeType) {
        switch (providerId) {
            case MessageData.HISTORYLOG_MEMBER_ID:
                setOneToOneChatMessageAsFailedDequeue(contact, id, mimeType);
                break;
            case FileTransferData.HISTORYLOG_MEMBER_ID:
                setOneToOneFileTransferAsFailedDequeue(contact, id);
                break;
            default:
                throw new IllegalArgumentException("Provider id " + providerId
                        + " not supported in this context!");
        }
    }

    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue all one-to-one chat messages and one-to-one file transfers.");
        }
        int providerId = -1;
        String id = null;
        ContactId contact = null;
        String mimeType = null;
        Cursor cursor = null;
        ImdnManager imdnManager = mImService.getImdnManager();
        boolean displayedReportEnabled = imdnManager
                .isRequestGroupDeliveryDisplayedReportsEnabled();
        boolean deliveryReportEnabled = imdnManager.isDeliveryDeliveredReportsEnabled();
        try {
            if (!isImsConnected()) {
                if (logActivated) {
                    mLogger.debug("IMS not connected, exiting dequeue task to dequeue all one-to-one chat messages and one-to-one file transfers.");
                }
                return;
            }
            if (isShuttingDownOrStopped()) {
                if (logActivated) {
                    mLogger.debug("Core service is shutting down/stopped, exiting dequeue task to dequeue all one-to-one chat messages and one-to-one file transfers.");
                }
                return;
            }
            cursor = mHistoryLog.getQueuedOneToOneChatMessagesAndOneToOneFileTransfers();
            int providerIdIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_PROVIDER_ID);
            int idIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_CONTACT);
            int contentIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_CONTENT);
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_MIME_TYPE);
            int fileIconIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_FILEICON);
            int statusIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_STATUS);
            int fileSizeIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_FILESIZE);
            while (cursor.moveToNext()) {
                try {
                    if (!isImsConnected()) {
                        if (logActivated) {
                            mLogger.debug("IMS not connected, exiting dequeue task to dequeue all one-to-one chat messages and one-to-one file transfers.");
                        }
                        return;
                    }
                    if (isShuttingDownOrStopped()) {
                        if (logActivated) {
                            mLogger.debug("Core service is shutting down/stopped, exiting dequeue task to dequeue all one-to-one chat messages and one-to-one file transfers.");
                        }
                        return;
                    }
                    providerId = cursor.getInt(providerIdIdx);
                    id = cursor.getString(idIdx);
                    String phoneNumber = cursor.getString(contactIdx);
                    contact = ContactUtil.createContactIdFromTrustedData(phoneNumber);
                    OneToOneChatImpl oneToOneChat = mChatService.getOrCreateOneToOneChat(contact);
                    mimeType = cursor.getString(mimeTypeIdx);
                    switch (providerId) {
                        case MessageData.HISTORYLOG_MEMBER_ID:
                            if (!isPossibleToDequeueOneToOneChatMessage(contact)) {
                                setOneToOneChatMessageAsFailedDequeue(contact, id, mimeType);
                                continue;
                            }
                            if (!isAllowedToDequeueOneToOneChatMessage(contact)) {
                                continue;
                            }
                            String content = cursor.getString(contentIdx);
                            long timestamp = System.currentTimeMillis();
                            /* For outgoing message, timestampSent = timestamp */
                            ChatMessage msg = ChatUtils.createChatMessage(id, mimeType, content,
                                    contact, null, timestamp, timestamp);
                            oneToOneChat.dequeueOneToOneChatMessage(msg);
                            break;
                        case FileTransferData.HISTORYLOG_MEMBER_ID:
                            Uri file = Uri.parse(cursor.getString(contentIdx));
                            if (!isPossibleToDequeueOneToOneFileTransfer(contact, file,
                                    cursor.getLong(fileSizeIdx))) {
                                setOneToOneFileTransferAsFailedDequeue(contact, id);
                                continue;
                            }
                            State state = State.valueOf(cursor.getInt(statusIdx));
                            switch (state) {
                                case QUEUED:
                                    if (!isAllowedToDequeueOneToOneFileTransfer(contact,
                                            mFileTransferService)) {
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
                                    mFileTransferService.dequeueOneToOneFileTransfer(id, contact,
                                            fileContent, fileIconContent);
                                    break;
                                case STARTED:
                                    if (!isPossibleToDequeueOneToOneChatMessage(contact)) {
                                        setOneToOneFileTransferAsFailedDequeue(contact, id);
                                        continue;
                                    }
                                    if (!isAllowedToDequeueOneToOneChatMessage(contact)) {
                                        continue;
                                    }
                                    String fileInfo = FileTransferUtils
                                            .createHttpFileTransferXml(mMessagingLog
                                                    .getFileDownloadInfo(id));
                                    OneToOneFileTransferImpl oneToOneFileTransfer = mFileTransferService
                                            .getOrCreateOneToOneFileTransfer(id);
                                    oneToOneChat.dequeueOneToOneFileInfo(id, fileInfo,
                                            displayedReportEnabled, deliveryReportEnabled,
                                            oneToOneFileTransfer);
                                    break;
                                default:
                                    break;
                            }
                            break;
                        default:
                            break;
                    }
                } catch (SessionUnavailableException | FileAccessException | NetworkException e) {
                    if (logActivated) {
                        mLogger.debug(new StringBuilder("Failed to dequeue one-one entry with id '")
                                .append(id).append("' for contact '").append(contact)
                                .append("' due to: ").append(e.getMessage()).toString());
                    }

                } catch (PayloadException e) {
                    mLogger.error(new StringBuilder("Failed to dequeue one-one entry with id '")
                            .append(id).append("' for contact '").append(contact).toString(), e);
                    setOneToOneChatEntryAsFailedDequeue(providerId, contact, id, mimeType);

                } catch (RuntimeException e) {
                    /*
                     * Normally all the terminal and non-terminal cases should be handled above so
                     * if we come here that means that there is a bug and so we output a stack trace
                     * so the bug can then be properly tracked down and fixed. We also mark the
                     * respective entry that failed to dequeue as FAILED.
                     */
                    mLogger.error(new StringBuilder("Failed to dequeue one-one entry with id '")
                            .append(id).append("' for contact '").append(contact).append("'!")
                            .toString(), e);
                    setOneToOneChatEntryAsFailedDequeue(providerId, contact, id, mimeType);
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
                            "Exception occurred while dequeueing one-to-one chat message and one-to-one file transfer with id '")
                            .append(id).append("' for contact '").append(contact).append("'!")
                            .toString(), e);
            if (id == null) {
                return;
            }
            setOneToOneChatEntryAsFailedDequeue(providerId, contact, id, mimeType);

        } finally {
            CursorUtil.close(cursor);
        }
    }
}
