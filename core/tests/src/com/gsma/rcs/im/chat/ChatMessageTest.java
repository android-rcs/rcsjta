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

import static com.gsma.rcs.utils.PhoneUtils.SIP_URI_HEADER;
import static com.gsma.rcs.utils.PhoneUtils.initialize;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.userprofile.UserProfile;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.messaging.MessageData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import java.io.IOException;
import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ChatMessageTest extends AndroidTestCase {
    private ContactId mContact;
    private ContentResolver mContentResolver;
    private Random mRandom = new Random();
    private long mTimestamp;
    private long mTimestampSent;
    private String mText;
    private MessagingLog mMessagingLog;
    private LocalContentResolver mLocalContentResolver;

    private static final String[] SELECTION = new String[] {
            Message.DIRECTION, Message.CONTACT, Message.CONTENT, Message.MIME_TYPE,
            Message.MESSAGE_ID, Message.TIMESTAMP, Message.TIMESTAMP_SENT
    };

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        mContentResolver = context.getContentResolver();
        mLocalContentResolver = new LocalContentResolver(mContentResolver);
        RcsSettings rcsSettings = RcsSettings.getInstance(mLocalContentResolver);
        initialize(rcsSettings);
        mMessagingLog = MessagingLog.getInstance(mLocalContentResolver, rcsSettings);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(context));
        mContact = contactUtils.formatContact("+339000000");
        ImsModule.setImsUserProfile(new UserProfile(mContact, "homeDomain", "privateID",
                "password", "realm", Uri.parse("xdmServerAddr"), "xdmServerLogin",
                "xdmServerPassword", formatSipUri("imConferenceUri"), rcsSettings));
        mTimestamp = mRandom.nextLong();
        mTimestampSent = mRandom.nextLong();
        mText = Long.toString(mRandom.nextLong());
    }

    public void testTextMessage() throws PayloadException, IOException, SQLDataException {
        String msgId = Long.toString(System.currentTimeMillis());
        ChatMessage msg = new ChatMessage(msgId, mContact, mText, MimeType.TEXT_MESSAGE,
                mTimestamp, mTimestampSent, "display");

        mMessagingLog.addOutgoingOneToOneChatMessage(msg, Message.Content.Status.SENT,
                Message.Content.ReasonCode.UNSPECIFIED, 0);

        String where = Message.MESSAGE_ID + "=?";
        String[] whereArgs = new String[] {
            msgId
        };
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(Message.CONTENT_URI, SELECTION, where, whereArgs,
                    Message.TIMESTAMP + " ASC");
            if (cursor == null) {
                throw new SQLDataException("Cannot query uri:" + Message.CONTENT_URI);
            }
            assertEquals(cursor.getCount(), 1);
            assertTrue(cursor.moveToNext());
            Direction direction = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndex(Message.DIRECTION)));
            String contact = cursor.getString(cursor.getColumnIndex(Message.CONTACT));
            String content = cursor.getString(cursor.getColumnIndex(Message.CONTENT));
            assertNotNull(content);
            String mimeType = cursor.getString(cursor.getColumnIndex(Message.MIME_TYPE));
            String id = cursor.getString(cursor.getColumnIndex(Message.MESSAGE_ID));
            long readTimestamp = cursor.getLong(cursor.getColumnIndex(Message.TIMESTAMP));
            long readTimestampSent = cursor.getLong(cursor.getColumnIndex(Message.TIMESTAMP_SENT));

            assertEquals(Direction.OUTGOING, direction);
            assertEquals(mContact.toString(), contact);
            assertEquals(mText, content);
            assertEquals(Message.MimeType.TEXT_MESSAGE, mimeType);
            assertEquals(msgId, id);
            assertEquals(mTimestamp, readTimestamp);
            assertEquals(mTimestampSent, readTimestampSent);
            mLocalContentResolver.delete(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                    null, null);
            assertFalse(mMessagingLog.isMessagePersisted(msgId));

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testGeolocMessage() throws PayloadException, IOException, SQLDataException {
        Geoloc geoloc = new Geoloc(mText, 10.0, 11.0, 2000, 2);
        ChatMessage chatMsg = ChatUtils.createGeolocMessage(mContact, geoloc, mTimestamp,
                mTimestampSent);
        String msgId = chatMsg.getMessageId();
        // Add entry
        mMessagingLog.addOutgoingOneToOneChatMessage(chatMsg, Message.Content.Status.SENT,
                Message.Content.ReasonCode.UNSPECIFIED, 0);

        // Read entry
        Uri uri = Uri.withAppendedPath(Message.CONTENT_URI, msgId);
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, SELECTION, null, null, Message.TIMESTAMP + " ASC");
            if (cursor == null) {
                throw new SQLDataException("Cannot query Selection:" + Arrays.toString(SELECTION));
            }

            assertEquals(cursor.getCount(), 1);
            assertTrue(cursor.moveToNext());
            Direction direction = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndex(Message.DIRECTION)));
            String contact = cursor.getString(cursor.getColumnIndex(Message.CONTACT));
            String content = cursor.getString(cursor.getColumnIndex(Message.CONTENT));
            assertNotNull(content);
            Geoloc readGeoloc = new Geoloc(content);
            assertNotNull(readGeoloc);
            String contentType = cursor.getString(cursor.getColumnIndex(Message.MIME_TYPE));
            String id = cursor.getString(cursor.getColumnIndex(Message.MESSAGE_ID));
            long readTimestamp = cursor.getLong(cursor.getColumnIndex(Message.TIMESTAMP));
            long readTimestampSent = cursor.getLong(cursor.getColumnIndex(Message.TIMESTAMP_SENT));

            assertEquals(Direction.OUTGOING, direction);
            assertEquals(mContact.toString(), contact);
            assertEquals(readGeoloc.getLabel(), geoloc.getLabel());
            assertEquals(readGeoloc.getLatitude(), geoloc.getLatitude());
            assertEquals(readGeoloc.getLongitude(), geoloc.getLongitude());
            assertEquals(readGeoloc.getExpiration(), geoloc.getExpiration());
            assertEquals(readGeoloc.getAccuracy(), geoloc.getAccuracy());
            assertEquals(Message.MimeType.GEOLOC_MESSAGE, contentType);
            assertEquals(msgId, id);
            assertEquals(mTimestamp, readTimestamp);
            assertEquals(mTimestampSent, readTimestampSent);
            mLocalContentResolver.delete(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                    null, null);
            assertFalse(mMessagingLog.isMessagePersisted(msgId));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Format to SIP-URI
     * 
     * @param path Sip Uri path
     * @return SIP-URI
     */
    private Uri formatSipUri(String path) {
        return path.startsWith(SIP_URI_HEADER) ? Uri.parse(path) : Uri.parse(SIP_URI_HEADER + path);
    }

    public void testChatMessageDeliveryExpiration() throws PayloadException {
        String msgId = Long.toString(mRandom.nextLong());
        ChatMessage msg = new ChatMessage(msgId, mContact, mText, MimeType.TEXT_MESSAGE,
                mTimestamp, mTimestampSent, "display");
        mMessagingLog.addOutgoingOneToOneChatMessage(msg, Status.SENDING, ReasonCode.UNSPECIFIED,
                System.currentTimeMillis() + 30000L);
        assertFalse(mMessagingLog.isChatMessageExpiredDelivery(msgId));
        mMessagingLog.setChatMessageDeliveryExpired(msgId);
        assertTrue(mMessagingLog.isChatMessageExpiredDelivery(msgId));
    }

    private void verifyMessageLogEntries(Cursor cursor, List<String> msgIds) {
        if (!cursor.moveToFirst()) {
            fail("Cursor should not be empty!");
        }
        int msgIdIdx = cursor.getColumnIndexOrThrow(Message.MESSAGE_ID);
        while (cursor.moveToNext()) {
            String msgId = cursor.getString(msgIdIdx);
            assertTrue(msgIds.contains(msgId));
        }
    }

    public void testClearChatMessageDeliveryExpiration() throws PayloadException {
        ArrayList<String> msgIds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            msgIds.add(Long.toString(mRandom.nextLong()));
        }
        for (String msgId : msgIds) {
            ChatMessage msg = new ChatMessage(msgId, mContact, mText, MimeType.TEXT_MESSAGE,
                    mTimestamp, mTimestampSent, "display");
            mMessagingLog.addOutgoingOneToOneChatMessage(msg, Status.SENDING,
                    ReasonCode.UNSPECIFIED, System.currentTimeMillis() + 30000L);
        }
        Cursor cursor = mMessagingLog.getUndeliveredOneToOneChatMessages();
        assertEquals(4, cursor.getCount());
        verifyMessageLogEntries(cursor, msgIds);
        CursorUtil.close(cursor);
        mMessagingLog.clearMessageDeliveryExpiration(msgIds);
        cursor = mMessagingLog.getUndeliveredOneToOneChatMessages();
        assertEquals(0, cursor.getCount());
        CursorUtil.close(cursor);
    }

    public void testMarkMessageAsRead() {
        String msgId = Long.toString(System.currentTimeMillis());
        assertEquals(null, mMessagingLog.isMessageRead(msgId));
        assertEquals(0, mMessagingLog.markMessageAsRead(msgId));
        ChatMessage msg = new ChatMessage(msgId, mContact, mText, MimeType.TEXT_MESSAGE,
                mTimestamp, mTimestampSent, "display");
        mMessagingLog.addIncomingOneToOneChatMessage(msg, true);
        assertFalse(mMessagingLog.isMessageRead(msgId));
        int count = mMessagingLog.markMessageAsRead(msgId);
        assertEquals(1, count);
        assertTrue(mMessagingLog.isMessageRead(msgId));
        count = mMessagingLog.markMessageAsRead(msgId);
        assertEquals(0, count);
        mLocalContentResolver.delete(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId), null,
                null);
        assertFalse(mMessagingLog.isMessagePersisted(msgId));
    }
}
