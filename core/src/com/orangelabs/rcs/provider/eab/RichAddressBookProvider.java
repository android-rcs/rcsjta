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

package com.orangelabs.rcs.provider.eab;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import com.gsma.services.rcs.capability.CapabilitiesLog;
import com.orangelabs.rcs.core.ims.service.ContactInfo;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Rich address book provider
 *
 * <br>This provider contains the list of the RCS contacts and their status
 * <br>It is used by the AddressBookManager to keep the synchronization between the native address book and the RCS contacts.
 * 
 * <br>It also contains the list of aggregations between native raw contacts and rcs raw contacts
 */
public class RichAddressBookProvider extends ContentProvider {

    private static final String CAPABILITY_TABLE = "capability";

    private static final String AGGREGATION_TABLE = "aggregation";

    private static final String RICH_ADDRESS_BOOK_SELECTION_WITH_CONTACT_ONLY = RichAddressBookData.KEY_CONTACT
            .concat("=?");

    private static final String AGGREGATION_DATA_SELECTION_WITH_ID_ONLY = AggregationData.KEY_ID.concat("=?");

    private final static String[] PHOTO_DATA_PROJECTION = new String[] { RichAddressBookData.KEY_PRESENCE_PHOTO_DATA };

    private static final String FILENAME_PREFIX = "photoData";

    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        sUriMatcher.addURI(RichAddressBookData.CONTENT_URI.getAuthority(),
                RichAddressBookData.CONTENT_URI.getPath().substring(1), UriType.InternalContacts.INTERNAL_CONTACTS);
        sUriMatcher.addURI(RichAddressBookData.CONTENT_URI.getAuthority(),
                RichAddressBookData.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.InternalContacts.INTERNAL_CONTACTS_WITH_ID);
        sUriMatcher.addURI(AggregationData.CONTENT_URI.getAuthority(),
                AggregationData.CONTENT_URI.getPath().substring(1), UriType.Aggregation.AGGREGATION);
        sUriMatcher.addURI(AggregationData.CONTENT_URI.getAuthority(), AggregationData.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.Aggregation.AGGREGATION_WITH_ID);
        sUriMatcher.addURI(CapabilitiesLog.CONTENT_URI.getAuthority(),
                CapabilitiesLog.CONTENT_URI.getPath().substring(1), UriType.Contacts.CONTACTS);
        sUriMatcher.addURI(CapabilitiesLog.CONTENT_URI.getAuthority(), CapabilitiesLog.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.Contacts.CONTACTS_WITH_ID);
    }

    /**
     * String to restrict projection for exposed URI to a set of columns
     */
    private static final String[] RESTRICTED_PROJECTION_FOR_EXTERNALLY_DEFINED_COLUMNS = new String[] {
            RichAddressBookData.KEY_CONTACT, RichAddressBookData.KEY_CAPABILITY_IMAGE_SHARING,
            RichAddressBookData.KEY_CAPABILITY_VIDEO_SHARING,
            RichAddressBookData.KEY_CAPABILITY_IM_SESSION,
            RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER,
            RichAddressBookData.KEY_CAPABILITY_GEOLOCATION_PUSH,
            RichAddressBookData.KEY_CAPABILITY_IP_VOICE_CALL,
            RichAddressBookData.KEY_CAPABILITY_IP_VIDEO_CALL,
            RichAddressBookData.KEY_CAPABILITY_EXTENSIONS, RichAddressBookData.KEY_AUTOMATA,
            RichAddressBookData.KEY_TIMESTAMP
    };

    private static final Set<String> RESTRICTED_PROJECTION_SET = new HashSet<String>(
            Arrays.asList(RESTRICTED_PROJECTION_FOR_EXTERNALLY_DEFINED_COLUMNS));

    private static final String RESTRICTED_SELECTION_QUERY_FOR_EXTERNALLY_DEFINED_COLUMNS = new StringBuilder(
            "(" + RichAddressBookData.KEY_RCS_STATUS).append("!=").append(ContactInfo.NO_INFO)
            .append(") AND (").append(RichAddressBookData.KEY_RCS_STATUS).append("!=")
            .append(ContactInfo.NOT_RCS).append(")").toString();

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

        private static final String DATABASE_NAME = "capability.db";

        private static final int DATABASE_VERSION = 24;

        private void createDb(SQLiteDatabase db) {
            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(CAPABILITY_TABLE).append("(")
                    .append(RichAddressBookData.KEY_CONTACT).append(" TEXT NOT NULL PRIMARY KEY,")
                    .append(RichAddressBookData.KEY_DISPLAY_NAME).append(" TEXT,")
                    .append(RichAddressBookData.KEY_RCS_STATUS).append(" TEXT,")
                    .append(RichAddressBookData.KEY_RCS_STATUS_TIMESTAMP).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_REGISTRATION_STATE).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_PRESENCE_SHARING_STATUS).append(" TEXT,")
                    .append(RichAddressBookData.KEY_PRESENCE_FREE_TEXT).append(" TEXT,")
                    .append(RichAddressBookData.KEY_PRESENCE_WEBLINK_NAME).append(" TEXT,")
                    .append(RichAddressBookData.KEY_PRESENCE_WEBLINK_URL).append(" TEXT,")
                    .append(RichAddressBookData.KEY_PRESENCE_PHOTO_EXIST_FLAG).append(" TEXT,")
                    .append(RichAddressBookData.KEY_PRESENCE_PHOTO_ETAG).append(" TEXT,")
                    .append(RichAddressBookData.KEY_PRESENCE_PHOTO_DATA).append(" TEXT,")
                    .append(RichAddressBookData.KEY_PRESENCE_GEOLOC_EXIST_FLAG).append(" TEXT,")
                    .append(RichAddressBookData.KEY_PRESENCE_GEOLOC_LATITUDE).append(" REAL,")
                    .append(RichAddressBookData.KEY_PRESENCE_GEOLOC_LONGITUDE).append(" REAL,")
                    .append(RichAddressBookData.KEY_PRESENCE_GEOLOC_ALTITUDE).append(" REAL,")
                    .append(RichAddressBookData.KEY_PRESENCE_TIMESTAMP).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_TIME_LAST_RQST).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_CS_VIDEO).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_IMAGE_SHARING).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_VIDEO_SHARING).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_IM_SESSION).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_PRESENCE_DISCOVERY).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_SOCIAL_PRESENCE).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_GEOLOCATION_PUSH).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER_HTTP).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER_THUMBNAIL).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_IP_VOICE_CALL).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_IP_VIDEO_CALL).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER_SF).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_GROUP_CHAT_SF).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_EXTENSIONS).append(" TEXT,")
                    .append(RichAddressBookData.KEY_IM_BLOCKED).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_FT_BLOCKED).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_CAPABILITY_IM_BLOCKED_TIMESTAMP).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_TIMESTAMP).append(" INTEGER,")
                    .append(RichAddressBookData.KEY_AUTOMATA).append(" TEXT,")
                    .append(RichAddressBookData.KEY_CAPABILITY_TIME_LAST_REFRESH).append(" INTEGER);").toString());
            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(AGGREGATION_TABLE).append("(")
                    .append(AggregationData.KEY_ID).append(" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,")
                    .append(AggregationData.KEY_RCS_NUMBER).append(" TEXT NOT NULL,")
                    .append(AggregationData.KEY_RAW_CONTACT_ID).append(" INTEGER NOT NULL,")
                    .append(AggregationData.KEY_RCS_RAW_CONTACT_ID).append(" INTEGER NOT NULL);").toString());
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
            return RICH_ADDRESS_BOOK_SELECTION_WITH_CONTACT_ONLY;
        }
        return new StringBuilder("(").append(RICH_ADDRESS_BOOK_SELECTION_WITH_CONTACT_ONLY).append(") AND (")
                .append(selection).append(")").toString();
    }

    private String[] getSelectionArgsWithContact(String[] selectionArgs, String contact) {
        String[] contactSelectionArg = new String[] {
            contact
        };
        if (selectionArgs == null) {
            return contactSelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(contactSelectionArg, selectionArgs);
    }

    private String getSelectionWithAggregationDataId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return AGGREGATION_DATA_SELECTION_WITH_ID_ONLY;
        }
        return new StringBuilder("(").append(AGGREGATION_DATA_SELECTION_WITH_ID_ONLY).append(") AND (")
                .append(selection).append(")").toString();
    }

    private String[] getSelectionArgsWithAggregationId(String[] selectionArgs,
            String aggregationDataId) {
        String[] idSelectionArg = new String[] {
            aggregationDataId
        };
        if (selectionArgs == null) {
            return idSelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(idSelectionArg, selectionArgs);
    }

    private String restrictSelectionToExternallyDefinedColumns(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return RESTRICTED_SELECTION_QUERY_FOR_EXTERNALLY_DEFINED_COLUMNS;
        }
        return new StringBuilder("(")
                .append(RESTRICTED_SELECTION_QUERY_FOR_EXTERNALLY_DEFINED_COLUMNS)
                .append(") AND (").append(selection).append(")").toString();
    }

    private String[] restrictProjectionToExternallyDefinedColumns(String[] projection)
            throws UnsupportedOperationException {
        if (projection == null || projection.length == 0) {
            return RESTRICTED_PROJECTION_FOR_EXTERNALLY_DEFINED_COLUMNS;
        }
        for (String projectedColumn : projection) {
            if (!RESTRICTED_PROJECTION_SET.contains(projectedColumn)) {
                throw new UnsupportedOperationException(new StringBuilder(
                        "No visibility to the accessed column ").append(projectedColumn)
                        .append("!").toString());
            }
        }
        return projection;
    }

    private ParcelFileDescriptor openPhotoDataFile(Uri uri, String mode)
            throws FileNotFoundException {
        Cursor cursor = null;
        try {
            cursor = query(uri, PHOTO_DATA_PROJECTION, null, null, null);
            if (!cursor.moveToFirst()) {
                throw new FileNotFoundException(new StringBuilder("No item found for URI ").append(uri)
                        .append("!").toString());
            }
            String path = cursor.getString(cursor
                    .getColumnIndexOrThrow(RichAddressBookData.KEY_PRESENCE_PHOTO_DATA));
            if (path == null) {
                throw new FileNotFoundException(new StringBuilder("No photo is defined for URI ").append(uri).append("!").toString());
            }
            return ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.parseMode(mode));

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
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
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalContacts.INTERNAL_CONTACTS:
                /* Intentional fall through */
            case UriType.InternalContacts.INTERNAL_CONTACTS_WITH_ID:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                String contact = initialValues.getAsString(RichAddressBookData.KEY_CONTACT);
                db.insert(CAPABILITY_TABLE, null, initialValues);
                if (!initialValues.containsKey(RichAddressBookData.KEY_PRESENCE_PHOTO_DATA)) {
                    try {
                        String filename = FILENAME_PREFIX.concat(contact);
                        /*
                         * Creating a empty file to get the path for the photo
                         * data
                         */
                        getContext().openFileOutput(filename, Context.MODE_PRIVATE).close();
                        String path = getContext().getFileStreamPath(filename).getAbsolutePath();
                        initialValues.put(RichAddressBookData.KEY_PRESENCE_PHOTO_DATA, path);
                        initialValues.put(RichAddressBookData.KEY_PRESENCE_PHOTO_EXIST_FLAG,
                                RichAddressBookData.FALSE_VALUE);

                    } catch (Exception e) {
                        /* TODO: Proper exception handling will be done in CR037 */
                        if (logger.isActivated()) {
                            logger.error("Problem while creating photoData", e);
                        }
                    }
                }
                db.update(CAPABILITY_TABLE, initialValues,
                        RICH_ADDRESS_BOOK_SELECTION_WITH_CONTACT_ONLY,
                        getSelectionArgsWithContact(null, contact));
                Uri notificationUri = Uri.withAppendedPath(CapabilitiesLog.CONTENT_URI, contact);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            case UriType.Contacts.CONTACTS_WITH_ID:
                /* Intentional fall through */
            case UriType.Contacts.CONTACTS:
                throw new UnsupportedOperationException(new StringBuilder("This provider (URI=")
                        .append(uri).append(") supports read only access.").toString());

            case UriType.Aggregation.AGGREGATION:
                /* Intentional fall through */
            case UriType.Aggregation.AGGREGATION_WITH_ID:
                db = mOpenHelper.getWritableDatabase();
                long rowID = db.insert(AGGREGATION_TABLE, null, initialValues);
                notificationUri = ContentUris.withAppendedId(AggregationData.CONTENT_URI, rowID);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sort) {
        Cursor cursor = null;
        Uri notificationUri = CapabilitiesLog.CONTENT_URI;
        try {
            switch (sUriMatcher.match(uri)) {
                case UriType.InternalContacts.INTERNAL_CONTACTS_WITH_ID:
                    String contact = uri.getLastPathSegment();
                    selection = getSelectionWithContact(selection);
                    selectionArgs = getSelectionArgsWithContact(selectionArgs, contact);
                    notificationUri = Uri.withAppendedPath(notificationUri, contact);
                    /* Intentional fall through */
                case UriType.InternalContacts.INTERNAL_CONTACTS:
                    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(CAPABILITY_TABLE, projection, selection, selectionArgs,
                            null, null, sort);
                    cursor.setNotificationUri(getContext().getContentResolver(), notificationUri);
                    return cursor;

                case UriType.Contacts.CONTACTS_WITH_ID:
                    /* Limited access with exposed URI */
                    contact = uri.getLastPathSegment();
                    selection = getSelectionWithContact(selection);
                    selectionArgs = getSelectionArgsWithContact(selectionArgs, contact);
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(CAPABILITY_TABLE, restrictProjectionToExternallyDefinedColumns(projection),
                            selection, selectionArgs, null, null, sort);
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;

                case UriType.Contacts.CONTACTS:
                    /* Limited access with exposed URI */
                    db = mOpenHelper.getReadableDatabase();
                    selection = restrictSelectionToExternallyDefinedColumns(selection);
                    cursor = db.query(CAPABILITY_TABLE, restrictProjectionToExternallyDefinedColumns(projection),
                            selection, selectionArgs, null, null, sort);
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;

                case UriType.Aggregation.AGGREGATION_WITH_ID:
                    String aggregationDataid = uri.getLastPathSegment();
                    selection = getSelectionWithAggregationDataId(selection);
                    selectionArgs = getSelectionArgsWithAggregationId(selectionArgs,
                            aggregationDataid);
                    /* Intentional fall through */
                case UriType.Aggregation.AGGREGATION:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(AGGREGATION_TABLE, projection, selection,
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
        Uri notificationUri = CapabilitiesLog.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalContacts.INTERNAL_CONTACTS_WITH_ID:
                String contact = uri.getLastPathSegment();
                selection = getSelectionWithContact(selection);
                selectionArgs = getSelectionArgsWithContact(selectionArgs, contact);
                notificationUri = Uri.withAppendedPath(notificationUri, contact);
                /* Intentional fall through */
            case UriType.InternalContacts.INTERNAL_CONTACTS:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.update(CAPABILITY_TABLE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(notificationUri, null);
                }
                return count;

            case UriType.Contacts.CONTACTS_WITH_ID:
                /* Intentional fall through */
            case UriType.Contacts.CONTACTS:
                throw new UnsupportedOperationException(new StringBuilder("This provider (URI=")
                        .append(uri).append(") supports read only access.").toString());

            case UriType.Aggregation.AGGREGATION_WITH_ID:
                String aggregationDataid = uri.getLastPathSegment();
                selection = getSelectionWithAggregationDataId(selection);
                selectionArgs = getSelectionArgsWithAggregationId(selectionArgs, aggregationDataid);
                /* Intentional fall through */
            case UriType.Aggregation.AGGREGATION:
                db = mOpenHelper.getWritableDatabase();
                count = db.update(AGGREGATION_TABLE, values, selection, selectionArgs);
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
        Uri notificationUri = CapabilitiesLog.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalContacts.INTERNAL_CONTACTS_WITH_ID:
                String contact = uri.getLastPathSegment();
                selection = getSelectionWithContact(selection);
                selectionArgs = getSelectionArgsWithContact(selectionArgs, contact);
                notificationUri = Uri.withAppendedPath(notificationUri, contact);
                /* Intentional fall through */
            case UriType.InternalContacts.INTERNAL_CONTACTS:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.delete(CAPABILITY_TABLE, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(notificationUri, null);
                }
                return count;

            case UriType.Contacts.CONTACTS_WITH_ID:
                /* Intentional fall through */
            case UriType.Contacts.CONTACTS:
                throw new UnsupportedOperationException(new StringBuilder("This provider (URI=")
                        .append(uri).append(") supports read only access.").toString());

            case UriType.Aggregation.AGGREGATION_WITH_ID:
                String aggregationDataId = uri.getLastPathSegment();
                selection = getSelectionWithAggregationDataId(selection);
                selectionArgs = getSelectionArgsWithAggregationId(selectionArgs, aggregationDataId);
                /* Intentional fall through */
            case UriType.Aggregation.AGGREGATION:
                db = mOpenHelper.getWritableDatabase();
                count = db.delete(AGGREGATION_TABLE, selection, selectionArgs);
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
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
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
                throw new UnsupportedOperationException(new StringBuilder(
                        "Opening file stream is not supported for URI ").append(uri).append("!")
                        .toString());
            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }
}
