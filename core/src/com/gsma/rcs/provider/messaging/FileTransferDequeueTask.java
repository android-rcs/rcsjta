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
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpException;
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
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * FileTransferDequeueTask tries to dequeue all one-to-one and group file transfers that are either
 * QUEUED or UPLOADED but not transferred
 */
public class FileTransferDequeueTask extends DequeueTask {

    public FileTransferDequeueTask(Object lock, Context ctx, Core core, MessagingLog messagingLog,
            ChatServiceImpl chatService, FileTransferServiceImpl fileTransferService,
            ContactManager contactManager, RcsSettings rcsSettings) {
        super(lock, ctx, core, contactManager, messagingLog, rcsSettings, chatService,
                fileTransferService);

    }

    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue one-to-one and group file transfers");
        }
        String id = null;
        ContactId contact = null;
        String chatId = null;
        boolean isGroupFileTransfer = false;
        Cursor cursor = null;
        ImdnManager imdnManager = mImService.getImdnManager();
        boolean displayedReportEnabled = imdnManager
                .isRequestGroupDeliveryDisplayedReportsEnabled();
        boolean deliveryReportEnabled = imdnManager.isDeliveryDeliveredReportsEnabled();
        try {
            synchronized (mLock) {
                if (!isImsConnected()) {
                    if (logActivated) {
                        mLogger.debug("IMS not connected, exiting dequeue task to dequeue one-to-one and group file transfers");
                    }
                    return;
                }
                if (mCore.isStopping()) {
                    if (logActivated) {
                        mLogger.debug("Core service is stopped, exiting dequeue task to dequeue one-to-one and group file transfers");
                    }
                    return;
                }
                cursor = mMessagingLog.getQueuedAndUploadedButNotTransferredFileTransfers();
                /* TODO: Handle cursor when null. */
                int fileTransferIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FT_ID);
                int fileIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILE);
                int fileIconIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILEICON);
                int contactIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CONTACT);
                int chatIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CHAT_ID);
                int stateIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_STATE);
                int fileSizeIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILESIZE);
                while (cursor.moveToNext()) {
                    if (!isImsConnected()) {
                        if (logActivated) {
                            mLogger.debug("IMS not connected, exiting dequeue task to dequeue one-to-one and group file transfers");
                        }
                        return;
                    }
                    if (mCore.isStopping()) {
                        if (logActivated) {
                            mLogger.debug("Core service is stopped, exiting dequeue task to dequeue one-to-one and group file transfers");
                        }
                        return;
                    }
                    id = cursor.getString(fileTransferIdIdx);
                    String contactNumber = cursor.getString(contactIdx);
                    contact = contactNumber != null ? ContactUtil
                            .createContactIdFromTrustedData(contactNumber) : null;
                    chatId = cursor.getString(chatIdIdx);
                    isGroupFileTransfer = !chatId.equals(contactNumber);
                    State state = State.valueOf(cursor.getInt(stateIdx));
                    Uri file = Uri.parse(cursor.getString(fileIdx));
                    long size = cursor.getLong(fileSizeIdx);
                    if (isGroupFileTransfer) {
                        if (!isPossibleToDequeueGroupFileTransfer(chatId, file, size)) {
                            setGroupFileTransferAsFailed(chatId, id);
                            continue;
                        }
                    } else {
                        if (!isPossibleToDequeueOneToOneFileTransfer(contact, file, size)) {
                            setOneToOneFileTransferAsFailed(contact, id);
                            continue;
                        }
                    }
                    switch (state) {
                        case QUEUED:
                            try {
                                if (isGroupFileTransfer) {
                                    if (!isAllowedToDequeueGroupFileTransfer()) {
                                        continue;
                                    }
                                } else {
                                    if (!isAllowedToDequeueOneToOneFileTransfer(contact,
                                            mFileTransferService)) {
                                        continue;
                                    }
                                }
                                MmContent content = FileTransferUtils.createMmContent(file);
                                MmContent fileIconContent = null;
                                String fileIcon = cursor.getString(fileIconIdx);
                                if (fileIcon != null) {
                                    Uri fileIconUri = Uri.parse(fileIcon);
                                    fileIconContent = FileTransferUtils
                                            .createMmContent(fileIconUri);
                                }
                                if (isGroupFileTransfer) {
                                    mFileTransferService.dequeueGroupFileTransfer(chatId, id,
                                            content, fileIconContent);
                                } else {
                                    mFileTransferService.dequeueOneToOneFileTransfer(id, contact,
                                            content, fileIconContent);
                                }
                            } catch (MsrpException e) {
                                if (logActivated) {
                                    mLogger.debug(new StringBuilder(
                                            "Failed to dequeue file transfer with fileTransferId '")
                                            .append(id).append("' on chat '").append(chatId)
                                            .append("' due to: ").append(e.getMessage()).toString());
                                }
                            }
                            break;
                        case STARTED:
                            if (isGroupFileTransfer) {
                                if (!isPossibleToDequeueGroupChatMessage(chatId)) {
                                    setGroupFileTransferAsFailed(chatId, id);
                                    continue;
                                }
                            } else {
                                if (!isPossibleToDequeueOneToOneChatMessage(contact)) {
                                    setOneToOneFileTransferAsFailed(contact, id);
                                    continue;
                                }
                                if (!isAllowedToDequeueOneToOneChatMessage(contact)) {
                                    continue;
                                }
                            }
                            try {
                                String fileInfo = FileTransferUtils
                                        .createHttpFileTransferXml(mMessagingLog
                                                .getGroupFileDownloadInfo(id));
                                if (isGroupFileTransfer) {
                                    GroupChatImpl groupChat = mChatService
                                            .getOrCreateGroupChat(chatId);
                                    GroupFileTransferImpl groupFileTransfer = mFileTransferService
                                            .getOrCreateGroupFileTransfer(chatId, id);
                                    groupChat.dequeueGroupFileInfo(id, fileInfo,
                                            displayedReportEnabled, deliveryReportEnabled,
                                            groupFileTransfer);
                                } else {
                                    OneToOneChatImpl oneToOneChat = mChatService
                                            .getOrCreateOneToOneChat(contact);
                                    OneToOneFileTransferImpl oneToOneFileTransfer = mFileTransferService
                                            .getOrCreateOneToOneFileTransfer(contact, id);
                                    oneToOneChat.dequeueOneToOneFileInfo(id, fileInfo,
                                            displayedReportEnabled, deliveryReportEnabled,
                                            oneToOneFileTransfer);
                                }
                            } catch (MsrpException e) {
                                if (logActivated) {
                                    mLogger.debug(new StringBuilder(
                                            "Failed to dequeue file transfer with fileTransferId '")
                                            .append(id).append("' on chat '").append(chatId)
                                            .append("' due to: ").append(e.getMessage()).toString());
                                }
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
            mLogger.error(new StringBuilder(
                    "Exception occured while dequeueing file transfer with transferId '")
                    .append(id).append("' and chatId '").append(chatId).append("'!").toString(), e);
            if (id == null) {
                return;
            }
            if (isGroupFileTransfer) {
                setGroupFileTransferAsFailed(chatId, id);
            } else {
                setOneToOneFileTransferAsFailed(contact, id);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
