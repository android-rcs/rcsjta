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

import com.gsma.services.rcs.ipcall.IPCallLog;
import com.gsma.services.rcs.vsh.VideoSharing;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.test.InstrumentationTestCase;

public class IPCallLogTest extends InstrumentationTestCase {
	private ContentResolver mContentResolver;
	private ContentProviderClient mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mContentResolver = getInstrumentation().getTargetContext().getContentResolver();
		mProvider = mContentResolver.acquireContentProviderClient(IPCallLog.CONTENT_URI);
	}

	/**
	 * Test the IPCallLog according to GSMA API specifications.<br>
	 * Check the following operations:
	 * <ul>
	 * <li>query
	 * <li>insert
	 * <li>delete
	 * <li>update
	 */
	public void testIPCallLog() {
		final String[] IPCALL_LOG_PROJECTION = new String[] { IPCallLog.ID, IPCallLog.CONTACT_NUMBER, IPCallLog.DIRECTION,
				// IPCallLog.DURATION, TODO
				IPCallLog.CALL_ID, IPCallLog.STATE, IPCallLog.TIMESTAMP };

		// Check that provider handles columns names and query operation
		Cursor c = null;
		try {
			String mSelectionClause = IPCallLog.ID + " = ?";
			c = mProvider.query(IPCallLog.CONTENT_URI, IPCALL_LOG_PROJECTION, mSelectionClause, null, null);
			assertNotNull(c);
			if (c != null) {
				int num = c.getColumnCount();
				assertTrue(num == IPCALL_LOG_PROJECTION.length);
			}
		} catch (Exception e) {
			fail("query of IPCallLog failed " + e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}

		// // Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(IPCallLog.CONTACT_NUMBER, "+3360102030405");
		values.put(IPCallLog.DIRECTION, VideoSharing.Direction.INCOMING);
		values.put(IPCallLog.CALL_ID, "call_id");
		values.put(IPCallLog.STATE, VideoSharing.State.INVITED);
		values.put(IPCallLog.TIMESTAMP, System.currentTimeMillis());
		// values.put(IPCallLog.DURATION, 60L);
		Throwable exception = null;
		try {
			mProvider.insert(IPCallLog.CONTENT_URI, values);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue("insert into IPCallLog should be forbidden", exception instanceof RuntimeException);

		// Check that provider supports delete operation
		try {
			String mSelectionClause = IPCallLog.ID + " = -1";
			int count = mProvider.delete(IPCallLog.CONTENT_URI, mSelectionClause, null);
			assertTrue(count == 0);
		} catch (Exception e) {
			fail("delete of IPCallLog failed " + e.getMessage());
		}

		exception = null;
		// Check that provider does not support update operation
		try {
			String mSelectionClause = IPCallLog.ID + " = -1";
			mProvider.update(IPCallLog.CONTENT_URI, values, mSelectionClause, null);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue("update of IPCallLog should be forbidden", exception instanceof RuntimeException);
	}

}
