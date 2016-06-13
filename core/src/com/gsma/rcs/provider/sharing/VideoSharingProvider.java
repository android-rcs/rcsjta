/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.history.HistoryMemberBaseIdCreator;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
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
import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * Video sharing provider
 * 
 * @author Jean-Marc AUFFRET
 */
@SuppressWarnings("ConstantConditions")
public class VideoSharingProvider extends ContentProvider {

    private static final int INVALID_ROW_ID = -1;

    private static final String SELECTION_WITH_SHARING_ID_ONLY = VideoSharingData.KEY_SHARING_ID
            .concat("=?");

    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(VideoSharingData.CONTENT_URI.getAuthority(),
                VideoSharingData.CONTENT_URI.getPath().substring(1),
                UriType.InternalVideoSharing.VIDEO_SHARING);
        sUriMatcher.addURI(VideoSharingData.CONTENT_URI.getAuthority(),
                VideoSharingData.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.InternalVideoSharing.VIDEO_SHARING_WITH_ID);
        sUriMatcher.addURI(VideoSharingLog.CONTENT_URI.getAuthority(), VideoSharingLog.CONTENT_URI
                .getPath().substring(1), UriType.VideoSharing.VIDEO_SHARING);
        sUriMatcher.addURI(VideoSharingLog.CONTENT_URI.getAuthority(), VideoSharingLog.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.VideoSharing.VIDEO_SHARING_WITH_ID);
    }

    /**
     * Table name
     */
    public static final String TABLE = "videoshare";

    /**
     * Database name
     */
    public static final String DATABASE_NAME = "videoshare.db";

    private static final class UriType {

        private static final class VideoSharing {
            private static final int VIDEO_SHARING = 1;

            private static final int VIDEO_SHARING_WITH_ID = 2;
        }

        private static final class InternalVideoSharing {
            private static final int VIDEO_SHARING = 3;

            private static final int VIDEO_SHARING_WITH_ID = 4;
        }
    }

    private static final class CursorType {

        private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/videoshare";

        private static final String TYPE_ITEM = "vnd.android.cursor.item/videoshare";
    }

    /**
     * Helper class for opening, creating and managing db version control
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 7;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // @formatter:off
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + '('
                    + VideoSharingData.KEY_BASECOLUMN_ID + " INTEGER NOT NULL,"
                    + VideoSharingData.KEY_SHARING_ID + " TEXT NOT NULL PRIMARY KEY,"
                    + VideoSharingData.KEY_CONTACT + " TEXT NOT NULL,"
                    + VideoSharingData.KEY_STATE + " INTEGER NOT NULL,"
                    + VideoSharingData.KEY_REASON_CODE + " INTEGER NOT NULL,"
                    + VideoSharingData.KEY_DIRECTION + " INTEGER NOT NULL,"
                    + VideoSharingData.KEY_TIMESTAMP + " INTEGER NOT NULL,"
                    + VideoSharingData.KEY_DURATION + " INTEGER NOT NULL,"
                    + VideoSharingData.KEY_VIDEO_ENCODING + " TEXT,"
                    + VideoSharingData.KEY_WIDTH + " INTEGER NOT NULL,"
                    + VideoSharingData.KEY_HEIGHT + " INTEGER NOT NULL)");
            // @formatter:on
            db.execSQL("CREATE INDEX " + VideoSharingData.KEY_BASECOLUMN_ID + "_idx" + " ON "
                    + TABLE + '(' + VideoSharingData.KEY_BASECOLUMN_ID + ')');
            db.execSQL("CREATE INDEX " + VideoSharingData.KEY_CONTACT + "_idx" + " ON " + TABLE
                    + '(' + VideoSharingData.KEY_CONTACT + ')');
            db.execSQL("CREATE INDEX " + VideoSharingData.KEY_TIMESTAMP + "_idx" + " ON " + TABLE
                    + '(' + VideoSharingData.KEY_TIMESTAMP + ')');
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
        return "(" + SELECTION_WITH_SHARING_ID_ONLY + ") AND (" + selection + ')';
    }

    private String[] getSelectionArgsWithSharingId(String[] selectionArgs, String sharingId) {
        return DatabaseUtils.appendIdWithSelectionArgs(sharingId, selectionArgs);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalVideoSharing.VIDEO_SHARING:
                /* Intentional fall through */
            case UriType.VideoSharing.VIDEO_SHARING:
                return CursorType.TYPE_DIRECTORY;

            case UriType.InternalVideoSharing.VIDEO_SHARING_WITH_ID:
                /* Intentional fall through */
            case UriType.VideoSharing.VIDEO_SHARING_WITH_ID:
                return CursorType.TYPE_ITEM;

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sort) {
        Cursor cursor = null;
        try {
            switch (sUriMatcher.match(uri)) {
                case UriType.InternalVideoSharing.VIDEO_SHARING_WITH_ID:
                    String sharingId = uri.getLastPathSegment();
                    selection = getSelectionWithSharingId(selection);
                    selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                    cursor = db
                            .query(TABLE, projection, selection, selectionArgs, null, null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, sharingId));
                    return cursor;

                case UriType.InternalVideoSharing.VIDEO_SHARING:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db
                            .query(TABLE, projection, selection, selectionArgs, null, null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            VideoSharingLog.CONTENT_URI);
                    return cursor;

                case UriType.VideoSharing.VIDEO_SHARING_WITH_ID:
                    sharingId = uri.getLastPathSegment();
                    selection = getSelectionWithSharingId(selection);
                    selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                    /* Intentional fall through */
                    //$FALL-THROUGH$
                case UriType.VideoSharing.VIDEO_SHARING:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db
                            .query(TABLE, projection, selection, selectionArgs, null, null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;

                default:
                    throw new IllegalArgumentException("Unsupported URI " + uri + "!");
            }
        } /*
           * TODO: Do not catch, close cursor, and then throw same exception. Callers should handle
           * exception.
           */
        catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        Uri notificationUri = VideoSharingLog.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalVideoSharing.VIDEO_SHARING_WITH_ID:
                String sharingId = uri.getLastPathSegment();
                selection = getSelectionWithSharingId(selection);
                selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                notificationUri = Uri.withAppendedPath(notificationUri, sharingId);
                /* Intentional fall through */
                //$FALL-THROUGH$
            case UriType.InternalVideoSharing.VIDEO_SHARING:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.update(TABLE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(notificationUri, null);
                }
                return count;

            case UriType.VideoSharing.VIDEO_SHARING_WITH_ID:
                /* Intentional fall through */
            case UriType.VideoSharing.VIDEO_SHARING:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalVideoSharing.VIDEO_SHARING:
                /* Intentional fall through */
            case UriType.InternalVideoSharing.VIDEO_SHARING_WITH_ID:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                String sharingId = initialValues.getAsString(VideoSharingData.KEY_SHARING_ID);
                initialValues.put(VideoSharingData.KEY_BASECOLUMN_ID, HistoryMemberBaseIdCreator
                        .createUniqueId(getContext(), VideoSharingData.HISTORYLOG_MEMBER_ID));
                if (db.insert(TABLE, null, initialValues) == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException("Unable to insert row for URI "
                            + uri.toString() + '!');
                }
                Uri notificationUri = Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, sharingId);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            case UriType.VideoSharing.VIDEO_SHARING_WITH_ID:
                /* Intentional fall through */
            case UriType.VideoSharing.VIDEO_SHARING:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        Uri notificationUri = VideoSharingLog.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalVideoSharing.VIDEO_SHARING_WITH_ID:
                String sharingId = uri.getLastPathSegment();
                selection = getSelectionWithSharingId(selection);
                selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                notificationUri = Uri.withAppendedPath(notificationUri, sharingId);
                /* Intentional fall through */
                //$FALL-THROUGH$
            case UriType.InternalVideoSharing.VIDEO_SHARING:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.delete(TABLE, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(notificationUri, null);
                }
                return count;

            case UriType.VideoSharing.VIDEO_SHARING_WITH_ID:
                /* Intentional fall through */
            case UriType.VideoSharing.VIDEO_SHARING:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");
            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

}
