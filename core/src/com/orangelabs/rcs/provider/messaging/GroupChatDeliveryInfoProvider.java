/*
 * Copyright (C) 2014 Sony Mobile Communications AB.
 * All rights, including trade secret rights, reserved.
 */
package com.orangelabs.rcs.provider.messaging;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Delivery info history provider of chat and file messages
 */
public class GroupChatDeliveryInfoProvider extends ContentProvider {

    public static final String DATABASE_TABLE = "deliveryinfo";

    private static final int DELIVERY_INFO = 1;

    private static final int MESSAGE_ID = 2;

    private static final int ROW_ID = 3;

    private static final String DATABASE_NAME = "groupchatdeliveryinfo.db";

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + DATABASE_TABLE + " (" + GroupChatDeliveryInfoData.KEY_ID
                    + " integer primary key autoincrement," + GroupChatDeliveryInfoData.KEY_CHAT_ID
                    + " text," + GroupChatDeliveryInfoData.KEY_MSG_ID + " text,"
                    + GroupChatDeliveryInfoData.KEY_CONTACT + " text,"
                    + GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS + " integer,"
                    + GroupChatDeliveryInfoData.KEY_REASON_CODE + " integer,"
                    + GroupChatDeliveryInfoData.KEY_TIMESTAMP_DELIVERED + " long,"
                    + GroupChatDeliveryInfoData.KEY_TIMESTAMP_DISPLAYED + " long);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }

