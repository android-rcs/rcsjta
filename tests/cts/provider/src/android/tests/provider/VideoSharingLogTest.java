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

import com.gsma.services.rcs.vsh.VideoSharing;
import com.gsma.services.rcs.vsh.VideoSharingLog;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.test.InstrumentationTestCase;

public class VideoSharingLogTest extends InstrumentationTestCase {
	private ContentResolver mContentResolver;
	private ContentProviderClient mProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mContentResolver = getInstrumentation().getTargetContext().getContentResolver();
		mProvider = mContentResolver.acquireContentProviderClient(VideoSharingLog.CONTENT_URI);
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
	public void testVideoSharingLog() {
		final String[] VIDEO_SHARING_LOG_PROJECTION = new String[] { VideoSharingLog.ID, VideoSharingLog.CONTACT_NUMBER,
				VideoSharingLog.DIRECTION, VideoSharingLog.DURATION, VideoSharingLog.SHARING_ID, VideoSharingLog.STATE,
				VideoSharingLog.TIMESTAMP };

		// Check that provider handles columns names and query operation
		Cursor c = null;
		try {
			String mSelectionClause = VideoSharingLog.ID + " = ?";
			c = mProvider.query(VideoSharingLog.CONTENT_URI, VIDEO_SHARING_LOG_PROJECTION, mSelectionClause, null, null);
			assertNotNull(c);
			if (c != null) {
				int num = c.getColumnCount();
				assertTrue(num == VIDEO_SHARING_LOG_PROJECTION.length);
			}
		} catch (Exception e) {
			fail("query of VideoSharingLog failed " + e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}

		// // Check that provider does not support insert operation
		ContentValues values = new ContentValues();
		values.put(VideoSharingLog.CONTACT_NUMBER, "+3360102030405");
		values.put(VideoSharingLog.DIRECTION, VideoSharing.Direction.INCOMING);
		values.put(VideoSharingLog.SHARING_ID, "sharing_id");
		values.put(VideoSharingLog.STATE, VideoSharing.State.INVITED);
		values.put(VideoSharingLog.TIMESTAMP, System.currentTimeMillis());
		values.put(VideoSharingLog.DURATION, 60L);
		Throwable exception = null;
		try {
			mProvider.insert(VideoSharingLog.CONTENT_URI, values);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue("insert into VideoSharingLog should be forbidden", exception instanceof RuntimeException);

		// Check that provider supports delete operation
		try {
			String mSelectionClause = VideoSharingLog.ID + " = -1";
			int count = mProvider.delete(VideoSharingLog.CONTENT_URI, mSelectionClause, null);
			assertTrue(count == 0);
		} catch (Exception e) {
			fail("delete of VideoSharingLog failed " + e.getMessage());
		}

		exception = null;
		// Check that provider does not support update operation
		try {
			String mSelectionClause = VideoSharingLog.ID + " = -1";
			mProvider.update(VideoSharingLog.CONTENT_URI, values, mSelectionClause, null);
		} catch (Exception ex) {
			exception = ex;
		}
		assertTrue("update of VideoSharingLog should be forbidden", exception instanceof RuntimeException);
	}

}
