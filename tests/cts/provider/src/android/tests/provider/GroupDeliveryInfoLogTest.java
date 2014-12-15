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

import com.gsma.services.rcs.GroupDeliveryInfoLog;

/**
 * @author Danielle Rouquier
 *
 */
public class GroupDeliveryInfoLogTest extends InstrumentationTestCase {

	private ContentProviderClient mProvider;
	private static final String[] GROUPDELIVERYINFO_LOG_PROJECTION = new String[] { GroupDeliveryInfoLog.ID,
			GroupDeliveryInfoLog.CONTACT, GroupDeliveryInfoLog.CHAT_ID, GroupDeliveryInfoLog.REASON_CODE,
			GroupDeliveryInfoLog.STATUS, GroupDeliveryInfoLog.TIMESTAMP_DELIVERED,
			GroupDeliveryInfoLog.TIMESTAMP_DISPLAYED };

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mProvider = getInstrumentation().getTargetContext().getContentResolver()
				.acquireContentProviderClient(GroupDeliveryInfoLog.CONTENT_URI);
		assertNotNull(mProvider);
	}

	/**
	 * Test the GroupDeliveryInfoLog according to GSMA API specifications.<br>
	 * Check the query operations
	 */
	public void testGroupDeliveryInfoLogQuery() {
		// Check that provider handles columns names and query operation
		Cursor cursor = null;
		try {
			String where = GroupDeliveryInfoLog.CONTACT.concat("=?");
			String[] whereArgs = new String[] { "+33123456789" };
			cursor = mProvider.query(GroupDeliveryInfoLog.CONTENT_URI, GROUPDELIVERYINFO_LOG_PROJECTION, where,
					whereArgs, null);
			assertNotNull(cursor);
		} catch (Exception e) {
			fail("query of GroupDeliveryInfoLog failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testGroupDeliveryInfoLogQueryById() {
		// Check that provider handles columns names and query operation by ID
		Uri uri = Uri.withAppendedPath(GroupDeliveryInfoLog.CONTENT_URI, "123456789");
		Cursor cursor = null;
		try {
			assertNotNull(mProvider);
			cursor = mProvider.query(uri, GROUPDELIVERYINFO_LOG_PROJECTION, null, null, null);
			assertNotNull(cursor);
		} catch (Exception e) {
			fail("query of GroupDeliveryInfoLog failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testGroupDeliveryInfoLogWithoutWhereClaused() {
		// Check that provider handles columns names and query operation where clause is null
		Cursor cursor = null;
		try {
			cursor = mProvider.query(GroupDeliveryInfoLog.CONTENT_URI, null, null, null, null);
			assertNotNull(cursor);
			if (cursor.moveToFirst()) {
				Utils.checkProjection(GROUPDELIVERYINFO_LOG_PROJECTION, cursor.getColumnNames());
			}
		} catch (Exception e) {
			fail("Without where clause query of GroupDeliveryInfoLog failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testGroupDeliveryInfoLogInsert() {
		// Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(GroupDeliveryInfoLog.ID, "delivinfo_id");
		values.put(GroupDeliveryInfoLog.CHAT_ID, "chat_id");
		values.put(GroupDeliveryInfoLog.CONTACT, "+3360102030405");
		values.put(GroupDeliveryInfoLog.REASON_CODE, GroupDeliveryInfoLog.ReasonCode.UNSPECIFIED);
		values.put(GroupDeliveryInfoLog.STATUS, GroupDeliveryInfoLog.Status.DELIVERED);
		values.put(GroupDeliveryInfoLog.TIMESTAMP_DELIVERED, 0);
		values.put(GroupDeliveryInfoLog.TIMESTAMP_DISPLAYED, 0);
		try {
			mProvider.insert(GroupDeliveryInfoLog.CONTENT_URI, values);
			fail("GroupDeliveryInfoLog is read only");
		} catch (Exception ex) {
			assertTrue("insert into GroupDeliveryInfoLog should be forbidden", ex instanceof RuntimeException);
		}
		
	}

	public void testGroupDeliveryInfoLogDelete() {
		// Check that provider supports delete operation
		try {
			mProvider.delete(GroupDeliveryInfoLog.CONTENT_URI, null, null);
			fail("GroupDeliveryInfoLog is read only");
		} catch (Exception e) {
			assertTrue("delete of GroupDeliveryInfoLog should be forbidden", e instanceof RuntimeException);
		}
	}

	public void GroupDeliveryInfoLogUpdate() {
		ContentValues values = new ContentValues();
		values.put(GroupDeliveryInfoLog.TIMESTAMP_DELIVERED, 0);
		// Check that provider does not support update operation
		try {
			String where = GroupDeliveryInfoLog.ID.concat("=?");
			String[] whereArgs = new String[] { "123456789" };
			mProvider.update(GroupDeliveryInfoLog.CONTENT_URI, values, where, whereArgs);
			fail("ChatLog is read only");
		} catch (Exception ex) {
			assertTrue("Updating a GroupDeliveryInfoLog should be forbidden", ex instanceof RuntimeException);
		}
	}

}