    private SQLiteOpenHelper openHelper;

    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("com.gsma.services.rcs.provider.groupchatdeliveryinfo", "deliveryinfo",
                DELIVERY_INFO);
        uriMatcher.addURI("com.gsma.services.rcs.provider.groupchatdeliveryinfo",
                "deliveryinfo/messageid/*", MESSAGE_ID);
        uriMatcher.addURI("com.gsma.services.rcs.provider.groupchatdeliveryinfo",
                "deliveryinfo/rowid/*", ROW_ID);
    }

    private int updateDatabase(Uri uri, ContentValues values, String selection, String[] selectionArgs,
            String key) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        if (selection == null) {
            String finalSelection = new StringBuilder(key).append("=?").toString();
            return db.update(DATABASE_TABLE, values, finalSelection, new String[] {
                uri.getLastPathSegment()
            });
        }
        List<String> selectionArgsList = new ArrayList<String>(selectionArgs.length + 1);
        selectionArgsList.add(uri.getLastPathSegment());
        selectionArgsList.addAll(Arrays.asList(selectionArgs));
        String finalSelection = new StringBuilder(key).append("=? AND (").append(selection)
                .append(")").toString();
        return db.update(DATABASE_TABLE, values, finalSelection,
                selectionArgsList.toArray(new String[selectionArgsList.size()]));
    }

    private int updateDatabase(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case DELIVERY_INFO:
                SQLiteDatabase db = openHelper.getWritableDatabase();
                return db.update(DATABASE_TABLE, values, selection, selectionArgs);
            case MESSAGE_ID:
                return updateDatabase(uri, values, selection, selectionArgs,
                        GroupChatDeliveryInfoData.KEY_MSG_ID);
            case ROW_ID:
                return updateDatabase(uri, values, selection, selectionArgs,
                        GroupChatDeliveryInfoData.KEY_ID);
            default:
                throw new SQLException(new StringBuilder("Cannot update URI: '").append(uri)
                        .append("'!").toString());
        }
    }

    private int deleteFromDatabase(Uri uri, String selection, String[] selectionArgs, String key) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        if (selection == null) {
            String finalSelection = new StringBuilder(key).append("=?").toString();
            return db.delete(DATABASE_TABLE, finalSelection, new String[] {
                uri.getLastPathSegment()
            });
        }
        List<String> selectionArgsList = new ArrayList<String>(selectionArgs.length + 1);
        selectionArgsList.add(uri.getLastPathSegment());
        selectionArgsList.addAll(Arrays.asList(selectionArgs));
        String finalSelection = new StringBuilder(key).append("=? AND (").append(selection)
                .append(")").toString();
        return db.delete(DATABASE_TABLE, finalSelection, selectionArgsList.toArray(new String[selectionArgsList.size()]));
    }

    private int deleteFromDatabase(Uri uri, String selection, String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case DELIVERY_INFO:
                SQLiteDatabase db = openHelper.getWritableDatabase();
                return db.delete(DATABASE_TABLE, selection, selectionArgs);
            case MESSAGE_ID:
                return deleteFromDatabase(uri, selection, selectionArgs, GroupChatDeliveryInfoData.KEY_MSG_ID);
            case ROW_ID:
                return deleteFromDatabase(uri, selection, selectionArgs, GroupChatDeliveryInfoData.KEY_ID);
            default:
                throw new SQLException(new StringBuilder("Failed to delete row : '").append(uri)
                        .append("'!").toString());
        }
    }

    private String buildKeyedSelection(String selectionKey, String selection) {
        StringBuilder selectionKeyBuilder = new StringBuilder(selectionKey).append("=?");
        if (selection == null) {
            return selectionKeyBuilder.toString();
        }

        return selectionKeyBuilder.append(" AND (").append(selection).append(")").toString();
    }

    private String getUriSelectionKey(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case DELIVERY_INFO:
                return null;
            case MESSAGE_ID:
                return GroupChatDeliveryInfoData.KEY_MSG_ID;
            case ROW_ID:
                return GroupChatDeliveryInfoData.KEY_ID;
            default:
                throw new SQLException(new StringBuilder("Unknown URI '")
                        .append(uri.toString()).append("'!").toString());
        }
    }

    @Override
    public boolean onCreate() {
        openHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case DELIVERY_INFO:
                return "vnd.android.cursor.dir/com.gsma.services.rcs.provider.groupchatdeliveryinfo";
            case MESSAGE_ID:
                return "vnd.android.cursor.dir/com.gsma.services.rcs.provider.groupchatdeliveryinfo";
            case ROW_ID:
                return "vnd.android.cursor.item/com.gsma.services.rcs.provider.groupchatdeliveryinfo";
            default:
                throw new IllegalArgumentException("Unsupported URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sort) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(DATABASE_TABLE);

        String uriSelectionKey = getUriSelectionKey(uri);
        Cursor cursor;
        SQLiteDatabase database = openHelper.getReadableDatabase();
        if (uriSelectionKey == null) {
            cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null,
                    sort);

        } else if (selectionArgs == null) {
            String finalSelection = buildKeyedSelection(uriSelectionKey, selection);
            cursor = queryBuilder.query(database, projection, finalSelection, new String[] {
                uri.getLastPathSegment()
            }, null, null, sort);

        } else {
            List<String> selectionArgsList = new ArrayList<String>(selectionArgs.length + 1);
            selectionArgsList.add(uri.getLastPathSegment());
            selectionArgsList.addAll(Arrays.asList(selectionArgs));
            String finalSelection = buildKeyedSelection(uriSelectionKey, selection);
            cursor = queryBuilder.query(database, projection, finalSelection,
                    selectionArgsList.toArray(new String[selectionArgsList.size()]), null, null,
                    sort);
        }

        cursor.setNotificationUri(getContext().getContentResolver(),
                GroupChatDeliveryInfoData.CONTENT_URI);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        switch (uriMatcher.match(uri)) {
            case DELIVERY_INFO:
            case MESSAGE_ID:
            case ROW_ID:
                SQLiteDatabase db = openHelper.getWritableDatabase();
                long rowId = db.insert(DATABASE_TABLE, null, initialValues);
                Uri rowUri = ContentUris.withAppendedId(GroupChatDeliveryInfoData.CONTENT_ROW_URI, rowId);
                getContext().getContentResolver().notifyChange(uri, null);
                return rowUri;
            default:
                throw new SQLException(
                        new StringBuilder("Failed to insert row into: '").append(uri.toString())
                                .append("'!").toString());
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = updateDatabase(uri, values, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = deleteFromDatabase(uri, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }
}
