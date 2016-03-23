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

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.HistoryUriBuilder;

import com.gsma.rcs.ri.messaging.adapter.TalkListArrayItem;
import com.gsma.rcs.ri.utils.ContactUtil;

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
            HistoryLog.READ_STATUS
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

    Collection<TalkListArrayItem> queryHistoryLogAndRefreshView() {
        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);
        uriBuilder.appendProvider(ChatLog.GroupChat.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(ChatLog.Message.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(FileTransferLog.HISTORYLOG_MEMBER_ID);
        Uri mUriHistoryProvider = uriBuilder.build();
        Map<String, TalkListArrayItem> dataMap = new HashMap<>();
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
                TalkListArrayItem item = dataMap.get(chatId);
                int providerId = cursor.getInt(columnProviderId);
                boolean unread = ChatLog.GroupChat.HISTORYLOG_MEMBER_ID != providerId
                        && RcsService.Direction.INCOMING == dir
                        && RcsService.ReadStatus.UNREAD == readStatus;
                int unreadCount = unread ? 1 : 0;
                if (item != null) {
                    if (timestamp < item.getTimestamp()) {
                        if (RcsService.Direction.INCOMING == dir
                                && RcsService.ReadStatus.UNREAD == readStatus) {
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
                dataMap.put(chatId, item);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return dataMap.values();
    }

}
