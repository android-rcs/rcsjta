/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.provider.sharing;

import com.gsma.rcs.provider.history.HistoryMemberBaseIdCreator;
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

public class GeolocSharingProvider extends ContentProvider {

    public static final String TABLE = "geolocshare";

    private static final String SELECTION_WITH_SHARING_ID_ONLY = GeolocSharingData.KEY_SHARING_ID
            .concat("=?");

    private static final class UriType {

        private static final int BASE = 1;

        private static final int WITH_SHARING_ID = 2;
    }

    private static final class CursorType {

        private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/geolocshare";

        private static final String TYPE_ITEM = "vnd.android.cursor.item/geolocshare";
    }

    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(GeolocSharingData.CONTENT_URI.getAuthority(),
                GeolocSharingData.CONTENT_URI.getPath().substring(1), UriType.BASE);
        sUriMatcher.addURI(GeolocSharingData.CONTENT_URI.getAuthority(),
                GeolocSharingData.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.WITH_SHARING_ID);
    }

    public static final String DATABASE_NAME = "geolocshare.db";

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 3;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(TABLE)
                    .append(" (")
                    .append(GeolocSharingData.KEY_SHARING_ID).append(" TEXT NOT NULL PRIMARY KEY,")
                    .append(GeolocSharingData.KEY_BASECOLUMN_ID).append(" INTEGER NOT NULL,")
                    .append(GeolocSharingData.KEY_CONTACT)
                    .append(" TEXT NOT NULL,").append(GeolocSharingData.KEY_CONTENT)
                    .append(" TEXT,").append(GeolocSharingData.KEY_MIME_TYPE)
                    .append(" TEXT NOT NULL,").append(GeolocSharingData.KEY_DIRECTION)
                    .append(" INTEGER NOT NULL,").append(GeolocSharingData.KEY_STATE)
                    .append(" INTEGER NOT NULL,").append(GeolocSharingData.KEY_REASON_CODE)
                    .append(" INTEGER NOT NULL,").append(GeolocSharingData.KEY_TIMESTAMP)
                    .append(" INTEGER NOT NULL);").toString());
            database.execSQL(new StringBuilder("CREATE INDEX ")
                    .append(GeolocSharingData.KEY_BASECOLUMN_ID).append("_idx ON ").append(TABLE)
                    .append("(").append(GeolocSharingData.KEY_BASECOLUMN_ID).append(")").toString());
            database.execSQL(new StringBuilder("CREATE INDEX ")
                    .append(GeolocSharingData.KEY_CONTACT).append("_idx ON ").append(TABLE)
                    .append("(").append(GeolocSharingData.KEY_CONTACT).append(")").toString());
            database.execSQL(new StringBuilder("CREATE INDEX ")
                    .append(GeolocSharingData.KEY_TIMESTAMP).append("_idx ON ").append(TABLE)
                    .append("(").append(GeolocSharingData.KEY_TIMESTAMP).append(")").toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int currentVersion) {
            database.execSQL("DROP TABLE IF EXISTS " + TABLE);
            onCreate(database);
        }
    }

    private SQLiteOpenHelper mOpenHelper;

    private String getSelectionWithSharingId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_SHARING_ID_ONLY;
        }
        return new StringBuilder("(").append(SELECTION_WITH_SHARING_ID_ONLY).append(") AND (")
                .append(selection).append(")").toString();
    }

    private String[] getSelectionArgsWithSharingId(String[] selectionArgs, String sharingId) {
        String[] sharingSelectionArg = new String[] {
            sharingId
        };
        if (selectionArgs == null) {
            return sharingSelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(sharingSelectionArg, selectionArgs);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.BASE:
                return CursorType.TYPE_DIRECTORY;

            case UriType.WITH_SHARING_ID:
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
                case UriType.WITH_SHARING_ID:
                    String sharingId = uri.getLastPathSegment();
                    selection = getSelectionWithSharingId(selection);
                    selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                    /* Intentional fall through */
                case UriType.BASE:
                    SQLiteDatabase database = mOpenHelper.getReadableDatabase();
                    cursor = database.query(TABLE, projection, selection, selectionArgs, null,
                            null, sort);

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
            case UriType.WITH_SHARING_ID:
                String sharingId = uri.getLastPathSegment();
                selection = getSelectionWithSharingId(selection);
                selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                /* Intentional fall through */
            case UriType.BASE:
                SQLiteDatabase database = mOpenHelper.getWritableDatabase();
                int count = database.update(TABLE, values, selection, selectionArgs);
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
            case UriType.WITH_SHARING_ID:
                /* Intentional fall through */
            case UriType.BASE:
                SQLiteDatabase database = mOpenHelper.getWritableDatabase();
                String sharingId = initialValues.getAsString(GeolocSharingData.KEY_SHARING_ID);
                initialValues.put(GeolocSharingData.KEY_BASECOLUMN_ID,
                        HistoryMemberBaseIdCreator.createUniqueId(getContext(), GeolocSharingData.HISTORYLOG_MEMBER_ID));
                database.insert(TABLE, null, initialValues);
                Uri notificationUri = GeolocSharingData.CONTENT_URI.buildUpon()
                        .appendPath(sharingId).build();
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
            case UriType.WITH_SHARING_ID:
                String sharingId = uri.getLastPathSegment();
                selection = getSelectionWithSharingId(selection);
                selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                /* Intentional fall through */
            case UriType.BASE:
                SQLiteDatabase database = mOpenHelper.getWritableDatabase();
                int count = database.delete(TABLE, selection, selectionArgs);
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
