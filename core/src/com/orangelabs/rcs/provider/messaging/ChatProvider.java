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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import com.gsma.services.rcs.chat.ChatLog;
import com.orangelabs.rcs.utils.DatabaseUtils;

/**
 * Chat provider
 *
 * @author Jean-Marc AUFFRET
 */
public class ChatProvider extends ContentProvider {

    private static final String TABLE_GROUP_CHAT = "groupchat";

    private static final String TABLE_MESSAGE = "message";

    private static final String SELECTION_WITH_CHAT_ID_ONLY = ChatData.KEY_CHAT_ID.concat("=?");

    private static final String SELECTION_WITH_MSG_ID_ONLY = MessageData.KEY_MESSAGE_ID.concat("=?");

    private static final String DATABASE_NAME = "chat.db";

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(ChatData.CONTENT_URI.getAuthority(), ChatData.CONTENT_URI.getPath().substring(1),
                UriType.InternalChat.CHAT);
        sUriMatcher.addURI(ChatData.CONTENT_URI.getAuthority(), ChatData.CONTENT_URI.getPath().substring(1)
                .concat("/*"), UriType.InternalChat.CHAT_WITH_ID);
        sUriMatcher.addURI(ChatLog.GroupChat.CONTENT_URI.getAuthority(),
                ChatLog.GroupChat.CONTENT_URI.getPath().substring(1), UriType.Chat.CHAT);
        sUriMatcher.addURI(ChatLog.GroupChat.CONTENT_URI.getAuthority(),
                ChatLog.GroupChat.CONTENT_URI.getPath().substring(1).concat("/*"), UriType.Chat.CHAT_WITH_ID);
        sUriMatcher.addURI(ChatLog.Message.CONTENT_URI.getAuthority(),
                ChatLog.Message.CONTENT_URI.getPath().substring(1), UriType.Message.MESSAGE);
        sUriMatcher.addURI(ChatLog.Message.CONTENT_URI.getAuthority(), ChatLog.Message.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.Message.MESSAGE_WITH_ID);

    }

    /**
     * String to restrict projection for exposed URI to a set of columns
     */
    private static final String[] RESTRICTED_PROJECTION_FOR_EXTERNALLY_DEFINED_COLUMNS = new String[] {
            ChatLog.GroupChat.CHAT_ID, ChatLog.GroupChat.CONTACT, ChatLog.GroupChat.STATE,
            ChatLog.GroupChat.SUBJECT, ChatLog.GroupChat.DIRECTION, ChatLog.GroupChat.TIMESTAMP,
            ChatLog.GroupChat.REASON_CODE, ChatLog.GroupChat.PARTICIPANTS
    };

    private static final Set<String> RESTRICTED_PROJECTION_SET = new HashSet<String>(
            Arrays.asList(RESTRICTED_PROJECTION_FOR_EXTERNALLY_DEFINED_COLUMNS));

    private static final class UriType {

        private static final class Chat {

            private static final int CHAT = 1;

            private static final int CHAT_WITH_ID = 2;
        }

        private static final class Message {

            private static final int MESSAGE = 3;

            private static final int MESSAGE_WITH_ID = 4;
        }

        private static final class InternalChat {

            private static final int CHAT = 5;

            private static final int CHAT_WITH_ID = 6;
        }
    }

    private static final class CursorType {

        private static final class Chat {

            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/groupchat";

            private static final String TYPE_ITEM = "vnd.android.cursor.item/groupchat";
        }

        private static final class Message {

            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/chatmessage";

            private static final String TYPE_ITEM = "vnd.android.cursor.item/chatmessage";
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 15;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(TABLE_GROUP_CHAT)
                    .append("(").append(ChatData.KEY_CHAT_ID).append(" TEXT NOT NULL PRIMARY KEY,")
                    .append(ChatData.KEY_REJOIN_ID).append(" TEXT,").append(ChatData.KEY_SUBJECT)
                    .append(" TEXT,").append(ChatData.KEY_PARTICIPANTS).append(" TEXT NOT NULL,")
                    .append(ChatData.KEY_STATE).append(" INTEGER NOT NULL,")
                    .append(ChatData.KEY_REASON_CODE).append(" INTEGER NOT NULL,")
                    .append(ChatData.KEY_DIRECTION).append(" INTEGER NOT NULL,")
                    .append(ChatData.KEY_TIMESTAMP).append(" INTEGER NOT NULL,")
                    .append(ChatData.KEY_USER_ABORTION).append(" INTEGER NOT NULL,")
                    .append(ChatData.KEY_CONTACT).append(" TEXT)").toString());
            db.execSQL(new StringBuilder("CREATE INDEX ").append(TABLE_GROUP_CHAT).append("_")
                    .append(ChatData.KEY_TIMESTAMP).append("_idx").append(" ON ")
                    .append(TABLE_GROUP_CHAT).append("(").append(ChatData.KEY_TIMESTAMP)
                    .append(")").toString());
            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(TABLE_MESSAGE)
                    .append("(").append(MessageData.KEY_CHAT_ID).append(" TEXT NOT NULL,")
                    .append(MessageData.KEY_CONTACT).append(" TEXT,")
                    .append(MessageData.KEY_MESSAGE_ID).append(" TEXT NOT NULL PRIMARY KEY,")
                    .append(MessageData.KEY_CONTENT).append(" TEXT,")
                    .append(MessageData.KEY_MIME_TYPE).append(" TEXT NOT NULL,")
                    .append(MessageData.KEY_DIRECTION).append(" INTEGER NOT NULL,")
                    .append(MessageData.KEY_STATUS).append(" INTEGER NOT NULL,")
                    .append(MessageData.KEY_REASON_CODE).append(" INTEGER NOT NULL,")
                    .append(MessageData.KEY_READ_STATUS).append(" INTEGER NOT NULL,")
                    .append(MessageData.KEY_TIMESTAMP).append(" INTEGER NOT NULL,")
                    .append(MessageData.KEY_TIMESTAMP_SENT).append(" INTEGER NOT NULL,")
                    .append(MessageData.KEY_TIMESTAMP_DELIVERED).append(" INTEGER NOT NULL,")
                    .append(MessageData.KEY_TIMESTAMP_DISPLAYED).append(" INTEGER NOT NULL)")
                    .toString());
            db.execSQL(new StringBuilder("CREATE INDEX ").append(TABLE_MESSAGE).append("_")
                    .append(MessageData.KEY_CHAT_ID).append("_idx").append(" ON ")
                    .append(TABLE_MESSAGE).append("(").append(MessageData.KEY_CHAT_ID).append(")")
                    .toString());
            db.execSQL(new StringBuilder("CREATE INDEX ").append(MessageData.KEY_TIMESTAMP)
                    .append("_idx").append(" ON ").append(TABLE_MESSAGE).append("(")
                    .append(MessageData.KEY_TIMESTAMP).append(")").toString());
            db.execSQL(new StringBuilder("CREATE INDEX ").append(MessageData.KEY_TIMESTAMP_SENT)
                    .append("_idx").append(" ON ").append(TABLE_MESSAGE).append("(")
                    .append(MessageData.KEY_TIMESTAMP_SENT).append(")").toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE_GROUP_CHAT));
            db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE_MESSAGE));
            onCreate(db);
        }
    }

    private SQLiteOpenHelper mOpenHelper;

    private String getSelectionWithChatId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_CHAT_ID_ONLY;
        }
        return new StringBuilder("(").append(SELECTION_WITH_CHAT_ID_ONLY).append(") AND (")
                .append(selection).append(")").toString();
    }

    private String[] getSelectionArgsWithChatId(String[] selectionArgs, String chatId) {
        String[] chatSelectionArg = new String[] {
            chatId
        };
        if (selectionArgs == null) {
            return chatSelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(chatSelectionArg, selectionArgs);
    }

    private String getSelectionWithMessageId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_MSG_ID_ONLY;
        }
        return new StringBuilder("(").append(SELECTION_WITH_MSG_ID_ONLY).append(") AND (")
                .append(selection).append(")").toString();
    }

    private String[] getSelectionArgsWithMessageId(String[] selectionArgs, String messageId) {
        String[] messageSelectionArg = new String[] {
            messageId
        };
        if (selectionArgs == null) {
            return messageSelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(messageSelectionArg, selectionArgs);
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

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalChat.CHAT:
                /* Intentional fall through */
            case UriType.Chat.CHAT:
                return CursorType.Chat.TYPE_DIRECTORY;

            case UriType.InternalChat.CHAT_WITH_ID:
                /* Intentional fall through */
            case UriType.Chat.CHAT_WITH_ID:
                return CursorType.Chat.TYPE_ITEM;

            case UriType.Message.MESSAGE:
                return CursorType.Message.TYPE_DIRECTORY;

            case UriType.Message.MESSAGE_WITH_ID:
                return CursorType.Message.TYPE_ITEM;

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sort) {
        Uri groupChatNotificationUri = ChatLog.GroupChat.CONTENT_URI;
        Cursor cursor = null;
        try {
            switch (sUriMatcher.match(uri)) {
                case UriType.InternalChat.CHAT_WITH_ID:
                    String chatId = uri.getLastPathSegment();
                    selection = getSelectionWithChatId(selection);
                    selectionArgs = getSelectionArgsWithChatId(selectionArgs, chatId);
                    groupChatNotificationUri = Uri.withAppendedPath(groupChatNotificationUri,
                            chatId);
                    /* Intentional fall through */
                case UriType.InternalChat.CHAT:
                    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_GROUP_CHAT, projection, selection, selectionArgs,
                            null, null, sort);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            groupChatNotificationUri);
                    return cursor;

                case UriType.Chat.CHAT_WITH_ID:
                    chatId = uri.getLastPathSegment();
                    selection = getSelectionWithChatId(selection);
                    selectionArgs = getSelectionArgsWithChatId(selectionArgs, chatId);
                    /* Intentional fall through */
                case UriType.Chat.CHAT:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_GROUP_CHAT,
                            restrictProjectionToExternallyDefinedColumns(projection), selection,
                            selectionArgs, null, null, sort);
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;

                case UriType.Message.MESSAGE_WITH_ID:
                    String msgId = uri.getLastPathSegment();
                    selection = getSelectionWithMessageId(selection);
                    selectionArgs = getSelectionArgsWithMessageId(selectionArgs, msgId);
                    /* Intentional fall through */
                case UriType.Message.MESSAGE:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_MESSAGE, projection, selection, selectionArgs,
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
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Uri groupChatNotificationUri = ChatLog.GroupChat.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalChat.CHAT_WITH_ID:
                String chatId = uri.getLastPathSegment();
                selection = getSelectionWithChatId(selection);
                selectionArgs = getSelectionArgsWithChatId(selectionArgs, chatId);
                groupChatNotificationUri = Uri.withAppendedPath(groupChatNotificationUri,
                        chatId);
                /* Intentional fall through */
            case UriType.InternalChat.CHAT:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.update(TABLE_GROUP_CHAT, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(groupChatNotificationUri, null);
                }
                return count;

            case UriType.Chat.CHAT_WITH_ID:
                /* Intentional fall through */
            case UriType.Chat.CHAT:
                throw new UnsupportedOperationException(new StringBuilder("This provider (URI=")
                        .append(uri).append(") supports read only access.").toString());

            case UriType.Message.MESSAGE_WITH_ID:
                String msgId = uri.getLastPathSegment();
                selection = getSelectionWithMessageId(selection);
                selectionArgs = getSelectionArgsWithMessageId(selectionArgs, msgId);
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                db = mOpenHelper.getWritableDatabase();
                count = db.update(TABLE_MESSAGE, values, selection, selectionArgs);
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
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalChat.CHAT:
                /* Intentional fall through */
            case UriType.InternalChat.CHAT_WITH_ID:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                String chatId = initialValues.getAsString(ChatData.KEY_CHAT_ID);
                db.insert(TABLE_GROUP_CHAT, null, initialValues);
                Uri notificationUri = Uri.withAppendedPath(ChatLog.GroupChat.CONTENT_URI, chatId);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            case UriType.Chat.CHAT_WITH_ID:
                /* Intentional fall through */
            case UriType.Chat.CHAT:
                throw new UnsupportedOperationException(new StringBuilder("This provider (URI=")
                        .append(uri).append(") supports read only access.").toString());

            case UriType.Message.MESSAGE:
                /* Intentional fall through */
            case UriType.Message.MESSAGE_WITH_ID:
                db = mOpenHelper.getWritableDatabase();
                String messageId = initialValues.getAsString(MessageData.KEY_MESSAGE_ID);
                db.insert(TABLE_MESSAGE, null, initialValues);
                notificationUri = Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, messageId);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Uri groupChatNotificationUri = ChatLog.GroupChat.CONTENT_URI;
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalChat.CHAT_WITH_ID:
                String chatId = uri.getLastPathSegment();
                selection = getSelectionWithChatId(selection);
                selectionArgs = getSelectionArgsWithChatId(selectionArgs, chatId);
                groupChatNotificationUri = Uri.withAppendedPath(groupChatNotificationUri,
                        chatId);
                /* Intentional fall through */
            case UriType.InternalChat.CHAT:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.delete(TABLE_GROUP_CHAT, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(groupChatNotificationUri, null);
                }
                return count;

            case UriType.Chat.CHAT_WITH_ID:
                /* Intentional fall through */
            case UriType.Chat.CHAT:
                throw new UnsupportedOperationException(new StringBuilder("This provider (URI=")
                        .append(uri).append(") supports read only access.").toString());

            case UriType.Message.MESSAGE_WITH_ID:
                String msgId = uri.getLastPathSegment();
                selection = getSelectionWithMessageId(selection);
                selectionArgs = getSelectionArgsWithMessageId(selectionArgs, msgId);
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                db = mOpenHelper.getWritableDatabase();
                count = db.delete(TABLE_MESSAGE, selection, selectionArgs);
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
