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
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;
import com.gsma.services.rcs.filetransfer.FileTransferLog;

import android.database.Cursor;

/*package private*/class OneToOneChatTerminalExceptionTask implements Runnable {

    private final ContactId mContact;

    private final ChatServiceImpl mChatService;

    private final FileTransferServiceImpl mFileTransferService;

    private final MessagingLog mMessagingLog;

    private final Object mLock;

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    /* package private */public OneToOneChatTerminalExceptionTask(ContactId contact,
            ChatServiceImpl chatService, FileTransferServiceImpl fileTransferService,
            MessagingLog messagingLog, Object lock) {
        mContact = contact;
        mChatService = chatService;
        mFileTransferService = fileTransferService;
        mMessagingLog = messagingLog;
        mLock = lock;
    }

    public void run() {
        synchronized (mLock) {
            Cursor messageCursor = null;
            Cursor fileCursor = null;
            try {
                messageCursor = mMessagingLog.getQueuedOneToOneChatMessages(mContact);
                int msgIdIdx = messageCursor.getColumnIndexOrThrow(Message.MESSAGE_ID);
                int mimeTypeIdx = messageCursor.getColumnIndexOrThrow(Message.MIME_TYPE);
                while (messageCursor.moveToNext()) {
                    String msgId = messageCursor.getString(msgIdIdx);
                    String mimeType = messageCursor.getString(mimeTypeIdx);
                    mChatService.setOneToOneChatMessageStatusAndReasonCode(msgId, mimeType,
                            mContact, Status.FAILED, Content.ReasonCode.FAILED_SEND);
                }
                fileCursor = mMessagingLog.getQueuedOneToOneFileTransfers(mContact);
                int fileTransferIdIdx = fileCursor.getColumnIndexOrThrow(FileTransferLog.FT_ID);
                while (fileCursor.moveToNext()) {
                    String fileTransferId = fileCursor.getString(fileTransferIdIdx);
                    mFileTransferService.setOneToOneFileTransferStateAndReasonCode(fileTransferId,
                            mContact, State.FAILED, ReasonCode.FAILED_NOT_ALLOWED_TO_SEND);
                }
            } catch (Exception e) {
                /*
                 * Exception will be handled better in CR037.
                 */
                if (mLogger.isActivated()) {
                    mLogger.error(
                            "Exception occured while trying to mark queued one-one chat messages and one-one file transfers as failed.",
                            e);
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
}
