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

import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.database.Cursor;

public class UpdateFileTransferStateAfterUngracefulTerminationTask implements Runnable {

    private final MessagingLog mMessagingLog;

    private final FileTransferServiceImpl mFileTransferService;

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    public UpdateFileTransferStateAfterUngracefulTerminationTask(MessagingLog messagingLog,
            FileTransferServiceImpl fileTransferService) {
        mMessagingLog = messagingLog;
        mFileTransferService = fileTransferService;
    }

    public void run() {
        Cursor cursor = null;
        try {
            cursor = mMessagingLog.getInterruptedFileTransfers();
            /* TODO: Handle cursor when null. */
            int chatIdIdx = cursor.getColumnIndex(FileTransferData.KEY_CHAT_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_CONTACT);
            int fileTransferIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FT_ID);
            int uploadIdIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_UPLOAD_TID);
            int downloadUriIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_DOWNLOAD_URI);
            int stateIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_STATE);
            int fileExpirationIdx = cursor
                    .getColumnIndexOrThrow(FileTransferData.KEY_FILE_EXPIRATION);
            int fileSizeIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_FILESIZE);
            int transferredIdx = cursor.getColumnIndexOrThrow(FileTransferData.KEY_TRANSFERRED);
            while (cursor.moveToNext()) {
                String fileTransferId = cursor.getString(fileTransferIdIdx);
                String contactNumber = cursor.getString(contactIdx);
                String chatId = cursor.getString(chatIdIdx);
                ContactId contact = contactNumber != null ? ContactUtil
                        .createContactIdFromTrustedData(contactNumber) : null;
                State state = State.valueOf(cursor.getInt(stateIdx));
                boolean groupFileTransfer = !chatId.equals(contact);
                if (cursor.getString(downloadUriIdx) == null
                        && cursor.getString(uploadIdIdx) == null) {
                    /* Msrp file transfer */
                    switch (state) {
                        case INITIATING:
                            if (groupFileTransfer) {
                                mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                        fileTransferId, chatId, State.FAILED,
                                        ReasonCode.FAILED_INITIATION);
                                break;
                            }
                            mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                    fileTransferId, contact, State.FAILED,
                                    ReasonCode.FAILED_INITIATION);
                            break;
                        case ACCEPTING:
                            if (groupFileTransfer) {
                                mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                        fileTransferId, chatId, State.ABORTED,
                                        ReasonCode.ABORTED_BY_SYSTEM);
                                break;
                            }
                            mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                    fileTransferId, contact, State.ABORTED,
                                    ReasonCode.ABORTED_BY_SYSTEM);
                            break;
                        case STARTED:
                            if (groupFileTransfer) {
                                mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                        fileTransferId, chatId, State.FAILED,
                                        ReasonCode.FAILED_DATA_TRANSFER);
                                break;
                            }
                            mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                    fileTransferId, contact, State.FAILED,
                                    ReasonCode.FAILED_DATA_TRANSFER);
                            break;
                        case INVITED:
                            if (groupFileTransfer) {
                                mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                        fileTransferId, chatId, State.REJECTED,
                                        ReasonCode.REJECTED_BY_SYSTEM);
                                break;
                            }
                            mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                    fileTransferId, contact, State.REJECTED,
                                    ReasonCode.REJECTED_BY_SYSTEM);
                            break;
                    /* TODO: Handle default. */
                    }
                } else {
                    /* Http file transfer */
                    switch (state) {
                        case INITIATING:
                            if (groupFileTransfer) {
                                mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                        fileTransferId, chatId, State.FAILED,
                                        ReasonCode.FAILED_INITIATION);
                                break;
                            }
                            mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                    fileTransferId, contact, State.FAILED,
                                    ReasonCode.FAILED_INITIATION);
                            break;
                        case ACCEPTING:
                            if (groupFileTransfer) {
                                mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                        fileTransferId, chatId, State.ABORTED,
                                        ReasonCode.ABORTED_BY_SYSTEM);
                                break;
                            }
                            mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                    fileTransferId, contact, State.ABORTED,
                                    ReasonCode.ABORTED_BY_SYSTEM);
                            break;
                        case STARTED:
                            if (groupFileTransfer) {
                                if (cursor.getLong(fileSizeIdx) != cursor.getLong(transferredIdx)) {
                                    mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                            fileTransferId, chatId, State.PAUSED,
                                            ReasonCode.PAUSED_BY_SYSTEM);
                                }
                                break;
                            }
                            mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                    fileTransferId, contact, State.PAUSED,
                                    ReasonCode.PAUSED_BY_SYSTEM);
                            break;
                        case INVITED:
                            if (cursor.getLong(fileExpirationIdx) > System.currentTimeMillis()) {
                                /* File is not yet expired on the server */
                                break;
                            }
                            if (groupFileTransfer) {
                                mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                        fileTransferId, chatId, State.REJECTED,
                                        ReasonCode.REJECTED_BY_TIMEOUT);
                                break;
                            }
                            mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                    fileTransferId, contact, State.REJECTED,
                                    ReasonCode.REJECTED_BY_TIMEOUT);
                            break;
                    /* TODO: Handle default. */
                    }
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
