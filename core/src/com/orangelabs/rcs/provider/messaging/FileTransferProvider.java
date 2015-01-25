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
package com.orangelabs.rcs.provider.messaging;

import com.gsma.services.rcs.ft.FileTransferLog;
import com.orangelabs.rcs.provider.messaging.FileTransferData;
import com.orangelabs.rcs.utils.DatabaseUtils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * File transfer content provider
 *
 * @author Jean-Marc AUFFRET
 */
public class FileTransferProvider extends ContentProvider {

    private static final String TABLE = "filetransfer";

    private static final String SELECTION_WITH_FT_ID_ONLY = FileTransferData.KEY_FT_ID.concat("=?");

    private static final String DATABASE_NAME = "filetransfer.db";

    private static final class UriType {

        private static final class FileTransfer {

            private static final int FILE_TRANSFER = 1;

            private static final int FILE_TRANSFER_WITH_ID = 2;
        }

        private static final class InternalFileTransfer {

            private static final int FILE_TRANSFER = 3;

            private static final int FILE_TRANSFER_WITH_ID = 4;
        }
    }

    private static final class CursorType {

        private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/filetransfer";

        private static final String TYPE_ITEM = "vnd.android.cursor.item/filetransfer";
    }

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(FileTransferLog.CONTENT_URI.getAuthority(),
                FileTransferLog.CONTENT_URI.getPath().substring(1), UriType.FileTransfer.FILE_TRANSFER);
        sUriMatcher.addURI(FileTransferLog.CONTENT_URI.getAuthority(), FileTransferLog.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.FileTransfer.FILE_TRANSFER_WITH_ID);
        sUriMatcher.addURI(FileTransferData.CONTENT_URI.getAuthority(),
                FileTransferData.CONTENT_URI.getPath().substring(1), UriType.InternalFileTransfer.FILE_TRANSFER);
        sUriMatcher.addURI(FileTransferData.CONTENT_URI.getAuthority(),
                FileTransferData.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.InternalFileTransfer.FILE_TRANSFER_WITH_ID);
    }

    /**
     * Strings to restrict projection for exposed URI to a set of columns
     */
    private static final String[] RESTRICTED_PROJECTION_FOR_EXTERNALLY_DEFINED_COLUMNS = new String[] {
            FileTransferLog.FT_ID, FileTransferLog.CHAT_ID, FileTransferLog.CONTACT,
            FileTransferLog.FILE, FileTransferLog.FILENAME, FileTransferLog.MIME_TYPE,
            FileTransferLog.FILEICON, FileTransferLog.FILEICON_MIME_TYPE,
            FileTransferLog.DIRECTION, FileTransferLog.FILESIZE, FileTransferLog.TRANSFERRED,
            FileTransferLog.TIMESTAMP, FileTransferLog.TIMESTAMP_SENT,
            FileTransferLog.TIMESTAMP_DELIVERED, FileTransferLog.TIMESTAMP_DISPLAYED,
            FileTransferLog.STATE, FileTransferLog.REASON_CODE, FileTransferLog.READ_STATUS
    };

    /**
     * Columns that are not exposed through external URI
     */
    private static final String[] COLUMNS_HIDDEN_FOR_EXTERNAL_ACCESS = new String[] {
    	FileTransferData.KEY_UPLOAD_TID, FileTransferData.KEY_DOWNLOAD_URI
    };

    private static final Set<String> COLUMN_SET_HIDDEN_FOR_EXTERNAL_ACCESS = new HashSet<String>(
            Arrays.asList(COLUMNS_HIDDEN_FOR_EXTERNAL_ACCESS));
    
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 13;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(TABLE).append("(")
                    .append(FileTransferData.KEY_FT_ID).append(" TEXT NOT NULL PRIMARY KEY,")
                    .append(FileTransferData.KEY_CONTACT).append(" TEXT,")
                    .append(FileTransferData.KEY_FILE).append(" TEXT NOT NULL,")
                    .append(FileTransferData.KEY_FILENAME).append(" TEXT NOT NULL,")
                    .append(FileTransferData.KEY_CHAT_ID).append(" TEXT NOT NULL,")
                    .append(FileTransferData.KEY_MIME_TYPE).append(" TEXT NOT NULL,")
                    .append(FileTransferData.KEY_STATE).append(" INTEGER NOT NULL,")
                    .append(FileTransferData.KEY_REASON_CODE).append(" INTEGER NOT NULL,")
                    .append(FileTransferData.KEY_READ_STATUS).append(" INTEGER NOT NULL,")
                    .append(FileTransferData.KEY_DIRECTION).append(" INTEGER NOT NULL,")
                    .append(FileTransferData.KEY_TIMESTAMP).append(" INTEGER NOT NULL,")
                    .append(FileTransferData.KEY_TIMESTAMP_SENT).append(" INTEGER NOT NULL,")
                    .append(FileTransferData.KEY_TIMESTAMP_DELIVERED).append(" INTEGER NOT NULL,")
                    .append(FileTransferData.KEY_TIMESTAMP_DISPLAYED).append(" INTEGER NOT NULL,")
                    .append(FileTransferData.KEY_TRANSFERRED).append(" INTEGER NOT NULL,")
                    .append(FileTransferData.KEY_FILESIZE).append(" INTEGER NOT NULL,")
                    .append(FileTransferData.KEY_FILEICON).append(" TEXT,")
                    .append(FileTransferData.KEY_UPLOAD_TID).append(" TEXT,")
                    .append(FileTransferData.KEY_DOWNLOAD_URI).append(" TEXT,")
                    .append(FileTransferData.KEY_FILEICON_MIME_TYPE).append(" TEXT)").toString());
            db.execSQL(new StringBuilder("CREATE INDEX ").append(FileTransferData.KEY_CHAT_ID)
                    .append("_idx").append(" ON ").append(TABLE).append("(")
                    .append(FileTransferData.KEY_CHAT_ID).append(")").toString());
            db.execSQL(new StringBuilder("CREATE INDEX ").append(FileTransferData.KEY_TIMESTAMP)
                    .append("_idx").append(" ON ").append(TABLE).append("(")
                    .append(FileTransferData.KEY_TIMESTAMP).append(")").toString());
            db.execSQL(new StringBuilder("CREATE INDEX ")
                    .append(FileTransferData.KEY_TIMESTAMP_SENT).append("_idx").append(" ON ")
                    .append(TABLE).append("(").append(FileTransferData.KEY_TIMESTAMP_SENT)
                    .append(")").toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE));
            onCreate(db);
        }
    }

    private SQLiteOpenHelper mOpenHelper;

    private String getSelectionWithFtId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_FT_ID_ONLY;
        }
        return new StringBuilder("(").append(SELECTION_WITH_FT_ID_ONLY).append(") AND (")
                .append(selection).append(")").toString();
    }

    private String[] getSelectionArgsWithFtId(String[] selectionArgs, String ftId) {
        String[] ftSelectionArg = new String[] {
            ftId
        };
        if (selectionArgs == null) {
            return ftSelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(ftSelectionArg, selectionArgs);
    }

    private String[] restrictProjectionToExternallyDefinedColumns(String[] projection)
            throws UnsupportedOperationException {
        if (projection == null || projection.length == 0) {
            return RESTRICTED_PROJECTION_FOR_EXTERNALLY_DEFINED_COLUMNS;
        }
        for (String projectedColumn : projection) {
            if (COLUMN_SET_HIDDEN_FOR_EXTERNAL_ACCESS.contains(projectedColumn)) {
                throw new UnsupportedOperationException(new StringBuilder(
                        "No visibility to the accessed column ").append(projectedColumn)
                        .append("!").toString());
            }
        }
        return projection;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalFileTransfer.FILE_TRANSFER:
                /* Intentional fall through */
            case UriType.FileTransfer.FILE_TRANSFER:
                return CursorType.TYPE_DIRECTORY;

            case UriType.InternalFileTransfer.FILE_TRANSFER_WITH_ID:
                /* Intentional fall through */
            case UriType.FileTransfer.FILE_TRANSFER_WITH_ID:
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
        Uri notificationUri = FileTransferLog.CONTENT_URI;
        try {
            switch (sUriMatcher.match(uri)) {
                case UriType.InternalFileTransfer.FILE_TRANSFER_WITH_ID:
                    String ftId = uri.getLastPathSegment();
                    selection = getSelectionWithFtId(selection);
                    selectionArgs = getSelectionArgsWithFtId(selectionArgs, ftId);
                    notificationUri = Uri.withAppendedPath(notificationUri, ftId);
                    /* Intentional fall through */
                case UriType.InternalFileTransfer.FILE_TRANSFER:
                    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE, projection, selection, selectionArgs, null,
                            null, sort);
                    cursor.setNotificationUri(getContext().getContentResolver(), notificationUri);
                    return cursor;

                case UriType.FileTransfer.FILE_TRANSFER_WITH_ID:
                    ftId = uri.getLastPathSegment();
                    selection = getSelectionWithFtId(selection);
                    selectionArgs = getSelectionArgsWithFtId(selectionArgs, ftId);
                    /* Intentional fall through */
                case UriType.FileTransfer.FILE_TRANSFER:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE,
                            restrictProjectionToExternallyDefinedColumns(projection), selection,
                            selectionArgs, null, null, sort);
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
        Uri notificationUri = FileTransferLog.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalFileTransfer.FILE_TRANSFER_WITH_ID:
                String ftId = uri.getLastPathSegment();
                selection = getSelectionWithFtId(selection);
                selectionArgs = getSelectionArgsWithFtId(selectionArgs, ftId);
                notificationUri = Uri.withAppendedPath(notificationUri, ftId);
                /* Intentional fall through */
            case UriType.InternalFileTransfer.FILE_TRANSFER:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.update(TABLE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(notificationUri, null);
                }
                return count;

            case UriType.FileTransfer.FILE_TRANSFER_WITH_ID:
                /* Intentional fall through */
            case UriType.FileTransfer.FILE_TRANSFER:
                throw new UnsupportedOperationException(new StringBuilder("This provider (URI=")
                        .append(uri).append(") supports read only access.").toString());

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalFileTransfer.FILE_TRANSFER:
                /* Intentional fall through */
            case UriType.InternalFileTransfer.FILE_TRANSFER_WITH_ID:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                String ftId = initialValues.getAsString(FileTransferData.KEY_FT_ID);
                db.insert(TABLE, null, initialValues);
                Uri notificationUri = Uri.withAppendedPath(FileTransferLog.CONTENT_URI, ftId);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            case UriType.FileTransfer.FILE_TRANSFER_WITH_ID:
                /* Intentional fall through */
            case UriType.FileTransfer.FILE_TRANSFER:
                throw new UnsupportedOperationException(new StringBuilder("This provider (URI=")
                        .append(uri).append(") supports read only access.").toString());

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Uri notificationUri = FileTransferLog.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalFileTransfer.FILE_TRANSFER_WITH_ID:
                String ftId = uri.getLastPathSegment();
                selection = getSelectionWithFtId(selection);
                selectionArgs = getSelectionArgsWithFtId(selectionArgs, ftId);
                notificationUri = Uri.withAppendedPath(notificationUri, ftId);
                /* Intentional fall through */
            case UriType.InternalFileTransfer.FILE_TRANSFER:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.delete(TABLE, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(notificationUri, null);
                }
                return count;

            case UriType.FileTransfer.FILE_TRANSFER_WITH_ID:
                /* Intentional fall through */
            case UriType.FileTransfer.FILE_TRANSFER:
                throw new UnsupportedOperationException(new StringBuilder("This provider (URI=")
                        .append(uri).append(") supports read only access.").toString());

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }
}
