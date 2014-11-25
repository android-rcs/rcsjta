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

package com.orangelabs.rcs.provider;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class LocalContentResolver {

    private final ContentResolver mContentResolver;

    public LocalContentResolver(ContentResolver contentResolver) {
        mContentResolver = contentResolver;
    }

    public final Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        ContentProviderClient contentProviderClient = null;
        try {
            contentProviderClient = mContentResolver.acquireContentProviderClient(uri);
            return contentProviderClient.getLocalContentProvider().query(uri, projection,
                    selection, selectionArgs, sortOrder);

        } finally {
            if (contentProviderClient != null) {
                contentProviderClient.release();
            }
        }
    }

    public final Uri insert(Uri uri, ContentValues values) {
        ContentProviderClient contentProviderClient = null;
        try {
            contentProviderClient = mContentResolver.acquireContentProviderClient(uri);
            return contentProviderClient.getLocalContentProvider().insert(uri, values);

        } finally {
            if (contentProviderClient != null) {
                contentProviderClient.release();
            }
        }
    }

    public final int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        ContentProviderClient contentProviderClient = null;
        try {
            contentProviderClient = mContentResolver.acquireContentProviderClient(uri);
            return contentProviderClient.getLocalContentProvider().update(uri, values, selection,
                    selectionArgs);

        } finally {
            if (contentProviderClient != null) {
                contentProviderClient.release();
            }
        }
    }

    public final int delete(Uri uri, String selection, String[] selectionArgs) {
        ContentProviderClient contentProviderClient = null;
        try {
            contentProviderClient = mContentResolver.acquireContentProviderClient(uri);
            return contentProviderClient.getLocalContentProvider().delete(uri, selection,
                    selectionArgs);

        } finally {
            if (contentProviderClient != null) {
                contentProviderClient.release();
            }
        }
    }
}
