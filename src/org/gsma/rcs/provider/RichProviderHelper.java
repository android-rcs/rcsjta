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

package org.gsma.rcs.provider;

/**
 * Class RichProviderHelper.
 */
public class RichProviderHelper extends android.database.sqlite.SQLiteOpenHelper {
    /**
     * Creates a new instance of RichProviderHelper.
     *  
     * @param arg1 The arg1.
     */
    private RichProviderHelper(android.content.Context arg1) {
        super((android.content.Context) null, (java.lang.String) null, (android.database.sqlite.SQLiteDatabase.CursorFactory) null, 0);
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void onCreate(android.database.sqlite.SQLiteDatabase arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     */
    public void onUpgrade(android.database.sqlite.SQLiteDatabase arg1, int arg2, int arg3) {

    }

    /**
     * Returns the instance.
     *  
     * @return  The instance.
     */
    public static RichProviderHelper getInstance() {
        return (RichProviderHelper) null;
    }

    /**
     * Creates the instance.
     *  
     * @param arg1 The arg1.
     */
    public static synchronized void createInstance(android.content.Context arg1) {

    }

} // end RichProviderHelper
