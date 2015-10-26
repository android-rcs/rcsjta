/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.provider;

import com.gsma.rcs.service.api.ServerApiPersistentStorageException;

import android.database.Cursor;
import android.net.Uri;

public class CursorUtil {

    public static void assertCursorIsNotNull(Cursor cursor, Uri requestedUri) {
        if (cursor == null) {
            throw new ServerApiPersistentStorageException(new StringBuilder("Query failed: ")
                    .append(requestedUri).toString());
        }
    }

    public static void assertCursorIsNotNull(Cursor cursor, String queriedTable) {
        if (cursor == null) {
            throw new ServerApiPersistentStorageException(new StringBuilder("Query failed: ")
                    .append(queriedTable).toString());
        }
    }

    public static final void close(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

}
