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
import com.gsma.services.rcs.chat.GroupChat;

public class ChatLogGroupChatTest extends InstrumentationTestCase {

	private final String[] CHAT_LOG_GROUPCHAT_PROJECTION = new String[] { ChatLog.GroupChat.CHAT_ID,
			ChatLog.GroupChat.CONTACT, ChatLog.GroupChat.DIRECTION, ChatLog.GroupChat.PARTICIPANTS,
			ChatLog.GroupChat.REASON_CODE, ChatLog.GroupChat.STATE, ChatLog.GroupChat.SUBJECT,
			ChatLog.GroupChat.TIMESTAMP };

	private ContentProviderClient mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mProvider = getInstrumentation().getTargetContext().getContentResolver()
				.acquireContentProviderClient(ChatLog.GroupChat.CONTENT_URI);
		assertNotNull(mProvider);
	}

	/**
	 * Test the ChatLog.GroupChat provider according to GSMA API specifications.<br>
	 * Check the following operations:
	 * <ul>
	 * <li>query
	 * <li>insert
	 * <li>delete
	 * <li>update
	 */
	public void testChatLogGroupQuery() {
		// Check that provider handles columns names and query operation
		Cursor cursor = null;
		try {
			String where = ChatLog.GroupChat.CHAT_ID.concat("=?");
			String[] whereArgs = new String[] { "123456789" };
			cursor = mProvider.query(ChatLog.GroupChat.CONTENT_URI, CHAT_LOG_GROUPCHAT_PROJECTION, where, whereArgs,
					null);
			assertNotNull(cursor);
		} catch (Exception e) {
			fail("query of ChatLog.GroupChat failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testChatLogGroupQueryById() {
		Uri uri = Uri.withAppendedPath(ChatLog.GroupChat.CONTENT_URI, "123456789");
		Cursor cursor = null;
		try {
			cursor = mProvider.query(uri, CHAT_LOG_GROUPCHAT_PROJECTION, null, null, null);
			assertNotNull(cursor);
		} catch (Exception e) {
			fail("By Id query of ChatLog.GroupChat failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testChatLogGroupQueryWithoutWhereClause() {
		Cursor cursor = null;
		try {
			cursor = mProvider.query(ChatLog.GroupChat.CONTENT_URI, null, null, null, null);
			assertNotNull(cursor);
			if (cursor.moveToFirst()) {
				Utils.checkProjection(CHAT_LOG_GROUPCHAT_PROJECTION, cursor.getColumnNames());
			}
		} catch (Exception e) {
			fail("Without where clause query of ChatLog.GroupChat failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testChatLogGroupInsert() {
		// Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(ChatLog.GroupChat.CHAT_ID, "123456789");
		values.put(ChatLog.GroupChat.DIRECTION, RcsCommon.Direction.INCOMING);
		values.put(ChatLog.GroupChat.STATE, GroupChat.State.INVITED);
		values.put(ChatLog.GroupChat.SUBJECT, "subject");
		values.put(ChatLog.GroupChat.TIMESTAMP, System.currentTimeMillis());
		values.put(ChatLog.GroupChat.CONTACT, "+33612345678");
		values.put(ChatLog.GroupChat.PARTICIPANTS, "participant1,participant2");
		values.put(ChatLog.GroupChat.REASON_CODE, GroupChat.ReasonCode.UNSPECIFIED);
		try {
			mProvider.insert(ChatLog.GroupChat.CONTENT_URI, values);
			fail("ChatLog is read only");
		} catch (Exception ex) {
			assertTrue("insert into ChatLog.GroupChat should be forbidden", ex instanceof RuntimeException);
		}
	}

	public void testChatLogGroupDelete() {
		// Check that provider supports delete operation
		try {
			mProvider.delete(ChatLog.GroupChat.CONTENT_URI, null, null);
			fail("ChatLog is read only");
		} catch (Exception ex) {
			assertTrue("delete of ChatLog.GroupChat should be forbidden", ex instanceof RuntimeException);
		}
	}

	public void testChatLogGroupUpdate() {
		ContentValues values = new ContentValues();
		values.put(ChatLog.GroupChat.TIMESTAMP, System.currentTimeMillis());
		// Check that provider does not support update operation
		try {
			String where = ChatLog.GroupChat.CHAT_ID.concat("=?");
			String[] whereArgs = new String[] { "123456789" };
			mProvider.update(ChatLog.GroupChat.CONTENT_URI, values, where, whereArgs);
			fail("ChatLog is read only");
		} catch (Exception ex) {
			assertTrue("update of ChatLog.GroupChat should be forbidden", ex instanceof RuntimeException);
		}
	}
}
