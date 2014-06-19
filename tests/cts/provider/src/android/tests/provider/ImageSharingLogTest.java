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

import com.gsma.services.rcs.ish.ImageSharing;
import com.gsma.services.rcs.ish.ImageSharingLog;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.test.InstrumentationTestCase;

public class ImageSharingLogTest extends InstrumentationTestCase {
	private ContentResolver mContentResolver;
	private ContentProviderClient mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mContentResolver = getInstrumentation().getTargetContext().getContentResolver();
		mProvider = mContentResolver.acquireContentProviderClient(ImageSharingLog.CONTENT_URI);
	}

	/**
	 * Test the ImageSharingLog provider according to GSMA API specifications.<br>
	 * Check the following operations:
	 * <ul>
	 * <li>query
	 * <li>insert
	 * <li>delete
	 * <li>update
	 */
	public void testImageSharingLog() {
		final String[] IMAGE_SHARING_LOG_PROJECTION = new String[] { ImageSharingLog.ID, ImageSharingLog.CONTACT_NUMBER,
				ImageSharingLog.DIRECTION, ImageSharingLog.FILENAME, ImageSharingLog.FILESIZE, ImageSharingLog.SHARING_ID,
				ImageSharingLog.MIME_TYPE, ImageSharingLog.STATE, ImageSharingLog.TIMESTAMP, ImageSharingLog.TRANSFERRED };

		// Check that provider handles columns names and query operation
		Cursor c = null;
		try {
			String mSelectionClause = ImageSharingLog.ID + " = ?";
			c = mProvider.query(ImageSharingLog.CONTENT_URI, IMAGE_SHARING_LOG_PROJECTION, mSelectionClause, null, null);
			assertNotNull(c);
			if (c != null) {
				int num = c.getColumnCount();
				assertTrue(num == IMAGE_SHARING_LOG_PROJECTION.length);
			}
		} catch (Exception e) {
			fail("query of ImageSharingLog failed " + e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}

		// // Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(ImageSharingLog.FILESIZE, 10000L);
		values.put(ImageSharingLog.FILENAME, "filename");
		values.put(ImageSharingLog.CONTACT_NUMBER, "+3360102030405");
		values.put(ImageSharingLog.DIRECTION, ImageSharing.Direction.INCOMING);
		values.put(ImageSharingLog.SHARING_ID, "sharing_id");
		values.put(ImageSharingLog.MIME_TYPE, "image/jpeg");
		values.put(ImageSharingLog.STATE, ImageSharing.State.INVITED);
		values.put(ImageSharingLog.TIMESTAMP, System.currentTimeMillis());
		values.put(ImageSharingLog.TRANSFERRED, 0L);
		Throwable exception = null;
		try {
			mProvider.insert(ImageSharingLog.CONTENT_URI, values);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue("insert into ImageSharingLog should be forbidden", exception instanceof RuntimeException);

		// Check that provider supports delete operation
		try {
			String mSelectionClause = ImageSharingLog.ID + " = -1";
			int count = mProvider.delete(ImageSharingLog.CONTENT_URI, mSelectionClause, null);
			assertTrue(count == 0);
		} catch (Exception e) {
			fail("delete of ImageSharingLog failed " + e.getMessage());
		}

		exception = null;
		// Check that provider does not support update operation
		try {
			String mSelectionClause = ImageSharingLog.ID + " = -1";
			mProvider.update(ImageSharingLog.CONTENT_URI, values, mSelectionClause, null);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue("update of ImageSharingLog should be forbidden", exception instanceof RuntimeException);
	}

}
