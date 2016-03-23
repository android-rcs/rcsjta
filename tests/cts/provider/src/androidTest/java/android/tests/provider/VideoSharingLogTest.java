/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.sharing.video.VideoSharing;
import com.gsma.services.rcs.sharing.video.VideoSharingLog;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.test.InstrumentationTestCase;

public class VideoSharingLogTest extends InstrumentationTestCase {

    // @formatter:off
    private final String[] VIDEO_SHARING_LOG_PROJECTION = new String[] {
            VideoSharingLog.BASECOLUMN_ID, 
            VideoSharingLog.CONTACT, 
            VideoSharingLog.DIRECTION,
            VideoSharingLog.DURATION, 
            VideoSharingLog.HEIGHT, 
            VideoSharingLog.REASON_CODE,
            VideoSharingLog.SHARING_ID, 
            VideoSharingLog.STATE, 
            VideoSharingLog.TIMESTAMP,
            VideoSharingLog.VIDEO_ENCODING, 
            VideoSharingLog.WIDTH
    };
    // @formatter:on

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
    public void testVideoSharingLogQuery() throws RemoteException {
        // Check that provider handles columns names and query operation
        Cursor cursor = null;
        try {
            String where = VideoSharingLog.SHARING_ID.concat("=?");
            String[] whereArgs = new String[] {
                "123456789"
            };
            cursor = mProvider.query(VideoSharingLog.CONTENT_URI, VIDEO_SHARING_LOG_PROJECTION,
                    where, whereArgs, null);
            assertNotNull(cursor);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testVideoSharingLogQueryById() throws RemoteException {
        // Check that provider handles columns names and query operation
        Uri uri = Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, "123456789");
        Cursor cursor = null;
        try {
            cursor = mProvider.query(uri, VIDEO_SHARING_LOG_PROJECTION, null, null, null);
            assertNotNull(cursor);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testVideoSharingLogQueryWithoutWhereClause() throws RemoteException {
        Cursor cursor = null;
        try {
            cursor = mProvider.query(VideoSharingLog.CONTENT_URI, null, null, null, null);
            assertNotNull(cursor);
            Utils.checkProjection(VIDEO_SHARING_LOG_PROJECTION, cursor.getColumnNames());

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
        values.put(VideoSharingLog.DIRECTION, RcsService.Direction.INCOMING.toInt());
        values.put(VideoSharingLog.SHARING_ID, "123456789");
        values.put(VideoSharingLog.STATE, VideoSharing.State.INVITED.toInt());
        values.put(VideoSharingLog.TIMESTAMP, System.currentTimeMillis());
        values.put(VideoSharingLog.DURATION, 60);
        values.put(VideoSharingLog.HEIGHT, 10);
        values.put(VideoSharingLog.REASON_CODE, VideoSharing.ReasonCode.UNSPECIFIED.toInt());
        values.put(VideoSharingLog.VIDEO_ENCODING, VideoSharing.Encoding.H264);
        values.put(VideoSharingLog.WIDTH, 0);
        try {
            mProvider.insert(VideoSharingLog.CONTENT_URI, values);
            fail("VideoSharingLog is read only");

        } catch (Exception ex) {
            assertTrue("insert into VideoSharingLog should be forbidden",
                    ex instanceof RuntimeException);
        }
    }

    public void testVideoSharingLogDelete() {
        // Check that provider supports delete operation
        try {
            mProvider.delete(VideoSharingLog.CONTENT_URI, null, null);
            fail("VideoSharingLog is read only");

        } catch (Exception e) {
            assertTrue("delete of VideoSharingLog should be forbidden",
                    e instanceof RuntimeException);
        }
    }

    public void testVideoSharingLogUpdate() {
        ContentValues values = new ContentValues();
        values.put(VideoSharingLog.TIMESTAMP, System.currentTimeMillis());
        // Check that provider does not support update operation
        try {
            String where = VideoSharingLog.SHARING_ID.concat("=?");
            String[] whereArgs = new String[] {
                "123456789"
            };
            mProvider.update(VideoSharingLog.CONTENT_URI, values, where, whereArgs);
            fail("VideoSharingLog is read only");

        } catch (Exception ex) {
            assertTrue("update of VideoSharingLog should be forbidden",
                    ex instanceof RuntimeException);
        }
    }

}
