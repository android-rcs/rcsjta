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

import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.database.Cursor;

/*package private*/class UpdateFileTransferStateTask implements Runnable {

    private final MessagingLog mMessagingLog;

    private final FileTransferServiceImpl mFileTransferService;

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    /* package private */UpdateFileTransferStateTask(MessagingLog messagingLog,
            FileTransferServiceImpl fileTransferService) {
        mMessagingLog = messagingLog;
        mFileTransferService = fileTransferService;
    }

    public void run() {
        Cursor cursor = null;
        try {
            cursor = mMessagingLog.getInterruptedFileTransfers();
            int chatIdIdx = cursor.getColumnIndex(FileTransferData.KEY_CHAT_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CONTACT);
            int fileTransferIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FT_ID);
            int directionIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_DIRECTION);
            int uploadIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_UPLOAD_TID);
            int downloadUriIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_DOWNLOAD_URI);
            while (cursor.moveToNext()) {
                String fileTransferId = cursor.getString(fileTransferIdIdx);
                String contactNumber = cursor.getString(contactIdx);
                String chatId = cursor.getString(chatIdIdx);
                ContactId contact = contactNumber != null ? ContactUtil
                        .createContactIdFromTrustedData(contactNumber) : null;
                Direction direction = Direction.valueOf(cursor.getInt(directionIdx));
                boolean groupFileTransfer = !chatId.equals(contact);
                switch (direction) {
                    case INCOMING:
                        if (cursor.getString(downloadUriIdx) == null) {
                            if (groupFileTransfer) {
                                mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                        fileTransferId, chatId, State.FAILED,
                                        ReasonCode.FAILED_DATA_TRANSFER);
                            } else {
                                mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                        fileTransferId, contact, State.FAILED,
                                        ReasonCode.FAILED_DATA_TRANSFER);
                            }
                        } else {
                            if (groupFileTransfer) {
                                mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                        fileTransferId, chatId, State.PAUSED,
                                        ReasonCode.PAUSED_BY_SYSTEM);
                            } else {
                                mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                        fileTransferId, contact, State.PAUSED,
                                        ReasonCode.PAUSED_BY_SYSTEM);
                            }
                        }
                        break;
                    case OUTGOING:
                        if (cursor.getString(uploadIdIdx) == null) {
                            if (groupFileTransfer) {
                                mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                        fileTransferId, chatId, State.FAILED,
                                        ReasonCode.FAILED_DATA_TRANSFER);
                            } else {
                                mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                        fileTransferId, contact, State.FAILED,
                                        ReasonCode.FAILED_DATA_TRANSFER);
                            }
                        } else {
                            if (groupFileTransfer) {
                                mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                        fileTransferId, chatId, State.PAUSED,
                                        ReasonCode.PAUSED_BY_SYSTEM);
                            } else {
                                mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                        fileTransferId, contact, State.PAUSED,
                                        ReasonCode.PAUSED_BY_SYSTEM);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            /*
             * Exception will be handled better in CR037.
             */
            if (mLogger.isActivated()) {
                mLogger.error(
                        "Exception occured while trying to update file transfer state for interrupted file transfers",
                        e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
