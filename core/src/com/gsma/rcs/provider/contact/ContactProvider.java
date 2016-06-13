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

package com.gsma.rcs.provider.contact;

import com.gsma.rcs.provider.ContentProviderBaseIdCreator;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.contact.ContactData.AggregationData;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.DatabaseUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.capability.CapabilitiesLog;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * RCS Contact provider <br>
 * This provider contains the list of the RCS contacts and their status <br>
 * It is used by the AddressBookManager to keep the synchronization between the native address book
 * and the RCS contacts. <br>
 * It also contains the list of aggregations between native raw contacts and RCS raw contacts
 */
@SuppressWarnings("ConstantConditions")
public class ContactProvider extends ContentProvider {

    private static final int INVALID_ROW_ID = -1;

    private static final String CAPABILITY_TABLE = "capability";

    private static final String AGGREGATION_TABLE = "aggregation";

    /**
     * Database filename
     */
    public static final String DATABASE_NAME = "capability.db";

    private static final String RCS_CONTACT_SELECTION_WITH_CONTACT_ONLY = ContactData.KEY_CONTACT
            .concat("=?");

    private static final String AGGREGATION_DATA_SELECTION_WITH_ID_ONLY = AggregationData.KEY_ID
            .concat("=?");

    private final static String[] PHOTO_DATA_PROJECTION = new String[] {
        ContactData.KEY_PRESENCE_PHOTO_DATA
    };

