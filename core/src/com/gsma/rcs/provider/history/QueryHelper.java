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

import static com.gsma.rcs.provider.history.HistoryConstants.FULL_PROJECTION;

import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;
import android.util.SparseArray;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* package private */class QueryHelper {

    private final Map<String[], String> mUriQueryCache = new HashMap<String[], String>();

    private final SparseArray<String> mSubQueries = new SparseArray<String>();

    /* package private */String generateSubQuery(int providerId, Map<String, String> columnMapper,
            String tablename) {
        StringBuilder query = null;
        for (String providerField : FULL_PROJECTION) {
            if (query == null) {
                query = new StringBuilder("SELECT ");
            } else {
                query.append(",");
            }
            String databaseField = columnMapper.get(providerField);
            if (providerField == HistoryLogData.KEY_PROVIDER_ID) {
                databaseField = Integer.toString(providerId);
            }
            query.append(databaseField).append(" AS ").append(providerField);
        }
        query.append(" FROM ").append(tablename);
        String subQuery = query.toString();
        mSubQueries.put(providerId, subQuery);
        return subQuery;
    }

    private StringBuilder generateParamlessSubQuery(int providerId, String selection) {
        StringBuilder query = new StringBuilder(mSubQueries.get(providerId));
        if (!TextUtils.isEmpty(selection)) {
            query.append(" WHERE ").append(selection);
        }
        return query;
    }

    private String generateParamlessUnionQuery(List<String> historyLogMembers, String selection) {
        String subQueries[] = new String[historyLogMembers.size()];
        int i = 0;
        for (String historyLogMember : historyLogMembers) {
            int providerId = Integer.valueOf(historyLogMember);
            subQueries[i++] = generateParamlessSubQuery(providerId, selection).toString();
        }
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        return queryBuilder.buildUnionQuery(subQueries, null, null);
    }

    private String generateUnionQuery(List<String> historyLogMembers) {
        String[] keyQueryCache = getKey(historyLogMembers);
        String unionQuery = mUriQueryCache.get(keyQueryCache);
        if (unionQuery != null) {
            return unionQuery;
        }
        String subQueries[] = new String[historyLogMembers.size()];
        int i = 0;
        for (String historyLogMember : historyLogMembers) {
            int providerId = Integer.valueOf(historyLogMember);
            subQueries[i++] = mSubQueries.get(providerId);
        }
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        unionQuery = queryBuilder.buildUnionQuery(subQueries, null, null);
        mUriQueryCache.put(keyQueryCache, unionQuery);
        return unionQuery;
    }

    /**
     * Will return a unique key for a specific set of strings
     * 
     * @param set
     * @return the generated key
     */
    private static final String[] getKey(List<String> providerIds) {
        String[] key = new String[providerIds.size()];
        providerIds.toArray(key);
        Arrays.sort(key);
        return key;
    }

    private static final boolean contains(String[] key, int providerId) {
        return Arrays.binarySearch(key, Integer.toString(providerId)) >= 0;
    }

    /**
     * When no selection arguments are given there is no need to bind selection parameters. Since
     * the history provider uses a union query with a surrounding selection clause the query can in
     * such case be optimized so that the selection is done on the internal level, which is a major
     * speed up in common use cases.
     */
    /* package private */String generateUnionQuery(List<String> historyLogMembers,
            String[] selectionArgs, String selection) {
        if (selectionArgs == null) {
            return generateParamlessUnionQuery(historyLogMembers, selection);
        } else {
            return generateUnionQuery(historyLogMembers);
        }
    }

    /* package private */void clearProvider(int providerId) {

        synchronized (mUriQueryCache) {
            for (String[] uriQueryCacheKey : mUriQueryCache.keySet()) {
                if (contains(uriQueryCacheKey, providerId)) {
                    mUriQueryCache.remove(uriQueryCacheKey);
                }
            }
            mSubQueries.remove(providerId);
        }

    }

    /* package private */void clear() {
        mSubQueries.clear();
        mUriQueryCache.clear();
    }

}
