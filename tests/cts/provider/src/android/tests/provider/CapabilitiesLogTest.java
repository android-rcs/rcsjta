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

//import android.os.*;
import com.gsma.services.rcs.capability.CapabilitiesLog;

public class CapabilitiesLogTest extends InstrumentationTestCase {

	private static final String[] CAPABILITIES_LOG_PROJECTION = new String[] { CapabilitiesLog.CONTACT,
			CapabilitiesLog.CAPABILITY_IMAGE_SHARE, CapabilitiesLog.CAPABILITY_VIDEO_SHARE,
			CapabilitiesLog.CAPABILITY_IM_SESSION, CapabilitiesLog.CAPABILITY_FILE_TRANSFER,
			CapabilitiesLog.CAPABILITY_GEOLOC_PUSH, CapabilitiesLog.CAPABILITY_EXTENSIONS, CapabilitiesLog.AUTOMATA,
			CapabilitiesLog.TIMESTAMP };

	private ContentProviderClient mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mProvider = getInstrumentation().getTargetContext().getContentResolver()
				.acquireContentProviderClient(CapabilitiesLog.CONTENT_URI);
		assertNotNull(mProvider);
	}

	/**
	 * Test the CapabilityLog according to GSMA API specifications.<br>
	 * Check the query operations
	 */
	public void testCapabilitiesLogQuery() {
		// Check that provider handles columns names and query operation
		Cursor cursor = null;
		try {
			String where = CapabilitiesLog.CONTACT.concat("=?");
			String[] whereArgs = new String[] { "+339000000" };
			cursor = mProvider.query(CapabilitiesLog.CONTENT_URI, CAPABILITIES_LOG_PROJECTION, where, whereArgs, null);
			assertNotNull(cursor);
		} catch (Exception e) {
			fail("query of CapabilitiesLog failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testCapabilitiesLogQueryById() {
		// Check that provider handles columns names and query operation by ID
		Uri uri = Uri.withAppendedPath(CapabilitiesLog.CONTENT_URI, "+33612345678");
		// Check that provider handles columns names and query operation
		Cursor cursor = null;
		try {
			cursor = mProvider.query(uri, CAPABILITIES_LOG_PROJECTION, null, null, null);
			assertNotNull(cursor);
		} catch (Exception e) {
			fail("query By Id of CapabilityLog failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testCapabilitiesLogQueryWithoutWhereClause() {
		Cursor cursor = null;
		try {
			cursor = mProvider.query(CapabilitiesLog.CONTENT_URI, null, null, null, null);
			assertNotNull(cursor);
			if (cursor.moveToFirst()) {
				Utils.checkProjection(CAPABILITIES_LOG_PROJECTION, cursor.getColumnNames());
			}
		} catch (Exception e) {
			fail("query without where clause of CapabilitiesLog failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testCapabilitiesLogInsert() {
		// Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(CapabilitiesLog.CAPABILITY_EXTENSIONS, "extension1;extension2");
		values.put(CapabilitiesLog.CAPABILITY_FILE_TRANSFER, CapabilitiesLog.NOT_SUPPORTED);
		values.put(CapabilitiesLog.CAPABILITY_GEOLOC_PUSH, CapabilitiesLog.NOT_SUPPORTED);
		values.put(CapabilitiesLog.CAPABILITY_IM_SESSION, CapabilitiesLog.NOT_SUPPORTED);
		values.put(CapabilitiesLog.CAPABILITY_IMAGE_SHARE, CapabilitiesLog.NOT_SUPPORTED);
		values.put(CapabilitiesLog.CAPABILITY_VIDEO_SHARE, CapabilitiesLog.NOT_SUPPORTED);
		values.put(CapabilitiesLog.TIMESTAMP, System.currentTimeMillis());
		values.put(CapabilitiesLog.CONTACT, "+33612345678");
		values.put(CapabilitiesLog.AUTOMATA, CapabilitiesLog.NOT_SUPPORTED);
		try {
			mProvider.insert(CapabilitiesLog.CONTENT_URI, values);
			fail("CapabilitiesLog is read only");
		} catch (Exception ex) {
			assertTrue("insert into CapabilitiesLog should be forbidden", ex instanceof RuntimeException);
		}
	}

	public void testCapabilitiesLogDelete() {
		// Check that provider does not support delete operation
		try {
			mProvider.delete(CapabilitiesLog.CONTENT_URI, null, null);
			fail("CapabilitiesLog is read only");
		} catch (Exception ex) {
			assertTrue("delete from CapabilitiesLog should be forbidden", ex instanceof RuntimeException);
		}
	}

	public void testCapabilitiesLogUpdate() {
		// Check that provider does not support update operation
		ContentValues values = new ContentValues();
		values.put(CapabilitiesLog.TIMESTAMP, System.currentTimeMillis());
		try {
			String where = CapabilitiesLog.CONTACT.concat("=?");
			String[] whereArgs = new String[] { "+339000000" };
			mProvider.update(CapabilitiesLog.CONTENT_URI, values, where, whereArgs);
			fail("CapabilitiesLog is read only");
		} catch (Exception ex) {
			assertTrue("update of CapabilitiesLog should be forbidden", ex instanceof RuntimeException);
		}
	}

}
