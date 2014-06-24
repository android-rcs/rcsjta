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
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.gsma.services.rcs.JoynServiceConfiguration;
import com.gsma.services.rcs.JoynServiceConfiguration.Settings;
import com.orangelabs.rcs.provider.settings.RcsSettings;

public class RcsSettingsTest extends AndroidTestCase {
	private ContentResolver cr;

	private RcsSettings rcsSettings;

	final String[] SETTINGS_PROJECTION = { JoynServiceConfiguration.Settings.ID, JoynServiceConfiguration.Settings.KEY,
			JoynServiceConfiguration.Settings.VALUE };

	protected void setUp() throws Exception {
		super.setUp();
		cr = mContext.getContentResolver();
		RcsSettings.createInstance(mContext);
		rcsSettings = RcsSettings.getInstance();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testRcsSettingsInsertRuntimeException() {
		// Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(JoynServiceConfiguration.Settings.KEY, JoynServiceConfiguration.Settings.MESSAGING_MODE);
		values.put(JoynServiceConfiguration.Settings.VALUE, JoynServiceConfiguration.Settings.MessagingModes.SEAMLESS);

		Throwable exception = null;
		try {
			cr.insert(JoynServiceConfiguration.Settings.CONTENT_URI, values);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue(exception instanceof RuntimeException);
	}

	public void testRcsSettingsDelete() {
		Throwable exception = null;
		try {
			String mSelectionClause = JoynServiceConfiguration.Settings.ID + " = -1";
			int count = cr.delete(JoynServiceConfiguration.Settings.CONTENT_URI, mSelectionClause, null);
			assertTrue(count == 0);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue(exception instanceof RuntimeException);
	}

	public void testRcsSettingsQueryProjection() {
		Cursor c = null;
		try {
			String where = new StringBuilder(JoynServiceConfiguration.Settings.KEY).append("=?").toString();
			String[] whereArgs = new String[] { JoynServiceConfiguration.Settings.MY_COUNTRY_CODE };
			c = cr.query(JoynServiceConfiguration.Settings.CONTENT_URI, null, where, whereArgs, null);
			// Check projection
			assertTrue(c.getColumnCount() == SETTINGS_PROJECTION.length);
			String[] columnNames = c.getColumnNames();
			Set<String> columnNamesSet = new HashSet<String>(Arrays.asList(columnNames));
			Set<String> projectionSet = new HashSet<String>(Arrays.asList(SETTINGS_PROJECTION));
			assertTrue(projectionSet.containsAll(columnNamesSet));
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
			String where = new StringBuilder(JoynServiceConfiguration.Settings.KEY).append("=?").toString();
			String[] whereArgs = new String[] { JoynServiceConfiguration.Settings.MY_COUNTRY_CODE };
			c = cr.query(JoynServiceConfiguration.Settings.CONTENT_URI, null, where, whereArgs, null);
			assertTrue(c.getCount() == 1);
			if (c.moveToFirst()) {
				String key = c.getString(c.getColumnIndexOrThrow(JoynServiceConfiguration.Settings.KEY));
				assertTrue(key.equals(JoynServiceConfiguration.Settings.MY_COUNTRY_CODE));
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
			String where = new StringBuilder(JoynServiceConfiguration.Settings.KEY).append("= '")
					.append(JoynServiceConfiguration.Settings.MY_COUNTRY_CODE).append("'").toString();
			c = cr.query(JoynServiceConfiguration.Settings.CONTENT_URI, null, where, null, null);
			assertTrue(c.getCount() == 1);
			if (c.moveToFirst()) {
				String key = c.getString(c.getColumnIndexOrThrow(JoynServiceConfiguration.Settings.KEY));
				assertTrue(key.equals(JoynServiceConfiguration.Settings.MY_COUNTRY_CODE));
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

	private int getIdForCC() {
		Cursor c = null;
		int id = -1;
		// Get ID for MY_COUNTRY_CODE
		try {
			String where = new StringBuilder(JoynServiceConfiguration.Settings.KEY).append("=?").toString();
			String[] whereArgs = new String[] { JoynServiceConfiguration.Settings.MY_COUNTRY_CODE };
			c = cr.query(JoynServiceConfiguration.Settings.CONTENT_URI, null, where, whereArgs, null);
			if (c.moveToFirst()) {
				id = c.getInt(c.getColumnIndexOrThrow(JoynServiceConfiguration.Settings.ID));
			}
		} catch (Exception e) {
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return id;
	}

	public void testRcsSettingsQueryByID() {
		int ccID = getIdForCC();
		Uri contenUtiWithId = ContentUris.withAppendedId(JoynServiceConfiguration.Settings.CONTENT_URI, ccID);
		Cursor c = null;
		try {
			c = cr.query(contenUtiWithId, null, null, null, null);
			assertTrue(c.getCount() == 1);
			if (c.moveToFirst()) {
				String key = c.getString(c.getColumnIndexOrThrow(JoynServiceConfiguration.Settings.KEY));
				assertTrue(key.equals(JoynServiceConfiguration.Settings.MY_COUNTRY_CODE));
			} else {
				fail("Cannot find by ID " + ccID);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public void testRcsSessionsUpdateByKey() {
		int defaultMessaginMethod = rcsSettings.getDefaultMessagingMethod();
		ContentValues values = new ContentValues();
		values.put(Settings.VALUE, JoynServiceConfiguration.Settings.DefaultMessagingMethods.NON_JOYN);
		String where = new StringBuilder(Settings.KEY).append("=?").toString();
		String[] whereArg = new String[] { Settings.DEFAULT_MESSAGING_METHOD };
		int row = cr.update(Settings.CONTENT_URI, values, where, whereArg);
		assertTrue(row == 1);
		assertTrue(rcsSettings.getDefaultMessagingMethod() == JoynServiceConfiguration.Settings.DefaultMessagingMethods.NON_JOYN);
		rcsSettings.setDefaultMessagingMethod(defaultMessaginMethod);
	}

	public void testRcsSessionsUpdateById() {
		String ccSav = rcsSettings.getCountryCode();
		int ccID = getIdForCC();
		Uri contenUtiWithId = ContentUris.withAppendedId(JoynServiceConfiguration.Settings.CONTENT_URI, ccID);
		ContentValues values = new ContentValues();
		values.put(Settings.VALUE, "+44");
		int row = cr.update(contenUtiWithId, values, null, null);
		assertTrue(row == 1);
		assertTrue(rcsSettings.getCountryCode().equals("+44"));
		rcsSettings.setCountryCode(ccSav);
	}
}