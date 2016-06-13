/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.provider.ContentProviderBaseIdCreator;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.DatabaseUtils;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfoLog;

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
 * Group Delivery info provider of chat and file messages
 */
@SuppressWarnings("ConstantConditions")
public class GroupDeliveryInfoProvider extends ContentProvider {

    private static final int INVALID_ROW_ID = -1;

    private static final String DATABASE_TABLE = "groupdeliveryinfo";

    private static final String SELECTION_WITH_ID_ONLY = GroupDeliveryInfoData.KEY_ID.concat("=?");

    public static final String DATABASE_NAME = "groupdeliveryinfo.db";

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(GroupDeliveryInfoData.CONTENT_URI.getAuthority(),
                GroupDeliveryInfoData.CONTENT_URI.getPath().substring(1),
                UriType.InternalGroupDeliveryInfo.DELIVERY);
        sUriMatcher.addURI(GroupDeliveryInfoData.CONTENT_URI.getAuthority(),
                GroupDeliveryInfoData.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.InternalGroupDeliveryInfo.DELIVERY_WITH_ID);
        sUriMatcher.addURI(GroupDeliveryInfoLog.CONTENT_URI.getAuthority(),
                GroupDeliveryInfoLog.CONTENT_URI.getPath().substring(1),
                UriType.GroupDeliveryInfo.DELIVERY);
        sUriMatcher.addURI(GroupDeliveryInfoLog.CONTENT_URI.getAuthority(),
                GroupDeliveryInfoLog.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.GroupDeliveryInfo.DELIVERY_WITH_ID);
    }

    private static final class UriType {

        private static final class GroupDeliveryInfo {
            private static final int DELIVERY = 1;

            private static final int DELIVERY_WITH_ID = 2;
        }

        private static final class InternalGroupDeliveryInfo {
            private static final int DELIVERY = 3;

            private static final int DELIVERY_WITH_ID = 4;
        }
    }

    private static final class CursorType {

        private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/com.gsma.services.rcs.provider.groupdeliveryinfo";

        private static final String TYPE_ITEM = "vnd.android.cursor.item/com.gsma.services.rcs.provider.groupdeliveryinfo";
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private static final int DATABASE_VERSION = 4;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // @formatter:off
            db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE + '('
                    + GroupDeliveryInfoData.KEY_CHAT_ID + " TEXT NOT NULL,"
                    + GroupDeliveryInfoData.KEY_BASECOLUMN_ID + " INTEGER NOT NULL,"
                    + GroupDeliveryInfoData.KEY_ID + " TEXT NOT NULL,"
                    + GroupDeliveryInfoData.KEY_CONTACT + " TEXT NOT NULL,"
                    + GroupDeliveryInfoData.KEY_STATUS + " INTEGER NOT NULL,"
                    + GroupDeliveryInfoData.KEY_REASON_CODE + " INTEGER NOT NULL,"
                    + GroupDeliveryInfoData.KEY_TIMESTAMP_DELIVERED + " INTEGER NOT NULL,"
                    + GroupDeliveryInfoData.KEY_TIMESTAMP_DISPLAYED + " INTEGER NOT NULL, "
                    + "PRIMARY KEY(" + GroupDeliveryInfoData.KEY_ID + ',' + GroupDeliveryInfoData.KEY_CONTACT + "))");
            // @formatter:on
            db.execSQL("CREATE INDEX " + GroupDeliveryInfoData.KEY_BASECOLUMN_ID + "_idx" + " ON "
                    + DATABASE_TABLE + '(' + GroupDeliveryInfoData.KEY_BASECOLUMN_ID + ')');
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
        return "(" + SELECTION_WITH_ID_ONLY + ") AND (" + selection + ')';
    }

    private String[] getSelectionArgsWithAppendedId(String[] selectionArgs, String appendedId) {
        return DatabaseUtils.appendIdWithSelectionArgs(appendedId, selectionArgs);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalGroupDeliveryInfo.DELIVERY:
                /* Intentional fall through */
            case UriType.GroupDeliveryInfo.DELIVERY:
                return CursorType.TYPE_ITEM;

            case UriType.InternalGroupDeliveryInfo.DELIVERY_WITH_ID:
                /* Intentional fall through */
            case UriType.GroupDeliveryInfo.DELIVERY_WITH_ID:
                return CursorType.TYPE_DIRECTORY;

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
                case UriType.InternalGroupDeliveryInfo.DELIVERY_WITH_ID:
                    String appendedId = uri.getLastPathSegment();
                    selection = getSelectionWithAppendedId(selection);
                    selectionArgs = getSelectionArgsWithAppendedId(selectionArgs, appendedId);
                    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(DATABASE_TABLE, projection, selection, selectionArgs, null,
                            null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            Uri.withAppendedPath(GroupDeliveryInfoLog.CONTENT_URI, appendedId));
                    return cursor;

                case UriType.InternalGroupDeliveryInfo.DELIVERY:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(DATABASE_TABLE, projection, selection, selectionArgs, null,
                            null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            GroupDeliveryInfoLog.CONTENT_URI);
                    return cursor;

                case UriType.GroupDeliveryInfo.DELIVERY_WITH_ID:
                    appendedId = uri.getLastPathSegment();
                    selection = getSelectionWithAppendedId(selection);
                    selectionArgs = getSelectionArgsWithAppendedId(selectionArgs, appendedId);
                    /* Intentional fall through */
                    //$FALL-THROUGH$
                case UriType.GroupDeliveryInfo.DELIVERY:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(DATABASE_TABLE, projection, selection, selectionArgs, null,
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
         */
        catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalGroupDeliveryInfo.DELIVERY:
                /* Intentional fall through */
            case UriType.InternalGroupDeliveryInfo.DELIVERY_WITH_ID:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                String appendedId = initialValues.getAsString(GroupDeliveryInfoData.KEY_ID);
                initialValues.put(GroupDeliveryInfoData.KEY_BASECOLUMN_ID,
                        ContentProviderBaseIdCreator.createUniqueId(getContext(),
                                GroupDeliveryInfoData.CONTENT_URI));
                if (db.insert(DATABASE_TABLE, null, initialValues) == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException("Unable to insert row for URI "
                            + uri.toString() + '!');
                }
                Uri notificationUri = Uri.withAppendedPath(GroupDeliveryInfoLog.CONTENT_URI,
                        appendedId);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            case UriType.GroupDeliveryInfo.DELIVERY:
                /* Intentional fall through */
            case UriType.GroupDeliveryInfo.DELIVERY_WITH_ID:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        Uri notificationUri = GroupDeliveryInfoLog.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalGroupDeliveryInfo.DELIVERY_WITH_ID:
                String appendedId = uri.getLastPathSegment();
                selection = getSelectionWithAppendedId(selection);
                selectionArgs = getSelectionArgsWithAppendedId(selectionArgs, appendedId);
                notificationUri = Uri.withAppendedPath(notificationUri, appendedId);
                /* Intentional fall through */
                //$FALL-THROUGH$
            case UriType.InternalGroupDeliveryInfo.DELIVERY:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.update(DATABASE_TABLE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(notificationUri, null);
                }
                return count;

            case UriType.GroupDeliveryInfo.DELIVERY_WITH_ID:
                /* Intentional fall through */
            case UriType.GroupDeliveryInfo.DELIVERY:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        Uri notificationUri = GroupDeliveryInfoLog.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalGroupDeliveryInfo.DELIVERY_WITH_ID:
                String appendedId = uri.getLastPathSegment();
                selection = getSelectionWithAppendedId(selection);
                selectionArgs = getSelectionArgsWithAppendedId(selectionArgs, appendedId);
                notificationUri = Uri.withAppendedPath(notificationUri, appendedId);
                /* Intentional fall through */
                //$FALL-THROUGH$
            case UriType.InternalGroupDeliveryInfo.DELIVERY:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.delete(DATABASE_TABLE, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(notificationUri, null);
                }
                return count;

            case UriType.GroupDeliveryInfo.DELIVERY_WITH_ID:
                /* Intentional fall through */
            case UriType.GroupDeliveryInfo.DELIVERY:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }
}
