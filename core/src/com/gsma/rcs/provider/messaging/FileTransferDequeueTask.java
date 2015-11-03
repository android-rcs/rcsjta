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
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.SessionNotEstablishedException;
import com.gsma.rcs.core.ims.service.im.chat.SessionUnavailableException;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.CursorUtil;
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

    public FileTransferDequeueTask(Context ctx, Core core, MessagingLog messagingLog,
            ChatServiceImpl chatService, FileTransferServiceImpl fileTransferService,
            ContactManager contactManager, RcsSettings rcsSettings) {
        super(ctx, core, contactManager, messagingLog, rcsSettings, chatService,
                fileTransferService);
    }

    private void setGroupFileTransferAsFailedDequeue(String id, boolean groupFile,
            ContactId contact, String chatId) {
        if (groupFile) {
            setGroupFileTransferAsFailedDequeue(chatId, id);
        } else {
            setOneToOneFileTransferAsFailedDequeue(contact, id);
        }
    }

    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue one-to-one and group file transfers");
        }
        String id = null;
        ContactId contact = null;
        String chatId = null;
        boolean groupFile = false;
        Cursor cursor = null;
        ImdnManager imdnManager = mImService.getImdnManager();
        boolean displayedReportEnabled = imdnManager
                .isRequestGroupDeliveryDisplayedReportsEnabled();
        boolean deliveryReportEnabled = imdnManager.isDeliveryDeliveredReportsEnabled();
        try {
            if (!isImsConnected()) {
                if (logActivated) {
                    mLogger.debug("IMS not connected, exiting dequeue task to dequeue one-to-one and group file transfers");
                }
                return;
            }
            if (isShuttingDownOrStopped()) {
                if (logActivated) {
                    mLogger.debug("Core service is shutting down/stopped, exiting dequeue task to dequeue one-to-one and group file transfers");
                }
                return;
            }
            cursor = mMessagingLog.getQueuedAndUploadedButNotTransferredFileTransfers();
            int fileTransferIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FT_ID);
            int fileIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILE);
            int fileIconIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILEICON);
            int contactIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CONTACT);
            int chatIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CHAT_ID);
            int stateIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_STATE);
            int fileSizeIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILESIZE);
            while (cursor.moveToNext()) {
                try {
                    if (!isImsConnected()) {
                        if (logActivated) {
                            mLogger.debug("IMS not connected, exiting dequeue task to dequeue one-to-one and group file transfers");
                        }
                        return;
                    }
                    if (isShuttingDownOrStopped()) {
                        if (logActivated) {
                            mLogger.debug("Core service is shutting down/stopped, exiting dequeue task to dequeue one-to-one and group file transfers");
                        }
                        return;
                    }
                    id = cursor.getString(fileTransferIdIdx);
                    String contactNumber = cursor.getString(contactIdx);
                    contact = contactNumber != null ? ContactUtil
                            .createContactIdFromTrustedData(contactNumber) : null;
                    chatId = cursor.getString(chatIdIdx);
                    groupFile = !chatId.equals(contactNumber);
                    State state = State.valueOf(cursor.getInt(stateIdx));
                    Uri file = Uri.parse(cursor.getString(fileIdx));
                    long size = cursor.getLong(fileSizeIdx);
                    if (groupFile) {
                        if (!isPossibleToDequeueGroupFileTransfer(chatId, file, size)) {
                            setGroupFileTransferAsFailedDequeue(chatId, id);
                            continue;
                        }
                    } else {
                        if (!isPossibleToDequeueOneToOneFileTransfer(contact, file, size)) {
                            setOneToOneFileTransferAsFailedDequeue(contact, id);
                            continue;
                        }
                    }
                    switch (state) {
                        case QUEUED:
                            if (groupFile) {
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
                                fileIconContent = FileTransferUtils.createMmContent(fileIconUri);
                            }
                            if (groupFile) {
                                mFileTransferService.dequeueGroupFileTransfer(chatId, id, content,
                                        fileIconContent);
                            } else {
                                mFileTransferService.dequeueOneToOneFileTransfer(id, contact,
                                        content, fileIconContent);
                            }
                            break;

                        case STARTED:
                            if (groupFile) {
                                if (!isPossibleToDequeueGroupChatMessagesAndGroupFileTransfers(chatId)) {
                                    setGroupFileTransferAsFailedDequeue(chatId, id);
                                    continue;
                                }
                            } else {
                                if (!isPossibleToDequeueOneToOneChatMessage(contact)) {
                                    setOneToOneFileTransferAsFailedDequeue(contact, id);
                                    continue;
                                }
                                if (!isAllowedToDequeueOneToOneChatMessage(contact)) {
                                    continue;
                                }
                            }
                            String fileInfo = FileTransferUtils
                                    .createHttpFileTransferXml(mMessagingLog
                                            .getFileDownloadInfo(cursor));
                            if (groupFile) {
                                GroupChatImpl groupChat = mChatService.getOrCreateGroupChat(chatId);
                                GroupFileTransferImpl groupFileTransfer = mFileTransferService
                                        .getOrCreateGroupFileTransfer(chatId, id);
                                groupChat.dequeueGroupFileInfo(id, fileInfo,
                                        displayedReportEnabled, deliveryReportEnabled,
                                        groupFileTransfer);
                            } else {
                                OneToOneChatImpl oneToOneChat = mChatService
                                        .getOrCreateOneToOneChat(contact);
                                OneToOneFileTransferImpl oneToOneFileTransfer = mFileTransferService
                                        .getOrCreateOneToOneFileTransfer(id);
                                oneToOneChat.dequeueOneToOneFileInfo(id, fileInfo,
                                        displayedReportEnabled, deliveryReportEnabled,
                                        oneToOneFileTransfer);
                            }
                            break;

                        default:
                            break;
                    }

                } catch (SessionUnavailableException | SessionNotEstablishedException
                        | FileAccessException | NetworkException e) {
                    if (logActivated) {
                        mLogger.debug(new StringBuilder(
                                "Failed to dequeue file transfer with fileTransferId '").append(id)
                                .append("' on chat '").append(chatId).append("' due to: ")
                                .append(e.getMessage()).toString());
                    }

                } catch (PayloadException e) {
                    mLogger.error(new StringBuilder(
                            "Failed to dequeue file transfer with fileTransferId '").append(id)
                            .append("' on chat '").append(chatId).toString(), e);
                    setGroupFileTransferAsFailedDequeue(id, groupFile, contact, chatId);

                } catch (RuntimeException e) {
                    /*
                     * Normally all the terminal and non-terminal cases should be handled above so
                     * if we come here that means that there is a bug and so we output a stack trace
                     * so the bug can then be properly tracked down and fixed. We also mark the
                     * respective entry that failed to dequeue as FAILED.
                     */
                    mLogger.error(new StringBuilder(
                            "Failed to dequeue file transfer with fileTransferId '").append(id)
                            .append("' and chatId '").append(chatId).append("'!").toString(), e);
                    setGroupFileTransferAsFailedDequeue(id, groupFile, contact, chatId);
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
                            "Exception occurred while dequeueing file transfer with transferId '")
                            .append(id).append("' and chatId '").append(chatId).append("'!")
                            .toString(), e);
            if (id == null) {
                return;
            }
            setGroupFileTransferAsFailedDequeue(id, groupFile, contact, chatId);

        } finally {
            CursorUtil.close(cursor);
        }
    }
}
