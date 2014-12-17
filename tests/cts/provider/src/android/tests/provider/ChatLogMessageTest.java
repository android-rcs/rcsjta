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
package android.tests.provider;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.InstrumentationTestCase;

import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.chat.ChatLog;

public class ChatLogMessageTest extends InstrumentationTestCase {

	private static final String[] CHAT_LOG_MESSAGE_PROJECTION = new String[] { ChatLog.Message.CHAT_ID,
			ChatLog.Message.CONTENT, ChatLog.Message.CONTACT, ChatLog.Message.DIRECTION, ChatLog.Message.READ_STATUS,
			ChatLog.Message.MESSAGE_ID, ChatLog.Message.MIME_TYPE, ChatLog.Message.TIMESTAMP,
			ChatLog.Message.REASON_CODE, ChatLog.Message.STATUS, ChatLog.Message.TIMESTAMP_SENT,
			ChatLog.Message.TIMESTAMP_DELIVERED, ChatLog.Message.TIMESTAMP_DISPLAYED };

	private ContentProviderClient mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mProvider = getInstrumentation().getTargetContext().getContentResolver()
				.acquireContentProviderClient(ChatLog.Message.CONTENT_URI);
		assertNotNull(mProvider);
	}

	/**
	 * Test the ChatLog.Message according to GSMA API specifications.<br>
	 * Check the following operations:
	 * <ul>
	 * <li>query
	 * <li>insert
	 * <li>delete
	 * <li>update
	 */

	public void testChatLogMessageQuery() {
		// Check that provider handles columns names and query operation with where arguments
		Cursor cursor = null;
		try {
			String where = ChatLog.Message.CHAT_ID.concat("=?");
			String[] whereArgs = new String[] { "123456789" };
			cursor = mProvider.query(ChatLog.Message.CONTENT_URI, CHAT_LOG_MESSAGE_PROJECTION, where, whereArgs, null);
			assertNotNull(cursor);
		} catch (Exception e) {
			fail("query of ChatLog.Message failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testChatLogMessageQueryById() {
		// Check that provider handles columns names and query operation by ID
		Uri uri = Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, "123456789");
		// Check that provider handles columns names and query operation
		Cursor cursor = null;
		try {
			cursor = mProvider.query(uri, CHAT_LOG_MESSAGE_PROJECTION, null, null, null);
			assertNotNull(cursor);
		} catch (Exception e) {
			fail("By Id query of ChatLog.Message failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testChatLogMessageQueryWithoutWhereClause() {
		// Check that provider handles columns names and query operation without where clause
		Cursor cursor = null;
		try {
			cursor = mProvider.query(ChatLog.Message.CONTENT_URI, null, null, null, null);
			assertNotNull(cursor);
			if (cursor.moveToFirst()) {
				Utils.checkProjection(CHAT_LOG_MESSAGE_PROJECTION, cursor.getColumnNames());
			}
		} catch (Exception e) {
			fail("Without where clause query of ChatLog.Message failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testChatLogMessageInsert() {
		// Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(ChatLog.Message.CHAT_ID, "0123456789");
		values.put(ChatLog.Message.CONTENT, "body");
		values.put(ChatLog.Message.CONTACT, "+3360102030405");
		values.put(ChatLog.Message.DIRECTION, RcsCommon.Direction.INCOMING);
		values.put(ChatLog.Message.READ_STATUS, RcsCommon.ReadStatus.UNREAD);
		values.put(ChatLog.Message.MESSAGE_ID, "012345789");
		values.put(ChatLog.Message.MIME_TYPE, ChatLog.Message.MimeType.TEXT_MESSAGE);
		values.put(ChatLog.Message.STATUS, ChatLog.Message.Status.Content.RECEIVED);
		values.put(ChatLog.Message.REASON_CODE, ChatLog.Message.ReasonCode.UNSPECIFIED);
		values.put(ChatLog.Message.TIMESTAMP, System.currentTimeMillis());
		values.put(ChatLog.Message.TIMESTAMP_DELIVERED, 0);
		values.put(ChatLog.Message.TIMESTAMP_DISPLAYED, 0);
		values.put(ChatLog.Message.TIMESTAMP_SENT, 0);
		try {
			mProvider.insert(ChatLog.Message.CONTENT_URI, values);
			fail("ChatLog is read only");
		} catch (Exception ex) {
			assertTrue("insert into ChatLog.Message should be forbidden", ex instanceof RuntimeException);
		}
	}

	public void testChatLogMessageDelete() {
		// Check that provider supports delete operation
		try {
			mProvider.delete(ChatLog.Message.CONTENT_URI, null, null);
			fail("ChatLog is read only");
		} catch (Exception ex) {
			assertTrue("delete of ChatLog.Message should be forbidden", ex instanceof RuntimeException);
		}
	}

	public void testChatLogMessageUpdate() {
		ContentValues values = new ContentValues();
		values.put(ChatLog.Message.TIMESTAMP, System.currentTimeMillis());
		// Check that provider does not support update operation
		try {
			String where = ChatLog.Message.MESSAGE_ID.concat("=?");
			String[] whereArgs = new String[] { "123456789" };
			mProvider.update(ChatLog.Message.CONTENT_URI, values, where, whereArgs);
			fail("ChatLog is read only");
		} catch (Exception ex) {
			assertTrue("Updating a ChatLog.Message should be forbidden", ex instanceof RuntimeException);
		}
	}

}
