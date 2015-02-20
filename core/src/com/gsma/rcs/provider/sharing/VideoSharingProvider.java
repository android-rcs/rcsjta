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

package com.gsma.rcs.provider.sharing;

import com.gsma.rcs.provider.history.HistoryMemberBaseIdCreator;
import com.gsma.rcs.utils.DatabaseUtils;
import com.gsma.services.rcs.sharing.video.VideoSharingLog;

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
 * Video sharing provider
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingProvider extends ContentProvider {

    public static final String TABLE = "videoshare";

    private static final String SELECTION_WITH_SHARING_ID_ONLY = VideoSharingData.KEY_SHARING_ID
            .concat("=?");

    /**
     * Database name
     */
    public static final String DATABASE_NAME = "videoshare.db";

    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(VideoSharingLog.CONTENT_URI.getAuthority(), VideoSharingLog.CONTENT_URI
                .getPath().substring(1), UriType.VIDEO_SHARING);
        sUriMatcher.addURI(VideoSharingLog.CONTENT_URI.getAuthority(), VideoSharingLog.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.VIDEO_SHARING_WITH_ID);
    }

    private static final class UriType {

        private static final int VIDEO_SHARING = 1;

        private static final int VIDEO_SHARING_WITH_ID = 2;
    }

    private static final class CursorType {

        private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/videoshare";

        private static final String TYPE_ITEM = "vnd.android.cursor.item/videoshare";
    }

    /**
     * Helper class for opening, creating and managing db version control
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 6;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(TABLE).append("(")
                    .append(VideoSharingData.KEY_BASECOLUMN_ID).append(" INTEGER NOT NULL,")
                    .append(VideoSharingData.KEY_SHARING_ID).append(" TEXT NOT NULL PRIMARY KEY,")
                    .append(VideoSharingData.KEY_CONTACT).append(" TEXT NOT NULL,")
                    .append(VideoSharingData.KEY_STATE).append(" INTEGER NOT NULL,")
                    .append(VideoSharingData.KEY_REASON_CODE).append(" INTEGER NOT NULL,")
                    .append(VideoSharingData.KEY_DIRECTION).append(" INTEGER NOT NULL,")
                    .append(VideoSharingData.KEY_TIMESTAMP).append(" INTEGER NOT NULL,")
                    .append(VideoSharingData.KEY_DURATION).append(" INTEGER NOT NULL,")
                    .append(VideoSharingData.KEY_VIDEO_ENCODING).append(" TEXT,")
                    .append(VideoSharingData.KEY_WIDTH).append(" INTEGER NOT NULL,")
                    .append(VideoSharingData.KEY_HEIGHT).append(" INTEGER NOT NULL)").toString());
            db.execSQL(new StringBuilder("CREATE INDEX ").append(VideoSharingData.KEY_BASECOLUMN_ID)
                    .append("_idx").append(" ON ").append(TABLE).append("(")
                    .append(VideoSharingData.KEY_BASECOLUMN_ID).append(")").toString());
            db.execSQL(new StringBuilder("CREATE INDEX ").append(VideoSharingData.KEY_CONTACT)
                    .append("_idx").append(" ON ").append(TABLE).append("(")
                    .append(VideoSharingData.KEY_CONTACT).append(")").toString());
            db.execSQL(new StringBuilder("CREATE INDEX ").append(VideoSharingData.KEY_TIMESTAMP)
                    .append("_idx").append(" ON ").append(TABLE).append("(")
                    .append(VideoSharingData.KEY_TIMESTAMP).append(")").toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE));
            onCreate(db);
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
            case UriType.VIDEO_SHARING:
                return CursorType.TYPE_DIRECTORY;

            case UriType.VIDEO_SHARING_WITH_ID:
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
                case UriType.VIDEO_SHARING_WITH_ID:
                    String sharingId = uri.getLastPathSegment();
                    selection = getSelectionWithSharingId(selection);
                    selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                    /* Intentional fall through */
                case UriType.VIDEO_SHARING:
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
            case UriType.VIDEO_SHARING_WITH_ID:
                String sharingId = uri.getLastPathSegment();
                selection = getSelectionWithSharingId(selection);
                selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                /* Intentional fall through */
            case UriType.VIDEO_SHARING:
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
            case UriType.VIDEO_SHARING:
                /* Intentional fall through */
            case UriType.VIDEO_SHARING_WITH_ID:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                String sharingId = initialValues.getAsString(VideoSharingData.KEY_SHARING_ID);
                initialValues.put(VideoSharingData.KEY_BASECOLUMN_ID,
                        HistoryMemberBaseIdCreator.createUniqueId(getContext(), VideoSharingData.HISTORYLOG_MEMBER_ID));
                db.insert(TABLE, null, initialValues);
                Uri notificationUri = Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, sharingId);
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
            case UriType.VIDEO_SHARING_WITH_ID:
                String sharingId = uri.getLastPathSegment();
                selection = getSelectionWithSharingId(selection);
                selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                /* Intentional fall through */
            case UriType.VIDEO_SHARING:
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
