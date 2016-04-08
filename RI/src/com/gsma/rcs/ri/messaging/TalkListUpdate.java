/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.ri.messaging;

import com.gsma.rcs.ri.messaging.adapter.TalkListArrayItem;
import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.HistoryUriBuilder;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to update the talk list in background
 *
 * @author Philippe LEMORDANT
 */
public class TalkListUpdate extends AsyncTask<Void, Void, Collection<TalkListArrayItem>> {

    // @formatter:off
    private static final String[] PROJECTION = new String[]{
            HistoryLog.BASECOLUMN_ID,
            HistoryLog.ID,
            HistoryLog.CHAT_ID,
            HistoryLog.PROVIDER_ID,
            HistoryLog.MIME_TYPE,
            HistoryLog.CONTENT,
            HistoryLog.FILENAME,
            HistoryLog.TIMESTAMP,
            HistoryLog.DIRECTION,
            HistoryLog.CONTACT,
            HistoryLog.READ_STATUS,
            HistoryLog.STATUS
    };
    // @formatter:on

    private final TaskCompleted mTaskCompleted;
    private final Context mCtx;

    public TalkListUpdate(Context ctx, TaskCompleted taskCompleted) {
        mCtx = ctx;
        mTaskCompleted = taskCompleted;
    }

    @Override
    protected Collection<TalkListArrayItem> doInBackground(Void... params) {
        /*
         * The MMS sending is performed in background because the API returns a message instance
         * only once it is persisted and to persist MMS, the core stack computes the file icon for
         * image attached files.
         */
        return queryHistoryLogAndRefreshView();
    }

    @Override
    protected void onPostExecute(Collection<TalkListArrayItem> result) {
        if (mTaskCompleted != null) {
            mTaskCompleted.onTaskComplete(result);
        }
    }

    public interface TaskCompleted {
        void onTaskComplete(Collection<TalkListArrayItem> result);
    }

    private boolean isUnread(int providerId, RcsService.Direction dir,
            RcsService.ReadStatus readStatus, int status) {
        switch (providerId) {
            case ChatLog.GroupChat.HISTORYLOG_MEMBER_ID:
                return false;

            case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                return RcsService.Direction.INCOMING == dir
                        && RcsService.ReadStatus.UNREAD == readStatus;

            case FileTransferLog.HISTORYLOG_MEMBER_ID:
                if (RcsService.Direction.INCOMING != dir) {
                    return false;
                }
                FileTransfer.State state = FileTransfer.State.valueOf(status);
                switch (state) {
                    case INVITED:
                    case TRANSFERRED:
                    case ACCEPTING:
                    case PAUSED:
                    case STARTED:
                        return RcsService.ReadStatus.UNREAD == readStatus;
                    default:
                        /*
                         * We consider that if the file transfer is rejected or failed then it
                         * cannot be read but should not be considered as unread.
                         */
                        return false;
                }
        }
        throw new IllegalArgumentException("Invalid provider ID=" + providerId);
    }

    Collection<TalkListArrayItem> queryHistoryLogAndRefreshView() {
        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);
        uriBuilder.appendProvider(ChatLog.GroupChat.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(ChatLog.Message.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(FileTransferLog.HISTORYLOG_MEMBER_ID);
        Uri mUriHistoryProvider = uriBuilder.build();
        /*
        
         */
        Map<String, TalkListArrayItem> threads = new HashMap<>();
        Cursor cursor = null;
        try {
            cursor = mCtx.getContentResolver().query(mUriHistoryProvider, PROJECTION, null, null,
                    null);
            if (cursor == null) {
                throw new SQLException("Cannot query History Log");
            }
            int columnTimestamp = cursor.getColumnIndexOrThrow(HistoryLog.TIMESTAMP);
            int columnProviderId = cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID);
            int columnDirection = cursor.getColumnIndexOrThrow(HistoryLog.DIRECTION);
            int columnChatId = cursor.getColumnIndexOrThrow(HistoryLog.CHAT_ID);
            int columnContact = cursor.getColumnIndexOrThrow(HistoryLog.CONTACT);
            int columnContent = cursor.getColumnIndexOrThrow(HistoryLog.CONTENT);
            int columnMimeType = cursor.getColumnIndexOrThrow(HistoryLog.MIME_TYPE);
            int columnReadStatus = cursor.getColumnIndexOrThrow(HistoryLog.READ_STATUS);
            int columnFilename = cursor.getColumnIndexOrThrow(HistoryLog.FILENAME);
            int columnStatus = cursor.getColumnIndexOrThrow(HistoryLog.STATUS);
            while (cursor.moveToNext()) {
                long timestamp = cursor.getLong(columnTimestamp);
                String chatId = cursor.getString(columnChatId);
                String phoneNumber = cursor.getString(columnContact);
                ContactId contact = null;
                if (phoneNumber != null) {
                    contact = ContactUtil.formatContact(phoneNumber);
                }
                RcsService.Direction dir = RcsService.Direction.valueOf(cursor
                        .getInt(columnDirection));
                RcsService.ReadStatus readStatus = RcsService.ReadStatus.valueOf(cursor
                        .getInt(columnReadStatus));
                TalkListArrayItem item = threads.get(chatId);
                int providerId = cursor.getInt(columnProviderId);
                int status = cursor.getInt(columnStatus);
                boolean unread = isUnread(providerId, dir, readStatus, status);
                int unreadCount = unread ? 1 : 0;
                if (item != null) {
                    /* Is it the newest item ? */
                    if (timestamp < item.getTimestamp()) {
                        /*
                         * it is not the newest item then increment unread count of newest one then
                         * read next
                         */
                        if (unread) {
                            item.incrementUnreadCount();
                        }
                        continue;
                    }
                    unreadCount += item.getUnreadCount();
                }

                String content;
                if (FileTransferLog.HISTORYLOG_MEMBER_ID != providerId) {
                    /* There is not body text message for RCS file transfer */
                    content = cursor.getString(columnContent);
                } else {
                    content = cursor.getString(columnFilename);
                }
                String mimeType = cursor.getString(columnMimeType);

                if (ChatLog.GroupChat.HISTORYLOG_MEMBER_ID == providerId) {
                    if (item != null) {
                        item.setSubject(content);
                    } else {
                        item = new TalkListArrayItem(chatId, contact, timestamp, dir, null,
                                mimeType, unreadCount);
                        item.setSubject(content);
                    }
                } else if (item != null) {
                    String subject = item.getSubject();
                    item = new TalkListArrayItem(chatId, contact, timestamp, dir, content,
                            mimeType, unreadCount);
                    item.setSubject(subject);
                } else {
                    item = new TalkListArrayItem(chatId, contact, timestamp, dir, content,
                            mimeType, unreadCount);
                }
                threads.put(chatId, item);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return threads.values();
    }

}
