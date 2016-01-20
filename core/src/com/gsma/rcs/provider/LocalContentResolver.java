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

package com.gsma.rcs.provider;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * The purpose of this class is to allow query-/insert-/update-/delete- and stream operations
 * directly on the local providers. The usage of this class allows the local providers to be write
 * protected for external use like from applications accessing the terminal api while still allowing
 * internal write access to the same providers by the service implementations.
 */

public class LocalContentResolver {

    private final ContentResolver mContentResolver;

    /**
     * Constructor
     * 
     * @param contentResolver the content resolver
     */
    public LocalContentResolver(ContentResolver contentResolver) {
        mContentResolver = contentResolver;
    }

    /**
     * Constructor
     * 
     * @param context the context
     */
    public LocalContentResolver(Context context) {
        this(context.getContentResolver());
    }

    /**
     * Handles query requests from clients
     * 
     * @param uri the URI to query
     * @param projection The list of columns to put into the cursor of null if all
     * @param selection A selection criteria to apply when filtering rows
     * @param selectionArgs The array of arguments for the selection or null if no argument.
     * @param sortOrder the sording order
     * @return a Cursor or null.
     */
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

    /**
     * Handles requests to insert a new row.
     * 
     * @param uri the URI
     * @param values A set of column_name/value pairs to add to the database.
     * @return The URI for the newly inserted item
     */
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

    /**
     * Handle requests to update one or more rows.
     * 
     * @param uri the URI
     * @param values A set of column_name/value pairs to update
     * @param selection A selection criteria to apply when filtering rows
     * @param selectionArgs The array of arguments for the selection or null if no argument.
     * @return the number of rows affected.
     */
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

    /**
     * Handles requests to delete one or more rows.
     * 
     * @param uri the URI
     * @param selection A selection criteria to apply when filtering rows
     * @param selectionArgs The array of arguments for the selection or null if no argument.
     * @return The number of rows affected.
     */
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

    /**
     * Create and return a new auto-close input stream for this URI
     * 
     * @param uri the URI
     * @return the InputStream
     * @throws FileNotFoundException
     */
    public final InputStream openContentInputStream(Uri uri) throws FileNotFoundException {
        ContentProviderClient contentProviderClient = null;
        try {
            contentProviderClient = mContentResolver.acquireContentProviderClient(uri);
            return contentProviderClient.getLocalContentProvider().openAssetFile(uri, "r")
                    .createInputStream();

        } catch (IOException e) {
            throw new FileNotFoundException("Unable to create stream");
        } finally {
            if (contentProviderClient != null) {
                contentProviderClient.release();
            }
        }
    }

    /**
     * Create and return a new auto-close output stream for this URI
     * 
     * @param uri the URI
     * @return the InputStream
     * @throws FileNotFoundException
     */
    public final OutputStream openContentOutputStream(Uri uri) throws FileNotFoundException {
        ContentProviderClient contentProviderClient = null;
        try {
            contentProviderClient = mContentResolver.acquireContentProviderClient(uri);
            return contentProviderClient.getLocalContentProvider().openAssetFile(uri, "w")
                    .createOutputStream();

        } catch (IOException e) {
            throw new FileNotFoundException("Unable to create stream");
        } finally {
            if (contentProviderClient != null) {
                contentProviderClient.release();
            }
        }
    }

    /**
     * Handles request to update multiple rows
     *
     * @param uri the URI
     * @param operations the list of the operations to apply
     * @return the results of the applications
     * @throws OperationApplicationException
     */
    public final ContentProviderResult[] applyBatch(Uri uri,
            ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        ContentProviderClient contentProviderClient = null;
        try {
            contentProviderClient = mContentResolver.acquireContentProviderClient(uri);
            return contentProviderClient.getLocalContentProvider().applyBatch(operations);
        } finally {
            if (contentProviderClient != null) {
                contentProviderClient.release();
            }
        }
    }
}
