/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.joyn.richcall;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import java.lang.String;

/**
 * Rich call content provider
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class RichCallProvider extends ContentProvider {

    /**
     * Database table name
     */
    public static final String TABLE = "csh";

    /**
     * Creates a new instance of RichCallProvider.
     */
    public RichCallProvider() {
        super();
    }

    /**
     * Returns the type.
     *
     * @param uri
     * @return  The type.
     */
    public String getType(Uri uri) {
        return (String) null;
    }

    /**
     * @param uri
     * @param where
     * @param whereArgs
     * @return  The int.
     */
    public int delete(Uri uri, String where, String[] whereArgs) {
        return 0;
    }

    /**
     * @param uri
     * @param initialValues
     * @return C
     */
    public Uri insert(Uri uri, ContentValues initialValues) {
        return (Uri) null;
    }

    /**
     * @param uri
     * @param projectionIn
     * @param selection
     * @param selectionArgs
     * @param sort
     * @return  The cursor.
     */
    public Cursor query(Uri uri, String[] projectionIn, String selection, String[] selectionArgs, String sort) {
        return (Cursor) null;
    }

    /**
     * @param uri
     * @param values
     * @param where
     * @param whereArgs
     * @return  The int.
     */
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        return 0;
    }

    /**
     *
     * @return  The boolean.
     */
    public boolean onCreate() {
        return false;
    }

}
