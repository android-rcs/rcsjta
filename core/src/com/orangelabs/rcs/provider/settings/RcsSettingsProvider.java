/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.orangelabs.rcs.provider.settings;

import java.util.ArrayList;

import javax2.sip.ListeningPoint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.gsma.services.rcs.RcsServiceConfiguration;
import com.orangelabs.rcs.R;
import com.orangelabs.rcs.utils.DatabaseUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * RCS settings provider
 *
 * @author jexa7410
 */
public class RcsSettingsProvider extends ContentProvider {

    private static final String TABLE = "setting";

    private static final String SELECTION_WITH_KEY_ONLY = RcsSettingsData.KEY_KEY.concat("=?");

    private static final String DATABASE_NAME = "rcs_settings.db";

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(RcsSettingsData.CONTENT_URI.getAuthority(),
                RcsSettingsData.CONTENT_URI.getPath().substring(1), UriType.InternalSettings.INTERNAL_SETTINGS);
        sUriMatcher.addURI(RcsSettingsData.CONTENT_URI.getAuthority(), RcsSettingsData.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.InternalSettings.INTERNAL_SETTINGS_WITH_KEY);
        sUriMatcher.addURI(RcsServiceConfiguration.Settings.CONTENT_URI.getAuthority(),
                RcsServiceConfiguration.Settings.CONTENT_URI.getPath().substring(1), UriType.Settings.SETTINGS);
        sUriMatcher.addURI(RcsServiceConfiguration.Settings.CONTENT_URI.getAuthority(),
                RcsServiceConfiguration.Settings.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.Settings.SETTINGS_WITH_KEY);
    }

    /**
     * String to restrict query for exposed Uri to a set of columns
     */
    private static final String RESTRICTED_SELECTION_QUERY_FOR_EXTERNALLY_DEFINED_COLUMNS = new StringBuilder(
            RcsSettingsData.KEY_KEY).append(" IN ('").append(RcsSettingsData.KEY_MESSAGING_MODE)
            .append("','").append(RcsSettingsData.USERPROFILE_IMS_USERNAME).append("','")
            .append(RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME).append("','")
            .append(RcsSettingsData.CONFIGURATION_VALID).append("','")
            .append(RcsSettingsData.COUNTRY_CODE).append("','")
            .append(RcsSettingsData.COUNTRY_AREA_CODE).append("','")
            .append(RcsSettingsData.KEY_DEFAULT_MESSAGING_METHOD).append("')").toString();

    /**
     * String to restrict update from exposed Uri to a set of columns
     */
    private static final String RESTRICTED_SELECTION_UPDATE_FOR_EXTERNALLY_DEFINED_COLUMNS = new StringBuilder(
            RcsSettingsData.KEY_KEY).append(" IN ('")
            .append(RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME).append("','")
            .append(RcsSettingsData.KEY_DEFAULT_MESSAGING_METHOD).append("')").toString();

    private static final class UriType {

        private static final class Settings {

            private static final int SETTINGS = 1;

            private static final int SETTINGS_WITH_KEY = 2;
        }

        private static final class InternalSettings {

            private static final int INTERNAL_SETTINGS = 3;

            private static final int INTERNAL_SETTINGS_WITH_KEY = 4;
        }
    }

    private static final class CursorType {

        private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/com.orangelabs.rcs.setting";

        private static final String TYPE_ITEM = "vnd.android.cursor.item/com.orangelabs.rcs.setting";
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 104;

        private Context mContext;

        /**
         * Add a parameter in the db
         *
         * @param db Database
         * @param key Key
         * @param value Value
         */
        private void addParameter(SQLiteDatabase db, String key, String value) {
            db.execSQL(new StringBuilder("INSERT INTO ").append(TABLE).append("(")
                    .append(RcsSettingsData.KEY_KEY).append(",").append(RcsSettingsData.KEY_VALUE)
                    .append(") VALUES ('").append(key).append("','").append(value).append("');")
                    .toString());
        }

        private void addParameter(SQLiteDatabase db, String key, boolean value) {
            addParameter(db, key, Boolean.toString(value));
        }

        private void addParameter(SQLiteDatabase db, String key, int value) {
            addParameter(db, key, Integer.toString(value));
        }

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);

            mContext = ctx;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(TABLE).append("(")
                    .append(RcsSettingsData.KEY_KEY).append(" TEXT NOT NULL PRIMARY KEY,")
                    .append(RcsSettingsData.KEY_VALUE).append(" TEXT NOT NULL)").toString());

            /* Insert default values for parameters */
            addParameter(db, RcsSettingsData.SERVICE_ACTIVATED,               false);
            addParameter(db, RcsSettingsData.PRESENCE_INVITATION_RINGTONE,    "");
            addParameter(db, RcsSettingsData.PRESENCE_INVITATION_VIBRATE,     true);
            addParameter(db, RcsSettingsData.CSH_INVITATION_RINGTONE,         "");
            addParameter(db, RcsSettingsData.CSH_INVITATION_VIBRATE,          true);
            addParameter(db, RcsSettingsData.CSH_AVAILABLE_BEEP,              true);
            addParameter(db, RcsSettingsData.FILETRANSFER_INVITATION_RINGTONE, "");
            addParameter(db, RcsSettingsData.FILETRANSFER_INVITATION_VIBRATE, true);
            addParameter(db, RcsSettingsData.CHAT_INVITATION_RINGTONE,        "");
            addParameter(db, RcsSettingsData.CHAT_INVITATION_VIBRATE,         true);
            addParameter(db, RcsSettingsData.CHAT_RESPOND_TO_DISPLAY_REPORTS, true);
            addParameter(db, RcsSettingsData.FREETEXT1,                       mContext.getString(R.string.rcs_settings_label_default_freetext_1));
            addParameter(db, RcsSettingsData.FREETEXT2,                       mContext.getString(R.string.rcs_settings_label_default_freetext_2));
            addParameter(db, RcsSettingsData.FREETEXT3,                       mContext.getString(R.string.rcs_settings_label_default_freetext_3));
            addParameter(db, RcsSettingsData.FREETEXT4,                       mContext.getString(R.string.rcs_settings_label_default_freetext_4));
            addParameter(db, RcsSettingsData.MIN_BATTERY_LEVEL,               0);
            addParameter(db, RcsSettingsData.MAX_PHOTO_ICON_SIZE,             256);
            addParameter(db, RcsSettingsData.MAX_FREETXT_LENGTH,              100);
            addParameter(db, RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH,         100);
            addParameter(db, RcsSettingsData.GEOLOC_EXPIRATION_TIME,          3600);
            addParameter(db, RcsSettingsData.MIN_STORAGE_CAPACITY,            10240);
            addParameter(db, RcsSettingsData.MAX_CHAT_PARTICIPANTS,           10);
            addParameter(db, RcsSettingsData.MAX_CHAT_MSG_LENGTH,             100);
            addParameter(db, RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH,        100);
            addParameter(db, RcsSettingsData.CHAT_IDLE_DURATION,              300);
            addParameter(db, RcsSettingsData.MAX_FILE_TRANSFER_SIZE,          3072);
            addParameter(db, RcsSettingsData.WARN_FILE_TRANSFER_SIZE,         2048);
            addParameter(db, RcsSettingsData.MAX_IMAGE_SHARE_SIZE,            3072);
            addParameter(db, RcsSettingsData.MAX_VIDEO_SHARE_DURATION,        54000);
            addParameter(db, RcsSettingsData.MAX_CHAT_SESSIONS,               20);
            addParameter(db, RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS,      10);
            addParameter(db, RcsSettingsData.MAX_IP_CALL_SESSIONS,            5);
            addParameter(db, RcsSettingsData.SMS_FALLBACK_SERVICE,            true);
            addParameter(db, RcsSettingsData.WARN_SF_SERVICE,                 false);
            addParameter(db, RcsSettingsData.AUTO_ACCEPT_CHAT,                false);
            addParameter(db, RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT,          false);
            addParameter(db, RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER,       false);
            addParameter(db, RcsSettingsData.IM_SESSION_START,                1);
            addParameter(db, RcsSettingsData.USERPROFILE_IMS_USERNAME,        "");
            addParameter(db, RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME,    "");
            addParameter(db, RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN,     "");
            addParameter(db, RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID,      "");
            addParameter(db, RcsSettingsData.USERPROFILE_IMS_PASSWORD,        "");
            addParameter(db, RcsSettingsData.USERPROFILE_IMS_REALM,           "");
            addParameter(db, RcsSettingsData.IMS_PROXY_ADDR_MOBILE,           "");
            addParameter(db, RcsSettingsData.IMS_PROXY_PORT_MOBILE,           5060);
            addParameter(db, RcsSettingsData.IMS_PROXY_ADDR_WIFI,             "");
            addParameter(db, RcsSettingsData.IMS_PROXY_PORT_WIFI,             5060);
            addParameter(db, RcsSettingsData.XDM_SERVER,                      "");
            addParameter(db, RcsSettingsData.XDM_LOGIN,                       "");
            addParameter(db, RcsSettingsData.XDM_PASSWORD,                    "");
            addParameter(db, RcsSettingsData.FT_HTTP_SERVER,                  "");
            addParameter(db, RcsSettingsData.FT_HTTP_LOGIN,                   "");
            addParameter(db, RcsSettingsData.FT_HTTP_PASSWORD,                "");
            addParameter(db, RcsSettingsData.FT_PROTOCOL,                     RcsSettingsData.FT_PROTOCOL_MSRP);
            addParameter(db, RcsSettingsData.IM_CONF_URI,                     RcsSettingsData.DEFAULT_GROUP_CHAT_URI);
            addParameter(db, RcsSettingsData.ENDUSER_CONFIRMATION_URI,        "");
            addParameter(db, RcsSettingsData.COUNTRY_CODE,                    "+33");
            addParameter(db, RcsSettingsData.COUNTRY_AREA_CODE,               0);
            addParameter(db, RcsSettingsData.MSISDN,                          "");
            addParameter(db, RcsSettingsData.CAPABILITY_CS_VIDEO,             false);
            addParameter(db, RcsSettingsData.CAPABILITY_IMAGE_SHARING,        true);
            addParameter(db, RcsSettingsData.CAPABILITY_VIDEO_SHARING,        true);
            addParameter(db, RcsSettingsData.CAPABILITY_IP_VOICE_CALL,        true);
            addParameter(db, RcsSettingsData.CAPABILITY_IP_VIDEO_CALL,        true);
            addParameter(db, RcsSettingsData.CAPABILITY_IM_SESSION,           true);
            addParameter(db, RcsSettingsData.CAPABILITY_IM_GROUP_SESSION,     true);
            addParameter(db, RcsSettingsData.CAPABILITY_FILE_TRANSFER,        true);
            addParameter(db, RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP,   true);
            addParameter(db, RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,   false);
            addParameter(db, RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE,      false);
            addParameter(db, RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH,     true);
            addParameter(db, RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL, false);
            addParameter(db, RcsSettingsData.CAPABILITY_GROUP_CHAT_SF,        false);
            addParameter(db, RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF,     false);
            addParameter(db, RcsSettingsData.CAPABILITY_RCS_EXTENSIONS,       "");
            addParameter(db, RcsSettingsData.IMS_SERVICE_POLLING_PERIOD,      300);
            addParameter(db, RcsSettingsData.SIP_DEFAULT_PORT,                5062);
            addParameter(db, RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE, ListeningPoint.UDP);
            addParameter(db, RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,   ListeningPoint.TCP);
            addParameter(db, RcsSettingsData.TLS_CERTIFICATE_ROOT,            "");
            addParameter(db, RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE,    "");
            addParameter(db, RcsSettingsData.SIP_TRANSACTION_TIMEOUT,         120);
            addParameter(db, RcsSettingsData.MSRP_DEFAULT_PORT,               20000);
            addParameter(db, RcsSettingsData.RTP_DEFAULT_PORT,                10000);
            addParameter(db, RcsSettingsData.MSRP_TRANSACTION_TIMEOUT,        5);
            addParameter(db, RcsSettingsData.REGISTER_EXPIRE_PERIOD,          600000);
            addParameter(db, RcsSettingsData.REGISTER_RETRY_BASE_TIME,        30);
            addParameter(db, RcsSettingsData.REGISTER_RETRY_MAX_TIME,         1800);
            addParameter(db, RcsSettingsData.PUBLISH_EXPIRE_PERIOD,           3600);
            addParameter(db, RcsSettingsData.REVOKE_TIMEOUT,                  300);
            addParameter(db, RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE,    RcsSettingsData.DIGEST_AUTHENT);
            addParameter(db, RcsSettingsData.IMS_AUTHENT_PROCEDURE_WIFI,      RcsSettingsData.DIGEST_AUTHENT);
            addParameter(db, RcsSettingsData.TEL_URI_FORMAT,                  true);
            addParameter(db, RcsSettingsData.RINGING_SESSION_PERIOD,          60);
            addParameter(db, RcsSettingsData.SUBSCRIBE_EXPIRE_PERIOD,         3600);
            addParameter(db, RcsSettingsData.IS_COMPOSING_TIMEOUT,            5);
            addParameter(db, RcsSettingsData.SESSION_REFRESH_EXPIRE_PERIOD,   0);
            addParameter(db, RcsSettingsData.PERMANENT_STATE_MODE,            true);
            addParameter(db, RcsSettingsData.TRACE_ACTIVATED,                 true);
            addParameter(db, RcsSettingsData.TRACE_LEVEL,                     Logger.DEBUG_LEVEL);
            addParameter(db, RcsSettingsData.SIP_TRACE_ACTIVATED,             false);
            addParameter(db, RcsSettingsData.SIP_TRACE_FILE,                  Environment.getExternalStorageDirectory() + "/sip.txt");
            addParameter(db, RcsSettingsData.MEDIA_TRACE_ACTIVATED,           false);
            addParameter(db, RcsSettingsData.CAPABILITY_REFRESH_TIMEOUT,      1);
            addParameter(db, RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT,       86400);
            addParameter(db, RcsSettingsData.CAPABILITY_POLLING_PERIOD,       3600);
            addParameter(db, RcsSettingsData.IM_CAPABILITY_ALWAYS_ON,         true);
            addParameter(db, RcsSettingsData.FT_CAPABILITY_ALWAYS_ON,         false);
            addParameter(db, RcsSettingsData.IM_USE_REPORTS,                  true);
            addParameter(db, RcsSettingsData.NETWORK_ACCESS,                  RcsSettingsData.ANY_ACCESS);
            addParameter(db, RcsSettingsData.SIP_TIMER_T1,                    2000);
            addParameter(db, RcsSettingsData.SIP_TIMER_T2,                    16000);
            addParameter(db, RcsSettingsData.SIP_TIMER_T4,                    17000);
            addParameter(db, RcsSettingsData.SIP_KEEP_ALIVE,                  true);
            addParameter(db, RcsSettingsData.SIP_KEEP_ALIVE_PERIOD,           60);
            addParameter(db, RcsSettingsData.RCS_APN,                         "");
            addParameter(db, RcsSettingsData.RCS_OPERATOR,                    "");
            addParameter(db, RcsSettingsData.MAX_CHAT_LOG_ENTRIES,            500);
            addParameter(db, RcsSettingsData.MAX_RICHCALL_LOG_ENTRIES,        200);
            addParameter(db, RcsSettingsData.MAX_IPCALL_LOG_ENTRIES,          200);
            addParameter(db, RcsSettingsData.GRUU,                            true);
            addParameter(db, RcsSettingsData.USE_IMEI_AS_DEVICE_ID,           true);
            addParameter(db, RcsSettingsData.CPU_ALWAYS_ON,                   false);
            addParameter(db, RcsSettingsData.AUTO_CONFIG_MODE,                RcsSettingsData.HTTPS_AUTO_CONFIG);
            addParameter(db, RcsSettingsData.PROVISIONING_TERMS_ACCEPTED,     false);
            addParameter(db, RcsSettingsData.PROVISIONING_VERSION,            0);
            addParameter(db, RcsSettingsData.PROVISIONING_TOKEN,              "");
            addParameter(db, RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS,  "");
            addParameter(db, RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY, false);
            addParameter(db, RcsSettingsData.DIRECTORY_PATH_PHOTOS,           Environment.getExternalStorageDirectory() + "/rcs/photos/");
            addParameter(db, RcsSettingsData.DIRECTORY_PATH_VIDEOS,           Environment.getExternalStorageDirectory() + "/rcs/videos/");
            addParameter(db, RcsSettingsData.DIRECTORY_PATH_FILES,            Environment.getExternalStorageDirectory() + "/rcs/files/");
            addParameter(db, RcsSettingsData.SECURE_MSRP_OVER_WIFI,           false);
            addParameter(db, RcsSettingsData.SECURE_RTP_OVER_WIFI,            false);
            addParameter(db, RcsSettingsData.KEY_MESSAGING_MODE,              RcsSettingsData.VALUE_MESSAGING_MODE_NONE);
            addParameter(db, RcsSettingsData.CAPABILITY_SIP_AUTOMATA,         false);
            addParameter(db, RcsSettingsData.KEY_GSMA_RELEASE,                RcsSettingsData.VALUE_GSMA_REL_BLACKBIRD);
            addParameter(db, RcsSettingsData.IPVOICECALL_BREAKOUT_AA,         false);
            addParameter(db, RcsSettingsData.IPVOICECALL_BREAKOUT_CS,         false);
            addParameter(db, RcsSettingsData.IPVIDEOCALL_UPGRADE_FROM_CS,     false);
            addParameter(db, RcsSettingsData.IPVIDEOCALL_UPGRADE_ON_CAPERROR, false);
            addParameter(db, RcsSettingsData.IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY, false);
            addParameter(db, RcsSettingsData.TCP_FALLBACK,                    false);
            addParameter(db, RcsSettingsData.VENDOR_NAME,                     "OrangeLabs");
            addParameter(db, RcsSettingsData.CONTROL_EXTENSIONS,              false);
            addParameter(db, RcsSettingsData.ALLOW_EXTENSIONS,                true);
            addParameter(db, RcsSettingsData.MAX_MSRP_SIZE_EXTENSIONS,        0);
            addParameter(db, RcsSettingsData.CONFIGURATION_VALID,             false);
            addParameter(db, RcsSettingsData.AUTO_ACCEPT_FT_IN_ROAMING,       false);
            addParameter(db, RcsSettingsData.AUTO_ACCEPT_FT_CHANGEABLE,       RcsSettingsData.VALUE_IMAGE_RESIZE_ASK);
            addParameter(db, RcsSettingsData.KEY_DEFAULT_MESSAGING_METHOD,    RcsSettingsData.VALUE_DEF_MSG_METHOD_AUTOMATIC);
            addParameter(db, RcsSettingsData.KEY_IMAGE_RESIZE_OPTION,         RcsSettingsData.VALUE_IMAGE_RESIZE_ONLY_ABOVE_MAX_SIZE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            /* Get old data before deleting the table */
            Cursor oldDataCursor = db.query(TABLE, null, null, null, null, null, null);

            /*
             * Get all the pairs key/value of the old table to insert them back
             * after update
             */
            ArrayList<ContentValues> valuesList = new ArrayList<ContentValues>();
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
                String[] selectionArgs = new String[] {
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
        return new StringBuilder("(").append(SELECTION_WITH_KEY_ONLY).append(") AND (")
                .append(selection).append(")").toString();
    }

    private String[] getSelectionArgsWithKey(String[] selectionArgs, String key) {
        String[] keySelectionArg = new String[] {
            key
        };
        if (selectionArgs == null) {
            return keySelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(keySelectionArg, selectionArgs);
    }

    private StringBuilder restrictSelectionToColumns(String selection, String restrictedSetOfColumns) {
        if (TextUtils.isEmpty(selection)) {
            return new StringBuilder(restrictedSetOfColumns);
        }
        return new StringBuilder("(").append(selection).append(") AND (")
                .append(restrictedSetOfColumns).append(")");
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalSettings.INTERNAL_SETTINGS:
                /* Intentional fall through */
            case UriType.Settings.SETTINGS:
                return CursorType.TYPE_DIRECTORY;

            case UriType.InternalSettings.INTERNAL_SETTINGS_WITH_KEY:
                /* Intentional fall through */
            case UriType.Settings.SETTINGS_WITH_KEY:
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
        Uri notificationUri = RcsServiceConfiguration.Settings.CONTENT_URI;
        try {
            switch (sUriMatcher.match(uri)) {
                case UriType.InternalSettings.INTERNAL_SETTINGS_WITH_KEY:
                    String key = uri.getLastPathSegment();
                    selection = getSelectionWithKey(selection);
                    selectionArgs = getSelectionArgsWithKey(selectionArgs, key);
                    notificationUri = Uri.withAppendedPath(notificationUri, key);
                    /* Intentional fall through */
                case UriType.InternalSettings.INTERNAL_SETTINGS:
                    SQLiteDatabase database = mOpenHelper.getReadableDatabase();
                    cursor = database.query(TABLE, projection, selection, selectionArgs, null,
                            null, sort);
                    cursor.setNotificationUri(getContext().getContentResolver(), notificationUri);
                    return cursor;

                case UriType.Settings.SETTINGS_WITH_KEY:
                    key = uri.getLastPathSegment();
                    selection = getSelectionWithKey(selection);
                    selectionArgs = getSelectionArgsWithKey(selectionArgs, key);
                    /* Intentional fall through */
                case UriType.Settings.SETTINGS:
                    database = mOpenHelper.getReadableDatabase();
                    selection = restrictSelectionToColumns(selection,
                            RESTRICTED_SELECTION_QUERY_FOR_EXTERNALLY_DEFINED_COLUMNS).toString();
                    cursor = database.query(TABLE, projection, selection, selectionArgs, null,
                            null, sort);
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
        Uri notificationUri = RcsServiceConfiguration.Settings.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalSettings.INTERNAL_SETTINGS_WITH_KEY:
                String key = uri.getLastPathSegment();
                selection = getSelectionWithKey(selection);
                selectionArgs = getSelectionArgsWithKey(selectionArgs, key);
                notificationUri = Uri.withAppendedPath(notificationUri, key);
                /* Intentional fall through */
            case UriType.InternalSettings.INTERNAL_SETTINGS:
                SQLiteDatabase database = mOpenHelper.getWritableDatabase();
                int count = database.update(TABLE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(notificationUri, null);
                }
                return count;

            case UriType.Settings.SETTINGS_WITH_KEY:
                key = uri.getLastPathSegment();
                selection = getSelectionWithKey(selection);
                selectionArgs = getSelectionArgsWithKey(selectionArgs, key);
                /* Intentional fall through */
            case UriType.Settings.SETTINGS:
                database = mOpenHelper.getReadableDatabase();
                selection = restrictSelectionToColumns(selection,
                        RESTRICTED_SELECTION_UPDATE_FOR_EXTERNALLY_DEFINED_COLUMNS).toString();
                count = database.update(TABLE, values, selection, selectionArgs);
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
        throw new UnsupportedOperationException(new StringBuilder("Cannot insert URI ").append(uri)
                .append("!").toString());
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        throw new UnsupportedOperationException(new StringBuilder("Cannot delete URI ").append(uri)
                .append("!").toString());
    }
}
