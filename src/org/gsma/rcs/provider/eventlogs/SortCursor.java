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

package org.gsma.rcs.provider.eventlogs;

/**
 * Class SortCursor.
 */
public class SortCursor extends android.database.AbstractCursor {
    /**
     * Constant TYPE_STRING.
     */
    public static final int TYPE_STRING = 0;

    /**
     * Constant TYPE_NUMERIC.
     */
    public static final int TYPE_NUMERIC = 1;

    /**
     * Creates a new instance of SortCursor.
     *  
     * @param arg1 The arg1 array.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     * @param arg4 The arg4.
     */
    public SortCursor(android.database.Cursor[] arg1, String arg2, int arg3, boolean arg4) {
        super();
    }

    /**
     * Returns the short.
     *  
     * @param arg1 The arg1.
     * @return  The short.
     */
    public short getShort(int arg1) {
        return (short) 0;
    }

    /**
     * Returns the int.
     *  
     * @param arg1 The arg1.
     * @return  The int.
     */
    public int getInt(int arg1) {
        return 0;
    }

    /**
     * Returns the long.
     *  
     * @param arg1 The arg1.
     * @return  The long.
     */
    public long getLong(int arg1) {
        return 0l;
    }

    /**
     * Returns the float.
     *  
     * @param arg1 The arg1.
     * @return  The float.
     */
    public float getFloat(int arg1) {
        return 0.0f;
    }

    /**
     * Returns the double.
     *  
     * @param arg1 The arg1.
     * @return  The double.
     */
    public double getDouble(int arg1) {
        return 0.0d;
    }

    public void close() {

    }

    /**
     * Returns the string.
     *  
     * @param arg1 The arg1.
     * @return  The string.
     */
    public String getString(int arg1) {
        return (java.lang.String) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @return  The boolean.
     */
    public boolean isNull(int arg1) {
        return false;
    }

    /**
     * Returns the count.
     *  
     * @return  The count.
     */
    public int getCount() {
        return 0;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void registerDataSetObserver(android.database.DataSetObserver arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void unregisterDataSetObserver(android.database.DataSetObserver arg1) {

    }

    /**
     * Returns the column names.
     *  
     * @return  The column names array.
     */
    public String[] getColumnNames() {
        return (java.lang.String []) null;
    }

    /**
     * Returns the blob.
     *  
     * @param arg1 The arg1.
     * @return  The blob array.
     */
    public byte[] getBlob(int arg1) {
        return (byte []) null;
    }

    public void deactivate() {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean requery() {
        return false;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The boolean.
     */
    public boolean onMove(int arg1, int arg2) {
        return false;
    }

    /**
     * Returns the current cursor index.
     *  
     * @return  The current cursor index.
     */
    public int getCurrentCursorIndex() {
        return 0;
    }

} // end SortCursor
