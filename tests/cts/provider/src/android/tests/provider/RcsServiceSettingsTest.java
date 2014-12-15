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
import android.database.Cursor;
import android.net.Uri;
import android.test.InstrumentationTestCase;

import com.gsma.services.rcs.RcsServiceConfiguration;

public class RcsServiceSettingsTest extends InstrumentationTestCase {
	final String[] SETTINGS_PROJECTION = { RcsServiceConfiguration.Settings.KEY, RcsServiceConfiguration.Settings.VALUE };

	private ContentProviderClient mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mProvider = getInstrumentation().getTargetContext().getContentResolver()
				.acquireContentProviderClient(RcsServiceConfiguration.Settings.CONTENT_URI);
		assertNotNull(mProvider);
	}

	/**
	 * Test the projection for the query operation.<br>
	 * The projection shall contain the key and column values.
	 */
	public void testRcsSettingsQueryProjection() {
		Cursor cursor = null;
		try {
			String where = RcsServiceConfiguration.Settings.KEY.concat("=?");
			String[] whereArgs = new String[] { RcsServiceConfiguration.Settings.MESSAGING_MODE };
			cursor = mProvider.query(RcsServiceConfiguration.Settings.CONTENT_URI, null, where, whereArgs, null);
			assertNotNull(cursor.moveToFirst());
			// Check projection
			Utils.checkProjection(SETTINGS_PROJECTION, cursor.getColumnNames());
		} catch (Exception e) {
			fail(e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testRcsSettingsQueryByKey() {
		Cursor cursor = null;
		try {
			String where = RcsServiceConfiguration.Settings.KEY.concat("=?");
			String[] whereArgs = new String[] { RcsServiceConfiguration.Settings.MESSAGING_MODE };
			cursor = mProvider.query(RcsServiceConfiguration.Settings.CONTENT_URI, null, where, whereArgs, null);
			assertTrue(cursor.moveToFirst());
			String key = cursor.getString(cursor.getColumnIndexOrThrow(RcsServiceConfiguration.Settings.KEY));
			assertTrue(key.equals(RcsServiceConfiguration.Settings.MESSAGING_MODE));
		} catch (Exception e) {
			fail(e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testRcsSettingsQueryByKeyBis() {
		Cursor cursor = null;
		try {
			String where = new StringBuilder(RcsServiceConfiguration.Settings.KEY).append("= '")
					.append(RcsServiceConfiguration.Settings.MESSAGING_MODE).append("'").toString();
			cursor = mProvider.query(RcsServiceConfiguration.Settings.CONTENT_URI, null, where, null, null);
			assertTrue(cursor.getCount() == 1);
			assertTrue(cursor.moveToFirst());
			String key = cursor.getString(cursor.getColumnIndexOrThrow(RcsServiceConfiguration.Settings.KEY));
			assertTrue(key.equals(RcsServiceConfiguration.Settings.MESSAGING_MODE));
		} catch (Exception e) {
			fail(e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testRcsSettingsQueryByKeyTer() {
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(RcsServiceConfiguration.Settings.CONTENT_URI,
					RcsServiceConfiguration.Settings.MESSAGING_MODE);
			cursor = mProvider.query(uri, null, null, null, null);
			assertTrue(cursor.getCount() == 1);
			assertTrue(cursor.moveToFirst());
			Utils.checkProjection(SETTINGS_PROJECTION, cursor.getColumnNames());
		} catch (Exception e) {
			fail(e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

}
