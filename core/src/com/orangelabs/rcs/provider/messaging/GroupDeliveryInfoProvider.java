/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */

package com.orangelabs.rcs.provider.messaging;

import com.gsma.services.rcs.GroupDeliveryInfoLog;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Group Delivery info provider of chat and file messages
 */
public class GroupDeliveryInfoProvider extends ContentProvider {

    private static final String DATABASE_TABLE = "groupdeliveryinfo";

    private static final String SELECTION_WITH_ID_ONLY = GroupDeliveryInfoData.KEY_ID
            .concat("=?");

    private static final String DATABASE_NAME = "groupdeliveryinfo.db";

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(GroupDeliveryInfoLog.CONTENT_URI.getAuthority(),
                GroupDeliveryInfoLog.CONTENT_URI.getPath().substring(1), UriType.DELIVERY);
        sUriMatcher.addURI(GroupDeliveryInfoLog.CONTENT_URI.getAuthority(), GroupDeliveryInfoLog.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.DELIVERY_WITH_ID);
    }

    private static final class UriType {

        private static final int DELIVERY = 1;

        private static final int DELIVERY_WITH_ID = 2;
    }

    private static final class CursorType {

        private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/com.gsma.services.rcs.provider.groupdeliveryinfo";

        private static final String TYPE_ITEM = "vnd.android.cursor.item/com.gsma.services.rcs.provider.groupdeliveryinfo";
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private static final int DATABASE_VERSION = 3;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(DATABASE_TABLE).append("(")
                    .append(GroupDeliveryInfoData.KEY_CHAT_ID).append(" TEXT NOT NULL,")
                    .append(GroupDeliveryInfoData.KEY_ID).append(" TEXT NOT NULL,")
                    .append(GroupDeliveryInfoData.KEY_CONTACT).append(" TEXT NOT NULL,")
                    .append(GroupDeliveryInfoData.KEY_DELIVERY_STATUS).append(" INTEGER NOT NULL,")
                    .append(GroupDeliveryInfoData.KEY_REASON_CODE).append(" INTEGER NOT NULL,")
                    .append(GroupDeliveryInfoData.KEY_TIMESTAMP_DELIVERED).append(" INTEGER NOT NULL,")
                    .append(GroupDeliveryInfoData.KEY_TIMESTAMP_DISPLAYED)
                    .append(" INTEGER NOT NULL, PRIMARY KEY(").append(GroupDeliveryInfoData.KEY_ID)
                    .append(",").append(GroupDeliveryInfoData.KEY_CONTACT).append("));")
                    .toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            db.execSQL("DROP TABLE IF EXISTS ".concat(DATABASE_TABLE));
            onCreate(db);
        }
    }

    private SQLiteOpenHelper mOpenHelper;

    private String getSelectionWithAppendedId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_ID_ONLY;
        }
        return new StringBuilder("(").append(SELECTION_WITH_ID_ONLY).append(") AND (")
                .append(selection).append(")").toString();
    }

    private String[] getSelectionArgsWithAppendedId(String[] selectionArgs, String appendedId) {
        String[] appendedIdSelectionArg = new String[] {
                appendedId
        };
        if (selectionArgs == null) {
            return appendedIdSelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(appendedIdSelectionArg, selectionArgs);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.DELIVERY:
                return CursorType.TYPE_ITEM;

            case UriType.DELIVERY_WITH_ID:
                return CursorType.TYPE_DIRECTORY;

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
                case UriType.DELIVERY_WITH_ID:
                    String appendedId = uri.getLastPathSegment();
                    selection = getSelectionWithAppendedId(selection);
                    selectionArgs = getSelectionArgsWithAppendedId(selectionArgs, appendedId);
                    /* Intentional fall through */
                case UriType.DELIVERY:
                    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(DATABASE_TABLE, projection, selection, selectionArgs,
                            null, null, sort);
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
    public Uri insert(Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.DELIVERY:
                /* Intentional fall through */
            case UriType.DELIVERY_WITH_ID:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                String appendedId = initialValues.getAsString(GroupDeliveryInfoData.KEY_ID);
                db.insert(DATABASE_TABLE, null, initialValues);
                Uri notificationUri = Uri.withAppendedPath(GroupDeliveryInfoLog.CONTENT_URI,
                        appendedId);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.DELIVERY_WITH_ID:
                String appendedId = uri.getLastPathSegment();
                selection = getSelectionWithAppendedId(selection);
                selectionArgs = getSelectionArgsWithAppendedId(selectionArgs, appendedId);
                /* Intentional fall through */
            case UriType.DELIVERY:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.update(DATABASE_TABLE, values, selection, selectionArgs);
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
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.DELIVERY_WITH_ID:
                String appendedId = uri.getLastPathSegment();
                selection = getSelectionWithAppendedId(selection);
                selectionArgs = getSelectionArgsWithAppendedId(selectionArgs, appendedId);
                /* Intentional fall through */
            case UriType.DELIVERY:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.delete(DATABASE_TABLE, selection, selectionArgs);
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
