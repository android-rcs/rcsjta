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

package com.gsma.rcs.provider.settings;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import com.gsma.rcs.utils.DatabaseUtils;

/**
 * RCS settings provider
 * 
 * @author jexa7410
 * @author yplo6403
 */
public class RcsSettingsProvider extends ContentProvider {

    private static final String TABLE = "setting";

    private static final String SELECTION_WITH_KEY_ONLY = RcsSettingsData.KEY_KEY.concat("=?");

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
        private static final int DATABASE_VERSION = 109;

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

        private void addParameter(SQLiteDatabase db, String key, long value) {
            addParameter(db, key, Long.toString(value));
        }

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(TABLE).append("(")
            .append(RcsSettingsData.KEY_KEY).append(" TEXT NOT NULL PRIMARY KEY,")
                    .append(RcsSettingsData.KEY_VALUE).append(" TEXT NOT NULL)").toString());

            /* Insert default values for parameters */
            addParameter(db, RcsSettingsData.SERVICE_ACTIVATED,
                    RcsSettingsData.DEFAULT_SERVICE_ACTIVATED);
            addParameter(db, RcsSettingsData.CHAT_RESPOND_TO_DISPLAY_REPORTS,
                    RcsSettingsData.DEFAULT_CHAT_RESPOND_TO_DISPLAY_REPORTS);
            addParameter(db, RcsSettingsData.MIN_BATTERY_LEVEL,
                    RcsSettingsData.DEFAULT_MIN_BATTERY_LEVEL);
            addParameter(db, RcsSettingsData.MAX_PHOTO_ICON_SIZE,
                    RcsSettingsData.DEFAULT_MAX_PHOTO_ICON_SIZE);
            addParameter(db, RcsSettingsData.MAX_FREETXT_LENGTH,
                    RcsSettingsData.DEFAULT_MAX_PHOTO_ICON_SIZE);
            addParameter(db, RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH,
                    RcsSettingsData.DEFAULT_MAX_GEOLOC_LABEL_LENGTH);
            addParameter(db, RcsSettingsData.GEOLOC_EXPIRATION_TIME,
                    RcsSettingsData.DEFAULT_GEOLOC_EXPIRATION_TIME);
            addParameter(db, RcsSettingsData.MAX_CHAT_PARTICIPANTS,
                    RcsSettingsData.DEFAULT_MAX_CHAT_PARTICIPANTS);
            addParameter(db, RcsSettingsData.MAX_CHAT_MSG_LENGTH,
                    RcsSettingsData.DEFAULT_MAX_CHAT_MSG_LENGTH);
            addParameter(db, RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH,
                    RcsSettingsData.DEFAULT_MAX_GC_MSG_LENGTH);
            addParameter(db, RcsSettingsData.CHAT_IDLE_DURATION,
                    RcsSettingsData.DEFAULT_CHAT_IDLE_DURATION);
            addParameter(db, RcsSettingsData.MAX_FILE_TRANSFER_SIZE,
                    RcsSettingsData.DEFAULT_MAX_FT_SIZE);
            addParameter(db, RcsSettingsData.WARN_FILE_TRANSFER_SIZE,
                    RcsSettingsData.DEFAULT_WARN_FT_SIZE);
            addParameter(db, RcsSettingsData.MAX_IMAGE_SHARE_SIZE,
                    RcsSettingsData.DEFAULT_MAX_ISH_SIZE);
            addParameter(db, RcsSettingsData.MAX_VIDEO_SHARE_DURATION,
                    RcsSettingsData.DEFAULT_MAX_VSH_DURATION);
            addParameter(db, RcsSettingsData.MAX_CHAT_SESSIONS,
                    RcsSettingsData.DEFAULT_MAX_CHAT_SESSIONS);
            addParameter(db, RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS,
                    RcsSettingsData.DEFAULT_MAX_FT_SESSIONS);
            addParameter(db, RcsSettingsData.MAX_CONCURRENT_OUTGOING_FILE_TRANSFERS,
                    RcsSettingsData.DEFAULT_MAX_CONCURRENT_OUTGOING_FT_SESSIONS);
            addParameter(db, RcsSettingsData.MAX_IP_CALL_SESSIONS,
                    RcsSettingsData.DEFAULT_MAX_IP_CALL_SESSIONS);
            addParameter(db, RcsSettingsData.SMS_FALLBACK_SERVICE,
                    RcsSettingsData.DEFAULT_SMS_FALLBACK_SERVICE);
            addParameter(db, RcsSettingsData.WARN_SF_SERVICE,
                    RcsSettingsData.DEFAULT_WARN_SF_SERVICE);
            addParameter(db, RcsSettingsData.AUTO_ACCEPT_CHAT,
                    RcsSettingsData.DEFAULT_AUTO_ACCEPT_CHAT);
            addParameter(db, RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT,
                    RcsSettingsData.DEFAULT_AUTO_ACCEPT_GC);
            addParameter(db, RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER,
                    RcsSettingsData.DEFAULT_AUTO_ACCEPT_FT);
            addParameter(db, RcsSettingsData.IM_SESSION_START,
                    RcsSettingsData.DEFAULT_IM_SESSION_START);
            addParameter(db, RcsSettingsData.USERPROFILE_IMS_USERNAME,
                    RcsSettingsData.DEFAULT_USERPROFILE_IMS_USERNAME);
            addParameter(db, RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME,
                    RcsSettingsData.DEFAULT_USERPROFILE_IMS_DISPLAY_NAME);
            addParameter(db, RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN,
                    RcsSettingsData.DEFAULT_USERPROFILE_IMS_HOME_DOMAIN);
            addParameter(db, RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID,
                    RcsSettingsData.DEFAULT_USERPROFILE_IMS_PRIVATE_ID);
            addParameter(db, RcsSettingsData.USERPROFILE_IMS_PASSWORD,
                    RcsSettingsData.DEFAULT_USERPROFILE_IMS_PASSWORD);
            addParameter(db, RcsSettingsData.USERPROFILE_IMS_REALM,
                    RcsSettingsData.DEFAULT_USERPROFILE_IMS_REALM);
            addParameter(db, RcsSettingsData.IMS_PROXY_ADDR_MOBILE,
                    RcsSettingsData.DEFAULT_IMS_PROXY_ADDR_MOBILE);
            addParameter(db, RcsSettingsData.IMS_PROXY_PORT_MOBILE,
                    RcsSettingsData.DEFAULT_IMS_PROXY_PORT_MOBILE);
            addParameter(db, RcsSettingsData.IMS_PROXY_ADDR_WIFI,
                    RcsSettingsData.DEFAULT_IMS_PROXY_ADDR_WIFI);
            addParameter(db, RcsSettingsData.IMS_PROXY_PORT_WIFI,
                    RcsSettingsData.DEFAULT_IMS_PROXY_PORT_WIFI);
            addParameter(db, RcsSettingsData.XDM_SERVER, RcsSettingsData.DEFAULT_XDM_SERVER);
            addParameter(db, RcsSettingsData.XDM_LOGIN, RcsSettingsData.DEFAULT_XDM_LOGIN);
            addParameter(db, RcsSettingsData.XDM_PASSWORD, RcsSettingsData.DEFAULT_XDM_PASSWORD);
            addParameter(db, RcsSettingsData.FT_HTTP_SERVER, RcsSettingsData.DEFAULT_FT_HTTP_SERVER);
            addParameter(db, RcsSettingsData.FT_HTTP_LOGIN, RcsSettingsData.DEFAULT_FT_HTTP_LOGIN);
            addParameter(db, RcsSettingsData.FT_HTTP_PASSWORD,
                    RcsSettingsData.DEFAULT_FT_HTTP_PASSWORD);
            addParameter(db, RcsSettingsData.FT_PROTOCOL, RcsSettingsData.DEFAULT_FT_PROTOCOL);
            addParameter(db, RcsSettingsData.IM_CONF_URI, RcsSettingsData.DEFAULT_IM_CONF_URI);
            addParameter(db, RcsSettingsData.ENDUSER_CONFIRMATION_URI,
                    RcsSettingsData.DEFAULT_ENDUSER_CONFIRMATION_URI);
            addParameter(db, RcsSettingsData.MSISDN, RcsSettingsData.DEFAULT_MSISDN);
            addParameter(db, RcsSettingsData.CAPABILITY_CS_VIDEO,
                    RcsSettingsData.DEFAULT_CAPABILITY_CS_VIDEO);
            addParameter(db, RcsSettingsData.CAPABILITY_IMAGE_SHARING,
                    RcsSettingsData.DEFAULT_CAPABILITY_ISH);
            addParameter(db, RcsSettingsData.CAPABILITY_VIDEO_SHARING,
                    RcsSettingsData.DEFAULT_CAPABILITY_VSH);
            addParameter(db, RcsSettingsData.CAPABILITY_IP_VOICE_CALL,
                    RcsSettingsData.DEFAULT_CAPABILITY_IP_VOICE_CALL);
            addParameter(db, RcsSettingsData.CAPABILITY_IP_VIDEO_CALL,
                    RcsSettingsData.DEFAULT_CAPABILITY_IP_VIDEO_CALL);
            addParameter(db, RcsSettingsData.CAPABILITY_IM_SESSION,
                    RcsSettingsData.DEFAULT_CAPABILITY_IM_SESSION);
            addParameter(db, RcsSettingsData.CAPABILITY_IM_GROUP_SESSION,
                    RcsSettingsData.DEFAULT_CAPABILITY_IM_GROUP_SESSION);
            addParameter(db, RcsSettingsData.CAPABILITY_FILE_TRANSFER,
                    RcsSettingsData.DEFAULT_CAPABILITY_FT);
            addParameter(db, RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP,
                    RcsSettingsData.DEFAULT_CAPABILITY_FT_HTTP);
            addParameter(db, RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
                    RcsSettingsData.DEFAULT_CAPABILITY_PRESENCE_DISCOVERY);
            addParameter(db, RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE,
                    RcsSettingsData.DEFAULT_CAPABILITY_SOCIAL_PRESENCE);
            addParameter(db, RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH,
                    RcsSettingsData.DEFAULT_CAPABILITY_GEOLOCATION_PUSH);
            addParameter(db, RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL,
                    RcsSettingsData.DEFAULT_CAPABILITY_FT_THUMBNAIL);
            addParameter(db, RcsSettingsData.CAPABILITY_GROUP_CHAT_SF,
                    RcsSettingsData.DEFAULT_CAPABILITY_GC_SF);
            addParameter(db, RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF,
                    RcsSettingsData.DEFAULT_CAPABILITY_FT_SF);
            addParameter(db, RcsSettingsData.CAPABILITY_RCS_EXTENSIONS,
                    RcsSettingsData.DEFAULT_CAPABILITY_RCS_EXTENSIONS);
            addParameter(db, RcsSettingsData.IMS_SERVICE_POLLING_PERIOD,
                    RcsSettingsData.DEFAULT_IMS_SERVICE_POLLING_PERIOD);
            addParameter(db, RcsSettingsData.SIP_DEFAULT_PORT,
                    RcsSettingsData.DEFAULT_SIP_DEFAULT_PORT);
            addParameter(db, RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
                    RcsSettingsData.DEFAULT_SIP_DEFAULT_PROTOCOL_FOR_MOBILE);
            addParameter(db, RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
                    RcsSettingsData.DEFAULT_SIP_DEFAULT_PROTOCOL_FOR_WIFI);
            addParameter(db, RcsSettingsData.TLS_CERTIFICATE_ROOT,
                    RcsSettingsData.DEFAULT_TLS_CERTIFICATE_ROOT);
            addParameter(db, RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE,
                    RcsSettingsData.DEFAULT_TLS_CERTIFICATE_INTERMEDIATE);
            addParameter(db, RcsSettingsData.SIP_TRANSACTION_TIMEOUT,
                    RcsSettingsData.DEFAULT_SIP_TRANSACTION_TIMEOUT);
            addParameter(db, RcsSettingsData.MSRP_DEFAULT_PORT,
                    RcsSettingsData.DEFAULT_MSRP_DEFAULT_PORT);
            addParameter(db, RcsSettingsData.RTP_DEFAULT_PORT,
                    RcsSettingsData.DEFAULT_RTP_DEFAULT_PORT);
            addParameter(db, RcsSettingsData.MSRP_TRANSACTION_TIMEOUT,
                    RcsSettingsData.DEFAULT_MSRP_TRANSACTION_TIMEOUT);
            addParameter(db, RcsSettingsData.REGISTER_EXPIRE_PERIOD,
                    RcsSettingsData.DEFAULT_REGISTER_EXPIRE_PERIOD);
            addParameter(db, RcsSettingsData.REGISTER_RETRY_BASE_TIME,
                    RcsSettingsData.DEFAULT_REGISTER_RETRY_BASE_TIME);
            addParameter(db, RcsSettingsData.REGISTER_RETRY_MAX_TIME,
                    RcsSettingsData.DEFAULT_REGISTER_RETRY_MAX_TIME);
            addParameter(db, RcsSettingsData.PUBLISH_EXPIRE_PERIOD,
                    RcsSettingsData.DEFAULT_PUBLISH_EXPIRE_PERIOD);
            addParameter(db, RcsSettingsData.REVOKE_TIMEOUT, RcsSettingsData.DEFAULT_REVOKE_TIMEOUT);
            addParameter(db, RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE,
                    RcsSettingsData.DEFAULT_IMS_AUTHENT_PROCEDURE_MOBILE);
            addParameter(db, RcsSettingsData.IMS_AUTHENT_PROCEDURE_WIFI,
                    RcsSettingsData.DEFAULT_IMS_AUTHENT_PROCEDURE_WIFI);
            addParameter(db, RcsSettingsData.TEL_URI_FORMAT, RcsSettingsData.DEFAULT_TEL_URI_FORMAT);
            addParameter(db, RcsSettingsData.RINGING_SESSION_PERIOD,
                    RcsSettingsData.DEFAULT_RINGING_SESSION_PERIOD);
            addParameter(db, RcsSettingsData.SUBSCRIBE_EXPIRE_PERIOD,
                    RcsSettingsData.DEFAULT_SUBSCRIBE_EXPIRE_PERIOD);
            addParameter(db, RcsSettingsData.IS_COMPOSING_TIMEOUT,
                    RcsSettingsData.DEFAULT_IS_COMPOSING_TIMEOUT);
            addParameter(db, RcsSettingsData.SESSION_REFRESH_EXPIRE_PERIOD,
                    RcsSettingsData.DEFAULT_SESSION_REFRESH_EXPIRE_PERIOD);
            addParameter(db, RcsSettingsData.PERMANENT_STATE_MODE,
                    RcsSettingsData.DEFAULT_PERMANENT_STATE_MODE);
            addParameter(db, RcsSettingsData.TRACE_ACTIVATED,
                    RcsSettingsData.DEFAULT_TRACE_ACTIVATED);
            addParameter(db, RcsSettingsData.TRACE_LEVEL, RcsSettingsData.DEFAULT_TRACE_LEVEL);
            addParameter(db, RcsSettingsData.SIP_TRACE_ACTIVATED,
                    RcsSettingsData.DEFAULT_SIP_TRACE_ACTIVATED);
            addParameter(db, RcsSettingsData.SIP_TRACE_FILE, RcsSettingsData.DEFAULT_SIP_TRACE_FILE);
            addParameter(db, RcsSettingsData.MEDIA_TRACE_ACTIVATED,
                    RcsSettingsData.DEFAULT_MEDIA_TRACE_ACTIVATED);
            addParameter(db, RcsSettingsData.CAPABILITY_REFRESH_TIMEOUT,
                    RcsSettingsData.DEFAULT_CAPABILITY_REFRESH_TIMEOUT);
            addParameter(db, RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT,
                    RcsSettingsData.DEFAULT_CAPABILITY_EXPIRY_TIMEOUT);
            addParameter(db, RcsSettingsData.CAPABILITY_POLLING_PERIOD,
                    RcsSettingsData.DEFAULT_CAPABILITY_POLLING_PERIOD);
            addParameter(db, RcsSettingsData.IM_CAPABILITY_ALWAYS_ON,
                    RcsSettingsData.DEFAULT_IM_CAPABILITY_ALWAYS_ON);
            addParameter(db, RcsSettingsData.GROUP_CHAT_INVITE_ONLY_FULL_SF,
                    RcsSettingsData.DEFAULT_GC_INVITE_ONLY_FULL_SF);
            addParameter(db, RcsSettingsData.FT_CAPABILITY_ALWAYS_ON,
                    RcsSettingsData.DEFAULT_FT_CAPABILITY_ALWAYS_ON);
            addParameter(db, RcsSettingsData.FT_HTTP_CAP_ALWAYS_ON,
                    RcsSettingsData.DEFAULT_FT_HTTP_CAP_ALWAYS_ON);
            addParameter(db, RcsSettingsData.MSG_CAP_VALIDITY_PERIOD,
                    RcsSettingsData.DEFAULT_MSG_CAP_VALIDITY_PERIOD);
            addParameter(db, RcsSettingsData.IM_USE_REPORTS, RcsSettingsData.DEFAULT_IM_USE_REPORTS);
            addParameter(db, RcsSettingsData.NETWORK_ACCESS, RcsSettingsData.DEFAULT_NETWORK_ACCESS);
            addParameter(db, RcsSettingsData.SIP_TIMER_T1, RcsSettingsData.DEFAULT_SIP_TIMER_T1);
            addParameter(db, RcsSettingsData.SIP_TIMER_T2, RcsSettingsData.DEFAULT_SIP_TIMER_T2);
            addParameter(db, RcsSettingsData.SIP_TIMER_T4, RcsSettingsData.DEFAULT_SIP_TIMER_T4);
            addParameter(db, RcsSettingsData.SIP_KEEP_ALIVE, RcsSettingsData.DEFAULT_SIP_KEEP_ALIVE);
            addParameter(db, RcsSettingsData.SIP_KEEP_ALIVE_PERIOD,
                    RcsSettingsData.DEFAULT_SIP_KEEP_ALIVE_PERIOD);
            addParameter(db, RcsSettingsData.RCS_APN, RcsSettingsData.DEFAULT_RCS_APN);
            addParameter(db, RcsSettingsData.RCS_OPERATOR, RcsSettingsData.DEFAULT_RCS_OPERATOR);
            addParameter(db, RcsSettingsData.MAX_CHAT_LOG_ENTRIES,
                    RcsSettingsData.DEFAULT_MAX_CHAT_LOG_ENTRIES);
            addParameter(db, RcsSettingsData.MAX_RICHCALL_LOG_ENTRIES,
                    RcsSettingsData.DEFAULT_MAX_RICHCALL_LOG_ENTRIES);
            addParameter(db, RcsSettingsData.MAX_IPCALL_LOG_ENTRIES,
                    RcsSettingsData.DEFAULT_MAX_IPCALL_LOG_ENTRIES);
            addParameter(db, RcsSettingsData.GRUU, RcsSettingsData.DEFAULT_GRUU);
            addParameter(db, RcsSettingsData.USE_IMEI_AS_DEVICE_ID,
                    RcsSettingsData.DEFAULT_USE_IMEI_AS_DEVICE_ID);
            addParameter(db, RcsSettingsData.CPU_ALWAYS_ON, RcsSettingsData.DEFAULT_CPU_ALWAYS_ON);
            addParameter(db, RcsSettingsData.CONFIG_MODE, RcsSettingsData.DEFAULT_CONFIG_MODE);
            addParameter(db, RcsSettingsData.PROVISIONING_TERMS_ACCEPTED,
                    RcsSettingsData.DEFAULT_PROVISIONING_TERMS_ACCEPTED);
            addParameter(db, RcsSettingsData.PROVISIONING_VERSION,
                    RcsSettingsData.DEFAULT_PROVISIONING_VERSION);
            addParameter(db, RcsSettingsData.PROVISIONING_TOKEN,
                    RcsSettingsData.DEFAULT_PROVISIONING_TOKEN);
            addParameter(db, RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS,
                    RcsSettingsData.DEFAULT_SECONDARY_PROV_ADDR);
            addParameter(db, RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY,
                    RcsSettingsData.DEFAULT_SECONDARY_PROV_ADDR_ONLY);
            addParameter(db, RcsSettingsData.DIRECTORY_PATH_PHOTOS,
                    RcsSettingsData.DEFAULT_DIRECTORY_PATH_PHOTOS);
            addParameter(db, RcsSettingsData.DIRECTORY_PATH_VIDEOS,
                    RcsSettingsData.DEFAULT_DIRECTORY_PATH_VIDEOS);
            addParameter(db, RcsSettingsData.DIRECTORY_PATH_FILES,
                    RcsSettingsData.DEFAULT_DIRECTORY_PATH_FILES);
            addParameter(db, RcsSettingsData.SECURE_MSRP_OVER_WIFI,
                    RcsSettingsData.DEFAULT_SECURE_MSRP_OVER_WIFI);
            addParameter(db, RcsSettingsData.SECURE_RTP_OVER_WIFI,
                    RcsSettingsData.DEFAULT_SECURE_RTP_OVER_WIFI);
            addParameter(db, RcsSettingsData.KEY_MESSAGING_MODE,
                    RcsSettingsData.DEFAULT_KEY_MESSAGING_MODE);
            addParameter(db, RcsSettingsData.CAPABILITY_SIP_AUTOMATA,
                    RcsSettingsData.DEFAULT_CAPABILITY_SIP_AUTOMATA);
            addParameter(db, RcsSettingsData.KEY_GSMA_RELEASE,
                    RcsSettingsData.DEFAULT_KEY_GSMA_RELEASE);
            addParameter(db, RcsSettingsData.IPVOICECALL_BREAKOUT_AA,
                    RcsSettingsData.DEFAULT_IPVOICECALL_BREAKOUT_AA);
            addParameter(db, RcsSettingsData.IPVOICECALL_BREAKOUT_CS,
                    RcsSettingsData.DEFAULT_IPVOICECALL_BREAKOUT_CS);
            addParameter(db, RcsSettingsData.IPVIDEOCALL_UPGRADE_FROM_CS,
                    RcsSettingsData.DEFAULT_IPVIDEOCALL_UPGRADE_FROM_CS);
            addParameter(db, RcsSettingsData.IPVIDEOCALL_UPGRADE_ON_CAPERROR,
                    RcsSettingsData.DEFAULT_IPVIDEOCALL_UPGRADE_ON_CAPERROR);
            addParameter(db, RcsSettingsData.IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY,
                    RcsSettingsData.DEFAULT_IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY);
            addParameter(db, RcsSettingsData.TCP_FALLBACK, RcsSettingsData.DEFAULT_TCP_FALLBACK);
            addParameter(db, RcsSettingsData.CONTROL_EXTENSIONS,
                    RcsSettingsData.DEFAULT_CONTROL_EXTENSIONS);
            addParameter(db, RcsSettingsData.ALLOW_EXTENSIONS,
                    RcsSettingsData.DEFAULT_ALLOW_EXTENSIONS);
            addParameter(db, RcsSettingsData.MAX_MSRP_SIZE_EXTENSIONS,
                    RcsSettingsData.DEFAULT_MAX_MSRP_SIZE_EXTENSIONS);
            addParameter(db, RcsSettingsData.CONFIGURATION_VALID,
                    RcsSettingsData.DEFAULT_CONFIGURATION_VALID);
            addParameter(db, RcsSettingsData.AUTO_ACCEPT_FT_IN_ROAMING,
                    RcsSettingsData.DEFAULT_AUTO_ACCEPT_FT_IN_ROAMING);
            addParameter(db, RcsSettingsData.AUTO_ACCEPT_FT_CHANGEABLE,
                    RcsSettingsData.DEFAULT_AUTO_ACCEPT_FT_CHANGEABLE);
            addParameter(db, RcsSettingsData.KEY_DEFAULT_MESSAGING_METHOD,
                    RcsSettingsData.DEFAULT_KEY_DEFAULT_MESSAGING_METHOD);
            addParameter(db, RcsSettingsData.KEY_IMAGE_RESIZE_OPTION,
                    RcsSettingsData.DEFAULT_KEY_IMAGE_RESIZE_OPTION);
            addParameter(db, RcsSettingsData.ENABLE_RCS_SWITCH,
                    RcsSettingsData.DEFAULT_ENABLE_RCS_SWITCH);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            /* Get old data before deleting the table */
            Cursor oldDataCursor = db.query(TABLE, null, null, null, null, null, null);

            /*
             * Get all the pairs key/value of the old table to insert them back after update
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

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.SETTINGS:
                return CursorType.TYPE_DIRECTORY;

            case UriType.SETTINGS_WITH_KEY:
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
        Uri notificationUri = RcsSettingsData.CONTENT_URI;
        try {
            switch (sUriMatcher.match(uri)) {
                case UriType.SETTINGS_WITH_KEY:
                    String key = uri.getLastPathSegment();
                    selection = getSelectionWithKey(selection);
                    selectionArgs = getSelectionArgsWithKey(selectionArgs, key);
                    notificationUri = Uri.withAppendedPath(notificationUri, key);
                    /* Intentional fall through */
                case UriType.SETTINGS:
                    SQLiteDatabase database = mOpenHelper.getReadableDatabase();
                    cursor = database.query(TABLE, projection, selection, selectionArgs, null,
                            null, sort);
                    cursor.setNotificationUri(getContext().getContentResolver(), notificationUri);
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
        Uri notificationUri = RcsSettingsData.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.SETTINGS_WITH_KEY:
                String key = uri.getLastPathSegment();
                selection = getSelectionWithKey(selection);
                selectionArgs = getSelectionArgsWithKey(selectionArgs, key);
                notificationUri = Uri.withAppendedPath(notificationUri, key);
                /* Intentional fall through */
            case UriType.SETTINGS:
                SQLiteDatabase database = mOpenHelper.getWritableDatabase();
                int count = database.update(TABLE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(notificationUri, null);
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
