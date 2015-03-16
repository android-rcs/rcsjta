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

package com.gsma.rcs.provider.history;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.history.HistoryUriBuilder;

import android.database.Cursor;
import android.net.Uri;

public class HistoryLog {

    private static HistoryLog sHistoryLog;

    private final LocalContentResolver mLocalContentResolver;

    private static final Uri CHATMESSAGE_AND_FILETRANSFER_CONTENT_URI = new HistoryUriBuilder(
            com.gsma.services.rcs.history.HistoryLog.CONTENT_URI)
            .appendProvider(Message.HISTORYLOG_MEMBER_ID)
            .appendProvider(FileTransferData.HISTORYLOG_MEMBER_ID).build();

    private static final String SELECTION_QUEUED_CHATMESSAGES_AND_FILETRANSFERS = new StringBuilder(
            "(").append(HistoryLogData.KEY_STATUS).append("=").append(Status.QUEUED.toInt())
            .append(" AND ").append(HistoryLogData.KEY_PROVIDER_ID).append("=")
            .append(Message.HISTORYLOG_MEMBER_ID).append(") OR (")
            .append(HistoryLogData.KEY_STATUS).append("=")
            .append(FileTransfer.State.QUEUED.toInt()).append(" AND ")
            .append(HistoryLogData.KEY_PROVIDER_ID).append("=")
            .append(FileTransferData.HISTORYLOG_MEMBER_ID).append(")").toString();

    private static final String SELECTION_QUEUED_GROUPCHATMESSAGES_AND_GROUPFILETRANSFERS = new StringBuilder(
            HistoryLogData.KEY_CHAT_ID).append("=? AND (")
            .append(SELECTION_QUEUED_CHATMESSAGES_AND_FILETRANSFERS).append(")").toString();

    private static final String SELECTION_QUEUED_ONETOONECHATMESSAGES_AND_ONETOONE_FILETRANSFERS = new StringBuilder(
            HistoryLogData.KEY_CHAT_ID).append("=").append(HistoryLogData.KEY_CONTACT)
            .append(" AND (").append(SELECTION_QUEUED_CHATMESSAGES_AND_FILETRANSFERS).append(")")
            .toString();

    private static final String ORDER_BY_TIMESTAMP_ASC = HistoryLogData.KEY_TIMESTAMP
            .concat(" ASC");

    private HistoryLog(LocalContentResolver localContentResolver) {
        mLocalContentResolver = localContentResolver;
    }

    /**
     * Create instance
     * 
     * @param localContentResolver Local content resolver
     */
    public static synchronized void createInstance(LocalContentResolver localContentResolver) {
        if (sHistoryLog == null) {
            sHistoryLog = new HistoryLog(localContentResolver);
        }
    }

    /**
     * Returns instance
     * 
     * @return Instance
     */
    public static HistoryLog getInstance() {
        return sHistoryLog;
    }

    public Cursor getQueuedGroupChatMessagesAndGroupFileTransfers(String chatId) {
        String[] selectionArgs = new String[] {
            chatId
        };
        return mLocalContentResolver.query(CHATMESSAGE_AND_FILETRANSFER_CONTENT_URI, null,
                SELECTION_QUEUED_GROUPCHATMESSAGES_AND_GROUPFILETRANSFERS, selectionArgs,
                ORDER_BY_TIMESTAMP_ASC);
    }

    public Cursor getQueuedOneToOneChatMessagesAndOneToOneFileTransfers() {
        return mLocalContentResolver.query(CHATMESSAGE_AND_FILETRANSFER_CONTENT_URI, null,
                SELECTION_QUEUED_ONETOONECHATMESSAGES_AND_ONETOONE_FILETRANSFERS, null,
                ORDER_BY_TIMESTAMP_ASC);
    }
}
