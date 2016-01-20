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
 * http://www.apache.org/licenses/LICENSE-2.0
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

package com.gsma.rcs.provider.settings;

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.utils.DatabaseUtils;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Map;

/**
 * RCS settings provider
 *
 * @author jexa7410
 * @author yplo6403
 */
@SuppressWarnings("ConstantConditions")
public class RcsSettingsProvider extends ContentProvider {

    private static final String TABLE = "setting";

    private static final String SELECTION_WITH_KEY_ONLY = RcsSettingsData.KEY_KEY.concat("=?");

    /**
     * Database filename
     */
    public static final String DATABASE_NAME = "rcs_settings.db";

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(RcsSettingsData.CONTENT_URI.getAuthority(), RcsSettingsData.CONTENT_URI
                .getPath().substring(1), UriType.SETTINGS);
        sUriMatcher.addURI(RcsSettingsData.CONTENT_URI.getAuthority(), RcsSettingsData.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.SETTINGS_WITH_KEY);
    }

    private static final class UriType {

        private static final int SETTINGS = 1;

        private static final int SETTINGS_WITH_KEY = 2;
    }

    private static final class CursorType {

        private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/com.gsma.rcs.setting";

        private static final String TYPE_ITEM = "vnd.android.cursor.item/com.gsma.rcs.setting";
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 120;

        /**
         * Add a parameter in the db
         *
         * @param db Database
         * @param key Key
         * @param value Value
         */
        private void addParameter(SQLiteDatabase db, String key, String value) {
            ContentValues values = new ContentValues();
            values.put(RcsSettingsData.KEY_KEY, key);
            values.put(RcsSettingsData.KEY_VALUE, value);
            db.insertOrThrow(TABLE, null, values);
        }

        private void addParameter(SQLiteDatabase db, String key, Object value) {
            addParameter(db, key, value == null ? null : value.toString());
        }

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + '(' + RcsSettingsData.KEY_KEY
                    + " TEXT NOT NULL PRIMARY KEY," + RcsSettingsData.KEY_VALUE + " TEXT)");
            /* Insert default values for parameters */
            for (Map.Entry<String, Object> entry : RcsSettingsData.sSettingsKeyDefaultValue.entrySet()) {
                addParameter(db, entry.getKey(),  entry.getValue());
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            /* Get old data before deleting the table */
            Cursor oldDataCursor = db.query(TABLE, null, null, null, null, null, null);
            CursorUtil.assertCursorIsNotNull(oldDataCursor, TABLE);

            /*
             * Get all the pairs key/value of the old table to insert them back after update
             */
            ArrayList<ContentValues> valuesList = new ArrayList<>();
            while (oldDataCursor.moveToNext()) {
                String key = null;
                String value = null;
                int index = oldDataCursor.getColumnIndex(RcsSettingsData.KEY_KEY);
                if (index != -1) {
                    key = oldDataCursor.getString(index);
                }
                index = oldDataCursor.getColumnIndex(RcsSettingsData.KEY_VALUE);
                if (index != -1) {
                    value = oldDataCursor.getString(index);
                }
                if (key != null && value != null) {
                    ContentValues values = new ContentValues();
                    values.put(RcsSettingsData.KEY_KEY, key);
                    values.put(RcsSettingsData.KEY_VALUE, value);
                    valuesList.add(values);
                }
            }
            oldDataCursor.close();

            db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE));

            onCreate(db);

            /* Put the old values back when possible */
            for (ContentValues values : valuesList) {
                String[] selectionArgs = new String[]{
                        values.getAsString(RcsSettingsData.KEY_KEY)
                };
                db.update(TABLE, values, SELECTION_WITH_KEY_ONLY, selectionArgs);
            }
        }
    }

    private SQLiteOpenHelper mOpenHelper;

    private String getSelectionWithKey(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_KEY_ONLY;
        }
        return "(" + SELECTION_WITH_KEY_ONLY + ") AND (" + selection + ')';
    }

    private String[] getSelectionArgsWithKey(String[] selectionArgs, String key) {
        String[] keySelectionArg = new String[]{
                key
        };
        if (selectionArgs == null) {
            return keySelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(keySelectionArg, selectionArgs);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.SETTINGS:
                return CursorType.TYPE_DIRECTORY;

            case UriType.SETTINGS_WITH_KEY:
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
                case UriType.SETTINGS_WITH_KEY:
                    String key = uri.getLastPathSegment();
                    selection = getSelectionWithKey(selection);
                    selectionArgs = getSelectionArgsWithKey(selectionArgs, key);
                    /* Intentional fall through */
                    //$FALL-THROUGH$
                case UriType.SETTINGS:
                    SQLiteDatabase database = mOpenHelper.getReadableDatabase();
                    cursor = database.query(TABLE, projection, selection, selectionArgs, null,
                            null, sort);
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
         */ catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.SETTINGS_WITH_KEY:
                String key = uri.getLastPathSegment();
                selection = getSelectionWithKey(selection);
                selectionArgs = getSelectionArgsWithKey(selectionArgs, key);
                /* Intentional fall through */
                //$FALL-THROUGH$
            case UriType.SETTINGS:
                SQLiteDatabase database = mOpenHelper.getWritableDatabase();
                int count = database.update(TABLE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return count;

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        throw new UnsupportedOperationException("Cannot insert URI " + uri + "!");
    }

    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Cannot delete URI " + uri + "!");
    }

    @Override
    public ContentProviderResult[] applyBatch(
            @NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase database = mOpenHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            ContentProviderResult[] results = new ContentProviderResult[operations.size()];
            int index = 0;
            for (ContentProviderOperation operation : operations) {
                results[index] = operation.apply(this, results, index);
                index++;
            }
            database.setTransactionSuccessful();
            return results;
        } finally {
            database.endTransaction();
        }
    }
}
