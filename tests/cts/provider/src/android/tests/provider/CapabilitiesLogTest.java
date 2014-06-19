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

import com.gsma.services.rcs.capability.CapabilitiesLog;

public class CapabilitiesLogTest extends InstrumentationTestCase {
	private ContentResolver mContentResolver;
	private ContentProviderClient mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mContentResolver = getInstrumentation().getTargetContext().getContentResolver();
		mProvider = mContentResolver.acquireContentProviderClient(CapabilitiesLog.CONTENT_URI);
	}

	/**
	 * Test the CapabilityLog according to GSMA API specifications.<br>
	 * Check the following operations:
	 * <ul>
	 * <li>query
	 * <li>insert
	 * <li>delete
	 * <li>update
	 */
	public void testCapabilitiesLog() {
		final String[] CAPABILITIES_LOG_PROJECTION = new String[] { CapabilitiesLog.CAPABILITY_EXTENSIONS,
				CapabilitiesLog.CAPABILITY_FILE_TRANSFER, CapabilitiesLog.CAPABILITY_GEOLOC_PUSH,
				CapabilitiesLog.CAPABILITY_IM_SESSION, CapabilitiesLog.CAPABILITY_IMAGE_SHARE,
				CapabilitiesLog.CAPABILITY_IP_VIDEO_CALL, CapabilitiesLog.CAPABILITY_IP_VOICE_CALL,
				CapabilitiesLog.CAPABILITY_VIDEO_SHARE, CapabilitiesLog.CONTACT_NUMBER, CapabilitiesLog.ID };

		// Check that provider handles columns names and query operation
		Cursor c = null;
		try {
			String mSelectionClause = CapabilitiesLog.ID + " = ?";
			c = mProvider.query(CapabilitiesLog.CONTENT_URI, CAPABILITIES_LOG_PROJECTION, mSelectionClause, null, null);
			assertNotNull(c);
			if (c != null) {
				int num = c.getColumnCount();
				assertTrue(num == CAPABILITIES_LOG_PROJECTION.length);
			}
		} catch (Exception e) {
			fail("query of CapabilitiesLog failed " + e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}

		// Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(CapabilitiesLog.CAPABILITY_EXTENSIONS, CapabilitiesLog.NOT_SUPPORTED);
		values.put(CapabilitiesLog.CAPABILITY_FILE_TRANSFER, CapabilitiesLog.NOT_SUPPORTED);
		values.put(CapabilitiesLog.CAPABILITY_GEOLOC_PUSH, CapabilitiesLog.NOT_SUPPORTED);
		values.put(CapabilitiesLog.CAPABILITY_IM_SESSION, CapabilitiesLog.NOT_SUPPORTED);
		values.put(CapabilitiesLog.CAPABILITY_IMAGE_SHARE, CapabilitiesLog.NOT_SUPPORTED);
		values.put(CapabilitiesLog.CAPABILITY_IP_VIDEO_CALL, CapabilitiesLog.NOT_SUPPORTED);
		values.put(CapabilitiesLog.CAPABILITY_IP_VOICE_CALL, CapabilitiesLog.NOT_SUPPORTED);
		values.put(CapabilitiesLog.CAPABILITY_VIDEO_SHARE, CapabilitiesLog.NOT_SUPPORTED);
		values.put(CapabilitiesLog.CONTACT_NUMBER, "+3360102030405");

		Throwable exception = null;
		try {
			mProvider.insert(CapabilitiesLog.CONTENT_URI, values);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue("insert into CapabilitiesLog should be forbidden", exception instanceof RuntimeException);

		// Check that provider does not support delete operation
		exception = null;
		try {
			String mSelectionClause = CapabilitiesLog.ID + " = -1";
			mProvider.delete(CapabilitiesLog.CONTENT_URI, mSelectionClause, null);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue("delete from CapabilitiesLog should be forbidden", exception instanceof RuntimeException);

		exception = null;
		// Check that provider does not support update operation
		try {
			String mSelectionClause = CapabilitiesLog.ID + " = -1";
			mProvider.update(CapabilitiesLog.CONTENT_URI, values, mSelectionClause, null);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue("update of CapabilitiesLog should be forbidden", exception instanceof RuntimeException);
	}

}
