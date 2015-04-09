/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.gsma.rcs.im.chat;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.userprofile.UserProfile;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import java.util.Random;

public class ChatMessageTest extends AndroidTestCase {
    private ContactId mContact;
    private Context mContext;
    private ContentResolver mContentResolver;
    private RcsSettings mRcsSettings;
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
        mContext = getContext();
        mContentResolver = mContext.getContentResolver();
        mLocalContentResolver = new LocalContentResolver(mContentResolver);
        mRcsSettings = RcsSettings.createInstance(mLocalContentResolver);
        mMessagingLog = MessagingLog.createInstance(mContext, mLocalContentResolver, mRcsSettings);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        mContact = contactUtils.formatContact("+339000000");
        ImsModule.IMS_USER_PROFILE = new UserProfile(mContact, "homeDomain", "privateID",
                "password", "realm", "xdmServerAddr", "xdmServerLogin", "xdmServerPassword",
                "imConferenceUri", mRcsSettings);
        mTimestamp = mRandom.nextLong();
        mTimestampSent = mRandom.nextLong();
        mText = Long.toString(mRandom.nextLong());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testTextMessage() {
        String msgId = Long.toString(System.currentTimeMillis());
        ChatMessage msg = new ChatMessage(msgId, mContact, mText, MimeType.TEXT_MESSAGE,
                mTimestamp, mTimestampSent, "display");

        // Add entry
        mMessagingLog.addOutgoingOneToOneChatMessage(msg, Message.Content.Status.SENT,
                Message.Content.ReasonCode.UNSPECIFIED);

        String where = new StringBuilder(Message.MESSAGE_ID).append("=?").toString();
        String[] whereArgs = new String[] {
            msgId
        };
        // Read entry
        Cursor cursor = mContentResolver.query(Message.CONTENT_URI, SELECTION, where, whereArgs,
                Message.TIMESTAMP + " ASC");
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
        mLocalContentResolver.delete(Uri.withAppendedPath(Message.CONTENT_URI, msgId), null, null);
        assertFalse(mMessagingLog.isMessagePersisted(msgId));
    }

    public void testGeolocMessage() {
        Geoloc geoloc = new Geoloc(mText, 10.0, 11.0, 2000, 2);
        ChatMessage chatMsg = ChatUtils.createGeolocMessage(mContact, geoloc, mTimestamp,
                mTimestampSent);
        String msgId = chatMsg.getMessageId();
        // Add entry
        mMessagingLog.addOutgoingOneToOneChatMessage(chatMsg, Message.Content.Status.SENT,
                Message.Content.ReasonCode.UNSPECIFIED);

        // Read entry
        Uri uri = Uri.withAppendedPath(Message.CONTENT_URI, msgId);
        Cursor cursor = mContentResolver.query(uri, SELECTION, null, null, Message.TIMESTAMP
                + " ASC");
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
        mLocalContentResolver.delete(Uri.withAppendedPath(Message.CONTENT_URI, msgId), null, null);
        assertFalse(mMessagingLog.isMessagePersisted(msgId));
    }
}
