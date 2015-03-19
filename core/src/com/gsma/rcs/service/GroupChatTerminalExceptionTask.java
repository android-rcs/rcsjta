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

import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;
import com.gsma.services.rcs.filetransfer.FileTransferLog;

import android.database.Cursor;

public class GroupChatTerminalExceptionTask implements Runnable {

    private final String mChatId;

    private final ChatServiceImpl mChatService;

    private final FileTransferServiceImpl mFileTransferService;

    private final MessagingLog mMessagingLog;

    private final Object mLock;

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    /* package private */public GroupChatTerminalExceptionTask(String chatId,
            ChatServiceImpl chatService, FileTransferServiceImpl fileTransferService,
            MessagingLog messagingLog, Object lock) {
        mChatId = chatId;
        mChatService = chatService;
        mFileTransferService = fileTransferService;
        mMessagingLog = messagingLog;
        mLock = lock;
    }

    public void run() {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Execute task to mark all queued group chat messages and group file transfers as failed with chatId "
                    .concat(mChatId));
        }
        Cursor messageCursor = null;
        Cursor fileCursor = null;
        try {
            synchronized (mLock) {
                messageCursor = mMessagingLog.getQueuedGroupChatMessages(mChatId);
                int msgIdIdx = messageCursor.getColumnIndexOrThrow(Message.MESSAGE_ID);
                int mimeTypeIdx = messageCursor.getColumnIndexOrThrow(Message.MIME_TYPE);
                while (messageCursor.moveToNext()) {
                    String msgId = messageCursor.getString(msgIdIdx);
                    String mimeType = messageCursor.getString(mimeTypeIdx);
                    mChatService.setGroupChatMessageStatusAndReasonCode(msgId, mimeType, mChatId,
                            Status.FAILED, Content.ReasonCode.FAILED_SEND);
                }
                fileCursor = mMessagingLog.getQueuedGroupFileTransfers(mChatId);
                int fileTransferIdIdx = fileCursor.getColumnIndexOrThrow(FileTransferLog.FT_ID);
                while (fileCursor.moveToNext()) {
                    String fileTransferId = fileCursor.getString(fileTransferIdIdx);
                    mFileTransferService.setGroupFileTransferStateAndReasonCode(fileTransferId,
                            mChatId, State.FAILED,
                            FileTransfer.ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
                }
            }
        } catch (Exception e) {
            /*
             * Exception will be handled better in CR037.
             */
            if (logActivated) {
                mLogger.error(
                        "Exception occured while trying to mark queued group chat messages and group file transfers as failed with chatId "
                                .concat(mChatId), e);
            }
        } finally {
            if (messageCursor != null) {
                messageCursor.close();
            }
            if (fileCursor != null) {
                fileCursor.close();
            }
        }
    }
}
