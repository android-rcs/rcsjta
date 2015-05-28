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
import com.gsma.rcs.core.ims.protocol.msrp.MsrpException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
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
import com.gsma.rcs.service.api.OneToOneChatImpl;
import com.gsma.rcs.service.api.OneToOneFileTransferImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.database.Cursor;
import android.net.Uri;

/**
 * OneToOneChatDequeueTask tries to dequeue and send all QUEUED one-one chat messages and QUEUED or
 * UPLOADED but not transferred one-one file transfers.
 */
public class OneToOneChatDequeueTask extends DequeueTask {

    private final ChatServiceImpl mChatService;

    private final FileTransferServiceImpl mFileTransferService;

    private final HistoryLog mHistoryLog;

    private final boolean mDisplayedReportEnabled;

    private final boolean mDeliveryReportEnabled;

    public OneToOneChatDequeueTask(Object lock, InstantMessagingService imService,
            ChatServiceImpl chatService, FileTransferServiceImpl fileTransferService,
            HistoryLog historyLog, MessagingLog messagingLog, ContactManager contactManager,
            RcsSettings rcsSettings) {
        super(lock, imService, contactManager, messagingLog, rcsSettings);
        mChatService = chatService;
        mFileTransferService = fileTransferService;
        mHistoryLog = historyLog;
        final ImdnManager imdnManager = mImService.getImdnManager();
        mDisplayedReportEnabled = imdnManager.isRequestGroupDeliveryDisplayedReportsEnabled();
        mDeliveryReportEnabled = imdnManager.isDeliveryDeliveredReportsEnabled();
    }

    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue all one-to-one chat messages and one-to-one file transfers.");
        }
        Cursor cursor = null;
        try {
            synchronized (mLock) {
                cursor = mHistoryLog.getQueuedOneToOneChatMessagesAndOneToOneFileTransfers();
                /* TODO: Handle cursor when null. */
                int providerIdIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_PROVIDER_ID);
                int idIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_ID);
                int contactIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_CONTACT);
                int contentIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_CONTENT);
                int mimeTypeIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_MIME_TYPE);
                int fileIconIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_FILEICON);
                int statusIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_STATUS);
                while (cursor.moveToNext()) {
                    int providerId = cursor.getInt(providerIdIdx);
                    String id = cursor.getString(idIdx);
                    String phoneNumber = cursor.getString(contactIdx);
                    ContactId contact = ContactUtil.createContactIdFromTrustedData(phoneNumber);
                    OneToOneChatImpl oneToOneChat = mChatService.getOrCreateOneToOneChat(contact);
                    try {
                        switch (providerId) {
                            case MessageData.HISTORYLOG_MEMBER_ID:
                                if (!isAllowedToDequeueOneToOneChatMessage(contact)) {
                                    continue;
                                }
                                String msgId = cursor.getString(idIdx);
                                String content = cursor.getString(contentIdx);
                                String mimeType = cursor.getString(mimeTypeIdx);
                                long timestamp = System.currentTimeMillis();
                                /* For outgoing message, timestampSent = timestamp */
                                ChatMessage message = ChatUtils.createChatMessage(msgId, mimeType,
                                        content, contact, null, timestamp, timestamp);
                                try {
                                    oneToOneChat.dequeueOneToOneChatMessage(message);
                                } catch (MsrpException e) {
                                    if (logActivated) {
                                        mLogger.debug(new StringBuilder(
                                                "Failed to dequeue one-one chat message '")
                                                .append(id).append("' message for contact '")
                                                .append(contact).append("' due to: ")
                                                .append(e.getMessage()).toString());
                                    }
                                }
                                break;
                            case FileTransferData.HISTORYLOG_MEMBER_ID:
                                State state = State.valueOf(cursor.getInt(statusIdx));
                                switch (state) {
                                    case QUEUED:
                                        if (!isAllowedToDequeueOneToOneFileTransfer()) {
                                            continue;
                                        }
                                        try {
                                            Uri file = Uri.parse(cursor.getString(contentIdx));
                                            MmContent fileContent = FileTransferUtils
                                                    .createMmContent(file);
                                            MmContent fileIconContent = null;
                                            String fileIcon = cursor.getString(fileIconIdx);
                                            if (fileIcon != null) {
                                                Uri fileIconUri = Uri.parse(fileIcon);
                                                fileIconContent = FileTransferUtils
                                                        .createMmContent(fileIconUri);
                                            }
                                            mFileTransferService.dequeueOneToOneFileTransfer(id,
                                                    contact, fileContent, fileIconContent);
                                        } catch (SecurityException e) {
                                            mLogger.error(
                                                    new StringBuilder(
                                                            "Security exception occured while dequeueing file transfer with transferId '")
                                                            .append(id)
                                                            .append("', so mark as failed")
                                                            .toString(), e);
                                            mFileTransferService
                                                    .setOneToOneFileTransferStateAndReasonCode(id,
                                                            contact, State.FAILED,
                                                            ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
                                        }
                                        break;
                                    case STARTED:
                                        if (!isAllowedToDequeueOneToOneChatMessage(contact)) {
                                            continue;
                                        }
                                        try {
                                            OneToOneFileTransferImpl oneToOneFileTransfer = mFileTransferService
                                                    .getOrCreateOneToOneFileTransfer(contact, id);
                                            String fileInfo = FileTransferUtils
                                                    .createHttpFileTransferXml(mMessagingLog
                                                            .getGroupFileDownloadInfo(id));
                                            oneToOneChat.dequeueOneToOneFileInfo(id, fileInfo,
                                                    mDisplayedReportEnabled,
                                                    mDeliveryReportEnabled, oneToOneFileTransfer);
                                        } catch (MsrpException e) {
                                            if (logActivated) {
                                                mLogger.debug(new StringBuilder(
                                                        "Failed to dequeue one-one file info '")
                                                        .append(id)
                                                        .append("' message for contact '")
                                                        .append(contact).append("' due to: ")
                                                        .append(e.getMessage()).toString());
                                            }
                                        } catch (SecurityException e) {
                                            mLogger.error(
                                                    new StringBuilder(
                                                            "Security exception occured while dequeueing file info with transferId '")
                                                            .append(id)
                                                            .append("', so mark as failed")
                                                            .toString(), e);
                                            mFileTransferService
                                                    .setOneToOneFileTransferStateAndReasonCode(id,
                                                            contact, State.FAILED,
                                                            ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
                                        }
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            default:
                                break;
                        }
                    } catch (RuntimeException e) {
                        /*
                         * Break only for terminal exception, in rest of the cases dequeue and try
                         * to send other messages.
                         */
                        mLogger.error(
                                new StringBuilder(
                                        "Exception occured while dequeueing one-to-one chat message and one-to-one file transfer with id '")
                                        .append(id).append("' for contact '").append(contact)
                                        .append("'!").toString(), e);
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
