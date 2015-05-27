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

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.DequeueTask;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.service.api.GroupChatImpl;
import com.gsma.rcs.service.api.GroupFileTransferImpl;
import com.gsma.rcs.service.api.OneToOneChatImpl;
import com.gsma.rcs.service.api.OneToOneFileTransferImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.database.Cursor;
import android.net.Uri;

/**
 * FileTransferDequeueTask tries to dequeue all one-to-one and group file transfers that are either
 * QUEUED or UPLOADED but not transferred
 */
public class FileTransferDequeueTask extends DequeueTask {

    private final ChatServiceImpl mChatService;

    private final FileTransferServiceImpl mFileTransferService;

    private final boolean mDisplayedReportEnabled;

    private final boolean mDeliveryReportEnabled;

    public FileTransferDequeueTask(Object lock, InstantMessagingService imService,
            MessagingLog messagingLog, ChatServiceImpl chatService,
            FileTransferServiceImpl fileTransferService, ContactManager contactManager,
            RcsSettings rcsSettings) {
        super(lock, imService, contactManager, messagingLog, rcsSettings);
        mChatService = chatService;
        mFileTransferService = fileTransferService;
        final ImdnManager imdnManager = mImService.getImdnManager();
        mDisplayedReportEnabled = imdnManager.isRequestGroupDeliveryDisplayedReportsEnabled();
        mDeliveryReportEnabled = imdnManager.isDeliveryDeliveredReportsEnabled();
    }

    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue one-to-one and group file transfers");
        }
        Cursor cursor = null;
        try {
            synchronized (mLock) {
                cursor = mMessagingLog.getQueuedAndUploadedButNotTransferredFileTransfers();
                /* TODO: Handle cursor when null. */
                int fileTransferIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FT_ID);
                int fileIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILE);
                int fileIconIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILEICON);
                int contactIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CONTACT);
                int chatIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CHAT_ID);
                int stateIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_STATE);
                while (cursor.moveToNext()) {
                    String fileTransferId = cursor.getString(fileTransferIdIdx);
                    String chatId = cursor.getString(chatIdIdx);
                    String contactNumber = cursor.getString(contactIdx);
                    ContactId contact = contactNumber != null ? ContactUtil
                            .createContactIdFromTrustedData(contactNumber) : null;
                    boolean isGroupFileTransfer = !chatId.equals(contactNumber);
                    try {
                        State state = State.valueOf(cursor.getInt(stateIdx));
                        switch (state) {
                            case QUEUED:
                                if (isGroupFileTransfer) {
                                    if (!isAllowedToDequeueGroupFileTransfer(chatId)) {
                                        continue;
                                    }
                                } else {
                                    if (!isAllowedToDequeueOneToOneFileTransfer()) {
                                        continue;
                                    }
                                }
                                try {
                                    Uri file = Uri.parse(cursor.getString(fileIdx));
                                    MmContent content = FileTransferUtils.createMmContent(file);
                                    MmContent fileIconContent = null;
                                    String fileIcon = cursor.getString(fileIconIdx);
                                    if (fileIcon != null) {
                                        Uri fileIconUri = Uri.parse(fileIcon);
                                        fileIconContent = FileTransferUtils
                                                .createMmContent(fileIconUri);
                                    }
                                    if (isGroupFileTransfer) {
                                        mFileTransferService.dequeueGroupFileTransfer(chatId,
                                                fileTransferId, content, fileIconContent);
                                    } else {
                                        mFileTransferService.dequeueOneToOneFileTransfer(
                                                fileTransferId, contact, content, fileIconContent);
                                    }
                                } catch (SecurityException e) {
                                    mLogger.error(new StringBuilder(
                                            "Security exception occured while dequeueing file transfer with transferId '")
                                            .append(fileTransferId).append("', so mark as failed")
                                            .toString());
                                    if (isGroupFileTransfer) {
                                        mFileTransferService
                                                .setGroupFileTransferStateAndReasonCode(
                                                        fileTransferId, chatId, State.FAILED,
                                                        ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
                                    } else {
                                        mFileTransferService
                                                .setOneToOneFileTransferStateAndReasonCode(
                                                        fileTransferId, contact, State.FAILED,
                                                        ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
                                    }
                                }
                                break;
                            case STARTED:
                                if (isGroupFileTransfer) {
                                    if (!isAllowedToDequeueGroupChatMessage(chatId)) {
                                        continue;
                                    }
                                } else {
                                    if (!isAllowedToDequeueOneToOneChatMessage(contact)) {
                                        continue;
                                    }
                                }
                                try {
                                    String fileInfo = FileTransferUtils
                                            .createHttpFileTransferXml(mMessagingLog
                                                    .getGroupFileDownloadInfo(fileTransferId));
                                    if (isGroupFileTransfer) {
                                        GroupChatImpl groupChat = mChatService
                                                .getOrCreateGroupChat(chatId);
                                        GroupFileTransferImpl groupFileTransfer = mFileTransferService
                                                .getOrCreateGroupFileTransfer(chatId,
                                                        fileTransferId);
                                        groupChat.dequeueGroupFileInfo(fileTransferId, fileInfo,
                                                mDisplayedReportEnabled, mDeliveryReportEnabled,
                                                groupFileTransfer);
                                    } else {
                                        OneToOneChatImpl oneToOneChat = mChatService
                                                .getOrCreateOneToOneChat(contact);
                                        OneToOneFileTransferImpl oneToOneFileTransfer = mFileTransferService
                                                .getOrCreateOneToOneFileTransfer(contact,
                                                        fileTransferId);
                                        oneToOneChat.dequeueOneToOneFileInfo(fileTransferId,
                                                fileInfo, mDisplayedReportEnabled,
                                                mDeliveryReportEnabled, oneToOneFileTransfer);
                                    }
                                } catch (MsrpException e) {
                                    mLogger.error(e.getMessage());
                                } catch (SecurityException e) {
                                    mLogger.error(new StringBuilder(
                                            "Security exception occured while dequeueing file info with transferId '")
                                            .append(fileTransferId).append("', so mark as failed")
                                            .toString());
                                    if (isGroupFileTransfer) {
                                        mFileTransferService
                                                .setGroupFileTransferStateAndReasonCode(
                                                        fileTransferId, chatId, State.FAILED,
                                                        ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
                                    } else {
                                        mFileTransferService
                                                .setOneToOneFileTransferStateAndReasonCode(
                                                        fileTransferId, contact, State.FAILED,
                                                        ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
                                    }
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
                                        "Exception occured while dequeueing file transfer with transferId '")
                                        .append(fileTransferId).append("' and chatId '")
                                        .append(chatId).append("'!").toString(), e);
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
