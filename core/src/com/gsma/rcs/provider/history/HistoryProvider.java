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

package com.gsma.rcs.provider.history;

import com.gsma.services.rcs.history.HistoryLog;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.security.ProviderException;
import java.util.List;

public class HistoryProvider extends MultiDbProvider {

    /**
     * The number of databases will not exceed 20 as SQlite cannot attach more.
     */
    public static final int MAX_ATTACHED_PROVIDERS = 20;

    private static final class CursorType {

        private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/history";

    }

    private static final class UriType {

        private static final int BASE = 1;

    }

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(HistoryLog.CONTENT_URI.getAuthority(), HistoryLog.CONTENT_URI.getPath()
                .substring(1), UriType.BASE);
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.BASE:
                return CursorType.TYPE_DIRECTORY;

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sort) {
        List<String> historyLogMembers = uri.getQueryParameters(HistoryLog.PROVIDER_ID);
        ensureDatabasesAttached(historyLogMembers);
        StringBuilder query = new StringBuilder("SELECT ");
        if (projection == null) {
            SQLiteQueryBuilder.appendColumns(query, HistoryConstants.FULL_PROJECTION);
        } else {
            SQLiteQueryBuilder.appendColumns(query, projection);
        }
        String unionQuery = mQueryHelper.generateUnionQuery(historyLogMembers, selectionArgs,
                selection);
        query.append(" FROM (").append(unionQuery).append(")");
        if (selectionArgs != null && !TextUtils.isEmpty(selection)) {
            query.append(" WHERE ").append(selection);
        }
        if (sort != null) {
            query.append(" ORDER BY ").append(sort);
        }
        return executeReadQuery(query.toString(), selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        throw new ProviderException("Operation not supported!");
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        throw new ProviderException("Operation not supported!");
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        throw new ProviderException("Operation not supported!");
    }

}
