/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2016 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.im.chat;

import com.gsma.rcs.RcsSettingsMock;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.messaging.MessageData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.test.AndroidTestCase;

import java.util.Map;
import java.util.Random;

public class MessageLogTest extends AndroidTestCase {

    private ContactId mContact1;
    private ContactId mContact2;
    private ContentResolver mContentResolver;
    private LocalContentResolver mLocalContentResolver;
    private MessagingLog mMessagingLog;
    private String mChatId;
    private long mTimestamp;
    private Random mRandom = new Random();

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        mContentResolver = context.getContentResolver();
        mLocalContentResolver = new LocalContentResolver(mContentResolver);
        RcsSettings rcsSettings = RcsSettingsMock.getMockSettings(context);
        mMessagingLog = MessagingLog.getInstance(mLocalContentResolver, rcsSettings);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        mContact1 = contactUtils.formatContact("+339000000");
        mContact2 = contactUtils.formatContact("+339000001");
        mChatId = String.valueOf(mRandom.nextLong());
        mTimestamp = Math.abs(mRandom.nextLong());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mMessagingLog.deleteAllEntries();
        RcsSettingsMock.restoreSettings();
    }

    public void testAddGroupChatEvent() {
        // Add entry
        String messageId = mMessagingLog.addGroupChatEvent(mChatId, mContact1,
                GroupChatEvent.Status.DEPARTED, mTimestamp);
        // Read entry
        Uri uri = Uri.withAppendedPath(MessageData.CONTENT_URI, messageId);
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, null, null, null, null);
            // Check entry
            if (cursor == null) {
                throw new SQLException("Can not query uri" + uri);
            }
            assertEquals(cursor.getCount(), 1);
            assertTrue(cursor.moveToFirst());
            String id = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.MESSAGE_ID));
            String chatId = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CHAT_ID));
            String contact = cursor
                    .getString(cursor.getColumnIndexOrThrow(ChatLog.Message.CONTACT));
            String mimeType = cursor.getString(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.MIME_TYPE));
            int direction = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.Message.DIRECTION));
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.Message.STATUS));
            int reason = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.Message.REASON_CODE));
            long timestamp = cursor
                    .getLong(cursor.getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP));
            long timestampSent = cursor.getLong(cursor
                    .getColumnIndexOrThrow(ChatLog.Message.TIMESTAMP_SENT));

            assertEquals(messageId, id);
            assertEquals(mContact1.toString(), contact);
            assertEquals(mChatId, chatId);
            assertEquals(Message.MimeType.GROUPCHAT_EVENT, mimeType);
            assertEquals(Direction.IRRELEVANT.toInt(), direction);
            assertEquals(Message.GroupChatEvent.Status.DEPARTED.toInt(), status);
            assertEquals(ReasonCode.UNSPECIFIED.toInt(), reason);
            assertEquals(mTimestamp, timestamp);
            assertEquals(mTimestamp, timestampSent);

            mLocalContentResolver.delete(uri, null, null);
            assertEquals(false, mMessagingLog.isMessagePersisted(messageId));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    public void testGetGroupChatEventsSimpleCase() {
        String id1 = mMessagingLog.addGroupChatEvent(mChatId, mContact1,
                GroupChatEvent.Status.DEPARTED, mTimestamp);
        String id2 = mMessagingLog.addGroupChatEvent(mChatId, mContact2,
                GroupChatEvent.Status.JOINED, mTimestamp);
        Map<ContactId, GroupChatEvent.Status> groupChatEvents = mMessagingLog
                .getGroupChatEvents(mChatId);
        assertNotNull(groupChatEvents);
        assertTrue(!groupChatEvents.isEmpty());
        assertTrue(groupChatEvents.containsKey(mContact1));
        assertTrue(groupChatEvents.containsKey(mContact2));
        assertEquals(GroupChatEvent.Status.DEPARTED, groupChatEvents.get(mContact1));
        assertEquals(GroupChatEvent.Status.JOINED, groupChatEvents.get(mContact2));

        Uri uri = Uri.withAppendedPath(MessageData.CONTENT_URI, id1);
        mLocalContentResolver.delete(uri, null, null);
        assertEquals(false, mMessagingLog.isMessagePersisted(id1));
        uri = Uri.withAppendedPath(MessageData.CONTENT_URI, id2);
        mLocalContentResolver.delete(uri, null, null);
        assertEquals(false, mMessagingLog.isMessagePersisted(id2));
    }

    public void testGetGroupChatEventsMultipleEntriesForSameContact() {
        mMessagingLog.addGroupChatEvent(mChatId, mContact1, GroupChatEvent.Status.DEPARTED,
                mTimestamp);
        mMessagingLog.addGroupChatEvent(mChatId, mContact2, GroupChatEvent.Status.JOINED,
                mTimestamp);
        mMessagingLog.addGroupChatEvent(mChatId, mContact1, GroupChatEvent.Status.JOINED,
                mTimestamp + 1);
        mMessagingLog.addGroupChatEvent(mChatId, mContact2, GroupChatEvent.Status.DEPARTED,
                mTimestamp + 1);
        Map<ContactId, GroupChatEvent.Status> groupChatEvents = mMessagingLog
                .getGroupChatEvents(mChatId);
        assertNotNull(groupChatEvents);
        assertTrue(!groupChatEvents.isEmpty());
        assertTrue(groupChatEvents.containsKey(mContact1));
        assertTrue(groupChatEvents.containsKey(mContact2));
        assertEquals(GroupChatEvent.Status.JOINED, groupChatEvents.get(mContact1));
        assertEquals(GroupChatEvent.Status.DEPARTED, groupChatEvents.get(mContact2));
    }
}
