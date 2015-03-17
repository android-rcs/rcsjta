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

package com.gsma.rcs.service;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.ContactUtils;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.database.Cursor;
import android.net.Uri;

/* Dequeue all one-to-one and group file transfers */
/* package private */class FileTransferDequeueTask extends DequeueTask {

    private final FileTransferServiceImpl mFileTransferService;

    /* package private */FileTransferDequeueTask(Object lock, InstantMessagingService imService,
            MessagingLog messagingLog, FileTransferServiceImpl fileTransferService,
            ContactsManager contactManager, RcsSettings rcsSettings) {
        super(lock, imService, contactManager, messagingLog, rcsSettings);
        mFileTransferService = fileTransferService;
    }

    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to dequeue one-to-one and group file transfers");
        }
        Cursor cursor = null;
        try {
            synchronized (mLock) {
                cursor = mMessagingLog.getQueuedFileTransfers();
                int fileTransferIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FT_ID);
                int fileIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILE);
                int fileIconIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILEICON);
                int contactIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CONTACT);
                int chatIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CHAT_ID);
                while (cursor.moveToNext()) {
                    String fileTransferId = cursor.getString(fileTransferIdIdx);
                    String chatId = cursor.getString(chatIdIdx);
                    String contactNumber = cursor.getString(contactIdx);
                    ContactId contact = contactNumber != null ? ContactUtils
                            .createContactId(contactNumber) : null;
                    boolean isGroupFileTransfer = !chatId.equals(contactNumber);
                    try {
                        Uri file = Uri.parse(cursor.getString(fileIdx));
                        MmContent content = FileTransferUtils.createMmContent(file);
                        MmContent fileIconContent = null;
                        String fileIcon = cursor.getString(fileIconIdx);
                        if (fileIcon != null) {
                            Uri fileIconUri = Uri.parse(fileIcon);
                            fileIconContent = FileTransferUtils.createMmContent(fileIconUri);
                        }
                        if (!isGroupFileTransfer) {
                            if (isAllowedToDequeueOneToOneFileTransfer()) {
                                mFileTransferService.dequeueOneToOneFileTransfer(fileTransferId,
                                        contact, content, fileIconContent);
                            }
                        } else {
                            if (isAllowedToDequeueGroupFileTransfer(chatId)) {
                                mFileTransferService.dequeueGroupFileTransfer(fileTransferId,
                                        content, fileIconContent, chatId);
                            }
                        }
                    } catch (SecurityException e) {
                        if (logActivated) {
                            mLogger.error(new StringBuilder(
                                    "Security exception occured while dequeueing file transfer with transferId '")
                                    .append(fileTransferId).append("', so mark as failed")
                                    .toString());
                        }
                        if (!isGroupFileTransfer) {
                            mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                    fileTransferId, contact, State.FAILED,
                                    ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
                        } else {
                            mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                    fileTransferId, chatId, State.FAILED,
                                    ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
                        }
                    } catch (Exception e) {
                        /*
                         * Break only for terminal exception, in rest of the cases dequeue and try
                         * to send other messages.
                         */
                        if (logActivated) {
                            mLogger.error(
                                    new StringBuilder(
                                            "Exception occured while dequeueing file transfer with transferId '")
                                            .append(fileTransferId).append("' ").toString(), e);
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
