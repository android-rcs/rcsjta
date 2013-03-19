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

package org.gsma.joyn.messaging;

import java.lang.String;

/**
 * Class RichMessagingProvider.
 *
 * @author mhsm6403 (Orange)
 * @version 1.0
 * @since 1.0
 */
public class RichMessagingProvider extends android.content.ContentProvider {
    /**
     * Constant TABLE.
     */
    public static final String TABLE = "messaging";

    /**
     * Creates a new instance of RichMessagingProvider.
     */
    public RichMessagingProvider() {
        super();
    }

    /**
     * Returns the type.
     *
     * @param 
     * @return  The type.
     */
    public String getType(android.net.Uri uri) {
        return (String) null;
    }

    /**
     *
     * @param 
     * @param 
     * @param 
     * @return  The int.
     */
    public int delete(android.net.Uri uri, String where, String[] whereArgs) {
        return 0;
    }

    /**
     *
     * @param 
     * @param 
     * @return  The uri.
     */
    public android.net.Uri insert(android.net.Uri uri, android.content.ContentValues initialValues) {
        return (android.net.Uri) null;
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
    public android.database.Cursor query(android.net.Uri uri, String[] projectionIn, String selection, String[] selectionArgs, String sort) {
        return (android.database.Cursor) null;
    }

    /**
     *
     * @param 
     * @param 
     * @param 
     * @param 
     * @return  The int.
     */
    public int update(android.net.Uri uri, android.content.ContentValues values, String where, String[] whereArgs) {
        return 0;
    }

    /**
     *
     * @return  The boolean.
     */
    public boolean onCreate() {
        return false;
    }

} // end RichMessagingProvider
