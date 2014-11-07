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
import android.text.TextUtils;

import com.gsma.services.rcs.chat.ChatLog;
import com.orangelabs.rcs.utils.PhoneUtils;

/**
 * Chat provider
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatProvider extends ContentProvider {
	/**
	 * Database tables
	 */
    private static final String TABLE_CHAT = "chat";
    private static final String TABLE_MESSAGE = "message";

	// Create the constants used to differentiate between the different URI requests
	private static final int CHATS = 1;
    private static final int CHAT_ID = 2;
    private static final int RCSAPI_CHATS = 3;
    private static final int RCSAPI_CHAT_ID = 4;
    
	private static final int MESSAGES = 5;
    private static final int MESSAGE_ID = 6;
    private static final int RCSAPI_MESSAGES = 7;
    private static final int RCSAPI_MESSAGE_ID = 8;

	// Allocate the UriMatcher object
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI("com.orangelabs.rcs.chat", "chat", CHATS);
        uriMatcher.addURI("com.orangelabs.rcs.chat", "chat/#", CHAT_ID);
		uriMatcher.addURI("com.gsma.services.rcs.provider.chat", "chat", RCSAPI_CHATS);
		uriMatcher.addURI("com.gsma.services.rcs.provider.chat", "chat/#", RCSAPI_CHAT_ID);	
        uriMatcher.addURI("com.orangelabs.rcs.chat", "message", MESSAGES);
        uriMatcher.addURI("com.orangelabs.rcs.chat", "message/*", MESSAGE_ID);
		uriMatcher.addURI("com.gsma.services.rcs.provider.chat", "message", RCSAPI_MESSAGES);
		uriMatcher.addURI("com.gsma.services.rcs.provider.chat", "message/*", RCSAPI_MESSAGE_ID);
    }

    private static final int INVALID_ROW_ID = -1;
    
    /**
     * Database helper class
     */
    private SQLiteOpenHelper openHelper;
    
    /**
     * Database name
     */
    public static final String DATABASE_NAME = "chat.db";
    
    /**
     * Helper class for opening, creating and managing database version control
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 12;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        // @formatter:off
        @Override
        public void onCreate(SQLiteDatabase db) {
        	db.execSQL("CREATE TABLE " + TABLE_CHAT + " ("
        			+ ChatData.KEY_ID + " integer primary key autoincrement,"
        			+ ChatData.KEY_CHAT_ID + " TEXT,"
        			+ ChatData.KEY_REJOIN_ID + " TEXT,"
        			+ ChatData.KEY_SUBJECT + " TEXT,"
        			+ ChatData.KEY_PARTICIPANTS + " TEXT,"
        			+ ChatData.KEY_STATE + " integer,"
        			+ ChatData.KEY_REASON_CODE + " integer,"
        			+ ChatData.KEY_DIRECTION + " integer,"
        			+ ChatData.KEY_TIMESTAMP + " long,"
        			+ ChatData.KEY_REJECT_GC + " integer DEFAULT 0);");
        	db.execSQL("CREATE TABLE " + TABLE_MESSAGE + " ("
        			+ MessageData.KEY_ID + " integer primary key autoincrement,"
        			+ MessageData.KEY_CHAT_ID + " TEXT,"
        			+ MessageData.KEY_CONTACT + " TEXT,"
        			+ MessageData.KEY_MSG_ID + " TEXT,"
        			+ MessageData.KEY_CONTENT + " TEXT,"
        			+ MessageData.KEY_CONTENT_TYPE + " TEXT,"
        			+ MessageData.KEY_DIRECTION + " integer,"
        			+ MessageData.KEY_STATUS + " integer,"
        			+ MessageData.KEY_REASON_CODE + " integer,"
        			+ MessageData.KEY_READ_STATUS + " integer,"
        			+ MessageData.KEY_TIMESTAMP + " long,"
        			+ MessageData.KEY_TIMESTAMP_SENT + " long,"
        			+ MessageData.KEY_TIMESTAMP_DELIVERED + " long,"
        			+ MessageData.KEY_TIMESTAMP_DISPLAYED + " long);");
        }
        // @formatter:on
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGE);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        openHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        int match = uriMatcher.match(uri);
        switch(match) {
            case CHATS:
			case RCSAPI_CHATS:
                return "vnd.android.cursor.dir/chat";
            case CHAT_ID:
			case RCSAPI_CHAT_ID:
                return "vnd.android.cursor.item/chat";
            case MESSAGES:
			case RCSAPI_MESSAGES:
                return "vnd.android.cursor.dir/message";
            case MESSAGE_ID:
			case RCSAPI_MESSAGE_ID:
                return "vnd.android.cursor.item/message";
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projectionIn, String selection, String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        boolean queryMessage = true;
        // Generate the body of the query
        int match = uriMatcher.match(uri);
        switch(match) {
            case CHATS:
			case RCSAPI_CHATS:
		        qb.setTables(TABLE_CHAT);
		        queryMessage = false;
		        break;
			case MESSAGES:
			case RCSAPI_MESSAGES:
		        qb.setTables(TABLE_MESSAGE);
                break;
			case CHAT_ID:
			case RCSAPI_CHAT_ID:
		        qb.setTables(TABLE_CHAT);
                qb.appendWhere(ChatData.KEY_CHAT_ID + "= '"+uri.getPathSegments().get(1)+"'");
                queryMessage = false;
                break;
			case MESSAGE_ID:
			case RCSAPI_MESSAGE_ID:
		        qb.setTables(TABLE_MESSAGE);
                qb.appendWhere(MessageData.KEY_CHAT_ID + "= '" +uri.getPathSegments().get(1) + "'");
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor c = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);

		// Register the contexts ContentResolver to be notified if the cursor result set changes
        if (c != null) {
        	if (queryMessage) {
        		c.setNotificationUri(getContext().getContentResolver(), ChatLog.Message.CONTENT_URI);
        	} else {
        		c.setNotificationUri(getContext().getContentResolver(), ChatLog.GroupChat.CONTENT_URI);
        	}
        }
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count = 0;
        SQLiteDatabase db = openHelper.getWritableDatabase();
        boolean updateMessage = true;
        int match = uriMatcher.match(uri);
        switch(match) {
	        case CHATS:
	            count = db.update(TABLE_CHAT, values, where, whereArgs);
	            updateMessage = false;
		        break;
			case MESSAGES:
	            count = db.update(TABLE_MESSAGE, values, where, whereArgs);
	            break;
			case CHAT_ID:
                count = db.update(TABLE_CHAT, values,
                		ChatData.KEY_ID + "=" + Integer.parseInt(uri.getPathSegments().get(1)), null);
                updateMessage = false;
	            break;
			case MESSAGE_ID:
                count = db.update(TABLE_MESSAGE, values,
                		MessageData.KEY_ID + "=" + Integer.parseInt(uri.getPathSegments().get(1)), null);
	            break;
            default:
                throw new UnsupportedOperationException("Cannot update URI " + uri);
        }
        if (count != 0) {
        	if (updateMessage) {
        		getContext().getContentResolver().notifyChange(ChatLog.Message.CONTENT_URI, null);
        	} else {
        		getContext().getContentResolver().notifyChange(ChatLog.GroupChat.CONTENT_URI, null);
        	}
        }
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        switch(uriMatcher.match(uri)) {
	        case CHATS:
	        case CHAT_ID:
	        	long chatRowId = db.insert(TABLE_CHAT, null, initialValues);
	    		uri = ContentUris.withAppendedId(ChatData.CONTENT_URI, chatRowId);
	    		if (chatRowId != INVALID_ROW_ID)  {
	    			getContext().getContentResolver().notifyChange(ChatLog.GroupChat.CONTENT_URI, null);
	    		}
	        	break;
	        case MESSAGES:
	        case MESSAGE_ID:
	    		long msgRowId = db.insert(TABLE_MESSAGE, null, initialValues);
	    		uri = ContentUris.withAppendedId(MessageData.CONTENT_URI, msgRowId);
	    		if (msgRowId != INVALID_ROW_ID)  {
	    			getContext().getContentResolver().notifyChange(ChatLog.Message.CONTENT_URI, null);
	    		}
	        	break;
	        default:
	    		throw new SQLException("Failed to insert row into " + uri);
        }
        return uri;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        int count = 0;
        boolean deleteMessage = true;
        switch(uriMatcher.match(uri)) {
	        case CHATS:
	        case RCSAPI_CHATS:
	        	count = db.delete(TABLE_CHAT, where, whereArgs);
	        	deleteMessage = false;
	        	break;
	        case CHAT_ID:
	        case RCSAPI_CHAT_ID:
				count = db.delete(TABLE_CHAT, ChatData.KEY_ID + "="
						+ uri.getPathSegments().get(1)
						+ (!TextUtils.isEmpty(where) ? " AND ("	+ where + ')' : ""),
						whereArgs);
				deleteMessage = false;
				break;
	        case MESSAGES:
	        case RCSAPI_MESSAGES:
	        	count = db.delete(TABLE_MESSAGE, where, whereArgs);
	        	break;
	        case MESSAGE_ID:
	        case RCSAPI_MESSAGE_ID:
				count = db.delete(TABLE_MESSAGE, MessageData.KEY_ID + "="
						+ PhoneUtils.formatNumberToInternational(uri.getPathSegments().get(1))
						+ (!TextUtils.isEmpty(where) ? " AND ("	+ where + ')' : ""),
						whereArgs);
				break;
	        default:
	    		throw new SQLException("Failed to delete row " + uri);
        }
        if (count != 0) {
        	if (deleteMessage) {
        		getContext().getContentResolver().notifyChange(ChatLog.Message.CONTENT_URI, null);
        	} else {
        		getContext().getContentResolver().notifyChange(ChatLog.GroupChat.CONTENT_URI, null);
        	}
        }
        return count;    
    }
}
