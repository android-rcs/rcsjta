/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.provider.sharing;

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

/**
 * Image sharing provider
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingProvider extends ContentProvider {
	// Database table
	public static final String TABLE = "ish";
	
	// Create the constants used to differentiate between the different
	// URI requests
	private static final int IMAGESHARES = 1;
	private static final int IMAGESHARE_ID = 2;
    private static final int RCSAPI = 3;
	
	// Allocate the UriMatcher object
	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI("com.orangelabs.rcs.ish", "ish", IMAGESHARES);
		uriMatcher.addURI("com.orangelabs.rcs.ish", "ish/#", IMAGESHARE_ID);
		uriMatcher.addURI("org.gsma.joyn.provider.ish", "ish", RCSAPI);
	}

    /**
     * Database helper class
     */
    private SQLiteOpenHelper openHelper;
    
    /**
     * Database name
     */
    public static final String DATABASE_NAME = "ish.db";

    /**
     * Helper class for opening, creating and managing database version control
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	db.execSQL("CREATE TABLE " + TABLE + " ("
        			+ ImageSharingData.KEY_ID + " integer primary key autoincrement,"
        			+ ImageSharingData.KEY_SESSION_ID + " TEXT,"
        			+ ImageSharingData.KEY_CONTACT + " TEXT,"
        			+ ImageSharingData.KEY_NAME + " TEXT,"
        			+ ImageSharingData.KEY_MIME_TYPE + " TEXT,"
        			+ ImageSharingData.KEY_STATUS + " integer,"
        			+ ImageSharingData.KEY_DIRECTION + " integer,"
        			+ ImageSharingData.KEY_TIMESTAMP + " long,"
        			+ ImageSharingData.KEY_SIZE + " long,"
        			+ ImageSharingData.KEY_TOTAL_SIZE + " long);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
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
		switch(uriMatcher.match(uri)){
			case IMAGESHARES:
				return "vnd.android.cursor.dir/com.orangelabs.rcs.ish";
			case IMAGESHARE_ID:
				return "vnd.android.cursor.item/com.orangelabs.rcs.ish";
			case RCSAPI:
				return "vnd.android.cursor.item/com.orangelabs.rcs.ish";
			default:
				throw new IllegalArgumentException("Unsupported URI " + uri);
		}
	}
	
    @Override
    public Cursor query(Uri uri, String[] projectionIn, String selection, String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE);

        // Generate the body of the query
        int match = uriMatcher.match(uri);
        switch(match) {
            case IMAGESHARES:
                break;
            case IMAGESHARE_ID:
                qb.appendWhere(ImageSharingData.KEY_ID + "=" + uri.getPathSegments().get(1));
                break;
        	case RCSAPI:
        		break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor c = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);

		// Register the contexts ContentResolver to be notified if the cursor result set changes.
        c.setNotificationUri(getContext().getContentResolver(), ImageSharingData.CONTENT_URI);

        return c;
    }
    
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count;
        SQLiteDatabase db = openHelper.getWritableDatabase();

        int match = uriMatcher.match(uri);
        switch (match) {
	        case IMAGESHARES:
	            count = db.update(TABLE, values, where, null);
	            break;
            case IMAGESHARE_ID:
                String segment = uri.getPathSegments().get(1);
                int id = Integer.parseInt(segment);
                count = db.update(TABLE, values, ImageSharingData.KEY_ID + "=" + id, null);
                break;
            default:
                throw new UnsupportedOperationException("Cannot update URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
    
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        switch(uriMatcher.match(uri)){
	        case IMAGESHARES:
	        case IMAGESHARE_ID:
	            // Insert the new row, will return the row number if successful
	        	// Use system clock to generate id : it should not be a common int otherwise it could be the 
	        	// same as an id present in MmsSms table (and that will create uniqueness problem when doing the tables merge) 
	        	int id = (int)System.currentTimeMillis();
	        	if (Integer.signum(id) == -1){
	        		// If generated id is <0, it is problematic for uris
	        		id = -id;
	        	}
	        	initialValues.put(ImageSharingData.KEY_ID, id);
	    		long rowId = db.insert(TABLE, null, initialValues);
	    		uri = ContentUris.withAppendedId(ImageSharingData.CONTENT_URI, rowId);
	        	break;
	        default:
	    		throw new SQLException("Failed to insert row into " + uri);
        }
		getContext().getContentResolver().notifyChange(uri, null);
        return uri;
    }
    
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        int count = 0;
        switch(uriMatcher.match(uri)){
	        case IMAGESHARES:
	        	count = db.delete(TABLE, where, whereArgs);
	        	break;
	        case IMAGESHARE_ID:
	        	String segment = uri.getPathSegments().get(1);
				count = db.delete(TABLE, ImageSharingData.KEY_ID + "="
						+ segment
						+ (!TextUtils.isEmpty(where) ? " AND ("	+ where + ')' : ""),
						whereArgs);
				
				break;
	        	
	        default:
	    		throw new SQLException("Failed to delete row " + uri);
        }
		getContext().getContentResolver().notifyChange(uri, null);
        return count;    
   }	
}
