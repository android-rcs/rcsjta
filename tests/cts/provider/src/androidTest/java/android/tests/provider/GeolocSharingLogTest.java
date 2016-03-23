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
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingLog;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.test.InstrumentationTestCase;

public class GeolocSharingLogTest extends InstrumentationTestCase {

    // @formatter:off
    private final String[] GEOLOC_SHARING_LOG_PROJECTION = new String[] {
            GeolocSharingLog.BASECOLUMN_ID, 
            GeolocSharingLog.CONTACT, 
            GeolocSharingLog.DIRECTION,
            GeolocSharingLog.CONTENT, 
            GeolocSharingLog.SHARING_ID, 
            GeolocSharingLog.MIME_TYPE,
            GeolocSharingLog.STATE, 
            GeolocSharingLog.REASON_CODE, 
            GeolocSharingLog.TIMESTAMP
    };
    // @formatter:on

    private ContentProviderClient mProvider;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mProvider = getInstrumentation().getTargetContext().getContentResolver()
                .acquireContentProviderClient(GeolocSharingLog.CONTENT_URI);
        assertNotNull(mProvider);
    }

    /**
     * Test the GeolocSharingLog provider according to GSMA API specifications.<br>
     * Check the following operations:
     * <ul>
     * <li>query
     * <li>insert
     * <li>delete
     * <li>update
     */
    public void testGeolocSharingLogQuery() throws RemoteException {
        /* Check that provider handles columns names and query operation */
        Cursor cursor = null;
        try {
            String where = GeolocSharingLog.SHARING_ID.concat("=?");
            String[] whereArgs = new String[] {
                "123456789"
            };
            cursor = mProvider.query(GeolocSharingLog.CONTENT_URI, GEOLOC_SHARING_LOG_PROJECTION,
                    where, whereArgs, null);
            assertNotNull(cursor);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testGeolocSharingLogQueryById() throws RemoteException {
        /* Check that provider handles columns names and query operation */
        Uri uri = Uri.withAppendedPath(GeolocSharingLog.CONTENT_URI, "123456789");
        Cursor cursor = null;
        try {
            cursor = mProvider.query(uri, GEOLOC_SHARING_LOG_PROJECTION, null, null, null);
            assertNotNull(cursor);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testGeolocSharingLogQueryWithoutWhereClause() throws RemoteException {
        /* Check that provider handles columns names and query operation */
        Cursor cursor = null;
        try {
            cursor = mProvider.query(GeolocSharingLog.CONTENT_URI, null, null, null, null);
            assertNotNull(cursor);
            Utils.checkProjection(GEOLOC_SHARING_LOG_PROJECTION, cursor.getColumnNames());

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testGeolocSharingLogInsert() {
        /* Check that provider does not support insert operation */
        ContentValues values = new ContentValues();
        values.put(GeolocSharingLog.CONTENT, "content");
        values.put(GeolocSharingLog.CONTACT, "+3360102030405");
        values.put(GeolocSharingLog.DIRECTION, RcsService.Direction.INCOMING.toInt());
        values.put(GeolocSharingLog.SHARING_ID, "0123456789");
        values.put(GeolocSharingLog.MIME_TYPE, MimeType.GEOLOC_MESSAGE);
        values.put(GeolocSharingLog.STATE, GeolocSharing.State.INVITED.toInt());
        values.put(GeolocSharingLog.REASON_CODE, GeolocSharing.ReasonCode.UNSPECIFIED.toInt());
        values.put(GeolocSharingLog.TIMESTAMP, System.currentTimeMillis());
        try {
            mProvider.insert(GeolocSharingLog.CONTENT_URI, values);
            fail("GeolocSharingLog is read only");

        } catch (Exception ex) {
            assertTrue("insert into GeolocSharingLog should be forbidden",
                    ex instanceof RuntimeException);
        }
    }

    public void testGeolocSharingLogDelete() {
        /* Check that provider supports delete operation */
        try {
            mProvider.delete(GeolocSharingLog.CONTENT_URI, null, null);
            fail("GeolocSharingLog is read only");

        } catch (Exception e) {
            assertTrue("delete of GeolocSharingLog should be forbidden",
                    e instanceof RuntimeException);
        }
    }

    public void testGeolocSharingLogUpdate() {
        /* Check that provider does not support update operation */
        ContentValues values = new ContentValues();
        values.put(GeolocSharingLog.CONTENT, "content");
        values.put(GeolocSharingLog.CONTACT, "+3360102030405");
        values.put(GeolocSharingLog.DIRECTION, RcsService.Direction.INCOMING.toInt());
        values.put(GeolocSharingLog.MIME_TYPE, MimeType.GEOLOC_MESSAGE);
        values.put(GeolocSharingLog.STATE, GeolocSharing.State.INVITED.toInt());
        values.put(GeolocSharingLog.REASON_CODE, GeolocSharing.ReasonCode.FAILED_SHARING.toInt());
        values.put(GeolocSharingLog.TIMESTAMP, System.currentTimeMillis());
        try {
            String where = GeolocSharingLog.SHARING_ID.concat("=?");
            String[] whereArgs = new String[] {
                "123456789"
            };
            mProvider.update(GeolocSharingLog.CONTENT_URI, values, where, whereArgs);
            fail("GeolocSharingLog is read only");

        } catch (Exception ex) {
            assertTrue("update of GeolocSharingLog should be forbidden",
                    ex instanceof RuntimeException);
        }
    }

}
