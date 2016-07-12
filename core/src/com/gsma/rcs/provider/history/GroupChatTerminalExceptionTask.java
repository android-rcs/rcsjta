/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 * Copyright (C) 2010-2016 Orange.
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

import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessageData;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.database.Cursor;

public class GroupChatTerminalExceptionTask implements Runnable {

    private final String mChatId;

    private final ChatServiceImpl mChatService;

    private final FileTransferServiceImpl mFileTransferService;

    private final HistoryLog mHistoryLog;

    private static final Logger sLogger = Logger.getLogger(GroupChatTerminalExceptionTask.class
            .getName());

    /* package private */public GroupChatTerminalExceptionTask(String chatId,
            ChatServiceImpl chatService, FileTransferServiceImpl fileTransferService,
            HistoryLog historyLog) {
        mChatId = chatId;
        mChatService = chatService;
        mFileTransferService = fileTransferService;
        mHistoryLog = historyLog;
    }

    @Override
    public void run() {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("Execute task to mark all queued group chat messages and group file transfers as failed with chatId "
                    .concat(mChatId));
        }
        Cursor cursor = null;
        try {
            cursor = mHistoryLog.getQueuedGroupChatMessagesAndGroupFileTransfers(mChatId);
            int providerIdIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_PROVIDER_ID);
            int idIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_ID);
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(HistoryLogData.KEY_MIME_TYPE);
            while (cursor.moveToNext()) {
                int providerId = cursor.getInt(providerIdIdx);
                String id = cursor.getString(idIdx);
                String mimeType = cursor.getString(mimeTypeIdx);
                switch (providerId) {
                    case MessageData.HISTORYLOG_MEMBER_ID:
                        mChatService.setGroupChatMessageStatusAndReasonCode(id, mimeType, mChatId,
                                Status.FAILED, Content.ReasonCode.FAILED_SEND);
                        break;
                    case FileTransferData.HISTORYLOG_MEMBER_ID:
                        mFileTransferService.setGroupFileTransferStateAndReasonCode(id, mChatId,
                                State.FAILED, FileTransfer.ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
                        break;
                    default:
                        throw new IllegalArgumentException("Not expecting to handle provider id '"
                                + providerId + "'!");
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
                    "Exception occurred while trying to mark queued group chat messages and group file transfers as failed with chatId "
                            + mChatId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