    private static final String FILENAME_PREFIX = "photoData";

    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        sUriMatcher.addURI(ContactData.CONTENT_URI.getAuthority(), ContactData.CONTENT_URI
                .getPath().substring(1), UriType.InternalContacts.INTERNAL_CONTACTS);
        sUriMatcher.addURI(ContactData.CONTENT_URI.getAuthority(), ContactData.CONTENT_URI
                .getPath().substring(1).concat("/*"),
                UriType.InternalContacts.INTERNAL_CONTACTS_WITH_ID);
        sUriMatcher.addURI(AggregationData.CONTENT_URI.getAuthority(), AggregationData.CONTENT_URI
                .getPath().substring(1), UriType.Aggregation.AGGREGATION);
        sUriMatcher.addURI(AggregationData.CONTENT_URI.getAuthority(), AggregationData.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.Aggregation.AGGREGATION_WITH_ID);
        sUriMatcher.addURI(CapabilitiesLog.CONTENT_URI.getAuthority(), CapabilitiesLog.CONTENT_URI
                .getPath().substring(1), UriType.Contacts.CONTACTS);
        sUriMatcher.addURI(CapabilitiesLog.CONTENT_URI.getAuthority(), CapabilitiesLog.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.Contacts.CONTACTS_WITH_ID);
    }

    /**
     * String to allow projection for exposed URI to a set of columns
     */
    private static final String[] COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS = new String[] {
            ContactData.KEY_BASECOLUMN_ID, ContactData.KEY_CONTACT,
            ContactData.KEY_CAPABILITY_IMAGE_SHARE, ContactData.KEY_CAPABILITY_VIDEO_SHARE,
            ContactData.KEY_CAPABILITY_IM_SESSION, ContactData.KEY_CAPABILITY_FILE_TRANSFER,
            ContactData.KEY_CAPABILITY_GEOLOC_PUSH, ContactData.KEY_CAPABILITY_EXTENSIONS,
            ContactData.KEY_AUTOMATA, ContactData.KEY_CAPABILITY_TIMESTAMP_LAST_RESPONSE
    };

    private static final Set<String> COLUMNS_SET_ALLOWED_FOR_EXTERNAL_ACCESS = new HashSet<>(
            Arrays.asList(COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS));

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final class UriType {

        private static final class Contacts {

            private static final int CONTACTS = 1;

            private static final int CONTACTS_WITH_ID = 2;
        }

        private static final class Aggregation {

            private static final int AGGREGATION = 3;

            private static final int AGGREGATION_WITH_ID = 4;
        }

        private static final class InternalContacts {

            private static final int INTERNAL_CONTACTS = 5;

            private static final int INTERNAL_CONTACTS_WITH_ID = 6;
        }
    }

    private static final class CursorType {

        private static final class Contacts {

            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/eab";

            private static final String TYPE_ITEM = "vnd.android.cursor.item/eab";
        }

        private static final class Aggregation {

            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/aggregation";

            private static final String TYPE_ITEM = "vnd.android.cursor.item/aggregation";
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private static final int DATABASE_VERSION = 28;

        private void createDb(SQLiteDatabase db) {
            // @formatter:off
            db.execSQL("CREATE TABLE IF NOT EXISTS " + CAPABILITY_TABLE + '('
                    + ContactData.KEY_CONTACT + " TEXT NOT NULL PRIMARY KEY,"
                    + ContactData.KEY_BASECOLUMN_ID + " INTEGER NOT NULL,"
                    + ContactData.KEY_DISPLAY_NAME + " TEXT,"
                    + ContactData.KEY_RCS_STATUS + " INTEGER NOT NULL,"
                    + ContactData.KEY_RCS_STATUS_TIMESTAMP + " INTEGER NOT NULL,"
                    + ContactData.KEY_REGISTRATION_STATE + " INTEGER NOT NULL,"
                    + ContactData.KEY_PRESENCE_SHARING_STATUS + " TEXT,"
                    + ContactData.KEY_PRESENCE_FREE_TEXT + " TEXT,"
                    + ContactData.KEY_PRESENCE_WEBLINK_NAME + " TEXT,"
                    + ContactData.KEY_PRESENCE_WEBLINK_URL + " TEXT,"
                    + ContactData.KEY_PRESENCE_PHOTO_EXIST_FLAG + " TEXT,"
                    + ContactData.KEY_PRESENCE_PHOTO_ETAG + " TEXT,"
                    + ContactData.KEY_PRESENCE_PHOTO_DATA + " TEXT,"
                    + ContactData.KEY_PRESENCE_GEOLOC_EXIST_FLAG + " TEXT,"
                    + ContactData.KEY_PRESENCE_GEOLOC_LATITUDE + " REAL,"
                    + ContactData.KEY_PRESENCE_GEOLOC_LONGITUDE + " REAL,"
                    + ContactData.KEY_PRESENCE_GEOLOC_ALTITUDE + " REAL,"
                    + ContactData.KEY_PRESENCE_TIMESTAMP + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_TIMESTAMP_LAST_REQUEST + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_CS_VIDEO + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_IMAGE_SHARE + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_VIDEO_SHARE + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_IM_SESSION + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_FILE_TRANSFER + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_PRESENCE_DISCOVERY + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_SOCIAL_PRESENCE + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_GEOLOC_PUSH + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_FILE_TRANSFER_HTTP + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_FILE_TRANSFER_THUMBNAIL + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_IP_VOICE_CALL + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_IP_VIDEO_CALL + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_FILE_TRANSFER_SF + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_GROUP_CHAT_SF + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_EXTENSIONS + " TEXT,"
                    + ContactData.KEY_BLOCKED + " INTEGER NOT NULL,"
                    + ContactData.KEY_BLOCKING_TIMESTAMP + " INTEGER NOT NULL,"
                    + ContactData.KEY_TIMESTAMP_CONTACT_UPDATED + " INTEGER NOT NULL,"
                    + ContactData.KEY_AUTOMATA + " INTEGER NOT NULL,"
                    + ContactData.KEY_CAPABILITY_TIMESTAMP_LAST_RESPONSE + " INTEGER NOT NULL)");
            // @formatter:on
            db.execSQL("CREATE INDEX " + ContactData.KEY_BASECOLUMN_ID + "_idx" + " ON "
                    + CAPABILITY_TABLE + '(' + ContactData.KEY_BASECOLUMN_ID + ')');
            db.execSQL("CREATE TABLE IF NOT EXISTS " + AGGREGATION_TABLE + '('
                    + AggregationData.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + AggregationData.KEY_RCS_NUMBER + " TEXT NOT NULL,"
                    + AggregationData.KEY_RAW_CONTACT_ID + " INTEGER NOT NULL,"
                    + AggregationData.KEY_RCS_RAW_CONTACT_ID + " INTEGER NOT NULL)");
        }

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createDb(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS ".concat(CAPABILITY_TABLE));
            db.execSQL("DROP TABLE IF EXISTS ".concat(AGGREGATION_TABLE));
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    private String getSelectionWithContact(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return RCS_CONTACT_SELECTION_WITH_CONTACT_ONLY;
        }
        return "(" + RCS_CONTACT_SELECTION_WITH_CONTACT_ONLY + ") AND (" + selection + ')';
    }

    private String[] getSelectionArgsWithContact(String[] selectionArgs, String contact) {
        return DatabaseUtils.appendIdWithSelectionArgs(contact, selectionArgs);
    }

    private String getSelectionWithAggregationDataId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return AGGREGATION_DATA_SELECTION_WITH_ID_ONLY;
        }
        return "(" + AGGREGATION_DATA_SELECTION_WITH_ID_ONLY + ") AND (" + selection + ')';
    }

    private String[] getSelectionArgsWithAggregationId(String[] selectionArgs,
            String aggregationDataId) {
        return DatabaseUtils.appendIdWithSelectionArgs(aggregationDataId, selectionArgs);
    }

    private String[] restrictProjectionToExternallyDefinedColumns(String[] projection)
            throws UnsupportedOperationException {
        if (projection == null || projection.length == 0) {
            return COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS;
        }
        for (String projectedColumn : projection) {
            if (!COLUMNS_SET_ALLOWED_FOR_EXTERNAL_ACCESS.contains(projectedColumn)) {
                throw new UnsupportedOperationException("No visibility to the accessed column "
                        + projectedColumn + "!");
            }
        }
        return projection;
    }

    private ParcelFileDescriptor openPhotoDataFile(Uri uri, String mode)
            throws FileNotFoundException {
        Cursor cursor = null;
        try {
            cursor = query(uri, PHOTO_DATA_PROJECTION, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, uri);
            if (!cursor.moveToFirst()) {
                throw new FileNotFoundException("No item found for URI " + uri + "!");
            }
            String path = cursor.getString(cursor
                    .getColumnIndexOrThrow(ContactData.KEY_PRESENCE_PHOTO_DATA));
            if (path == null) {
                throw new FileNotFoundException("No photo is defined for URI " + uri + "!");
            }
            return ParcelFileDescriptor.open(new File(path), DatabaseUtils.parseMode(mode));

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalContacts.INTERNAL_CONTACTS:
                /* Intentional fall through */
            case UriType.Contacts.CONTACTS:
                return CursorType.Contacts.TYPE_DIRECTORY;

            case UriType.InternalContacts.INTERNAL_CONTACTS_WITH_ID:
                /* Intentional fall through */
            case UriType.Contacts.CONTACTS_WITH_ID:
                return CursorType.Contacts.TYPE_ITEM;

            case UriType.Aggregation.AGGREGATION:
                return CursorType.Aggregation.TYPE_DIRECTORY;

            case UriType.Aggregation.AGGREGATION_WITH_ID:
                return CursorType.Aggregation.TYPE_ITEM;

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalContacts.INTERNAL_CONTACTS:
                /* Intentional fall through */
            case UriType.InternalContacts.INTERNAL_CONTACTS_WITH_ID:
                Context context = getContext();
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                String contact = initialValues.getAsString(ContactData.KEY_CONTACT);
                initialValues.put(ContactData.KEY_BASECOLUMN_ID, ContentProviderBaseIdCreator
                        .createUniqueId(getContext(), ContactData.CONTENT_URI));
                if (db.insert(CAPABILITY_TABLE, null, initialValues) == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException("Unable to insert row for URI "
                            + uri.toString() + '!');
                }
                if (!initialValues.containsKey(ContactData.KEY_PRESENCE_PHOTO_DATA)) {
                    try {
                        String filename = FILENAME_PREFIX.concat(contact);
                        /*
                         * Creating a empty file to get the path for the photo data
                         */
                        context.openFileOutput(filename, Context.MODE_PRIVATE).close();
                        String path = context.getFileStreamPath(filename).getAbsolutePath();
                        initialValues.put(ContactData.KEY_PRESENCE_PHOTO_DATA, path);
                        initialValues.put(ContactData.KEY_PRESENCE_PHOTO_EXIST_FLAG,
                                ContactData.FALSE_VALUE);
                    } catch (IOException e) {
                        /*
                         * As Social Presence Profile Picture is optional for a RCS user, So if
                         * there is an issue with creating an empty file for same, We should still
                         * proceed with updating user capability information.
                         */
                        if (logger.isActivated()) {
                            logger.debug(e.getMessage());
                        }
                    }
                }
                db.update(CAPABILITY_TABLE, initialValues, RCS_CONTACT_SELECTION_WITH_CONTACT_ONLY,
                        getSelectionArgsWithContact(null, contact));
                Uri notificationUri = Uri.withAppendedPath(CapabilitiesLog.CONTENT_URI, contact);
                context.getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            case UriType.Contacts.CONTACTS_WITH_ID:
                /* Intentional fall through */
            case UriType.Contacts.CONTACTS:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            case UriType.Aggregation.AGGREGATION:
                /* Intentional fall through */
            case UriType.Aggregation.AGGREGATION_WITH_ID:
                db = mOpenHelper.getWritableDatabase();
                long rowID = db.insert(AGGREGATION_TABLE, null, initialValues);
                notificationUri = ContentUris.withAppendedId(AggregationData.CONTENT_URI, rowID);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

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
                case UriType.InternalContacts.INTERNAL_CONTACTS_WITH_ID:
                    String contact = uri.getLastPathSegment();
                    selection = getSelectionWithContact(selection);
                    selectionArgs = getSelectionArgsWithContact(selectionArgs, contact);
                    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(CAPABILITY_TABLE, projection, selection, selectionArgs, null,
                            null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            Uri.withAppendedPath(CapabilitiesLog.CONTENT_URI, contact));
                    return cursor;

                case UriType.InternalContacts.INTERNAL_CONTACTS:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(CAPABILITY_TABLE, projection, selection, selectionArgs, null,
                            null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            CapabilitiesLog.CONTENT_URI);
                    return cursor;

                case UriType.Contacts.CONTACTS_WITH_ID:
                    contact = uri.getLastPathSegment();
                    selection = getSelectionWithContact(selection);
                    selectionArgs = getSelectionArgsWithContact(selectionArgs, contact);
                    /* Intentional fall through */
                    //$FALL-THROUGH$
                case UriType.Contacts.CONTACTS:
                    /* Limited access with exposed URI */
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(CAPABILITY_TABLE,
                            restrictProjectionToExternallyDefinedColumns(projection), selection,
                            selectionArgs, null, null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;

                case UriType.Aggregation.AGGREGATION_WITH_ID:
                    String aggregationDataid = uri.getLastPathSegment();
                    selection = getSelectionWithAggregationDataId(selection);
                    selectionArgs = getSelectionArgsWithAggregationId(selectionArgs,
                            aggregationDataid);
                    /* Intentional fall through */
                    //$FALL-THROUGH$
                case UriType.Aggregation.AGGREGATION:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(AGGREGATION_TABLE, projection, selection, selectionArgs,
                            null, null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;

                default:
                    throw new IllegalArgumentException("Unsupported URI " + uri + "!");
            }
        } /*
           * TODO: Do not catch, close cursor, and then throw same exception. Callers should handle
           * exception.
           */catch (RuntimeException e) {
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
            case UriType.InternalContacts.INTERNAL_CONTACTS_WITH_ID:
                String contact = uri.getLastPathSegment();
                selection = getSelectionWithContact(selection);
                selectionArgs = getSelectionArgsWithContact(selectionArgs, contact);
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.update(CAPABILITY_TABLE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(
                            Uri.withAppendedPath(CapabilitiesLog.CONTENT_URI, contact), null);
                }
                return count;

            case UriType.InternalContacts.INTERNAL_CONTACTS:
                db = mOpenHelper.getWritableDatabase();
                count = db.update(CAPABILITY_TABLE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(CapabilitiesLog.CONTENT_URI,
                            null);
                }
                return count;

            case UriType.Contacts.CONTACTS_WITH_ID:
                /* Intentional fall through */
            case UriType.Contacts.CONTACTS:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            case UriType.Aggregation.AGGREGATION_WITH_ID:
                String aggregationDataid = uri.getLastPathSegment();
                selection = getSelectionWithAggregationDataId(selection);
                selectionArgs = getSelectionArgsWithAggregationId(selectionArgs, aggregationDataid);
                /* Intentional fall through */
                //$FALL-THROUGH$
            case UriType.Aggregation.AGGREGATION:
                db = mOpenHelper.getWritableDatabase();
                count = db.update(AGGREGATION_TABLE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return count;

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalContacts.INTERNAL_CONTACTS_WITH_ID:
                String contact = uri.getLastPathSegment();
                selection = getSelectionWithContact(selection);
                selectionArgs = getSelectionArgsWithContact(selectionArgs, contact);
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.delete(CAPABILITY_TABLE, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(
                            Uri.withAppendedPath(CapabilitiesLog.CONTENT_URI, contact), null);
                }
                return count;

            case UriType.InternalContacts.INTERNAL_CONTACTS:
                db = mOpenHelper.getWritableDatabase();
                count = db.delete(CAPABILITY_TABLE, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(CapabilitiesLog.CONTENT_URI,
                            null);
                }
                return count;

            case UriType.Contacts.CONTACTS_WITH_ID:
                /* Intentional fall through */
            case UriType.Contacts.CONTACTS:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            case UriType.Aggregation.AGGREGATION_WITH_ID:
                String aggregationDataId = uri.getLastPathSegment();
                selection = getSelectionWithAggregationDataId(selection);
                selectionArgs = getSelectionArgsWithAggregationId(selectionArgs, aggregationDataId);
                /* Intentional fall through */
                //$FALL-THROUGH$
            case UriType.Aggregation.AGGREGATION:
                db = mOpenHelper.getWritableDatabase();
                count = db.delete(AGGREGATION_TABLE, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return count;

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        switch (sUriMatcher.match(uri)) {
            case UriType.Contacts.CONTACTS_WITH_ID:
                /* Intentional fall through */
            case UriType.InternalContacts.INTERNAL_CONTACTS_WITH_ID:
                return openPhotoDataFile(uri, mode);

            case UriType.InternalContacts.INTERNAL_CONTACTS:
                /* Intentional fall through */
            case UriType.Contacts.CONTACTS:
                /* Intentional fall through */
            case UriType.Aggregation.AGGREGATION:
                /* Intentional fall through */
            case UriType.Aggregation.AGGREGATION_WITH_ID:
                throw new UnsupportedOperationException(
                        "Opening file stream is not supported for URI " + uri + "!");
            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }
}
