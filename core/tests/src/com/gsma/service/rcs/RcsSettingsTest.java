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
package com.gsma.service.rcs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.gsma.services.rcs.RcsServiceConfiguration;
import com.gsma.services.rcs.RcsServiceConfiguration.Settings;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.DefaultMessagingMethod;

public class RcsSettingsTest extends AndroidTestCase {
	private ContentResolver cr;

	private RcsSettings rcsSettings;

	final String[] SETTINGS_PROJECTION = { RcsServiceConfiguration.Settings.KEY, RcsServiceConfiguration.Settings.VALUE };

	protected void setUp() throws Exception {
		super.setUp();
		cr = getContext().getContentResolver();
		RcsSettings.createInstance(getContext());
		rcsSettings = RcsSettings.getInstance();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testRcsSettingsInsertRuntimeException() {
		// Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(RcsServiceConfiguration.Settings.KEY, RcsServiceConfiguration.Settings.MESSAGING_MODE);
		values.put(RcsServiceConfiguration.Settings.VALUE, RcsServiceConfiguration.Settings.MessagingModes.SEAMLESS);

		Throwable exception = null;
		try {
			cr.insert(RcsServiceConfiguration.Settings.CONTENT_URI, values);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue(exception instanceof RuntimeException);
	}

	public void testRcsSettingsDelete() {
		Throwable exception = null;
		try {
			int count = cr.delete(RcsServiceConfiguration.Settings.CONTENT_URI, null, null);
			assertTrue(count == 0);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue(exception instanceof RuntimeException);
	}

	public void testRcsSettingsQueryProjection() {
		Cursor c = null;
		try {
			String where = new StringBuilder(RcsServiceConfiguration.Settings.KEY).append("=?").toString();
			String[] whereArgs = new String[] { RcsServiceConfiguration.Settings.CONFIGURATION_VALIDITY };
			c = cr.query(RcsServiceConfiguration.Settings.CONTENT_URI, null, where, whereArgs, null);
			// Check projection
			String[] columnNames = c.getColumnNames();
			Set<String> columnNamesSet = new HashSet<String>(Arrays.asList(columnNames));
			Set<String> projectionSet = new HashSet<String>(Arrays.asList(SETTINGS_PROJECTION));
			assertTrue(columnNamesSet.containsAll(projectionSet));
		} catch (Exception e) {
			fail(e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public void testRcsSettingsQueryByKey() {
		Cursor c = null;
		try {
			String where = new StringBuilder(RcsServiceConfiguration.Settings.KEY).append("=?").toString();
			String[] whereArgs = new String[] { RcsServiceConfiguration.Settings.CONFIGURATION_VALIDITY };
			c = cr.query(RcsServiceConfiguration.Settings.CONTENT_URI, null, where, whereArgs, null);
			assertTrue(c.getCount() == 1);
			if (c.moveToFirst()) {
				String key = c.getString(c.getColumnIndexOrThrow(RcsServiceConfiguration.Settings.KEY));
				assertTrue(key.equals(RcsServiceConfiguration.Settings.CONFIGURATION_VALIDITY));
			} else {
				fail("Cannot find ID");
			}
		} catch (Exception e) {
			fail(e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public void testRcsSettingsQueryByKeyBis() {
		Cursor c = null;
		try {
			String where = new StringBuilder(RcsServiceConfiguration.Settings.KEY).append("= '")
					.append(RcsServiceConfiguration.Settings.CONFIGURATION_VALIDITY).append("'").toString();
			c = cr.query(RcsServiceConfiguration.Settings.CONTENT_URI, null, where, null, null);
			assertTrue(c.getCount() == 1);
			if (c.moveToFirst()) {
				String key = c.getString(c.getColumnIndexOrThrow(RcsServiceConfiguration.Settings.KEY));
				assertTrue(key.equals(RcsServiceConfiguration.Settings.CONFIGURATION_VALIDITY));
			} else {
				fail("Cannot find ID");
			}
		} catch (Exception e) {
			fail(e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}
	
	public void testRcsSettingsQueryByKeyTer() {
		Cursor c = null;
		try {
			Uri uri = Uri.withAppendedPath(RcsServiceConfiguration.Settings.CONTENT_URI, RcsServiceConfiguration.Settings.CONFIGURATION_VALIDITY);
			c = cr.query(uri, null, null, null, null);
			assertTrue(c.getCount() == 1);
			if (c.moveToFirst()) {
				String key = c.getString(c.getColumnIndexOrThrow(RcsServiceConfiguration.Settings.KEY));
				assertTrue(key.equals(RcsServiceConfiguration.Settings.CONFIGURATION_VALIDITY));
			} else {
				fail("Cannot find ID");
			}
		} catch (Exception e) {
			fail(e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public void testRcsSessionsUpdateByKey() {
		DefaultMessagingMethod defaultMessaginMethod = rcsSettings.getDefaultMessagingMethod();
		ContentValues values = new ContentValues();
		values.put(Settings.VALUE, RcsServiceConfiguration.Settings.DefaultMessagingMethods.NON_RCS);
		String where = new StringBuilder(Settings.KEY).append("=?").toString();
		String[] whereArg = new String[] { Settings.DEFAULT_MESSAGING_METHOD };
		int row = cr.update(Settings.CONTENT_URI, values, where, whereArg);
		assertTrue(row == 1);
		assertTrue(DefaultMessagingMethod.NON_RCS.equals(rcsSettings.getDefaultMessagingMethod()));
		rcsSettings.setDefaultMessagingMethod(defaultMessaginMethod);
	}


}