package android.tests.provider;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.test.InstrumentationTestCase;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.GroupChat;

public class ChatLogGroupChatTest extends InstrumentationTestCase {
	private ContentResolver mContentResolver;
	private ContentProviderClient mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mContentResolver = getInstrumentation().getTargetContext().getContentResolver();
		mProvider = mContentResolver.acquireContentProviderClient(ChatLog.GroupChat.CONTENT_URI);
	}

	/**
	 * Test the ChatLog.GroupChat according to GSMA API specifications.<br>
	 * Check the following operations:
	 * <ul>
	 * <li> query
	 * <li> insert
	 * <li> delete
	 * <li> update
	 */
	public void testChatLogGroupChat() {
		final String[] CHAT_LOG_GROUPCHAT_PROJECTION = new String[] { ChatLog.GroupChat.ID, ChatLog.GroupChat.CHAT_ID,
				ChatLog.GroupChat.DIRECTION, ChatLog.GroupChat.STATE, ChatLog.GroupChat.SUBJECT, ChatLog.GroupChat.TIMESTAMP };

		// Check that provider handles columns names and query operation
		Cursor c = null;
		try {
			String mSelectionClause = ChatLog.GroupChat.ID + " = ?";
			c = mProvider.query(ChatLog.GroupChat.CONTENT_URI, CHAT_LOG_GROUPCHAT_PROJECTION, mSelectionClause, null, null);
			assertNotNull(c);
			if (c != null) {
				int num = c.getColumnCount();
				assertTrue(num == CHAT_LOG_GROUPCHAT_PROJECTION.length);
			}
		} catch (Exception e) {
			fail("Unexpected Exception" + e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}

		// Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(ChatLog.GroupChat.CHAT_ID, "chat_id");
		values.put(ChatLog.GroupChat.DIRECTION, GroupChat.Direction.INCOMING);
		values.put(ChatLog.GroupChat.STATE, GroupChat.State.INVITED);
		values.put(ChatLog.GroupChat.SUBJECT, "subject");
		values.put(ChatLog.GroupChat.TIMESTAMP, System.currentTimeMillis());
		Throwable exception = null;
		try {
			mProvider.insert(ChatLog.GroupChat.CONTENT_URI, values);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue(exception instanceof RuntimeException);

		// Check that provider supports delete operation
		try {
			String mSelectionClause = ChatLog.GroupChat.ID + " = -1";
			int count = mProvider.delete(ChatLog.GroupChat.CONTENT_URI, mSelectionClause, null);
			assertTrue(count == 0);
		} catch (Exception e) {
			fail("Unexpected Exception" + e.getMessage());
		}

		exception = null;
		// Check that provider does not support update operation
		try {
			String mSelectionClause = ChatLog.GroupChat.ID + " = -1";
			mProvider.update(ChatLog.GroupChat.CONTENT_URI, values, mSelectionClause, null);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue(exception instanceof RuntimeException);
	}
}
