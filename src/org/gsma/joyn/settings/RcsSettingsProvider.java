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

package org.gsma.joyn.settings;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import java.lang.String;

/**
 * Class RcsSettingsProvider.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class RcsSettingsProvider extends ContentProvider {
    /**
     * Constant DATABASE_NAME.
     */
    public static final String DATABASE_NAME = "rcs_settings.db";

    /**
     * Creates a new instance of RcsSettingsProvider.
     */
    public RcsSettingsProvider() {
        super();
    }

    /**
     * Returns the type.
     *
     * @param 
     * @return  The type.
     */
    public String getType(Uri uri) {
        return (String) null;
    }

    /**
     *
     * @param 
     * @param 
     * @param 
     * @return  The int.
     */
    public int delete(Uri uri, String where, String[] whereArgs) {
        return 0;
    }

    /**
     *
     * @param 
     * @param 
     * @return  The uri.
     */
    public Uri insert(Uri uri, ContentValues initialValues) {
        return (Uri) null;
    }

    /**
     *
     * @param 
     * @param 
     * @param 
     * @param 
     * @param 
     * @return  The cursor.
     */
    public Cursor query(Uri uri, String[] projectionIn, String selection, String[] selectionArgs, String sort) {
        return (Cursor) null;
    }

    /**
     *
     * @param 
     * @param 
     * @param 
     * @param 
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
