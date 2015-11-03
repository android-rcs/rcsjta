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

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessageData;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.history.HistoryUriBuilder;

import android.database.Cursor;
import android.net.Uri;

public class HistoryLog {

    private static volatile HistoryLog sInstance;

    private final LocalContentResolver mLocalContentResolver;

    private static final Uri CHATMESSAGE_AND_FILETRANSFER_CONTENT_URI = new HistoryUriBuilder(
            HistoryLogData.CONTENT_URI).appendProvider(MessageData.HISTORYLOG_MEMBER_ID)
            .appendProvider(FileTransferData.HISTORYLOG_MEMBER_ID).build();

    private static final String SELECTION_QUEUED_CHATMESSAGES_AND_FILETRANSFERS = new StringBuilder(
            "(").append(HistoryLogData.KEY_STATUS).append("=").append(Status.QUEUED.toInt())
            .append(" AND ").append(HistoryLogData.KEY_MIME_TYPE).append("<>'")
            .append(MimeType.GROUPCHAT_EVENT).append("' AND ")
            .append(HistoryLogData.KEY_PROVIDER_ID).append("=")
            .append(MessageData.HISTORYLOG_MEMBER_ID).append(") OR (")
            .append(HistoryLogData.KEY_STATUS).append("=")
            .append(FileTransfer.State.QUEUED.toInt()).append(" AND ")
            .append(HistoryLogData.KEY_PROVIDER_ID).append("=")
            .append(FileTransferData.HISTORYLOG_MEMBER_ID).append(')').toString();

    private static final String SELECTION_UPLOADED_BUT_NOT_TRANSFERRED_FILETRANSFERS = new StringBuilder(
            "(").append(HistoryLogData.KEY_PROVIDER_ID).append("=")
            .append(FileTransferData.HISTORYLOG_MEMBER_ID).append(" AND ")
            .append(HistoryLogData.KEY_STATUS).append("=")
            .append(FileTransfer.State.STARTED.toInt()).append(" AND ")
            .append(HistoryLogData.KEY_DIRECTION).append("=").append(Direction.OUTGOING.toInt())
            .append(" AND ").append(HistoryLogData.KEY_FILESIZE).append("=")
            .append(HistoryLogData.KEY_TRANSFERRED).append(")").toString();

    private static final String SELECTION_QUEUED_GROUPCHATMESSAGES_AND_GROUPFILETRANSFERS = new StringBuilder(
            HistoryLogData.KEY_CHAT_ID).append("=? AND (")
            .append(SELECTION_QUEUED_CHATMESSAGES_AND_FILETRANSFERS).append(" OR ")
            .append(SELECTION_UPLOADED_BUT_NOT_TRANSFERRED_FILETRANSFERS).append(')').toString();

    private static final String SELECTION_QUEUED_ONETOONECHATMESSAGES_AND_ONETOONE_FILETRANSFERS = new StringBuilder(
            HistoryLogData.KEY_CHAT_ID).append("=").append(HistoryLogData.KEY_CONTACT)
            .append(" AND (").append(SELECTION_QUEUED_CHATMESSAGES_AND_FILETRANSFERS)
            .append(" OR ").append(SELECTION_UPLOADED_BUT_NOT_TRANSFERRED_FILETRANSFERS)
            .append(')').toString();

    private static final String SELECTION_ID = new StringBuilder(HistoryLogData.KEY_ID)
            .append("=?").toString();

    private static final String[] PROJECTION_REMOTE_CONTACT = new String[] {
        HistoryLogData.KEY_CONTACT
    };

    private static final int FIRST_COLUMN_IDX = 0;

    private static final String ORDER_BY_TIMESTAMP_ASC = HistoryLogData.KEY_TIMESTAMP
            .concat(" ASC");

    private HistoryLog(LocalContentResolver localContentResolver) {
        mLocalContentResolver = localContentResolver;
    }

    /**
     * Create instance
     * 
     * @param localContentResolver Local content resolver
     * @return HistoryLog instance
     */
    public static HistoryLog createInstance(LocalContentResolver localContentResolver) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (HistoryLog.class) {
            if (sInstance == null) {
                sInstance = new HistoryLog(localContentResolver);
            }
            return sInstance;
        }
    }

    public Cursor getQueuedGroupChatMessagesAndGroupFileTransfers(String chatId) {
        String[] selectionArgs = new String[] {
            chatId
        };
        Cursor cursor = mLocalContentResolver.query(CHATMESSAGE_AND_FILETRANSFER_CONTENT_URI, null,
                SELECTION_QUEUED_GROUPCHATMESSAGES_AND_GROUPFILETRANSFERS, selectionArgs,
                ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, CHATMESSAGE_AND_FILETRANSFER_CONTENT_URI);
        return cursor;
    }

    public Cursor getQueuedOneToOneChatMessagesAndOneToOneFileTransfers() {
        Cursor cursor = mLocalContentResolver.query(CHATMESSAGE_AND_FILETRANSFER_CONTENT_URI, null,
                SELECTION_QUEUED_ONETOONECHATMESSAGES_AND_ONETOONE_FILETRANSFERS, null,
                ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, CHATMESSAGE_AND_FILETRANSFER_CONTENT_URI);
        return cursor;
    }

    /**
     * Get remote contact corresponding to the unique ID of message/ FT entry
     * 
     * @param id Unique ID of message/ FT entry
     * @return ContactId
     */
    public ContactId getRemoteContact(String id) {
        String[] selectionArgs = new String[] {
            id
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CHATMESSAGE_AND_FILETRANSFER_CONTENT_URI,
                    PROJECTION_REMOTE_CONTACT, SELECTION_ID, selectionArgs, null);
            if (!cursor.moveToNext()) {
                return null;
            }
            if (cursor.isNull(FIRST_COLUMN_IDX)) {
                return null;
            }
            return ContactUtil.createContactIdFromTrustedData(cursor.getString(FIRST_COLUMN_IDX));

        } finally {
            CursorUtil.close(cursor);
        }
    }
}
