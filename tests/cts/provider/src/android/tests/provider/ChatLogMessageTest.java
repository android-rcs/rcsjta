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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.test.InstrumentationTestCase;

import com.gsma.services.rcs.chat.ChatLog;

public class ChatLogMessageTest extends InstrumentationTestCase {
	private ContentResolver mContentResolver;
	private ContentProviderClient mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mContentResolver = getInstrumentation().getTargetContext().getContentResolver();
		mProvider = mContentResolver.acquireContentProviderClient(ChatLog.Message.CONTENT_URI);
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
	public void testChatLogMessage() {
		final String[] CHAT_LOG_MESSAGE_PROJECTION = new String[] { ChatLog.Message.ID, ChatLog.Message.BODY,
				ChatLog.Message.CHAT_ID, ChatLog.Message.CONTACT_NUMBER, ChatLog.Message.DIRECTION, ChatLog.Message.MESSAGE_STATUS,
				ChatLog.Message.MESSAGE_ID, ChatLog.Message.MESSAGE_TYPE, ChatLog.Message.MIME_TYPE, ChatLog.Message.TIMESTAMP // ,
		// ChatLog.Message.TIMESTAMP_DELIVERED, TODO
		// ChatLog.Message.TIMESTAMP_DISPLAYED,
		// ChatLog.Message.TIMESTAMP_SENT
		};

		// Check that provider handles columns names and query operation
		Cursor c = null;
		try {
			String mSelectionClause = ChatLog.Message.ID + " = ?";
			c = mProvider.query(ChatLog.Message.CONTENT_URI, CHAT_LOG_MESSAGE_PROJECTION, mSelectionClause, null, null);
			assertNotNull(c);
			if (c != null) {
				int num = c.getColumnCount();
				assertTrue(num == CHAT_LOG_MESSAGE_PROJECTION.length);
			}
		} catch (Exception e) {
			fail("query of ChatLog.Message failed " + e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}

		// Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(ChatLog.Message.BODY, "body");
		values.put(ChatLog.Message.CHAT_ID, "chat_id");
		values.put(ChatLog.Message.CONTACT_NUMBER, "+3360102030405");
		values.put(ChatLog.Message.DIRECTION, ChatLog.Message.Direction.INCOMING);
		values.put(ChatLog.Message.MESSAGE_STATUS, ChatLog.Message.Status.Content.UNREAD);
		values.put(ChatLog.Message.MESSAGE_ID, "msg_id");
		values.put(ChatLog.Message.MESSAGE_TYPE, ChatLog.Message.Type.CONTENT);
		values.put(ChatLog.Message.MIME_TYPE, "text/plain");
		values.put(ChatLog.Message.TIMESTAMP, System.currentTimeMillis());
		Throwable exception = null;
		try {
			mProvider.insert(ChatLog.Message.CONTENT_URI, values);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue("insert into ChatLog.Message should be forbidden", exception instanceof RuntimeException);

		// Check that provider supports delete operation
		try {
			String mSelectionClause = ChatLog.Message.ID + " = -1";
			int count = mProvider.delete(ChatLog.Message.CONTENT_URI, mSelectionClause, null);
			assertTrue(count == 0);
		} catch (Exception e) {
			fail("delete of ChatLog.Message failed " + e.getMessage());
		}

		exception = null;
		// Check that provider does not support update operation
		try {
			String mSelectionClause = ChatLog.Message.ID + " = -1";
			mProvider.update(ChatLog.Message.CONTENT_URI, values, mSelectionClause, null);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue("update of ChatLog.Message should be forbidden", exception instanceof RuntimeException);
	}

}
