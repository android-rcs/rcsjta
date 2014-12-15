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
import com.gsma.services.rcs.vsh.VideoSharing;
import com.gsma.services.rcs.vsh.VideoSharingLog;

public class VideoSharingLogTest extends InstrumentationTestCase {

	private final String[] VIDEO_SHARING_LOG_PROJECTION = new String[] { VideoSharingLog.CONTACT,
			VideoSharingLog.DIRECTION, VideoSharingLog.DURATION, VideoSharingLog.SHARING_ID, VideoSharingLog.HEIGHT,
			VideoSharingLog.ORIENTATION, VideoSharingLog.REASON_CODE, VideoSharingLog.STATE, VideoSharingLog.TIMESTAMP,
			VideoSharingLog.VIDEO_ENCODING, VideoSharingLog.WIDTH };

	private ContentProviderClient mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mProvider = getInstrumentation().getTargetContext().getContentResolver()
				.acquireContentProviderClient(VideoSharingLog.CONTENT_URI);
		assertNotNull(mProvider);
	}

	/**
	 * Test the VideoSharingLog provider according to GSMA API specifications.<br>
	 * Check the following operations:
	 * <ul>
	 * <li>query
	 * <li>insert
	 * <li>delete
	 * <li>update
	 */
	public void testVideoSharingLogQuery() {
		// Check that provider handles columns names and query operation
		Cursor cursor = null;
		try {
			String where = VideoSharingLog.SHARING_ID.concat("=?");
			String[] whereArgs = new String[] { "123456789" };
			cursor = mProvider.query(VideoSharingLog.CONTENT_URI, VIDEO_SHARING_LOG_PROJECTION, where, whereArgs, null);
			assertNotNull(cursor);
		} catch (Exception e) {
			fail("query of VideoSharingLog failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testVideoSharingLogQueryById() {
		// Check that provider handles columns names and query operation
		Uri uri = Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, "123456789");
		Cursor cursor = null;
		try {
			cursor = mProvider.query(uri, VIDEO_SHARING_LOG_PROJECTION, null, null, null);
			assertNotNull(cursor);
		} catch (Exception e) {
			fail("By Id query of VideoSharingLog failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testVideoSharingLogQueryWithoutWhereClause() {
		Cursor cursor = null;
		try {
			cursor = mProvider.query(VideoSharingLog.CONTENT_URI, null, null, null, null);
			assertNotNull(cursor);
			if (cursor.moveToFirst()) {
				Utils.checkProjection(VIDEO_SHARING_LOG_PROJECTION, cursor.getColumnNames());
			}
		} catch (Exception e) {
			fail("Without where clause query of VideoSharingLog failed " + e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void testVideoSharingLogInsert() {
		// // Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(VideoSharingLog.CONTACT, "+3360102030405");
		values.put(VideoSharingLog.DIRECTION, RcsCommon.Direction.INCOMING);
		values.put(VideoSharingLog.SHARING_ID, "123456789");
		values.put(VideoSharingLog.STATE, VideoSharing.State.INVITED);
		values.put(VideoSharingLog.TIMESTAMP, System.currentTimeMillis());
		values.put(VideoSharingLog.DURATION, 60);
		values.put(VideoSharingLog.HEIGHT, 10);
		values.put(VideoSharingLog.ORIENTATION, 0);
		values.put(VideoSharingLog.REASON_CODE, VideoSharing.ReasonCode.UNSPECIFIED);
		values.put(VideoSharingLog.VIDEO_ENCODING, VideoSharing.Encoding.H264);
		values.put(VideoSharingLog.WIDTH, 0);
		try {
			mProvider.insert(VideoSharingLog.CONTENT_URI, values);
			fail("VideoSharingLog is read only");
		} catch (Exception ex) {
			assertTrue("insert into VideoSharingLog should be forbidden", ex instanceof RuntimeException);
		}
	}

	public void testVideoSharingLogDelete() {
		// Check that provider supports delete operation
		try {
			mProvider.delete(VideoSharingLog.CONTENT_URI, null, null);
			fail("VideoSharingLog is read only");
		} catch (Exception e) {
			assertTrue("delete of VideoSharingLog should be forbidden", e instanceof RuntimeException);
		}
	}

	public void testVideoSharingLogUpdate() {
		ContentValues values = new ContentValues();
		values.put(VideoSharingLog.TIMESTAMP, System.currentTimeMillis());
		// Check that provider does not support update operation
		try {
			String where = VideoSharingLog.SHARING_ID.concat("=?");
			String[] whereArgs = new String[] { "123456789" };
			mProvider.update(VideoSharingLog.CONTENT_URI, values, where, whereArgs);
			fail("VideoSharingLog is read only");
		} catch (Exception ex) {
			assertTrue("update of VideoSharingLog should be forbidden", ex instanceof RuntimeException);
		}
	}

}
