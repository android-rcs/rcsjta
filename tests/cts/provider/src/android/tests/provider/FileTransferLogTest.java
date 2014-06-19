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

import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferLog;

public class FileTransferLogTest extends InstrumentationTestCase {
	private ContentResolver mContentResolver;
	private ContentProviderClient mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mContentResolver = getInstrumentation().getTargetContext().getContentResolver();
		mProvider = mContentResolver.acquireContentProviderClient(FileTransferLog.CONTENT_URI);
	}

	/**
	 * Test the FileTransferLog according to GSMA API specifications.<br>
	 * Check the following operations:
	 * <ul>
	 * <li>query
	 * <li>insert
	 * <li>delete
	 * <li>update
	 */
	public void testFileTransferLog() {
		final String[] FILE_TRANSFER_LOG_PROJECTION = new String[] { FileTransferLog.ID, FileTransferLog.CONTACT_NUMBER,
				FileTransferLog.DIRECTION, FileTransferLog.FILENAME, FileTransferLog.FILESIZE, FileTransferLog.FT_ID,
				FileTransferLog.MIME_TYPE, FileTransferLog.STATE, FileTransferLog.TIMESTAMP,
				// FileTransferLog.TIMESTAMP_DELIVERED, TODO
				// FileTransferLog.TIMESTAMP_DISPLAYED, 
				// FileTransferLog.TIMESTAMP_SENT,
				FileTransferLog.TRANSFERRED };

		// Check that provider handles columns names and query operation
		Cursor c = null;
		try {
			String mSelectionClause = FileTransferLog.ID + " = ?";
			c = mProvider.query(FileTransferLog.CONTENT_URI, FILE_TRANSFER_LOG_PROJECTION, mSelectionClause, null, null);
			assertNotNull(c);
			if (c != null) {
				int num = c.getColumnCount();
				assertTrue(num == FILE_TRANSFER_LOG_PROJECTION.length);
			}
		} catch (Exception e) {
			fail("query of FileTransferLog failed " + e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}

		// // Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(FileTransferLog.FILESIZE, 10000L);
		values.put(FileTransferLog.FILENAME, "filename");
		values.put(FileTransferLog.CONTACT_NUMBER, "+3360102030405");
		values.put(FileTransferLog.DIRECTION, FileTransfer.Direction.INCOMING);
		values.put(FileTransferLog.FT_ID, "ft_id");
		values.put(FileTransferLog.MIME_TYPE, "image/jpeg");
		values.put(FileTransferLog.STATE, FileTransfer.State.INVITED);
		values.put(FileTransferLog.TIMESTAMP, System.currentTimeMillis());
		values.put(FileTransferLog.TRANSFERRED, 0L);
		Throwable exception = null;
		try {
			mProvider.insert(FileTransferLog.CONTENT_URI, values);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue("insert into FileTransferLog should be forbidden", exception instanceof RuntimeException);

		// Check that provider supports delete operation
		try {
			String mSelectionClause = FileTransferLog.ID + " = -1";
			int count = mProvider.delete(FileTransferLog.CONTENT_URI, mSelectionClause, null);
			assertTrue(count == 0);
		} catch (Exception e) {
			fail("delete of FileTransferLog failed " + e.getMessage());
		}

		exception = null;
		// Check that provider does not support update operation
		try {
			String mSelectionClause = FileTransferLog.ID + " = -1";
			mProvider.update(FileTransferLog.CONTENT_URI, values, mSelectionClause, null);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue("update of FileTransferLog should be forbidden", exception instanceof RuntimeException);
	}

}
