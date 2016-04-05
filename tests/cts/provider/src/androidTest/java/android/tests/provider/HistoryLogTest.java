/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package android.tests.provider;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.HistoryUriBuilder;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingLog;
import com.gsma.services.rcs.sharing.image.ImageSharingLog;
import com.gsma.services.rcs.sharing.video.VideoSharingLog;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.test.InstrumentationTestCase;


/**
 * Created by sandrine on 31/03/2016.
 */
public class HistoryLogTest extends InstrumentationTestCase {
    private ContentProviderClient mProvider;

    // @formatter:off
    private final String[] HISTORY_LOG_PROJECTION = new String[]{
            HistoryLog.BASECOLUMN_ID,
            HistoryLog.CHAT_ID,
            HistoryLog.CONTACT,
            HistoryLog.CONTENT,
            HistoryLog.DIRECTION,
            HistoryLog.DISPOSITION,
            HistoryLog.DURATION,
            HistoryLog.EXPIRED_DELIVERY,
            HistoryLog.FILEICON,
            HistoryLog.FILEICON_MIME_TYPE,
            HistoryLog.FILESIZE,
            HistoryLog.FILENAME,
            HistoryLog.ID,
            HistoryLog.MIME_TYPE,
            HistoryLog.READ_STATUS,
            HistoryLog.REASON_CODE,
            HistoryLog.STATUS,
            HistoryLog.PROVIDER_ID,
            HistoryLog.TRANSFERRED,
            HistoryLog.TIMESTAMP,
            HistoryLog.TIMESTAMP_SENT,
            HistoryLog.TIMESTAMP_DELIVERED,
            HistoryLog.TIMESTAMP_DISPLAYED};
    // @formatter:on

    private Uri mUri;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mProvider = getInstrumentation().getTargetContext().getContentResolver()
                .acquireContentProviderClient(HistoryLog.CONTENT_URI);
        assertNotNull(mProvider);
        mUri = getUriWithAllInternalProviders();
    }

    private Uri getUriWithAllInternalProviders() {
        return createHistoryUri(ChatLog.Message.HISTORYLOG_MEMBER_ID,
                ChatLog.GroupChat.HISTORYLOG_MEMBER_ID, FileTransferLog.HISTORYLOG_MEMBER_ID,
                VideoSharingLog.HISTORYLOG_MEMBER_ID, ImageSharingLog.HISTORYLOG_MEMBER_ID,
                GeolocSharingLog.HISTORYLOG_MEMBER_ID);
    }

    private static Uri createHistoryUri(int... providerIds) {
        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);

        for (int providerId : providerIds) {
            uriBuilder.appendProvider(providerId);
        }

        return uriBuilder.build();
    }

    public void testHistoryLogQuery() throws RemoteException {
        /* Check that provider handles columns names and query operation */
        Cursor cursor = null;
        try {
            String where = HistoryLog.ID.concat("=?");
            String[] whereArgs = new String[]{
                    "123456789"
            };
            cursor = mProvider.query(mUri, HISTORY_LOG_PROJECTION, where,
                    whereArgs, null);
            assertNotNull(cursor);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testHistoryLogSharingLogQueryById() throws RemoteException {
        /* Check that provider handles columns names and query operation */
        Uri uri = Uri.withAppendedPath(mUri, "123456789");
        Cursor cursor = null;
        try {
            cursor = mProvider.query(uri, HISTORY_LOG_PROJECTION, null, null, null);
            assertNotNull(cursor);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testHistoryLogQueryWithoutWhereClause() throws RemoteException {
        Cursor cursor = null;
        try {
            cursor = mProvider.query(mUri, null, null, null, null);
            assertNotNull(cursor);
            Utils.checkProjection(HISTORY_LOG_PROJECTION, cursor.getColumnNames());

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testHistoryLogInsert() {
        // // Check that provider does not support insert operation
        ContentValues values = new ContentValues();
        values.put(HistoryLog.FILESIZE, 10000);
        values.put(HistoryLog.FILENAME, "filename");
        values.put(HistoryLog.DIRECTION, RcsService.Direction.INCOMING.toInt());
        values.put(HistoryLog.MIME_TYPE, "image/jpeg");
        values.put(HistoryLog.REASON_CODE, FileTransfer.ReasonCode.UNSPECIFIED.toInt());
        values.put(HistoryLog.READ_STATUS, RcsService.ReadStatus.UNREAD.toInt());
        values.put(HistoryLog.TIMESTAMP, System.currentTimeMillis());
        values.put(HistoryLog.TIMESTAMP_DELIVERED, 0);
        values.put(HistoryLog.TIMESTAMP_SENT, 0);
        values.put(HistoryLog.TIMESTAMP_DISPLAYED, 0);
        values.put(HistoryLog.TRANSFERRED, 0);
        values.put(HistoryLog.CHAT_ID, "0102030405");

        try {
            mProvider.insert(mUri, values);
            fail("HistoryLog is read only");

        } catch (Exception ex) {
            assertTrue("insert into HistoryLog should be forbidden",
                    ex instanceof RuntimeException);
        }
    }

    public void testHistoryLogDelete() {
        // Check that provider supports delete operation
        try {
            mProvider.delete(mUri, null, null);
            fail("HistoryLog is read only");

        } catch (Exception ex) {
            assertTrue("delete of HistoryLog should be forbidden",
                    ex instanceof RuntimeException);
        }
    }

    public void testHistoryLogUpdate() {
        ContentValues values = new ContentValues();
        values.put(HistoryLog.TIMESTAMP, System.currentTimeMillis());
        // Check that provider does not support update operation
        try {
            String where = HistoryLog.ID.concat("=?");
            String[] whereArgs = new String[]{
                    "123456789"
            };
            mProvider.update(mUri, values, where, whereArgs);
            fail("HistoryLog is read only");

        } catch (Exception ex) {
            assertTrue("update of HistoryLog should be forbidden",
                    ex instanceof RuntimeException);
        }
    }

}
