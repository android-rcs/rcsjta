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

package org.gsma.rcs.settings;

/**
 * Class RcsSettingsProvider.
 */
public class RcsSettingsProvider extends android.content.ContentProvider {
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
     * @param arg1 The arg1.
     * @return  The type.
     */
    public String getType(android.net.Uri arg1) {
        return (java.lang.String) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3 array.
     * @return  The int.
     */
    public int delete(android.net.Uri arg1, String arg2, String[] arg3) {
        return 0;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The uri.
     */
    public android.net.Uri insert(android.net.Uri arg1, android.content.ContentValues arg2) {
        return (android.net.Uri) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2 array.
     * @param arg3 The arg3.
     * @param arg4 The arg4 array.
     * @param arg5 The arg5.
     * @return  The cursor.
     */
    public android.database.Cursor query(android.net.Uri arg1, String[] arg2, String arg3, String[] arg4, String arg5) {
        return (android.database.Cursor) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     * @param arg4 The arg4 array.
     * @return  The int.
     */
    public int update(android.net.Uri arg1, android.content.ContentValues arg2, String arg3, String[] arg4) {
        return 0;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean onCreate() {
        return false;
    }

} // end RcsSettingsProvider
