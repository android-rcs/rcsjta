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

package com.orangelabs.rcs.utils;

import android.os.Environment;
import android.os.StatFs;

/**
 * Storage utility functions
 *
 * @author Deutsche Telekom AG
 */
public class StorageUtils {

    /**
     * Verify if external storage is available (read or write) using
     * {@link Environment} external storage state
     *
     * @see Environment#MEDIA_MOUNTED
     * @see Environment#MEDIA_MOUNTED_READ_ONLY
     * @return <code>true</code> if available, otherwise <code>false</code>
     */
    public static boolean hasExternalStorage() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    /**
     * Get available space in external storage, only if external storage is
     * ready to write
     *
     * @return Available space in bytes, otherwise <code>-1</code>
     */
    public static long getExternalStorageFreeSpace() {
        long freeSpace = -1;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            freeSpace = blockSize * availableBlocks;
        }
        return freeSpace;
    }
}
