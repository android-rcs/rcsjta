/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provider.ipcall;

import com.gsma.rcs.provider.ContentProviderBaseIdCreator;
import com.gsma.rcs.service.ipcalldraft.IPCallLog;
import com.gsma.rcs.utils.DatabaseUtils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

/**
 * IP call history provider
 * 
 * @author owom5460
 */
public class IPCallProvider extends ContentProvider {

    private static final String TABLE = "ipcall";

    private static final String SELECTION_WITH_CALLID_ONLY = IPCallData.KEY_CALL_ID.concat("=?");

    public static final String DATABASE_NAME = "ipcall.db";

    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(IPCallLog.CONTENT_URI.getAuthority(), IPCallLog.CONTENT_URI.getPath()
                .substring(1), UriType.IPCALL);
        sUriMatcher.addURI(IPCallLog.CONTENT_URI.getAuthority(), IPCallLog.CONTENT_URI.getPath()
                .substring(1).concat("/*"), UriType.IPCALL_WITH_CALLID);
    }

    private static final class UriType {

        private static final int IPCALL = 1;

        private static final int IPCALL_WITH_CALLID = 2;
    }

    private static final class CursorType {

        private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/ipcall";

        private static final String TYPE_ITEM = "vnd.android.cursor.item/ipcall";
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 6;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(TABLE).append("(")
                    .append(IPCallData.KEY_CALL_ID).append(" TEXT NOT NULL PRIMARY KEY,")
                    .append(IPCallData.KEY_BASECOLUMN_ID).append(" INTEGER NOT NULL,")
                    .append(IPCallData.KEY_CONTACT).append(" TEXT NOT NULL,")
                    .append(IPCallData.KEY_STATE).append(" INTEGER NOT NULL,")
                    .append(IPCallData.KEY_REASON_CODE).append(" INTEGER NOT NULL,")
                    .append(IPCallData.KEY_DIRECTION).append(" INTEGER NOT NULL,")
                    .append(IPCallData.KEY_TIMESTAMP).append(" INTEGER NOT NULL,")
                    .append(IPCallData.KEY_VIDEO_ENCODING).append(" TEXT,")
                    .append(IPCallData.KEY_AUDIO_ENCODING).append(" TEXT,")
                    .append(IPCallData.KEY_WIDTH).append(" INTEGER NOT NULL,")
                    .append(IPCallData.KEY_HEIGHT).append(" INTEGER NOT NULL)").toString());
            db.execSQL(new StringBuilder("CREATE INDEX ").append(IPCallData.KEY_CONTACT)
                    .append("_idx").append(" ON ").append(TABLE).append("(")
                    .append(IPCallData.KEY_CONTACT).append(")").toString());
            db.execSQL(new StringBuilder("CREATE INDEX ").append(IPCallData.KEY_BASECOLUMN_ID)
                    .append("_idx").append(" ON ").append(TABLE).append("(")
                    .append(IPCallData.KEY_BASECOLUMN_ID).append(")").toString());
            db.execSQL(new StringBuilder("CREATE INDEX ").append(IPCallData.KEY_TIMESTAMP)
                    .append("_idx").append(" ON ").append(TABLE).append("(")
                    .append(IPCallData.KEY_TIMESTAMP).append(")").toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE));
            onCreate(db);
        }
    }

    private SQLiteOpenHelper mOpenHelper;

    private String getSelectionWithCallId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_CALLID_ONLY;
        }
        return new StringBuilder("(").append(SELECTION_WITH_CALLID_ONLY).append(") AND (")
                .append(selection).append(")").toString();
    }

    private String[] getSelectionArgsWithCallId(String[] selectionArgs, String callId) {
        String[] callSelectionArg = new String[] {
            callId
        };
        if (selectionArgs == null) {
            return callSelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(callSelectionArg, selectionArgs);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.IPCALL:
                return CursorType.TYPE_DIRECTORY;

            case UriType.IPCALL_WITH_CALLID:
                return CursorType.TYPE_ITEM;

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sort) {
        Cursor cursor = null;
        try {
            switch (sUriMatcher.match(uri)) {
                case UriType.IPCALL_WITH_CALLID:
                    String callId = uri.getLastPathSegment();
                    selection = getSelectionWithCallId(selection);
                    selectionArgs = getSelectionArgsWithCallId(selectionArgs, callId);
                    /* Intentional fall through */
                case UriType.IPCALL:
                    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                    cursor = db
                            .query(TABLE, projection, selection, selectionArgs, null, null, sort);
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;

                default:
                    throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                            .append(uri).append("!").toString());
            }
        } catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.IPCALL_WITH_CALLID:
                String callId = uri.getLastPathSegment();
                selection = getSelectionWithCallId(selection);
                selectionArgs = getSelectionArgsWithCallId(selectionArgs, callId);
                /* Intentional fall through */
            case UriType.IPCALL:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.update(TABLE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return count;

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.IPCALL:
                /* Intentional fall through */
            case UriType.IPCALL_WITH_CALLID:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                String callId = initialValues.getAsString(IPCallData.KEY_CALL_ID);
                initialValues.put(IPCallData.KEY_BASECOLUMN_ID, ContentProviderBaseIdCreator
                        .createUniqueId(getContext(), IPCallLog.CONTENT_URI));
                db.insert(TABLE, null, initialValues);
                Uri notificationUri = Uri.withAppendedPath(IPCallLog.CONTENT_URI, callId);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.IPCALL_WITH_CALLID:
                String callId = uri.getLastPathSegment();
                selection = getSelectionWithCallId(selection);
                selectionArgs = getSelectionArgsWithCallId(selectionArgs, callId);
                /* Intentional fall through */
            case UriType.IPCALL:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.delete(TABLE, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return count;

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }
}
