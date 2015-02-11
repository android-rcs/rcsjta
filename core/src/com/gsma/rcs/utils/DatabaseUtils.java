/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.utils;

import android.os.ParcelFileDescriptor;

import java.util.HashMap;
import java.util.Map;

public class DatabaseUtils {

    private static final Map<String, Integer> sModeBits = new HashMap<String, Integer>();;
    static {
        sModeBits.put("r", ParcelFileDescriptor.MODE_READ_ONLY);
        sModeBits.put("w", ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE
                | ParcelFileDescriptor.MODE_TRUNCATE);
        sModeBits.put("wt", ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE
                | ParcelFileDescriptor.MODE_TRUNCATE);
        sModeBits.put("wa", ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE
                | ParcelFileDescriptor.MODE_APPEND);
        sModeBits
                .put("rw", ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE);
        sModeBits.put("rwt", ParcelFileDescriptor.MODE_READ_WRITE
                | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
    }

    public static String[] appendSelectionArgs(String[] selectionArgs,
            String[] selectionArgsToAppend) {
        String[] resultingSelectionArgs = new String[selectionArgs.length
                + selectionArgsToAppend.length];
        System.arraycopy(selectionArgs, 0, resultingSelectionArgs, 0, selectionArgs.length);
        System.arraycopy(selectionArgsToAppend, 0, resultingSelectionArgs, selectionArgs.length,
                selectionArgsToAppend.length);
        return resultingSelectionArgs;
    }

    public static int parseMode(String mode) {
        Integer modeBits = sModeBits.get(mode);
        if (modeBits == null) {
            throw new IllegalArgumentException("Bad mode '" + mode + "!");
        }
        return modeBits.intValue();
    }
}
