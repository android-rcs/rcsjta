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
import com.gsma.services.rcs.sharing.image.ImageSharingLog;

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
 * Image sharing provider
 * 
 * @author Jean-Marc AUFFRET
 */
@SuppressWarnings("ConstantConditions")
public class ImageSharingProvider extends ContentProvider {

    private static final int INVALID_ROW_ID = -1;

    private static final String SELECTION_WITH_SHARING_ID_ONLY = ImageSharingData.KEY_SHARING_ID
            .concat("=?");

    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(ImageSharingData.CONTENT_URI.getAuthority(),
                ImageSharingData.CONTENT_URI.getPath().substring(1),
                UriType.InternalImageSharing.IMAGE_SHARING);
        sUriMatcher.addURI(ImageSharingData.CONTENT_URI.getAuthority(),
                ImageSharingData.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.InternalImageSharing.IMAGE_SHARING_WITH_ID);
        sUriMatcher.addURI(ImageSharingLog.CONTENT_URI.getAuthority(), ImageSharingLog.CONTENT_URI
                .getPath().substring(1), UriType.ImageSharing.IMAGE_SHARING);
        sUriMatcher.addURI(ImageSharingLog.CONTENT_URI.getAuthority(), ImageSharingLog.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.ImageSharing.IMAGE_SHARING_WITH_ID);
    }

    /**
     * Table name
     */
    public static final String TABLE = "imageshare";

    /**
     * Database name
     */
    public static final String DATABASE_NAME = "imageshare.db";

    private static final class UriType {

        private static final class ImageSharing {
            private static final int IMAGE_SHARING = 1;

            private static final int IMAGE_SHARING_WITH_ID = 2;
        }

        private static final class InternalImageSharing {
            private static final int IMAGE_SHARING = 3;

            private static final int IMAGE_SHARING_WITH_ID = 4;
        }
    }

    private static final class CursorType {

        private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/imageshare";

        private static final String TYPE_ITEM = "vnd.android.cursor.item/imageshare";
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 6;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // @formatter:off
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + '('
                    + ImageSharingData.KEY_BASECOLUMN_ID + " INTEGER NOT NULL,"
                    + ImageSharingData.KEY_SHARING_ID + " TEXT NOT NULL PRIMARY KEY,"
                    + ImageSharingData.KEY_CONTACT + " TEXT NOT NULL," 
                    + ImageSharingData.KEY_FILE + " TEXT NOT NULL," 
                    + ImageSharingData.KEY_FILENAME + " TEXT NOT NULL,"
                    + ImageSharingData.KEY_MIME_TYPE + " TEXT NOT NULL,"
                    + ImageSharingData.KEY_STATE + " INTEGER NOT NULL,"
                    + ImageSharingData.KEY_REASON_CODE + " INTEGER NOT NULL,"
                    + ImageSharingData.KEY_DIRECTION + " INTEGER NOT NULL,"
                    + ImageSharingData.KEY_TIMESTAMP + " INTEGER NOT NULL,"
                    + ImageSharingData.KEY_TRANSFERRED + " INTEGER NOT NULL,"
                    + ImageSharingData.KEY_FILESIZE + " INTEGER NOT NULL)");
            // @formatter:on
            db.execSQL("CREATE INDEX " + ImageSharingData.KEY_BASECOLUMN_ID + "_idx" + " ON "
                    + TABLE + '(' + ImageSharingData.KEY_BASECOLUMN_ID + ')');
            db.execSQL("CREATE INDEX " + ImageSharingData.KEY_CONTACT + "_idx" + " ON " + TABLE
                    + '(' + ImageSharingData.KEY_CONTACT + ')');
            db.execSQL("CREATE INDEX " + ImageSharingData.KEY_TIMESTAMP + "_idx" + " ON " + TABLE
                    + '(' + ImageSharingData.KEY_TIMESTAMP + ')');
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
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalImageSharing.IMAGE_SHARING:
                /* Intentional fall through */
            case UriType.ImageSharing.IMAGE_SHARING:
                return CursorType.TYPE_DIRECTORY;

            case UriType.InternalImageSharing.IMAGE_SHARING_WITH_ID:
                /* Intentional fall through */
            case UriType.ImageSharing.IMAGE_SHARING_WITH_ID:
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
                case UriType.InternalImageSharing.IMAGE_SHARING_WITH_ID:
                    String sharingId = uri.getLastPathSegment();
                    selection = getSelectionWithSharingId(selection);
                    selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                    cursor = db
                            .query(TABLE, projection, selection, selectionArgs, null, null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            Uri.withAppendedPath(ImageSharingLog.CONTENT_URI, sharingId));
                    return cursor;

                case UriType.InternalImageSharing.IMAGE_SHARING:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db
                            .query(TABLE, projection, selection, selectionArgs, null, null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            ImageSharingLog.CONTENT_URI);
                    return cursor;

                case UriType.ImageSharing.IMAGE_SHARING_WITH_ID:
                    sharingId = uri.getLastPathSegment();
                    selection = getSelectionWithSharingId(selection);
                    selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                    /* Intentional fall through */
                    //$FALL-THROUGH$
                case UriType.ImageSharing.IMAGE_SHARING:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db
                            .query(TABLE, projection, selection, selectionArgs, null, null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;

                default:
                    throw new IllegalArgumentException("Unsupported URI " + uri + "!");
            }
        }
        /*
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
        Uri notificationUri = ImageSharingLog.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalImageSharing.IMAGE_SHARING_WITH_ID:
                String sharingId = uri.getLastPathSegment();
                selection = getSelectionWithSharingId(selection);
                selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                notificationUri = Uri.withAppendedPath(notificationUri, sharingId);
                /* Intentional fall through */
                //$FALL-THROUGH$
            case UriType.InternalImageSharing.IMAGE_SHARING:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.update(TABLE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(notificationUri, null);
                }
                return count;

            case UriType.ImageSharing.IMAGE_SHARING_WITH_ID:
                /* Intentional fall through */
            case UriType.ImageSharing.IMAGE_SHARING:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalImageSharing.IMAGE_SHARING:
                /* Intentional fall through */
            case UriType.InternalImageSharing.IMAGE_SHARING_WITH_ID:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                String sharingId = initialValues.getAsString(ImageSharingData.KEY_SHARING_ID);
                initialValues.put(ImageSharingData.KEY_BASECOLUMN_ID, HistoryMemberBaseIdCreator
                        .createUniqueId(getContext(), ImageSharingData.HISTORYLOG_MEMBER_ID));
                if (db.insert(TABLE, null, initialValues) == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException("Unable to insert row for URI "
                            + uri.toString() + '!');
                }
                Uri notificationUri = Uri.withAppendedPath(ImageSharingLog.CONTENT_URI, sharingId);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            case UriType.ImageSharing.IMAGE_SHARING_WITH_ID:
                /* Intentional fall through */
            case UriType.ImageSharing.IMAGE_SHARING:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        Uri notificationUri = ImageSharingLog.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalImageSharing.IMAGE_SHARING_WITH_ID:
                String sharingId = uri.getLastPathSegment();
                selection = getSelectionWithSharingId(selection);
                selectionArgs = getSelectionArgsWithSharingId(selectionArgs, sharingId);
                notificationUri = Uri.withAppendedPath(notificationUri, sharingId);
                /* Intentional fall through */
                //$FALL-THROUGH$
            case UriType.InternalImageSharing.IMAGE_SHARING:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.delete(TABLE, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(notificationUri, null);
                }
                return count;

            case UriType.ImageSharing.IMAGE_SHARING_WITH_ID:
                /* Intentional fall through */
            case UriType.ImageSharing.IMAGE_SHARING:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

}
