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

import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.ish.ImageSharing;
import com.gsma.services.rcs.ish.ImageSharingLog;

public class ImageSharingLogTest extends InstrumentationTestCase {

	private final String[] IMAGE_SHARING_LOG_PROJECTION = new String[] { ImageSharingLog.CONTACT,
			ImageSharingLog.DIRECTION, ImageSharingLog.FILE, ImageSharingLog.FILENAME, ImageSharingLog.FILESIZE,
			ImageSharingLog.SHARING_ID, ImageSharingLog.MIME_TYPE, ImageSharingLog.STATE, ImageSharingLog.REASON_CODE,
			ImageSharingLog.TIMESTAMP, ImageSharingLog.TRANSFERRED };

	private ContentProviderClient mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mProvider = getInstrumentation().getTargetContext().getContentResolver()
				.acquireContentProviderClient(ImageSharingLog.CONTENT_URI);
		assertNotNull(mProvider);
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
	public void testImageSharingLogQuery() {
		// Check that provider handles columns names and query operation
		Cursor cursor = null;
		try {
			String where = ImageSharingLog.SHARING_ID.concat("=?");
			String[] whereArgs = new String[] { "123456789" };
			cursor = mProvider.query(ImageSharingLog.CONTENT_URI, IMAGE_SHARING_LOG_PROJECTION, where, whereArgs, null);
			assertNotNull(cursor);
		} catch (Exception e) {
			fail("query of ImageSharingLog failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testImageSharingLogQueryById() {
		// Check that provider handles columns names and query operation
		Uri uri = Uri.withAppendedPath(ImageSharingLog.CONTENT_URI, "123456789");
		Cursor cursor = null;
		try {
			cursor = mProvider.query(uri, IMAGE_SHARING_LOG_PROJECTION, null, null, null);
			assertNotNull(cursor);
		} catch (Exception e) {
			fail("By Id query of ImageSharingLog failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testImageSharingLogQueryWithoutWhereClause() {
		// Check that provider handles columns names and query operation
		Cursor cursor = null;
		try {
			cursor = mProvider.query(ImageSharingLog.CONTENT_URI, null, null, null, null);
			assertNotNull(cursor);
			if (cursor.moveToFirst()) {
				Utils.checkProjection(IMAGE_SHARING_LOG_PROJECTION, cursor.getColumnNames());
			}
		} catch (Exception e) {
			fail("Without where clause query of ImageSharingLog failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testImageSharingLogInsert() {
		// // Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(ImageSharingLog.FILESIZE, 10000L);
		values.put(ImageSharingLog.FILENAME, "filename");
		values.put(ImageSharingLog.CONTACT, "+3360102030405");
		values.put(ImageSharingLog.DIRECTION, RcsCommon.Direction.INCOMING);
		values.put(ImageSharingLog.SHARING_ID, "0123456789");
		values.put(ImageSharingLog.MIME_TYPE, "image/jpeg");
		values.put(ImageSharingLog.STATE, ImageSharing.State.INVITED);
		values.put(ImageSharingLog.REASON_CODE, ImageSharing.ReasonCode.UNSPECIFIED);
		values.put(ImageSharingLog.TIMESTAMP, System.currentTimeMillis());
		values.put(ImageSharingLog.TRANSFERRED, 0L);
		try {
			mProvider.insert(ImageSharingLog.CONTENT_URI, values);
			fail("ImageSharingLog is read only");
		} catch (Exception ex) {
			assertTrue("insert into ImageSharingLog should be forbidden", ex instanceof RuntimeException);
		}

	}

	public void testImageSharingLogDelete() {
		// Check that provider supports delete operation
		try {
			mProvider.delete(ImageSharingLog.CONTENT_URI, null, null);
			fail("ImageSharingLog is read only");
		} catch (Exception e) {
			assertTrue("delete of ImageSharingLog should be forbidden", e instanceof RuntimeException);
		}
	}

	public void testImageSharingLogUpdate() {
		// Check that provider does not support update operation
		ContentValues values = new ContentValues();
		values.put(ImageSharingLog.FILESIZE, 10000L);
		values.put(ImageSharingLog.FILENAME, "filename");
		values.put(ImageSharingLog.CONTACT, "+3360102030405");
		values.put(ImageSharingLog.DIRECTION, RcsCommon.Direction.INCOMING);
		values.put(ImageSharingLog.MIME_TYPE, "image/jpeg");
		values.put(ImageSharingLog.STATE, ImageSharing.State.INVITED);
		values.put(ImageSharingLog.TIMESTAMP, System.currentTimeMillis());
		values.put(ImageSharingLog.TRANSFERRED, 0L);
		try {
			String where = ImageSharingLog.SHARING_ID.concat("=?");
			String[] whereArgs = new String[] { "123456789" };
			mProvider.update(ImageSharingLog.CONTENT_URI, values, where, whereArgs);
			fail("ImageSharingLog is read only");
		} catch (Exception ex) {
			assertTrue("update of ImageSharingLog should be forbidden", ex instanceof RuntimeException);
		}
	}

}
