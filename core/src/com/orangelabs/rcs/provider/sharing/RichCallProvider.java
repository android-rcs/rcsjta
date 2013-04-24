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

import com.orangelabs.rcs.provider.RichProviderHelper;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Rich call history provider
 * 
 * @author mhsm6403
 */
public class RichCallProvider extends ContentProvider {
	// Database table
	public static final String TABLE = "csh";
	
	// Create the constants used to differentiate between the different
	// URI requests
	private static final int CSH = 1;
	private static final int CSH_ID = 2;
	
	// Allocate the UriMatcher object, where a URI ending in 'contacts'
	// will correspond to a request for all contacts, and 'contacts'
	// with a trailing '/[rowID]' will represent a single contact row.
	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI("com.orangelabs.rcs.csh", "csh", CSH);
		uriMatcher.addURI("com.orangelabs.rcs.csh", "csh/#", CSH_ID);
	}

    /**
     * Database helper class
     */
    private SQLiteOpenHelper openHelper;

	@Override 
	public boolean onCreate() {
		if(RichProviderHelper.getInstance()==null){
        	RichProviderHelper.createInstance(this.getContext());
        }
        this.openHelper = RichProviderHelper.getInstance();
        return true;
	}

	@Override
	public String getType(Uri uri) {
		switch(uriMatcher.match(uri)){
			case CSH:
				return "vnd.android.cursor.dir/com.orangelabs.rcs.csh";
			case CSH_ID:
				return "vnd.android.cursor.item/com.orangelabs.rcs.csh";
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
            case CSH:
                break;
            case CSH_ID:
                qb.appendWhere(RichCallData.KEY_ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor c = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);

		// Register the contexts ContentResolver to be notified if the cursor result set changes.
        c.setNotificationUri(getContext().getContentResolver(), RichCallData.CONTENT_URI);

        return c;
    }
    
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count;
        SQLiteDatabase db = openHelper.getWritableDatabase();

        int match = uriMatcher.match(uri);
        switch (match) {
	        case CSH:
	            count = db.update(TABLE, values, where, null);
	            break;
            case CSH_ID:
                String segment = uri.getPathSegments().get(1);
                int id = Integer.parseInt(segment);
                count = db.update(TABLE, values, RichCallData.KEY_ID + "=" + id, null);
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
	        case CSH:
	        case CSH_ID:
	            // Insert the new row, will return the row number if successful
	        	// Use system clock to generate id : it should not be a common int otherwise it could be the 
	        	// same as an id present in MmsSms table (and that will create uniqueness problem when doing the tables merge) 
	        	int id = (int)System.currentTimeMillis();
	        	if (Integer.signum(id) == -1){
	        		// If generated id is <0, it is problematic for uris
	        		id = -id;
	        	}
	        	initialValues.put(RichCallData.KEY_ID, id);
	    		long rowId = db.insert(TABLE, null, initialValues);
	    		uri = ContentUris.withAppendedId(RichCallData.CONTENT_URI, rowId);
	        	break;
	        default:
	    		throw new SQLException("Failed to insert row into " + uri);
        }
		getContext().getContentResolver().notifyChange(uri, null);
        return uri;
    }
    
    /**
     * This method should not be used if deletion isn't made on the whole messages of a contact.
     * Prefer methods from RichCall class, otherwise Recycler wont work.
     *  
     * If all messages of a contact, or all rich messages are to be deleted, this method could be used.
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        int count = 0;
        switch(uriMatcher.match(uri)){
	        case CSH:
	        	count = db.delete(TABLE, where, whereArgs);
	        	break;
	        case CSH_ID:
	        	String segment = uri.getPathSegments().get(1);
				count = db.delete(TABLE, RichCallData.KEY_ID + "="
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
