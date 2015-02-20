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

import android.net.Uri;

import java.util.Map;

/* package private */class HistoryMemberDatabase {

    private final int mProviderId;

    private final Uri mContentProviderUri;

    private Uri mDatabaseUri;

    private String mDatabaseName;

    private final String mTableName;

    private final Map<String, String> mColumnMapping;

    private long mMaxId = 0;

    private boolean mAttached;

    public HistoryMemberDatabase(int memberId, Uri contentProviderUri, String databaseName,
            Uri databaseUri, String tableName, Map<String, String> columnMapping) {
        super();
        mProviderId = memberId;
        mContentProviderUri = contentProviderUri;
        mDatabaseName = databaseName;
        mDatabaseUri = databaseUri;
        mTableName = tableName;
        mColumnMapping = columnMapping;
    }

    public String getDatabaseName() {
        return mDatabaseName;
    }

    public int getProviderId() {
        return mProviderId;
    }

    public Uri getContentProviderUri() {
        return mContentProviderUri;
    }

    void setDatabaseUri(Uri databaseUri) {
        mDatabaseUri = databaseUri;
    }

    public Uri getDatabaseUri() {
        return mDatabaseUri;
    }

    public String getTableName() {
        return mTableName;
    }

    public Map<String, String> getColumnMapping() {
        return mColumnMapping;
    }

    public void setAttached(boolean attached) {
        mAttached = attached;
    }

    public boolean isAttached() {
        return mAttached;
    }

    public long getMaxId() {
        return mMaxId;
    }

    public void setMaxId(long maxId) {
        mMaxId = maxId;
    }
}
