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

import com.gsma.rcs.provider.CursorUtil;
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

    private static final Logger sLogger = Logger
            .getLogger(UpdateFileTransferStateAfterUngracefulTerminationTask.class.getName());

    public UpdateFileTransferStateAfterUngracefulTerminationTask(MessagingLog messagingLog,
            FileTransferServiceImpl fileTransferService) {
        mMessagingLog = messagingLog;
        mFileTransferService = fileTransferService;
    }

    @Override
    public void run() {
        if (sLogger.isActivated()) {
            sLogger.debug("initiating.");
        }
        Cursor cursor = null;
        try {
            cursor = mMessagingLog.getInterruptedFileTransfers();
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
                boolean groupFileTransfer = !chatId.equals(contactNumber);
                if (cursor.getString(downloadUriIdx) == null
                        && cursor.getString(uploadIdIdx) == null) {
                    /* MSRP file transfer */
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
                        default:
                            sLogger.error(getClass().getName() + " unexpected state (" + state
                                    + ") detected! Error in SQL statement?");
                            break;
                    }
                } else {
                    /* HTTP file transfer */
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
                            if (cursor.getLong(fileSizeIdx) == cursor.getLong(transferredIdx)) {
                                break;
                            }
                            if (groupFileTransfer) {
                                mFileTransferService.setGroupFileTransferStateAndReasonCode(
                                        fileTransferId, chatId, State.PAUSED,
                                        ReasonCode.PAUSED_BY_SYSTEM);
                            } else {
                                mFileTransferService.setOneToOneFileTransferStateAndReasonCode(
                                        fileTransferId, contact, State.PAUSED,
                                        ReasonCode.PAUSED_BY_SYSTEM);
                            }
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
                        default:
                            sLogger.error(getClass().getName() + " unexpected state (" + state
                                    + ") detected! Error in SQL statement?");
                            break;
                    }
                }
            }
        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error(
                    "Exception occured while trying to update file transfer state for interrupted file transfers",
                    e);
        } finally {
            CursorUtil.close(cursor);
            if (sLogger.isActivated()) {
                sLogger.debug("done.");
            }
        }
    }
}
